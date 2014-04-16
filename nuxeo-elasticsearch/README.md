nuxeo-elasticsearch
===================

## About

This project aims at providing Nuxeo Bundles to integrate
ElasticSearch with Nuxeo Platform.

The idea is to index/query Nuxeo Document to/from ElasticSearch.

## Building

To build and run the tests, simply start the maven build:

    mvn clean install

You can also download prebuild packages from our QA server:

http://qa.nuxeo.org/jenkins/job/addons_nuxeo-elasticsearch-master/lastStableBuild/artifact/target/

Or use the marketplace package:

https://github.com/nuxeo/marketplace-elasticsearch

## Limitations

- Don't work with an H2 backend (incompatible lucene versions)

- Only support a single repository.

- ACL filtering works fine only on simplified ACL, we don't support
  negative ACE other than `Deny Everyone`. Documents with negative ACL
  will not be accessible for non administrator account.

- NXQL Limitations:

  - Fulltext search on field is supported if the Elasticsearch mapping
	is properly defined, for instance `ecm:fulltext.dc:title = 'foo*'`
	works if there is a multi field `dc:title.fulltext`, visit the
	[default configuration](https://github.com/nuxeo/nuxeo-elasticsearch/blob/master/nuxeo-elasticsearch-core/src/main/resources/OSGI-INF/elasticsearch-default-index-contrib.xml)
	to find some example.
  - The fulltext syntax accepted is different from the default NXQL
    it is the Elasticsearch
    [simple query string syntax](http://www.elasticsearch.org/guide/en/elasticsearch/reference/current/query-dsl-simple-query-string-query.html#_simple_query_string_syntax).
  - For now the `ecm:fulltext` match the Elasticsearch `_all` field
    which is the concatenation of all fields, this is different from
    the NXQL `ecm:fulltext` which match only some explicit fields.
    Also there is no support of custom fulltext index
    `ecm:fulltext_someindex` will match the `_all` field.
  - Fields are case sensitive `ecm:primaryType` vs `ecm:primarytype`
  - No select clause support, so NO : DISTINCT, COUNT, AVERAGE, MAX,
    MIN, operators like: + - / *
  - ILIKE will work only with a proper mapping with an analyzer
    configured to lowercase index
  - No support of complex list correlation like:

        files/*1/file/name LIKE '%.txt' AND files/*1/file/length = 0)

- Content view does not support ORDER BY clause inside fixed part or
  pattern, you need to use the proper SORT element.


## Status

This is an ongoing development [check the jira tikets](https://jira.nuxeo.com/issues/?jql=project%20%3D%20NXP%20AND%20component%20%3D%20Elasticsearch%20AND%20Status%20!%3D%20%22Resolved%22%20ORDER%20BY%20updated%20DESC%2C%20priority%20DESC%2C%20created%20ASC)
