package helper

import org.slf4j.{LoggerFactory, Logger}
import org.elasticsearch.node.NodeBuilder._
import global.Global
import java.io.File
import org.elasticsearch.action.index.IndexRequestBuilder
import org.elasticsearch.common.xcontent.XContentFactory._
import org.apache.commons.io.FileUtils
import org.elasticsearch.index.query.QueryBuilders
import javax.xml.bind.DatatypeConverter
import java.security.MessageDigest
import java.math.BigInteger


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
      val response = client.prepareDeleteByQuery(index).
        setQuery(QueryBuilders.matchAllQuery()).
        setTypes(indexType).execute().actionGet()
      println(response)
    } catch {
      case o_O: Exception => log.warn("Unable to clear index", o_O)
    }
    // re index
    val bulkIndex = client.prepareBulk()
    val requests = handleFile(Global.getDocumentBaseDir)
    if (requests.size > 0) {
      requests.foreach(indexAction => bulkIndex.add(indexAction))
      val response = bulkIndex.execute().actionGet()
      println(response)
    }

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
          List(client.prepareIndex(index, indexType, hashFileName(file))
            .setSource(// _body
              jsonBuilder()
                .startObject()
                .field("_name", file.getName)
                .field("_folder", file.getParentFile.getAbsolutePath.replace(Global.documentFolder, ""))
                .field("content", base64(file))
                .endObject()
            )
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
    md5(file.getParentFile.getAbsolutePath)
  }

  private def base64(file: File): String = {
    DatatypeConverter.printBase64Binary(FileUtils.readFileToByteArray(file)).replace("\n", "")
  }

  private def md5(fileName: String): String = {
    new BigInteger(1, md5.digest(fileName.getBytes)).toString(16)
  }
}
