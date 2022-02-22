package opentab.server

import upickle.default.{Writer, Reader}
import org.http4s.{EntityEncoder, EntityDecoder}
import cats.MonadError

/**
 * Offers easy interoperability between http4s and uPickle by providing generic
 * implicit instances of [[org.http4s.EntityEncoder]] and 
 * [[org.http4s.EntityDecoder]].
 */
trait JSONHelpers {

  /** Spawns an [[org.http4s.EntityEncoder]] for any uPickle-writable type */
  implicit def uPickleEntityEncoder[F[_], A: Writer]: EntityEncoder[F, A] =
    new UPickleEntityEncoder

  /** Spawns an [[org.http4s.EntityDecoder]] for any uPickle-readable type */
  implicit def uPickleEntityDecoder[F[_], A: Reader]
      (implicit me: MonadError[F, Throwable], se: EntityDecoder[F, String])
      : EntityDecoder[F, A] =
    new UPickleEntityDecoder

}
