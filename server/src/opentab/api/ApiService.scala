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

      case rq @ POST -> Root / "tab" / IntVar(tabId) / "team" =>
        team.post(rq, tabId)

      case rq @ DELETE -> Root / "team" / IntVar(teamId) =>
        team.delete(rq, teamId)

      case rq @ PATCH -> Root / "team" / IntVar(teamId) =>
        team.patch(rq, teamId)

    }
  }

  override def processExpected: PartialFunction[Throwable,IO[Response[IO]]] = {
    case UnexpectedEnd => NotFound("Not found")
    case ex: MalformedMessageBodyFailure => BadRequest(ex.getMessage)
    case ex: InvalidMessageBodyFailure => BadRequest(ex.getMessage)
  }
  
}