package opentab.api

import opentab._
import opentab.auth._
import org.http4s._
import org.http4s.dsl.io._
import cats.effect.IO
import gensrv.GenService
import doobie.util.invariant.UnexpectedEnd

object ApiService extends GenService {

  def routes = (xa, config) => { 
    implicit val xa_ = xa
    implicit val c = config
    val tab = new TabActions
    val team = new TeamActions
    val speaker = new SpeakerActions
    val judge = new JudgeActions
    ;{

      case GET -> Root =>
        Ok("OpenTab API server")

      case rq @ GET -> Root / "token" =>
        getToken(rq)

      case rq @ GET -> Root / "tabs" =>
        tab.getAll(rq)
      
      case rq @ GET -> Root / "tabs" / "permissions" =>
        tab.getAllPermissions(rq)

      case rq @ GET -> Root / "tab" / IntVar(tabId) =>
        tab.get(rq, tabId)

      case rq @ POST -> Root / "tab" =>
        tab.post(rq)

      case rq @ PATCH -> Root / "tab" / IntVar(tabId) =>
        tab.patch(rq, tabId)

      case rq @ DELETE -> Root / "tab" / IntVar(tabId) =>
        tab.delete(rq, tabId)

      case rq @ GET -> Root / "tab" / IntVar(tabId) / "permissions" =>
        tab.getPermissions(rq, tabId)

      case rq @ GET -> Root / "tab" / IntVar(tabId) / "teams" =>
        team.getAllForTab(rq, tabId)

      case rq @ GET -> Root / "tab" / IntVar(tabId) / "delegations" =>
        team.getAllDelegationsForTab(rq, tabId)

      case rq @ GET -> Root / "tab" / IntVar(tabId) / "speakers" =>
        speaker.getAllForTab(rq, tabId)

      case rq @ GET -> Root / "tab" / IntVar(tabId) / "judges" =>
        judge.getAllForTab(rq, tabId)

      case rq @ POST -> Root / "tab" / IntVar(tabId) / "team" =>
        team.post(rq, tabId)

      case rq @ DELETE -> Root / "team" / IntVar(teamId) =>
        team.delete(rq, teamId)

      case rq @ PATCH -> Root / "team" / IntVar(teamId) =>
        team.patch(rq, teamId)

      case rq @ GET -> Root / "team" / IntVar(teamId) / "speakers" =>
        speaker.getAllForTeam(rq, teamId)

      case rq @ POST -> Root / "team" / IntVar(teamId) / "speaker" =>
        speaker.post(rq, teamId)

      case rq @ DELETE -> Root / "speaker" / IntVar(speakerId) =>
        speaker.delete(rq, speakerId)

      case rq @ PATCH -> Root / "speaker" / IntVar(speakerId) =>
        speaker.patch(rq, speakerId)

      case rq @ POST -> Root / "tab" / IntVar(judgeId) / "judge" =>
        judge.post(rq, judgeId)

      case rq @ DELETE -> Root / "judge" / IntVar(judgeId) =>
        judge.delete(rq, judgeId)

      case rq @ PATCH -> Root / "judge" / IntVar(judgeId) =>
        judge.patch(rq, judgeId)

      case rq @ GET -> Root / "judge" / IntVar(judgeId) / "clashes" =>
        judge.getClashes(rq, judgeId)

      case rq @ POST -> Root / "judge" / IntVar(judgeId) / "clashes" / IntVar(teamId) / IntVar(level) =>
        judge.setClash(rq, judgeId, teamId, level)

      case rq @ POST -> Root / "judge" / IntVar(judgeId) / "verify-key" =>
        judge.verifyKey(rq, judgeId)
    }
  }

  override def processExpected: PartialFunction[Throwable,IO[Response[IO]]] = {
    case UnexpectedEnd => NotFound("Not found")
    case ex: MalformedMessageBodyFailure => BadRequest(ex.getMessage)
    case ex: InvalidMessageBodyFailure => BadRequest(ex.getMessage)
  }
  
}