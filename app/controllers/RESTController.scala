package eu.seitzal.opentab.controllers

import eu.seitzal.opentab._
import shortcuts._
import models._
import models.permissions._
import upickle.{default => json}

import javax.inject._
import play.api._
import play.api.mvc._
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
  
  case class KeyData(
    found: Boolean,
    temporary: Boolean,
    expired: Boolean,
    expires: Long,
    userid: Int)
  
  object KeyData {
    implicit val rw: json.ReadWriter[KeyData] = json.macroRW
  }

  def verifyKey(key: String): KeyData = {
    val connection = database.getConnection()
    val queryText = "SELECT * FROM api_keys WHERE val = ?"
    val query = connection.prepareStatement(queryText)
    query.setString(1, key)
    val queryResult = query.executeQuery()
    connection.close()
    if (queryResult.next()) {
      val expires = queryResult.getLong("expires")
      val temporary = expires != 0
      val expired = temporary && expires < timestamp()
      val userid  = queryResult.getInt("userid")
      KeyData(true, temporary, expired, expires, userid) 
    } else {
      KeyData(false, false, false, 0L, 0)
    }
  }

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

}