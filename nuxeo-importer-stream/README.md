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
curl -X POST 'http://localhost:8080/nuxeo/site/automation/StreamImporter.runRandomDocumentProducers' -u Administrator:Administrator -H 'content-type: application/json+nxrequest' \
  -d '{"params":{"nbDocuments": 100, "nbThreads": 5}}'
```

| Params| Default | Description |
| --- | ---: | --- |
| `nbDocuments` |  | The number of documents to generate per producer thread |
| `nbThreads` | `8` | The number of concurrent producer to run |
| `avgBlobSizeKB` | `1` | The average blob size fo each file documents in KB. If set to `0` create File document without blob. |
| `lang` | `en_US` |The locale used for the generated content, can be `fr_FR` or `en_US` |
| `logName` | `doc` | The name of the Log. |
| `logSize` | `$nbThreads` |The number of partitions in the Log which will fix the maximum number of consumer threads |
| `logBlobInfo` |  | A Log name containing blob information to use, see section below for use case |
| `logConfig` | `import` | The Log configuration registered in Nuxeo |

2. Run consumers of document messages creating Nuxeo documents, the concurrency will match the previous nbThreads producers parameters
  ```
curl -X POST 'http://localhost:8080/nuxeo/site/automation/StreamImporter.runDocumentConsumers' -u Administrator:Administrator -H 'content-type: application/json+nxrequest' \
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
| `logName` | `doc` | The name of the Log to tail |
| `logConfig` | `import` | The Log configuration registered in Nuxeo |
| `useBulkMode` | `false` | Process asynchronous listeners in bulk mode |
| `blockIndexing` | `false` | Do not index created document with Elasticsearch |
| `blockAsyncListeners` | `false` | Do not process any asynchronous listeners |
| `blockPostCommitListeners` | `false` | Do not process any post commit listeners |
| `blockDefaultSyncListeners` | `false` | Disable some default synchronous listeners: dublincore, mimetype, notification, template, binarymetadata and uid |

### 4 steps import: Generate and Import blobs, then Generate and Import documents

1. Run producers of random blob messages
  ```
curl -X POST 'http://localhost:8080/nuxeo/site/automation/StreamImporter.runRandomBlobProducers' -u Administrator:Administrator -H 'content-type: application/json+nxrequest' \
  -d '{"params":{"nbBlobs": 100, "nbThreads": 5}}'
```

| Params| Default | Description |
| --- | ---: | --- |
| `nbBlobs` |  | The number of blobs to generate per producer thread |
| `nbThreads` | `8` | The number of concurrent producer to run |
| `avgBlobSizeKB` | `1` | The average blob size fo each file documents in KB |
| `lang` | `en_US` | The locale used for the generated content, can be "fr_FR" or "en_US" |
| `logName` | `blob` |  The name of the Log to append blobs. |
| `logSize` | `$nbThreads`| The number of partitions in the Log which will fix the maximum number of consumer threads |
| `logConfig` | `import` | The Log configuration registered in Nuxeo |

2. Run consumers of blob messages importing into the Nuxeo binary store, saving blob information into a new Log.
  ```
mkdir /tmp/a
curl -X POST 'http://localhost:8080/nuxeo/site/automation/StreamImporter.runBlobConsumers' -u Administrator:Administrator -H 'content-type: application/json+nxrequest' \
  -d '{"params":{"blobProviderName": "default", "logBlobInfo": "blob-info"}}'
```

| Params| Default | Description |
| --- | ---: | --- |
| `blobProviderName` | `default` | The name of the binary store blob provider |
| `logName` | `blob` | The name of the Log that contains the blob |
| `logBlobInfo` | `blob-info` | The name of the Log to append blob information about imported blobs |
| `nbThreads` | `$logSize` | The number of concurrent consumer, should not be greater than the number of partitions in the Log |
| `retryMax` | `3` | Number of time a consumer retry to import in case of failure |
| `retryDelayS` | `2` | Delay between retries |
| `logConfig` | `import` | The Log configuration registered in Nuxeo |

3. Run producers of random Nuxeo document messages which use produced blobs created in step 2
  ```
curl -X POST 'http://localhost:8080/nuxeo/site/automation/StreamImporter.runRandomDocumentProducers' -u Administrator:Administrator -H 'content-type: application/json+nxrequest' \
  -d '{"params":{"nbDocuments": 200, "nbThreads": 5, "logBlobInfo": "blob-info"}}'
```
Same params listed in the previous previous runRandomDocumentProducers call, here we set the `logBlobInfo` parameter.

4. Run consumers of document messages
  ```
curl -X POST 'http://localhost:8080/nuxeo/site/automation/StreamImporter.runDocumentConsumers' -u Administrator:Administrator -H 'content-type: application/json+nxrequest' \
  -d '{"params":{"rootFolder": "/default-domain/workspaces"}}'
```

Same params listed in the previous previous runDocumentConsumers call.


## Building

To build and run the tests, simply start the Maven build:

    mvn clean install

### Following Project QA Status

[![Build Status](https://qa.nuxeo.org/jenkins/buildStatus/icon?job=master/addon_nuxeo-platform-importer-master)](https://qa.nuxeo.org/jenkins/job/master/job/addon_nuxeo-platform-importer-master/)


## About Nuxeo
Nuxeo dramatically improves how content-based applications are built, managed and deployed, making customers more agile, innovative and successful. Nuxeo provides a next generation, enterprise ready platform for building traditional and cutting-edge content oriented applications. Combining a powerful application development environment with SaaS-based tools and a modular architecture, the Nuxeo Platform and Products provide clear business value to some of the most recognizable brands including Verizon, Electronic Arts, Sharp, FICO, the U.S. Navy, and Boeing. Nuxeo is headquartered in New York and Paris. More information is available at www.nuxeo.com.
