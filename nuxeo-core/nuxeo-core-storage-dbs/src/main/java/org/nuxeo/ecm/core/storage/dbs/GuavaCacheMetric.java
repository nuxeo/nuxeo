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
 * Contributors:
 *     Kevin Leturc
 */
package org.nuxeo.ecm.core.storage.dbs;

import java.util.HashMap;
import java.util.Map;

import com.codahale.metrics.Gauge;
import com.codahale.metrics.Metric;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.MetricSet;
import com.google.common.cache.Cache;

/**
 * Wrapper used to wrap the Guava cache's statistics into Gauges in order to report them via Codahale Metrics.
 *
 * @since 8.10
 */
public class GuavaCacheMetric implements MetricSet {

    private Map<String, Metric> metrics = new HashMap<>();

    private GuavaCacheMetric() {
    }

    @Override
    public Map<String, Metric> getMetrics() {
        return metrics;
    }

    private <T> void putMetrics(Gauge<T> gauge, String name, String... names) {
        metrics.put(MetricRegistry.name(name, names), gauge);
    }

    public static MetricSet of(Cache cache, String name, String... names) {
        String basicName = MetricRegistry.name(name, names);

        GuavaCacheMetric metrics = new GuavaCacheMetric();
        metrics.putMetrics(() -> cache.size(), basicName, "size");
        metrics.putMetrics(() -> cache.stats().averageLoadPenalty(), basicName, "average", "load", "penalty");
        metrics.putMetrics(() -> cache.stats().evictionCount(), basicName, "eviction", "count");
        metrics.putMetrics(() -> cache.stats().hitCount(), basicName, "hit", "count");
        metrics.putMetrics(() -> cache.stats().hitRate(), basicName, "hit", "rate");
        metrics.putMetrics(() -> cache.stats().loadCount(), basicName, "load", "count");
        metrics.putMetrics(() -> cache.stats().loadExceptionCount(), basicName, "load", "exception", "count");
        metrics.putMetrics(() -> cache.stats().loadExceptionRate(), basicName, "load", "exception", "rate");
        metrics.putMetrics(() -> cache.stats().loadSuccessCount(), basicName, "load", "success", "count");
        metrics.putMetrics(() -> cache.stats().missCount(), basicName, "miss", "count");
        metrics.putMetrics(() -> cache.stats().missRate(), basicName, "miss", "rate");
        metrics.putMetrics(() -> cache.stats().requestCount(), basicName, "request", "count");
        metrics.putMetrics(() -> cache.stats().totalLoadTime(), basicName, "total", "load", "time");
        return metrics;
    }

}
