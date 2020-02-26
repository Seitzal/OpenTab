package opentab

import java.security.SecureRandom
import scala.collection.mutable.ArrayBuffer

object Base64 {

  def unsign(b : Byte) : Int =
    if (b < 0) 256 + b else b

  class MalformedStringException extends Exception("Base64: Malformed String")

  val table = (('A' to 'Z') ++ ('a' to 'z') ++ ('0' to '9')).toArray :+ '+' :+ '/'

  def encode(data : Array[Byte]) : String = {
    val ndata = data.map(unsign)
    val mod3 = data.length % 3
    val regular = (for (i <- 0 until ndata.length - mod3 by 3) yield {
      val first = ndata(i) >> 2
      val second = ((ndata(i) & 3) << 4) | (ndata(i + 1) >> 4)
      val third = ((ndata(i + 1) & 15) << 2) | (ndata(i + 2) >> 6)
      val fourth = ndata(i + 2) & 63
      "" + table(first) + table(second) + table(third) + table(fourth)
    }).mkString
    if (mod3 == 2) {
      val first = ndata(ndata.length - 2) >> 2
      val second = ((ndata(ndata.length - 2) & 3) << 4) | (ndata(ndata.length - 1) >> 4)
      val third = (ndata(ndata.length - 1) & 15) << 2
      regular + table(first) + table(second) + table(third) + "="
    } else if (mod3 == 1) {
      val first = ndata(ndata.length - mod3) >> 2
      val second = (ndata(ndata.length - mod3) & 3) << 4
      regular + table(first) + table(second) + "=="
    } else regular
  }

}

object Keygen {

  val ran = new SecureRandom

  def newKey(length : Int) : Array[Byte] = {
    val response = new Array[Byte](length)
    ran.nextBytes(response)
    response
  }

  def newTempKey: String = Base64.encode(newKey(32))

  def newPermKey: String = Base64.encode(newKey(64))
}