
## Lucene / H2

Current unit tests run using Derby rather than H2 because otherwise we have a conflict between the lucene version comming from H2 and the version of lucence comming from Elasticsearch.

The solution is to upgrade H2/Lucene integration.

## Async Indexing Tasks

We probably want to avoid to schedule multiple indexing of the same Document.

The current implementation is very bad : it used a single list in memory and the implementation is incomplete.

In the target solution we may choose :

 - to rely on the WorkManager + Redis ?
 - directly use Redis ?

## Lucene field configuration

To be added

## PageProvider impl




