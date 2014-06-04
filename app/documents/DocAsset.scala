package documents

import java.io.{FileInputStream, File}
import org.apache.poi.hwpf.HWPFDocument

class DocAsset(val file: File) extends Asset {

  override def readContent(): String = {
    val fileStream = new FileInputStream(file)
    val document = new HWPFDocument(fileStream)
    val result = document.getText
    fileStream.close()
    result.toString
  }
}
