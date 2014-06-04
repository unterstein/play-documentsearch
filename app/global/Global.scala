package global

import play.api._
import com.typesafe.config.ConfigFactory
import helper.ElasticSearchHelper
import akka.actor.{Props, Cancellable}
import play.libs.Akka
import scala.concurrent.duration._
import actors.CronActor
import scala.concurrent.ExecutionContext
import ExecutionContext.Implicits.global
import org.slf4j.{LoggerFactory, Logger}

object Global extends GlobalSettings {

  private val log: Logger = LoggerFactory.getLogger(Global.getClass)

  private val conf = ConfigFactory.load()

  val documentFolder = replaceHome(conf.getString("documentFolder"))
  val cronCommand = conf.getString("cronCommand")

  private var cronJob: Cancellable = null
  private val cronIntervalInSeconds: Int = conf.getInt("cronIntervalInSeconds")

  override def onStart(app: Application) = {
    log.info("starting")
    // cron actor
    val cron = Akka.system.actorOf(Props[CronActor])
    cronJob = Akka.system.scheduler.schedule(200 milliseconds, cronIntervalInSeconds seconds, cron, "sync")
  }

  override def onStop(app: Application) = {
    ElasticSearchHelper.close()
    cronJob.cancel()
  }

  def replaceHome(path: String): String = {
    if (path.startsWith("~")) {
      System.getProperty("user.home") + path.substring(1)
    } else {
      path
    }
  }
}
