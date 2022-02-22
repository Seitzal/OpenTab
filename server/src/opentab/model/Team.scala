package opentab.model

import opentab._
import opentab.server._

import doobie._
import doobie.implicits._
import cats.effect.IO
import cats.implicits._
import upickle.default._

case class Team(
  id: Int,
  tabId: Int,
  name: String,
  delegation: String,
  status: Int,
  isActive: Boolean
) {

  def delete(implicit xa: Xa): IO[Unit] =
    sql"DELETE FROM teams WHERE id = $id"
      .update
      .run
      .map(_ => {})
      .transact(xa)

  def update(
      newName: Option[String] = None,
      newDelegation: Option[String] = None,
      newStatus: Option[Int] = None,
      newIsActive: Option[Boolean] = None)
      (implicit xa: Xa): IO[Team] =
    updateSql("teams")(
      updateFragment("name", newName),
      updateFragment("delegation", newDelegation),
      updateFragment("status", newStatus),
      updateFragment("isactive", newIsActive)
    )(fr"WHERE id = $id")
      .update
      .withUniqueGeneratedKeys[Team](
        "id", "tabid", "name", "delegation", "status", "isactive")
      .transact(xa)

} 

object Team {

  def apply(teamId: Int)(implicit xa: Xa): IO[Team] =
    sql"SELECT * FROM teams WHERE id = $teamId"
      .query[Team]
      .unique
      .transact(xa)

  def getAll(implicit xa: Xa): IO[List[Team]] =
    sql"SELECT * FROM teams"
      .query[Team]
      .stream
      .compile
      .toList
      .map(_.sortBy(_.id))
      .transact(xa)

  def getAllForTab(tabId: Int)(implicit xa: Xa): IO[List[Team]] =
    sql"SELECT * FROM teams WHERE tabid = $tabId"
      .query[Team]
      .stream
      .compile
      .toList
      .map(_.sortBy(_.id))
      .transact(xa)
  
  def create
      (tabId: Int, name: String, delegation: String, status: Int)
      (implicit xa: Xa): IO[Team] =
    sql"INSERT INTO teams (tabid, name, delegation, status, isactive) VALUES ($tabId, $name, $delegation, $status, TRUE)"
      .update
      .withUniqueGeneratedKeys[Team](
        "id", "tabid", "name", "delegation", "status", "isactive")
      .transact(xa)

  implicit val rw: ReadWriter[Team] = macroRW

}

case class TeamPartial(
  name: String,
  delegation: String,
  status: String
)

object TeamPartial {
  implicit val rw: ReadWriter[TeamPartial] = macroRW
}
