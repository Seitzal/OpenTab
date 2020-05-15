package opentab

import cats._
import cats.effect._
import cats.implicits._
import org.http4s._
import org.http4s.dsl.io._
import doobie._
import doobie.implicits._
import upickle.default._
import gensrv.GenService
import com.typesafe.config.Config
import doobie.util.invariant.UnexpectedEnd

object ApiService extends GenService {

  def routes = (xa, config) => { implicit val xa_ = xa; {

    case GET -> Root =>
      Ok("OpenTab API server")

    case GET -> Root / "user" / IntVar(userId) =>
      Ok(User(userId))

  }}

  override def processExpected: PartialFunction[Throwable,IO[Response[IO]]] = {
    case UnexpectedEnd => NotFound("Not found")
  }
  
}