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

import org.nuxeo.runtime.metrics.NuxeoMetricSet;

import com.google.common.cache.Cache;

import io.dropwizard.metrics5.MetricName;
import io.dropwizard.metrics5.MetricSet;

/**
 * Wrapper used to wrap the Guava cache's statistics into Gauges in order to report them via Codahale Metrics.
 *
 * @since 8.10
 */
public class GuavaCacheMetric extends NuxeoMetricSet {

    private GuavaCacheMetric(MetricName name) {
        super(name);
    }

    private GuavaCacheMetric(String name, String... names) {
        super(name, names);
    }

    public static MetricSet of(Cache<?, ?> cache, MetricName name) {
        GuavaCacheMetric metrics = new GuavaCacheMetric(name);
        addCacheMetrics(cache, metrics);
        return metrics;
    }

    public static MetricSet of(Cache<?, ?> cache, String name, String... names) {
        GuavaCacheMetric metrics = new GuavaCacheMetric(name, names);
        addCacheMetrics(cache, metrics);
        return metrics;
    }

    protected static void addCacheMetrics(Cache<?, ?> cache, GuavaCacheMetric metrics) {
        metrics.putGauge(() -> cache.size(), "size");
        metrics.putGauge(() -> cache.stats().evictionCount(), "eviction", "count");
        metrics.putGauge(() -> cache.stats().hitCount(), "hit", "count");
        metrics.putGauge(() -> cache.stats().hitRate(), "hit", "ratio");
        metrics.putGauge(() -> cache.stats().missCount(), "miss", "count");
        metrics.putGauge(() -> cache.stats().requestCount(), "read", "count");
        // metrics.putGauge(() -> cache.stats().averageLoadPenalty(), "average", "load", "penalty");
        // metrics.putGauge(() -> cache.stats().loadCount(), "load", "count");
        // metrics.putGauge(() -> cache.stats().loadExceptionCount(), "load", "exception", "count");
        // metrics.putGauge(() -> cache.stats().loadExceptionRate(), "load", "exception", "rate");
        // metrics.putGauge(() -> cache.stats().loadSuccessCount(), "load", "success", "count");
        // metrics.putGauge(() -> cache.stats().missRate(), "miss", "rate");
        // metrics.putGauge(() -> cache.stats().totalLoadTime(), "total", "load", "time");
    }

}
