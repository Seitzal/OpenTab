package opentab

import doobie._
import doobie.implicits._
import cats.effect._
import cats.implicits._
import upickle.default._

case class Round(
  tabId: Int,
  roundNo: Int,
  isLocked: Boolean,
  isCompleted: Boolean
) {

  def delete(implicit xa: Xa): IO[Unit] =
    sql"DELETE FROM rounds WHERE tabid = $tabId AND roundno = $roundNo"
      .update
      .run
      .map(_ => {})
      .transact(xa)

  def lock(implicit xa: Xa): IO[Round] =
    sql"UPDATE rounds SET islocked = TRUE WHERE tabid = $tabId AND roundno = $roundNo"
      .update
      .withUniqueGeneratedKeys[Round]("tabid", "roundno", "islocked", "iscompleted")
      .transact(xa)

  def unlock(implicit xa: Xa): IO[Round] =
    sql"UPDATE rounds SET islocked = FALSE WHERE tabid = $tabId AND roundno = $roundNo"
      .update
      .withUniqueGeneratedKeys[Round]("tabid", "roundno", "islocked", "iscompleted")
      .transact(xa)

  def complete(implicit xa: Xa): IO[Round] =
    sql"UPDATE rounds SET iscompleted = TRUE WHERE tabid = $tabId AND roundno = $roundNo"
      .update
      .withUniqueGeneratedKeys[Round]("tabid", "roundno", "islocked", "iscompleted")
      .transact(xa)

  def reopen(implicit xa: Xa): IO[Round] =
    sql"UPDATE rounds SET iscompleted = FALSE WHERE tabid = $tabId AND roundno = $roundNo"
      .update
      .withUniqueGeneratedKeys[Round]("tabid", "roundno", "islocked", "iscompleted")
      .transact(xa)

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

  def getLastForTab(tabId: Int)(implicit xa: Xa): IO[Round] =
    sql"SELECT * FROM rounds WHERE tabid = $tabId AND roundno = (SELECT MAX(roundno) FROM rounds WHERE tabid = $tabId)"
        .query[Round]
        .unique
        .transact(xa)

  def add(tabId: Int)(implicit xa: Xa): IO[Round] =
    for {
      tab <- Tab(tabId)
      existingRounds <- tab.numberOfRounds
      round <-
        sql"INSERT INTO ROUNDS (tabid, roundno, islocked, iscompleted) VALUES ($tabId, ${existingRounds + 1}, FALSE, FALSE)"
          .update
          .withUniqueGeneratedKeys[Round]("tabid", "roundno", "islocked", "iscompleted")
          .transact(xa)
     } yield round

  implicit val rw: ReadWriter[Round] = macroRW
}