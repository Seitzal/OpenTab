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
import org.mindrot.jbcrypt.BCrypt
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

  def verifyUser(username: String, password: String): Option[Int] = {
    val connection = db.getConnection()
    val queryText = "SELECT * FROM users WHERE username = ?"
    val query = connection.prepareStatement(queryText)
    query.setString(1, username)
    val queryResult = query.executeQuery()
    connection.close()
    if (queryResult.next()) {
      val retrievedHash = queryResult.getString("passwd")
      if (BCrypt.checkpw(password, retrievedHash))
        Option(queryResult.getInt("id"))
      else None
    } else throw new NotFoundException("user", "name", username)
  }

  def registerKey(key: String, userid: Int): Unit = {
    val connection = db.getConnection()
    val queryText = 
      "INSERT INTO api_keys (val, expires, userid) VALUES (?, ?, ?)"
    val query = connection.prepareStatement(queryText)
    query.setString(1, key)
    query.setLong(2, timestamp() + config.get[Int]("api.keyLife"))
    query.setInt(3, userid)
    query.executeUpdate()
    connection.close()
  }

  def unregisterKey(key: String): Unit = {
    val connection = db.getConnection()
    val queryText = 
      "DELETE FROM api_keys WHERE val = ?"
    val query = connection.prepareStatement(queryText)
    query.setString(1, key)
    query.executeUpdate()
    connection.close()
  }

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
                  Redirect(location + url).withSession(session: _*)
                case None =>
                  Redirect(location).withSession(session: _*)
                }
              }
              case None => Redirect("login?origin=" + origin)
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