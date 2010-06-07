package org.nuxeo.runtime.management.metrics;

import java.lang.management.ManagementFactory;
import java.util.HashMap;
import java.util.Map;

import javax.management.MBeanServer;
import javax.management.ObjectName;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class MetricRegisterer {

    protected MBeanServer server = ManagementFactory.getPlatformMBeanServer();

    protected Log log = LogFactory.getLog(MetricRegisterer.class);

    protected Map<ObjectName, Object> registry = new HashMap<ObjectName, Object>();

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
        registerMXBean(mbean,name);
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
