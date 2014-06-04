package helper

import org.slf4j.{LoggerFactory, Logger}
import org.elasticsearch.node.NodeBuilder._
import javax.xml.bind.DatatypeConverter
import global.Global
import java.io.File
import org.elasticsearch.action.index.IndexRequestBuilder
import org.elasticsearch.common.xcontent.XContentFactory._
import org.apache.commons.io.FileUtils
import org.elasticsearch.action.admin.indices.delete.DeleteIndexRequest
import org.elasticsearch.index.query.QueryBuilders


object ElasticSearchHelper {

  private val log: Logger = LoggerFactory.getLogger(ElasticSearchHelper.getClass)

  private val client = nodeBuilder().local(true).node().client()

  private val index = "documentsearch"

  private val indexType = "document"

  def sync(): Unit = {
    log.info("Syncing files to elasticsearch")
    try {
      // clear old index
      client.prepareDeleteByQuery(index).
        setQuery(QueryBuilders.matchAllQuery()).
        setTypes(indexType).execute().actionGet()
    } catch {
      case o_O: Exception => log.warn("Unable to clear index", o_O)
    }
    // re index
    val bulkIndex = client.prepareBulk()
    handleFile(new File(Global.documentFolder)).foreach(indexAction => bulkIndex.add(indexAction))
    bulkIndex.execute().actionGet()
  }

  private def handleFile(file: File): List[IndexRequestBuilder] = {
    if (file.exists()) {
      if (file.isDirectory) {
        file.listFiles().map {
          f =>
            handleFile(f)
        }.flatten.toList
      } else {
        // is file
        List(client.prepareIndex(index, indexType)
          .setSource(// _body
            jsonBuilder()
              .startObject()
              .field("_name", file.getName)
              .field("_folder", cleanFolderName(file))
              .field("content", base64(file))
              .endObject()
          )
        )
      }
    } else {
      List() // empty
    }
  }

  def close(): Unit = {
    client.close()
  }

  private def cleanFolderName(file: File): String = {
    file.getParentFile.getAbsolutePath.replace(Global.documentFolder, "")
  }

  private def base64(file: File): String = {
    DatatypeConverter.printBase64Binary(FileUtils.readFileToByteArray(file)).replace("\n", "")
  }
}
