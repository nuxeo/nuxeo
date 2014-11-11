nuxeo-elasticsearch
===================

## About

This project aims at providing Nuxeo bundles to integrate
Elasticsearch with the Nuxeo Platform.

The idea is to index/query Nuxeo Documents to/from Elasticsearch.

## Building

To build and run the tests, simply start the Maven build:

    mvn clean install

You can also download prebuilt packages from our QA server:

http://qa.nuxeo.org/jenkins/job/addons_nuxeo-elasticsearch-master/lastStableBuild/artifact/target/

Or use the Marketplace package:

https://github.com/nuxeo/marketplace-elasticsearch

## Limitations

- The 5.8 version does not support default H2 database and don't have Aggregate support.

- Only supports a single repository.

- ACL filtering works fine only on simplified ACL. We don't support
  negative ACE other than `Deny Everyone`. Documents with negative ACL
  will not be accessible for non administrator accounts.

- NXQL Limitations:

  - Fulltext search on field is supported if the Elasticsearch mapping
	is properly defined. For instance `ecm:fulltext.dc:title = 'foo*'`
	works if there is a multi field `dc:title.fulltext`. Visit the
	[default configuration](https://github.com/nuxeo/nuxeo-elasticsearch/blob/master/nuxeo-elasticsearch-core/src/main/resources/OSGI-INF/elasticsearch-default-index-contrib.xml)
	to find some examples.
  - The fulltext syntax accepted is different from the default NXQL.
    It is the Elasticsearch
    [simple query string syntax](http://www.elasticsearch.org/guide/en/elasticsearch/reference/current/query-dsl-simple-query-string-query.html#_simple_query_string_syntax).
  - For now the `ecm:fulltext` matches the Elasticsearch `_all` field
    which is the concatenation of all fields. This is different from
    the NXQL `ecm:fulltext` which matches only some explicit fields.
    Also custom fulltext indexes are not supported.
    `ecm:fulltext_someindex` will match the `_all` field.
  - Fields are case-sensitive: `ecm:primaryType` vs `ecm:primarytype`
  - No select clause support, so NO: DISTINCT, COUNT, AVERAGE, MAX,
    MIN, operators like: + - / *.
  - ILIKE will work only with a proper mapping with an analyzer
    configured to lowercase index.
  - No support of complex list correlations like:

        files/*1/file/name LIKE '%.txt' AND files/*1/file/length = 0)

- Content view does not support ORDER BY clause inside fixed part or
  pattern. You need to use the proper SORT element.


## Status

This is an ongoing development [check the jira tikets](https://jira.nuxeo.com/issues/?jql=project%20%3D%20NXP%20AND%20component%20%3D%20Elasticsearch%20AND%20Status%20!%3D%20%22Resolved%22%20ORDER%20BY%20updated%20DESC%2C%20priority%20DESC%2C%20created%20ASC)
