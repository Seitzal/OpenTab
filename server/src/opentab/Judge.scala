package opentab

import doobie._
import doobie.implicits._
import cats.effect._
import cats.implicits._
import upickle.default._
import scala.util.Random

case class Judge (
  id: Int,
  tabId: Int,
  firstName: String,
  lastName: String,
  rating: Int,
  key: Int,
  isActive: Boolean
) {

  def delete(implicit xa: Transactor[IO]): IO[Unit] =
    sql"DELETE FROM judges WHERE id = $id"
      .update
      .run
      .map(_ => {})
      .transact(xa)

  def update(
      newFirstName: Option[String] = None,
      newLastName: Option[String] = None,
      newRating: Option[Int] = None,
      newIsActive: Option[Boolean] = None)
      (implicit xa: Transactor[IO]): IO[Judge] =
    updateSql("judges")(
      updateFragment("firstname", newFirstName),
      updateFragment("lastname", newLastName),
      updateFragment("rating", newRating),
      updateFragment("isactive", newIsActive)
    )(fr"WHERE id = $id")
      .update
      .withUniqueGeneratedKeys[Judge](
        "id", "tabid", "firstname", "lastname", "rating", "urlkey", "isactive")
      .transact(xa)

  def getAllClashes(implicit xa: Transactor[IO]): IO[List[(Int, Int)]] =
    sql"SELECT teamid, level FROM judge_clashes WHERE judgeid = $id"
      .query[(Int, Int)]
      .stream
      .compile
      .toList
      .transact(xa)

  private def getClashRaw(team: Team)(implicit xa: Transactor[IO]): IO[Option[Int]] =
    sql"""
      SELECT level FROM judge_clashes 
      WHERE judgeid = $id AND teamid = ${team.id}
    """
      .query[Int]
      .stream
      .compile
      .toList
      .map {
        case Nil => None
        case head :: next => Some(head)
      }.transact(xa)
  
  def getClash(team: Team)(implicit xa: Transactor[IO]): IO[Int] =
    getClashRaw(team).map(_.getOrElse(0))

  def setClash(team: Team, level: Int)(implicit xa: Transactor[IO]): IO[Unit] =
    getClashRaw(team).flatMap { result => 
      ((result, level) match {
        case (_, 0) => sql"""
          DELETE FROM judge_clashes 
          WHERE judgeid = $id AND teamid = ${team.id}"""
        case (None, _) => sql"""
          INSERT INTO judge_clashes (judgeid, teamid, level)
          VALUES($id, ${team.id}, $level)"""
        case (Some(currentLevel), _) => sql"""
          UPDATE judge_clashes SET level = $level 
          WHERE judgeid = $id AND teamid = ${team.id}"""
      }).update
      .run
      .map(_ => {})
      .transact(xa)
    }

  def unsetClash(team: Team)(implicit xa: Transactor[IO]): IO[Unit] =
    sql"DELETE FROM judge_clashes WHERE judgeid = $id AND teamid = ${team.id}"
      .update
      .run
      .map(_ => {})
      .transact(xa)

}

object Judge {

  implicit val rw: ReadWriter[Judge] = macroRW

  private val keygen = new Random

  def apply(judgeId: Int)(implicit xa: Transactor[IO]): IO[Judge] =
    sql"SELECT * FROM judges WHERE id = $judgeId"
      .query[Judge]
      .unique
      .transact(xa)

  def getAll(implicit xa: Transactor[IO]): IO[List[Judge]] =
    sql"SELECT * FROM judges"
      .query[Judge]
      .stream
      .compile
      .toList
      .map(_.sortBy(_.id))
      .transact(xa)

  def getAllForTab(tabId: Int)(implicit xa: Transactor[IO]): IO[List[Judge]] =
    sql"SELECT * FROM judges WHERE tabid = $tabId"
      .query[Judge]
      .stream
      .compile
      .toList
      .map(_.sortBy(_.id))
      .transact(xa)

  def create(
      tabId: Int, 
      firstName: String,
      lastName: String, 
      rating: Int, 
      delegation: Option[String])
      (implicit xa: Transactor[IO]): IO[Judge] =
    sql"""
      INSERT INTO judges (tabid, firstname, lastname, rating, urlkey, isactive)
      VALUES ($tabId, $firstName, $lastName, $rating, ${keygen.nextInt(1000000000)}, TRUE)
    """
      .update
      .withUniqueGeneratedKeys[Judge](
        "id", "tabid", "firstname", "lastname", "rating", "urlkey", "isactive")
      .transact(xa)
      .flatMap { judge => delegation match {
        case Some(delegation_) => Team.getAllForTab(tabId).flatMap { _
          .filter(_.delegation == delegation_)
          .map(judge.setClash(_, 10))
          .sequence
          .map(_ => judge)
        }
        case None => IO(judge)
      }}
}

case class JudgePartial(
  firstName: String,
  lastName: String,
  rating: Int,
  delegation: Option[String]
)

object JudgePartial {
  implicit val rw: ReadWriter[JudgePartial] = macroRW
}
