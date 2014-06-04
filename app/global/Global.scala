package global

import play.api._
import com.typesafe.config.ConfigFactory
import helper.ElasticSearchHelper

object Global extends GlobalSettings {

  private val conf = ConfigFactory.load()

  val documentFolder = replaceHome(conf.getString("documentFolder"))
  val cronCommand = conf.getString("cronCommand")

  override def onStart(app: Application) = {

  }

  override def onStop(app: Application) = {
    ElasticSearchHelper.close()
  }

  def replaceHome(path: String): String = {
    if (path.startsWith("~")) {
      System.getProperty("user.home") + path.substring(1)
    } else {
      path
    }
  }
}
