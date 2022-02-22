package opentab

import upickle.{default => json}
import java.io.File

trait Preloads {
  val defaultTabSettings = json.read[Map[String, String]](new File("tabsettings.default.json"))
}