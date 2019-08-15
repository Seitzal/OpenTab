package eu.seitzal.opentab.controllers

import eu.seitzal.opentab._
import shortcuts._
import upickle.{default => json}

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

}
