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
 */
package org.nuxeo.ecm.core.cache;

import java.io.Serializable;

import org.nuxeo.runtime.metrics.MetricsService;

import io.dropwizard.metrics5.Counter;
import io.dropwizard.metrics5.Gauge;
import io.dropwizard.metrics5.MetricName;
import io.dropwizard.metrics5.MetricRegistry;
import io.dropwizard.metrics5.RatioGauge;
import io.dropwizard.metrics5.SharedMetricRegistries;

public class CacheMetrics extends CacheWrapper {

    protected MetricRegistry registry;

    protected Counter read;

    protected Counter read_hit;

    protected Counter read_miss;

    protected RatioGauge read_hit_ratio;

    protected Counter write;

    protected Counter invalidation;

    protected Gauge<Long> size;

    protected final MetricName READ_HIT_NAME = nameOf("hit");

    protected final MetricName READ_HIT_RATIO_NAME = nameOf("hit.ratio");

    protected final MetricName READ_MISS_NAME = nameOf("miss");

    protected final MetricName READ_NAME = nameOf("read");

    protected final MetricName WRITE_NAME = nameOf("write");

    protected final MetricName INVALIDATE_ALL_NAME = nameOf("invalidation");

    protected final MetricName SIZE_NAME = nameOf("size");

    protected MetricName nameOf(String name) {
        return MetricName.build("nuxeo", "cache", name).tagged("cache", getName());
    }

    public CacheMetrics(CacheManagement cache) {
        super(cache);
    }

    @Override
    public void start() {
        registry = SharedMetricRegistries.getOrCreate(MetricsService.class.getName());
        read = registry.counter(READ_NAME);
        read_hit = registry.counter(READ_HIT_NAME);
        read_miss = registry.counter(READ_MISS_NAME);
        registry.register(READ_HIT_RATIO_NAME, read_hit_ratio = new RatioGauge() {

            @Override
            protected Ratio getRatio() {
                Ratio ratio = Ratio.of(read_hit.getCount(), read.getCount());
                return ratio;
            }
        });
        write = registry.counter(WRITE_NAME);
        invalidation = registry.counter(INVALIDATE_ALL_NAME);
        registry.register(SIZE_NAME, size = new Gauge<Long>() {

            @Override
            public Long getValue() {
                return Long.valueOf(getSize());
            }
        });
    }

    @Override
    public void stop() {
        registry.remove(READ_NAME);
        registry.remove(READ_HIT_NAME);
        registry.remove(READ_MISS_NAME);
        registry.remove(READ_HIT_RATIO_NAME);
        registry.remove(WRITE_NAME);
        registry.remove(INVALIDATE_ALL_NAME);
        registry.remove(SIZE_NAME);
    }

    @Override
    public Serializable get(String key) {
        Serializable value = super.get(key);
        read.inc();
        if (value != null || super.hasEntry(key)) {
            read_hit.inc();
        } else {
            read_miss.inc();
        }
        return value;
    }

    @Override
    public void put(String key, Serializable value) {
        try {
            super.put(key, value);
        } finally {
            write.inc();
        }
    }

    @Override
    public void invalidateAll() {
        try {
            super.invalidateAll();
        } finally {
            invalidation.inc();
        }
    }

}
