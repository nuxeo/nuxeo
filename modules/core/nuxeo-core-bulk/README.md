nuxeo-core-bulk
===============

## About

This module provides the ability to execute actions asynchronously on a -possibly large- set of documents. This is done by leveraging [Nuxeo Streams](https://github.com/nuxeo/nuxeo/tree/master/nuxeo-runtime/nuxeo-runtime-stream#nuxeo-runtime-stream) that bring scalability and fault tolerance.

## Definitions


- __document set__: a list of documents from a repository represented as a list of document identifiers.

- __action__: an operation that can be applied to a document set.

- __command__: a set of parameters building a request to apply an action on a document set.

- __bucket__: a portion of a document set that fits into a stream record.

- __batch__: a smaller (or equals) portion of a bucket where the action is applied within a transaction.

## Requirements

To work properly the Bulk Service need a true KeyValue storage to store the command its status,
there are 2 possibles choices:

- Use `RedisKeyValueStore` this is the case if you have `nuxeo.redis.enabled=true` in your nuxeo.conf.
- Use `MongoDBKeyValueStore` this is the case if you are using the `mongodb` template.

You should not rely on the default `MemKeyValueStore` implementation that flushes the data on restart.


## Bulk Command

The bulk command is the input of the framework.
It is composed by the unique name of the action to execute, the NXQL query that materializes the document set, the user submitting the command, the repository and some optional parameters that could be needed by the action:

```java
BulkCommand command = new BulkCommand.Builder("myAction", "SELECT * from Document")
                                        .repository("myRepository")
                                        .user("myUser")
                                        .param("param1", "myParam1")
                                        .param("param2", "myParam2")
                                        .build();
String commandId = Framework.getService(BulkService.class).submit(command);
```


## Execution flow

![baf](bulk-overview.png)

### The BulkService
The entry point is the [`BulkService`](./src/main/java/org/nuxeo/ecm/core/bulk/BulkService.java) that takes a bulk command as an input.
The service submits this command, meaning it appends the `BulkCommand` to the `command` stream.

The BulkService can also returns the status of a command which is internally stored into a KeyValueStore.

### The Scroller computation

The `command` stream is the input of the `Scroller` computation.

This computation scrolls the database using a cursor to retrieve the document ids matching the NXQL query.
The ids are grouped into a bucket that fit into a record.

The `BulkBucket` record is appended to the action's stream.

The scroller send command status update to inform that the scroll is in progress or terminated and to set the total number of document in the materialized document set.

### Actions processors

Each action runs its own stream processor (a topology of computations).

The action processor must respect the following rules:

- action must send a status update containing the number of processed documents since the last update.

- action must handle possible error, for instance the user that send the command might not have write permission on all documents

- the total number of processed documents reported must match at some point the number of documents in the document set.

- action that aggregates bucket records per command must handle interleaved commands.
  This can be done by maintaining a local state for each command.

- action that aggregates bucket records per command should checkpoint only when there no other interleaved command in progress.
  This is to prevent checkpoint while some records are not yet processed resulting in possible loss of record.

An [`AbstractBulkComputation`](./src/main/java/org/nuxeo/ecm/core/bulk/action/computation/AbstractBulkComputation.java) is provided so an action can be implemented easily with a single computation
See [`SetPropertiesAction`](./src/main/java/org/nuxeo/ecm/core/bulk/action/SetPropertiesAction.java) for a simple example.

See The [`CSVExportAction`](./src/main/java/org/nuxeo/ecm/core/bulk/action/CSVExportAction.java) and particularly the [`MakeBlob`](./src/main/java/org/nuxeo/ecm/core/bulk/action/computation/MakeBlob.java) computation for an advanced example.


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
  <streamProcessor name="myAction" class="org.nuxeo.ecm.core.bulk.action.MyActionProcessor"
      defaultConcurrency="2" defaultPartitions="4" />
</extension>
```

See [`SetPropertiesAction`](./src/main/java/org/nuxeo/ecm/core/bulk/action/SetPropertiesAction.java) for a very basic action implementation.

You can find more info on how to configure a stream processor in the following link:
https://github.com/nuxeo/nuxeo/tree/master/nuxeo-runtime/nuxeo-runtime-stream#stream-processing


## Testing a bulk action with REST API

Here is an example on how to launch a bulk command and get status:

```bash
## Run a bulk action
curl -s -X POST 'http://localhost:8080/nuxeo/site/automation/Bulk.RunAction' -u Administrator:Administrator -H 'content-type: application/json+nxrequest' -d '{
  "context": {},
  "params": {
    "action": "csvExport",
    "query": "SELECT * FROM File WHERE ecm:isVersion = 0 AND ecm:isTrashed = 0",
    "parameters": {}
  }
}' | tee /tmp/bulk-command.txt
# {"commandId":"e8cc059d-6b9d-480b-a6e1-b0edace6d982"}

## Extract the command id from the output
commandId=$(cat /tmp/bulk-command.txt | jq .[] | tr -d '"')

## Ask for the command status
curl -s -X GET "http://localhost:8080/nuxeo/api/v1/bulk/$commandId"  -u Administrator:Administrator  -H 'content-type: application/json' | jq .
# {
#  "entity-type": "bulkStatus",
#  "commandId": "e8cc059d-6b9d-480b-a6e1-b0edace6d982",
#  "state": "RUNNING",
#  "processed": 0,
#  "total": 1844,
#  "submitted": "2018-10-11T13:10:26.825Z",
#  "scrollStart": "2018-10-11T13:10:26.827Z",
#  "scrollEnd": "2018-10-11T13:10:26.846Z",
#  "completed": null
#}

## Wait for the completion of the command, this is only for testing purpose
## a normal client should poll the status regularly instead of using this call:
curl -X POST 'http://localhost:8080/nuxeo/site/automation/Bulk.WaitForAction' -u Administrator:Administrator -H 'content-type: application/json+nxrequest' -d $'{
  "context": {},
  "params": {
    "commandId": "'"$commandId"'",
    "timeoutSecond": "3600"
  }
}'
# {"entity-type":"boolean","value":true}

## Get the status again:
curl -s -X GET "http://localhost:8080/nuxeo/api/v1/bulk/$commandId"  -u Administrator:Administrator  -H 'content-type: application/json' | jq .
#{
#  "entity-type": "bulkStatus",
#  "commandId": "e8cc059d-6b9d-480b-a6e1-b0edace6d982",
#  "state": "COMPLETED",
#  "processed": 1844,
#  "total": 1844,
#  "submitted": "2018-10-11T13:10:26.825Z",
#  "scrollStart": "2018-10-11T13:10:26.827Z",
#  "scrollEnd": "2018-10-11T13:10:26.846Z",
#  "completed": "2018-10-11T13:10:28.243Z"
#}
```

Also a command can be aborted, this is useful for long running command launched by error,
or to by pass a command that fails systematically which blocks the entire action processor:

```
## Abort a command
curl -s -X PUT "http://localhost:8080/nuxeo/api/v1/bulk/$commandId/abort"  -u Administrator:Administrator  -H 'content-type: application/json' | jq .

```



## Debugging

All streams used by the bulk service and action can be introspected using
the Nuxeo `bin/stream.sh` script.

For instance to see the latest commands submitted:
```bash
## When using Kafka
./bin/stream.sh tail -k -l bulk-command --codec avro
## When using Chronicle Queue
# ./bin/stream.sh tail --chronicle ./nxserver/data/stream/bulk -l command --codec avro
```
| offset | watermark | flag | key | length | data |
| --- | --- | --- | --- | ---: | --- |
|bulk-command-01:+2|2018-10-11 11:18:34.955:0|[DEFAULT]|setProperties|164|{"id": "b667b677-d40e-471a-8377-eb16dd301b42", "action": "setProperties", "query": "Select * from Document", "username": "Administrator", "repository": "default", "bucketSize": 100, "batchSize": 25, "params": "{\"dc:description\":\"a new new testbulk description\"}"}|
|bulk-command-00:+2|2018-10-11 15:10:26.826:0|[DEFAULT]|csvExport|151|{"id": "e8cc059d-6b9d-480b-a6e1-b0edace6d982", "action": "csvExport", "query": "SELECT * FROM File WHERE ecm:isVersion = 0 AND ecm:isTrashed = 0", "username": "Administrator", "repository": "default", "bucketSize": 100, "batchSize": 50, "params": null}|


To get the latest commands completed:
```bash
./bin/stream.sh tail -k -l bulk-done --codec avro
```
| offset | watermark | flag | key | length | data |
| --- | --- | --- | --- | ---: | --- |
|bulk-done-00:+4|2018-10-11 14:23:29.219:0|[DEFAULT]|580df47d-dd90-4d16-b23c-0e39ae363e06|96|{"commandId": "580df47d-dd90-4d16-b23c-0e39ae363e06", "action": "csvExport", "delta": false, "processed": 3873, "state": "COMPLETED", "submitTime": 1539260607207, "scrollStartTime": 1539260607275, "scrollEndTime": 1539260607326, "completedTime": 1539260609218, "total": 3873, "result": null}|
|bulk-done-00:+5|2018-10-11 15:10:28.244:0|[DEFAULT]|e8cc059d-6b9d-480b-a6e1-b0edace6d982|96|{"commandId": "e8cc059d-6b9d-480b-a6e1-b0edace6d982", "action": "csvExport", "delta": false, "processed": 1844, "state": "COMPLETED", "submitTime": 1539263426825, "scrollStartTime": 1539263426827, "scrollEndTime": 1539263426846, "completedTime": 1539263428243, "total": 1844, "result": null}|


Of course you can view the BulkBucket message
```bash
./bin/stream.sh tail -k -l bulk-csvExport --codec avro
```
| offset | watermark | flag | key | length | data |
| --- | --- | --- | --- | ---: | --- |
|bulk-csvExport-01:+48|2018-10-11 15:10:26.842:0|[DEFAULT]|e8cc059d-6b9d-480b-a6e1-b0edace6d982:18|3750|{"commandId": "e8cc059d-6b9d-480b-a6e1-b0edace6d982", "ids": ["763135b8-ca49-4eea-9a52-1ceaa227e60a", ...]}|

And check for any lag on any computation, for more information on `stream.sh`:
```bash
./bin/stream.sh help
```

### Following Project QA Status

[![Build Status](https://qa.nuxeo.org/jenkins/buildStatus/icon?job=master/nuxeo-master)](https://qa.nuxeo.org/jenkins/job/master/job/nuxeo-master/)

## About Nuxeo
Nuxeo dramatically improves how content-based applications are built, managed and deployed, making customers more agile, innovative and successful. Nuxeo provides a next generation, enterprise ready platform for building traditional and cutting-edge content oriented applications. Combining a powerful application development environment with SaaS-based tools and a modular architecture, the Nuxeo Platform and Products provide clear business value to some of the most recognizable brands including Verizon, Electronic Arts, Sharp, FICO, the U.S. Navy, and Boeing. Nuxeo is headquartered in New York and Paris. More information is available at www.nuxeo.com.
