package opentab

import org.http4s._
import org.http4s.headers._
import upickle.default._
import upickle.core._
import cats.MonadError
import fs2.Stream
import fs2.text.utf8

/**
 * Offers easy interoperability between http4s and uPickle by providing generic
 * implicit instances of [[org.http4s.EntityEncoder]] and 
 * [[org.http4s.EntityDecoder]].
 */
package object json {

  /** Spawns an [[org.http4s.EntityEncoder]] for any uPickle-writable type */
  implicit def uPickleEntityEncoder[F[_], A: Writer]: EntityEncoder[F, A] =
    new UPickleEntityEncoder

  /** Spawns an [[org.http4s.EntityDecoder]] for any uPickle-readable type */
  implicit def uPickleEntityDecoder[F[_], A: Reader]
      (implicit me: MonadError[F, Throwable], se: EntityDecoder[F, String])
      : EntityDecoder[F, A] =
    new UPickleEntityDecoder

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

  /**
   * Generic [[org.http4s.EntityEncoder]] for any type for which an implicit
   * [[upickle.default.Writer]] is in scope.
   * @tparam F The monadic type which is used for wrapping requests/responses.
   *           Usually, this will be [[cats.effect.IO]].
   * @tparam A The type of resource which this encoder is supposed to write.
   */
  final class UPickleEntityEncoder[F[_], A: Writer] 
      extends EntityEncoder[F, A] {

    override def toEntity(a: A) =
      Entity(Stream(write(a)).through(utf8.encode))

    override def headers = 
      Headers(
        `Content-Type`(
          MediaType.application.json,
          Charset.`UTF-8`))

  }

}
