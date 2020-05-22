package opentab.api

import opentab._
import opentab.auth._
import cats.effect._
import cats.implicits._
import org.http4s._
import org.http4s.dsl.io._
import eu.seitzal.http4s_upickle._
import com.typesafe.config.Config
import ujson.Obj

class SpeakerActions(implicit xa: Xa, config: Config) {

  def getAllForTab(rq: Request[IO], tabId: Int) = withAuthOpt(rq) {
    case Some(user) =>
      Permissions(user.id, tabId)
        .map(_.view)
        .flatMap {
          case false => denied
          case true => Ok(Speaker.getAllForTab(tabId))
        }
    case None => 
      Tab(tabId)
        .map(_.isPublic)
        .flatMap {
          case false => unauthorized
          case true => Ok(Speaker.getAllForTab(tabId))
        }
  }

  def getAllForTeam(rq: Request[IO], teamId: Int) = withAuthOpt(rq) {
    case Some(user) =>
      Team(teamId)
        .flatMap(team => Permissions(user.id, team.tabId))
        .map(_.view)
        .flatMap {
          case false => denied
          case true => Ok(Speaker.getAllForTeam(teamId))
        }
    case None => 
      Team(teamId)
        .flatMap(team => Tab(team.tabId))
        .map(_.isPublic)
        .flatMap {
          case false => unauthorized
          case true => Ok(Speaker.getAllForTeam(teamId))
        }
  }

  def post(rq: Request[IO], teamId: Int) = withAuth(rq) { user =>
    for {
      data      <- rq.as[SpeakerPartial]
      team      <- Team(teamId)
      permitted <- Permissions(user.id, team.tabId).map(_.setup)
      re        <- if (permitted)
        Ok(Speaker.create(team.tabId, teamId, data.firstName, data.lastName, data.status.toInt))
        else denied
    } yield re
  }

  def delete(rq: Request[IO], speakerId: Int) = withAuth(rq) { user =>
    for {
      speaker   <- Speaker(speakerId)
      permitted <- Permissions(user.id, speaker.tabId).map(_.setup)
      re        <- if (permitted) speaker.delete.flatMap(_ => NoContent()) else denied
    } yield re
  }

  def patch(rq: Request[IO], speakerId: Int) = withAuth(rq) { user => 
    for {
      speaker      <- Speaker(speakerId)
      permitted    <- Permissions(user.id, speaker.tabId).map(_.setup)
      data         <- rq.as[Obj]
      newFirstName <- IO(data.value.get("firstName").map(_.str))
      newLastName  <- IO(data.value.get("lastName").map(_.str))
      newStatus    <- IO(data.value.get("status").map(_.num.toInt))
      re           <- if(permitted)
        Ok(speaker.update(newFirstName, newLastName, newStatus))
        else denied
    } yield re
  }

}