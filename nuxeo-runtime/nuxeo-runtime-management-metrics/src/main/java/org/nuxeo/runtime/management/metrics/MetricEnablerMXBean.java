package org.nuxeo.runtime.management.metrics;

import java.io.IOException;

public interface MetricEnablerMXBean {

    void enable();

    void disable();

    boolean isEnabled();

    void enableLogging();

    void disableLogging();

    boolean isLogging();

    void enableSerializing() throws IOException;

    void disableSerializing() throws IOException;

    boolean isSerializing();

}
