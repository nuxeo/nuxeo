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
 *     Florent Guillaume
 */
package org.nuxeo.ecm.core.management.jtajca.internal;

import org.apache.commons.pool2.impl.GenericKeyedObjectPool;
import org.nuxeo.ecm.core.management.jtajca.ConnectionPoolMonitor;
import org.nuxeo.ecm.core.management.jtajca.internal.DefaultMonitorComponent.ServerInstance;
import org.nuxeo.runtime.metrics.MetricsService;

import io.dropwizard.metrics5.MetricName;
import io.dropwizard.metrics5.MetricRegistry;
import io.dropwizard.metrics5.SharedMetricRegistries;
import io.dropwizard.metrics5.jvm.JmxAttributeGauge;

/**
 * Connection pool monitor for an Apache Commons Pool.
 */
public class ObjectPoolMonitor implements ConnectionPoolMonitor {

    protected static final MetricRegistry METRICS = SharedMetricRegistries.getOrCreate(MetricsService.class.getName());

    protected final String name;

    protected final GenericKeyedObjectPool<String, ?> pool;

    protected final String key;

    protected MetricName countGauge;

    protected MetricName idleGauge;

    protected MetricName killedGauge;

    protected ObjectPoolMonitor(String name, GenericKeyedObjectPool<String, ?> pool, String key) {
        this.name = name;
        this.pool = pool;
        this.key = key;
    }

    protected ServerInstance self;

    @Override
    public void install() {
        self = DefaultMonitorComponent.bind(this, name);
        countGauge = MetricName.build("nuxeo", "repositories", "repository", "connection", "count")
                               .tagged("repository", name);
        idleGauge = MetricName.build("nuxeo", "repositories", "repository", "connection", "idle")
                              .tagged("repository", name);
        killedGauge = MetricName.build("nuxeo", "repositories", "repository", "connection", "killed")
                                .tagged("repository", name);
        METRICS.register(countGauge, new JmxAttributeGauge(self.name, "ConnectionCount"));
        METRICS.register(idleGauge, new JmxAttributeGauge(self.name, "IdleConnectionCount"));
        METRICS.register(killedGauge, new JmxAttributeGauge(self.name, "KilledActiveConnectionCount"));
    }

    @Override
    public void uninstall() {
        DefaultMonitorComponent.unbind(self);
        METRICS.remove(countGauge);
        METRICS.remove(idleGauge);
        METRICS.remove(killedGauge);
        self = null;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public int getConnectionCount() {
        return pool.getNumActive(key) + pool.getNumIdle(key);
    }

    @Override
    public int getIdleConnectionCount() {
        return pool.getNumIdle(key);
    }

    @Override
    public int getBlockingTimeoutMilliseconds() {
        return (int) pool.getMaxWaitMillis();
    }

    @Override
    public int getIdleTimeoutMinutes() {
        return -1;
    }

    @Override
    public int getActiveTimeoutMinutes() {
        return -1;
    }

    @Override
    public void reset() {
        pool.clear();
    }

    @Override
    public long getKilledActiveConnectionCount() {
        return 0;
    }

    @Override
    public int killActiveTimedoutConnections() {
        return 0;
    }

}
