/*
 * (C) Copyright 2020 Nuxeo SA (http://nuxeo.com/) and others.
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
package org.nuxeo.runtime.stream;

import static org.nuxeo.lib.stream.computation.log.ComputationRunner.NUXEO_METRICS_REGISTRY_NAME;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.logging.log4j.Logger;
import org.nuxeo.lib.stream.codec.AvroMessageCodec;
import org.nuxeo.lib.stream.codec.Codec;
import org.nuxeo.lib.stream.computation.AbstractComputation;
import org.nuxeo.lib.stream.computation.ComputationContext;
import org.nuxeo.lib.stream.computation.Record;
import org.nuxeo.lib.stream.computation.Watermark;
import org.nuxeo.lib.stream.log.Latency;
import org.nuxeo.lib.stream.log.LogManager;
import org.nuxeo.lib.stream.log.internals.LogPartitionGroup;
import org.nuxeo.runtime.api.Framework;

import io.dropwizard.metrics5.Gauge;
import io.dropwizard.metrics5.MetricName;
import io.dropwizard.metrics5.MetricRegistry;
import io.dropwizard.metrics5.SharedMetricRegistries;

/**
 * @since 11.1
 */
public class StreamMetricsComputation extends AbstractComputation {
    private static final Logger log = org.apache.logging.log4j.LogManager.getLogger(
            StreamMetricsComputation.class);

    protected static final String NAME = "streamMetrics";

    protected MetricRegistry registry = SharedMetricRegistries.getOrCreate(NUXEO_METRICS_REGISTRY_NAME);

    protected final long intervalMs;

    protected final List<String> inputStreams;

    protected final List<String> streams = new ArrayList<>();

    protected final Set<String> invalidStreams = new HashSet<>();

    protected final List<LogPartitionGroup> groups = new ArrayList<>();

    protected final List<LatencyMetric> metrics = new ArrayList<>();

    protected LogManager manager;

    protected final Codec<Record> codec = new AvroMessageCodec<>(Record.class);

    protected long refreshGroupCounter;

    public StreamMetricsComputation(Duration interval, List<String> streams) {
        super(NAME, 1, 0);
        this.intervalMs = interval.toMillis();
        this.inputStreams = streams;
    }

    @Override
    public void init(ComputationContext context) {
        if (context.isSpareComputation()) {
            log.info("Spare instance nothing to report");
            unregisterMetrics();
        } else {
            log.warn("Instance elected to report stream metrics");
            context.setTimer("tracker", System.currentTimeMillis() + intervalMs);
        }
    }

    @Override
    public void destroy() {
        unregisterMetrics();
    }

    protected void registerMetrics() {
        unregisterMetrics();
        getGroups().forEach(group -> metrics.add(new LatencyMetric(group, registry)));
    }

    protected void unregisterMetrics() {
        metrics.forEach(LatencyMetric::destroy);
        metrics.clear();
    }

    @Override
    public void processTimer(ComputationContext context, String key, long timestamp) {
        refreshMetricsIfNeeded();
        log.debug("start update metrics: " + metrics.size());
        List<LatencyMetric> toRemove = metrics.stream()
                                              .filter(metric -> metric.update(getManager(), codec))
                                              .collect(Collectors.toList());
        toRemove.forEach(LatencyMetric::destroy);
        toRemove.forEach(metric -> invalidStreams.add(metric.getStream()));
        metrics.removeAll(toRemove);
        context.setTimer("tracker", System.currentTimeMillis() + intervalMs);
    }

    protected void refreshMetricsIfNeeded() {
        if (streams.isEmpty() || groups.isEmpty() || metrics.isEmpty() || ++refreshGroupCounter % 5 == 0) {
            streams.clear();
            groups.clear();
            registerMetrics();
        }
    }

    protected List<String> getStreams() {
        if (streams.isEmpty()) {
            if (inputStreams == null || inputStreams.isEmpty()) {
                streams.addAll(getManager().listAll());
                log.debug("Use all available streams: {}", streams);
            } else {
                streams.addAll(inputStreams);
                log.debug("Use input streams: {}", streams);
            }
            if (!invalidStreams.isEmpty()) {
                streams.removeAll(invalidStreams);
                log.debug("Filtered list of streams: {}", streams);
            }
        }
        return streams;
    }

    protected List<LogPartitionGroup> getGroups() {
        if (groups.isEmpty()) {
            getStreams().forEach(name -> {
                getManager().listConsumerGroups(name)
                            .forEach(group -> groups.add(new LogPartitionGroup(group, name, 0)));
            });
            log.info("Update list of consumers: {}", groups);
        }
        return groups;
    }

    protected LogManager getManager() {
        if (manager == null) {
            manager = Framework.getService(StreamService.class).getLogManager("default");
        }
        return manager;
    }

    @Override
    public void processRecord(ComputationContext context, String inputStreamName, Record record) {
        // this is not supposed to happen
    }

    public static class LatencyMetric {
        public static final Object PREFIX = "nuxeo.streams.global.stream.group.";

        protected final LogPartitionGroup consumer;

        protected final MetricRegistry registry;

        protected final MetricName endMetric;

        protected final MetricName posMetric;

        protected final MetricName lagMetric;

        protected final MetricName latMetric;

        protected Latency latency;

        protected boolean registered;

        public LatencyMetric(LogPartitionGroup consumer, MetricRegistry registry) {
            this.consumer = consumer;
            this.registry = registry;
            endMetric = getMetricName("end");
            posMetric = getMetricName("pos");
            lagMetric = getMetricName("lag");
            latMetric = getMetricName("latency");
        }

        protected MetricName getMetricName(String name) {
            return MetricName.build(PREFIX + name).tagged("stream", consumer.name).tagged("group", consumer.group);
        }

        protected void registerMetrics() {
            registry.register(endMetric, (Gauge<Long>) () -> latency.lag().upper());
            registry.register(posMetric, (Gauge<Long>) () -> latency.lag().lower());
            registry.register(lagMetric, (Gauge<Long>) () -> latency.lag().lag());
            registry.register(latMetric, (Gauge<Long>) () -> latency.latency());
        }

        protected void unregisterMetrics() {
            registry.remove(endMetric);
            registry.remove(posMetric);
            registry.remove(lagMetric);
            registry.remove(latMetric);
        }

        public boolean update(LogManager manager, Codec<Record> codec) {
            try {
                latency = manager.getLatency(consumer.name, consumer.group, codec,
                        (rec -> Watermark.ofValue(rec.getWatermark()).getTimestamp()), (Record::getKey));
                if (!registered) {
                    registerMetrics();
                    registered = true;
                }
            } catch (Exception e) {
                if (e.getCause() instanceof ClassNotFoundException || e.getCause() instanceof ClassCastException
                        || e instanceof IllegalStateException || e instanceof IllegalArgumentException) {
                    log.warn("Invalid stream, cannot get latency: " + consumer, e);
                    return true;
                }
                throw e;
            }
            return false;
        }

        public void destroy() {
            unregisterMetrics();
        }

        public String getStream() {
            return consumer.getLogPartition().name();
        }

    }

}
