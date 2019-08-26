package eu.seitzal.opentab.controllers

import eu.seitzal.opentab._
import models._
import permissions._
import shortcuts._
import upickle.{default => json}

import java.net.URLEncoder
import javax.inject._
import play.api._
import play.api.mvc._
import play.api.db.Database
import akka.actor.ActorSystem
import scala.concurrent.{Future, ExecutionContext}

@Singleton
class TemplateController @Inject()(
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

  def renderIndex() = Action.async { implicit request: Request[AnyContent] =>
    Future {
      Ok(views.html.index(config))
    }
  }

  def renderLogin() = Action.async { implicit request: Request[AnyContent] =>
    Future {
      if (loggedIn) {
        Redirect(location)
      } else {
        Ok(views.html.login(config))
      }
    }
  }

  def renderTabIndex(tabid: Int) = Action.async { 
    implicit request: Request[AnyContent] => {
      implicit val ec = jdbcExecutionContext
      Future {
        val tab = Tab(tabid)
        if (tab.isPublic) {
          Ok(views.html.tab.tabindex(tab, config, db))
        } else request.session.get("userid").map(_.toInt) match {
          case Some(uid) => {
            if (userCanSeeTab(uid, tab)) {
              Ok(views.html.tab.tabindex(tab, config, db))
            } else {
              Forbidden("403 Forbidden: Permission denied")
            }
          }
          case None =>
            Redirect(location + "/login?origin=" + 
              URLEncoder.encode(location + "/tab/" + tabid, "utf-8"))
        }
      } recover {
        case ex: NotFoundException => 
          NotFound("404 Not Found: " + ex.getMessage)
        case ex: Throwable => 
          InternalServerError("503 Internal Server Error: " + ex.getMessage)
      }
    }
  }

  def renderTeams(tabid: Int) = Action.async { 
    implicit request: Request[AnyContent] => {
      implicit val ec = jdbcExecutionContext
      Future {
        val tab = Tab(tabid)
        if (tab.isPublic) {
          Ok(views.html.tab.teams(tab, config, db))
        } else request.session.get("userid").map(_.toInt) match {
          case Some(uid) => {
            if (userCanSeeTab(uid, tab)) {
              Ok(views.html.tab.teams(tab, config, db))
            } else {
              Forbidden("403 Forbidden: Permission denied")
            }
          }
          case None =>
            Redirect(location + "/login?origin=" + 
              URLEncoder.encode(location + "/tab/" + tabid + "/teams", "utf-8"))
        }
      } recover {
        case ex: NotFoundException => 
          NotFound("404 Not Found: " + ex.getMessage)
        case ex: Throwable => 
          InternalServerError("503 Internal Server Error: " + ex.getMessage)
      }
    }
  }

  def renderSpeakers(tabid: Int, team : Option[String]) = Action.async { 
    implicit request: Request[AnyContent] => {
      implicit val ec = jdbcExecutionContext
      Future {
        val tab = Tab(tabid)
        if (tab.isPublic) {
          Ok(views.html.tab.speakers(tab, config, db, team))
        } else request.session.get("userid").map(_.toInt) match {
          case Some(uid) => {
            if (userCanSeeTab(uid, tab)) {
              Ok(views.html.tab.speakers(tab, config, db, team))
            } else {
              Forbidden("403 Forbidden: Permission denied")
            }
          }
          case None =>
            Redirect(location + "/login?origin=" + 
              URLEncoder.encode(location + "/tab/" + tabid + "/speakers", "utf-8"))
        }
      } recover {
        case ex: NotFoundException => 
          NotFound("404 Not Found: " + ex.getMessage)
        case ex: Throwable => 
          InternalServerError("503 Internal Server Error: " + ex.getMessage)
      }
    }
  }

  def renderRounds(tabid: Int) = Action.async { 
    implicit request: Request[AnyContent] => {
      implicit val ec = jdbcExecutionContext
      Future {
        val tab = Tab(tabid)
        if (tab.isPublic) {
          Ok(views.html.tab.rounds(tab, config, db))
        } else request.session.get("userid").map(_.toInt) match {
          case Some(uid) => {
            if (userCanSeeTab(uid, tab)) {
              Ok(views.html.tab.rounds(tab, config, db))
            } else {
              Forbidden("403 Forbidden: Permission denied")
            }
          }
          case None =>
            Redirect(location + "/login?origin=" + 
              URLEncoder.encode(location + "/tab/" + tabid + "/rounds", "utf-8"))
        }
      } recover {
        case ex: NotFoundException => 
          NotFound("404 Not Found: " + ex.getMessage)
        case ex: Throwable => 
          InternalServerError("503 Internal Server Error: " + ex.getMessage)
      }
    }
  }

}
