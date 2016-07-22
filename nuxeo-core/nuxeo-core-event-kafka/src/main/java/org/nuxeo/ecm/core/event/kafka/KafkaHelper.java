/*
 * (C) Copyright 2016 Nuxeo SA (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl-2.1.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     tiry
 */
package org.nuxeo.ecm.core.event.kafka;

import java.util.Properties;

import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;

/**
 * Helper to host Kafka Consumer/Producer setup code
 *
 * @since 8.4
 */
public class KafkaHelper {

    public static Properties getProducerSettings(String host, String port) {
        Properties producerProps = new Properties();
        producerProps.setProperty("bootstrap.servers", host + ":" + port);
        producerProps.put("acks", "all");
        producerProps.put("retries", 0);
        producerProps.put("batch.size", 16384);
        producerProps.put("linger.ms", 1);
        producerProps.put("buffer.memory", 33554432);
        producerProps.setProperty("key.serializer", "org.apache.kafka.common.serialization.StringSerializer");
        producerProps.setProperty("value.serializer", "org.apache.kafka.common.serialization.StringSerializer");
        return producerProps;
    }

    public static Properties getConsumerSettings(String host, String port) {
        Properties consumerProps = new Properties();
        consumerProps.setProperty("bootstrap.servers", host + ":" + port);
        consumerProps.setProperty("group.id", "group0");
        consumerProps.put("enable.auto.commit", "true");
        consumerProps.put("auto.commit.interval.ms", "1000");
        consumerProps.setProperty("key.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");
        consumerProps.setProperty("value.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");
        consumerProps.put("auto.offset.reset", "earliest");
        return consumerProps;
    }

    public static KafkaProducer<String, String> createProducer(String host, String port) {
        Properties producerProps = getProducerSettings(host, port);
        return new KafkaProducer<String, String>(producerProps);
    }

    public static KafkaConsumer<String, String> createConsumer(String host, String port) {
        Properties consumerProps = getConsumerSettings(host, port);
        return new KafkaConsumer<>(consumerProps);
    }

}
