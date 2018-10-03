nuxeo-core-bulk
===============

## About

This module provides the ability to execute actions asynchronously on a -possibly large- set of documents. This is done by leveraging [Nuxeo Streams](https://github.com/nuxeo/nuxeo/tree/master/nuxeo-runtime/nuxeo-runtime-stream#nuxeo-runtime-stream) that bring scalability and fault tolerance.

## Definitions


- __document set__: a list of documents of a repository represented as a list of document identifiers.

- __action__: an operation that can be applied to a document set.

- __bulk command__: a set of parameters building a request to apply an action on a document set.

- __bucket__: a portion of a document set that fits into a stream record.

- __batch__: a smaller (or equals) portion of a bucket where the action is applied within a transaction.

## Bulk Command

The bulk command is the input of the framework. It is composed by the user submitting the command, the repository, the NXQL query that materializes the document set, the unique name of the action to execute and some optional parameters that could be needed by the action:

```java
BulkCommand command = new BulkCommand().withRepository("myRepository")
                                       .withUsername("myUser")
                                       .withQuery("SELECT * from Document")
                                       .withAction("myAction")
                                       .withParam("param1", "myParam1")
                                       .withParam("param2", "myParam2")
```

## Execution flow

![baf](bulk-overview.png)

### The BulkService
The entry point is the [BulkService](https://github.com/nuxeo/nuxeo/blob/master/nuxeo-core/nuxeo-core-bulk/src/main/java/org/nuxeo/ecm/core/bulk/BulkService.java) that takes a bulk command as an input. The service submits this command, meaning it is sent to the `command` stream.

The BulkService can also returns the status of a command which is internally stored into a KeyValueStore.

### The Scroller computation

The `command` stream is the input of the `Scroller` computation.

This computation scrolls the database using a cursor to retrieve the document ids matching the NXQL query.
The ids are grouped into a bucket that fit into a record.

The bucket record is sent directly to the action stream given in the command.

The scroller will also sent status update to inform that the scroll is in progress or terminated and to set the total number of document in the materialized document set.

### Actions processors

Each action is run its own stream processor (a topology of computation).

The action to be part of the bulk service must respect the following contract:

- action must send a status update to inform on the number of document processed for a command,
  at some point these reported processed documents must match the total number of document in the set.

- action that want to aggregate results per command must handle interleaved commands by maintaining a local state
  per command, checkpoint must be done only when there are no aggregation in progress.

An `AbstractBulkComputation` is provided so an action can be implemented easily with a single computation
See `SetPropertiesAction` for a trivial example.

See The `CSVExportAction` and particularly the `MakeBlob` computation for an advanced example.


### The Status computation

This computation reads from the `status` stream and aggregate status update to build the current status of command.
The status is saved into a KeyValueStore.
When the number of processed document is equals to the number of document in the set, the state is changed to completed.
And the computation appends the final status to the `done` stream.

This `done` stream can be used as an input by custom computation to execute other actions once a command is completed.


## How to contribute an action

You need to register a couple action / stream processor :

```xml
<extension target="org.nuxeo.ecm.core.bulk" point="actions">
  <action name="myAction" bucketSize="100" batchSize="20"/>
</extension>
```

```xml
<extension target="org.nuxeo.runtime.stream.service" point="streamProcessor">
  <streamProcessor name="myAction" class="org.nuxeo.ecm.core.bulk.action.MyActionProcessor" logConfig="bulk"
      defaultConcurrency="2" defaultPartitions="4" />
</extension>
```

You can find more info on how to configure a stream processor in the following link:
https://github.com/nuxeo/nuxeo/tree/master/nuxeo-runtime/nuxeo-runtime-stream#stream-processing


### Following Project QA Status

[![Build Status](https://qa.nuxeo.org/jenkins/buildStatus/icon?job=master/nuxeo-master)](https://qa.nuxeo.org/jenkins/job/master/job/nuxeo-master/)

## About Nuxeo
Nuxeo dramatically improves how content-based applications are built, managed and deployed, making customers more agile, innovative and successful. Nuxeo provides a next generation, enterprise ready platform for building traditional and cutting-edge content oriented applications. Combining a powerful application development environment with SaaS-based tools and a modular architecture, the Nuxeo Platform and Products provide clear business value to some of the most recognizable brands including Verizon, Electronic Arts, Sharp, FICO, the U.S. Navy, and Boeing. Nuxeo is headquartered in New York and Paris. More information is available at www.nuxeo.com.
