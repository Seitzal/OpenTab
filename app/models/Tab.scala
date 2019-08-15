package eu.seitzal.opentab.models

import eu.seitzal.opentab._
import shortcuts._
import upickle.{default => json}

import play.api.db.Database

case class Tab (
  id: Int,
  name: String,
  isPublic: Boolean) {}

object Tab {

  implicit val rw: json.ReadWriter[Tab] = json.macroRW

  def apply(id: Int)(implicit database: Database): Tab = {
    val connection = database.getConnection()
    val queryText = "SELECT * FROM tabs WHERE id = ?"
    val query = connection.prepareStatement(queryText)
    query.setInt(1, id)
    val queryResult = query.executeQuery()
    if (queryResult.next()) {
      val id = queryResult.getInt("id")
      val name = queryResult.getString("name")
      val isPublic = queryResult.getBoolean("public")
      Tab(id, name, isPublic)
    } else {
      throw new NotFoundException("tab", "ID", id.toString)
    }
  }

  def getAll(implicit database: Database): List[Tab] = {
    val connection = database.getConnection()
    val queryText = "SELECT * FROM tabs"
    val query = connection.prepareStatement(queryText)
    val queryResult = query.executeQuery()
    def iter(tabs: List[Tab]) : List[Tab] =
      if (queryResult.next()) {
        val id = queryResult.getInt("id")
        val name = queryResult.getString("name")
        val isPublic = queryResult.getBoolean("public")
        iter(Tab(id, name, isPublic) :: tabs)
      } else tabs.reverse
    iter(Nil)
  }

}