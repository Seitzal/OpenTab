package opentab.auth

import opentab.model._
import opentab.server._
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
      (implicit xa: Xa): IO[Permissions] =
    updateSql("permissions")(
      updateFragment("view", newView),
      updateFragment("results", newResults),
      updateFragment("setup", newSetup),
      updateFragment("own", newOwn)
    )(fr"WHERE userid = $userId AND tabid = $tabId")
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

object Permissions {

  def apply(userId: Int, tabId: Int)(implicit xa: Xa): IO[Permissions] =
    (for {
      user <- User(userId)
      tab  <- Tab(tabId)
    } yield {
      if (user.isAdmin || tab.owner == userId)
        IO(Permissions(userId, tabId, true, true, true, true))
      else
        sql"SELECT * FROM permissions WHERE userid = $userId AND tabid = $tabId"
          .query[Permissions]
          .unique
          .transact(xa)
          .handleErrorWith(_ =>
            tab.isPublic.map {
              case true => Permissions(userId, tabId, true, false, false, false)
              case false => Permissions(userId, tabId, false, false, false, false)
            }
          )
     }).flatten

  implicit val rw: ReadWriter[Permissions] = macroRW

}
