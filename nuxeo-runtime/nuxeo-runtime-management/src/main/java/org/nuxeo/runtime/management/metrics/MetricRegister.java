/*
 * (C) Copyright 2010 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Nuxeo - initial API and implementation
 */

package org.nuxeo.runtime.management.metrics;

import java.util.HashMap;
import java.util.HashSet;

import javax.management.MBeanServer;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.management.ObjectNameFactory;
import org.nuxeo.runtime.management.ResourcePublisher;
import org.nuxeo.runtime.management.ServerLocator;

public class MetricRegister {

    protected static final Log log = LogFactory.getLog(MetricRegister.class);

    protected final MBeanServer server = Framework.getService(ServerLocator.class).lookupServer();

    protected final HashMap<String, String> cnames = new HashMap<String, String>();

    protected String canonicalName(String name, String type) {
        return ObjectNameFactory.formatMetricQualifiedName(name, type);
    }

    public void registerMXBean(Object mbean, String name, Class<?> itf, String type) {
        ResourcePublisher srv = Framework.getService(ResourcePublisher.class);
        String cname = canonicalName(name, type);
        srv.registerResource(name, cname, itf, mbean);
        cnames.put(name, cname);
    }

    public void unregisterMXBean(Object mbean) {
        unregisterMXBean(mbean.getClass().getSimpleName());
    }

    public void unregisterMXBean(String name) {
        ResourcePublisher srv = Framework.getService(ResourcePublisher.class);
        if (srv == null) {
            return;
        }
        String cname = cnames.remove(name);
        if (cname != null) {
            srv.unregisterResource(name, cname);
        }
    }

    public void unregisterAll() {
        HashSet<String> names = new HashSet<String>();
        names.addAll(cnames.keySet());
        for (String name : names) {
            unregisterMXBean(name);
        }
    }
}
