package opentab.server

import org.http4s._
import cats.MonadError
import upickle.default.{Reader, read}
import upickle.core.AbortException

/**
 * Generic [[org.http4s.EntityDecoder]] for any type for which an implicit
 * [[upickle.default.Reader]] is in scope.
 * @tparam F The monadic type which is used for wrapping requests/responses.
 *           Usually, this will be [[cats.effect.IO]].
 * @tparam A The type of resource which this decoder is supposed to read.
 */
final class UPickleEntityDecoder[F[_], A: Reader]
    (implicit me: MonadError[F, Throwable], se: EntityDecoder[F, String])
    extends EntityDecoder[F, A] {

  override def decode(m: Media[F], strict: Boolean) = {
    val tryDecode =
      me.map(m.as[String])(s => read[A](ujson.Readable.fromString(s)))
    DecodeResult(me.map(me.attempt(tryDecode)){
      case Right(v) => Right(v)
      case Left(ex: AbortException) =>
        Left(MalformedMessageBodyFailure("malformed data", Some(ex)))
      case Left(ex) =>
        Left(InvalidMessageBodyFailure("invalid json", Some(ex)))
    })
  }

  override def consumes: Set[MediaRange] = Set(MediaType.application.json)

}
