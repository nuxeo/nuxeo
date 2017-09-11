nuxeo-elasticsearch-http-read-only
==================================

## About

This addon exposes a limited set of Read Only Elasticsearch HTTP REST API, taking in account the Nuxeo authentication
and authorization.


## Building


     mvn clean install


## Requirements

- An Elasticsearch instance with an [HTTP REST API](http://www.elastic.co/guide/en/elasticsearch/reference/current/modules-http.html),
  the default port is 9200.
- Deploy the `nuxeo-elasticsearch-http-read-only-VERSION.jar` into your Nuxeo server under `nxserver/bundles/`.
- Configure the base URL to access the REST API, in the `nuxeo.conf` add:

        elasticsearch.httpReadOnly.baseUrl = http://localhost:9200

## Usage

A Nuxeo webengine module acts as a proxy and provides:

- The Nuxeo authentication: only valid Nuxeo user can access the REST API.
- The Nuxeo ACL authorization, user can access only documents that they are allowed to see.
- Limit access to Elasticsearch index (and types) defined inside Nuxeo.

For instance if your REST client application want to query Elasticsearch like this:

    curl -XGET 'http://localhost:9200/_search?size=0' -d '{ "query": { "match_all":{}}}'

To do this search through Nuxeo, you need to change the base URL and use authentication:

    curl -XGET -u jdoe:password  'http://localhost:8080/nuxeo/site/es/_search?size=0' -d '{ "query": { "match_all":{}}}'

Note that the base URL change from **http://my-elastic-search-server:9200** to **http://my-nuxeo-server:8080/nuxeo/site/es**.

The previous request is rewritten and the final request submitted to Elasticsearch is equivalent to:

    curl -XGET 'http://localhost:9200/nuxeo/doc/_search?size=0' -d '{"query":{"bool":{"filter":{"terms":{"ecm:acl":["members","user1","Everyone"]}},"must":{"match_all":{}}}}}'

We can see that `index` and `type` have been explicitly set and the query has a filter to match the *jdoe* user ACL.

Nuxeo will submit only HTTP GET request to Elasticsearch, even if Nuxeo accepts search using HTTP POST.

The Document GET API is also filtered, for non Adminitrator user, there is a first request to retrieve the document ACL
then only if it is allowed the original request is forwarded.

## REST API exposed

Only a small part of the API is exposed:

The Search APIs:

- The [Request Body Search](http://www.elastic.co/guide/en/elasticsearch/reference/current/search-request-body.html)
- The [URI Search API](http://www.elastic.co/guide/en/elasticsearch/reference/current/search-uri-request.html)

The Document APIs:

- The [Get API](http://www.elastic.co/guide/en/elasticsearch/reference/current/docs-get.html)

## Limitations

### Request Body Search with POST request

The Elasticsearch Request Body Search API can be done using GET or POST request. The reason for this is that some
clients are not able to send GET request with a body.

The Nuxeo proxy accepts POST request, but only if the content type is set properly, like this:

    curl -XPOST -u jdoe:password -H "Content-Type: application/json" 'http://localhost:8080/nuxeo/site/es/_search?size=0' -d '{ "query": { "match_all":{}}}'

If you don't set this header you will get error like:

    {"entity-type":"exception","code":"javax.ws.rs.WebApplicationException","status":500,"message":null}1

### GET API with HEAD request

There is no HEAD request support at the moment.

### GET API _source endpoint

No support for the /{index}/{type}/{id}/_source endpoint at the moment.

## Links

 - Administration: http://doc.nuxeo.com/x/UBY5AQ
