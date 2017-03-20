/*
 * (C) Copyright 2017 Nuxeo SA (http://nuxeo.com/) and others.
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

import com.codahale.metrics.Counter;
import com.codahale.metrics.Gauge;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.RatioGauge;
import com.codahale.metrics.SharedMetricRegistries;

public class CacheMetrics extends CacheWrapper {

    protected final MetricRegistry registry;

    protected final Counter read;

    protected final Counter read_hit;

    protected final Counter read_miss;

    protected final RatioGauge read_hit_ratio;

    protected final Counter write;

    protected final Counter invalidation;

    protected final Gauge<Long> size;

    protected final String READ_HIT_NAME = nameOf("read-hit-counter");
    protected final String READ_HITRATIO_NAME = nameOf("read-hit-ratio");
    protected final String READ_MISS_NAME = nameOf("read-miss-counter");
    protected final String READ_NAME = nameOf("read-counter");
    protected final String WRITE_NAME = nameOf("write-counter");
    protected final String INVALIDATE_ALL_NAME = nameOf("invalidate-all-counter");
    protected final String SIZE_NAME = nameOf("size");

    CacheMetrics(Cache cache) {
        super(cache);
        registry = SharedMetricRegistries.getOrCreate(MetricsService.class.getName());
        read = registry.counter(READ_NAME);
        read_hit = registry.counter(READ_HIT_NAME);
        read_miss = registry.counter(READ_MISS_NAME);
        registry.register(READ_HITRATIO_NAME, read_hit_ratio = new RatioGauge() {

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
                return Long.valueOf(cache.getSize());
            }

        });
    }

    protected String nameOf(String name) {
        return MetricRegistry.name("nuxeo", "cache", cache.getName(), name);
    }

    @Override
    protected void onStop() {
        registry.remove(READ_NAME);
        registry.remove(READ_HIT_NAME);
        registry.remove(READ_HITRATIO_NAME);
        registry.remove(READ_MISS_NAME);
        registry.remove(WRITE_NAME);
        registry.remove(INVALIDATE_ALL_NAME);
        registry.remove(SIZE_NAME);
    }

    @Override
    public Serializable get(String key) {
        Serializable value = cache.get(key);
        read.inc();
        if (value != null || cache.hasEntry(key)) {
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
    public void invalidate(String key) {
        cache.invalidate(key);
    }

    @Override
    public void invalidateAll() {
        try {
            cache.invalidateAll();
        } finally {
            invalidation.inc();
        }
    }

    @Override
    public long getSize() {
        return cache.getSize();
    }


}
