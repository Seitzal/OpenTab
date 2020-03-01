package opentab.models

import opentab._
import shortcuts._
import upickle.{default => json}

import play.api.db.Database

package object permissions {

  case class Entry(
    view: Boolean,
    results : Boolean,
    setup: Boolean,
    own: Boolean)

  object Entry {
    implicit val rw: json.ReadWriter[Entry] = json.macroRW
  }

  def readEntry(userid: Int, tabid: Int, attempt: Int = 1)
               (implicit database: Database) : Entry = {
    if (attempt > 4) {
      throw new Throwable("Internal authorization error.")
    }
    val connection = database.getConnection()
    val queryText = "SELECT * FROM permissions WHERE userid = ? AND tabid = ?"
    val query = connection.prepareStatement(queryText)
    query.setInt(1, userid)
    query.setInt(2, tabid)
    val queryResult = query.executeQuery()
    connection.close()
    if (queryResult.next()) {
      val view    = queryResult.getBoolean("p_view")
      val results = queryResult.getBoolean("p_results")
      val setup   = queryResult.getBoolean("p_setup")
      val own     = queryResult.getBoolean("p_own")
      Entry(view, results, setup, own)
    } else {
      val connection = database.getConnection()
      val queryText =
        "INSERT INTO permissions " +
        "(userid, tabid, p_view, p_results, p_setup, p_own) " + {
          if (Tab(tabid).owner == userid || User(userid).isAdmin)
            "VALUES (?, ?, 1, 1, 1, 1)"
          else
            "VALUES (?, ?, 0, 0, 0, 0)"
        }
      val query = connection.prepareStatement(queryText)
      query.setInt(1, userid)
      query.setInt(2, tabid)
      query.executeUpdate()
      connection.close()
      readEntry(userid, tabid, attempt + 1)
    }
  }

  def userCanSeeTab(userid: Int, tabid: Int)
                   (implicit database: Database) : Boolean =
    readEntry(userid, tabid).view || Tab(tabid).isPublic

  def userCanSeeTab(userid: Int, tab: Tab)
                   (implicit database: Database) : Boolean =
    readEntry(userid, tab.id).view || tab.isPublic

  def userCanSeeTab(user: User, tabid: Int)
                   (implicit database: Database) : Boolean =
    readEntry(user.id, tabid).view || Tab(tabid).isPublic

  def userCanSeeTab(user: User, tab: Tab)
                   (implicit database: Database) : Boolean =
    readEntry(user.id, tab.id).view || tab.isPublic

  def userCanSetupTab(userid: Int, tabid: Int)
                     (implicit database: Database) : Boolean =
    readEntry(userid, tabid).setup

  def userCanSetupTab(userid: Int, tab: Tab)
                     (implicit database: Database) : Boolean =
    readEntry(userid, tab.id).setup

  def userCanSetupTab(user: User, tabid: Int)
                     (implicit database: Database) : Boolean =
    readEntry(user.id, tabid).setup

  def userCanSetupTab(user: User, tab: Tab)
                     (implicit database: Database) : Boolean =
    readEntry(user.id, tab.id).setup

}