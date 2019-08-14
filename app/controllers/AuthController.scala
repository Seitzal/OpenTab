package eu.seitzal.opentab.controllers

import javax.inject._

import play.api._
import play.api.mvc._

import play.api.db.Database

import akka.actor.ActorSystem
import scala.concurrent.{Future, ExecutionContext}

import org.mindrot.jbcrypt.BCrypt

import eu.seitzal.opentab.exceptions._

@Singleton
class AuthController @Inject()
    (actorSystem: ActorSystem, db: Database, cc: ControllerComponents)
    (implicit ec: ExecutionContext)
    extends AbstractController(cc) {

  val jdbcExecutionContext = actorSystem.dispatchers.lookup("jdbc-execution-context")

  def verifyUser(username: String, password: String) = {
    val connection = db.getConnection()
    val stmt = connection.prepareStatement("SELECT * FROM users WHERE username = ?")
    stmt.setString(1, username)
    val rs = stmt.executeQuery()
    if (rs.next()) {
      val retrievedHash = rs.getString("passwd")
      BCrypt.checkpw(password, retrievedHash)
    } else {
      throw new UserNotFoundException(username)
    }
  }

  def processLogin = Action.async(parse.formUrlEncoded) {
    implicit request: Request[Map[String,Seq[String]]] => {
      implicit val ec = jdbcExecutionContext
      Future {
        val formData = request.body
        (formData.get("username"), formData.get("password")) match {
          case (Some(username), Some(password)) => {
            val success = verifyUser(username(0), password(0))
            if (success) Ok("Login data correct.") else Unauthorized("Login data incorrect.")
          }
          case _ => BadRequest("400 Bad Request: Invalid form data.")
        }
      }.recover {
        case ex: UserNotFoundException  => NotFound("404 Not found: " + ex.getMessage)
        case ex: Throwable              => InternalServerError("503 Internal server error: " + ex.getMessage)
      }
    }
  }

}