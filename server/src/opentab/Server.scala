package opentab

import cats.implicits._
import cats.effect._
import com.typesafe.config.ConfigFactory
import java.io.File
import org.http4s.server.middleware.CORS
import org.http4s.server.blaze.BlazeServerBuilder
import shapeless.Data
import com.typesafe.scalalogging.LazyLogging

object Server extends IOApp with LazyLogging {

  val conf = ConfigFactory.parseFile(new File("config.json"))

  val db = new Database(conf)

  def host: IO[ExitCode] =
    BlazeServerBuilder[IO]
        .bindHttp(conf.getInt("server.port"), conf.getString("server.host"))
        .withHttpApp(CORS(new Api(conf, db).toService))
        .serve
        .compile
        .drain
        .as(ExitCode.Success)
 
  def run(args: List[String]): IO[ExitCode] =
    (db.check *> host)
      .handleError {
        err => logger.error(err.getLocalizedMessage(), err)
        ExitCode.Error
      }

}
