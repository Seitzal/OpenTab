package eu.seitzal.opentab.models

import eu.seitzal.opentab._
import shortcuts._
import upickle.{default => json}

import play.api.db.Database

case class Round(
  tabid: Int,
  roundNumber: Int,
  locked: Boolean,
  finished: Boolean) {

    def delete()(implicit database: Database): Unit = {
      Pairing.deleteAllForRound(tabid, roundNumber)
      val connection = database.getConnection()
      if (locked) throw new Exception("Can't delete a locked round!")
      val queryText = "DELETE FROM rounds WHERE tabid = ? AND roundno >= ?"
      val query = connection.prepareStatement(queryText)
      query.setInt(1, tabid)
      query.setInt(2, roundNumber)
      query.executeUpdate()
      connection.close()
    }

    def pairings(implicit database: Database) = Pairing.getAllForRound(tabid, roundNumber)

    def setDraw(draw: Draw)(implicit database: Database): Unit = {
      Pairing.deleteAllForRound(tabid, roundNumber)
      val tab = Tab(tabid)
      val pairings = draw.teamOnBye match {
        case Some (teamOnBye) => {
          if (teamOnBye.sideTendency > 0)
            (tab.bye, teamOnBye) :: draw.pairings
          else
            (teamOnBye, tab.bye) :: draw.pairings
        }
        case None => draw.pairings
      }
      pairings.map(p => Pairing.create(tabid, roundNumber, p._1.id, p._2.id))
    }

}

object Round {

  implicit val rw: json.ReadWriter[Round] = json.macroRW

  def add(tabid: Int)(implicit database: Database): Round = {
    val tab = Tab(tabid) // throw error if tab doesn't exist.
    val existing = getAll(tabid)
    val roundNumber = existing.length + 1
    val connection = database.getConnection()
    val queryText = 
      "INSERT INTO rounds " +
      "(tabid, roundno, locked, finished)" +
      "VALUES (?, ?, 0, 0)"
    val query = connection.prepareStatement(queryText)
    query.setInt(1, tabid)
    query.setInt(2, roundNumber)
    query.executeUpdate()
    connection.close()
    Round(tabid, roundNumber)
  }

  def apply(tabid: Int, roundNumber: Int)(implicit database: Database): Round = {
    val tab = Tab(tabid) // throw error if tab doesn't exist.
    val connection = database.getConnection()
    val queryText = "SELECT * FROM rounds WHERE tabid = ? AND roundno = ?"
    val query = connection.prepareStatement(queryText)
    query.setInt(1, tabid)
    query.setInt(2, roundNumber)
    val queryResult = query.executeQuery()
    connection.close()
    if (queryResult.next()) {
      val tabid = queryResult.getInt("tabid")
      val roundNumber = queryResult.getInt("roundno")
      val locked = queryResult.getBoolean("locked")
      val finished = queryResult.getBoolean("finished")
      Round(tabid, roundNumber, locked, finished)
    } else {
      throw new NotFoundException("round", "number", roundNumber.toString)
    }
  }

  def getAll(tabid: Int)(implicit database: Database): List[Round] = {
    val connection = database.getConnection()
    val queryText = "SELECT * FROM rounds WHERE tabid = ?"
    val query = connection.prepareStatement(queryText)
    query.setInt(1, tabid)
    val queryResult = query.executeQuery()
    connection.close()
    def iter(rounds: List[Round]) : List[Round] =
      if (queryResult.next()) {
        val tabid = queryResult.getInt("tabid")
        val roundNumber = queryResult.getInt("roundno")
        val locked = queryResult.getBoolean("locked")
        val finished = queryResult.getBoolean("finished")
        iter(Round(tabid, roundNumber, locked, finished) :: rounds)
      } else rounds.reverse
    iter(Nil)
  }

}