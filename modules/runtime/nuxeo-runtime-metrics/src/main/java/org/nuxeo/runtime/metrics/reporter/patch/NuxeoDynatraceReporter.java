/*
 * (C) Copyright 2026 Nuxeo (http://nuxeo.com/) and others.
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
package org.nuxeo.runtime.metrics.reporter.patch;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.concurrent.TimeUnit;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import io.dropwizard.metrics5.Counter;
import io.dropwizard.metrics5.Gauge;
import io.dropwizard.metrics5.Histogram;
import io.dropwizard.metrics5.Meter;
import io.dropwizard.metrics5.MetricAttribute;
import io.dropwizard.metrics5.MetricFilter;
import io.dropwizard.metrics5.MetricName;
import io.dropwizard.metrics5.MetricRegistry;
import io.dropwizard.metrics5.ScheduledReporter;
import io.dropwizard.metrics5.Snapshot;
import io.dropwizard.metrics5.Timer;

/**
 * A Dropwizard Metrics ScheduledReporter that sends metrics to Dynatrace OneAgent via UDP.
 * <p>
 * Uses the DogStatsD-compatible format that Dynatrace supports: {@code metric.name:value|type|#dim1:val1,dim2:val2}
 * <p>
 * Metrics are sent over UDP which is non-blocking - if the OneAgent is not available, metrics are silently dropped
 * without affecting application performance.
 *
 * @since 2025.15
 */
public class NuxeoDynatraceReporter extends ScheduledReporter {

    private static final Logger log = LogManager.getLogger(NuxeoDynatraceReporter.class);

    /** Maximum UDP packet size to avoid fragmentation */
    private static final int MAX_PACKET_SIZE = 8192;

    /** Reserve bytes to ensure we flush before exceeding MAX_PACKET_SIZE */
    private static final int FLUSH_THRESHOLD = MAX_PACKET_SIZE - 256;

    private final InetSocketAddress address;

    private final Set<MetricAttribute> deniedExpansions;

    private final boolean emptyTimerAsCount;

    private final DatagramSocket socket;

    private final StatsDPayloadBuilder payloadBuilder;

    protected NuxeoDynatraceReporter(MetricRegistry registry, MetricFilter filter, String host, int port, String prefix,
            String hostname, Map<String, String> dimensions, Set<MetricAttribute> deniedExpansions,
            boolean emptyTimerAsCount) {
        super(registry, "dynatrace-reporter", filter, TimeUnit.SECONDS, TimeUnit.MILLISECONDS);
        this.address = new InetSocketAddress(host, port);
        this.deniedExpansions = deniedExpansions;
        this.emptyTimerAsCount = emptyTimerAsCount;
        this.payloadBuilder = new StatsDPayloadBuilder(prefix, hostname, dimensions);
        try {
            this.socket = new DatagramSocket();
        } catch (SocketException e) {
            throw new IllegalStateException("Cannot create UDP socket for Dynatrace reporter", e);
        }
    }

    public static Builder builder(MetricRegistry registry) {
        return new Builder(registry);
    }

    @Override
    @SuppressWarnings("rawtypes") // Gauge raw type inherited from ScheduledReporter
    public void report(SortedMap<MetricName, Gauge> gauges, SortedMap<MetricName, Counter> counters,
            SortedMap<MetricName, Histogram> histograms, SortedMap<MetricName, Meter> meters,
            SortedMap<MetricName, Timer> timers) {

        StringBuilder batch = new StringBuilder();

        for (Map.Entry<MetricName, Gauge> entry : gauges.entrySet()) {
            appendGauge(batch, entry.getKey(), entry.getValue());
            flushIfNeeded(batch);
        }

        for (Map.Entry<MetricName, Counter> entry : counters.entrySet()) {
            appendCounter(batch, entry.getKey(), entry.getValue());
            flushIfNeeded(batch);
        }

        for (Map.Entry<MetricName, Histogram> entry : histograms.entrySet()) {
            appendHistogram(batch, entry.getKey(), entry.getValue());
            flushIfNeeded(batch);
        }

        for (Map.Entry<MetricName, Meter> entry : meters.entrySet()) {
            appendMeter(batch, entry.getKey(), entry.getValue());
            flushIfNeeded(batch);
        }

        for (Map.Entry<MetricName, Timer> entry : timers.entrySet()) {
            appendTimer(batch, entry.getKey(), entry.getValue());
            flushIfNeeded(batch);
        }

        // Send remaining metrics
        if (batch.length() > 0) {
            send(batch.toString());
        }
    }

    private void flushIfNeeded(StringBuilder batch) {
        if (batch.length() >= FLUSH_THRESHOLD) {
            send(batch.toString());
            batch.setLength(0);
        }
    }

    @SuppressWarnings("rawtypes") // Gauge raw type from parent method
    private void appendGauge(StringBuilder sb, MetricName name, Gauge gauge) {
        Object value = gauge.getValue();
        if (value instanceof Number) {
            payloadBuilder.appendMetricLine(sb, name.getKey(), name.getTags(), ((Number) value).doubleValue());
        } else if (value instanceof Boolean) {
            payloadBuilder.appendMetricLine(sb, name.getKey(), name.getTags(), ((Boolean) value) ? 1.0 : 0.0);
        }
        // Skip non-numeric gauges
    }

    private void appendCounter(StringBuilder sb, MetricName name, Counter counter) {
        payloadBuilder.appendMetricLine(sb, name.getKey(), name.getTags(), counter.getCount());
    }

    private void appendHistogram(StringBuilder sb, MetricName name, Histogram histogram) {
        String baseName = name.getKey();
        Map<String, String> tags = name.getTags();
        Snapshot snapshot = histogram.getSnapshot();

        appendIfAllowed(sb, baseName, "count", tags, histogram.getCount(), MetricAttribute.COUNT);
        appendIfAllowed(sb, baseName, "min", tags, snapshot.getMin(), MetricAttribute.MIN);
        appendIfAllowed(sb, baseName, "max", tags, snapshot.getMax(), MetricAttribute.MAX);
        appendIfAllowed(sb, baseName, "mean", tags, snapshot.getMean(), MetricAttribute.MEAN);
        appendIfAllowed(sb, baseName, "stddev", tags, snapshot.getStdDev(), MetricAttribute.STDDEV);
        appendIfAllowed(sb, baseName, "p50", tags, snapshot.getMedian(), MetricAttribute.P50);
        appendIfAllowed(sb, baseName, "p75", tags, snapshot.get75thPercentile(), MetricAttribute.P75);
        appendIfAllowed(sb, baseName, "p95", tags, snapshot.get95thPercentile(), MetricAttribute.P95);
        appendIfAllowed(sb, baseName, "p98", tags, snapshot.get98thPercentile(), MetricAttribute.P98);
        appendIfAllowed(sb, baseName, "p99", tags, snapshot.get99thPercentile(), MetricAttribute.P99);
        appendIfAllowed(sb, baseName, "p999", tags, snapshot.get999thPercentile(), MetricAttribute.P999);
    }

    private void appendMeter(StringBuilder sb, MetricName name, Meter meter) {
        String baseName = name.getKey();
        Map<String, String> tags = name.getTags();

        appendIfAllowed(sb, baseName, "count", tags, meter.getCount(), MetricAttribute.COUNT);
        appendIfAllowed(sb, baseName, "m1_rate", tags, convertRate(meter.getOneMinuteRate()), MetricAttribute.M1_RATE);
        appendIfAllowed(sb, baseName, "m5_rate", tags, convertRate(meter.getFiveMinuteRate()), MetricAttribute.M5_RATE);
        appendIfAllowed(sb, baseName, "m15_rate", tags, convertRate(meter.getFifteenMinuteRate()),
                MetricAttribute.M15_RATE);
        appendIfAllowed(sb, baseName, "mean_rate", tags, convertRate(meter.getMeanRate()), MetricAttribute.MEAN_RATE);
    }

    private void appendTimer(StringBuilder sb, MetricName name, Timer timer) {
        // Handle empty timers: either report only count or skip entirely
        if (timer.getCount() == 0) {
            if (emptyTimerAsCount) {
                appendIfAllowed(sb, name.getKey(), "count", name.getTags(), 0, MetricAttribute.COUNT);
            }
            return;
        }

        String baseName = name.getKey();
        Map<String, String> tags = name.getTags();
        Snapshot snapshot = timer.getSnapshot();

        // Count and rates
        appendIfAllowed(sb, baseName, "count", tags, timer.getCount(), MetricAttribute.COUNT);
        appendIfAllowed(sb, baseName, "m1_rate", tags, convertRate(timer.getOneMinuteRate()), MetricAttribute.M1_RATE);
        appendIfAllowed(sb, baseName, "m5_rate", tags, convertRate(timer.getFiveMinuteRate()), MetricAttribute.M5_RATE);
        appendIfAllowed(sb, baseName, "m15_rate", tags, convertRate(timer.getFifteenMinuteRate()),
                MetricAttribute.M15_RATE);
        appendIfAllowed(sb, baseName, "mean_rate", tags, convertRate(timer.getMeanRate()), MetricAttribute.MEAN_RATE);

        // Duration statistics (converted to milliseconds)
        appendIfAllowed(sb, baseName, "min", tags, convertDuration(snapshot.getMin()), MetricAttribute.MIN);
        appendIfAllowed(sb, baseName, "max", tags, convertDuration(snapshot.getMax()), MetricAttribute.MAX);
        appendIfAllowed(sb, baseName, "mean", tags, convertDuration(snapshot.getMean()), MetricAttribute.MEAN);
        appendIfAllowed(sb, baseName, "stddev", tags, convertDuration(snapshot.getStdDev()), MetricAttribute.STDDEV);
        appendIfAllowed(sb, baseName, "p50", tags, convertDuration(snapshot.getMedian()), MetricAttribute.P50);
        appendIfAllowed(sb, baseName, "p75", tags, convertDuration(snapshot.get75thPercentile()), MetricAttribute.P75);
        appendIfAllowed(sb, baseName, "p95", tags, convertDuration(snapshot.get95thPercentile()), MetricAttribute.P95);
        appendIfAllowed(sb, baseName, "p98", tags, convertDuration(snapshot.get98thPercentile()), MetricAttribute.P98);
        appendIfAllowed(sb, baseName, "p99", tags, convertDuration(snapshot.get99thPercentile()), MetricAttribute.P99);
        appendIfAllowed(sb, baseName, "p999", tags, convertDuration(snapshot.get999thPercentile()),
                MetricAttribute.P999);

        // Sum (total time spent)
        appendIfAllowed(sb, baseName, "sum", tags, convertDuration(timer.getSum()), MetricAttribute.SUM);
    }

    private void appendIfAllowed(StringBuilder sb, String baseName, String suffix, Map<String, String> tags,
            double value, MetricAttribute attr) {
        if (!deniedExpansions.contains(attr)) {
            payloadBuilder.appendMetricLine(sb, baseName + "." + suffix, tags, value);
        }
    }

    private void send(String data) {
        try {
            byte[] bytes = data.getBytes(StandardCharsets.UTF_8);
            DatagramPacket packet = new DatagramPacket(bytes, bytes.length, address);
            socket.send(packet);
            if (log.isTraceEnabled()) {
                log.trace("Sent {} bytes to Dynatrace at {}", bytes.length, address);
            }
        } catch (IOException e) {
            // UDP is fire-and-forget, just log at debug level
            log.debug("Failed to send metrics to Dynatrace at {}: {}", address, e.getMessage());
        }
    }

    @Override
    public void stop() {
        super.stop();
        if (socket != null && !socket.isClosed()) {
            socket.close();
        }
        log.debug("Dynatrace reporter stopped");
    }

    /**
     * Builder for creating NuxeoDynatraceReporter instances.
     */
    public static class Builder {

        private final MetricRegistry registry;

        private String host = "localhost";

        private int port = 18125;

        private String prefix = "nuxeo";

        private String hostname;

        private Map<String, String> dimensions = Collections.emptyMap();

        private MetricFilter filter = MetricFilter.ALL;

        private Set<MetricAttribute> deniedExpansions = Collections.emptySet();

        private boolean emptyTimerAsCount;

        public Builder(MetricRegistry registry) {
            this.registry = registry;
        }

        public Builder withHost(String host) {
            this.host = host;
            return this;
        }

        public Builder withPort(int port) {
            this.port = port;
            return this;
        }

        public Builder withPrefix(String prefix) {
            this.prefix = prefix;
            return this;
        }

        public Builder withHostname(String hostname) {
            this.hostname = hostname;
            return this;
        }

        public Builder withDimensions(Map<String, String> dimensions) {
            this.dimensions = dimensions != null ? dimensions : Collections.emptyMap();
            return this;
        }

        public Builder withFilter(MetricFilter filter) {
            this.filter = filter != null ? filter : MetricFilter.ALL;
            return this;
        }

        public Builder withDeniedExpansions(Set<MetricAttribute> deniedExpansions) {
            this.deniedExpansions = deniedExpansions != null ? deniedExpansions : Collections.emptySet();
            return this;
        }

        public Builder withEmptyTimerAsCount(boolean emptyTimerAsCount) {
            this.emptyTimerAsCount = emptyTimerAsCount;
            return this;
        }

        public NuxeoDynatraceReporter build() {
            return new NuxeoDynatraceReporter(registry, filter, host, port, prefix, hostname, dimensions,
                    deniedExpansions, emptyTimerAsCount);
        }
    }
}
