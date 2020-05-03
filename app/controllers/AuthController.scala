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
  implicit def actionBuilder = Action
  implicit def defaultParser = parse.anyContent

  val jdbcExecutionContext =
    actorSystem.dispatchers.lookup("jdbc-execution-context")

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

  def remoteVerifyToken = authAction((user, request) => {
    Ok(json.write(user)).as("application/json")
  })

}