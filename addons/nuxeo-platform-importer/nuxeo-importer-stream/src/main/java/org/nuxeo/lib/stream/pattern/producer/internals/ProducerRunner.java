/*
 * (C) Copyright 2017 Nuxeo SA (http://nuxeo.com/) and others.
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
 * Contributors:
 *     bdelbosc
 */
package org.nuxeo.lib.stream.pattern.producer.internals;

import static java.lang.Thread.currentThread;

import java.util.concurrent.Callable;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.lib.stream.log.LogAppender;
import org.nuxeo.lib.stream.pattern.Message;
import org.nuxeo.lib.stream.pattern.consumer.internals.ConsumerRunner;
import org.nuxeo.lib.stream.pattern.producer.ProducerFactory;
import org.nuxeo.lib.stream.pattern.producer.ProducerIterator;
import org.nuxeo.lib.stream.pattern.producer.ProducerStatus;

import com.codahale.metrics.Counter;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.SharedMetricRegistries;
import com.codahale.metrics.Timer;

/**
 * A callable pulling a producer iterator in loop.
 *
 * @since 9.1
 */
public class ProducerRunner<M extends Message> implements Callable<ProducerStatus> {
    private static final Log log = LogFactory.getLog(ProducerRunner.class);

    protected final int producerId;

    protected final LogAppender<M> appender;

    protected final ProducerFactory<M> factory;

    protected String threadName;

    protected final MetricRegistry registry = SharedMetricRegistries.getOrCreate(
            ConsumerRunner.NUXEO_METRICS_REGISTRY_NAME);

    protected final Timer producerTimer;

    protected final Counter producersCount;

    protected long counter;

    public ProducerRunner(ProducerFactory<M> factory, LogAppender<M> appender, int producerId) {
        this.factory = factory;
        this.producerId = producerId;
        this.appender = appender;
        producerTimer = registry.timer(MetricRegistry.name("nuxeo", "importer", "stream", "producer"));
        producersCount = registry.counter(MetricRegistry.name("nuxeo", "importer", "stream", "producers"));
        log.debug("ProducerIterator thread created: " + producerId);
    }

    @Override
    public ProducerStatus call() throws Exception {
        threadName = currentThread().getName();
        long start = System.currentTimeMillis();
        producersCount.inc();
        try (ProducerIterator<M> producer = factory.createProducer(producerId)) {
            producerLoop(producer);
        } finally {
            producersCount.dec();
        }
        return new ProducerStatus(producerId, counter, start, System.currentTimeMillis(), false);
    }

    protected void producerLoop(ProducerIterator<M> producer) {
        M message;
        while (producer.hasNext()) {
            try (Timer.Context ignored = producerTimer.time()) {
                message = producer.next();
                setThreadName(message);
                counter++;
            }
            appender.append(producer.getPartition(message, appender.size()), message);
        }
    }

    protected void setThreadName(M message) {
        String name = threadName + "-" + counter;
        if (message != null) {
            name += "-" + message.getId();
        } else {
            name += "-null";
        }
        currentThread().setName(name);
    }
}
