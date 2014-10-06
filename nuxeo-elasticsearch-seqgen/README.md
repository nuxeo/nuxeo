nuxeo-elasticsearch-seqgen
==========================

## About

This project provides a service for generating sequence numbers.

This implementation uses Elasticsearch instead of a SQL Database.

## Why this service ?

The Elasticsearch backend for the Nuxeo Audit service needs to generate sequence ids.

It seems bad to use a SQL Database just for handling these sequences : that's why this ES based implementation does exist.

## How it works

The implementation is based on the Blog post "[ElasticSearch::Sequence - a blazing fast ticket server](http://blogs.perl.org/users/clinton_gormley/2011/10/elasticsearchsequence--a-blazing-fast-ticket-server.html)".

Basically, it uses an index with a single entry where the revisionn number is used has current value of the sequence.

## Building

To build and run the tests, simply start the Maven build:

    mvn clean install

