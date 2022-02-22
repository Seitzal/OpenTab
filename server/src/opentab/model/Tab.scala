package opentab.model

import opentab._
import opentab.server._
import doobie._
import doobie.implicits._
import cats.effect._
import cats.implicits._
import upickle.default._
import java.io.File

case class Tab(
  id: Int,
  name: String,
  owner: Int
) {

  def isPublic(implicit xa: Xa): IO[Boolean] =
    getSetting("public").map(_ == "true")

  def delete(implicit xa: Xa): IO[Unit] =
    sql"DELETE FROM tabs WHERE id = $id"
      .update
      .run
      .map(_ => {})
      .transact(xa)

  def rename(newName: String)(implicit xa: Xa): IO[Tab] =
    sql"UPDATE tabs SET name = $newName WHERE id = $id"
      .update
      .withUniqueGeneratedKeys[Tab]("id", "name", "owner")
      .transact(xa)

  def numberOfRounds(implicit xa: Xa): IO[Int] =
    Round.getLastForTab(id).map(_.map(_.roundNo).getOrElse(0))

  def rounds(implicit xa: Xa): IO[List[Round]] = Round.getAllForTab(id)

  private def getSettingRaw(key: String)(implicit xa: Xa): IO[Option[String]] =
    sql"SELECT value FROM tabsettings WHERE tabid = $id AND key = $key"
      .query[String]
      .option
      .transact(xa)

  private def updateSettingRaw(key: String, value: String)(implicit xa: Xa): IO[String] =
    getSettingRaw(key).map {
      case None =>
        sql"INSERT INTO tabsettings (tabid, key, value) VALUES ($id, $key, $value)"
      case Some(_) =>
        sql"UPDATE tabsettings SET value = $value WHERE tabid = $id AND key = $key"
    }.flatMap {
      _.update
      .withUniqueGeneratedKeys[String]("value")
      .transact(xa)
    }

  private def initSetting(key: String)(implicit xa: Xa): IO[String] =
    updateSettingRaw(key, defaultTabSettings(key))

  def getSetting(key: String)(implicit xa: Xa): IO[String] = {
    if (defaultTabSettings.contains(key)) {
      getSettingRaw(key).flatMap {
        case Some(value) => IO.pure(value)
        case None => initSetting(key)
      }
    } else IO.raiseError(new Error(s"$key is not a valid tab settings key"))
  }

  def getSettings(implicit xa: Xa): IO[Map[String, String]] =
    defaultTabSettings
      .keys
      .toList
      .map(getSetting)
      .sequence
      .map(defaultTabSettings.keys.toList.zip)
      .map(_.toMap)

  def updateSetting(key: String, value: String)(implicit xa: Xa): IO[String] =
    if (defaultTabSettings.contains(key))
      updateSettingRaw(key, value)
    else IO.raiseError(new Error(s"$key is not a valid tab settings key"))

  def updateSettings(settings: Map[String, String])(implicit xa: Xa): IO[Map[String, String]] =
    settings
      .toList
      .map{case (k,v) => updateSetting(k, v)}
      .sequence
      .*>(getSettings)

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

  def getAllPublic(implicit xa: Xa): IO[List[Tab]] =
    sql"SELECT * FROM tabs WHERE id IN (SELECT tabid FROM tabsettings WHERE key = 'public' AND value = 'true')"
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
      (name: String, owner: Int)
      (implicit xa: Xa): IO[Tab] =
    sql"INSERT INTO tabs (name, owner) VALUES ($name, $owner)"
      .update
      .withUniqueGeneratedKeys[Tab]("id", "name", "owner")
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
