/*
 * (C) Copyright 2016 Nuxeo SA (http://nuxeo.com/) and others.
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
 */
package org.nuxeo.ecm.platform.importer.queue.producer;

import com.codahale.metrics.Counter;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.SharedMetricRegistries;
import org.nuxeo.ecm.platform.importer.log.ImporterLogger;
import org.nuxeo.ecm.platform.importer.queue.AbstractTaskRunner;
import org.nuxeo.ecm.platform.importer.source.Node;
import org.nuxeo.ecm.platform.importer.queue.manager.QueuesManager;
import org.nuxeo.runtime.metrics.MetricsService;

import java.util.Random;

/**
 * @since 8.3
 */
public abstract class AbstractProducer extends AbstractTaskRunner implements Producer {

    final protected ImporterLogger log;

    protected QueuesManager qm;

    protected final MetricRegistry registry = SharedMetricRegistries.getOrCreate(MetricsService.class.getName());

    protected final Counter producerCounter;

    protected final Random rand;

    public AbstractProducer(ImporterLogger log) {
        assert(log != null);
        this.log = log;
        producerCounter = registry.counter(MetricRegistry.name("nuxeo", "importer", "queue", "producer"));
        rand = new Random(System.currentTimeMillis());
    }

    @Override
    public void init(QueuesManager qm) {
        this.qm = qm;
    }

    protected void dispatch(Node node) throws InterruptedException {
        int idx = getTargetQueue(node, qm.count());
        qm.put(idx, node);
        producerCounter.inc();
        incrementProcessed();
    }

    @Override
    public int getTargetQueue(Node node, int nbQueues) {
        return rand.nextInt(nbQueues);
    }
}
