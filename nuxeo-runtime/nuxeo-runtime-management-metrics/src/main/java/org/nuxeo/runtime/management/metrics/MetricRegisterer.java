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
import java.util.Map;

import javax.management.MBeanServer;
import javax.management.ObjectName;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class MetricRegisterer {

    protected static final Log log = LogFactory.getLog(MetricRegisterer.class);

    protected final MBeanServer server = ManagementFactory.getPlatformMBeanServer();

    protected final Map<ObjectName, Object> registry = new HashMap<ObjectName, Object>();

    protected ObjectName newObjectName(String name) {
        try {
            return new ObjectName("org.nuxeo", "name", name);
        } catch (Exception e) {
            throw new Error(String.format("Cannot build qualified name for %s",
                    name), e);
        }
    }

    public void registerMXBean(Object mbean) {
        String name = mbean.getClass().getSimpleName();
        registerMXBean(mbean, name);
    }

    public void registerMXBean(Object mbean, String name) {
        ObjectName oName = newObjectName(name);
        try {
            server.registerMBean(mbean, oName);
        } catch (Exception e) {
            throw new Error(String.format("Cannot register %s", name), e);
        }
        registry.put(oName, mbean);
    }

    public void unregisterMXBean(Object mbean) {
        unregisterBean(mbean.getClass().getSimpleName());
    }

    public void unregisterBean(String name) {
        ObjectName oName = newObjectName(name);
        try {
            server.unregisterMBean(oName);
        } catch (Exception e) {
            throw new Error(String.format("Cannot register %s", name), e);
        }
        registry.remove(oName);
    }

}
