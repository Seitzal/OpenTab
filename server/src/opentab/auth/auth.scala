package opentab

import opentab.model._
import opentab.server._

import org.http4s._
import org.http4s.headers._
import org.http4s.dsl.io._
import cats.effect.IO
import cats.implicits._
import pdi.jwt._
import pdi.jwt.exceptions._
import com.typesafe.config.Config
import com.typesafe.scalalogging.LazyLogging
import upickle.core.AbortException
import upickle.default.read

import java.util.Base64
import scala.util.Try

package object auth extends LazyLogging {

  def getBasicCreds(rq: Request[IO]): IO[String] =
    rq.headers
      .get[`Authorization`]
      .map(_.credentials.renderString)
      match {
        case Some(s"Basic $creds") => IO(creds)
        case _ => IO.raiseError(new Error("Malformed or missing auth header"))
      }

  def decodeBasicCreds(creds: String): IO[(String, String)] =
    IO(new String(
      Base64.getDecoder().decode(creds.getBytes("UTF-8"))
    )).flatMap {
      case s"$username:$password" => IO((username, password))
      case _ => IO.raiseError(new Error("Invalid base64"))
    }

  def checkBasicCreds(creds: (String, String))(implicit xa: Xa): IO[Boolean] =
    User(creds._1)
      .flatMap(_.checkPassword(creds._2))

  def getToken
      (rq: Request[IO])
      (implicit xa: Xa, config: Config): IO[Response[IO]] = {
    val duration = config.getInt("server.tokenLife") match {
      case 0 => None
      case n => Some(n)
    }
    val secret = config.getString("server.secret")
    val io = for {
      credsEncoded <- getBasicCreds(rq)
      credsDecoded <- decodeBasicCreds(credsEncoded)
      credsCorrect <- checkBasicCreds(credsDecoded)
      user         <- User(credsDecoded._1)
      result       <- credsCorrect match {
        case true  => Ok(IO(user.issueToken(duration, secret)))
        case false => IO.raiseError(new Error("Incorrect credentials"))}
    } yield result
    io.handleError{ e =>
      logger.info(s"Failed login attempt from ${rq.from.get}", e)
      Response(status = Unauthorized)
    }
  }

  def verifyToken(token: String)(implicit config: Config): Try[User] =
    JwtUpickle.decodeJson(
      token = token, 
      key = config.getString("server.secret"),
      algorithms = Seq(JwtAlgorithm.HS256))
    .map(r => read[User](r, false))

  def checkToken(rq: Request[IO])(implicit config: Config): IO[Option[User]] =
    rq.headers
      .get[`Authorization`]
      .map(_.credentials.renderString)
      match {
        case Some(s"Bearer $token") => 
          IO.fromTry(verifyToken(token))
            .map(Some(_))
        case Some(_) => IO.raiseError(new Error("Invalid authorization header."))
        case None => IO(None)
      }

  def withAuth
      (rq: Request[IO])
      (f: User => IO[Response[IO]])
      (implicit config: Config): IO[Response[IO]] =
    checkToken(rq).flatMap {
      case None => unauthorized("Unauthorized")
      case Some(user) => f(user)
    }.recoverWith {
      case _: JwtLengthException | 
          _: IllegalArgumentException |
          _: JwtValidationException |
          _: AbortException =>
        BadRequest("Invalid token")
      case _: JwtExpirationException =>
        unauthorized("Expired token")
    }

  def withAuthOpt
      (rq: Request[IO])
      (f: Option[User] => IO[Response[IO]])
      (implicit config: Config): IO[Response[IO]] =
    checkToken(rq)
      .flatMap(f)
      .recoverWith {
        case _: JwtLengthException | 
            _: IllegalArgumentException |
            _: JwtValidationException |
            _: AbortException =>
          BadRequest("Invalid token")
        case _: JwtExpirationException =>
          unauthorized("Expired token")
      }

}
