#!/bin/sh
#curl -X POST http://localhost:9200/documentsearch -d '{
#  "mappings" : {
#    "document" : {
#      "properties" : {
#        "content" : {
#          "type" : "attachment",
#          "fields" : {
#            "content"  : { "store" : "yes" },
#            "author"   : { "store" : "yes" },
#            "title"    : { "store" : "yes" },
#            "date"     : { "store" : "yes" },
#            "keywords" : { "store" : "yes", "analyzer" : "keyword" },
#            "name"    : { "store" : "yes" },
#            "content_type" : { "store" : "yes" },
#            "content_length" : { "store" : "yes" }
#          }
#        }
#      }
#    }
#  }
#}'

curl -X DELETE http://localhost:9200/documentsearch
curl -X POST http://localhost:9200/documentsearch -d '{
  "mappings" : {
    "document" : {
      "properties" : {
        "file"  : { "type": "string", "store" : "yes", "analyzer": "german" },
        "folder"  : { "type": "string", "store" : "yes", "analyzer": "german" },
        "content"  : { "type": "string", "store" : "yes", "analyzer": "german" },
        "attributes"  : { "type": "string", "store" : "yes", "analyzer": "german" }
      }
    }
  }
}'