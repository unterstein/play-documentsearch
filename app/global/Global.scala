package global

import play.api._
import com.typesafe.config.ConfigFactory

object Global extends GlobalSettings {

  private val conf = ConfigFactory.load()

  override def onStart(app: Application) = {

  }

  override def onStop(app: Application) = {

  }

  val documentFolder = replaceHome(conf.getString("documentFolder"))
  val cronCommand = conf.getString("cronCommand")

  def replaceHome(path: String): String = {
    if (path.startsWith("~")) {
      System.getProperty("user.home") + path.substring(1)
    } else {
      path
    }
  }
}
