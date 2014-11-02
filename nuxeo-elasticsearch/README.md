nuxeo-elasticsearch
===================

## About

This project aims at providing Nuxeo bundles to integrate
Elasticsearch with the Nuxeo Platform.

The idea is to index/query Nuxeo Documents to/from Elasticsearch.

## Building

To build and run the tests, simply start the Maven build:

    mvn clean install

## Elasticsearch configuration
See http://doc.nuxeo.com/display/NXDOC/Indexing+and+Query#IndexingandQuery-ElasticsearchConfigurationElasticsearchConfiguration


## Reindexing

When upgrading or changing the Elasticsearch configuration you need to index the content

     1. Go to Admin Center > Elasticsearch > Admin

     2. Use the ReIndex button
        This is an asynchron job, you can see in the Admin Center > Elasticsearch > Info
        when there is no more indexing activity.


## Change the Elasticsearch settings or mapping of an existing instance

See http://doc.nuxeo.com/x/WxI5AQ

## Limitations

See http://doc.nuxeo.com/display/NXDOC/NXQL#NXQL-ElasticsearchLimitations


## Reporting Problems

To understand why a document is not present in search results or not
indexed, you can activate a debug trace, look at the `lib/log4j.xml`
file and uncomment the ELASTIC section:

      <appender name="ELASTIC" class="org.apache.log4j.FileAppender">
        <errorHandler class="org.apache.log4j.helpers.OnlyOnceErrorHandler" />
        <param name="File" value="${nuxeo.log.dir}/elastic.log" />
        <param name="Append" value="false" />
        <layout class="org.apache.log4j.PatternLayout">
          <param name="ConversionPattern" value="%d{ISO8601} %-5p [%t][%c] %m%X%n" />
        </layout>
      </appender>

      <category name="org.nuxeo.elasticsearch" additivity="false">
        <priority value="TRACE" />
        <appender-ref ref="ELASTIC" />
      </category>

The `elastic.log` will contain all the requests done by Nuxeo to
Elasticsearch including the `curl` command ready to be copied/pasted in a
term.

If you run in default embedded mode you need to enable the HTTP access
to perform request, just add `elasticsearch.httpEnabled=true` in your
`nuxeo.conf`. Note that this is only for debug purpose you should never
ever expose publicly the Elasticsearch ports.


## Status

Check the ongoing development [check the jira tikets](https://jira.nuxeo.com/issues/?jql=project%20%3D%20NXP%20AND%20component%20%3D%20Elasticsearch%20AND%20Status%20!%3D%20%22Resolved%22%20ORDER%20BY%20updated%20DESC%2C%20priority%20DESC%2C%20created%20ASC)
