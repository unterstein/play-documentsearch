package actors

import org.slf4j.{Logger, LoggerFactory}
import akka.actor.Actor
import helper.ElasticSearchHelper

class CronActor extends Actor {

  private val log: Logger = LoggerFactory.getLogger(classOf[CronActor])

  def receive = {
    case "sync" =>
      ElasticSearchHelper.sync()
  }

}
