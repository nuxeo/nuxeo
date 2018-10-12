/*
 * (C) Copyright 2011 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     matic
 */
package org.nuxeo.ecm.core.management.jtajca.internal;

import org.nuxeo.ecm.core.management.jtajca.ConnectionPoolMonitor;
import org.nuxeo.ecm.core.management.jtajca.internal.DefaultMonitorComponent.ServerInstance;
import org.nuxeo.runtime.jtajca.NuxeoConnectionManager;
import org.nuxeo.runtime.jtajca.NuxeoContainer;
import org.nuxeo.runtime.metrics.MetricsService;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.SharedMetricRegistries;
import com.codahale.metrics.jvm.JmxAttributeGauge;

/**
 * @author matic
 */
public class DefaultConnectionPoolMonitor implements ConnectionPoolMonitor {

    // @since 5.7.2
    protected final MetricRegistry registry = SharedMetricRegistries.getOrCreate(MetricsService.class.getName());

    protected final String name;

    protected NuxeoConnectionManager cm;

    protected DefaultConnectionPoolMonitor(String mame, NuxeoConnectionManager cm) {
        name = mame;
        this.cm = cm;
    }

    protected ServerInstance self;

    public NuxeoConnectionManager getManager() {
        return cm;
    }

    @Override
    public void install() {
        self = DefaultMonitorComponent.bind(this, name);
        registry.register(MetricRegistry.name("nuxeo", "repositories", name, "connections", "count"),
                new JmxAttributeGauge(self.name, "ConnectionCount"));
        registry.register(MetricRegistry.name("nuxeo", "repositories", name, "connections", "idle"),
                new JmxAttributeGauge(self.name, "IdleConnectionCount"));
        registry.register(MetricRegistry.name("nuxeo", "repositories", name, "connections", "killed"),
                new JmxAttributeGauge(self.name, "KilledActiveConnectionCount"));
    }

    @Override
    public void uninstall() {
        DefaultMonitorComponent.unbind(self);
        registry.remove(MetricRegistry.name("nuxeo", "repositories", name, "connections", "count"));
        registry.remove(MetricRegistry.name("nuxeo", "repositories", name, "connections", "idle"));
        registry.remove(MetricRegistry.name("nuxeo", "repositories", name, "connections", "killed"));
        self = null;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public int getConnectionCount() {
        return cm.getConnectionCount();
    }

    @Override
    public int getIdleConnectionCount() {
        return cm.getIdleConnectionCount();
    }

    @Override
    public int getBlockingTimeoutMilliseconds() {
        return cm.getBlockingTimeoutMilliseconds();
    }

    @Override
    public int getIdleTimeoutMinutes() {
        return cm.getIdleTimeoutMinutes();
    }

    @Override
    public int getActiveTimeoutMinutes() {
        return cm.getActiveTimeoutMinutes();
    }

    @Override
    public int getPartitionCount() {
        return cm.getPartitionCount();
    }

    @Override
    public int getPartitionMaxSize() {
        return cm.getPartitionMaxSize();
    }

    @Override
    public void setPartitionMaxSize(int maxSize) throws InterruptedException {
        cm.setPartitionMaxSize(maxSize);
    }

    @Override
    public int getPartitionMinSize() {
        return cm.getPartitionMinSize();
    }

    @Override
    public void setPartitionMinSize(int minSize) {
        cm.setPartitionMinSize(minSize);
    }

    @Override
    public void setBlockingTimeoutMilliseconds(int timeoutMilliseconds) {
        cm.setBlockingTimeoutMilliseconds(timeoutMilliseconds);
    }

    @Override
    public void setIdleTimeoutMinutes(int idleTimeoutMinutes) {
        cm.setIdleTimeoutMinutes(idleTimeoutMinutes);
    }

    /**
     * @since 5.8
     */
    public void handleNewConnectionManager(NuxeoConnectionManager cm) {
        this.cm = cm;
    }

    @Override
    public void reset() {
        NuxeoContainer.resetConnectionManager(name);
    }

    @Override
    public long getKilledActiveConnectionCount() {
        return cm.getKilledConnectionCount();
    }

    @Override
    public int killActiveTimedoutConnections() {
        return cm.killActiveTimedoutConnections(System.currentTimeMillis()).size();
    }

}
