package org.nuxeo.runtime.test.runner;

import java.util.Properties;

import org.nuxeo.runtime.jtajca.JtaActivator;

@Deploy({"org.nuxeo.runtime.jtajca", "org.nuxeo.runtime.datasource" })
public class ContainerFeature extends SimpleFeature {

    protected String autoactivationValue;

    @Override
    public void start(FeaturesRunner runner) {
        autoactivationValue = System.getProperty(JtaActivator.AUTO_ACTIVATION);
        System.setProperty(JtaActivator.AUTO_ACTIVATION, "true");
    }

    @Override
    public void stop(FeaturesRunner runner) {
        Properties props = System.getProperties();
        if (autoactivationValue != null) {
            props.put(JtaActivator.AUTO_ACTIVATION, autoactivationValue);
        } else {
            props.remove(JtaActivator.AUTO_ACTIVATION);
        }
    }
}