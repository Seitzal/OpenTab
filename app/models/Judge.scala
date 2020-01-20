package eu.seitzal.opentab.models

import eu.seitzal.opentab._
import shortcuts._
import upickle.{default => json}

import play.api.db.Database

case class Judge (
  id: Int,
  tabid: Int,
  firstName: String,
  lastName: String,
  rating: Int,
  active: Boolean) {

  def delete()(implicit database: Database): Unit = {
    val connection = database.getConnection()
    val queryText = "DELETE FROM judge_clashes WHERE judgeid = ?"
    val query = connection.prepareStatement(queryText)
    query.setInt(1, id)
    query.executeUpdate()
    val query2Text = "DELETE FROM judges WHERE id = ?"
    val query2 = connection.prepareStatement(query2Text)
    query2.setInt(1, id)
    query2.executeUpdate()
    connection.close()
  }

  def updateFirstName(newName: String)(implicit database: Database): Judge = {
    val connection = database.getConnection()
    val queryText = "UPDATE judges SET firstname = ? WHERE id = ?"
    val query = connection.prepareStatement(queryText)
    query.setString(1, newName)
    query.setInt(2, id)
    query.executeUpdate()
    connection.close()
    Judge(id, tabid, newName, lastName, rating, active)
  }

  def updateLastName(newName: String)(implicit database: Database): Judge = {
    val connection = database.getConnection()
    val queryText = "UPDATE judges SET lastname = ? WHERE id = ?"
    val query = connection.prepareStatement(queryText)
    query.setString(1, newName)
    query.setInt(2, id)
    query.executeUpdate()
    connection.close()
    Judge(id, tabid, firstName, newName, rating, active)
  }


  def updateRating(newRating: Int)(implicit database: Database): Judge = {
    val connection = database.getConnection()
    val queryText = "UPDATE judges SET rating = ? WHERE id = ?"
    val query = connection.prepareStatement(queryText)
    query.setInt(1, newRating)
    query.setInt(2, id)
    query.executeUpdate()
    connection.close()
    Judge(id, tabid, firstName, lastName, newRating, active)
  }

  def update(
    newFirstName: Option[String],
    newLastName: Option[String],
    newRating: Option[Int])(implicit database: Database): Judge =
    newFirstName match {
      case Some(fn) => updateFirstName(fn).update(None, newLastName, newRating)
      case None => newLastName match {
        case Some(ln) => updateLastName(ln).update(None, None, newRating)
        case None => newRating match {
          case Some(r) => updateRating(r)
          case None => this
        }
      }
    }

  def toggleActive(isActive: Boolean = !active)
                  (implicit database: Database): Judge = {
    val connection = database.getConnection()
    val queryText = "UPDATE judges SET active = ? WHERE id = ?"
    val query = connection.prepareStatement(queryText)
    query.setBoolean(1, isActive)
    query.setInt(2, id)
    query.executeUpdate()
    connection.close()
    Judge(id, tabid, firstName, lastName, rating, isActive)
  }

  def clashes(implicit database: Database): List[(Team, Int)] = {
    val connection = database.getConnection()
    val queryText = "SELECT * FROM judge_clashes WHERE judgeid = ?"
    val query = connection.prepareStatement(queryText)
    query.setInt(1, id)
    val queryResult = query.executeQuery()
    connection.close()
    def iter(clashes: List[(Team, Int)]) : List[(Team, Int)] =
      if (queryResult.next()) {
        val teamid = queryResult.getInt("teamid")
        val level = queryResult.getInt("level")
        val team = Team(teamid)
        iter((team, level) :: clashes)
      } else clashes.reverse
    iter(Nil)
  }

  def setClash(team: Team, level: Int)(implicit database: Database): Unit = {
    val connection = database.getConnection()
    val queryText = "SELECT * FROM judge_clashes WHERE judgeid = ? AND teamid = ?"
    val query = connection.prepareStatement(queryText)
    query.setInt(1, id)
    query.setInt(2, team.id)
    val queryResult = query.executeQuery()
    val query2Text = 
      if (queryResult.next())
        "UPDATE judge_clashes SET level = ? WHERE judgeid = ? AND teamid = ?"
      else
        "INSERT INTO judge_clashes (level, judgeid, teamid) VALUES (?, ?, ?)"
    val query2 = connection.prepareStatement(query2Text)
    query2.setInt(1, level)
    query2.setInt(2, id)
    query2.setInt(3, team.id)
    query2.executeUpdate()
    connection.close()
  }

  def unsetClash(team: Team)(implicit database: Database): Unit = {
    val connection = database.getConnection()
    val queryText = "DELETE FROM judge_clashes WHERE judgeid = ? AND teamid = ?"
    val query = connection.prepareStatement(queryText)
    query.setInt(1, id)
    query.setInt(2, team.id)
    query.executeUpdate()
    connection.close()
  }

  def clash(team: Team)(implicit database: Database): Int = {
    val connection = database.getConnection()
    val queryText = "SELECT * FROM judge_clashes WHERE judgeid = ? AND teamid = ?"
    val query = connection.prepareStatement(queryText)
    query.setInt(1, id)
    query.setInt(2, team.id)
    val queryResult = query.executeQuery()
    connection.close()
    if (queryResult.next())
      queryResult.getInt("level")
    else 0
  }

  def tab(implicit database: Database) = Tab(tabid)

}

object Judge {

  implicit val rw: json.ReadWriter[Judge] = json.macroRW

  def apply(id: Int)(implicit database: Database): Judge = {
    val connection = database.getConnection()
    val queryText = "SELECT * FROM judges WHERE id = ?"
    val query = connection.prepareStatement(queryText)
    query.setInt(1, id)
    val queryResult = query.executeQuery()
    connection.close()
    if (queryResult.next()) {
      val id = queryResult.getInt("id")
      val tabid = queryResult.getInt("tabid")
      val firstName = queryResult.getString("firstname")
      val lastName = queryResult.getString("lastname")
      val rating = queryResult.getInt("rating")
      val isActive = queryResult.getBoolean("active")
      Judge(id, tabid, firstName, lastName, rating, isActive)
    } else {
      throw new NotFoundException("judge", "ID", id.toString)
    }
  }

  def getAll(tabid: Int)(implicit database: Database): List[Judge] = {
    val connection = database.getConnection()
    val queryText = "SELECT * FROM judges WHERE tabid = ?"
    val query = connection.prepareStatement(queryText)
    query.setInt(1, tabid)
    val queryResult = query.executeQuery()
    connection.close()
    def iter(judges: List[Judge]) : List[Judge] =
      if (queryResult.next()) {
        val id = queryResult.getInt("id")
        val tabid = queryResult.getInt("tabid")
        val firstName = queryResult.getString("firstname")
        val lastName = queryResult.getString("lastname")
        val rating = queryResult.getInt("rating")
        val isActive = queryResult.getBoolean("active")
        iter(Judge(id, tabid, firstName, lastName, rating, isActive) :: judges)
      } else judges.reverse
    iter(Nil)
  }

  def create(
    tabid: Int,
    firstName: String,
    lastName: String,
    rating: Int)(implicit database: Database) : Judge = {

    val connection = database.getConnection()
    val queryText = "SELECT * FROM judges WHERE tabid = ? AND firstname = ? AND lastname = ?"
    val query = connection.prepareStatement(queryText)
    query.setInt(1, tabid)
    query.setString(2, firstName)
    query.setString(3, lastName)
    val queryResult = query.executeQuery()
    if (queryResult.next()) {
      connection.close()
      throw new ExistsAlreadyException("judge", "name", firstName + " " + lastName)
    } else {
      val queryText = 
        "INSERT INTO judges " +
        "(tabid, firstname, lastname, rating, active)" +
        "VALUES (?, ?, ?, ?, 1)" 
      val query = connection.prepareStatement(queryText)
      query.setInt(1, tabid)
      query.setString(2, firstName)
      query.setString(3, lastName)
      query.setInt(4, rating)
      query.executeUpdate()
      val query2Text = "SELECT * FROM judges WHERE tabid = ? AND firstname = ? AND lastname = ?"
      val query2 = connection.prepareStatement(query2Text)
      query2.setInt(1, tabid)
      query2.setString(2, firstName)
      query2.setString(3, lastName)
      val queryResult = query2.executeQuery()
      connection.close()
      if (queryResult.next()) {
        val id = queryResult.getInt("id")
        val tabid = queryResult.getInt("tabid")
        val firstName = queryResult.getString("firstname")
        val lastName = queryResult.getString("lastname")
        val rating = queryResult.getInt("rating")
        val isActive = queryResult.getBoolean("active")
        Judge(id, tabid, firstName, lastName, rating, isActive)
      } else {
        throw new Throwable(
          "Error during judge registration. Most likely a database failure")
      }
    }
  }

}