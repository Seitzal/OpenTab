package opentab

package object exceptions {
  abstract class TabException(msg: String) extends Exception(msg)
  case class RoundStatusException(msg: String) 
    extends TabException(s"Failed to change round status: $msg")
}