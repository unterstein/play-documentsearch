package models

case class SearchHit(score: Float, query: String, file: String, folder: String, content: String, attributes: Any)
