package opentab

import doobie._
import doobie.implicits._
import cats.effect._
import cats.implicits._
import org.mindrot.jbcrypt.BCrypt
import upickle.default.macroRW
import eu.seitzal.http4s_upickle.UPickleEntityCodec

case class User (
  id: Int,
  name: String,
  email: String,
  isAdmin: Boolean
) {

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
      (implicit xa: Xa): IO[User] = {
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
      .transact(xa)
  }
}

object User {

  def apply(id: Int)(implicit xa: Xa): IO[User] =
    sql"SELECT id, name, email, isadmin FROM users WHERE id = $id"
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

  implicit val rw = macroRW[User]
  implicit val ec = new UPickleEntityCodec[IO, User]
}
