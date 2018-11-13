nuxeo-runtime-stream
======================

## About

This module provides an integration of nuxeo-stream with Nuxeo.
It adds 2 services:
- a Kafka configuration service: to register Kafka and Zookeeper access and consumer producer properties.
- a Stream service: to define LogManager configuration, initialize Log and start StreamProcessor.

## Kafka Configurations

 You can register one or multiple Kafka configurations using this Nuxeo extension point:

```xml
<?xml version="1.0"?>
<component name="my.project.kafka.contrib">
  <extension target="org.nuxeo.runtime.stream.kafka.service" point="kafkaConfig">
    <kafkaConfig name="default" zkServers="localhost:2181" topicPrefix="nuxeo-">      
      <producer>
        <property name="bootstrap.servers">localhost:9092</property>
      </producer>      
      <consumer>
        <property name="bootstrap.servers">localhost:9092</property>
        <property name="request.timeout.ms">65000</property>
        <property name="max.poll.interval.ms">60000</property>
        <property name="session.timeout.ms">20000</property>
        <property name="heartbeat.interval.ms">1000</property>
        <property name="max.poll.records">50</property>
      </consumer>     
    </kafkaConfig>
  </extension>
</component>
```

This Kafka configuration named `default` can be used in the Log configuration below.

Make sure you have read the [nuxeo-stream README](../nuxeo-stream/README.md) to setup properly Kafka.


## Stream Service

This service enable to define Log configurations and to register stream processors.

### The Log configuration

There are 2 types of Log configurations:

- Chronicle: limited for single node: all producers and consumers must be on the same node.
- Kafka: required for distributed producers and consumers.

You can define a Log configuration with the following Nuxeo extension point:

```xml
<?xml version="1.0"?>
<component name="my.project.stream.log.contrib">
  <extension target="org.nuxeo.runtime.stream.service" point="logConfig">
    <!-- Chronicle impl, default storage under default directory, default retention -->
    <logConfig name="default" />
    <!-- Chronicle impl, storage in /tmp/imp, a week of retention -->
    <logConfig name="import" type="chronicle">
      <option name="directory">imp</option>
      <option name="basePath">/tmp</option>
      <option name="retention">7d</option>
    </logConfig>
    <!-- Chronicle impl, default storage and retention,
         create a Log named myStream with 7 partitions if it does not exist. -->
    <logConfig name="custom">
      <log name="myStream" size="7" />
    </logConfig>
    <!-- Kafka impl, referencing the default Kafka config -->
    <logConfig name="work" type="kafka">
      <option name="kafkaConfig">default</option>
    </logConfig>
    <!-- Kafka impl,
         create a Log named pubSub if it does not exist. -->
    <logConfig name="nuxeo" type="kafka">
      <option name="kafkaConfig">default</option>
      <log name="pubSub" size="1" />
    </logConfig>


  </extension>
</component>
```

The default Log type is Chronicle.

The default retention for Chronicle is 4 days, this can be changed using the `nuxeo.conf` option: `nuxeo.stream.chronicle.retention.duration`.

The retention value is expressed as a string like: `12h` or `7d`, respectively for 12 hours and 7 days.

The default storage for Chronicle is: `${nuxeo.data.dir}/data/stream`, this path can be changed using the `nuxeo.conf` option: `nuxeo.stream.chronicle.dir`.


#### Using Log from Nuxeo

The Nuxeo Stream service enables to get and share access to LogManager:

```java
  StreamService service = Framework.getService(StreamService.class);
  LogManager manager = service.getLogManager("custom");
  // write a record to myStream, the log exists because it has been initialized by the service 
  LogAppender<Record> appender = manager.getAppender("myStream");
  appender.append(key, Record.of(key, value.getBytes()));

  // read
  try (LogTailer<Record> tailer = manager.createTailer("myGroup", "myStream")) {
      LogRecord<Record> logRecord = tailer.read(Duration.ofSeconds(1));
      assertEquals(key, logRecord.message().key);
  }
  // don't close the manager, this is done by the service
```

### Stream processing

It is possible to register stream processors, this way they are initialized and started with Nuxeo.

The extension point refer to a class that returns the topology of computations, 
the settings are configurable in the contribution.

```xml
<?xml version="1.0"?>
<component name="my.project.stream.stream.contrib">
  <extension target="org.nuxeo.runtime.stream.service" point="streamProcessor">    
  <streamProcessor name="myStreamProcessor" logConfig="default" defaultConcurrency="4" defaultPartitions="12"
      class="org.nuxeo.runtime.stream.tests.MyStreamProcessor">
      <stream name="output" partitions="1" />
      <computation name="myComputation" concurrency="8" />
    </streamProcessor>
  </extension>
</component>
```

### Following Project QA Status

[![Build Status](https://qa.nuxeo.org/jenkins/buildStatus/icon?job=master/nuxeo-master)](https://qa.nuxeo.org/jenkins/job/master/job/nuxeo-master/)


## About Nuxeo
Nuxeo dramatically improves how content-based applications are built, managed and deployed, making customers more agile, innovative and successful. Nuxeo provides a next generation, enterprise ready platform for building traditional and cutting-edge content oriented applications. Combining a powerful application development environment with SaaS-based tools and a modular architecture, the Nuxeo Platform and Products provide clear business value to some of the most recognizable brands including Verizon, Electronic Arts, Sharp, FICO, the U.S. Navy, and Boeing. Nuxeo is headquartered in New York and Paris. More information is available at www.nuxeo.com.
