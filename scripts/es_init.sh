#!/bin/bash

# Configure ES
ESHOST=${ESHOST:-localhost}
ESPORT=${ESPORT:-9200}
ESINDEX=${ESINDEX:-nuxeo}
# Single shard = single lucene index, easier to debug using luke, not for production
ESSHARDS=${ESSHARDS:-1}
ESREPLICAS=${ESREPLICAS:-0}

read -r -p "### Going to RESET the elasticsearch index $ESHOST:$ESPORT/$ESINDEX, are you sure? [y/N] " response
case $response in
    [yY][eE][sS]|[yY]) 
        ;;
    *)
	echo "Canceled, bye"
        exit 0
        ;;
esac
echo "### Delete index..."
curl -s -XDELETE "$ESHOST:$ESPORT/$ESINDEX" curl | grep error

echo "### Creating index $ESINDEX ..."
curl -s -XPUT "$ESHOST:$ESPORT/$ESINDEX" -d '{
  "settings" : { 
    "index.number_of_shards": '"$ESSHARDS"', 
    "index.number_of_replicas": '"$ESREPLICAS"',
        "analysis": {
      "filter" : {
         "en_stem_filter" : {
            "name" : "minimal_english",
            "type" : "stemmer"
         },
         "en_stop_filter" : {
            "stopwords" : [
               "_english_"
            ],
            "type" : "stop"
         },
         "fr_elision_filter" : {
            "articles" : [
               "c",
               "l",
               "m",
               "t",
               "qu",
               "n",
               "s",
               "j"
            ],
            "type" : "elision"
         },
         "fr_stem_filter" : {
            "name" : "minimal_french",
            "type" : "stemmer"
         },
         "fr_stop_filter" : {
            "stopwords" : [
               "_french_"
            ],
            "type" : "stop"
         }
      },
      "tokenizer" : {
         "path_tokenizer" : {
            "delimiter" : "/",
            "type" : "path_hierarchy"
         }
      },
      "analyzer" : {
         "en_analyzer" : {
            "alias" : "fulltext",
            "filter" : [
               "lowercase",
               "en_stop_filter",
               "en_stem_filter",
               "asciifolding"
            ],
            "type" : "custom",
            "tokenizer" : "standard"
         },
         "fr_analyzer" : {
            "filter" : [
               "lowercase",
               "fr_stop_filter",
               "fr_stem_filter",
               "asciifolding",
               "fr_elision_filter"
            ],
            "type" : "custom",
            "tokenizer" : "standard"
         },
         "path_analyzer" : {
            "type" : "custom",
            "tokenizer" : "path_tokenizer"
         },
         "default" : {
            "type" : "custom",
            "tokenizer" : "keyword"
         }
      }
   }
    }
}' |  grep error && exit -1

echo "### Creating mapping doc ..."
curl -s -X PUT "$ESHOST:$ESPORT/$ESINDEX/doc/_mapping" -d '{
        "doc" : {
   "_all" : {
      "analyzer" : "en_analyzer"
   },
   "properties" : {
      "dc:description" : {
         "boost" : 1.5,
         "type" : "string",
         "analyzer" : "en_analyzer"
      },
      "dc:title" : {
         "boost" : 2,
         "type" : "string",
         "analyzer" : "en_analyzer"
      },
      "dc:created": {
         "format": "dateOptionalTime",
        "type": "date"
      },
      "dc:modified": {
         "format": "dateOptionalTime",
        "type": "date"
      },
      "ecm:path" : {
         "fields" : {
            "children" : {
               "search_analyzer" : "keyword",
               "index_analyzer" : "path_analyzer",
               "type" : "string"
            },
            "ecm:path" : {
               "index" : "not_analyzed",
               "type" : "string"
            }
         },
         "type" : "multi_field"
      }
   }
        }
  }' | grep "error" && exit -2

echo "### Done"
