package opentab.models

import opentab._
import shortcuts._

import upickle.{default => json}
import play.api.Logging
import play.api.db.Database

import scala.util.{Try, Success, Failure}
import scala.concurrent.{Future, ExecutionContext}

case class Judge(
    id: Int,
    tabid: Int,
    firstName: String,
    lastName: String,
    rating: Int,
    active: Boolean) extends Logging {

  def delete()(implicit db: Database, ec: ExecutionContext): Unit = {
    Cache.judges.remove(id)
    Future {
      val connection = db.getConnection()
      val queryText = "DELETE FROM judge_clashes WHERE judgeid = ?"
      val query = connection.prepareStatement(queryText)
      query.setInt(1, id)
      query.executeUpdate()
      val query2Text = "DELETE FROM judges WHERE id = ?"
      val query2 = connection.prepareStatement(query2Text)
      query2.setInt(1, id)
      query2.executeUpdate()
      connection.close()
    } onComplete {
      case Success(_) => logger.debug("Async db delete task completed for judge " + id)
      case Failure(t) => logger.error("Async db delete task failed for judge " + id)
    }
  }

  def updateFirstName(newName: String)(implicit db: Database, ec: ExecutionContext): Judge = {
    val newJudge = Judge(id, tabid, newName, lastName, rating, active)
    Cache.judges.replace(id, this, newJudge)
    Future {
      val connection = db.getConnection()
      val queryText = "UPDATE judges SET firstname = ? WHERE id = ?"
      val query = connection.prepareStatement(queryText)
      query.setString(1, newName)
      query.setInt(2, id)
      query.executeUpdate()
      connection.close()
    } onComplete {
      case Success(_) => logger.debug("Async db update task completed for judge " + id)
      case Failure(t) => logger.error("Async db update task failed for judge " + id)
    }
    newJudge
  }

  def updateLastName(newName: String)(implicit db: Database, ec: ExecutionContext): Judge = {
    val newJudge = Judge(id, tabid, firstName, newName, rating, active)
    Cache.judges.replace(id, this, newJudge)
    Future {
      val connection = db.getConnection()
      val queryText = "UPDATE judges SET lastname = ? WHERE id = ?"
      val query = connection.prepareStatement(queryText)
      query.setString(1, newName)
      query.setInt(2, id)
      query.executeUpdate()
      connection.close()
    } onComplete {
      case Success(_) => logger.debug("Async db update task completed for judge " + id)
      case Failure(t) => logger.error("Async db update task failed for judge " + id)
    }
    newJudge
  }

  def updateRating(newRating: Int)(implicit db: Database, ec: ExecutionContext): Judge = {
    val newJudge = Judge(id, tabid, firstName, lastName, newRating, active)
    Cache.judges.replace(id, this, newJudge)
    Future {
      val connection = db.getConnection()
      val queryText = "UPDATE judges SET rating = ? WHERE id = ?"
      val query = connection.prepareStatement(queryText)
      query.setInt(1, newRating)
      query.setInt(2, id)
      query.executeUpdate()
      connection.close()
    } onComplete {
      case Success(_) => logger.debug("Async db update task completed for judge " + id)
      case Failure(t) => logger.error("Async db update task failed for judge " + id)
    }
    newJudge
  }

  def update(
    newFirstName: Option[String],
    newLastName: Option[String],
    newRating: Option[Int])(implicit db: Database, ec: ExecutionContext): Judge =
    newFirstName match {
      case Some(fn) => updateFirstName(fn)(db, ec).update(None, newLastName, newRating)(db, ec)
      case None => newLastName match {
        case Some(ln) => updateLastName(ln)(db, ec).update(None, None, newRating)(db, ec)
        case None => newRating match {
          case Some(r) => updateRating(r)(db, ec)
          case None => this
        }
      }
    }

  def toggleActive(isActive: Boolean = !active)
                  (implicit db: Database, ec: ExecutionContext): Judge = {
    val newJudge = Judge(id, tabid, firstName, lastName, rating, isActive)
    Cache.judges.replace(id, this, newJudge)
    Future {
      val connection = db.getConnection()
      val queryText = "UPDATE judges SET active = ? WHERE id = ?"
      val query = connection.prepareStatement(queryText)
      query.setBoolean(1, isActive)
      query.setInt(2, id)
      query.executeUpdate()
      connection.close()
    } onComplete {
      case Success(_) => logger.debug("Async db update task completed for judge " + id)
      case Failure(t) => logger.error("Async db update task failed for judge " + id)
    }
    newJudge
  }

  def clashes(implicit db: Database): Map[Int, Int] = {
    if (Cache.loadedForJudge(id, "clashes")) {
      Cache.judgeClashes(id)
    } else {
      val connection = db.getConnection()
      val queryText = "SELECT * FROM judge_clashes WHERE judgeid = ?"
      val query = connection.prepareStatement(queryText)
      query.setInt(1, id)
      val queryResult = query.executeQuery()
      connection.close()
      def iter(clashes: List[(Int, Int)]) : List[(Int, Int)] =
        if (queryResult.next()) {
          val teamid = queryResult.getInt("teamid")
          val level = queryResult.getInt("level")
          iter((teamid, level) :: clashes)
        } else clashes.reverse
      val clashes = iter(Nil).toMap
      Cache.judgeClashes.putIfAbsent(id, clashes)
      Cache.loadedForJudge.set(id, "clashes")
      clashes
    }
  }

  def setClash(team: Team, level: Int)(implicit db: Database, ec: ExecutionContext): Unit = {
    val cl = clashes
    Cache.judgeClashes.replace(id, cl.updated(team.id, level))
    Future {
      val connection = db.getConnection()
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
    } onComplete {
      case Success(_) => logger.debug("Async db clash set task completed for judge " + id)
      case Failure(t) => logger.error("Async db clash set task failed for judge " + id)
    }
  }

  def unsetClash(team: Team)(implicit db: Database, ec: ExecutionContext): Unit = {
    val cl = clashes
    Cache.judgeClashes.replace(id, cl - team.id)
    Future {
      val connection = db.getConnection()
      val queryText = "DELETE FROM judge_clashes WHERE judgeid = ? AND teamid = ?"
      val query = connection.prepareStatement(queryText)
      query.setInt(1, id)
      query.setInt(2, team.id)
      query.executeUpdate()
      connection.close()
    } onComplete {
      case Success(_) => logger.debug("Async db clash unset task completed for judge " + id)
      case Failure(t) => logger.error("Async db clash unset task failed for judge " + id)
    }
  }

  def clash(team: Team)(implicit db: Database): Int =
    clashes.apply(team.id)

  def tab(implicit db: Database) = Tab(tabid)

}

object Judge extends Logging {

  implicit val rw: json.ReadWriter[Judge] = json.macroRW

  def apply(id: Int)(implicit db: Database): Judge =
    Cache.judges.getOrElse(id, {
      logger.debug("No cached result for judge " + id + ", querying database")
      val connection = db.getConnection()
      val queryText = "SELECT * FROM judges WHERE id = ?"
      val query = connection.prepareStatement(queryText)
      query.setInt(1, id)
      val queryResult = query.executeQuery()
      connection.close()
      if (queryResult.next()) {
        val judge = Judge(
          queryResult.getInt("id"),
          queryResult.getInt("tabid"),
          queryResult.getString("firstname"),
          queryResult.getString("lastname"),
          queryResult.getInt("rating"),
          queryResult.getBoolean("active"))
        Cache.judges.putIfAbsent(id, judge)
        judge
      } else {
        throw new NotFoundException("judge", "ID", id.toString)
      }
    })

  def getAll(tabid: Int)(implicit db: Database): List[Judge] =
    if (Cache.loadedForTab(tabid, "judges")) {
      Cache.judges
        .filter{ case (id, judge) => judge.tabid == tabid }
        .map(_._2)
        .toList
        .sortBy(_.id)
    } else {
      val connection = db.getConnection()
      val queryText = "SELECT * FROM judges WHERE tabid = ?"
      val query = connection.prepareStatement(queryText)
      query.setInt(1, tabid)
      val queryResult = query.executeQuery()
      connection.close()
      def iter(judges: List[Judge]) : List[Judge] =
        if (queryResult.next()) {
          val judge = Judge(
            queryResult.getInt("id"),
            queryResult.getInt("tabid"),
            queryResult.getString("firstname"),
            queryResult.getString("lastname"),
            queryResult.getInt("rating"),
            queryResult.getBoolean("active"))
          iter(judge :: judges)
        } else judges.reverse
      val judges = iter(Nil)
      judges.foreach(judge => Cache.judges.putIfAbsent(judge.id, judge))
      Cache.loadedForTab.set(tabid, "judges")
      judges
    }

  def create(
      tabid: Int,
      firstName: String,
      lastName: String,
      rating: Int)(implicit db: Database) : Judge = {
    val connection = db.getConnection()
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
        val judge = Judge(
          queryResult.getInt("id"),
          queryResult.getInt("tabid"),
          queryResult.getString("firstname"),
          queryResult.getString("lastname"),
          queryResult.getInt("rating"),
          queryResult.getBoolean("active"))
        Cache.judges.putIfAbsent(judge.id, judge)
        judge
      } else {
        throw new Throwable(
          "Error during judge registration. Most likely a db failure")
      }
    }
  }

}