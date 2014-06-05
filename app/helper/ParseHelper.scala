package helper

import java.io.{StringWriter, FileInputStream, File}
import org.slf4j.{LoggerFactory, Logger}
import org.apache.tika.sax.WriteOutContentHandler
import org.apache.tika.parser.AutoDetectParser
import org.apache.tika.metadata.Metadata
import java.util
import scala.collection.JavaConversions._

object ParseHelper {

  private val log: Logger = LoggerFactory.getLogger(ParseHelper.getClass)

  def parse(file: File): ParseResult = {
    try {
      val input = new FileInputStream(file)
      val writer = new StringWriter()
      val metadata = new Metadata
      new AutoDetectParser().parse(input, new WriteOutContentHandler(writer), metadata)
      val attributes = new util.HashMap[String, Object]()
      metadata.names().foreach(name => attributes.put(name, metadata.get(name)))
      new ParseResult(writer.toString, attributes)
    } catch {
      case o_O: Exception =>
        log.warn("Unable to extract file content from " + file, o_O)
        new ParseResult("", Map[String, Object]())
    }
  }

  case class ParseResult(content: String, attributes: util.Map[String, Object])

}
