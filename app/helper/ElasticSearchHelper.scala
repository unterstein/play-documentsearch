package helper

import org.slf4j.{LoggerFactory, Logger}
import global.Global
import org.elasticsearch.common.xcontent.XContentFactory._
import org.elasticsearch.index.query.QueryBuilders
import java.security.MessageDigest
import org.elasticsearch.common.unit.TimeValue
import java.util
import java.lang.reflect.Type
import com.google.gson.reflect.TypeToken
import com.google.gson.Gson
import java.io.File
import org.elasticsearch.action.index.IndexRequestBuilder
import scala.collection.JavaConversions._
import java.math.BigInteger
import org.elasticsearch.common.settings.ImmutableSettings
import org.elasticsearch.node.NodeBuilder._
import models.SearchHit
import models.SearchResult
import org.apache.commons.io.FileUtils
import org.apache.commons.lang3.StringUtils

object ElasticSearchHelper {

  private val log: Logger = LoggerFactory.getLogger(ElasticSearchHelper.getClass)

  private val elasticsearchSettings = ImmutableSettings.settingsBuilder()
    .put("path.data", "target/elasticsearch-data")
    .put("http.port", 9200)

  private val node = nodeBuilder().local(true).settings(elasticsearchSettings.build()).node()

  val client = node.client()

  private val md5 = MessageDigest.getInstance("MD5")

  private val index = "documentsearch"

  private val indexType = "document"

  private val typeToken: Type = new TypeToken[util.Map[String, Any]]() {}.getType

  private var syncing = false

  def sync(): Unit = {
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
    log.info("Syncing files to elasticsearch")
    syncing = true
    log.info("clean old index")
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
    log.info("re-index")
    val bulkIndex = client.prepareBulk()
    val requests = handleFile(Global.getDocumentBaseDir)
    if (requests.size > 0) {
      requests.foreach(indexAction => bulkIndex.add(indexAction))
      val response = bulkIndex.setTimeout(TimeValue.timeValueSeconds(5)).execute().actionGet()
      if (response.hasFailures) {
        log.warn("Warnings during bulk indexing documents: " + response.buildFailureMessage())
      }
    }
    syncing = false
    log.info("Syncing files to elasticsearch - successfully")
  }

  def search(query: String): SearchResult = {
    if(syncing == false) {
      try {
        val result = client.prepareSearch(index).
          setTypes(indexType).
          addField("_name").
          addField("_folder").
          addField("content").
          addHighlightedField("content", 100, 100). // this is magic :)
          setQuery(QueryBuilders.multiMatchQuery(query, "_name", "_folder", "content")).
          execute().actionGet()
        SearchResult(result.getHits.toList.sortBy(_.score()).reverse.map {
          entry =>
            val file = if (entry.field("_name") != null) entry.field("_name").getValue[String] else ""
            val folder = if (entry.field("_folder") != null) entry.field("_folder").getValue[String] else ""
            val content = if (entry.field("content") != null) entry.field("content").getValue[String] else ""
            val attributes: util.Map[String, Any] = if (entry.field("attributes") != null) {
              val value = entry.field("attributes").getValue[String]
              new Gson().fromJson(value, typeToken)
            }
            else null

            val contentHighlights = entry.highlightFields().get("content")
            val highlights: util.List[String] = if (contentHighlights != null) {
              contentHighlights.fragments().map { t => t.string()}.toList
            } else {
              List()
            }
            SearchHit(entry.score, query, file, folder, content, attributes, highlights)
        }.toList)
      } catch {
        case o_O: Exception =>
          log.warn("Unable to search elasticsearch", o_O)
          emptyResult
      }
    } else {
      emptyResult
    }
  }

  private def emptyResult = SearchResult(List())

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
          List(client.prepareIndex(index, indexType, hashFileName(file))
            .setSource(// _body
              jsonBuilder()
                .startObject()
                .field("_name", file.getName)
                .field("_folder", file.getParentFile.getAbsolutePath.replace(Global.documentFolder, ""))
                .field("content", base64(file))
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
    node.close()
  }

  private def hashFileName(file: File): String = {
    md5(file.getAbsolutePath)
  }

  private def base64(file: File): String = {
    new sun.misc.BASE64Encoder().encode(FileUtils.readFileToByteArray(file))
  }

  private def md5(fileName: String): String = {
    new BigInteger(1, md5.digest(fileName.getBytes)).toString(16)
  }
}
