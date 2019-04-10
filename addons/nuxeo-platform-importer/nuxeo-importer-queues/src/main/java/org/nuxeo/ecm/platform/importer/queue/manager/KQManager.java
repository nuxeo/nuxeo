/*
 * (C) Copyright 2006-2016 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 *
 * Contributors:
 *     anechaev
 */
package org.nuxeo.ecm.platform.importer.queue.manager;

import com.google.common.collect.Lists;
import kafka.api.FetchRequest;
import kafka.api.FetchRequestBuilder;
import kafka.api.PartitionFetchInfo;
import kafka.common.TopicAndPartition;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.Metric;
import org.apache.kafka.common.MetricName;
import org.apache.kafka.common.TopicPartition;
import org.nuxeo.common.utils.ExceptionUtils;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.platform.importer.kafka.service.DefaultKafkaService;
import org.nuxeo.ecm.platform.importer.log.BufferredLogger;
import org.nuxeo.ecm.platform.importer.log.ImporterLogger;
import org.nuxeo.ecm.platform.importer.source.Node;
import org.nuxeo.runtime.api.Framework;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class KQManager<N extends Node> extends AbstractQueuesManager<N> {

    private static final Log log = LogFactory.getLog(KQManager.class);

    private final List<LinkedList<N>> listCache;
    private final KafkaConsumer<String, N>[] consumers;
    private List<KafkaProducer<String, N>> producers;
    private List<String> topics;
    private String client;

    private Properties producerProps;
    private Properties consumerProps;

    @SuppressWarnings("unchecked")
    public KQManager(ImporterLogger logger, int queuesNb) {
        super(logger, queuesNb);

        listCache = Collections.synchronizedList(new ArrayList<>(
                Collections.nCopies(queuesNb, new LinkedList<>())
        ));

        DefaultKafkaService service = Framework.getService(DefaultKafkaService.class);

        try {
            topics = service.propagateTopics(queuesNb, (short)1, 10000);
        } catch (IOException e) {
            throw new NuxeoException("Fail to initialize Kafka topics", e);
        }
        producerProps = service.getProducerProperties();
        consumerProps = service.getConsumerProperties();
        client = (String) producerProps.get(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG);

        producers = Collections.synchronizedList(new ArrayList<>(queuesNb));
        consumers = new KafkaConsumer[queuesNb];

        propagateProducers(queuesNb);
    }

    @Override
    public void put(int queue, N node) throws InterruptedException {
        String randTopic = topics.get(queue % topics.size());
        ProducerRecord<String, N> record = new ProducerRecord<>(randTopic, queue, node.getName(), node);
        producers.get(queue)
                .send(record);
    }

    @Override
    public N poll(int queue) {
        try {
            return poll(queue, 2000, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            log.error(e);
            ExceptionUtils.checkInterrupt(e);
            return null;
        }
    }

    @Override
    public synchronized N poll(int queue, long timeout, TimeUnit unit) throws InterruptedException {
        if (!listCache.get(queue).isEmpty()) {
            return listCache.get(queue).removeFirst();
        } else {
            KafkaConsumer<String, N> consumer;
            if (consumers[queue] == null) {
                consumer = new KafkaConsumer<>(consumerProps);
                consumers[queue] = consumer;
                List<TopicPartition> partitions = topics.stream()
                        .map(topic -> new TopicPartition(topic, queue))
                        .collect(Collectors.toList());
                consumer.assign(partitions);
            } else {
                consumer = consumers[queue];
            }

            consumer.commitSync();
            ConsumerRecords<String, N> records = consumer.poll(unit.toMillis(timeout));

            // poll one more time;
            if (records.isEmpty()) {
                records = consumer.poll(unit.toMillis(timeout));
            }

            N node = null;
            for (ConsumerRecord<String, N> record : records) {
                if (node == null) {
                    node = record.value();
                } else {
                    listCache.get(queue)
                            .add(record.value());
                }
            }

            return node;
        }
    }

    @Override
    public boolean isEmpty(int queue) {
//        Map<TopicAndPartition, PartitionFetchInfo> map = new HashMap<>();
//        PartitionFetchInfo info = new PartitionFetchInfo(1, 512);
//
//        FetchRequest request = new FetchRequest(-1, client, 5000, 8, 24000,map);
        return listCache.get(queue).isEmpty();
    }

    @Override
    public int size(int queue) {
        return listCache.size();
    }

    public void getStat() {
        Map<TopicAndPartition, PartitionFetchInfo> map = new HashMap<>();
        PartitionFetchInfo info = new PartitionFetchInfo(1, 512);
        ImporterLogger logger = new BufferredLogger(log);
        FetchRequest request = new FetchRequestBuilder()
                .clientId(client)
                .replicaId(0)
                .requestVersion((short) -1)
                .build();

        Arrays.stream(consumers)
                .forEach(c -> {
                    logger.info(String.format("Consumer assigned to %d partition, working on thread: %s", c.assignment().iterator().next().partition(), Thread.currentThread().getName()));
                    for (Map.Entry<MetricName, ? extends Metric> entry : c.metrics().entrySet()) {
                        log.info(String.format("Key: %s, Value: %f", entry.getKey().name(), entry.getValue().value()));
                    }
                });

        logger.info(request.describe(true));
    }

    public void close() {
        producers.forEach(KafkaProducer::close);
        Lists.newArrayList(consumers).forEach(KafkaConsumer::close);
    }

    public List<String> allTopics() {
        return topics;
    }

    private void propagateProducers(int queues) {
        IntStream.range(0, queues)
                .forEach( i -> producers.add(new KafkaProducer<>(producerProps)));
    }
}