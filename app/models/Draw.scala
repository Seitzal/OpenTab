package eu.seitzal.opentab.models

import eu.seitzal.opentab._
import shortcuts._
import upickle.{default => json}

case class Draw(
  pairings: List[(Team, Team)],
  teamOnBye: Option[Team])

object Draw {
  implicit val rw: json.ReadWriter[Draw] = json.macroRW
}