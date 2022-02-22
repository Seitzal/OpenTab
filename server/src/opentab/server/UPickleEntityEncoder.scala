package opentab.server

import org.http4s._
import org.http4s.headers._
import fs2.Stream
import fs2.text.utf8
import upickle.default.{Writer, write}

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
