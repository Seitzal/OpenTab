package opentab.controllers

import opentab._
import opentab.shortcuts._
import opentab.models._
import opentab.auth._
import play.api._
import play.api.mvc._
import play.api.db.Database
import akka.actor.ActorSystem
import upickle.{default => json}
import scala.concurrent.{Future, ExecutionContext}
import javax.inject._
import java.util.Base64
import pdi.jwt.exceptions.JwtExpirationException
import pdi.jwt.exceptions.JwtValidationException
import pdi.jwt.exceptions.JwtLengthException

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

  def getToken(permanent: Boolean) = Action.async {
    implicit request: Request[AnyContent] => Future {
      if (permanent && !config.get[Boolean]("auth.allowPermanentTokens"))
        Forbidden("Server configuration disallows issuing permanent tokens.")
      else request.headers.get("Authorization") match {
        case Some(s"Basic $cred") =>
          new String(Base64.getDecoder().decode(cred.getBytes("UTF-8"))) match {
            case s"$username:$password" =>
              verifyUser(username, password) match {
                case Some(userid) => Ok(issueToken(User(userid), permanent))
                case None => Unauthorized("Invalid username or password.")
              }
            case _ => BadRequest("Invalid authorization header.")
          }
        case _ => BadRequest("Invalid authorization header.")
      }
    } (jdbcExecutionContext)
    .recover {
      case _: NotFoundException => Unauthorized("Invalid username or password.")
      case ex: Throwable => InternalServerError(ex.getMessage)
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

  def remoteVerifyKey = Action.async { 
    implicit request: Request[AnyContent] => Future {
      request.headers.get("Authorization").map(verifyKey) match {
        case Some(keyData) => Ok(json.write(keyData)).as("application/json")
        case None => BadRequest("No API key found in authorization header.")
      }
    } recover {
        case ex: Throwable => InternalServerError(ex.getMessage)
    }
  }

  def remoteVerifyToken = Action.async { 
    implicit request: Request[AnyContent] => Future {
      request.headers.get("Authorization") match {
        case Some(s"Bearer $token") => Ok(json.write(verifyToken(token).get))
        case _ => BadRequest("Authorization header missing or invalid.")
      }
    } recover {
      case _: JwtLengthException =>
        BadRequest("Invalid token.")
      case _: IllegalArgumentException =>
        BadRequest("Invalid token.")
      case _: JwtExpirationException =>
        Unauthorized("Token has expired.")
      case _: JwtValidationException =>
        Unauthorized("Token has been forged or compromised.")
      case _: upickle.core.AbortException =>
        Unauthorized("Token payload is invalid.")
      case ex: Throwable =>
        InternalServerError(ex.getClass.toString + ": " + ex.getMessage)
    }
  }

}