nuxeo-elasticsearch-audit
=========================

## About

This project provides a backend based on Elasticsearch for Nuxeo for Audit Service.

The idea is to use Lucene / Elastocsearch as storage backend for the Audit trail entries.

Usage of Elasticsearch as a replacement for the JPA based default backend allows to easily make the Audit service scale :

 - when there are a lot of entries
     - lot of access
     - custom logging
 - when there are a lot of queries 
     - reporting
     - usage of sync systems like Nuxeo Drive
 - when custom attributes on Audit entries are used

## How it works

An Elasticsearch  based `AuditBackend` is contributed at startup to replace the default JPA based one and it use dedicated `audit` Elasticsearch index to handle storage and queries.

The queries and PageProviders are based on Elasticsearch native DSL : no automatic conversion is done between EJBQL and Elasticsearch DSL.

The orginal Audit service uses a JPA sequence to assign each audit entry a unique id.

In the Elasticsearch implementation, an alternate sequence genaration system is used : `nuxeo-elasticsearch-seqgen`.

## Data Migration

When `nuxeo-elasticsearch-audit` is deployed it will automatically replace the default JPA implementation.

However, if you have previous data inside JPA you have to migrate it.

The migration is done in a Worker that will simply go through all existing JPA Audit entries to store them inside the Elasticsearch index.

The migration is done using a configurable batch size (default is 1000 entries).

An Automation Operation is provided to trigger the migration Work.

Sample call using curl :

    curl -H 'Content-Type:application/json' -X POST -d '{"params":{"batchSize":5000}}' -u Administrator:Administrator http://127.0.0.1:8080/nuxeo/api/v1/automation/Audit.StartMigration
    

NB : Migration of 16 Millions entries on a C3.XLarge AWS instance with default PGSQL setup and default embedded Elasticsearch takes about 3h (1500 entries/s).

## Building

To build and run the tests, simply start the Maven build:

    mvn clean install
