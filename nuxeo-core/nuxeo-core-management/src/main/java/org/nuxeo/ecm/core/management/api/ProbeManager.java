package org.nuxeo.ecm.core.management.api;

import java.util.Collection;

public interface ProbeManager extends ProbeRunnerMBean {

    Collection<ProbeInfo> getAllProbeInfos();

    Collection<ProbeInfo> getInSuccessProbeInfos();

    Collection<ProbeInfo> getInFailureProbeInfos();

    ProbeInfo runProbe(ProbeInfo probe);

    ProbeInfo runProbe(String name);

    ProbeInfo getProbeInfo(String name);

    ProbeInfo getProbeInfo(Class<? extends Probe> probeClass);
}
