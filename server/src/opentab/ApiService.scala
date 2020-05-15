package opentab

import opentab.auth._

import cats._
import cats.effect._
import cats.implicits._
import org.http4s._
import org.http4s.dsl.io._
import doobie._
import doobie.implicits._
import upickle.default._
import eu.seitzal.http4s_upickle._
import gensrv.GenService
import com.typesafe.config.Config
import doobie.util.invariant.UnexpectedEnd
import ujson.Obj

object ApiService extends GenService {

  def routes = (xa, config) => { implicit val xa_ = xa; implicit val c = config; {

    case GET -> Root =>
      Ok("OpenTab API server")

    case rq @ GET -> Root / "token" =>
      getToken(rq)

    case rq @ GET -> Root / "token" / "verify" =>
      withAuth(rq)(Ok(_))

    case rq @ GET -> Root / "token" / "verifyOpt" =>
      withAuthOpt(rq)(Ok(_))

    case rq @ GET -> Root / "tab" => withAuthOpt(rq) {
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

    case rq @ GET -> Root / "tab" / IntVar(tabId) => withAuthOpt(rq) {
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

    case rq @ POST -> Root / "tab" => withAuth(rq) { user =>
      rq.as[TabPartial]
        .flatMap(tp => Tab.create(tp.name, user.id, tp.isPublic))
        .flatMap(Ok(_))
    }

    case rq @ PATCH -> Root / "tab" / IntVar(tabId) => withAuth(rq) { user =>
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

    case rq @ DELETE -> Root / "tab" / IntVar(tabId) => withAuth(rq) { user =>
      Permissions(user.id, tabId).map(_.own).flatMap {
        case false => denied
        case true => 
          Tab(tabId)
            .flatMap(_.delete)
            .flatMap(_ => NoContent())
      }
    }

  }}

  override def processExpected: PartialFunction[Throwable,IO[Response[IO]]] = {
    case UnexpectedEnd => NotFound("Not found")
    case ex: MalformedMessageBodyFailure => badRequest(ex.getMessage)
    case ex: InvalidMessageBodyFailure => badRequest(ex.getMessage)
  }
  
}