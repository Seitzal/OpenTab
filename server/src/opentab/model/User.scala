package opentab.model

import opentab._
import doobie._
import doobie.implicits._
import cats.effect._
import cats.implicits._
import pdi.jwt._
import upickle.default._
import org.mindrot.jbcrypt.BCrypt
import upickle.default.macroRW
import java.time.{Instant, Duration}

case class User (
  id: Int,
  name: String,
  email: String,
  isAdmin: Boolean
) {

  def issueToken(duration: Option[Int], secret: String): String =
    JwtUpickle.encode(
      claim = JwtClaim(
        content = write(this),
        issuedAt = Some(Instant.now.getEpochSecond),
        expiration = duration.map(seconds =>
          Instant.now
            .plus(Duration.ofSeconds(seconds))
            .getEpochSecond
        )
      ),
      key = secret,
      algorithm = JwtAlgorithm.HS256
    )

  def checkPassword(password: String)(implicit xa: Xa): IO[Boolean] =
    sql"SELECT password FROM users WHERE id = $id"
      .query[String]
      .unique
      .map(BCrypt.checkpw(password, _))
      .transact(xa)

  def delete(implicit xa: Xa): IO[Unit] =
    sql"DELETE FROM users WHERE id = $id"
      .update
      .run
      .map(_ => {})
      .transact(xa)

  def update(
      newPassword: Option[String] = None, 
      newEmail: Option[String] = None,
      newIsAdmin: Option[Boolean] = None)
      (implicit xa: Xa): IO[User] =
    updateSql("users")(
      updateFragment("password", 
        newPassword.map(BCrypt.hashpw(_, BCrypt.gensalt()))),
      updateFragment("email", newEmail),
      updateFragment("isadmin", newIsAdmin)
    )(fr"WHERE id = $id")
      .update
      .withUniqueGeneratedKeys[User]("id", "name", "email", "isadmin")
      .transact(xa)

}

object User {

  def apply(id: Int)(implicit xa: Xa): IO[User] =
    sql"SELECT id, name, email, isadmin FROM users WHERE id = $id"
      .query[User]
      .unique
      .transact(xa)

  def apply(name: String)(implicit xa: Xa): IO[User] =
    sql"SELECT id, name, email, isadmin FROM users WHERE name = $name"
      .query[User]
      .unique
      .transact(xa)

  def getAll(implicit xa: Xa): IO[List[User]] =
    sql"SELECT id, name, email, isadmin FROM users"
      .query[User]
      .to[List]
      .transact(xa)

  def create(
      name: String, 
      password: String, 
      email: String, 
      isAdmin: Boolean)
      (implicit xa: Xa): IO[User] = {
    val hashedPassword = BCrypt.hashpw(password, BCrypt.gensalt())
    sql"""INSERT INTO users (name, password, email, isadmin) 
          VALUES ($name, $hashedPassword, $email, $isAdmin)"""
      .update
      .withUniqueGeneratedKeys[User]("id", "name", "email", "isadmin")
      .transact(xa)
    }

  implicit val rw: ReadWriter[User] = macroRW[User]
}
