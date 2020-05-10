package opentab

import org.http4s._
import org.http4s.implicits._
import org.http4s.dsl.io._
import ujson._
import eu.seitzal.http4s_upickle.{UPickleEntityDecoder, UPickleEntityEncoder}
import doobie._
import doobie.implicits._
import doobie.util.invariant._
import cats._
import cats.effect._
import cats.implicits._
import com.typesafe.config.Config
import com.typesafe.scalalogging.LazyLogging
import scala.collection.mutable.ArrayBuffer

final class Api(conf: Config, db: DB) extends LazyLogging {

  val routes: PartialFunction[Request[IO], IO[Response[IO]]] = {

    case GET -> Root =>
      Ok("OpenTab 2 API server")

    case GET -> Root / "user" / name =>
      Ok(User(name).transact(db.t))

    case rq @ POST -> Root / "user" =>
      Ok(
        for {
          data     <- rq.as[Obj]
          name     <- IO.delay(data("name").str)
          password <- IO.delay(data("password").str)
          email    <- IO.delay(data("email").str)
          user     <- User.create(name, password, email, false).transact(db.t)
        } yield user
      )

    case DELETE -> Root / "user" / name =>
      User(name)
        .flatMap(_.delete)
        .transact(db.t)
        .flatMap(_ => NoContent())

    case rq @ PATCH -> Root / "user" / name =>
      Ok(
        for {
          data        <- rq.as[Obj]
          user        <- User(name).transact(db.t)
          newPassword <- IO.delay(data.value.get("password").map(_.str))
          newEmail    <- IO.delay(data.value.get("email").map(_.str))
          newIsAdmin  <- IO.delay(data.value.get("isAdmin").map(_.bool))
          newUser     <- user.update(newPassword, newEmail, newIsAdmin).transact(db.t)
        } yield newUser
      )
      
  }

  implicit val jsonEE: EntityEncoder[IO, Obj] = new UPickleEntityEncoder
  implicit val jsonED: EntityDecoder[IO, Obj] = new UPickleEntityDecoder
  
  def processExpected(io: IO[Response[IO]]) =
    io.recoverWith {
      case UnexpectedEnd => NotFound("Not found")
    }
  
  def processUnexpected(io: IO[Response[IO]]) =
    io.handleErrorWith {
      ex: Throwable =>
      logger.debug("Unexpected", ex)
      if (conf.getBoolean("server.debug")) {
        InternalServerError(Obj(
          "message" -> Str(ex.getMessage),
          "type" -> Str(ex.getClass.toString),
          "stackTrace" -> Arr(ArrayBuffer.from(ex.getStackTrace().map(el => Str(el.toString))))
        ))
      } else InternalServerError(ex.getLocalizedMessage())
    }

  val routesWithErrorProcessing = 
    new PartialFunction[Request[IO], IO[Response[IO]]] {
      def apply(v1: Request[IO]) = 
        processUnexpected(processExpected(routes(v1)))
      def isDefinedAt(x: Request[IO]) =
        routes.isDefinedAt(x)
    }

  def toService =
    HttpRoutes.of(routesWithErrorProcessing).orNotFound

}