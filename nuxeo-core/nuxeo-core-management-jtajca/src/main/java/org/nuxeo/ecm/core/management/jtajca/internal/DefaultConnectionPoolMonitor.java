/*
 * (C) Copyright 2011 Nuxeo SA (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     matic
 */
package org.nuxeo.ecm.core.management.jtajca.internal;

import javax.management.ObjectInstance;
import org.nuxeo.ecm.core.management.jtajca.ConnectionPoolMonitor;
import org.nuxeo.runtime.jtajca.NuxeoConnectionManager;
import org.nuxeo.runtime.jtajca.NuxeoContainer;
import org.nuxeo.runtime.metrics.MetricsService;
import com.codahale.metrics.JmxAttributeGauge;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.SharedMetricRegistries;

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

    protected ObjectInstance self;

    public NuxeoConnectionManager getManager() {
        return cm;
    }

    @Override
    public void install() {
        self = DefaultMonitorComponent.bind(this, name);
        registry.register(MetricRegistry.name("nuxeo", "repositories", name, "connections", "count"),
                new JmxAttributeGauge(self.getObjectName(), "ConnectionCount"));
        registry.register(MetricRegistry.name("nuxeo", "repositories", name, "connections", "idle"),
                new JmxAttributeGauge(self.getObjectName(), "IdleConnectionCount"));
        registry.register(MetricRegistry.name("nuxeo", "repositories", name, "connections", "killed"),
                new JmxAttributeGauge(self.getObjectName(), "KilledActiveConnectionCount"));
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
