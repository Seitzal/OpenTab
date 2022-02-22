package opentab.model

import opentab._
import opentab.server._
import doobie._
import doobie.implicits._
import cats.effect._
import cats.implicits._
import upickle.default._

case class Speaker (
  id: Int,
  tabId: Int,
  teamId: Int,
  firstName: String,
  lastName: String,
  status: Int
) {
  
  def delete(implicit xa: Xa): IO[Unit] =
    sql"DELETE FROM speakers WHERE id = $id"
      .update
      .run
      .map(_ => {})
      .transact(xa)

  def update(
      newFirstName: Option[String] = None,
      newLastName: Option[String] = None,
      newStatus: Option[Int] = None)
      (implicit xa: Xa): IO[Speaker] =
    updateSql("speakers")(
      updateFragment("firstname", newFirstName),
      updateFragment("lastname", newLastName),
      updateFragment("status", newStatus)
    )(fr"WHERE id = $id")
      .update
      .withUniqueGeneratedKeys[Speaker](
        "id", "tabid", "teamid", "firstname", "lastname", "status")
      .transact(xa)

}

object Speaker {

  def apply(speakerId: Int)(implicit xa: Xa): IO[Speaker] =
    sql"SELECT * FROM speakers WHERE id = $speakerId"
      .query[Speaker]
      .unique
      .transact(xa)

  def getAll(implicit xa: Xa): IO[List[Speaker]] =
    sql"SELECT * FROM speakers"
      .query[Speaker]
      .stream
      .compile
      .toList
      .map(_.sortBy(_.id))
      .transact(xa)

  def getAllForTab(tabId: Int)(implicit xa: Xa): IO[List[Speaker]] =
    sql"SELECT * FROM speakers WHERE tabid = $tabId"
      .query[Speaker]
      .stream
      .compile
      .toList
      .map(_.sortBy(_.id))
      .transact(xa)

  def getAllForTeam(teamId: Int)(implicit xa: Xa): IO[List[Speaker]] =
    sql"SELECT * FROM speakers WHERE teamid = $teamId"
      .query[Speaker]
      .stream
      .compile
      .toList
      .map(_.sortBy(_.id))
      .transact(xa)

  def create
      (tabId: Int, teamId: Int, firstName: String, lastName: String, status: Int)
      (implicit xa: Xa): IO[Speaker] =
    sql"""
        INSERT INTO speakers (tabid, teamid, firstname, lastname, status) 
        VALUES ($tabId, $teamId, $firstName, $lastName, $status)"""
      .update
      .withUniqueGeneratedKeys[Speaker](
        "id", "tabid", "teamid", "firstname", "lastname", "status")
      .transact(xa)

  implicit val rw: ReadWriter[Speaker] = macroRW
}

case class SpeakerPartial (
  firstName: String,
  lastName: String,
  status: String
)

object SpeakerPartial {
  implicit val rw: ReadWriter[SpeakerPartial] = macroRW
}
