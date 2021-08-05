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

class TabActions(implicit xa: Xa, config: Config) {

  def getAll(rq: Request[IO]) = withAuthOpt(rq) {
    case None =>
      Ok(Tab.getAllPublic)
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
      tab.flatMap(_.isPublic).flatMap {
        case true => Ok(tab)
        case false => unauthorized
      }
    }
  }

  def getAllPermissions(rq: Request[IO]) = withAuthOpt(rq) {
    case Some(user) =>
      Tab.getIds
        .map(_.map(tabId => Permissions(user.id, tabId)))
        .flatMap(_.sequence)
        .map(_.filter(_.view))
        .map(_.map(perms => (perms.tabId, perms)))
        .flatMap(Ok(_))
    case None =>
      Tab.getAllPublic
        .map(_.map(tab => (tab.id, Permissions(-1, tab.id, true, false, false, false))))
        .flatMap(Ok(_))
  }

  def getPermissions(rq: Request[IO], tabId: Int) = withAuthOpt(rq) {
    case Some(user) =>
      Ok(Permissions(user.id, tabId))
    case None =>
      Tab(tabId).flatMap(_.isPublic).flatMap {
        case true  => Ok(Permissions(-1, tabId, true, false, false, false))
        case false => Ok(Permissions(-1, tabId, false, false, false, false))
      }
  }

  def post(rq: Request[IO]) = withAuth(rq) { user =>
    rq.as[TabPartial]
      .flatMap(tp => Tab.create(tp.name, user.id))
      .flatMap(Ok(_))
  }

  def rename(rq: Request[IO], tabId: Int) = withAuth(rq) { user =>
    Permissions(user.id, tabId).map(_.setup).flatMap {
      case false => denied
      case true => Ok(
        for {
          tab         <- Tab(tabId)
          data        <- rq.as[Obj]
          newName     <- IO(data.value.get("name").map(_.str))
          newTab      <- tab.rename(newName.get)
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

  def getSettings(rq: Request[IO], tabId: Int) = withAuth(rq) { user =>
    Permissions(user.id, tabId).map(_.setup).flatMap {
      case false => denied
      case true => Ok(Tab(tabId).flatMap(_.getSettings))
    }
  }

  def updateSettings(rq: Request[IO], tabId: Int) = withAuth(rq) { user =>
    Permissions(user.id, tabId).map(_.setup).flatMap {
      case false => denied
      case true => Ok(
        for {
          tab         <- Tab(tabId)
          settingsObj <- rq.as[Obj]
          newSettings <- tab.updateSettings(settingsObj.obj.map{case (k,v) => (k, v.str)}.toMap)
        } yield newSettings
      )
    }
  }

}