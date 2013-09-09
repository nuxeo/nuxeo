This modules contains a first level integration of Elastic Search with Nuxeo

For now this is more a POC than a workable module.

However, it does contains some working feature : at least in unit tests.

## Nuxeo Runtime service to manage ElasticSeach

`ElasticSearchComponent` exposes 2 service interfaces :

 - `ElasticSearchService` to access ElasticSearch features
 - `ElasticSearchAdmin` to manage ElasticSearch administration

Uppon Nuxeo startup, depending on the configuration, the component will :

 - start an embedded ElasticSearch instance 
 - connect to an existing ElasticSearch instance
 - start a new ElasticSearch node in a separated Java process (using `ElasticSearchControler`)

## Indexing

`ElasticsearchIndexingListener` is declared a listener that will run each time a Document is created or modified.

This listener will then call `ElasticSearchService` to trigger indexing.

The actual indexing task will actually be handled inside an AsyncWorker via `IndexingWorker`

## Searching

Strangly enought, this is currently the less implemented part.

For now, you can directly access to bare ElasticSearch API.

An implementation that returns connected DocumentModel after post-filtring is in progress, but currently not workable.


