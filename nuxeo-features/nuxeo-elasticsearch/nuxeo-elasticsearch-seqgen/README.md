nuxeo-elasticsearch-seqgen
==========================

## About

This project provides a sequence number generator based on Elasticsearch by contributing to the `sequencers` extension point of the `UIDGeneratorService`.

## Why this module?

The Elasticsearch backend for the Nuxeo Audit service `ESAuditBackend` needs to generate sequence ids.

It seems bad to use a SQL database just for handling these sequences: that's why this ES based implementation does exist.

## How it works

The implementation is based on the Blog post "[ElasticSearch::Sequence - a blazing fast ticket server](http://blogs.perl.org/users/clinton_gormley/2011/10/elasticsearchsequence---a-blazing-fast-ticket-server.html)".

Basically, it uses an index with a single entry where the revision number is used as current value of the sequence.

## Using it

    UIDGeneratorService service = Framework.getService(UIDGeneratorService.class);
    UIDSequencer seq = service.getSequencer();
    // The previous call assumes the `uidgen` contribution is the default one, else you need to specify the sequencer name explicitely:
    // UIDSequencer seq = service.getSequencer("uidgen");
    int number = seq.getNext(key);

## Building

To build and run the tests, simply run the Maven build:

    mvn clean install
