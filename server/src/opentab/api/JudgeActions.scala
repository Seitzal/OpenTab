package opentab.api

import opentab._
import opentab.model._
import opentab.auth._
import cats.effect._
import cats.implicits._
import org.http4s._
import org.http4s.dsl.io._
import eu.seitzal.http4s_upickle._
import com.typesafe.config.Config
import ujson.Obj
import doobie.util.invariant.UnexpectedEnd

class JudgeActions(implicit xa: Xa, config: Config) {

  def getAllForTab(rq: Request[IO], tabId: Int) = withAuth(rq) { user =>
    Permissions(user.id, tabId)
      .map(_.view)
      .flatMap {
        case false => denied
        case true => Ok(Judge.getAllForTab(tabId))
      }
  }

  def post(rq: Request[IO], tabId: Int) = withAuth(rq) { user =>
    for {
      data      <- rq.as[JudgePartial]
      permitted <- Permissions(user.id, tabId).map(_.setup)
      re        <- if (permitted)
        Ok(Judge.create(
          tabId, data.firstName, data.lastName, data.rating, data.delegation))
        else denied
    } yield re
  }

  def delete(rq: Request[IO], judgeId: Int) = withAuth(rq) { user =>
    for {
      judge     <- Judge(judgeId)
      permitted <- Permissions(user.id, judge.tabId).map(_.setup)
      re        <- if (permitted) judge.delete.flatMap(_ => NoContent()) else denied
    } yield re
  }

  def patch(rq: Request[IO], judgeId: Int) = withAuth(rq) { user => 
    for {
      judge        <- Judge(judgeId)
      permitted    <- Permissions(user.id, judge.tabId).map(_.setup)
      data         <- rq.as[Obj]
      newFirstName <- IO(data.value.get("firstName").map(_.str))
      newLastName  <- IO(data.value.get("lastName").map(_.str))
      newRating    <- IO(data.value.get("rating").map(_.num.toInt))
      newIsActive  <- IO(data.value.get("isActive").map(_.bool))
      re           <- if(permitted)
        Ok(judge.update(newFirstName, newLastName, newRating, newIsActive))
        else denied
    } yield re
  }

  def getClashes(rq: Request[IO], judgeId: Int) = withAuth(rq) { user =>
    for {
      judge     <- Judge(judgeId)
      clashes   <- judge.getAllClashes
      permitted <- Permissions(user.id, judge.tabId).map(_.setup)
      re        <- if (permitted) Ok(clashes) else denied
    } yield re
  }

  def setClash(rq: Request[IO], judgeId: Int, teamId: Int, level: Int) = 
    withAuth(rq) { user =>
      for {
        judge     <- Judge(judgeId)
        team      <- Team(teamId)
        permitted <- Permissions(user.id, judge.tabId).map(_.setup)
        re        <-
          if(!permitted) denied
          else if (judge.tabId != team.tabId) 
            InternalServerError("Judge and team must be on the same tab.")
          else judge.setClash(team, level).flatMap(_ => NoContent())
      } yield re
    }

  def verifyKey(rq: Request[IO], judgeId: Int) =
    (for {
      judge <- Judge(judgeId)
      key   <- rq.as[Int]
      re    <- if (judge.key == key) Ok(true) else Ok(false)
    } yield re)
      .recoverWith {
        case UnexpectedEnd => Ok(false)
      }

}