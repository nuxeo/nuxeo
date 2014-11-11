package org.nuxeo.runtime.management.metrics;

import org.javasimon.SimonManager;
import org.javasimon.jmx.JmxRegisterCallback;
import org.javasimon.jmx.SimonSuperMXBean;

public class MetricRegisteringCallback extends JmxRegisterCallback {

    @Override
    public void initialize() {
        for (String name:SimonManager.simonNames()) {
            this.simonCreated(SimonManager.getSimon(name));
        }
    }


    @Override
    protected String constructObjectName(SimonSuperMXBean simonMxBean) {
        return String.format("org.nuxeo:metric=%s,management=metric", simonMxBean.getName());
    }
}
