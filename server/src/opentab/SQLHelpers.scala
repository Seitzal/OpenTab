package opentab

import doobie._
import doobie.implicits._
import cats.implicits._
import cats.effect.IO

trait SQLHelpers {

  type Xa = Transactor[IO]

  def updateFragment[A: Put](fieldName: String, newValueOpt: Option[A]) =
    newValueOpt match {
      case Some(v) => List(fr"name = $v")
      case None => Nil
    }
  
  def updateSql(tableName: String, entryId: Int)(fragments: List[Fragment]*): Fragment =
    fr"UPDATE $tableName SET " ++
      fragments.toList.flatten.intercalate(fr",") ++
      fr"WHERE id = $entryId"

}