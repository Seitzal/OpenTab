package eu.seitzal.opentab

import play.api.mvc.RequestHeader

package object shortcuts {

  def loggedIn(implicit request: RequestHeader) =
    request.session.get("userid").isDefined

}