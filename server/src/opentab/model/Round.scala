package opentab.model

import opentab._
import opentab.server._
import opentab.exceptions._

import doobie._
import doobie.implicits._
import cats.effect.IO
import cats.implicits._
import upickle.default._
import doobie.util.invariant

case class Round(
  tabId: Int,
  roundNo: Int,
  isPrepared: Boolean,
  isLocked: Boolean
) {

  // Deleting a round should cascade forward, also deleting all subsequent rounds
  def delete(implicit xa: Xa): IO[Unit] =
    sql"DELETE FROM rounds WHERE tabid = $tabId AND roundno >= $roundNo"
      .update
      .run
      .map(_ => {})
      .transact(xa)

  // Unsafely updates round status *without checking prerequisites*
  private def updateStatus(value: Boolean)(implicit xa: Xa): IO[Round] =
    sql"UPDATE rounds SET isLocked = $value WHERE tabid = $tabId AND roundno = $roundNo"
    .update
    .withUniqueGeneratedKeys[Round]("tabid", "roundno", "islocked")
    .transact(xa)

  // Locking a round should only be possible if the round is unlocked and all previous rounds are locked
  def lock(implicit xa: Xa): IO[Round] =
    if (isLocked) 
      IO.raiseError(RoundStatusException("Round is already locked"))
    else if (roundNo == 1) 
      updateStatus(true)
    else Round(tabId, roundNo - 1)
      .map(_.isLocked)
      .flatMap {
        case true => updateStatus(true)
        case false => 
          IO.raiseError(RoundStatusException("Previous round is still unlocked"))
      }

  // Unlocking a round should be possible if it is locked.
  // Unlocking a round should cascade forward, also unlocking all subsequent rounds.
  def unlock(implicit xa: Xa): IO[Round] =
    if (!isLocked) 
      IO.raiseError(RoundStatusException("Round is already unlocked"))
    else Round(tabId, roundNo + 1)
      .flatMap { nextRound =>
        if (nextRound.isLocked) nextRound.unlock *> updateStatus(false)
        else updateStatus(false)
      }.recoverWith {
        case invariant.UnexpectedEnd => updateStatus(false)
      }

}

object Round {

  def apply(tabId: Int, roundNo: Int)(implicit xa: Xa): IO[Round] =
    sql"SELECT * FROM rounds WHERE tabid = $tabId AND roundno = $roundNo"
        .query[Round]
        .unique
        .transact(xa)

  def getAllForTab(tabId: Int)(implicit xa: Xa): IO[List[Round]] =
    sql"SELECT * FROM rounds WHERE tabid = $tabId"
      .query[Round]
      .stream
      .compile
      .toList
      .map(_.sortBy(_.roundNo))
      .transact(xa)

  def getLastForTab(tabId: Int)(implicit xa: Xa): IO[Option[Round]] =
    sql"SELECT * FROM rounds WHERE tabid = $tabId AND roundno = (SELECT MAX(roundno) FROM rounds WHERE tabid = $tabId)"
        .query[Round]
        .option
        .transact(xa)

  def add(tabId: Int, prepared: Boolean)(implicit xa: Xa): IO[Round] =
    for {
      tab <- Tab(tabId)
      existingRounds <- tab.numberOfRounds
      round <-
        sql"INSERT INTO ROUNDS (tabid, roundno, isprepared, islocked) VALUES ($tabId, ${existingRounds + 1}, $prepared, FALSE)"
          .update
          .withUniqueGeneratedKeys[Round]("tabid", "roundno", "isprepared", "islocked")
          .transact(xa)
     } yield round

  implicit val rw: ReadWriter[Round] = macroRW
}