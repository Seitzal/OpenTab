package opentab.models

import opentab._
import shortcuts._

import upickle.{default => json}

import play.api.Logging
import play.api.db.Database

import scala.util.{Try, Success, Failure}
import scala.concurrent.{Future, ExecutionContext => EC}

case class Team(
    id: Int,
    tabid: Int,
    name: String,
    delegation: String,
    status: Int,
    active: Boolean) extends Logging {

  def delete()(implicit db: Database, ec: EC): Unit = {
    Cache.teams.remove(id)
    Cache.speakers.filterInPlace{case (id, speaker) => speaker.teamid != id}
    Future {
      val connection = db.getConnection()
      val queryText = "DELETE FROM speakers WHERE teamid = ?"
      val query = connection.prepareStatement(queryText)
      query.setInt(1, id)
      query.executeUpdate()
      val queryText2 = "DELETE FROM teams WHERE id = ?"
      val query2 = connection.prepareStatement(queryText2)
      query2.setInt(1, id)
      query2.executeUpdate()
      connection.close()
    } onComplete {
      case Success(_) => logger.debug("Async database delete task completed for team " + id)
      case Failure(t) => logger.error("Async database delete task failed for team " + id)
    }
  }

  def updateName(newName: String)(implicit database: Database, ec: EC): Team = {
    val newTeam = Team(id, tabid, newName, delegation, status, active)
    Cache.teams.replace(id, this, newTeam)
    Future {
      val connection = database.getConnection()
      val queryText = "UPDATE teams SET name = ? WHERE id = ?"
      val query = connection.prepareStatement(queryText)
      query.setString(1, newName)
      query.setInt(2, id)
      query.executeUpdate()
      connection.close()
    } onComplete {
      case Success(_) => logger.debug("Async database update task completed for team " + id)
      case Failure(t) => logger.error("Async database update task failed for team " + id)
    }
    newTeam
  }

  def updateDelegation(newDeleg: String)(implicit db: Database, ec: EC): Team = {
    val newTeam = Team(id, tabid, name, newDeleg, status, active)
    Cache.teams.replace(id, this, newTeam)
    Future {
      val connection = db.getConnection()
      val queryText = "UPDATE teams SET delegation = ? WHERE id = ?"
      val query = connection.prepareStatement(queryText)
      query.setString(1, newDeleg)
      query.setInt(2, id)
      query.executeUpdate()
      connection.close()
    } onComplete {
      case Success(_) => logger.debug("Async database update task completed for team " + id)
      case Failure(t) => logger.error("Async database update task failed for team " + id)
    }
    newTeam
  }

  def updateStatus(newStatus: Int)(implicit db: Database, ec: EC): Team = {
    val newTeam = Team(id, tabid, name, delegation, newStatus, active)
    Cache.teams.replace(id, this, newTeam)
    Future {
      val connection = db.getConnection()
      val queryText = "UPDATE teams SET langstatus = ? WHERE id = ?"
      val query = connection.prepareStatement(queryText)
      query.setInt(1, newStatus)
      query.setInt(2, id)
      query.executeUpdate()
      connection.close()
    } onComplete {
      case Success(_) => logger.debug("Async database update task completed for team " + id)
      case Failure(t) => logger.error("Async database update task failed for team " + id)
    }
    newTeam
  }

  def update(
    newName: Option[String],
    newDeleg: Option[String],
    newStatus: Option[Int])(implicit db: Database, ec: EC): Team =
    newName match {
      case Some(n) => updateName(n)(db, ec).update(None, newDeleg, newStatus)(db, ec)
      case None => newDeleg match {
        case Some(d) => updateDelegation(d)(db, ec).update(None, None, newStatus)(db, ec)
        case None => newStatus match {
          case Some(s) => updateStatus(s)(db, ec)
          case None => this
        }
      }
    }

  def toggleActive(isActive: Boolean = !active)(implicit db: Database, ec: EC): Team = {
    val newTeam = Team(id, tabid, name, delegation, status, isActive)
    Cache.teams.replace(id, this, newTeam)
    Future {
      val connection = db.getConnection()
      val queryText = "UPDATE teams SET active = ? WHERE id = ?"
      val query = connection.prepareStatement(queryText)
      query.setBoolean(1, isActive)
      query.setInt(2, id)
      query.executeUpdate()
      connection.close()
    } onComplete {
      case Success(_) => logger.debug("Async database update task completed for team " + id)
      case Failure(t) => logger.error("Async database update task failed for team " + id)
    }
    newTeam
  }

  def tab(implicit database: Database) = Tab(tabid)

  def speakers(implicit database: Database) = Speaker.getAllOnTeam(id)

  def previousOpponents(implicit database: Database): List[Int] =
    Cache.previousOpponents.get(id) match {
      case Some(list) => list
      case None => {
        Cache.previousOpponents.putIfAbsent(id, {
          Pairing.getAllForTeam(this)
            .filter(pairing =>
              pairing.teamidPro > 0 && pairing.teamidOpp > 0
            ).map(pairing =>
              if (pairing.teamidPro == this.id)
                Try(Team(pairing.teamidOpp).id).toOption
              else
                Try(Team(pairing.teamidPro).id).toOption
            ).filter(_.isDefined).map(_.get)
        })
        previousOpponents
      }
    }

  def sideTendency(implicit database: Database): Int =
  Cache.sideTendencies.get(id) match {
    case Some(tendency) => tendency
    case None => {
      Cache.sideTendencies.putIfAbsent(id, {
        Pairing.getAllForTeam(this).map(pairing =>
          if (pairing.teamidPro == this.id) 1
          else -1
        ).sum
      })
      sideTendency
    }
  }

}

object Team extends Logging {

  class Bye(tabid: Int) extends Team(-tabid, tabid, "BYE", "BYE", -1, true)

  implicit val rw: json.ReadWriter[Team] = json.macroRW

  def apply(id: Int)(implicit database: Database): Team =
    Cache.teams.getOrElse(id, {
      logger.debug("No cached result for team " + id + ", querying database")
      val connection = database.getConnection()
      val queryText = "SELECT * FROM teams WHERE id = ?"
      val query = connection.prepareStatement(queryText)
      query.setInt(1, id)
      val queryResult = query.executeQuery()
      connection.close()
      if (queryResult.next()) {
        val team = Team(
          queryResult.getInt("id"),
          queryResult.getInt("tabid"),
          queryResult.getString("name"),
          queryResult.getString("delegation"),
          queryResult.getInt("langstatus"),
          queryResult.getBoolean("active"))
        Cache.teams.putIfAbsent(id, team)
        team
      } else {
        throw new NotFoundException("team", "ID", id.toString)
      }
    })

  def getAll(tabid: Int)(implicit database: Database): List[Team] = 
    if (Cache.loadedForTab(tabid, "teams")) {
      Cache.teams
        .filter{ case (id, team) => team.tabid == tabid }
        .map(_._2)
        .toList
        .sortBy(_.id)
    } else {
      logger.debug("No cached result for teams in tab " + tabid + ", querying database")
      val connection = database.getConnection()
      val queryText = "SELECT * FROM teams WHERE tabid = ?"
      val query = connection.prepareStatement(queryText)
      query.setInt(1, tabid)
      val queryResult = query.executeQuery()
      connection.close()
      def iter(teams: List[Team]) : List[Team] =
        if (queryResult.next()) {
          val team = Team(
            queryResult.getInt("id"),
            queryResult.getInt("tabid"),
            queryResult.getString("name"),
            queryResult.getString("delegation"),
            queryResult.getInt("langstatus"),
            queryResult.getBoolean("active"))
          iter(team :: teams)
        } else teams.reverse
      val teams = iter(Nil)
      teams.foreach(team => Cache.teams.putIfAbsent(team.id, team))
      Cache.loadedForTab.set(tabid, "teams")
      teams
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
        val team = Team(
          queryResult.getInt("id"),
          queryResult.getInt("tabid"),
          queryResult.getString("name"),
          queryResult.getString("delegation"),
          queryResult.getInt("langstatus"),
          queryResult.getBoolean("active"))
        Cache.teams.putIfAbsent(team.id, team)
        team
      } else {
        throw new Throwable(
          "Error during team creation. Most likely a database failure")
      }
    }
  }

}