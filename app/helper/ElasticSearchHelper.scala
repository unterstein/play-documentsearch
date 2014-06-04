package helper

import org.slf4j.{LoggerFactory, Logger}
import org.elasticsearch.node.NodeBuilder._
import global.Global
import java.io.File
import org.elasticsearch.action.index.IndexRequestBuilder
import org.elasticsearch.common.xcontent.XContentFactory._
import org.elasticsearch.index.query.QueryBuilders
import java.security.MessageDigest
import org.elasticsearch.common.unit.TimeValue
import scala.collection.JavaConversions._
import java.math.BigInteger
import java.util

object ElasticSearchHelper {

  private val log: Logger = LoggerFactory.getLogger(ElasticSearchHelper.getClass)

  private val client = nodeBuilder().local(true).node().client()

  private val md5 = MessageDigest.getInstance("MD5")

  private val index = "documentsearch"

  private val indexType = "document"

  def sync(): Unit = {
    log.info("Syncing files to elasticsearch")
    try {
      // clear old index
      client.prepareDeleteByQuery(index).
        setQuery(QueryBuilders.matchAllQuery()).
        setTimeout(TimeValue.timeValueSeconds(5)).
        setTypes(indexType).execute().actionGet()
    } catch {
      case o_O: Exception => log.warn("Unable to clear index", o_O)
    }
    // re index
    val bulkIndex = client.prepareBulk()
    val requests = handleFile(Global.getDocumentBaseDir)
    if (requests.size > 0) {
      requests.foreach(indexAction => bulkIndex.add(indexAction))
      val response = bulkIndex.setTimeout(TimeValue.timeValueSeconds(5)).execute().actionGet()
      if (response.hasFailures) {
        log.warn("Warnings during bulk indexing documents: " + response.buildFailureMessage())
      }
    }
    log.info("Syncing files to elasticsearch - successfully")
  }

  def search(query: String): SearchResult = {
    val result = client.prepareSearch(index).
      setTypes(indexType).
      addField("file").
      addField("folder").
      addField("content").
      addField("attributes").
      execute().actionGet()
    SearchResult(result.getHits.map {
      entry =>
        val file = if (entry.field("file") != null) entry.field("file").getValue[String] else ""
        val folder = if (entry.field("folder") != null) entry.field("folder").getValue[String] else ""
        val content = if (entry.field("content") != null) entry.field("content").getValue[String] else ""
        val attributes = if (entry.field("attributes") != null) entry.field("attributes").getValue[String] else null
        SearchHit(file, folder, content, attributes)
    }.toList)
  }

  case class SearchHit(file: String, folder: String, content: String, attributes: Any)
  case class SearchResult(result: util.List[SearchHit])

  private def handleFile(file: File): List[IndexRequestBuilder] = {
    if (file.exists()) {
      if (file.isDirectory) {
        file.listFiles().map {
          f =>
            handleFile(f)
        }.flatten.toList
      } else {
        if (file.isFile && file.getName.startsWith(".") == false) {
          // is file
          val parseResult = ParseHelper.parse(file)
          List(client.prepareIndex(index, indexType, hashFileName(file))
            .setSource(// _body
              jsonBuilder()
                .startObject()
                .field("file", file.getName)
                .field("folder", file.getParentFile.getAbsolutePath.replace(Global.documentFolder, ""))
                .field("content", parseResult._1)
                .field("attributes", parseResult._2)
                .endObject()
            ).setCreate(true)
          )
        } else {
          List() // empty
        }
      }
    } else {
      List() // empty
    }
  }

  def close(): Unit = {
    client.close()
  }

  private def hashFileName(file: File): String = {
    md5(file.getAbsolutePath)
  }

  private def md5(fileName: String): String = {
    new BigInteger(1, md5.digest(fileName.getBytes)).toString(16)
  }
}
