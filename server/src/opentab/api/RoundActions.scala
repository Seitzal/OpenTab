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

class RoundActions(implicit xa: Xa, config: Config) {

  def getAllForTab(rq: Request[IO], tabId: Int) = withAuthOpt(rq) {
    case Some(user) => Permissions(user.id, tabId).map(_.view).flatMap {
      case false => denied
      case true => Round.getAllForTab(tabId).flatMap(Ok(_))
    }
    case None =>
      Tab(tabId).flatMap(_.isPublic).flatMap {
        case true  => Round.getAllForTab(tabId).flatMap(Ok(_))
        case false => unauthorized
      }
  }

  def post(rq: Request[IO], tabId: Int) = withAuth(rq) { user =>
    Permissions(user.id, tabId).map(_.setup).flatMap {
      case false => denied
      case true => Round.add(tabId).flatMap(Ok(_))
    }
  }

  def delete(rq: Request[IO], tabId: Int, roundNo: Int) = withAuth(rq) {
    user =>
    Permissions(user.id, tabId).map(_.setup).flatMap {
      case false => denied
      case true => Round(tabId, roundNo).flatMap(_.delete).flatMap(_ => NoContent())
    }
  }

  def lock(rq: Request[IO], tabId: Int, roundNo: Int) = withAuth(rq) { user =>
    Permissions(user.id, tabId).map(_.setup).flatMap {
      case false => denied
      case true => Ok(Round(tabId, roundNo).flatMap(_.lock))
    }
  }

  def unlock(rq: Request[IO], tabId: Int, roundNo: Int) = withAuth(rq) { user =>
    Permissions(user.id, tabId).map(_.setup).flatMap {
      case false => denied
      case true => Ok(Round(tabId, roundNo).flatMap(_.unlock))
    }
  }

}