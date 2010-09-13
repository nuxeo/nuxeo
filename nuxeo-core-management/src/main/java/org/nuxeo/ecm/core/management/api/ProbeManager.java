package org.nuxeo.ecm.core.management.api;

import java.util.Collection;

import org.nuxeo.ecm.core.management.probes.ProbeInfo;

public interface ProbeManager extends ProbeRunnerMBean {

    Collection<ProbeInfo> getProbeInfos();

    Collection<ProbeInfo> getProbesInfoInSuccess();

    Collection<ProbeInfo> getInSuccessProbeInfos();

    Collection<ProbeInfo> getInFailureProbeInfos();

    boolean runProbe(ProbeInfo probe);

    ProbeInfo getProbeInfo(String name);

}
