package eu.seitzal.opentab.controllers

import eu.seitzal.opentab._
import shortcuts._
import models.permissions._
import upickle.{default => json}

import javax.inject._
import play.api._
import play.api.mvc._
import play.api.db.Database
import akka.actor.ActorSystem
import scala.concurrent.{Future, ExecutionContext}
import play.api.libs.json._
import play.api.libs.json.Reads._
import play.api.libs.functional.syntax._

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

  val jdbcExecutionContext = 
    actorSystem.dispatchers.lookup("jdbc-execution-context")
  
  case class KeyData(
    found: Boolean,
    temporary: Boolean,
    expired: Boolean,
    expires: Long,
    userid: Int)
  
  object KeyData {
    implicit val rw: json.ReadWriter[KeyData] = json.macroRW
  }

  def verifyKey(key: String): KeyData = {
    val connection = database.getConnection()
    val queryText = "SELECT * FROM api_keys WHERE val = ?"
    val query = connection.prepareStatement(queryText)
    query.setString(1, key)
    val queryResult = query.executeQuery()
    connection.close()
    if (queryResult.next()) {
      val expires = queryResult.getLong("expires")
      val temporary = expires != 0
      val expired = temporary && expires < timestamp()
      val userid  = queryResult.getInt("userid")
      KeyData(true, temporary, expired, expires, userid) 
    } else {
      KeyData(false, false, false, 0L, 0)
    }
  }

  def getAllTabs = Action.async { implicit request: Request[AnyContent] =>
    implicit val ec = jdbcExecutionContext
    Future {
      val auth = request.headers.get("Authorization").map(verifyKey)
      val tabs = models.Tab.getAll(database)
      auth match {
        case Some(keyData) => {
          if (!keyData.found)
            Unauthorized("Invalid API key")
          else if (keyData.expired)
            Unauthorized("API key has expired")
          else {
            val visibleTabs = 
              tabs.filter(tab => userCanSeeTab(keyData.userid, tab))
            Ok(json.write(visibleTabs)).as("application/json")
          }
        }
        case None => {
          val visibleTabs = tabs.filter(_.isPublic)
          Ok(json.write(visibleTabs)).as("application/json")
        }
      }
    } recover {
      case ex: Throwable => InternalServerError(ex.getMessage)
    }
  }

  def remoteVerifyKey = Action.async { implicit request: Request[AnyContent] =>
    implicit val ec = jdbcExecutionContext
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