package models

case class Document(mimeType: String, fileName: String, path: String, content: String, meta: Map[String, String])
