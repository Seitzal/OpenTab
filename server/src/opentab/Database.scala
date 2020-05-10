package opentab

import doobie._
import doobie.implicits._
import doobie.util.ExecutionContexts
import cats._
import cats.effect._
import cats.implicits._
import com.typesafe.config.Config
import com.typesafe.scalalogging.LazyLogging

final class Database(conf: Config) extends LazyLogging {

  implicit val cs = IO.contextShift(ExecutionContexts.synchronous)

  val transactor = Transactor.fromDriverManager[IO](
    "org.postgresql.Driver",
    "jdbc:postgresql://" +
      conf.getString("db.host") +
      ":" + conf.getInt("db.port") +
      "/" + conf.getString("db.name"),
    conf.getString("db.user"),
    conf.getString("db.pw"),
    Blocker.liftExecutionContext(ExecutionContexts.synchronous)
  )

  def check: IO[Unit] =
    sql"SELECT schemaVersion FROM instance"
      .query[String]
      .unique
      .transact(transactor)
      .map(sv => logger.info("Database schema version: " + sv))

}