package opentab

import doobie._
import doobie.implicits._
import cats.effect._
import cats.implicits._
import upickle.default._

case class Tab(
  id: Int,
  name: String,
  owner: Int,
  isPublic: Boolean
) {

  def delete(implicit xa: Xa): IO[Unit] =
    sql"DELETE FROM tabs WHERE id = $id"
      .update
      .run
      .map(_ => {})
      .transact(xa)
  
  def update(
      newName: Option[String] = None, 
      newIsPublic: Option[Boolean] = None)
      (implicit xa: Xa): IO[Tab] =
    updateSql("tabs")(
      updateFragment("name", newName),
      updateFragment("ispublic", newIsPublic)
    )(fr"WHERE id = $id")
      .update
      .withUniqueGeneratedKeys[Tab]("id", "name", "owner", "ispublic")
      .transact(xa)

  def numberOfRounds(implicit xa: Xa): IO[Int] =
    Round.getLastForTab(id).map(_.map(_.roundNo).getOrElse(0))

  def rounds(implicit xa: Xa): IO[List[Round]] = Round.getAllForTab(id)

}

object Tab {

  def apply(tabId: Int)(implicit xa: Xa): IO[Tab] =
    sql"SELECT * FROM tabs WHERE id = $tabId"
      .query[Tab]
      .unique
      .transact(xa)

  def getAll(implicit xa: Xa): IO[List[Tab]] =
    sql"SELECT * FROM tabs"
      .query[Tab]
      .stream
      .compile
      .toList
      .transact(xa)
  
  def getIds(implicit xa: Xa): IO[List[Int]] =
    sql"SELECT id FROM tabs"
      .query[Int]
      .stream
      .compile
      .toList
      .transact(xa)

  def create
      (name: String, owner: Int, isPublic: Boolean)
      (implicit xa: Xa): IO[Tab] =
    sql"INSERT INTO tabs (name, owner, ispublic) VALUES ($name, $owner, $isPublic)"
      .update
      .withUniqueGeneratedKeys[Tab]("id", "name", "owner", "ispublic")
      .transact(xa)

  implicit val rw: ReadWriter[Tab] = macroRW
}

case class TabPartial (
  name: String,
  isPublic: Boolean
)

object TabPartial {
  implicit val rw: ReadWriter[TabPartial] = macroRW
}
