package eu.seitzal.opentab.models

import eu.seitzal.opentab._
import shortcuts._
import upickle.{default => json}

import play.api.db.Database

case class Team (
  id: Int,
  tabid: Int,
  name: String,
  delegation: String,
  status: Int,
  active: Boolean) {

  def delete()(implicit database: Database): Unit = {
    val connection = database.getConnection()
    val queryText = "DELETE FROM speakers WHERE teamid = ?"
    val query = connection.prepareStatement(queryText)
    query.setInt(1, id)
    query.executeUpdate()
    val queryText2 = "DELETE FROM teams WHERE id = ?"
    val query2 = connection.prepareStatement(queryText2)
    query2.setInt(1, id)
    query2.executeUpdate()
    connection.close()
  }

  def updateName(newName: String)(implicit database: Database): Team = {
    val connection = database.getConnection()
    val queryText = "UPDATE teams SET name = ? WHERE id = ?"
    val query = connection.prepareStatement(queryText)
    query.setString(1, newName)
    query.setInt(2, id)
    query.executeUpdate()
    connection.close()
    Team(id, tabid, newName, delegation, status, active)
  }

  def updateDelegation(newDeleg: String)(implicit database: Database): Team = {
    val connection = database.getConnection()
    val queryText = "UPDATE teams SET delegation = ? WHERE id = ?"
    val query = connection.prepareStatement(queryText)
    query.setString(1, newDeleg)
    query.setInt(2, id)
    query.executeUpdate()
    connection.close()
    Team(id, tabid, name, newDeleg, status, active)
  }

  def updateStatus(newStatus: Int)(implicit database: Database): Team = {
    val connection = database.getConnection()
    val queryText = "UPDATE teams SET langstatus = ? WHERE id = ?"
    val query = connection.prepareStatement(queryText)
    query.setInt(1, newStatus)
    query.setInt(2, id)
    query.executeUpdate()
    connection.close()
    Team(id, tabid, name, delegation, newStatus, active)
  }

  def update(
    newName: Option[String],
    newDeleg: Option[String],
    newStatus: Option[Int])(implicit database: Database): Team =
    newName match {
      case Some(n) => updateName(n).update(None, newDeleg, newStatus)
      case None => newDeleg match {
        case Some(d) => updateDelegation(d).update(None, None, newStatus)
        case None => newStatus match {
          case Some(s) => updateStatus(s)
          case None => this
        }
      }
    }

  def toggleActive(isActive: Boolean = !active)(implicit database: Database): Team = {
    val connection = database.getConnection()
    val queryText = "UPDATE teams SET active = ? WHERE id = ?"
    val query = connection.prepareStatement(queryText)
    query.setBoolean(1, isActive)
    query.setInt(2, id)
    query.executeUpdate()
    connection.close()
    Team(id, tabid, name, delegation, status, isActive)
  }

  def tab(implicit database: Database) = Tab(tabid)

  def speakers(implicit database: Database) = Speaker.getAllOnTeam(id)

}

object Team {

  implicit val rw: json.ReadWriter[Team] = json.macroRW

  def apply(id: Int)(implicit database: Database): Team = {
    val connection = database.getConnection()
    val queryText = "SELECT * FROM teams WHERE id = ?"
    val query = connection.prepareStatement(queryText)
    query.setInt(1, id)
    val queryResult = query.executeQuery()
    connection.close()
    if (queryResult.next()) {
      val id = queryResult.getInt("id")
      val tabid = queryResult.getInt("tabid")
      val name = queryResult.getString("name")
      val delegation = queryResult.getString("delegation")
      val status = queryResult.getInt("langstatus")
      val isActive = queryResult.getBoolean("active")
      Team(id, tabid, name, delegation, status, isActive)
    } else {
      throw new NotFoundException("team", "ID", id.toString)
    }
  }

  def getAll(tabid: Int)(implicit database: Database): List[Team] = {
    val connection = database.getConnection()
    val queryText = "SELECT * FROM teams WHERE tabid = ?"
    val query = connection.prepareStatement(queryText)
    query.setInt(1, tabid)
    val queryResult = query.executeQuery()
    connection.close()
    def iter(teams: List[Team]) : List[Team] =
      if (queryResult.next()) {
        val id = queryResult.getInt("id")
      val tabid = queryResult.getInt("tabid")
      val name = queryResult.getString("name")
      val delegation = queryResult.getString("delegation")
      val status = queryResult.getInt("langstatus")
      val isActive = queryResult.getBoolean("active")
      iter(Team(id, tabid, name, delegation, status, isActive) :: teams)
      } else teams.reverse
    iter(Nil)
  }

  def create(
    tabid: Int,
    name: String,
    delegation: String,
    status: Int)(implicit database: Database) : Team = {

    val connection = database.getConnection()
    val queryText = "SELECT * FROM teams WHERE tabid = ? AND name = ?"
    val query = connection.prepareStatement(queryText)
    query.setInt(1, tabid)
    query.setString(2, name)
    val queryResult = query.executeQuery()
    if (queryResult.next()) {
      connection.close()
      throw new ExistsAlreadyException("team", "name", name)
    } else {
      val queryText = 
        "INSERT INTO teams " +
        "(tabid, name, delegation, langstatus, active)" +
        "VALUES (?, ?, ?, ?, 1)" 
      val query = connection.prepareStatement(queryText)
      query.setInt(1, tabid)
      query.setString(2, name)
      query.setString(3, delegation)
      query.setInt(4, status)
      query.executeUpdate()
      val query2Text = "SELECT * FROM teams WHERE tabid = ? AND name = ?"
      val query2 = connection.prepareStatement(query2Text)
      query2.setInt(1, tabid)
      query2.setString(2, name)
      val queryResult = query2.executeQuery()
      connection.close()
      if (queryResult.next()) {
        val id = queryResult.getInt("id")
        val tabid = queryResult.getInt("tabid")
        val name = queryResult.getString("name")
        val delegation = queryResult.getString("delegation")
        val status = queryResult.getInt("langstatus")
        val isActive = queryResult.getBoolean("active")
        Team(id, tabid, name, delegation, status, isActive)
      } else {
        throw new Throwable(
          "Error during team creation. Most likely a database failure")
      }
    }
  }

}