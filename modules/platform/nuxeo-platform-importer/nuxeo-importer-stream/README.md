nuxeo-importer-stream
======================

## About

This module defines a producer/consumer pattern and uses the Log features provided by Nuxeo Stream.

## Producer/Consumer pattern with automation operations

The Log is used to perform mass import.

It decouples the Extraction/Transformation from the Load (using the [ETL](https://en.wikipedia.org/wiki/Extract-transform-load) terminology).

The extraction and transformation is done by a document message producer with custom logic.

This module comes with a random document and a random blob generator, that does the same job as the random importer of the `nuxeo-importer-core` module.

The load into Nuxeo is done with a generic consumer.

Automation operations are exposed to run producers and consumers.


### Two steps import: Generate and Import documents with blobs

1. Run a random producers of document messages, these message represent Folder and File document a blob. The total number of document created is: `nbThreads * nbDocuments`.
  ```
curl -X POST 'http://localhost:8080/nuxeo/site/automation/StreamImporter.runRandomDocumentProducers' -u Administrator:Administrator -H 'content-type: application/json' \
  -d '{"params":{"nbDocuments": 100, "nbThreads": 5}}'
```

| Params| Default | Description |
| --- | ---: | --- |
| `nbDocuments` |  | The number of documents to generate per producer thread |
| `nbThreads` | `8` | The number of concurrent producer to run |
| `avgBlobSizeKB` | `1` | The average blob size fo each file documents in KB. If set to `0` create File document without blob. |
| `lang` | `en_US` |The locale used for the generated content, can be `fr_FR` or `en_US` |
| `logName` | `import/doc` | The name of the Log. |
| `logSize` | `$nbThreads` |The number of partitions in the Log which will fix the maximum number of consumer threads |
| `logBlobInfo` |  | A Log name containing blob information to use, see section below for use case |
| `logConfig` | `default` | The Log configuration registered in Nuxeo |

2. Run consumers of document messages creating Nuxeo documents, the concurrency will match the previous nbThreads producers parameters
  ```
curl -X POST 'http://localhost:8080/nuxeo/site/automation/StreamImporter.runDocumentConsumers' -u Administrator:Administrator -H 'content-type: application/json' \
  -d '{"params":{"rootFolder": "/default-domain/workspaces"}}'
```

| Params| Default | Description |
| --- | ---: | --- |
| `rootFolder` |  | The path of the Nuxeo container to import documents, this document must exists |
| `repositoryName` |  | The repository name used to import documents |
| `nbThreads` | `logSize` | The number of concurrent consumer, should not be greater than the number of partition in the Log |
| `batchSize` | `10` | The consumer commit documents every batch size |
| `batchThresholdS` | `20` | The consumer commit documents if the transaction is longer that this threshold |
| `retryMax` | `3` | Number of time a consumer retry to import in case of failure |
| `retryDelayS` | `2` | Delay between retries |
| `logName` | `import/doc` | The name of the Log to tail |
| `logConfig` | `default` | The Log configuration registered in Nuxeo |
| `useBulkMode` | `false` | Process asynchronous listeners in bulk mode |
| `blockIndexing` | `false` | Do not index created document with Elasticsearch |
| `blockAsyncListeners` | `false` | Do not process any asynchronous listeners |
| `blockPostCommitListeners` | `false` | Do not process any post commit listeners |
| `blockDefaultSyncListeners` | `false` | Disable some default synchronous listeners: dublincore, mimetype, notification, template, binarymetadata and uid |

### 4 steps import: Generate and Import blobs, then Generate and Import documents

1. Run producers of random blob messages
  ```
curl -X POST 'http://localhost:8080/nuxeo/site/automation/StreamImporter.runRandomBlobProducers' -u Administrator:Administrator -H 'content-type: application/json' \
  -d '{"params":{"nbBlobs": 100, "nbThreads": 5}}'
```

| Params| Default | Description |
| --- | ---: | --- |
| `nbBlobs` |  | The number of blobs to generate per producer thread |
| `nbThreads` | `8` | The number of concurrent producer to run |
| `avgBlobSizeKB` | `1` | The average blob size fo each file documents in KB |
| `lang` | `en_US` | The locale used for the generated content, can be "fr_FR" or "en_US" |
| `logName` | `import/blob` |  The name of the Log to append blobs. |
| `logSize` | `$nbThreads`| The number of partitions in the Log which will fix the maximum number of consumer threads |
| `logConfig` | `default` | The Log configuration registered in Nuxeo |

2. Run consumers of blob messages importing into the Nuxeo binary store, saving blob information into a new Log.
  ```
curl -X POST 'http://localhost:8080/nuxeo/site/automation/StreamImporter.runBlobConsumers' -u Administrator:Administrator -H 'content-type: application/json' \
  -d '{"params":{"blobProviderName": "default", "logBlobInfo": "blob-info"}}'
```

| Params| Default | Description |
| --- | ---: | --- |
| `blobProviderName` | `default` | The name of the binary store blob provider |
| `logName` | `import/blob` | The name of the Log that contains the blob |
| `logBlobInfo` | `import/blob-info` | The name of the Log to append blob information about imported blobs |
| `nbThreads` | `$logSize` | The number of concurrent consumer, should not be greater than the number of partitions in the Log |
| `retryMax` | `3` | Number of time a consumer retry to import in case of failure |
| `retryDelayS` | `2` | Delay between retries |
| `logConfig` | `default` | The Log configuration registered in Nuxeo |

3. Run producers of random Nuxeo document messages which use produced blobs created in step 2
  ```
curl -X POST 'http://localhost:8080/nuxeo/site/automation/StreamImporter.runRandomDocumentProducers' -u Administrator:Administrator -H 'content-type: application/json' \
  -d '{"params":{"nbDocuments": 200, "nbThreads": 5, "logBlobInfo": "blob-info"}}'
```
Same params listed in the previous previous runRandomDocumentProducers call, here we set the `logBlobInfo` parameter.

4. Run consumers of document messages
  ```
curl -X POST 'http://localhost:8080/nuxeo/site/automation/StreamImporter.runDocumentConsumers' -u Administrator:Administrator -H 'content-type: application/json' \
  -d '{"params":{"rootFolder": "/default-domain/workspaces"}}'
```

Same params listed in the previous previous runDocumentConsumers call.

### Create blobs using existing files

Create a file containing the list of files to import then:

1. Generate blob messages corresponding to the files, dispatch the messages into 4 partitions:
  ```
curl -X POST 'http://localhost:8080/nuxeo/site/automation/StreamImporter.runFileBlobProducers' -u Administrator:Administrator -H 'content-type: application/json' \
  -d '{"params":{"listFile": "/tmp/my-file-list.txt", "logSize": 4}}'
```

| Params| Default | Description |
| --- | ---: | --- |
| `listFile` | | The path to the listing file |
| `basePath` | '' | The base path to use as prefix of each file listed in the `listFile` |
| `nbBlobs` | 0 | The number of blobs to generate per producer thread, 0 means all entries, loop on `listFile` entries if necessary |
| `nbThreads` | `1` | The number of concurrent producer to run |
| `logName` | `import/blob` |  The name of the Log to append blobs. |
| `logSize` | `$nbThreads`| The number of partitions in the Log which will fix the maximum number of consumer threads |
| `logConfig` | `default` | The Log configuration registered in Nuxeo |


The you can use the 3 others steps describes the above section to import blobs with 4 threads and create documents.

Note that the type of document will be adapted to the detected mime type of the file so that
- image file will generate a `Picture` document
- video file will generate a `Video` document
- other type will be translated to `File` document


### Generate random file for testing purpose

For testing purpose it can be handy to generate different file from an existing one, the goal is to generate lots of unique files with a limited set of files.

To do this you need to first generates blob messages pointing to file (see previous section) and choose the `nbBlobs` corresponding to the expected number of blob to import,
(use a greater number that the existing files).

The next step is to add some special option to blob consumer so that instead of importing the existing file, a watermark will be
added to the blob before importing it.


2. Run consumers of blob messages adding watermark to file and importing into the Nuxeo binary store, saving blob information into a new Log.
  ```
curl -X POST 'http://localhost:8080/nuxeo/site/automation/StreamImporter.runBlobConsumers' -u Administrator:Administrator -H 'content-type: application/json' \
  -d '{"params":{"watermark": "foo"}}'
```

The additional parameters are:

| Params| Default | Description |
| --- | ---: | --- |
| `watermark` | | Ask to add a watermark to the file before importing it, use the provided string if possible. |
| `persistBlobPath` | | Use a path if you want to keep the generated files on disk |
| `blobProviderName` | `default` | If blank there is no Nuxeo blob import, this can be useful for import with Gatling/Redis |


Continue with other steps described above to generate and create documents.

Note that only few mime type are supported for watermark so far:
- `text/plain`: Insert a uniq tag at the beginning of text.
- `image/jpeg`: Set the exif software tag to a uniq tag.
- `video/mp4`:  Set the title with the uniq tag.


### Import document using REST API via Gatling/Redis

Instead of doing mass import creating document by batch with the efficient internal API,
you can save them into Redis in a way it can be used by Gatling simulation, this way we can stress the REST API.

To do this instead of the document creationg step 4 we do:

4. Run Redis consumers of document messages
  ```
curl -X POST 'http://localhost:8080/nuxeo/site/automation/StreamImporter.runRedisDocumentConsumers' -u Administrator:Administrator -H 'content-type: application/json' \
  -d '{"params":{"rootFolder": "/default-domain/workspaces"}}'
```

Note that the Nuxeo must be configured with Redis (`nuxeo.redis.enabled=true`).

After this you need to use simulations in `nuxeo-distribution/nuxeo-jsf-ui-gatling-tests/`:

```
# init the infra, creating a group of test users and a workspace
mvn -nsu gatling:test -Dgatling.simulationClass=org.nuxeo.cap.bench.Sim00Setup -Pbench -DredisDb=0 -Durl=http://localhost:8080/nuxeo

# import the folder structure
mvn -nsu gatling:test -Dgatling.simulationClass=org.nuxeo.cap.bench.Sim10CreateFolders -Pbench -DredisDb=0 -Durl=http://localhost:8080/nuxeo

# import the documents using 8 concurrent users
mvn -nsu gatling:test -Dgatling.simulationClass=org.nuxeo.cap.bench.Sim20CreateDocuments -Pbench -DredisDb=0 -Dusers=8 -Durl=http://localhost:8080/nuxeo

```

The node running the Gatling simulation must have access to the files to import.

Here is an overview of possible usage to generate mass import and load tests with the stream importer:

![import diagram](import-diag.png)

Visit [nuxe-jsf-ui-gatling](https://github.com/nuxeo/nuxeo/tree/master/nuxeo-distribution/nuxeo-jsf-ui-gatling-tests) for more information.



## Building

To build and run the tests, simply start the Maven build:

    mvn clean install

### Following Project QA Status

[![Build Status](https://qa.nuxeo.org/jenkins/buildStatus/icon?job=master/addon_nuxeo-platform-importer-master)](https://qa.nuxeo.org/jenkins/job/master/job/addon_nuxeo-platform-importer-master/)


## About Nuxeo
Nuxeo dramatically improves how content-based applications are built, managed and deployed, making customers more agile, innovative and successful. Nuxeo provides a next generation, enterprise ready platform for building traditional and cutting-edge content oriented applications. Combining a powerful application development environment with SaaS-based tools and a modular architecture, the Nuxeo Platform and Products provide clear business value to some of the most recognizable brands including Verizon, Electronic Arts, Sharp, FICO, the U.S. Navy, and Boeing. Nuxeo is headquartered in New York and Paris. More information is available at www.nuxeo.com.
