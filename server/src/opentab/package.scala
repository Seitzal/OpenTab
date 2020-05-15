import doobie.util.transactor.Transactor
import cats.effect.IO

package object opentab {
  type Xa = Transactor[IO]
}
