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
    ;{

      case GET -> Root =>
        Ok("OpenTab API server")

      case rq @ GET -> Root / "token" =>
        getToken(rq)

      case rq @ GET -> Root / "tab" =>
        tab.getAll(rq)

      case rq @ GET -> Root / "tab" / IntVar(tabId) =>
        tab.get(rq, tabId)

      case rq @ POST -> Root / "tab" =>
        tab.post(rq)

      case rq @ PATCH -> Root / "tab" / IntVar(tabId) =>
        tab.patch(rq, tabId)

      case rq @ DELETE -> Root / "tab" / IntVar(tabId) =>
        tab.delete(rq, tabId)

    }
  }

  override def processExpected: PartialFunction[Throwable,IO[Response[IO]]] = {
    case UnexpectedEnd => NotFound("Not found")
    case ex: MalformedMessageBodyFailure => BadRequest(ex.getMessage)
    case ex: InvalidMessageBodyFailure => BadRequest(ex.getMessage)
  }
  
}