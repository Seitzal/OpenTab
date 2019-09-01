package eu.seitzal.opentab.controllers

import eu.seitzal.opentab._
import shortcuts._
import models._
import auth._

import upickle.{default => json}

import javax.inject._
import play.api._
import play.api.mvc._
import play.api.routing._
import play.api.db.Database
import akka.actor.ActorSystem
import scala.concurrent.{Future, ExecutionContext}

@Singleton
class RESTController @Inject()(
  actorSystem: ActorSystem,
  cfg: Configuration,
  db: Database,
  cc: ControllerComponents)(
  implicit ec: ExecutionContext)
  extends AbstractController(cc) {

  implicit def database = db
  implicit def config = cfg

  val jdbcExecutionContext = 
    actorSystem.dispatchers.lookup("jdbc-execution-context")

  val pairingExecutionContext = 
    actorSystem.dispatchers.lookup("pairing-execution-context")

  def remoteVerifyKey = Action.async { implicit request: Request[AnyContent] =>
    implicit val ec = jdbcExecutionContext
    Future {
      request.headers.get("Authorization").map(verifyKey) match {
        case Some(keyData) => Ok(json.write(keyData)).as("application/json")
        case None => BadRequest("No API key found in authorization header.")
      }
    } recover {
        case ex: Throwable => InternalServerError(ex.getMessage)
    }
  }

  def getAllTabs = Action.async { implicit request: Request[AnyContent] =>
    implicit val ec = jdbcExecutionContext
    Future {
      val auth = request.headers.get("Authorization").map(verifyKey)
      val tabs = Tab.getAll(database)
      auth match {
        case Some(keyData) => {
          if (!keyData.found)
            Unauthorized("Invalid API key")
          else if (keyData.expired)
            Unauthorized("API key has expired")
          else {
            val visibleTabs = 
              tabs.filter(tab => userCanSeeTab(keyData.userid, tab))
            Ok(json.write(visibleTabs)).as("application/json")
          }
        }
        case None => {
          val visibleTabs = tabs.filter(_.isPublic)
          Ok(json.write(visibleTabs)).as("application/json")
        }
      }
    } recover {
      case ex: Throwable => InternalServerError(ex.getMessage)
    }
  }

  def getTab(tabid: Int) =  Action.async { implicit request: Request[AnyContent] =>
    implicit val ec = jdbcExecutionContext
    Future {
      val auth = request.headers.get("Authorization").map(verifyKey)
      val tab = Tab(tabid)
      auth match {
        case Some(keyData) => {
          if (!keyData.found)
            Unauthorized("Invalid API key")
          else if (keyData.expired)
            Unauthorized("API key has expired")
          else if (tab.isPublic || userCanSeeTab(keyData.userid, tab))
            Ok(json.write(tab)).as("application/json")
          else
            Forbidden("Permission denied")
        }
        case None => {
          if (tab.isPublic)
            Ok(json.write(tab)).as("application/json")
          else
            Unauthorized("Authorization required")
        }
      }
    } recover {
      case ex: NotFoundException => NotFound(ex.getMessage)
      case ex: Throwable => InternalServerError(ex.getMessage)
    }
  }

  def getAllTeams(tabid: Int) = Action.async {
    implicit request: Request[AnyContent] => {
      implicit val ec = jdbcExecutionContext
      Future {
        val auth = request.headers.get("Authorization").map(verifyKey)
        val tab = Tab(tabid)
        val teams = Team.getAll(tabid)
        auth match {
          case Some(keyData) => {
            if (!keyData.found)
              Unauthorized("Invalid API key")
            else if (keyData.expired)
              Unauthorized("API key has expired")
            else if (tab.isPublic || userCanSeeTab(keyData.userid, tab))
              Ok(json.write(teams)).as("application/json")
            else
              Forbidden("Permission denied")
          }
          case None => {
            if (tab.isPublic)
              Ok(json.write(teams)).as("application/json")
            else
              Unauthorized("Authorization required")
          }
        }
      } recover {
        case ex: NotFoundException => NotFound(ex.getMessage)
        case ex: Throwable => InternalServerError(ex.getMessage)
      }
    }
  }

  def getTeam(id: Int) = Action.async {implicit request: Request[AnyContent] =>
    implicit val ec = jdbcExecutionContext
    Future {
      val auth = request.headers.get("Authorization").map(verifyKey)
      val team = Team(id)
      val tab = Tab(team.tabid)
      auth match {
        case Some(keyData) => {
          if (!keyData.found)
            Unauthorized("Invalid API key")
          else if (keyData.expired)
            Unauthorized("API key has expired")
          else if (tab.isPublic || userCanSeeTab(keyData.userid, tab))
            Ok(json.write(team)).as("application/json")
          else
            Forbidden("Permission denied")
        }
        case None => {
          if (tab.isPublic)
            Ok(json.write(team)).as("application/json")
          else
            Unauthorized("Authorization required")
        }
      }
    } recover {
      case ex: NotFoundException => NotFound(ex.getMessage)
      case ex: Throwable => InternalServerError(ex.getMessage)
    }
  }

  def createTeam() = Action.async(parse.formUrlEncoded) {
    implicit request: Request[Map[String,Seq[String]]] => {
      implicit val ec = jdbcExecutionContext
      Future {
        val auth = request.headers.get("Authorization").map(verifyKey)
        val formData = request.body
        auth match {
          case Some(keyData) => {
            if (!keyData.found)
              Unauthorized("Invalid API key")
            else if (keyData.expired)
              Unauthorized("API key has expired")
            else formData.get("tabid").map(seq => Tab(seq(0).toInt)) match {
              case Some(tab) => {
                if (userCanSetupTab(keyData.userid, tab.id)) {
                  val nameOpt = formData.get("name").map(seq => seq(0))
                  val delegOpt = formData.get("delegation").map(seq => seq(0))
                  val statusOpt = formData.get("status").map(seq => seq(0).toInt)
                  (nameOpt, delegOpt, statusOpt) match {
                    case (Some(name), Some(deleg), Some(status)) => {
                      val newteam = Team.create(tab.id, name, deleg, status)
                      Ok(json.write(newteam)).as("application/json")
                    }
                    case _ => BadRequest("Invalid post data")
                  }
                } else {
                  Forbidden("Permission denied")
                }
              }
              case None => BadRequest("No tab ID specified")
            }
          }
          case None => Unauthorized("Authorization required")
        }
      } recover {
        case ex: NotFoundException => NotFound(ex.getMessage)
        case ex: Throwable => InternalServerError(ex.getMessage)
      }
    }
  }

  def deleteTeam(id: Int) = Action.async {implicit request: Request[AnyContent] =>
    implicit val ec = jdbcExecutionContext
    Future {
      val auth = request.headers.get("Authorization").map(verifyKey)
      val team = Team(id)
      val tab = Tab(team.tabid)
      auth match {
        case Some(keyData) => {
          if (!keyData.found)
            Unauthorized("Invalid API key")
          else if (keyData.expired)
            Unauthorized("API key has expired")
          else if (userCanSetupTab(keyData.userid, tab)) {
            team.delete()
            NoContent
          } else
            Forbidden("Permission denied")
        }
        case None => Unauthorized("Authorization required")
      }
    } recover {
      case ex: NotFoundException => NotFound(ex.getMessage)
      case ex: Throwable => InternalServerError(ex.getMessage)
    }
  }

  def updateTeam(id: Int) = Action.async(parse.formUrlEncoded) {
    implicit request: Request[Map[String,Seq[String]]] => {
      implicit val ec = jdbcExecutionContext
      Future {
        val auth = request.headers.get("Authorization").map(verifyKey)
        val team = Team(id)
        val tab = Tab(team.tabid)
        auth match {
          case Some(keyData) => {
            if (!keyData.found)
              Unauthorized("Invalid API key")
            else if (keyData.expired)
              Unauthorized("API key has expired")
            else if (userCanSetupTab(keyData.userid, tab)) {
              val formData = request.body
              val newteam = team.update(
                formData.get("name").map(seq => seq(0)),
                formData.get("delegation").map(seq => seq(0)),
                formData.get("status").map(seq => seq(0).toInt))
              Ok(json.write(newteam)).as("application/json")
            } else
              Forbidden("Permission denied")
          }
          case None => Unauthorized("Authorization required")
        }
      } recover {
        case ex: NotFoundException => NotFound(ex.getMessage)
        case ex: Throwable => InternalServerError(ex.getMessage)
      }
    }
  }

  def toggleTeam(id: Int) = Action.async {implicit request: Request[AnyContent] =>
    implicit val ec = jdbcExecutionContext
    Future {
      val auth = request.headers.get("Authorization").map(verifyKey)
      val team = Team(id)
      val tab = Tab(team.tabid)
      auth match {
        case Some(keyData) => {
          if (!keyData.found)
            Unauthorized("Invalid API key")
          else if (keyData.expired)
            Unauthorized("API key has expired")
          else if (userCanSetupTab(keyData.userid, tab)) {
            val newteam = team.toggleActive()
            Ok(json.write(newteam)).as("application/json")
          } else
            Forbidden("Permission denied")
        }
        case None => Unauthorized("Authorization required")
      }
    } recover {
      case ex: NotFoundException => NotFound(ex.getMessage)
      case ex: Throwable => InternalServerError(ex.getMessage)
    }
  }

  def getAllSpeakers(tabid: Int) = Action.async {
    implicit request: Request[AnyContent] => {
      implicit val ec = jdbcExecutionContext
      Future {
        val auth = request.headers.get("Authorization").map(verifyKey)
        val tab = Tab(tabid)
        val speakers = tab.speakers.map(_.externalRepresentation)
        auth match {
          case Some(keyData) => {
            if (!keyData.found)
              Unauthorized("Invalid API key")
            else if (keyData.expired)
              Unauthorized("API key has expired")
            else if (tab.isPublic || userCanSeeTab(keyData.userid, tab))
              Ok(json.write(speakers)).as("application/json")
            else
              Forbidden("Permission denied")
          }
          case None => {
            if (tab.isPublic)
              Ok(json.write(speakers)).as("application/json")
            else
              Unauthorized("Authorization required")
          }
        }
      } recover {
        case ex: NotFoundException => NotFound(ex.getMessage)
        case ex: Throwable => InternalServerError(ex.getMessage)
      }
    }
  }

  def getAllSpeakersOnTeam(teamid: Int) = Action.async {
    implicit request: Request[AnyContent] => {
      implicit val ec = jdbcExecutionContext
      Future {
        val auth = request.headers.get("Authorization").map(verifyKey)
        val team = Team(teamid)
        val tab = team.tab
        val speakers = team.speakers.map(_.externalRepresentation)
        auth match {
          case Some(keyData) => {
            if (!keyData.found)
              Unauthorized("Invalid API key")
            else if (keyData.expired)
              Unauthorized("API key has expired")
            else if (tab.isPublic || userCanSeeTab(keyData.userid, tab))
              Ok(json.write(speakers)).as("application/json")
            else
              Forbidden("Permission denied")
          }
          case None => {
            if (tab.isPublic)
              Ok(json.write(speakers)).as("application/json")
            else
              Unauthorized("Authorization required")
          }
        }
      } recover {
        case ex: NotFoundException => NotFound(ex.getMessage)
        case ex: Throwable => InternalServerError(ex.getMessage)
      }
    }
  }

  def getSpeaker(id: Int) = Action.async {implicit request: Request[AnyContent] =>
    implicit val ec = jdbcExecutionContext
    Future {
      val auth = request.headers.get("Authorization").map(verifyKey)
      val speaker = Speaker(id).externalRepresentation
      val tab = Tab(speaker.tabid)
      auth match {
        case Some(keyData) => {
          if (!keyData.found)
            Unauthorized("Invalid API key")
          else if (keyData.expired)
            Unauthorized("API key has expired")
          else if (tab.isPublic || userCanSeeTab(keyData.userid, tab))
            Ok(json.write(speaker)).as("application/json")
          else
            Forbidden("Permission denied")
        }
        case None => {
          if (tab.isPublic)
            Ok(json.write(speaker)).as("application/json")
          else
            Unauthorized("Authorization required")
        }
      }
    } recover {
      case ex: NotFoundException => NotFound(ex.getMessage)
      case ex: Throwable => InternalServerError(ex.getMessage)
    }
  }

  def createSpeaker() = Action.async(parse.formUrlEncoded) {
    implicit request: Request[Map[String,Seq[String]]] => {
      implicit val ec = jdbcExecutionContext
      Future {
        val auth = request.headers.get("Authorization").map(verifyKey)
        val formData = request.body
        auth match {
          case Some(keyData) => {
            if (!keyData.found)
              Unauthorized("Invalid API key")
            else if (keyData.expired)
              Unauthorized("API key has expired")
            else formData.get("teamid").map(seq => Team(seq(0).toInt).tab) match {
              case Some(tab) => {
                if (userCanSetupTab(keyData.userid, tab.id)) {
                  val teamidOpt = formData.get("teamid").map(seq => seq(0).toInt)
                  val firstNameOpt = formData.get("firstname").map(seq => seq(0))
                  val lastNameOpt = formData.get("lastname").map(seq => seq(0))
                  val statusOpt = formData.get("status").map(seq => seq(0).toInt)
                  (teamidOpt, firstNameOpt, lastNameOpt, statusOpt) match {
                    case (Some(teamid), Some(fname), Some(lname), Some(status)) => {
                      val newspeaker = Speaker.create(teamid, fname, lname, status)
                      Ok(json.write(newspeaker.externalRepresentation)).as("application/json")
                    }
                    case _ => BadRequest("Invalid post data")
                  }
                } else {
                  Forbidden("Permission denied")
                }
              }
              case None => BadRequest("No valid team id specified")
            }
          }
          case None => Unauthorized("Authorization required")
        }
      } recover {
        case ex: NotFoundException => NotFound(ex.getMessage)
        case ex: Throwable => InternalServerError(ex.getMessage)
      }
    }
  }

  def deleteSpeaker(id: Int) = Action.async {implicit request: Request[AnyContent] =>
    implicit val ec = jdbcExecutionContext
    Future {
      val auth = request.headers.get("Authorization").map(verifyKey)
      val speaker = Speaker(id)
      val tab = Tab(speaker.tabid)
      auth match {
        case Some(keyData) => {
          if (!keyData.found)
            Unauthorized("Invalid API key")
          else if (keyData.expired)
            Unauthorized("API key has expired")
          else if (userCanSetupTab(keyData.userid, tab)) {
            speaker.delete()
            NoContent
          } else
            Forbidden("Permission denied")
        }
        case None => Unauthorized("Authorization required")
      }
    } recover {
      case ex: NotFoundException => NotFound(ex.getMessage)
      case ex: Throwable => InternalServerError(ex.getMessage)
    }
  }

  def updateSpeaker(id: Int) = Action.async(parse.formUrlEncoded) {
    implicit request: Request[Map[String,Seq[String]]] => {
      implicit val ec = jdbcExecutionContext
      Future {
        val auth = request.headers.get("Authorization").map(verifyKey)
        val speaker = Speaker(id)
        val tab = speaker.tab
        auth match {
          case Some(keyData) => {
            if (!keyData.found)
              Unauthorized("Invalid API key")
            else if (keyData.expired)
              Unauthorized("API key has expired")
            else if (userCanSetupTab(keyData.userid, tab)) {
              val formData = request.body
              val newspeaker = speaker.update(
                formData.get("firstname").map(seq => seq(0)),
                formData.get("lastname").map(seq => seq(0)),
                formData.get("status").map(seq => seq(0).toInt))
              Ok(json.write(newspeaker)).as("application/json")
            } else
              Forbidden("Permission denied")
          }
          case None => Unauthorized("Authorization required")
        }
      } recover {
        case ex: NotFoundException => NotFound(ex.getMessage)
        case ex: Throwable => InternalServerError(ex.getMessage)
      }
    }
  }

  def getRandomPairings(tabid: Int) = Action.async{ implicit request: Request[AnyContent] =>
    implicit val ec = pairingExecutionContext
    Future {
      val auth = request.headers.get("Authorization").map(verifyKey)
      val tab = Tab(tabid)
      auth match {
        case Some(keyData) => {
          if (!keyData.found)
            Unauthorized("Invalid API key")
          else if (keyData.expired)
            Unauthorized("API key has expired")
          else if (userCanSetupTab(keyData.userid, tab)) {
            Ok(json.write(RandomPairings(tab))).as("application/json")
          } else
            Forbidden("Permission denied")
        }
        case None => Unauthorized("Authorization required")
      }
    } recover {
      case ex: NotFoundException => NotFound(ex.getMessage)
      case ex: Throwable => InternalServerError(ex.getMessage)
    }
  }

  def getRound(tabid: Int, roundnumber: Int) = Action.async {
    implicit request: Request[AnyContent] => {
      implicit val ec = jdbcExecutionContext
      Future {
        val auth = request.headers.get("Authorization").map(verifyKey)
        val tab = Tab(tabid)
        val round = tab.round(roundnumber)
        auth match {
          case Some(keyData) => {
            if (!keyData.found)
              Unauthorized("Invalid API key")
            else if (keyData.expired)
              Unauthorized("API key has expired")
            else if (tab.isPublic || userCanSeeTab(keyData.userid, tab))
              Ok(json.write(round)).as("application/json")
            else
              Forbidden("Permission denied")
          }
          case None => {
            if (tab.isPublic)
              Ok(json.write(round)).as("application/json")
            else
              Unauthorized("Authorization required")
          }
        }
      } recover {
        case ex: NotFoundException => NotFound(ex.getMessage)
        case ex: Throwable => InternalServerError(ex.getMessage)
      }
    }
  }

  def getRounds(tabid: Int) = Action.async {
    implicit request: Request[AnyContent] => {
      implicit val ec = jdbcExecutionContext
      Future {
        val auth = request.headers.get("Authorization").map(verifyKey)
        val tab = Tab(tabid)
        val rounds = tab.rounds
        auth match {
          case Some(keyData) => {
            if (!keyData.found)
              Unauthorized("Invalid API key")
            else if (keyData.expired)
              Unauthorized("API key has expired")
            else if (tab.isPublic || userCanSeeTab(keyData.userid, tab))
              Ok(json.write(rounds)).as("application/json")
            else
              Forbidden("Permission denied")
          }
          case None => {
            if (tab.isPublic)
              Ok(json.write(rounds)).as("application/json")
            else
              Unauthorized("Authorization required")
          }
        }
      } recover {
        case ex: NotFoundException => NotFound(ex.getMessage)
        case ex: Throwable => InternalServerError(ex.getMessage)
      }
    }
  }

  def addRound(tabid: Int) = Action.async {
    implicit request: Request[AnyContent] => {
      implicit val ec = jdbcExecutionContext
      Future {
        val auth = request.headers.get("Authorization").map(verifyKey)
        val tab = Tab(tabid)
        auth match {
          case Some(keyData) => {
            if (!keyData.found)
              Unauthorized("Invalid API key")
            else if (keyData.expired)
              Unauthorized("API key has expired")
            else if (userCanSetupTab(keyData.userid, tab)) {
              val round = tab.addRound()
              Ok(json.write(round)).as("application/json")
            } else
              Forbidden("Permission denied")
          }
          case None => Unauthorized("Authorization required")
        }
      } recover {
        case ex: NotFoundException => NotFound(ex.getMessage)
        case ex: Throwable => InternalServerError(ex.getMessage)
      }
    }
  }

  def deleteRound(tabid: Int, roundNumber: Int) = Action.async {
    implicit request: Request[AnyContent] => {
      implicit val ec = jdbcExecutionContext
      Future {
        val auth = request.headers.get("Authorization").map(verifyKey)
        val tab = Tab(tabid)
        auth match {
          case Some(keyData) => {
            if (!keyData.found)
              Unauthorized("Invalid API key")
            else if (keyData.expired)
              Unauthorized("API key has expired")
            else if (userCanSetupTab(keyData.userid, tab)) {
              val round = tab.round(roundNumber)
              round.delete()
              NoContent
            } else
              Forbidden("Permission denied")
          }
          case None => Unauthorized("Authorization required")
        }
      } recover {
        case ex: NotFoundException => NotFound(ex.getMessage)
        case ex: Throwable => InternalServerError(ex.getMessage)
      }
    }
  }

  def jsRouter() = Action.async { implicit request => Future {
    Ok(
      JavaScriptReverseRouter("routes")(
        routes.javascript.RESTController.remoteVerifyKey,
        routes.javascript.RESTController.getAllTabs,
        routes.javascript.RESTController.getTab,
        routes.javascript.RESTController.getAllTeams,
        routes.javascript.RESTController.getTeam,
        routes.javascript.RESTController.createTeam,
        routes.javascript.RESTController.deleteTeam,
        routes.javascript.RESTController.updateTeam,
        routes.javascript.RESTController.toggleTeam,
        routes.javascript.RESTController.getAllSpeakers,
        routes.javascript.RESTController.getAllSpeakersOnTeam,
        routes.javascript.RESTController.getSpeaker,
        routes.javascript.RESTController.createSpeaker,
        routes.javascript.RESTController.deleteSpeaker,
        routes.javascript.RESTController.updateSpeaker,
        routes.javascript.RESTController.getRandomPairings,
        routes.javascript.RESTController.getRound,
        routes.javascript.RESTController.getRounds,
        routes.javascript.RESTController.addRound,
        routes.javascript.RESTController.deleteRound
      )
    ).as(http.MimeTypes.JAVASCRIPT)
  }}

}