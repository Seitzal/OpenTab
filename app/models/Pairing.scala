package eu.seitzal.opentab.models

import eu.seitzal.opentab._
import shortcuts._
import upickle.{default => json}

import play.api.db.Database

case class Pairing(
  id: Int,
  tabid: Int,
  roundNumber: Int,
  teamidPro: Int,
  teamidOpp: Int) {

  def pro(implicit database: Database) = Team(teamidPro)

  def opp(implicit database: Database) = Team(teamidOpp)

  def externalRepresentation(implicit database: Database) =
    PairingExternalRepresentation(roundNumber, teamidPro, pro.name, teamidOpp, opp.name)

}

object Pairing {

  implicit val rw: json.ReadWriter[Pairing] = json.macroRW

  def getAll(tabid: Int)(implicit database: Database): List[Pairing] = {
    val tab = Tab(tabid) // throw error if tab doesn't exist.
    val connection = database.getConnection()
    val queryText = "SELECT * FROM pairings WHERE tabid = ?"
    val query = connection.prepareStatement(queryText)
    query.setInt(1, tab.id)
    val queryResult = query.executeQuery()
    connection.close()
    def iter(pairings: List[Pairing]) : List[Pairing] =
      if (queryResult.next()) {
        val id = queryResult.getInt("id")
        val tabid = queryResult.getInt("tabid")
        val roundNumber = queryResult.getInt("roundno")
        val teamidPro = queryResult.getInt("pro")
        val teamidOpp = queryResult.getInt("opp")
        iter(Pairing(id, tabid, roundNumber, teamidPro, teamidOpp) :: pairings)
      } else pairings.reverse
    iter(Nil)
  }

  def getAllForRound(tabid: Int,
      roundNumber: Int)(implicit database: Database): List[Pairing] = 
    getAll(tabid).filter(pairing => pairing.roundNumber == roundNumber)
  
  def getAllForTeam(team: Team)(implicit database: Database): List[Pairing] =
    getAll(team.tabid).filter(pairing =>
      pairing.teamidPro == team.id || pairing.teamidOpp == team.id)

}

case class PairingExternalRepresentation(
  roundNumber: Int,
  teamidPro: Int,
  pro: String,
  teamidOpp: Int,
  opp: String) 

object PairingExternalRepresentation {
  implicit val rw: json.ReadWriter[PairingExternalRepresentation] = json.macroRW
}