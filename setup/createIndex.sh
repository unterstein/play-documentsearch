curl -X POST http://localhost:9200/documentsearch -d '{
  "mappings" : {
    "document" : {
      "properties" : {
        "content" : {
          "type" : "attachment",
          "fields" : {
            "content"  : { "store" : "yes" },
            "author"   : { "store" : "yes" },
            "title"    : { "store" : "yes", "analyzer" : "english"},
            "date"     : { "store" : "yes" },
            "keywords" : { "store" : "yes", "analyzer" : "keyword" },
            "_name"    : { "store" : "yes" },
            "content_type" : { "store" : "yes" }
          }
        }
      }
    }
  }
}'