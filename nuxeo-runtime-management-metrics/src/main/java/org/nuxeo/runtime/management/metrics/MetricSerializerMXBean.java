package org.nuxeo.runtime.management.metrics;

import java.io.IOException;

public interface MetricSerializerMXBean {

    int getCount();

    long getLastUsage();

    void closeOutput() throws IOException;

    void resetOutput() throws IOException;

    void resetOutput(String path) throws IOException;

    String getOutputLocation();

}
