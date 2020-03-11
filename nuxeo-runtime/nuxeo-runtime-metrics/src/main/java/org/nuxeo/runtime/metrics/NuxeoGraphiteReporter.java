/*
 * (C) Copyright 2020 Nuxeo (http://nuxeo.com/) and others.
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
package org.nuxeo.runtime.metrics;

import java.util.Collections;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.concurrent.TimeUnit;

import io.dropwizard.metrics5.Counter;
import io.dropwizard.metrics5.Gauge;
import io.dropwizard.metrics5.Histogram;
import io.dropwizard.metrics5.Meter;
import io.dropwizard.metrics5.Metric;
import io.dropwizard.metrics5.MetricFilter;
import io.dropwizard.metrics5.MetricName;
import io.dropwizard.metrics5.MetricRegistry;
import io.dropwizard.metrics5.ScheduledReporter;
import io.dropwizard.metrics5.Timer;
import io.dropwizard.metrics5.graphite.GraphiteReporter;

/**
 * A Graphite Reporter that handles metric name with tagging. A metric "foo.bar.baz" with tag "bar=qux" is rewritten as
 * "foo.bar.qux.baz".
 *
 * @since 11.1
 */
public class NuxeoGraphiteReporter extends ScheduledReporter {

    protected final GraphiteReporter reporter;

    public NuxeoGraphiteReporter(MetricRegistry registry, MetricFilter filter, GraphiteReporter reporter) {
        super(registry, "graphite-reporter", filter, TimeUnit.SECONDS,
                TimeUnit.SECONDS);
        this.reporter = reporter;
    }

    @Override
    public void report(SortedMap<MetricName, Gauge> gauges, SortedMap<MetricName, Counter> counters,
            SortedMap<MetricName, Histogram> histograms, SortedMap<MetricName, Meter> meters,
            SortedMap<MetricName, Timer> timers) {
        reporter.report(graphiteMetrics(gauges), graphiteMetrics(counters), graphiteMetrics(histograms),
                graphiteMetrics(meters), graphiteMetrics(timers));
    }

    protected <T extends Metric> SortedMap<MetricName, T> graphiteMetrics(SortedMap<MetricName, T> metrics) {
        final SortedMap<MetricName, T> nuxeoMetrics = new TreeMap<>();
        for (Map.Entry<MetricName, T> entry : metrics.entrySet()) {
            MetricName name = entry.getKey();
            if (name.getTags().isEmpty()) {
                nuxeoMetrics.put(name, entry.getValue());
            } else {
                nuxeoMetrics.put(convertName(name), entry.getValue());
            }
        }
        return Collections.unmodifiableSortedMap(nuxeoMetrics);
    }

    protected MetricName convertName(MetricName name) {
        String graphiteName = MetricsDescriptor.GraphiteDescriptor.metricToName(name);
        return MetricName.build(graphiteName);
    }

}
