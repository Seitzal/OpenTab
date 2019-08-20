package eu.seitzal.opentab.models

import eu.seitzal.opentab._
import shortcuts._
import upickle.{default => json}

import play.api.db.Database

case class Speaker(
  id: Int,
  tabid: Int,
  teamid: Int,
  firstName: String,
  lastName: String,
  status: Int) {

  def delete()(implicit database: Database): Unit = {
    val connection = database.getConnection()
    val queryText = "DELETE FROM speakers WHERE id = ?"
    val query = connection.prepareStatement(queryText)
    query.setInt(1, id)
    query.executeUpdate()
    connection.close()
  }

  def updateFirstName(newName: String)(implicit database: Database): Speaker = {
    val connection = database.getConnection()
    val queryText = "UPDATE speakers SET firstname = ? WHERE id = ?"
    val query = connection.prepareStatement(queryText)
    query.setString(1, newName)
    query.setInt(2, id)
    query.executeUpdate()
    connection.close()
    Speaker(id, tabid, teamid, newName, lastName, status)
  }

  def updateLastName(newName: String)(implicit database: Database): Speaker = {
    val connection = database.getConnection()
    val queryText = "UPDATE speakers SET lastname = ? WHERE id = ?"
    val query = connection.prepareStatement(queryText)
    query.setString(1, newName)
    query.setInt(2, id)
    query.executeUpdate()
    connection.close()
    Speaker(id, tabid, teamid, firstName, newName, status)
  }

  def updateStatus(newStatus: Int)(implicit database: Database): Speaker = {
    val connection = database.getConnection()
    val queryText = "UPDATE speakers SET langstatus = ? WHERE id = ?"
    val query = connection.prepareStatement(queryText)
    query.setInt(1, newStatus)
    query.setInt(2, id)
    query.executeUpdate()
    connection.close()
    Speaker(id, tabid, teamid, firstName, lastName, newStatus)
  }

  def update(
    newFirstName: Option[String],
    newLastName: Option[String],
    newStatus: Option[Int])(implicit database: Database): Speaker =
    newFirstName match {
      case Some(fn) => updateFirstName(fn).update(None, newLastName, newStatus)
      case None => newLastName match {
        case Some(ln) => updateLastName(ln).update(None, None, newStatus)
        case None => newStatus match {
          case Some(s) => updateStatus(s)
          case None => this
        }
      }
    }
  
  def tab(implicit database: Database) = Tab(tabid)

  def team(implicit database: Database) = Team(teamid)

  def externalRepresentation(implicit database: Database) = {
    val team = this.team
    SpeakerExternalRepresentation (
      id,
      tabid,
      teamid,
      firstName,
      lastName,
      team.name,
      team.delegation,
      status)
  }

}

object Speaker {

  implicit val rw: json.ReadWriter[Speaker] = json.macroRW

  def apply(id: Int)(implicit database: Database): Speaker = {
    val connection = database.getConnection()
    val queryText = "SELECT * FROM speakers WHERE id = ?"
    val query = connection.prepareStatement(queryText)
    query.setInt(1, id)
    val queryResult = query.executeQuery()
    connection.close()
    if (queryResult.next()) {
      val id = queryResult.getInt("id")
      val tabid = queryResult.getInt("tabid")
      val teamid = queryResult.getInt("teamid")
      val firstName = queryResult.getString("firstname")
      val lastName = queryResult.getString("lastname")
      val status = queryResult.getInt("langstatus")
      Speaker(id, tabid, teamid, firstName, lastName, status)
    } else {
      throw new NotFoundException("speaker", "ID", id.toString)
    }
  }

  def getAll(tabid: Int)(implicit database: Database): List[Speaker] = {
    val connection = database.getConnection()
    val queryText = "SELECT * FROM speakers WHERE tabid = ?"
    val query = connection.prepareStatement(queryText)
    query.setInt(1, tabid)
    val queryResult = query.executeQuery()
    connection.close()
    def iter(speakers: List[Speaker]) : List[Speaker] =
      if (queryResult.next()) {
      val id = queryResult.getInt("id")
      val tabid = queryResult.getInt("tabid")
      val teamid = queryResult.getInt("teamid")
      val firstName = queryResult.getString("firstname")
      val lastName = queryResult.getString("lastname")
      val status = queryResult.getInt("langstatus")
      iter(Speaker(id, tabid, teamid, firstName, lastName, status) :: speakers)
      } else speakers.reverse
    iter(Nil)
  }

  def getAllOnTeam(teamid: Int)(implicit database: Database): List[Speaker] = {
    val connection = database.getConnection()
    val queryText = "SELECT * FROM speakers WHERE teamid = ?"
    val query = connection.prepareStatement(queryText)
    query.setInt(1, teamid)
    val queryResult = query.executeQuery()
    connection.close()
    def iter(speakers: List[Speaker]) : List[Speaker] =
      if (queryResult.next()) {
      val id = queryResult.getInt("id")
      val tabid = queryResult.getInt("tabid")
      val teamid = queryResult.getInt("teamid")
      val firstName = queryResult.getString("firstname")
      val lastName = queryResult.getString("lastname")
      val status = queryResult.getInt("langstatus")
      iter(Speaker(id, tabid, teamid, firstName, lastName, status) :: speakers)
      } else speakers.reverse
    iter(Nil)
  }

  def create(
    teamid: Int,
    firstName: String,
    lastName: String,
    status: Int)(implicit database: Database) : Speaker = {

    val tabid = Team(teamid).tabid
    val connection = database.getConnection()
    val queryText = "SELECT * FROM speakers WHERE tabid = ? AND firstname = ? AND lastname = ?"
    val query = connection.prepareStatement(queryText)
    query.setInt(1, tabid)
    query.setString(2, firstName)
    query.setString(3, lastName)
    val queryResult = query.executeQuery()
    if (queryResult.next()) {
      connection.close()
      throw new ExistsAlreadyException("speaker", "name", firstName + " " + lastName)
    } else {
      val queryText = 
        "INSERT INTO speakers " +
        "(tabid, teamid, firstName, lastName, langstatus)" +
        "VALUES (?, ?, ?, ?, ?)" 
      val query = connection.prepareStatement(queryText)
      query.setInt(1, tabid)
      query.setInt(2, teamid)
      query.setString(3, firstName)
      query.setString(4, lastName)
      query.setInt(5, status)
      query.executeUpdate()
      val query2Text = "SELECT * FROM speakers WHERE tabid = ? AND firstname = ? AND lastname = ?"
      val query2 = connection.prepareStatement(query2Text)
      query2.setInt(1, tabid)
      query2.setString(2, firstName)
      query2.setString(3, lastName)
      val query2Result = query2.executeQuery()
      connection.close()
      if (query2Result.next()) {
        val id = query2Result.getInt("id")
        val tabid = query2Result.getInt("tabid")
        val teamid = query2Result.getInt("teamid")
        val firstName = query2Result.getString("firstname")
        val lastName = query2Result.getString("lastname")
        val status = query2Result.getInt("langstatus")
        Speaker(id, tabid, teamid, firstName, lastName, status)
      } else {
        throw new Throwable(
          "Error during speaker creation. Most likely a database failure")
      }
    }
  }

}

case class SpeakerExternalRepresentation(
  id: Int,
  tabid: Int,
  teamid: Int,
  firstName: String,
  lastName: String,
  team: String,
  delegation: String,
  status: Int) {}

object SpeakerExternalRepresentation {
  implicit val rw: json.ReadWriter[SpeakerExternalRepresentation] = json.macroRW
}