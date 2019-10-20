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
    val connection = database.getConnection()
    if (locked) throw new Exception("Can't delete a locked round!")
    for (r <- Tab(tabid).rounds if r.roundNumber >= roundNumber)
      Pairing.deleteAllForRound(tabid, r.roundNumber)
    val queryText = "DELETE FROM rounds WHERE tabid = ? AND roundno >= ?"
    val query = connection.prepareStatement(queryText)
    query.setInt(1, tabid)
    query.setInt(2, roundNumber)
    query.executeUpdate()
    connection.close()
  }

  def pairings(implicit database: Database) = 
    Pairing.getAllForRound(tabid, roundNumber)

  def draw(implicit database: Database) =
    pairings.partition(p => p.teamidPro < 0 || p.teamidOpp < 0) match {
      case (Nil, Nil) => {
        throw new Exception("Round has not been drawn yet")
      }
      case (byeEntry :: byeTail, otherEntries) => {
        if (byeEntry.teamidPro < 0) {
          Draw(
            otherEntries.map(entry => (entry.pro, entry.opp)), 
            Some(byeEntry.opp))
        } else {
          Draw(otherEntries.map(entry => (entry.pro, entry.opp)), 
          Some(byeEntry.pro))
        }
      }
      case (Nil, entries) => {
        Draw(entries.map(entry => (entry.pro, entry.opp)), None)
      }
    }

  def drawOption(implicit database: Database) =
    pairings.partition(p => p.teamidPro < 0 || p.teamidOpp < 0) match {
      case (Nil, Nil) => {
        None
      }
      case (byeEntry :: byeTail, otherEntries) => {
        if (byeEntry.teamidPro < 0) {
          Some(Draw(
            otherEntries.map(entry => (entry.pro, entry.opp)), 
            Some(byeEntry.opp)))
        } else {
          Some(Draw(otherEntries.map(entry => (entry.pro, entry.opp)), 
          Some(byeEntry.pro)))
        }
      }
      case (Nil, entries) => {
        Some(Draw(entries.map(entry => (entry.pro, entry.opp)), None))
      }
    }

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

  def lock()(implicit database: Database) : Unit = {
    if (locked)
      throw new Exception("Round is already locked")
    for (i <- 1 until roundNumber) {
      if (!Round(tabid, i).locked) {
        throw new Exception("Cannot lock a round while previous rounds are still open")
      }
    }
    if (drawOption.isEmpty) {
      throw new Exception("Cannot lock a round while draw is unfinished")
    }
    val connection = database.getConnection()
    val queryText = 
      "UPDATE rounds " +
      "SET locked = 1 " +
      "WHERE tabid = ? AND roundno = ?"
    val query = connection.prepareStatement(queryText)
    query.setInt(1, tabid)
    query.setInt(2, roundNumber)
    query.executeUpdate()
    connection.close()
  }

  def unlock()(implicit database: Database) : Unit = {
    if (!locked)
      throw new Exception("Round is already open")
    if (finished)
      throw new Exception("Cannot unlock a round that has been marked as finished")
    val allRounds = Round.getAll(tabid)
    for(i <- roundNumber + 1 to allRounds.length) {
      if (Round(tabid, i).locked)
        throw new Exception("Cannot unlock a round while any subsequent rounds are locked")
    }
    val connection = database.getConnection()
    val queryText = 
      "UPDATE rounds " +
      "SET locked = 0 " +
      "WHERE tabid = ? AND roundno = ?"
    val query = connection.prepareStatement(queryText)
    query.setInt(1, tabid)
    query.setInt(2, roundNumber)
    query.executeUpdate()
    connection.close()
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