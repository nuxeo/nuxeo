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
            "analyzer": {
                "default": {
                    "type": "custom",
                    "tokenizer": "keyword",
                    "filter": ["lowercase", "asciifolding"]
                },
                "fr_analyzer" : {
                    "type":"custom",
                    "tokenizer" : "standard",
                    "filter": ["lowercase", "fr_stop_filter", "fr_stem_filter", "asciifolding", "fr_elision_filter"]
                },
                "en_analyzer": {
                    "type": "custom",
                    "tokenizer": "standard",
                    "filter": ["lowercase", "en_stop_filter", "en_stem_filter", "asciifolding"]
                },
                "path_analyzer": {
                    "type": "custom",
                    "tokenizer": "path_tokenizer"
                }
            },
            "filter" : {
                "fr_stop_filter":{
                    "type":"stop",
                    "stopwords":["_french_"]
                },
                "fr_stem_filter": {
                    "type": "stemmer",
                    "name": "minimal_french"
                },
                "fr_elision_filter" : {
                    "type" : "elision",
                    "articles" : ["c", "l", "m", "t", "qu", "n", "s", "j"]
                },
                "en_stop_filter":{
                    "type":"stop",
                    "stopwords":["_english_"]
                },
                "en_stem_filter": {
                    "type": "stemmer",
                    "name": "minimal_english"
                }
            },
            "tokenizer": {
                "path_tokenizer": {
                    "type": "path_hierarchy",
                    "delimiter": "/"
                }
            }
        }
    }
}' |  grep error && exit -1

echo "### Creating mapping doc ..."
curl -s -X PUT "$ESHOST:$ESPORT/$ESINDEX/doc/_mapping" -d '{
        "doc" : {
            "_all" : {
              "analyzer" : "fr_analyzer"
            },
            "_size" : {
              "enabled" : true
            },
            "_timestamp" : {
              "enabled" : true,
              "path" : "dc:modified"
            },
            "properties" : {
                "ecm:path" : {
                    "type" : "multi_field",
                    "fields" : {
                        "ecm:path" : {
                           "type" : "string",
                           "index" : "not_analyzed"
                         },
                         "children" : {
                            "type" : "string",
                            "index_analyzer": "path_analyzer",
                            "search_analyzer": "keyword"
                         }
                      }
                },
                "dc:title" : {
                    "type" : "string",
                    "analyzer" : "fr_analyzer",
                    "boost": 2.0
                },
                "dc:description" : {
                    "type" : "string",
                    "analyzer" : "fr_analyzer",
                    "boost": 1.5
                }
            }
        }
  }' | grep "error" && exit -2

echo "### Done"
