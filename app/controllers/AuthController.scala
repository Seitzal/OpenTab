package eu.seitzal.opentab.controllers

import eu.seitzal.opentab._
import auth._
import shortcuts._
import upickle.{default => json}

import javax.inject._
import play.api._
import play.api.mvc._
import play.api.db.Database
import akka.actor.ActorSystem
import scala.concurrent.{Future, ExecutionContext}
import java.net.URLDecoder

@Singleton
class AuthController @Inject()(
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

  def login(origin: Option[String]) = Action.async(parse.formUrlEncoded) {
    implicit request: Request[Map[String,Seq[String]]] => {
      implicit val ec = jdbcExecutionContext
      Future {
        val formData = request.body
        (formData.get("username"), formData.get("password")) match {
          case (Some(username), Some(password)) => {
            verifyUser(username(0), password(0)) match {
              case Some(userid) => {
                val key = Keygen.newTempKey
                registerKey(key, userid)
                val session = List(
                  "userid" -> userid.toString,
                  "username" -> username(0),
                  "api_key" -> key)
                origin.map(str => URLDecoder.decode(str, "utf-8")) match {
                case Some(url) => 
                  Redirect(url).withSession(session: _*)
                case None =>
                  Redirect(location).withSession(session: _*)
                }
              }
              case None => Redirect("login?origin=" + origin.getOrElse(location))
            }
          }
          case _ => BadRequest("400 Bad Request: Invalid form data.")
        }
      } recover {
        case ex: NotFoundException =>
          NotFound("404 Not found: " + ex.getMessage)
        case ex: Throwable =>
          InternalServerError("503 Internal server error: " + ex.getMessage)
      }
    }
  }

  def logout = Action.async { implicit request: Request[AnyContent] =>
    implicit val ec = jdbcExecutionContext
    Future {
      request.session.get("api_key").map(unregisterKey)
      Redirect("/").withNewSession
    }
  }

}