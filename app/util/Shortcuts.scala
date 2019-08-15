package eu.seitzal.opentab

import play.api.mvc.RequestHeader
import play.api.Configuration
import java.time.Instant

package object shortcuts {

  def loggedIn(implicit request: RequestHeader): Boolean =
    request.session.get("userid").isDefined
  
  def timestamp() : Long =
    java.time.Instant.now.getEpochSecond()

  def location(implicit config: Configuration) = config.get[String]("location")

}