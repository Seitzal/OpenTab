package opentab

import org.http4s._
import org.http4s.dsl.io._
import cats.effect.IO
import fs2.Stream

trait HTTPHelpers {

  def unauthorized(msg: String) = IO(Response[IO](
    status = Status.Unauthorized, 
    body = Stream.emits("Unauthorized".getBytes())
  ))

  val unauthorized: IO[Response[IO]] = unauthorized("Unauthorized")

  val denied: IO[Response[IO]] = Forbidden("Permission denied")

}