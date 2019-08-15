package eu.seitzal.opentab.models

import eu.seitzal.opentab._
import shortcuts._
import upickle.{default => json}

import play.api.db.Database

package object permissions {

  private case class Entry(
    view: Boolean,
    results : Boolean,
    setup: Boolean,
    own: Boolean)

  private def readEntry(userid: Int, tabid: Int)
                       (implicit database: Database) : Entry = {
    val connection = database.getConnection()
    val queryText = "SELECT * FROM permissions WHERE userid = ? AND tabid = ?"
    val query = connection.prepareStatement(queryText)
    query.setInt(1, userid)
    query.setInt(2, userid)
    val queryResult = query.executeQuery()
    if (queryResult.next()) {
      val view    = queryResult.getBoolean("p_view")
      val results = queryResult.getBoolean("p_results")
      val setup   = queryResult.getBoolean("p_setup")
      val own     = queryResult.getBoolean("p_own")
      Entry(view, results, setup, own)
    } else {
      val queryText = "INSERT INTO permissions " +
        "(userid, tabid, p_view, p_results, p_setup, p_own) " +
        "VALUES (?, ?, 0, 0, 0, 0)"
      val query = connection.prepareStatement(queryText)
      query.setInt(1, userid)
      query.setInt(2, userid)
      query.executeUpdate()
      Entry(false, false, false, false)
    }
  }

  def userCanSeeTab(userid: Int, tabid: Int)
                   (implicit database: Database) : Boolean =
    readEntry(userid, tabid).view || Tab(tabid).isPublic

  def userCanSeeTab(userid: Int, tab: Tab)
                   (implicit database: Database) : Boolean =
    readEntry(userid, tab.id).view || tab.isPublic

}