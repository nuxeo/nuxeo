nuxeo-importer-queue
======================

## About

This module implements a single producer, multi consumers importer pattern.

A producer generates source nodes that are dispatched into differents queues.

Each queue has a consumer thread that reads source nodes and create Nuxeo documents, performing commit by batch.

In case of error the consumer rollback and replay the batch of nodes one by one to isolate a potential invalide node.

Nodes that can not be consumed are marked as rejected. 

The queues are bounded, so when a queue is full the producer will wait for its consumer before continuing its processing.
This means that to work efficiently:
- the dispatch should distribute as equaly as possible the nodes between queues
- a consumer should never hang (use timeouts when using webservices)
- the queue length should be long enough

When a consumer get an unexpeced error it stops processing nodes but it drains the queue to rejected nodes. This prevents to
block the producer.


## Warning

This module is under developpent and still experimental, interfaces and implementations will change until it is announced as a stable module.


## TODO

- Clean stats, add a retry counter
- Add metrics: timers on processing, queue sizes ...
- Use TP Executor for consumers
- Restart consumer on unexpected error
- Get consumer result stats and error using CompletableFuture
- Stop consumer threads using a poison pill source node
- Provide a Kafka implementation, this will enable to:
    - distribute consumer on multiple Nuxeo node
    - stop and continue import process, producer can be run only once, consumer can be run multiple time
    - store error nodes in a topic, so they can be reprocessed
    - never block the producer
    - scale to billions of docs
    - use Kafka stream workflow to manipulate source nodes before being injected


## Building

To build and run the tests, simply start the Maven build:

    mvn clean install

## Links

- [Open jira tikets](https://jira.nuxeo.com/browse/NXP-19902?jql=project%20%3D%20NXP%20AND%20component%20%3D%20Importer%20AND%20status%20!%3D%20Resolved%20ORDER%20BY%20updated%20DESC%2C%20priority%20DESC)
