package helper

import java.io.{StringWriter, FileInputStream, File}
import org.slf4j.{LoggerFactory, Logger}
import org.apache.tika.sax.WriteOutContentHandler
import org.apache.tika.parser.AutoDetectParser
import org.apache.tika.metadata.Metadata

// http://www.elasticsearch.org/guide/en/elasticsearch/reference/current/mapping-attachment-type.html
// bin/plugin -install elasticsearch/elasticsearch-mapper-attachments/2.0.0
object ParseHelper {

  private val logger: Logger = LoggerFactory.getLogger(ParseHelper.getClass)

  def parse(file: File): (String, Map[String, String]) = {
    val input = new FileInputStream(file)
    val writer = new StringWriter()
    val metadata = new Metadata
    new AutoDetectParser().parse(input, new WriteOutContentHandler(writer), metadata)
    (writer.toString, metadata.names().map(name => (name, metadata.get(name))).toMap)
  }

}
