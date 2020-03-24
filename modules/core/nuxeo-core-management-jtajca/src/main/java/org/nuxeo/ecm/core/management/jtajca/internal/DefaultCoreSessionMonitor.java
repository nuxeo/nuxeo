/*
 * (C) Copyright 2013 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Stephane Lacoin
 */
package org.nuxeo.ecm.core.management.jtajca.internal;

import org.nuxeo.ecm.core.api.CoreSessionService;
import org.nuxeo.ecm.core.management.jtajca.CoreSessionMonitor;
import org.nuxeo.ecm.core.management.jtajca.internal.DefaultMonitorComponent.ServerInstance;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.metrics.MetricsService;

import io.dropwizard.metrics5.MetricName;
import io.dropwizard.metrics5.MetricRegistry;
import io.dropwizard.metrics5.SharedMetricRegistries;
import io.dropwizard.metrics5.jvm.JmxAttributeGauge;

public class DefaultCoreSessionMonitor implements CoreSessionMonitor {

    // @since 5.7.2
    protected final MetricRegistry registry = SharedMetricRegistries.getOrCreate(MetricsService.class.getName());

    protected MetricName sessionGauge;

    @Override
    public int getCount() {
        return Framework.getService(CoreSessionService.class).getNumberOfOpenCoreSessions();
    }

    @Override
    public String[] getInfos() {
        return new String[0];
    }

    protected ServerInstance self;

    @Override
    public void install() {
        self = DefaultMonitorComponent.bind(CoreSessionMonitor.class, this);
        sessionGauge = MetricRegistry.name("nuxeo.repositories.sessions");
        registry.register(sessionGauge, new JmxAttributeGauge(self.name, "Count"));
    }

    @Override
    public void uninstall() {
        DefaultMonitorComponent.unbind(self);
        registry.remove(sessionGauge);
        self = null;
    }

}
