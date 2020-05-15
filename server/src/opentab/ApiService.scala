package opentab

import opentab.auth._

import cats._
import cats.effect._
import cats.implicits._
import org.http4s._
import org.http4s.dsl.io._
import doobie._
import doobie.implicits._
import upickle.default._
import eu.seitzal.http4s_upickle._
import gensrv.GenService
import com.typesafe.config.Config
import doobie.util.invariant.UnexpectedEnd

object ApiService extends GenService {

  def routes = (xa, config) => { implicit val xa_ = xa; implicit val c = config; {

    case GET -> Root =>
      Ok("OpenTab API server")

    case rq @ GET -> Root / "token" =>
      getToken(rq)

    case rq @ GET -> Root / "token" / "verify" =>
      withAuth(rq)(Ok(_))

    case rq @ GET -> Root / "token" / "verifyOpt" =>
      withAuthOpt(rq)(Ok(_))

  }}

  override def processExpected: PartialFunction[Throwable,IO[Response[IO]]] = {
    case UnexpectedEnd => NotFound("Not found")
  }
  
}