/*
 * (C) Copyright 2017 Nuxeo (http://nuxeo.com/) and others.
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
 *     Kevin Leturc <kleturc@nuxeo.com>
 */
package org.nuxeo.runtime.metrics;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

import io.dropwizard.metrics5.Gauge;
import io.dropwizard.metrics5.Metric;
import io.dropwizard.metrics5.MetricName;
import io.dropwizard.metrics5.MetricSet;

/**
 * Just a helper to easily declare metrics inside a {@link MetricSet} with th ease of Java 8 Lambda expression.
 *
 * @since 8.10-HF08, 9.2
 */
public class NuxeoMetricSet implements MetricSet {

    protected final Map<MetricName, Metric> metrics;

    protected final MetricName prefixName;

    public NuxeoMetricSet() {
        // we can inject null as prefix because MetricRegistry#name(String, String...) doesn't print null value
        this(null);
    }

    public NuxeoMetricSet(String name, String... names) {
        this(MetricName.build(name).append(MetricName.build(names)));
    }

    public NuxeoMetricSet(MetricName name) {
        this(HashMap::new, name);
    }

    public NuxeoMetricSet(Supplier<Map<MetricName, Metric>> metricsSupplier, MetricName name) {
        this.metrics = metricsSupplier.get();
        this.prefixName = name;
    }

    /**
     * Put a gauge inside this {@link MetricSet} as name {@code prefixName.name.names[0].names[1]...};
     */
    public <T> void putGauge(Gauge<T> gauge, MetricName name) {
        metrics.put(prefixName.append(name), gauge);
    }

    public <T> void putGauge(Gauge<T> gauge, String name, String... names) {
        metrics.put(prefixName.append(MetricName.build(name).append(MetricName.build(names))), gauge);
    }

    @Override
    public Map<MetricName, Metric> getMetrics() {
        return metrics;
    }

    /**
     * @return the prefix name used by this {@link MetricSet} to prefix all added metrics, the value could be empty
     */
    public MetricName getPrefixName() {
        return prefixName;
    }

    /**
     * @return all metric names registered into this {@link MetricSet}
     */
    public Set<MetricName> getMetricNames() {
        return metrics.keySet();
    }

}
