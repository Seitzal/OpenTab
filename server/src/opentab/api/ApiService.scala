package opentab.api

import opentab._
import opentab.auth._
import opentab.server._
import opentab.model._
import org.http4s._
import org.http4s.dsl.io._
import cats.effect.IO
import doobie.util.invariant.UnexpectedEnd

object ApiService extends Service {

  def routes = (xa, config) => {
    implicit val xa_ = xa
    implicit val c = config
    val tab = new TabActions
    val team = new TeamActions
    val speaker = new SpeakerActions
    val judge = new JudgeActions
    val round = new RoundActions
    ;{

      case GET -> Root =>
        Ok("OpenTab API server")

      case rq @ GET -> Root / "token" =>
        getToken(rq)

      // TAB ACTIONS

      // get all tabs
      case rq @ GET -> Root / "tabs" =>
        tab.getAll(rq)

      // get permissions for all tabs
      case rq @ GET -> Root / "tabs" / "permissions" =>
        tab.getAllPermissions(rq)

      // get one tab
      case rq @ GET -> Root / "tab" / IntVar(tabId) =>
        tab.get(rq, tabId)

      // create a new tab
      case rq @ POST -> Root / "tab" =>
        tab.post(rq)

      // rename a tab
      case rq @ PATCH -> Root / "tab" / IntVar(tabId) =>
        tab.rename(rq, tabId)

      // delete a tab
      case rq @ DELETE -> Root / "tab" / IntVar(tabId) =>
        tab.delete(rq, tabId)

      // get permissions for a tab
      case rq @ GET -> Root / "tab" / IntVar(tabId) / "permissions" =>
        tab.getPermissions(rq, tabId)

      // get settings for a tab
      case rq @ GET -> Root / "tab" / IntVar(tabId) / "settings" =>
        tab.getSettings(rq, tabId)

      // update settings for a tab
      case rq @ PATCH -> Root / "tab" / IntVar(tabId) / "settings" =>
        tab.updateSettings(rq, tabId)

      // TEAM ACTIONS

      // get teams on a tab
      case rq @ GET -> Root / "tab" / IntVar(tabId) / "teams" =>
        team.getAllForTab(rq, tabId)

      // get delegations on a tab
      case rq @ GET -> Root / "tab" / IntVar(tabId) / "delegations" =>
        team.getAllDelegationsForTab(rq, tabId)

      // register a new team
      case rq @ POST -> Root / "tab" / IntVar(tabId) / "team" =>
        team.post(rq, tabId)

      // delete a team
      case rq @ DELETE -> Root / "team" / IntVar(teamId) =>
        team.delete(rq, teamId)

      // update properties for a team
      case rq @ PATCH -> Root / "team" / IntVar(teamId) =>
        team.patch(rq, teamId)

      // get speakers on a team
      case rq @ GET -> Root / "team" / IntVar(teamId) / "speakers" =>
        speaker.getAllForTeam(rq, teamId)

      // SPEAKER ACTIONS

      // get speakers on a tab
      case rq @ GET -> Root / "tab" / IntVar(tabId) / "speakers" =>
        speaker.getAllForTab(rq, tabId)

      // register a new speaker
      case rq @ POST -> Root / "team" / IntVar(teamId) / "speaker" =>
        speaker.post(rq, teamId)

      // delete a speaker
      case rq @ DELETE -> Root / "speaker" / IntVar(speakerId) =>
        speaker.delete(rq, speakerId)

      // update properties for a speaker
      case rq @ PATCH -> Root / "speaker" / IntVar(speakerId) =>
        speaker.patch(rq, speakerId)

      // JUDGE ACTIONS

      // get judges on a tab
      case rq @ GET -> Root / "tab" / IntVar(tabId) / "judges" =>
        judge.getAllForTab(rq, tabId)

      // register a new judge
      case rq @ POST -> Root / "tab" / IntVar(judgeId) / "judge" =>
        judge.post(rq, judgeId)

      // delete a judge
      case rq @ DELETE -> Root / "judge" / IntVar(judgeId) =>
        judge.delete(rq, judgeId)

      // update properties for a judge
      case rq @ PATCH -> Root / "judge" / IntVar(judgeId) =>
        judge.patch(rq, judgeId)

      // get clashes for a judge
      case rq @ GET -> Root / "judge" / IntVar(judgeId) / "clashes" =>
        judge.getClashes(rq, judgeId)

      // set clash for a judge against a team
      case rq @ POST -> Root / "judge" / IntVar(judgeId) / "clashes" / IntVar(teamId) / IntVar(level) =>
        judge.setClash(rq, judgeId, teamId, level)

      // verify personal key for a judge
      case rq @ POST -> Root / "judge" / IntVar(judgeId) / "verify-key" =>
        judge.verifyKey(rq, judgeId)

      // ROUND ACTIONS

      // get rounds for a tab
      case rq @ GET -> Root / "tab" / IntVar(tabId) / "rounds" =>
        round.getAllForTab(rq, tabId)

      // add a round to a tab
      case rq @ POST -> Root / "tab" / IntVar(tabId) / "round" =>
        round.post(rq, tabId)

      // delete a round from a tab
      case rq @ DELETE -> Root / "tab" / IntVar(tabId) / "round" / IntVar(roundNo) =>
        round.delete(rq, tabId, roundNo)

      // lock a round
      case rq @ PATCH -> Root / "tab" / IntVar(tabId) / "round" / IntVar(roundNo) / "lock" =>
        round.lock(rq, tabId, roundNo)

      // unlock a round
      case rq @ PATCH -> Root / "tab" / IntVar(tabId) / "round" / IntVar(roundNo) / "unlock" =>
        round.unlock(rq, tabId, roundNo)

    }
  }

  // standard handling procedures / response codes for expected runtime errors
  override def processExpected: PartialFunction[Throwable,IO[Response[IO]]] = {
    case UnexpectedEnd => NotFound("Not found")
    case ex: MalformedMessageBodyFailure => BadRequest(ex.getMessage)
    case ex: InvalidMessageBodyFailure => BadRequest(ex.getMessage)
    case ex: TabException => internalServerError(ex.getMessage)
  }

}