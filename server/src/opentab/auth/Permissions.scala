package opentab.auth
import opentab._

import doobie._
import doobie.implicits._
import cats.effect._
import cats.implicits._
import upickle.default._

case class Permissions(
  userId: Int,
  tabId: Int,
  view: Boolean,
  results: Boolean,
  setup: Boolean,
  own: Boolean
) {

  def update(
      newView: Option[Boolean],
      newResults: Option[Boolean],
      newSetup: Option[Boolean],
      newOwn: Option[Boolean])
      (implicit xa: Xa): IO[Permissions] = {
    val queryString =
      fr"UPDATE permissions SET " ++ List(
        newView match {
          case Some(v) => List(fr"view = $v")
          case None => Nil
        },
        newResults match {
          case Some(v) => List(fr"view = $v")
          case None => Nil
        },
        newSetup match {
          case Some(v) => List(fr"view = $v")
          case None => Nil
        },
        newOwn match {
          case Some(v) => List(fr"view = $v")
          case None => Nil
        }
      ).flatten.intercalate(fr",") ++
      fr"WHERE userid = $userId AND tabid = $tabId"
    queryString
      .update
      .withUniqueGeneratedKeys[Permissions](
        "userid", "tabid", "view", "results", "setup", "own")
      .orElse {
        sql"""
          INSERT INTO permissions (userid, tabid, view, results, setup, own)
          VALUES (
            $userId,
            $tabId,
            ${newView.getOrElse(view)},
            ${newResults.getOrElse(results)},
            ${newSetup.getOrElse(setup)},
            ${newOwn.getOrElse(own)}
          )
        """
          .update
          .withUniqueGeneratedKeys[Permissions](
            "userid", "tabid", "view", "results", "setup", "own")
      }.transact(xa)
  }

}

object Permissions {

  def apply(userId: Int, tabId: Int)(implicit xa: Xa): IO[Permissions] =
    (for {
      user <- User(userId)
      tab  <- Tab(tabId)
    } yield {
      if (user.isAdmin || tab.owner == userId) 
        IO.delay(Permissions(userId, tabId, true, true, true, true))
      else
        sql"SELECT * FROM permissions WHERE userid = $userId AND tabid = $tabId"
          .query[Permissions]
          .unique
          .transact(xa)
          .handleError(_ =>
            if (tab.isPublic)
              Permissions(userId, tabId, true, false, false, false)
            else 
              Permissions(userId, tabId, false, false, false, false))
     }).flatten

  implicit val rw: ReadWriter[Permissions] = macroRW

}
