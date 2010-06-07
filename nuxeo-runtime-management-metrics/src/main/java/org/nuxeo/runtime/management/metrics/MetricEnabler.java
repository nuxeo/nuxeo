package org.nuxeo.runtime.management.metrics;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.javasimon.Simon;
import org.javasimon.SimonManager;
import org.javasimon.jmx.JmxRegisterCallback;
import org.javasimon.utils.LoggingCallback;

public class MetricEnabler implements MetricEnablerMXBean {;

    protected MetricSerializer serializer;

    protected void setSerializer(MetricSerializer serializer) {
        this.serializer = serializer;
    }

    protected LoggingCallback lgCB;

    protected JmxRegisterCallback jmxCB = new JmxRegisterCallback();

    public void enable() {
        SimonManager.enable();
        SimonManager.callback().addCallback(jmxCB);
        for (String name : SimonManager.simonNames()) {
            Simon simon = SimonManager.getSimon(name);
            jmxCB.simonCreated(simon);
        }
    }

    public void disable() {
        SimonManager.callback().removeCallback(jmxCB);
        for (String name : SimonManager.simonNames()) {
            Simon simon = SimonManager.getSimon(name);
            jmxCB.simonDestroyed(simon);
        }
        SimonManager.disable();
    }

    public boolean isEnabled() {
        return SimonManager.isEnabled();
    }

    public void enableLogging() {
        lgCB = new LoggingCallback();
        lgCB.setLogger(Logger.getLogger("org.javasimon"));
        lgCB.setLevel(Level.FINEST);
        SimonManager.callback().addCallback(lgCB);
    }

    public void disableLogging() {
        SimonManager.callback().removeCallback(lgCB);
        lgCB = null;
    }

    public boolean isLogging() {
        return lgCB != null;
    }

    protected MetricSerializingCallback srzCB;

    public void enableSerializing() throws IOException {
            serializer.resetOutput();
            srzCB = new MetricSerializingCallback(serializer);
            SimonManager.callback().addCallback(srzCB);
    }

    public void disableSerializing() throws IOException {
            serializer.closeOutput();
            SimonManager.callback().removeCallback(srzCB);
            srzCB = null;
    }

    public boolean isSerializing() {
        return srzCB != null;
    }

}
