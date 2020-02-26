package opentab.controllers

import opentab._
import shortcuts._
import models._
import auth._

import upickle.{default => json}

import javax.inject._
import play.api._
import play.api.mvc._
import play.api.db.Database
import akka.actor.ActorSystem
import scala.concurrent.{Future, ExecutionContext}

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

  def signIn = Action.async {
    implicit request: Request[AnyContent] => {
      implicit val ec = jdbcExecutionContext
      Future {
        request.body.asJson.map(c => json.read[Credentials](c.toString())) match {
          case (Some(credentials)) => {
            verifyUser(credentials.username, credentials.password) match {
              case Some(userid) => {
                val key = Keygen.newTempKey
                registerKey(key, userid)
                Ok(json.write((key, verifyKey(key)))).as("application/json")
              }
              case None => Unauthorized("Invalid username or password.")
            }
          }
          case _ => BadRequest("Invalid form data.")
        }
      } recover {
        case ex: NotFoundException =>
          Unauthorized("Invalid username or password.") // Prevent username checking
        case ex: Throwable =>
          InternalServerError(ex.getMessage)
      }
    }
  }

  def signOut = Action.async { implicit request: Request[AnyContent] =>
    implicit val ec = jdbcExecutionContext
    Future {
      request.headers.get("Authorization") match {
        case Some(key) => {
          unregisterKey(request.headers.get("Authorization").get)
          NoContent
        }
        case None => BadRequest("No key transmitted.")
      }
    } recover {
      case ex: NotFoundException =>
        NotFound(ex.getMessage)
      case ex: Throwable =>
        InternalServerError(ex.getMessage)
    }
  }

  def remoteVerifyKey = Action.async { implicit request: Request[AnyContent] =>
    Future {
      request.headers.get("Authorization").map(verifyKey) match {
        case Some(keyData) => Ok(json.write(keyData)).as("application/json")
        case None => BadRequest("No API key found in authorization header.")
      }
    } recover {
        case ex: Throwable => InternalServerError(ex.getMessage)
    }
  }

}