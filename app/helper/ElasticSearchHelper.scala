package helper

import org.slf4j.{LoggerFactory, Logger}
import global.Global
import org.elasticsearch.index.query.QueryBuilders
import java.security.MessageDigest
import java.util
import java.io.File
import scala.collection.JavaConversions._
import java.math.BigInteger
import models.SearchHit
import models.SearchResult
import org.apache.commons.io.FileUtils
import org.apache.commons.lang3.StringUtils
import elasticsearch.services.ElasticsearchServiceProvider
import elasticsearch.models.Document
import org.springframework.data.elasticsearch.core.query.NativeSearchQueryBuilder
import org.elasticsearch.search.highlight.HighlightBuilder

object ElasticSearchHelper {

  private val log: Logger = LoggerFactory.getLogger(ElasticSearchHelper.getClass)

  private val md5 = MessageDigest.getInstance("MD5")

  private val index = "documentsearch"

  private val indexType = "document"

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
    // clear old index
    ElasticsearchServiceProvider.get().documentRepository.deleteAll()
    // re index
    log.info("re-index")
    val documents = handleFile(Global.getDocumentBaseDir)
    if (documents.size > 0) {
      ElasticsearchServiceProvider.get().documentRepository.save(documents)
    }
    syncing = false
    log.info("Syncing files to elasticsearch - successfully")
  }

  def startup(): Unit = {
    try {
      val tmpFile = File.createTempFile("playdocusearch", "sh")
      FileUtils.copyInputStreamToFile(getClass.getClassLoader.getResourceAsStream("createIndex.sh"), tmpFile)
      Runtime.getRuntime.exec("chmod u+x " + tmpFile.getAbsolutePath)
      Runtime.getRuntime.exec(tmpFile.getAbsolutePath)
    } catch {
      case o_O: Exception =>
        log.warn("Unable to delete/create index", o_O)
    }
  }

  def search(query: String): SearchResult = {
    if (syncing == false) {
      val nativQuery = new NativeSearchQueryBuilder().withFields("name", "folder", "content")
        .withHighlightFields(new HighlightBuilder.Field("content").fragmentSize(100).numOfFragments(100))
        .withQuery(QueryBuilders.multiMatchQuery(query, "name", "folder", "content")).build()
      val documents = ElasticsearchServiceProvider.get().documentRepository.search(nativQuery)

      try {
        val result = ElasticsearchServiceProvider.get().client.prepareSearch(index).
          setTypes(indexType).
          addField("name").
          addField("folder").
          addField("content").
          addHighlightedField("content", 100, 100). // this is magic :)
          setQuery(QueryBuilders.multiMatchQuery(query, "name", "folder", "content")).
          execute().actionGet()
        SearchResult(result.getHits.toList.sortBy(_.score()).reverse.map {
          entry =>
            val file = if (entry.field("content") != null) entry.field("name").getValue[String] else ""
            val folder = if (entry.field("name") != null) entry.field("folder").getValue[String] else ""
            val content = if (entry.field("content") != null) entry.field("content").getValue[String] else ""

            val contentHighlights = entry.highlightFields().get("content")
            val highlights: util.List[String] = if (contentHighlights != null) {
              contentHighlights.fragments().map { t => t.string()}.toList
            } else {
              List()
            }
            SearchHit(entry.score, query, file, folder, content, highlights)
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

  private def handleFile(file: File): List[Document] = {
    if (file.exists()) {
      if (file.isDirectory) {
        file.listFiles().map {
          f =>
            handleFile(f)
        }.flatten.toList
      } else {
        if (file.isFile && file.getName.startsWith(".") == false) {
          // is file
          val doc = new Document()
          doc.id = hashFileName(file)
          doc.folder = file.getParentFile.getAbsolutePath.replace(Global.documentFolder, "")
          doc.name = file.getName
          doc.content = base64(file)
          List(doc)
        } else {
          List() // empty
        }
      }
    } else {
      List() // empty
    }
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
