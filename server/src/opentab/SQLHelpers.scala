package opentab

import doobie._
import doobie.implicits._
import cats.implicits._
import cats.effect.IO
import com.typesafe.scalalogging.LazyLogging

trait SQLHelpers extends LazyLogging {

  type Xa = Transactor[IO]

  def updateFragment[A: Put](fieldName: String, newValueOpt: Option[A]) =
    newValueOpt match {
      case Some(v) => List(Fragment(s"$fieldName = ", Nil) ++ fr"$v")
      case None => Nil
    }
  
  def updateSql(tableName: String)(fragments: List[Fragment]*)(condition: Fragment): Fragment =
    Fragment(s"UPDATE $tableName SET ", Nil) ++
      fragments.toList.flatten.intercalate(fr",") ++
      condition


}