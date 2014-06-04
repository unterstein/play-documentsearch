package actors

import org.slf4j.{Logger, LoggerFactory}
import akka.actor.Actor
import helper.ElasticSearchHelper
import global.Global
import org.apache.commons.lang3.StringUtils

class CronActor extends Actor {

  private val log: Logger = LoggerFactory.getLogger(classOf[CronActor])

  def receive = {
    case "sync" =>
      val command = Global.cronCommand
      if (StringUtils.isNotEmpty(command)) {
        log.info("Executing cron command")
        try {
          new ProcessBuilder(command).directory(Global.getDocumentBaseDir).start().waitFor()
        } catch {
          case o_O: Exception =>
            log.warn("Unable to run cron command: " + command, o_O)
        }
      }

      ElasticSearchHelper.sync()
  }

}
