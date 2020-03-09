package opentab.models

import scala.collection.concurrent.TrieMap
import play.api.db.Database
import scala.concurrent.{Future, ExecutionContext}

object Cache {

  object loadedForTab {
    private val state = new TrieMap[(Int, String), Boolean]
    def apply(tab: Int, what: String) = state.getOrElse((tab, what), false)
    def set(tab: Int, what: String) = state.putIfAbsent((tab, what), true)
  }

  val teams = new TrieMap[Int, Team]

}