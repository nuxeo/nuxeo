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
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.nuxeo.ecm.core.event.EventBundle;
import org.nuxeo.ecm.core.event.EventServiceAdmin;
import org.nuxeo.ecm.core.event.impl.AsyncEventExecutor;
import org.nuxeo.ecm.core.event.impl.EventListenerDescriptor;
import org.nuxeo.ecm.core.event.impl.EventListenerList;
import org.nuxeo.ecm.core.event.pipe.AbstractEventBundlePipe;
import org.nuxeo.runtime.api.Framework;

/**
 * @since 8.4
 */
public class KafkaPipe extends AbstractEventBundlePipe<String> {

    public static final String BROKERHOST = "KafkaBrokerHost";

    public static final String BROKERPORT = "KafkaBrokerPort";

    public static final String TOPIC = "KafkaTopic";

    protected String brokerHost;

    protected String brokerPort;

    protected String topic;

    protected KafkaProducer<String, String> producer;

    protected KafkaConsumer<String, String> consumer;

    protected boolean stop = false;

    protected ThreadPoolExecutor consumerTPE;

    protected EventBundleJSONIO io = new EventBundleJSONIO();

    @Override
    public void initPipe(String name, Map<String, String> params) {
        super.initPipe(name, params);

        brokerHost = params.get(BROKERHOST);
        if (brokerHost == null) {
            brokerHost = Framework.getProperty("org.nuxeo." + BROKERHOST, "127.0.0.1");
        }
        brokerPort = params.get(BROKERPORT);
        if (brokerPort == null) {
            brokerPort = Framework.getProperty("org.nuxeo." + BROKERPORT, "2181");
        }

        topic = params.get(TOPIC);
        if (topic == null) {
            topic = Framework.getProperty("org.nuxeo." + TOPIC, "NUXEO");
        }

        // setup producer
        producer = KafkaHelper.createProducer(brokerHost, brokerPort);

        // setup consumer
        consumer = KafkaHelper.createConsumer(brokerHost, brokerPort);
        consumer.subscribe(Arrays.asList(topic));

        initConsumerThread();

    }

    protected void initConsumerThread() {

        AsyncEventExecutor asyncExec = new AsyncEventExecutor();

        consumerTPE = new ThreadPoolExecutor(1, 1, 60, TimeUnit.MINUTES, new LinkedBlockingQueue<Runnable>());
        consumerTPE.prestartCoreThread();
        consumerTPE.execute(new Runnable() {

            protected void process(ConsumerRecord<String, String> record) {
                String message = record.value();

                EventBundle bundle = io.unmarshal(message);

                // direct exec ?!
                EventServiceAdmin eventService = Framework.getService(EventServiceAdmin.class);
                EventListenerList listeners = eventService.getListenerList();
                List<EventListenerDescriptor> postCommitAsync = listeners.getEnabledAsyncPostCommitListenersDescriptors();

                asyncExec.run(postCommitAsync, bundle);
            }

            @Override
            public void run() {

                while (!stop) {
                    ConsumerRecords<String, String> records = consumer.poll(2000);
                    Iterator<ConsumerRecord<String, String>> recordIterator = records.iterator();
                    while (recordIterator.hasNext()) {
                        ConsumerRecord<String, String> record = recordIterator.next();
                        process(record);
                    }
                }

                consumer.close();
            }
        });

    }

    @Override
    public void shutdown() throws InterruptedException {
        stop = true;
        waitForCompletion(5000L);
        consumerTPE.shutdown();
        producer.close();
    }

    @Override
    public boolean waitForCompletion(long timeoutMillis) throws InterruptedException {
        producer.flush();
        Thread.sleep(2000); // XXX
        return true;
    }

    @Override
    protected String marshall(EventBundle events) {
        return io.marshall(events);
    }

    @Override
    protected void send(String message) {
        ProducerRecord<String, String> data = new ProducerRecord<>(topic, null, message);
        producer.send(data);
    }

}
