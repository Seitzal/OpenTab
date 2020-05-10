package opentab

import doobie._
import doobie.implicits._
import upickle.default._
import eu.seitzal.http4s_upickle._
import cats.effect.IO
import cats.implicits._

case class User (
  name: String,
  password: String,
  email: String,
  isAdmin: Boolean
) {

  def delete: ConnectionIO[Unit] =
    sql"DELETE FROM users WHERE name = $name"
      .update
      .run
      .map(_ => {})

  def update(
      newPassword: Option[String] = None, 
      newEmail: Option[String] = None,
      newIsAdmin: Option[Boolean] = None): ConnectionIO[User] = {
    val queryString =
      fr"UPDATE users SET " ++ List(
        newPassword match {
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
      .withUniqueGeneratedKeys[User]("name", "password", "email", "isadmin")
  }
}

object User {

  def apply(name: String): ConnectionIO[User] =
    sql"SELECT * FROM users WHERE name = $name"
      .query[User]
      .unique

  def getAll: ConnectionIO[List[User]] =
    sql"SELECT * FROM users"
      .query[User]
      .to[List]

  def create(
      name: String, 
      password: String, 
      email: String, 
      isAdmin: Boolean): ConnectionIO[User] =
    sql"INSERT INTO users VALUES ($name, $password, $email, $isAdmin)"
      .update
      .withUniqueGeneratedKeys[User]("name", "password", "email", "isadmin")
      
  implicit val rw = macroRW[User]
  implicit val ee = new UPickleEntityEncoder[IO, User]
  implicit val ed = new UPickleEntityDecoder[IO, User]
}
