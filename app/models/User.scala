package eu.seitzal.opentab.models

import eu.seitzal.opentab._
import shortcuts._
import upickle.{default => json}

import play.api.db.Database

case class User (
  id: Int,
  username: String,
  isAdmin: Boolean) {}

object User {

  implicit val rw: json.ReadWriter[User] = json.macroRW

  def apply(id: Int)(implicit database: Database): User = {
    val connection = database.getConnection()
    val queryText = "SELECT * FROM users WHERE id = ?"
    val query = connection.prepareStatement(queryText)
    query.setInt(1, id)
    val queryResult = query.executeQuery()
    connection.close()
    if (queryResult.next()) {
      val id = queryResult.getInt("id")
      val name = queryResult.getString("username")
      val isAdmin = queryResult.getBoolean("admin")
      User(id, name, isAdmin)
    } else {
      throw new NotFoundException("user", "ID", id.toString)
    }
  }

}