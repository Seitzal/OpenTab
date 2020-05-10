package opentab

import doobie._
import doobie.implicits._
import upickle.default._
import eu.seitzal.http4s_upickle._
import cats.effect.IO
import cats.implicits._
import org.mindrot.jbcrypt.BCrypt

case class User (
  id: Int,
  name: String,
  email: String,
  isAdmin: Boolean
) {

  def checkPassword(password: String): ConnectionIO[Boolean] =
    sql"SELECT password FROM users WHERE id = $id"
      .query[String]
      .unique
      .map(BCrypt.checkpw(password, _))

  def delete: ConnectionIO[Unit] =
    sql"DELETE FROM users WHERE id = $id"
      .update
      .run
      .map(_ => {})

  def update(
      newPassword: Option[String] = None, 
      newEmail: Option[String] = None,
      newIsAdmin: Option[Boolean] = None): ConnectionIO[User] = {
    val hashedNewPassword = newPassword.map(BCrypt.hashpw(_, BCrypt.gensalt()))
    val queryString =
      fr"UPDATE users SET " ++ List(
        hashedNewPassword match {
          case Some(v) => List(fr"password = $v")
          case None => Nil
        },
        newEmail match {
          case Some(v) => List(fr"email = $v")
          case None => Nil
        },
        newIsAdmin match {
          case Some(v) => List(fr"isadmin = $v")
          case None => Nil
        }
      ).flatten.intercalate(fr",") ++
      fr"WHERE name = $name"
    queryString
      .update
      .withUniqueGeneratedKeys[User]("id", "name", "email", "isadmin")
  }
}

object User {

  def apply(id: Int): ConnectionIO[User] =
    sql"SELECT (id, name, email, isadmin) FROM users WHERE id = $id"
      .query[User]
      .unique

  def getAll: ConnectionIO[List[User]] =
    sql"SELECT (id, name, email, isadmin) FROM users"
      .query[User]
      .to[List]

  def create(
      name: String, 
      password: String, 
      email: String, 
      isAdmin: Boolean): ConnectionIO[User] = {
    val hashedPassword = BCrypt.hashpw(password, BCrypt.gensalt())
    sql"""INSERT INTO users (name, password, email, isadmin) 
          VALUES ($name, $hashedPassword, $email, $isAdmin)"""
      .update
      .withUniqueGeneratedKeys[User]("id", "name", "email", "isadmin")
    }

  implicit val rw = macroRW[User]
  implicit val ee = new UPickleEntityEncoder[IO, User]
  implicit val ed = new UPickleEntityDecoder[IO, User]
}
