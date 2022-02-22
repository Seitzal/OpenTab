package opentab.gensrv

import ujson._
import cats._
import cats.effect._
import cats.implicits._
import doobie._
import doobie.implicits._
import doobie.hikari._
import org.http4s._
import org.http4s.implicits._
import org.http4s.dsl.io._
import org.http4s.server.middleware.CORS
import org.http4s.blaze.server.BlazeServerBuilder
import eu.seitzal.http4s_upickle._
import com.typesafe.scalalogging.LazyLogging
import com.typesafe.config.{ConfigFactory, Config}
import scala.collection.mutable.ArrayBuffer
import java.io.File

abstract class GenService extends IOApp with LazyLogging {

  final lazy val defaultConfig =
    ConfigFactory.parseFile(new File("config.json"))

  def config: Config =
    defaultConfig

  def transactorResource: Resource[IO, Transactor[IO]] =
    defaultTransactorResource

  final def defaultTransactorResource: Resource[IO, HikariTransactor[IO]] =
    for {
      ce <- ExecutionContexts.fixedThreadPool[IO](config.getInt("db.connectPoolSize"))
      xa <- HikariTransactor.newHikariTransactor[IO](
              "org.postgresql.Driver",
              "jdbc:postgresql://" +
                config.getString("db.host") +
                ":" + config.getInt("db.port") +
                "/" + config.getString("db.name"),
              config.getString("db.user"),
              config.getString("db.pw"),
              ce
            )
    } yield xa

  def routes: (Transactor[IO], Config) => 
    PartialFunction[Request[IO], IO[Response[IO]]]

  def processExpected = new PartialFunction[Throwable, IO[Response[IO]]] {
    override def apply(v1: Throwable): IO[Response[IO]] = ???
    override def isDefinedAt(x: Throwable): Boolean = false
  }

  private def applyProcessExpected(io: IO[Response[IO]]) =
    io.recoverWith(processExpected)

  def processUnexpected(io: IO[Response[IO]]) =
    io.handleErrorWith {
      ex: Throwable =>
      logger.info(s"Uncaught error while processing request: ${ex.getClass.getName}: ${ex.getMessage}", ex)
      if (config.getBoolean("server.debug")) {
        InternalServerError(Obj(
          "message" -> 
            Str(ex.getMessage),
          "type" -> 
            Str(ex.getClass.toString),
          "stackTrace" -> 
            Arr(ArrayBuffer.from(ex.getStackTrace().map(el => Str(el.toString))))
        ))
      } else InternalServerError("Internal server error")
    }

  private def routesFinal(xa: Transactor[IO], config: Config) = {
    val routesInjected = routes(xa, config)
    HttpRoutes.of(new PartialFunction[Request[IO], IO[Response[IO]]] {
      def apply(v1: Request[IO]) =
        processUnexpected(applyProcessExpected(routesInjected(v1)))
      def isDefinedAt(x: Request[IO]) =
        routesInjected.isDefinedAt(x)
    }).orNotFound
  }

  override def run(args: List[String]): IO[ExitCode] =
    transactorResource.use { xa =>
      BlazeServerBuilder[IO]
        .bindHttp(config.getInt("server.port"), config.getString("server.host"))
        .withHttpApp(CORS(routesFinal(xa, config)))
        .serve
        .compile
        .drain
        .as(ExitCode.Success)
    }

}