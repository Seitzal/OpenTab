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
      (implicit xa: Xa): IO[Tab] = {
    val queryString =
      fr"UPDATE tabs SET " ++ List(
        newName match {
          case Some(v) => List(fr"name = $v")
          case None => Nil
        },
        newIsPublic match {
          case Some(v) => List(fr"ispublic = $v")
          case None => Nil
        }
      ).flatten.intercalate(fr",") ++
      fr"WHERE id = $id"
    queryString
      .update
      .withUniqueGeneratedKeys[Tab]("id", "name", "owner", "ispublic")
      .transact(xa)
  }

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
