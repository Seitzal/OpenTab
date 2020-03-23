package opentab.models

import opentab._
import shortcuts._

import upickle.{default => json}
import play.api.Logging
import play.api.db.Database

import scala.util.{Try, Success, Failure}
import scala.concurrent.{Future, ExecutionContext}

case class Speaker(
    id: Int,
    tabid: Int,
    teamid: Int,
    firstName: String,
    lastName: String,
    status: Int) extends Logging {

  def delete()(implicit db: Database, ec: ExecutionContext): Unit = {
    Cache.speakers.remove(id)
    Future {
      val connection = db.getConnection()
      val queryText = "DELETE FROM speakers WHERE id = ?"
      val query = connection.prepareStatement(queryText)
      query.setInt(1, id)
      query.executeUpdate()
      connection.close()
    } onComplete {
      case Success(_) => logger.debug("Async db delete task completed for speaker " + id)
      case Failure(t) => logger.error("Async db delete task failed for speaker " + id)
    }
  }

  def updateFirstName(newName: String)(implicit db: Database, ec: ExecutionContext): Speaker = {
    val newSpeaker = Speaker(id, tabid, teamid, newName, lastName, status)
    Cache.speakers.replace(id, this, newSpeaker)
    Future {
      val connection = db.getConnection()
      val queryText = "UPDATE speakers SET firstname = ? WHERE id = ?"
      val query = connection.prepareStatement(queryText)
      query.setString(1, newName)
      query.setInt(2, id)
      query.executeUpdate()
      connection.close()
    } onComplete {
      case Success(_) => logger.debug("Async db update task completed for speaker " + id)
      case Failure(t) => logger.error("Async db update task failed for speaker " + id)
    }
    newSpeaker
  }

  def updateLastName(newName: String)(implicit db: Database, ec: ExecutionContext): Speaker = {
    val newSpeaker = Speaker(id, tabid, teamid, firstName, newName, status)
    Cache.speakers.replace(id, this, newSpeaker)
    Future {
      val connection = db.getConnection()
      val queryText = "UPDATE speakers SET lastname = ? WHERE id = ?"
      val query = connection.prepareStatement(queryText)
      query.setString(1, newName)
      query.setInt(2, id)
      query.executeUpdate()
      connection.close()
    } onComplete {
      case Success(_) => logger.debug("Async db update task completed for speaker " + id)
      case Failure(t) => logger.error("Async db update task failed for speaker " + id)
    }
    newSpeaker
  }

  def updateStatus(newStatus: Int)(implicit db: Database, ec: ExecutionContext): Speaker = {
    val newSpeaker = Speaker(id, tabid, teamid, firstName, lastName, newStatus)
    Cache.speakers.replace(id, this, newSpeaker)
    Future {
      val connection = db.getConnection()
      val queryText = "UPDATE speakers SET langstatus = ? WHERE id = ?"
      val query = connection.prepareStatement(queryText)
      query.setInt(1, newStatus)
      query.setInt(2, id)
      query.executeUpdate()
      connection.close()
    } onComplete {
      case Success(_) => logger.debug("Async db update task completed for speaker " + id)
      case Failure(t) => logger.error("Async db update task failed for speaker " + id)
    }
    newSpeaker
  }

  def update(
    newFirstName: Option[String],
    newLastName: Option[String],
    newStatus: Option[Int])(implicit db: Database, ec: ExecutionContext): Speaker =
    newFirstName match {
      case Some(fn) => updateFirstName(fn)(db, ec).update(None, newLastName, newStatus)(db, ec)
      case None => newLastName match {
        case Some(ln) => updateLastName(ln)(db, ec).update(None, None, newStatus)(db, ec)
        case None => newStatus match {
          case Some(s) => updateStatus(s)(db, ec)
          case None => this
        }
      }
    }
  
  def tab(implicit db: Database) = Tab(tabid)

  def team(implicit db: Database) = Team(teamid)

  def externalRepresentation(implicit db: Database) = {
    val team = this.team
    SpeakerExternalRepresentation(
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

object Speaker extends Logging {

  implicit val rw: json.ReadWriter[Speaker] = json.macroRW

  def apply(id: Int)(implicit db: Database): Speaker =
    Cache.speakers.getOrElse(id, {
      logger.debug("No cached result for speaker " + id + ", querying database")
      val connection = db.getConnection()
      val queryText = "SELECT * FROM speakers WHERE id = ?"
      val query = connection.prepareStatement(queryText)
      query.setInt(1, id)
      val queryResult = query.executeQuery()
      connection.close()
      if (queryResult.next()) {
        val speaker = Speaker(
          queryResult.getInt("id"),
          queryResult.getInt("tabid"),
          queryResult.getInt("teamid"),
          queryResult.getString("firstname"),
          queryResult.getString("lastname"),
          queryResult.getInt("langstatus"))
        Cache.speakers.putIfAbsent(id, speaker)
        speaker
      } else {
        throw new NotFoundException("speaker", "ID", id.toString)
      }
    })

  def getAll(tabid: Int)(implicit db: Database): List[Speaker] =
    if (Cache.loadedForTab(tabid, "speakers")) {
      Cache.speakers
        .filter{ case (id, speaker) => speaker.tabid == tabid }
        .map(_._2)
        .toList
        .sortBy(_.id)
    } else {
      logger.debug("No cached result for speakers in tab " + tabid + ", querying database")
      val connection = db.getConnection()
      val queryText = "SELECT * FROM speakers WHERE tabid = ?"
      val query = connection.prepareStatement(queryText)
      query.setInt(1, tabid)
      val queryResult = query.executeQuery()
      connection.close()
      def iter(speakers: List[Speaker]) : List[Speaker] =
        if (queryResult.next()) {
          val speaker = Speaker(
            queryResult.getInt("id"),
            queryResult.getInt("tabid"),
            queryResult.getInt("teamid"),
            queryResult.getString("firstname"),
            queryResult.getString("lastname"),
            queryResult.getInt("langstatus"))
        iter(speaker :: speakers)
        } else speakers.reverse
      val speakers = iter(Nil)
      speakers.foreach(speaker => Cache.speakers.putIfAbsent(speaker.id, speaker))
      Cache.loadedForTab.set(tabid, "speakers")
      speakers
    }

  def getAllOnTeam(teamid: Int)(implicit db: Database): List[Speaker] = 
    if (Cache.loadedForTeam(teamid, "speakers")) {
      Cache.speakers
        .filter{ case (id, speaker) => speaker.teamid == teamid }
        .map(_._2)
        .toList
        .sortBy(_.id)
    } else {
      logger.debug("No cached result for speakers on team " + teamid + ", querying database")
      val connection = db.getConnection()
      val queryText = "SELECT * FROM speakers WHERE teamid = ?"
      val query = connection.prepareStatement(queryText)
      query.setInt(1, teamid)
      val queryResult = query.executeQuery()
      connection.close()
      def iter(speakers: List[Speaker]) : List[Speaker] =
        if (queryResult.next()) {
          val speaker = Speaker(
            queryResult.getInt("id"),
            queryResult.getInt("tabid"),
            queryResult.getInt("teamid"),
            queryResult.getString("firstname"),
            queryResult.getString("lastname"),
            queryResult.getInt("langstatus"))
        iter(speaker :: speakers)
        } else speakers.reverse
      val speakers = iter(Nil)
      speakers.foreach(speaker => Cache.speakers.putIfAbsent(speaker.id, speaker))
      Cache.loadedForTeam.set(teamid, "speakers")
      speakers
    }

  def create(
    teamid: Int,
    firstName: String,
    lastName: String,
    status: Int)(implicit db: Database) : Speaker = {

    val tabid = Team(teamid).tabid
    val connection = db.getConnection()
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
        val speaker = Speaker(
            query2Result.getInt("id"),
            query2Result.getInt("tabid"),
            query2Result.getInt("teamid"),
            query2Result.getString("firstname"),
            query2Result.getString("lastname"),
            query2Result.getInt("langstatus"))
        Cache.speakers.putIfAbsent(speaker.id, speaker)
        speaker
      } else {
        throw new Throwable(
          "Error during speaker creation. Most likely a db failure")
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