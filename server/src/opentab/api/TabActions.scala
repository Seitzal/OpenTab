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

class TabActions(implicit xa: Xa, config: Config) {

  def getAll(rq: Request[IO]) = withAuthOpt(rq) {
    case None => 
      Tab.getAll
        .map(_.filter(_.isPublic))
        .flatMap(Ok(_))
    case Some(user) =>
      Tab.getIds
        .map(_.map(tabId => Permissions(user.id, tabId)))
        .flatMap(_.sequence)
        .map(_.filter(_.view))
        .map(_.map(perms => Tab(perms.tabId)))
        .flatMap(_.sequence)
        .flatMap(Ok(_))
  }

  def get(rq: Request[IO], tabId: Int) = withAuthOpt(rq) {
    case Some(user) =>
      Permissions(user.id, tabId)
        .map(_.view)
        .flatMap {
          case true => Ok(Tab(tabId))
          case false => denied
        }
    case None => {
      val tab = Tab(tabId)
      tab.map(_.isPublic).flatMap {
        case true => Ok(tab)
        case false => unauthorized
      }
    }
  }

  def getPermissions(rq: Request[IO], tabId: Int) = withAuthOpt(rq) {
    case Some(user) =>
      Ok(Permissions(user.id, tabId))
    case None =>
      Tab(tabId).map(_.isPublic).flatMap {
        case true  => Ok(Permissions(-1, tabId, true, false, false, false))
        case false => Ok(Permissions(-1, tabId, false, false, false, false))
      }
  }

  def post(rq: Request[IO]) = withAuth(rq) { user =>
    rq.as[TabPartial]
      .flatMap(tp => Tab.create(tp.name, user.id, tp.isPublic))
      .flatMap(Ok(_))
  }

  def patch(rq: Request[IO], tabId: Int) = withAuth(rq) { user =>
    Permissions(user.id, tabId).map(_.setup).flatMap {
      case false => denied
      case true => Ok(
        for {
          tab         <- Tab(tabId)
          data        <- rq.as[Obj]
          newName     <- IO.delay(data.value.get("name").map(_.str))
          newIsPublic <- IO.delay(data.value.get("isPublic").map(_.bool))
          newTab      <- tab.update(newName, newIsPublic)
        } yield newTab
      )
    }
  }

  def delete(rq: Request[IO], tabId: Int) = withAuth(rq) { user =>
    Permissions(user.id, tabId).map(_.own).flatMap {
      case false => denied
      case true => 
        Tab(tabId)
          .flatMap(_.delete)
          .flatMap(_ => NoContent())
    }
  }

}