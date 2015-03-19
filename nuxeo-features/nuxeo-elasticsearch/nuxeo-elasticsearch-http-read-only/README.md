nuxeo-elasticsearch-http-read-only
==================================

## About

This addon exposes a limited set of Read Only Elasticsearch HTTP REST API, taking in account the Nuxeo authentication


## Building


     mvn clean install


## Requirements

- An Elasticsearch instance with an HTTP REST API, default port is 9200 when _http.enabled_ is enabled.
- Set the HTTP Rest URL on the nuxeo.conf

        elasticsearch.httpReadOnly.baseUrl = http://localhost:9200

## How it works

A Nuxeo webengine module acts as a proxy and provide:
- The Nuxeo authentication, only valid Nuxeo user can access the REST API
- The Nuxeo ACL authorization, user can access only document that they are allowed to see.
- Limit access to Elasticsearch index and types defined inside Nuxeo.

For instance instead of doing:

    curl -XGET 'http://localhost:9200/_search?size=0' -d '{ "query": { "match_all":{}}}'

Change the base URL to **http://localhost:8080/nuxeo/site/es**, like this:

    curl -XGET 'http://jdoe:password@localhost:8080/nuxeo/site/es/_search?size=0' -d '{ "query": { "match_all":{}}}'

The final request submitted to Elasticsearch will be equivalent to:

    curl -XGET 'http://localhost:9200/nuxeo/doc/_search?size=0' -d '{"query":{"filtered":{"filter":{"terms":{"ecm:acl"

We can see that index and type have been set and the query has been filtered to match the jdoe user ACL.

Note that Nuxeo will submit ONLY HTTP GET request to Elasticsearch, even if Nuxeo accepts Request Body Search using HT

The Document GET API is also filtered, for non Adminitrator there is a first request to get the document ACL then if a

## REST API exposed

Only a small part of the API is exposed:

The Search APIs:

- The [Request Body Search](http://www.elastic.co/guide/en/elasticsearch/reference/current/search-request-body.html) u
- The [URI Search API](http://www.elastic.co/guide/en/elasticsearch/reference/current/search-uri-request.html)

The Document APIs:

- The [Get API](http://www.elastic.co/guide/en/elasticsearch/reference/current/docs-get.html)

## Links

 - Administration: http://doc.nuxeo.com/x/UBY5AQ
