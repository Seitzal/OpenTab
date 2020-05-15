import doobie.util.transactor.Transactor
import cats.effect.IO
import org.http4s._
import org.http4s.dsl.io._
import fs2.Stream

package object opentab {
  type Xa = Transactor[IO]

  def unauthorized(msg: String) = IO.delay(Response[IO](
    status = Status.Unauthorized, 
    body = Stream.emits("Unauthorized".getBytes())
  ))

  val unauthorized: IO[Response[IO]] = unauthorized("Unauthorized")
  val denied: IO[Response[IO]] = Forbidden("Permission denied")

}
