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

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

import org.nuxeo.ecm.core.api.CoreSessionService;
import org.nuxeo.ecm.core.api.CoreSessionService.CoreSessionRegistrationInfo;
import org.nuxeo.ecm.core.management.jtajca.CoreSessionMonitor;
import org.nuxeo.ecm.core.management.jtajca.Defaults;
import org.nuxeo.ecm.core.management.jtajca.internal.DefaultMonitorComponent.ServerInstance;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.metrics.MetricsService;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.SharedMetricRegistries;
import com.codahale.metrics.jvm.JmxAttributeGauge;

public class DefaultCoreSessionMonitor implements CoreSessionMonitor {

    // @since 5.7.2
    protected final MetricRegistry registry = SharedMetricRegistries.getOrCreate(MetricsService.class.getName());

    @Override
    public int getCount() {
        return Framework.getService(CoreSessionService.class).getNumberOfOpenCoreSessions();
    }

    @Override
    public String[] getInfos() {
        List<CoreSessionRegistrationInfo> infos = Framework.getService(CoreSessionService.class)
                                                           .getCoreSessionRegistrationInfos();
        return toInfos(toSortedRegistration(infos));
    }

    public CoreSessionRegistrationInfo[] toSortedRegistration(List<CoreSessionRegistrationInfo> infos) {
        CoreSessionRegistrationInfo[] sortedInfos = infos.toArray(new CoreSessionRegistrationInfo[infos.size()]);
        Arrays.sort(sortedInfos, new Comparator<CoreSessionRegistrationInfo>() {

            @Override
            public int compare(CoreSessionRegistrationInfo o1, CoreSessionRegistrationInfo o2) {
                return o2.getCoreSession().getSessionId().compareTo(o1.getCoreSession().getSessionId());
            }

        });
        return sortedInfos;
    }

    public String[] toInfos(CoreSessionRegistrationInfo[] infos) {
        String[] values = new String[infos.length];
        for (int i = 0; i < infos.length; ++i) {
            values[i] = Defaults.instance.printStackTrace(infos[i]);
        }
        return values;
    }

    protected ServerInstance self;

    @Override
    public void install() {
        self = DefaultMonitorComponent.bind(CoreSessionMonitor.class, this);
        registry.register(MetricRegistry.name("nuxeo.repositories", "sessions"),
                new JmxAttributeGauge(self.name, "Count"));
    }

    @Override
    public void uninstall() {
        DefaultMonitorComponent.unbind(self);
        registry.remove(MetricRegistry.name("nuxeo.repositories", "sessions"));
        self = null;
    }

}
