package helper

import java.io.{StringWriter, FileInputStream, File}
import org.slf4j.{LoggerFactory, Logger}
import org.apache.tika.sax.WriteOutContentHandler
import org.apache.tika.parser.AutoDetectParser
import org.apache.tika.metadata.Metadata
import java.util

object ParseHelper {

  private val logger: Logger = LoggerFactory.getLogger(ParseHelper.getClass)

  def parse(file: File): ParseResult = {
    val input = new FileInputStream(file)
    val writer = new StringWriter()
    val metadata = new Metadata
    new AutoDetectParser().parse(input, new WriteOutContentHandler(writer), metadata)
    val attributes = new util.HashMap[String, Object]()
    metadata.names().foreach(name => attributes.put(name, metadata.get(name)))
    new ParseResult(writer.toString, attributes)
  }

  case class ParseResult(content: String, attributes: util.Map[String, Object])

}
