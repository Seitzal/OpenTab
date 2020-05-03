package opentab

import opentab._
import opentab.models._
import opentab.shortcuts._
import play.api.Configuration
import play.api.db.Database
import play.api.mvc._
import play.api.mvc.Results._
import upickle.{default => json}
import pdi.jwt._
import org.mindrot.jbcrypt.BCrypt
import scala.concurrent.{Future, ExecutionContext}
import java.time.{Instant, Duration}

package object auth {

  case class Credentials(username: String, password: String)

  object Credentials {
    implicit val rw: json.ReadWriter[Credentials] = json.macroRW
  }

  case class KeyData(
    found: Boolean,
    temporary: Boolean,
    expired: Boolean,
    expires: Long,
    userid: Int)
  
  object KeyData {
    implicit val rw: json.ReadWriter[KeyData] = json.macroRW
  }

  private case class PermissionsEntry(
    view: Boolean,
    results : Boolean,
    setup: Boolean,
    own: Boolean)

  def verifyUser(username: String, password: String)
                (implicit db: Database): Option[Int] = {
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

  def issueToken(user: User)(implicit config: Configuration): String =
    JwtUpickle.encode(
      claim = JwtClaim(
        content = json.write(user),
        issuedAt = Some(Instant.now.getEpochSecond),
        expiration = Some(
          Instant.now
            .plus(Duration.ofSeconds(config.get[Int]("api.keyLife")))
            .getEpochSecond
        )
      ),
      key = config.get[String]("play.http.secret.key"),
      algorithm = JwtAlgorithm.HS256
    )

  def registerKey(key: String, userid: Int)
                 (implicit db: Database, config: Configuration): Unit = {
    val connection = db.getConnection()
    val queryText =
      "DELETE FROM api_keys WHERE userid = ? AND expires != 0"
    val query = connection.prepareStatement(queryText)
    query.setInt(1, userid)
    query.executeUpdate()
    val query2Text = 
      "INSERT INTO api_keys (val, expires, userid) VALUES (?, ?, ?)"
    val query2 = connection.prepareStatement(query2Text)
    query2.setString(1, key)
    query2.setLong(2, timestamp() + config.get[Int]("api.keyLife"))
    query2.setInt(3, userid)
    query2.executeUpdate()
    connection.close()
  }

  def unregisterKey(key: String)(implicit db: Database): Unit = {
    val connection = db.getConnection()
    val queryText = 
      "DELETE FROM api_keys WHERE val = ?"
    val query = connection.prepareStatement(queryText)
    query.setString(1, key)
    query.executeUpdate()
    connection.close()
  }

  def verifyKey(key: String)(implicit db: Database): KeyData = {
    val connection = db.getConnection()
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

  private def readPermissionsEntry(userid: Int, tabid: Int, attempt: Int = 1)
                       (implicit database: Database) : PermissionsEntry = {
    if (attempt > 4) {
      throw new Throwable("Internal authorization error.")
    }
    val connection = database.getConnection()
    val queryText = "SELECT * FROM permissions WHERE userid = ? AND tabid = ?"
    val query = connection.prepareStatement(queryText)
    query.setInt(1, userid)
    query.setInt(2, tabid)
    val queryResult = query.executeQuery()
    connection.close()
    if (queryResult.next()) {
      val view    = queryResult.getBoolean("p_view")
      val results = queryResult.getBoolean("p_results")
      val setup   = queryResult.getBoolean("p_setup")
      val own     = queryResult.getBoolean("p_own")
      PermissionsEntry(view, results, setup, own)
    } else {
      val connection = database.getConnection()
      val queryText =
        "INSERT INTO permissions " +
        "(userid, tabid, p_view, p_results, p_setup, p_own) " + {
          if (Tab(tabid).owner == userid || User(userid).isAdmin)
            "VALUES (?, ?, 1, 1, 1, 1)"
          else
            "VALUES (?, ?, 0, 0, 0, 0)"
        }
      val query = connection.prepareStatement(queryText)
      query.setInt(1, userid)
      query.setInt(2, tabid)
      query.executeUpdate()
      connection.close()
      readPermissionsEntry(userid, tabid, attempt + 1)
    }
  }

  def userCanSeeTab(userid: Int, tabid: Int)
                   (implicit database: Database) : Boolean =
    readPermissionsEntry(userid, tabid).view || Tab(tabid).isPublic

  def userCanSeeTab(userid: Int, tab: Tab)
                   (implicit database: Database) : Boolean =
    readPermissionsEntry(userid, tab.id).view || tab.isPublic

  def userCanSeeTab(user: User, tabid: Int)
                   (implicit database: Database) : Boolean =
    readPermissionsEntry(user.id, tabid).view || Tab(tabid).isPublic

  def userCanSeeTab(user: User, tab: Tab)
                   (implicit database: Database) : Boolean =
    readPermissionsEntry(user.id, tab.id).view || tab.isPublic

  def userCanSetupTab(userid: Int, tabid: Int)
                     (implicit database: Database) : Boolean =
    readPermissionsEntry(userid, tabid).setup

  def userCanSetupTab(userid: Int, tab: Tab)
                     (implicit database: Database) : Boolean =
    readPermissionsEntry(userid, tab.id).setup

  def userCanSetupTab(user: User, tabid: Int)
                     (implicit database: Database) : Boolean =
    readPermissionsEntry(user.id, tabid).setup

  def userCanSetupTab(user: User, tab: Tab)
                     (implicit database: Database) : Boolean =
    readPermissionsEntry(user.id, tab.id).setup

  def authAction[A](
      parser: BodyParser[A], 
      block: (User, Request[A]) => Result)(
      implicit executionContext: ExecutionContext,
      database: Database,
      actionBuilder: ActionBuilder[Request, AnyContent]): Action[A] =
    actionBuilder.async(parser) {
      implicit request: Request[A] => Future {
        val auth = request.headers.get("Authorization").map(verifyKey)
        auth match {
          case Some(keyData) => 
            if (!keyData.found)
              Unauthorized("Invalid API key")
            else if (keyData.expired)
              Unauthorized("API key has expired")
            else
              block(User(keyData.userid), request)
          case None =>
              Unauthorized("Authorization Required")
        }
      } recover {
        case ex: NotFoundException => NotFound(ex.getMessage)
        case ex: Throwable => InternalServerError(ex.getMessage)
      }
    }

  def authAction(
      block: (User, Request[AnyContent]) => Result)(
      implicit executionContext: ExecutionContext,
      database: Database,
      actionBuilder: ActionBuilder[Request, AnyContent],
      defaultParser: BodyParser[AnyContent]) =
    authAction[AnyContent](defaultParser, block)

  def optionalAuthAction[A](
      parser: BodyParser[A], 
      block: (Option[User], Request[A]) => Result)(
      implicit executionContext: ExecutionContext,
      database: Database,
      actionBuilder: ActionBuilder[Request, AnyContent]): Action[A] =
    actionBuilder.async(parser) {
      implicit request: Request[A] => Future {
        val auth = request.headers.get("Authorization").map(verifyKey)
        auth match {
          case Some(keyData) => 
            if (!keyData.found)
              Unauthorized("Invalid API key")
            else if (keyData.expired)
              Unauthorized("API key has expired")
            else
              block(Some(User(keyData.userid)), request)
          case None =>
              block(None, request)
        }
      } recover {
        case ex: NotFoundException => NotFound(ex.getMessage)
        case ex: Throwable => InternalServerError(ex.getMessage)
      }
    }

  def optionalAuthAction(
      block: (Option[User], Request[AnyContent]) => Result)(
      implicit executionContext: ExecutionContext,
      database: Database,
      actionBuilder: ActionBuilder[Request, AnyContent],
      defaultParser: BodyParser[AnyContent]) =
    optionalAuthAction[AnyContent](defaultParser, block)

  val PermissionDenied = Forbidden("Permission denied.")
  val AuthorizationRequired = Unauthorized("Authorization required.")

}
