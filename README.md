nuxeo-elasticsearch
===================

## About

This project aims at providing Nuxeo Bundles to integrate ElasticSearch with Nuxeo Platform.

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

- ACL filtering works fine only on simplified ACL, we don't support negative ACE
  other than block everyone. Documents with negative ACL will not be accessible
  for non administrator account.

- ORDER BY on a fulltext field don't work properly NXP-14246

- Content view does not support ORDER BY clause inside fixed part or
  pattern, you need to use the proper SORT element.

- NXQL Limitations:

  - field are case sensitive ecm:primaryType vs ecm:primarytype
  - For now the `ecm:fulltext` match the Elasticsearch `_all` field
    which is the concatenation of all fields, this is different from
    the NXQL `ecm:fulltext` which match only some explicit fields.
  - fields declared as fulltext in contribution are requested as
    fulltext NXP-14247
  - no select clause: distinct, count, average, max, min, operators
  - ILIKE will work only with a proper analyzer configured to
    lowercase index
  - no complex list correlation like

        files/*1/file/name LIKE '%.txt' AND files/*1/file/length = 0)

## Status

This is an ongoing development [check the jira tikets](https://jira.nuxeo.com/issues/?jql=project%20%3D%20NXP%20AND%20component%20%3D%20Elasticsearch%20AND%20Status%20!%3D%20%22Resolved%22%20ORDER%20BY%20updated%20DESC%2C%20priority%20DESC%2C%20created%20ASC)
