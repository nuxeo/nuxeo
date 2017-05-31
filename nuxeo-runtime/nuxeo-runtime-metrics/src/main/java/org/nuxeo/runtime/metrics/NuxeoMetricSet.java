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

import com.codahale.metrics.Gauge;
import com.codahale.metrics.Metric;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.MetricSet;

/**
 * Just a helper to easily declare metrics inside a {@link MetricSet} with th ease of Java 8 Lambda expression.
 * 
 * @since 8.10-HF08, 9.2
 */
public class NuxeoMetricSet implements MetricSet {

    protected final Map<String, Metric> metrics;

    protected final String prefixName;

    public NuxeoMetricSet() {
        // we can inject null as prefix because MetricRegistry#name(String, String...) doesn't print null value
        this(null);
    }

    public NuxeoMetricSet(String name, String... names) {
        this(HashMap::new, name, names);
    }

    public NuxeoMetricSet(Supplier<Map<String, Metric>> metricsSupplier, String name, String... names) {
        this.metrics = metricsSupplier.get();
        this.prefixName = MetricRegistry.name(name, names);
    }

    /**
     * Put a gauge inside this {@link MetricSet} as name {@code prefixName.name.names[0].names[1]...};
     */
    public <T> void putGauge(Gauge<T> gauge, String name, String... names) {
        metrics.put(buildNameWithPrefix(name, names), gauge);
    }

    /**
     * @return the name built from {@link MetricRegistry#name(String, String...)} prefixed with this
     *         {@link NuxeoMetricSet}'s prefix
     */
    protected String buildNameWithPrefix(String name, String[] names) {
        return MetricRegistry.name(MetricRegistry.name(prefixName, name), names);
    }

    @Override
    public Map<String, Metric> getMetrics() {
        return metrics;
    }

    /**
     * @return the prefix name used by this {@link MetricSet} to prefix all added metrics, the value could be empty
     */
    public String getPrefixName() {
        return prefixName;
    }

    /**
     * @return all metric names registered into this {@link MetricSet}
     */
    public Set<String> getMetricNames() {
        return metrics.keySet();
    }

}
