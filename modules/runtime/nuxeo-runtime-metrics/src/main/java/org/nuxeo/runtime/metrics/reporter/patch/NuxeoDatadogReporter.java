/*
 * Simplified BSD License
 *
 *  Copyright (c) 2014, Vistar Media
 *  All rights reserved.
 *
 *  Redistribution and use in source and binary forms, with or without
 *  modification, are permitted provided that the following conditions are met:
 *
 *      * Redistributions of source code must retain the above copyright notice,
 *        this list of conditions and the following disclaimer.
 *      * Redistributions in binary form must reproduce the above copyright notice,
 *        this list of conditions and the following disclaimer in the documentation
 *        and/or other materials provided with the distribution.
 *      * Neither the name of Vistar Media nor the names of its contributors
 *        may be used to endorse or promote products derived from this software
 *        without specific prior written permission.
 *
 *  THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 *  AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 *  IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 *  DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE
 *  FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 *  DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 *  SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 *  CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 *  OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 *  OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 * Contributors:
 *     coursera https://github.com/coursera/metrics-datadog/blob/master/metrics-datadog/src/main/java/org/coursera/metrics/datadog/DatadogReporter.java
 *     bdelbosc
 */

package org.nuxeo.runtime.metrics.reporter.patch;

import java.io.IOException;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.concurrent.TimeUnit;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.coursera.metrics.datadog.AwsHelper;
import org.coursera.metrics.datadog.DefaultMetricNameFormatter;
import org.coursera.metrics.datadog.MetricNameFormatter;
import org.coursera.metrics.datadog.model.DatadogGauge;
import org.coursera.metrics.datadog.transport.Transport;

import io.dropwizard.metrics5.Clock;
import io.dropwizard.metrics5.Counter;
import io.dropwizard.metrics5.Gauge;
import io.dropwizard.metrics5.Histogram;
import io.dropwizard.metrics5.Meter;
import io.dropwizard.metrics5.Metered;
import io.dropwizard.metrics5.MetricFilter;
import io.dropwizard.metrics5.MetricName;
import io.dropwizard.metrics5.MetricRegistry;
import io.dropwizard.metrics5.ScheduledReporter;
import io.dropwizard.metrics5.Snapshot;
import io.dropwizard.metrics5.Timer;

/**
 * A copy of Coursera DatadogReporter with minor adaptation to handle metric with tags.
 *
 * @since 11.1
 */
public class NuxeoDatadogReporter extends ScheduledReporter {
    protected static final Log log = LogFactory.getLog(NuxeoDatadogReporter.class);

    private static final Expansion[] STATS_EXPANSIONS = { Expansion.MAX, Expansion.MEAN, Expansion.MIN,
            Expansion.STD_DEV, Expansion.MEDIAN, Expansion.P75, Expansion.P95, Expansion.P98, Expansion.P99,
            Expansion.P999 };

    private static final Expansion[] RATE_EXPANSIONS = { Expansion.RATE_1_MINUTE, Expansion.RATE_5_MINUTE,
            Expansion.RATE_15_MINUTE, Expansion.RATE_MEAN };

    private final Transport transport;

    private final Clock clock;

    private final String host;

    private final EnumSet<Expansion> expansions;

    private final MetricNameFormatter metricNameFormatter;

    private final List<String> tags;

    private final String prefix;

    private Transport.Request request;

    private NuxeoDatadogReporter(MetricRegistry metricRegistry, Transport transport, MetricFilter filter, Clock clock,
                                 String host, EnumSet<Expansion> expansions, TimeUnit rateUnit, TimeUnit durationUnit,
                                 MetricNameFormatter metricNameFormatter, List<String> tags, String prefix) {
        super(metricRegistry, "datadog-reporter", filter, rateUnit, durationUnit);
        this.clock = clock;
        this.host = host;
        this.expansions = expansions;
        this.metricNameFormatter = metricNameFormatter;
        this.tags = (tags == null) ? new ArrayList<>() : tags;
        this.transport = transport;
        this.prefix = prefix;
    }

    @Override
    public void report(SortedMap<MetricName, Gauge> gauges, SortedMap<MetricName, Counter> counters,
            SortedMap<MetricName, Histogram> histograms, SortedMap<MetricName, Meter> meters,
            SortedMap<MetricName, Timer> timers) {
        final long timestamp = clock.getTime() / 1000;

        try {
            request = transport.prepare();

            for (Map.Entry<MetricName, Gauge> entry : gauges.entrySet()) {
                reportGauge(prefix(entry.getKey().getKey()), entry.getValue(), timestamp,
                        getTags(entry.getKey().getTags()));
            }

            for (Map.Entry<MetricName, Counter> entry : counters.entrySet()) {
                reportCounter(prefix(entry.getKey().getKey()), entry.getValue(), timestamp,
                        getTags(entry.getKey().getTags()));
            }

            for (Map.Entry<MetricName, Histogram> entry : histograms.entrySet()) {
                reportHistogram(prefix(entry.getKey().getKey()), entry.getValue(), timestamp,
                        getTags(entry.getKey().getTags()));
            }

            for (Map.Entry<MetricName, Meter> entry : meters.entrySet()) {
                reportMetered(prefix(entry.getKey().getKey()), entry.getValue(), timestamp,
                        getTags(entry.getKey().getTags()));
            }

            for (Map.Entry<MetricName, Timer> entry : timers.entrySet()) {
                reportTimer(prefix(entry.getKey().getKey()), entry.getValue(), timestamp,
                        getTags(entry.getKey().getTags()));
            }
            request.send();
        } catch (Throwable e) {
            log.error("Error reporting metrics to Datadog", e);
        }
    }

    protected List<String> getTags(Map<String, String> metricTags) {
        List<String> ret = new ArrayList<>(tags);
        metricTags.forEach((k, v) -> ret.add(k + ":" + v));
        return ret;
    }

    private void reportTimer(String name, Timer timer, long timestamp, List<String> tags) throws IOException {
        final Snapshot snapshot = timer.getSnapshot();

        double[] values = { snapshot.getMax(), snapshot.getMean(), snapshot.getMin(), snapshot.getStdDev(),
                snapshot.getMedian(), snapshot.get75thPercentile(), snapshot.get95thPercentile(),
                snapshot.get98thPercentile(), snapshot.get99thPercentile(), snapshot.get999thPercentile() };

        for (int i = 0; i < STATS_EXPANSIONS.length; i++) {
            if (expansions.contains(STATS_EXPANSIONS[i])) {
                request.addGauge(new DatadogGauge(appendExpansionSuffix(name, STATS_EXPANSIONS[i]),
                        toNumber(convertDuration(values[i])), timestamp, host, tags));
            }
        }

        reportMetered(name, timer, timestamp, tags);
    }

    private void reportMetered(String name, Metered meter, long timestamp, List<String> tags) throws IOException {
        if (expansions.contains(Expansion.COUNT)) {
            request.addGauge(new DatadogGauge(appendExpansionSuffix(name, Expansion.COUNT), meter.getCount(), timestamp,
                    host, tags));
        }

        double[] values = { meter.getOneMinuteRate(), meter.getFiveMinuteRate(), meter.getFifteenMinuteRate(),
                meter.getMeanRate() };

        for (int i = 0; i < RATE_EXPANSIONS.length; i++) {
            if (expansions.contains(RATE_EXPANSIONS[i])) {
                request.addGauge(new DatadogGauge(appendExpansionSuffix(name, RATE_EXPANSIONS[i]),
                        toNumber(convertRate(values[i])), timestamp, host, tags));
            }
        }
    }

    private void reportHistogram(String name, Histogram histogram, long timestamp, List<String> tags)
            throws IOException {
        final Snapshot snapshot = histogram.getSnapshot();

        if (expansions.contains(Expansion.COUNT)) {
            request.addGauge(new DatadogGauge(appendExpansionSuffix(name, Expansion.COUNT), histogram.getCount(),
                    timestamp, host, tags));
        }

        Number[] values = { snapshot.getMax(), snapshot.getMean(), snapshot.getMin(), snapshot.getStdDev(),
                snapshot.getMedian(), snapshot.get75thPercentile(), snapshot.get95thPercentile(),
                snapshot.get98thPercentile(), snapshot.get99thPercentile(), snapshot.get999thPercentile() };

        for (int i = 0; i < STATS_EXPANSIONS.length; i++) {
            if (expansions.contains(STATS_EXPANSIONS[i])) {
                request.addGauge(new DatadogGauge(appendExpansionSuffix(name, STATS_EXPANSIONS[i]), toNumber(values[i]),
                        timestamp, host, tags));
            }
        }
    }

    private void reportCounter(String name, Counter counter, long timestamp, List<String> tags) throws IOException {
        // A Metrics counter is actually a Datadog Gauge. Datadog Counters are for rates which is
        // similar to the Metrics Meter type. Metrics counters have increment and decrement
        // functionality, which implies they are instantaneously measurable, which implies they are
        // actually a gauge. The Metrics documentation agrees, stating:
        // "A counter is just a gauge for an AtomicLong instance. You can increment or decrement its
        // value. For example, we may want a more efficient way of measuring the pending job in a queue"
        request.addGauge(new DatadogGauge(metricNameFormatter.format(name), counter.getCount(), timestamp, host, tags));
    }

    /**
     * Gauges are the only metrics which can throw exceptions. With a thrown exception all other metrics will not be
     * reported to Datadog.
     */
    private void reportGauge(String name, Gauge gauge, long timestamp, List<String> tags) {
        try {
            final Number value = toNumber(gauge.getValue());
            if (value != null) {
                request.addGauge(new DatadogGauge(metricNameFormatter.format(name), value, timestamp, host, tags));
            }
        } catch (Exception e) {
            String errorMessage = String.format("Error reporting gauge metric (name: %s, tags: %s) to Datadog, "
                    + "continuing reporting other metrics.", name, tags);
            log.error(errorMessage, e);
        }
    }

    private Number toNumber(Object o) {
        if (o instanceof Number) {
            return (Number) o;
        }
        return null;
    }

    private String appendExpansionSuffix(String name, Expansion expansion) {
        return metricNameFormatter.format(name, expansion.toString());
    }

    private String prefix(String name) {
        if (prefix == null) {
            return name;
        } else {
            return String.format("%s.%s", prefix, name);
        }
    }

    public static enum Expansion {
        COUNT("count"), RATE_MEAN("meanRate"), RATE_1_MINUTE("1MinuteRate"), RATE_5_MINUTE(
                "5MinuteRate"), RATE_15_MINUTE("15MinuteRate"), MIN("min"), MEAN("mean"), MAX("max"), STD_DEV(
                        "stddev"), MEDIAN("median"), P75("p75"), P95("p95"), P98("p98"), P99("p99"), P999("p999");

        public static EnumSet<Expansion> ALL = EnumSet.allOf(Expansion.class);

        private final String displayName;

        private Expansion(String displayName) {
            this.displayName = displayName;
        }

        @Override
        public String toString() {
            return displayName;
        }
    }

    public static Builder forRegistry(MetricRegistry registry) {
        return new Builder(registry);
    }

    public static class Builder {
        private final MetricRegistry registry;

        private String host;

        private EnumSet<Expansion> expansions;

        private Clock clock;

        private TimeUnit rateUnit;

        private TimeUnit durationUnit;

        private MetricFilter filter;

        private MetricNameFormatter metricNameFormatter;

        private List<String> tags;

        private Transport transport;

        private String prefix;

        public Builder(MetricRegistry registry) {
            this.registry = registry;
            this.expansions = Expansion.ALL;
            this.clock = Clock.defaultClock();
            this.rateUnit = TimeUnit.SECONDS;
            this.durationUnit = TimeUnit.MILLISECONDS;
            this.filter = MetricFilter.ALL;
            this.metricNameFormatter = new DefaultMetricNameFormatter();
            this.tags = new ArrayList<>();
        }

        public Builder withHost(String host) {
            this.host = host;
            return this;
        }

        public Builder withEC2Host() throws IOException {
            this.host = AwsHelper.getEc2InstanceId();
            return this;
        }

        public Builder withExpansions(EnumSet<Expansion> expansions) {
            this.expansions = expansions;
            return this;
        }

        public Builder convertRatesTo(TimeUnit rateUnit) {
            this.rateUnit = rateUnit;
            return this;
        }

        /**
         * Tags that would be sent to datadog with each and every metrics. This could be used to set global metrics like
         * version of the app, environment etc.
         *
         * @param tags List of tags eg: [env:prod, version:1.0.1, name:kafka_client] etc
         */
        public Builder withTags(List<String> tags) {
            this.tags = tags;
            return this;
        }

        /**
         * Prefix all metric names with the given string.
         *
         * @param prefix The prefix for all metric names.
         */
        public Builder withPrefix(String prefix) {
            this.prefix = prefix;
            return this;
        }

        public Builder withClock(Clock clock) {
            this.clock = clock;
            return this;
        }

        public Builder filter(MetricFilter filter) {
            this.filter = filter;
            return this;
        }

        public Builder withMetricNameFormatter(MetricNameFormatter formatter) {
            this.metricNameFormatter = formatter;
            return this;
        }

        public Builder convertDurationsTo(TimeUnit durationUnit) {
            this.durationUnit = durationUnit;
            return this;
        }

        /**
         * The transport mechanism to push metrics to datadog. Supports http webservice and UDP dogstatsd protocol as of
         * now.
         *
         * @see org.coursera.metrics.datadog.transport.HttpTransport
         * @see org.coursera.metrics.datadog.transport.UdpTransport
         */
        public Builder withTransport(Transport transport) {
            this.transport = transport;
            return this;
        }

        public NuxeoDatadogReporter build() {
            if (transport == null) {
                throw new IllegalArgumentException(
                        "Transport for datadog reporter is null. " + "Please set a valid transport");
            }
            return new NuxeoDatadogReporter(this.registry, this.transport, this.filter, this.clock, this.host,
                    this.expansions, this.rateUnit, this.durationUnit, this.metricNameFormatter, this.tags,
                    this.prefix);
        }
    }
}
