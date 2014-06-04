package helper

import org.slf4j.{LoggerFactory, Logger}
import org.elasticsearch.node.NodeBuilder._

object ElasticSearchHelper {

  private val logger: Logger = LoggerFactory.getLogger(ElasticSearchHelper.getClass)

  private val client = nodeBuilder().local(true).node().client()

  def sync(): Unit = {

  }

  def close(): Unit = {
    client.close()
  }

}
