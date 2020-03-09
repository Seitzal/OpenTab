package opentab.controllers

import opentab._
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
  implicit def actionBuilder = Action
  implicit def defaultParser = parse.anyContent

  val jdbcExecutionContext = 
    actorSystem.dispatchers.lookup("jdbc-execution-context")

  val pairingExecutionContext = 
    actorSystem.dispatchers.lookup("pairing-execution-context")

  def getAllTabs = optionalAuthAction((userOpt, request) => userOpt match {
    case Some(user) => {
      val visibleTabs = Tab.getAll.filter(tab => userCanSeeTab(user, tab))
      Ok(json.write(visibleTabs)).as("application/json")
    }
    case None => {
      val visibleTabs = Tab.getAll.filter(_.isPublic)
      Ok(json.write(visibleTabs)).as("application/json")
    }
  })

  def getTab(tabid: Int) = optionalAuthAction((userOpt, request) => {
    val tab = Tab(tabid)
    userOpt match {
      case Some(user) =>
        if (tab.isPublic || userCanSeeTab(user, tab))
          Ok(json.write(tab)).as("application/json")
        else PermissionDenied
      case None =>
        if (tab.isPublic)
          Ok(json.write(tab)).as("application/json")
        else AuthorizationRequired
    }
  })

  def getPermissions(tabid: Int) = optionalAuthAction((userOpt, request) => {
    val tab = Tab(tabid)
    userOpt match {
      case Some(user) =>
        if (tab.isPublic) {
          val perms = permissions.readEntry(user.id, tabid)
          val perms_ = permissions.Entry(true, perms.results, perms.setup, perms.own)
          Ok(json.write(perms)).as("application/json")
        } else 
          Ok(json.write(permissions.readEntry(user.id, tabid))).as("application/json")
      case None =>
        if (tab.isPublic)
          Ok(json.write(permissions.Entry(true, false, false, false))).as("application/json")
        else 
          Ok(json.write(permissions.Entry(false, false, false, false))).as("application/json")
    }
  })

  def getAllPermissions() = optionalAuthAction((userOpt, request) => {
    userOpt match {
      case Some(user) => {
        val perms =
          Tab.getAll
          .filter(tab => userCanSeeTab(user, tab))
          .map(tab => (tab.id, permissions.readEntry(user.id, tab.id)))
          .map(set =>
            if (Tab(set._1).isPublic)
              (set._1, permissions.Entry(true, set._2.results, set._2.setup, set._2.own))
              else set)
        Ok(json.write(perms)).as("application/json")
      }
      case None => {
        val perms =
          Tab.getAll
          .filter(_.isPublic)
          .map(tab => (tab.id, permissions.Entry(true, false, false, false)))
        Ok(json.write(perms)).as("application/json")
      }
    }
  })

  def getAllTeams(tabid: Int) = optionalAuthAction((userOpt, request) => {
    val tab = Tab(tabid)
    userOpt match {
      case Some(user) =>
        if (tab.isPublic || userCanSeeTab(user, tab))
          Ok(json.write(tab.teams)).as("application/json")
        else PermissionDenied
      case None =>
        if (tab.isPublic)
          Ok(json.write(tab.teams)).as("application/json")
        else AuthorizationRequired
    }
  })

  def getAllDelegations(tabid: Int) = authAction((user, request) => {
    val tab = Tab(tabid)
    if (userCanSetupTab(user, tab))
      Ok(json.write(tab.delegations)).as("application/json")
    else PermissionDenied
  })

  def getTeam(teamid: Int) = optionalAuthAction((userOpt, request) => {
    val team = Team(teamid)
    val tab = team.tab
    userOpt match {
      case Some(user) =>
        if (tab.isPublic || userCanSeeTab(user, tab))
          Ok(json.write(team)).as("application/json")
        else PermissionDenied
      case None =>
        if (tab.isPublic)
          Ok(json.write(team)).as("application/json")
        else AuthorizationRequired
    }
  })

  def createTeam() = authAction[Map[String,Seq[String]]](parse.formUrlEncoded, (user, request) => {
    val formData = request.body
    formData.get("tabid").map(seq => Tab(seq(0).toInt)) match {
      case Some(tab) =>
        if (userCanSetupTab(user, tab.id)) {
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
        } else PermissionDenied
      case None => BadRequest("No tab ID specified")
    }
  })

  def deleteTeam(id: Int) = authAction((user, request) => {
    val team = Team(id)
    if (userCanSetupTab(user, team.tab)) {
      team.delete()
      NoContent
    } else PermissionDenied
  })

  def updateTeam(id: Int) = authAction[Map[String,Seq[String]]](parse.formUrlEncoded, (user, request) => {
    val team = Team(id)
    if (userCanSetupTab(user, team.tab)) {
      val formData = request.body
      val newteam = team.update(
        formData.get("name").map(seq => seq(0)),
        formData.get("delegation").map(seq => seq(0)),
        formData.get("status").map(seq => seq(0).toInt))
      Ok(json.write(newteam)).as("application/json")
    } else PermissionDenied
  })

  def toggleTeam(id: Int) = authAction((user, request) => {
    val team = Team(id)
    if (userCanSetupTab(user, team.tab)) {
      val newteam = team.toggleActive()
      Ok(json.write(newteam)).as("application/json")
    } else PermissionDenied
  })

  def getAllSpeakers(tabid: Int) = optionalAuthAction((userOpt, request) => {
    val tab = Tab(tabid)
    userOpt match {
      case Some(user) =>
        if (tab.isPublic || userCanSeeTab(user, tab))
          Ok(json.write(tab.speakers.map(_.externalRepresentation))).as("application/json")
        else PermissionDenied
      case None =>
        if (tab.isPublic)
          Ok(json.write(tab.speakers.map(_.externalRepresentation))).as("application/json")
        else AuthorizationRequired
    }
  })

  def getAllSpeakersOnTeam(id: Int) = optionalAuthAction((userOpt, request) => {
    val team = Team(id)
    val tab = team.tab
    userOpt match {
      case Some(user) =>
        if (tab.isPublic || userCanSeeTab(user, tab))
          Ok(json.write(team.speakers.map(_.externalRepresentation))).as("application/json")
        else PermissionDenied
      case None =>
        if (tab.isPublic)
          Ok(json.write(team.speakers.map(_.externalRepresentation))).as("application/json")
        else AuthorizationRequired
    }
  })

  def getSpeaker(id: Int) = optionalAuthAction((userOpt, request) => {
    val speaker = Speaker(id)
    val tab = speaker.tab
    userOpt match {
      case Some(user) =>
        if (tab.isPublic || userCanSeeTab(user, tab))
          Ok(json.write(speaker.externalRepresentation)).as("application/json")
        else PermissionDenied
      case None =>
        if (tab.isPublic)
          Ok(json.write(speaker.externalRepresentation)).as("application/json")
        else AuthorizationRequired
    }
  })

  def createSpeaker() = authAction[Map[String,Seq[String]]](parse.formUrlEncoded, (user, request) => {
    val formData = request.body
    formData.get("teamid").map(seq => Team(seq(0).toInt).tab) match {
      case Some(tab) => {
        if (userCanSetupTab(user, tab)) {
          val teamidOpt = formData.get("teamid").map(seq => seq(0).toInt)
          val firstNameOpt = formData.get("firstName").map(seq => seq(0))
          val lastNameOpt = formData.get("lastName").map(seq => seq(0))
          val statusOpt = formData.get("status").map(seq => seq(0).toInt)
          (teamidOpt, firstNameOpt, lastNameOpt, statusOpt) match {
            case (Some(teamid), Some(fname), Some(lname), Some(status)) => {
              val speaker = Speaker.create(teamid, fname, lname, status)
              Ok(json.write(speaker.externalRepresentation)).as("application/json")
            }
            case _ => BadRequest("Invalid post data")
          }
        } else PermissionDenied
      }
      case None => BadRequest("No valid team id specified")
    }
  })

  def deleteSpeaker(id: Int) = authAction((user, request) => {
    val speaker = Speaker(id)
    if (userCanSetupTab(user, speaker.tab)) {
      speaker.delete()
      NoContent
    } else PermissionDenied
  })

  def updateSpeaker(id: Int) = authAction[Map[String,Seq[String]]](parse.formUrlEncoded, (user, request) => {
    val speaker = Speaker(id)
    if (userCanSetupTab(user, speaker.tab)) {
      val formData = request.body
      val newSpeaker = speaker.update(
        formData.get("firstName").map(seq => seq(0)),
        formData.get("lastName").map(seq => seq(0)),
        formData.get("status").map(seq => seq(0).toInt))
      Ok(json.write(newSpeaker.externalRepresentation)).as("application/json")
    } else PermissionDenied
  })

  def getRandomPairings(tabid: Int) = authAction((user, request) => {
      val tab = Tab(tabid)
      if (userCanSetupTab(user, tab))
        Ok(json.write(RandomPairings(tab))).as("application/json")
      else PermissionDenied
  })(pairingExecutionContext, db, Action, parse.anyContent)

  def getRound(tabid: Int, roundnumber: Int) = optionalAuthAction((userOpt, request) => {
    val tab = Tab(tabid)
    val round = tab.round(roundnumber)
    userOpt match {
      case Some(user) =>
        if (tab.isPublic || userCanSeeTab(user, tab))
          Ok(json.write(round)).as("application/json")
        else PermissionDenied
      case None =>
        if (tab.isPublic)
          Ok(json.write(round)).as("application/json")
        else AuthorizationRequired
    }
  })

  def getRounds(tabid: Int) = optionalAuthAction((userOpt, request) => {
    val tab = Tab(tabid)
    userOpt match {
      case Some(user) =>
        if (tab.isPublic || userCanSeeTab(user, tab))
          Ok(json.write(tab.rounds)).as("application/json")
        else PermissionDenied
      case None =>
        if (tab.isPublic)
          Ok(json.write(tab.rounds)).as("application/json")
        else AuthorizationRequired
    }
  })

  def addRound(tabid: Int) = authAction((user, request) => {
    val tab = Tab(tabid)
    if (userCanSetupTab(user, tab)) {
      val round = tab.addRound()
      Ok(json.write(round)).as("application/json")
    } else PermissionDenied
  })

  def deleteRound(tabid: Int, roundNumber: Int) = authAction((user, request) => {
    val tab = Tab(tabid)
    if (userCanSetupTab(user, tab)) {
      tab.round(roundNumber).delete()
      NoContent
    } else PermissionDenied
  })

  def getDraw(tabid: Int, roundnumber: Int) = optionalAuthAction((userOpt, request) => {
    val tab = Tab(tabid)
    val round = tab.round(roundnumber)
    userOpt match {
      case Some(user) =>
        if (tab.isPublic || userCanSeeTab(user, tab))
          Ok(json.write(round.draw)).as("application/json")
        else PermissionDenied
      case None =>
        if (tab.isPublic)
          Ok(json.write(round.draw)).as("application/json")
        else AuthorizationRequired
    }
  })

  def isDrawn(tabid: Int, roundnumber: Int) = optionalAuthAction((userOpt, request) => {
    val tab = Tab(tabid)
    val round = tab.round(roundnumber)
    userOpt match {
      case Some(user) =>
        if (tab.isPublic || userCanSeeTab(user, tab))
          Ok(json.write(round.drawOption.isDefined)).as("application/json")
        else PermissionDenied
      case None =>
        if (tab.isPublic)
          Ok(json.write(round.drawOption.isDefined)).as("application/json")
        else AuthorizationRequired
    }
  })

  def setDraw(tabid: Int, roundNumber: Int) = authAction((user, request) => {
    val tab = Tab(tabid)
    if (userCanSetupTab(user, tab)) {
      val round = tab.round(roundNumber)
      val drawOpt = request.body.asJson.map(c => json.read[Draw](c.toString()))
      drawOpt match {
        case Some(draw) => {
          round.setDraw(draw)
          Ok(json.write(round.pairings))
        }
        case None =>
          BadRequest("Request body must contain JSON-formatted draw")
      }
    } else PermissionDenied
  })

  def lockRound(tabid: Int, roundNumber: Int) = authAction((user, request) => {
    val tab = Tab(tabid)
    if (userCanSetupTab(user, tab)) {
      tab.round(roundNumber).lock()
      NoContent
    } else PermissionDenied
  })

  def unlockRound(tabid: Int, roundNumber: Int) = authAction((user, request) => {
    val tab = Tab(tabid)
    if (userCanSetupTab(user, tab)) {
      tab.round(roundNumber).unlock()
      NoContent
    } else PermissionDenied
  })

  def getAllJudges(tabid: Int) = authAction((user, request) => {
    val tab = Tab(tabid)
    if (userCanSetupTab(user, tab))
      Ok(json.write(tab.judges)).as("application/json")
    else PermissionDenied
  })

  def getJudge(id: Int) = authAction((user, request) => {
    val judge = Judge(id)
    val tab = judge.tab
    if (userCanSetupTab(user, tab))
      Ok(json.write(judge)).as("application/json")
    else PermissionDenied
  })

  def createJudge() = authAction[Map[String,Seq[String]]](parse.formUrlEncoded, (user, request) => {
    val formData = request.body
    formData.get("tabid").map(seq => Tab(seq(0).toInt)) match {
      case Some(tab) => {
        if (userCanSetupTab(user, tab)) {
          val firstNameOpt = formData.get("firstName").map(seq => seq(0))
          val lastNameOpt = formData.get("lastName").map(seq => seq(0))
          val delegOpt = formData.get("delegation").map(seq => seq(0))
          val ratingOpt = formData.get("rating").map(seq => seq(0).toInt)
          (firstNameOpt, lastNameOpt, ratingOpt) match {
            case (Some(firstName), Some(lastName), Some(rating)) => {
              val newJudge = Judge.create(tab.id, firstName, lastName, rating)
              delegOpt match {
                case Some(delegation) => {
                  tab.teams.filter(t => t.delegation == delegation).foreach(
                    team => newJudge.setClash(team, 10))
                }
                case _ => {}
              }
              Ok(json.write(newJudge)).as("application/json")
            }
            case _ => BadRequest("Invalid post data")
          }
        } else PermissionDenied
      }
      case None => BadRequest("No tab ID specified")
    }
  })

  def deleteJudge(id: Int) = authAction((user, request) => {
    val judge = Judge(id)
    if (userCanSetupTab(user, judge.tab)) {
      judge.delete()
      NoContent
    } else PermissionDenied
  })

  def updateJudge(id: Int) = authAction[Map[String,Seq[String]]](parse.formUrlEncoded, (user, request) => {
    val judge = Judge(id)
    if (userCanSetupTab(user, judge.tab)) {
      val formData = request.body
      val newJudge = judge.update(
        formData.get("firstName").map(seq => seq(0)),
        formData.get("lastName").map(seq => seq(0)),
        formData.get("rating").map(seq => seq(0).toInt))
      Ok(json.write(newJudge)).as("application/json")
    } else PermissionDenied
  })

  def toggleJudge(id: Int) = authAction((user, request) => {
    val judge = Judge(id)
    if (userCanSetupTab(user, judge.tab)) {
      val newJudge = judge.toggleActive()
      Ok(json.write(newJudge)).as("application/json")
    } else PermissionDenied
  })

  def getClashesForJudge(id: Int) = authAction((user, request) => {
    val judge = Judge(id)
    val tab = judge.tab
    if (userCanSetupTab(user, tab))
      Ok(json.write(judge.clashes)).as("application/json")
    else PermissionDenied
  })

  def setClash(judgeid: Int, teamid: Int, level: Int)  = authAction((user, request) => {
    val judge = Judge(judgeid)
    val team = Team(teamid)
    val tab = judge.tab
    if (userCanSetupTab(user, tab))
      if(judge.tabid == team.tabid) {
        judge.setClash(team, level)
        NoContent
      } else Forbidden("Judge and team must be on the same tab")
    else PermissionDenied
  })
        
  def unsetClash(judgeid: Int, teamid: Int)  = authAction((user, request) => {
    val judge = Judge(judgeid)
    val team = Team(teamid)
    val tab = judge.tab
    if (userCanSetupTab(user, tab))
      if(judge.tabid == team.tabid) {
        judge.unsetClash(team)
        NoContent
      } else Forbidden("Judge and team must be on the same tab")
    else PermissionDenied
  })
  
  def jsRouter() = Action.async { implicit request => Future {
    Ok(JavaScriptReverseRouter("routes")(
      routes.javascript.AuthController.remoteVerifyKey,
      routes.javascript.AuthController.signIn,
      routes.javascript.AuthController.signOut,
      routes.javascript.RESTController.getAllPermissions,
      routes.javascript.RESTController.getPermissions,
      routes.javascript.RESTController.getAllTabs,
      routes.javascript.RESTController.getTab,
      routes.javascript.RESTController.getAllTeams,
      routes.javascript.RESTController.getAllDelegations,
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
      routes.javascript.RESTController.deleteRound,
      routes.javascript.RESTController.getDraw,
      routes.javascript.RESTController.setDraw,
      routes.javascript.RESTController.isDrawn,
      routes.javascript.RESTController.lockRound,
      routes.javascript.RESTController.unlockRound,
      routes.javascript.RESTController.getAllJudges,
      routes.javascript.RESTController.getJudge,
      routes.javascript.RESTController.createJudge,
      routes.javascript.RESTController.updateJudge,
      routes.javascript.RESTController.toggleJudge,
      routes.javascript.RESTController.deleteJudge,
      routes.javascript.RESTController.getClashesForJudge,
      routes.javascript.RESTController.setClash,
      routes.javascript.RESTController.unsetClash)
    ).as(http.MimeTypes.JAVASCRIPT)
  }}

}
