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

import java.util.Arrays;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;

import static org.junit.Assert.*;


@RunWith(FeaturesRunner.class)
@Features({ KafkaFeature.class })
public class TestKafkaSetup {

    protected static final Log log = LogFactory.getLog(TestKafkaSetup.class);

    @Test
    public void testTopicReadWrite() throws InterruptedException{

        // setup producer
        KafkaProducer<String, String> producer = KafkaHelper.createProducer(KafkaFeature.BROKERHOST, KafkaFeature.BROKERPORT);

        // setup consumer
        KafkaConsumer<String, String> consumer = KafkaHelper.createConsumer(KafkaFeature.BROKERHOST, KafkaFeature.BROKERPORT);
        consumer.subscribe(Arrays.asList(KafkaFeature.TOPIC));

        // send something
        ProducerRecord<String, String> data = new ProducerRecord<>(KafkaFeature.TOPIC, "T", "testMessage");
        producer.send(data);
        producer.flush();

        // check consumer !
        ConsumerRecords<String, String> records = consumer.poll(2000); // 2000 is the min !

        assertEquals(1, records.count());

        producer.close();
        consumer.close();
    }

}
