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
import models.{SearchHit, SearchResult}
import org.elasticsearch.client.transport.TransportClient
import org.elasticsearch.common.settings.ImmutableSettings
import org.elasticsearch.node.NodeBuilder._
import models.SearchHit
import models.SearchResult

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

  def sync(): Unit = {
    log.info("Syncing files to elasticsearch")
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
    log.info("Syncing files to elasticsearch - successfully")
  }

  def search(query: String): SearchResult = {
    val result = client.prepareSearch(index).
      setTypes(indexType).
      addField("file").
      addField("folder").
      addField("content").
      addField("attributes").
      addHighlightedField("content", 100, 100). // this is magic :)
      setQuery(QueryBuilders.multiMatchQuery(query, "file", "folder", "content", "attributes")).
      execute().actionGet()
    SearchResult(result.getHits.toList.sortBy(_.score()).reverse.map {
      entry =>
        val file = if (entry.field("file") != null) entry.field("file").getValue[String] else ""
        val folder = if (entry.field("folder") != null) entry.field("folder").getValue[String] else ""
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
  }

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
                .field("content", parseResult.content)
                .field("attributes", new Gson().toJson(parseResult.attributes))
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
