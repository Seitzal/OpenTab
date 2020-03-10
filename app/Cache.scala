package opentab

import opentab.models._

import scala.collection.concurrent.TrieMap
import play.api.db.Database
import scala.concurrent.{Future, ExecutionContext}
import upickle.{default => json}

case class CacheState(
  loadedForTab: Map[(Int, String), Boolean],
  loadedForTeam: Map[(Int, String), Boolean],
  teams: Map[Int, Team],
  previousOpponents: Map[Int, List[Int]],
  sideTendencies: Map[Int, Int]
)

object CacheState {
  implicit val rw: json.ReadWriter[CacheState] = json.macroRW
}

object Cache {

  case object loadedForTab {
    val state = new TrieMap[(Int, String), Boolean]
    def getState = state
    def apply(tabid: Int, what: String) = state.getOrElse((tabid, what), false)
    def set(tabid: Int, what: String) = state.putIfAbsent((tabid, what), true)
  }

  case object loadedForTeam {
    val state = new TrieMap[(Int, String), Boolean]
    def apply(teamid: Int, what: String) = state.getOrElse((teamid, what), false)
    def set(teamid: Int, what: String) = state.putIfAbsent((teamid, what), true)
  }

  val teams = new TrieMap[Int, Team]
  val previousOpponents = new TrieMap[Int, List[Int]]
  val sideTendencies = new TrieMap[Int, Int]

  def snapshot = CacheState(
    loadedForTab.state.toMap,
    loadedForTeam.state.toMap,
    teams.toMap,
    previousOpponents.toMap,
    sideTendencies.toMap
  )

  def flush() : Unit = {
    loadedForTab.state.clear()
    loadedForTab.state.clear()
    teams.clear()
    previousOpponents.clear()
    sideTendencies.clear()
  }
}