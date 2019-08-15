package eu.seitzal.opentab.controllers

import javax.inject._

import play.api._
import play.api.mvc._

import play.api.db.Database

import akka.actor.ActorSystem
import scala.concurrent.{Future, ExecutionContext}

import eu.seitzal.opentab.exceptions._

@Singleton
class TemplateController @Inject()(
  actorSystem: ActorSystem,
  db: Database,
  cc: ControllerComponents)(
  implicit ec: ExecutionContext)
  extends AbstractController(cc) {

  val jdbcExecutionContext = 
    actorSystem.dispatchers.lookup("jdbc-execution-context")

  val config = Map("apptitle" -> "OpenTab Alpha", "location" -> ".")

  def renderIndex() = Action.async { implicit request: Request[AnyContent] =>
    Future {
      Ok(views.html.index(config, Map()))
    }
  }

  def renderLogin() = Action.async { implicit request: Request[AnyContent] =>
    Future {
      Ok(views.html.login(config, Map()))
    }
  }

}
