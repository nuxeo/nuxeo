/*
 * (C) Copyright 2010 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     Nuxeo - initial API and implementation
 */

package org.nuxeo.runtime.management.metrics;

import java.lang.management.ManagementFactory;
import java.util.HashMap;
import java.util.HashSet;

import javax.management.MBeanServer;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.management.ObjectNameFactory;
import org.nuxeo.runtime.management.ResourcePublisher;

public class MetricRegister {

    protected static final Log log = LogFactory.getLog(MetricRegister.class);

    protected final MBeanServer server = ManagementFactory.getPlatformMBeanServer();

    protected final HashMap<String,String> cnames = new HashMap<String,String>();

    protected String canonicalName(String name, String type) {
        return ObjectNameFactory.formatMetricQualifiedName(name, type);
    }

    public void registerMXBean(Object mbean, String name, Class<?> itf, String type) {
        ResourcePublisher srv = Framework.getLocalService(ResourcePublisher.class);
        String cname = canonicalName(name, type);
        srv.registerResource(name, cname, itf, mbean);
        cnames.put(name, cname);
    }

    public void unregisterMXBean(Object mbean) {
        unregisterMXBean(mbean.getClass().getSimpleName());
    }

    public void unregisterMXBean(String name) {
        ResourcePublisher srv = Framework.getLocalService(ResourcePublisher.class);
        if (srv == null) {
            return;
        }
        String cname = cnames.remove(name);
        if (cname!=null) {
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
