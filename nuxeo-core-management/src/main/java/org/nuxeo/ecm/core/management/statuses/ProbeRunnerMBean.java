package org.nuxeo.ecm.core.management.statuses;

import java.util.Collection;

public interface ProbeRunnerMBean {

    public abstract Collection<String> getProbeNames();

    public abstract int getProbesCount();

    public abstract Collection<String> getProbesInError();

    public abstract int getProbesInErrorCount();

    public abstract Collection<String> getProbesInSuccess();

    public abstract int getProbesInSuccessCount();

    public abstract boolean run();

}