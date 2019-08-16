package eu.seitzal.opentab

import play.api.mvc.RequestHeader
import play.api.Configuration
import java.time.Instant

package object shortcuts {

  def loggedIn(implicit request: RequestHeader): Boolean =
    request.session.get("userid").isDefined
  
  def sessionUserId(implicit request: RequestHeader): Int =
    request.session.get("userid").get.toInt

  def sessionUserName(implicit request: RequestHeader): String =
    request.session.get("username").get
  
  def timestamp() : Long =
    java.time.Instant.now.getEpochSecond()

  def location(implicit config: Configuration) = config.get[String]("location")

}