package opentab.api

import opentab.auth._
import opentab.model._
import opentab.json._
import opentab.server._

import cats.effect.IO
import cats.implicits._
import org.http4s._
import org.http4s.dsl.io._
import com.typesafe.config.Config
import ujson.Obj

class TeamActions(implicit xa: Xa, config: Config) {

  def getAllForTab(rq: Request[IO], tabId: Int) = withAuthOpt(rq) {
    case Some(user) =>
      Permissions(user.id, tabId)
        .map(_.view)
        .flatMap {
          case false => denied
          case true => Ok(Team.getAllForTab(tabId))
        }
    case None =>
      Tab(tabId)
        .flatMap(_.isPublic)
        .flatMap {
          case false => unauthorized
          case true => Ok(Team.getAllForTab(tabId))
        }
  }

  def getAllDelegationsForTab(rq: Request[IO], tabId: Int) = withAuthOpt(rq) {
    case Some(user) =>
      Permissions(user.id, tabId)
        .map(_.view)
        .flatMap {
          case false => denied
          case true =>
            Ok(Team.getAllForTab(tabId).map(_.map(_.delegation).distinct))
        }
    case None =>
      Tab(tabId)
        .flatMap(_.isPublic)
        .flatMap {
          case false => unauthorized
          case true =>
            Ok(Team.getAllForTab(tabId).map(_.map(_.delegation).distinct))
        }
  }

  def post(rq: Request[IO], tabId: Int) = withAuth(rq) { user =>
    for {
      data      <- rq.as[TeamPartial]
      permitted <- Permissions(user.id, tabId).map(_.setup)
      re        <- if (permitted)
        Ok(Team.create(tabId, data.name, data.delegation, data.status.toInt))
        else denied
    } yield re
  }

  def delete(rq: Request[IO], teamId: Int) = withAuth(rq) { user =>
    for {
      team      <- Team(teamId)
      permitted <- Permissions(user.id, team.tabId).map(_.setup)
      re        <- if (permitted) team.delete.flatMap(_ => NoContent()) else denied
    } yield re
  }

  def patch(rq: Request[IO], teamId: Int) = withAuth(rq) { user =>
    for {
      team          <- Team(teamId)
      permitted     <- Permissions(user.id, team.tabId).map(_.setup)
      data          <- rq.as[Obj]
      newName       <- IO(data.value.get("name").map(_.str))
      newDelegation <- IO(data.value.get("delegation").map(_.str))
      newStatus     <- IO(data.value.get("status").map(_.num.toInt))
      newIsActive   <- IO(data.value.get("isActive").map(_.bool))
      re            <- if(permitted)
        Ok(team.update(newName, newDelegation, newStatus, newIsActive))
        else denied
    } yield re
  }

}