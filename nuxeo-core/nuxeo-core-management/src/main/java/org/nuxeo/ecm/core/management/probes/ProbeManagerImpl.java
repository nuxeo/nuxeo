/*
 * (C) Copyright 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Contributors:
 *     mcedica
 */
package org.nuxeo.ecm.core.management.probes;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.management.api.Probe;
import org.nuxeo.ecm.core.management.api.ProbeInfo;
import org.nuxeo.ecm.core.management.api.ProbeManager;
import org.nuxeo.ecm.core.management.api.ProbeStatus;
import org.nuxeo.ecm.core.management.statuses.HealthCheckResult;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.management.ManagementRuntimeException;

public class ProbeManagerImpl implements ProbeManager {

    protected static final Log log = LogFactory.getLog(ProbeManagerImpl.class);

    protected final Map<Class<? extends Probe>, ProbeInfo> infosByTypes = new HashMap<Class<? extends Probe>, ProbeInfo>();

    protected final Map<String, ProbeInfo> infosByShortcuts = new HashMap<String, ProbeInfo>();

    protected final Map<String, Probe> probesByShortcuts = new HashMap<String, Probe>();

    protected final Map<String, ProbeInfo> probesForHealthCheck = new HashMap<String, ProbeInfo>();

    protected final Set<ProbeInfo> failed = new HashSet<ProbeInfo>();

    protected final Set<ProbeInfo> succeed = new HashSet<ProbeInfo>();

    public static final String DEFAULT_HEALTH_CHECK_INTERVAL_SECONDS_PROPERTY = "nuxeo.healthcheck.refresh.interval.seconds";

    public static final String DEFAULT_HEALTH_CHECK_INTERVAL_SECONDS = "20";

    protected Set<String> doExtractProbesName(Collection<ProbeInfo> runners) {
        Set<String> names = new HashSet<String>();
        for (ProbeInfo runner : runners) {
            names.add(runner.getShortcutName());
        }
        return names;
    }

    @Override
    public Collection<ProbeInfo> getAllProbeInfos() {
        return Collections.unmodifiableCollection(infosByTypes.values());
    }

    @Override
    public Collection<ProbeInfo> getInSuccessProbeInfos() {
        return Collections.unmodifiableCollection(succeed);
    }

    @Override
    public Collection<ProbeInfo> getInFailureProbeInfos() {
        return Collections.unmodifiableCollection(failed);
    }

    @Override
    public Collection<String> getProbeNames() {
        return infosByShortcuts.keySet();
    }

    @Override
    public int getProbesCount() {
        return infosByTypes.size();
    }

    @Override
    public Collection<String> getProbesInError() {
        return doExtractProbesName(failed);
    }

    @Override
    public int getProbesInErrorCount() {
        return failed.size();
    }

    @Override
    public Collection<String> getProbesInSuccess() {
        return doExtractProbesName(succeed);
    }

    @Override
    public int getProbesInSuccessCount() {
        return succeed.size();
    }

    @Override
    public ProbeInfo getProbeInfo(Class<? extends Probe> probeClass) {
        ProbeInfo info = infosByTypes.get(probeClass);
        if (info == null) {
            throw new IllegalArgumentException("no probe registered for " + probeClass);
        }
        return info;
    }

    @Override
    public boolean runAllProbes() {
        doRun();
        return getProbesInErrorCount() <= 0;
    }

    @Override
    public ProbeInfo runProbe(ProbeInfo probe) {
        doRunProbe(probe);
        return probe;
    }

    @Override
    public ProbeInfo runProbe(String name) {
        ProbeInfo probeInfo = getProbeInfo(name);
        if (probeInfo == null) {
            log.warn("Probe " + name + " can not be found");
            return null;
        }
        return runProbe(probeInfo);
    }

    @Override
    public ProbeInfo getProbeInfo(String name) {
        return infosByShortcuts.get(name);
    }

    public void registerProbe(ProbeDescriptor descriptor) {
        Class<? extends Probe> probeClass = descriptor.getProbeClass();
        Probe probe;
        try {
            probe = probeClass.newInstance();
        } catch (ReflectiveOperationException e) {
            throw new ManagementRuntimeException("Cannot create management probe for " + descriptor);
        }

        ProbeInfoImpl info = new ProbeInfoImpl(descriptor);
        infosByTypes.put(probeClass, info);
        infosByShortcuts.put(descriptor.getShortcut(), info);
        probesByShortcuts.put(descriptor.getShortcut(), probe);
    }

    public void unregisterProbe(ProbeDescriptor descriptor) {
        Class<? extends Probe> probeClass = descriptor.getProbeClass();
        infosByTypes.remove(probeClass);
        infosByShortcuts.remove(descriptor.getShortcut());
        probesForHealthCheck.remove(descriptor.getShortcut());
    }

    protected void doRun() {
        for (ProbeInfo probe : infosByTypes.values()) {
            doRunProbe(probe);
        }
    }

    protected static Long doGetDuration(Date fromDate, Date toDate) {
        return toDate.getTime() - fromDate.getTime();
    }

    protected void doRunProbe(ProbeInfo probe) {
        if (!probe.isEnabled()) {
            return;
        }
        boolean ok = false;
        try {
            ProbeInfoImpl probeInfoImpl = (ProbeInfoImpl) probe;
            Thread currentThread = Thread.currentThread();
            ClassLoader lastLoader = currentThread.getContextClassLoader();
            currentThread.setContextClassLoader(ProbeInfoImpl.class.getClassLoader());
            probeInfoImpl.lastRunnedDate = new Date();
            probeInfoImpl.runnedCount += 1;
            try {
                Probe runnableProbe = probesByShortcuts.get(probe.getShortcutName());
                probeInfoImpl.lastStatus = runnableProbe.run();
                if (probeInfoImpl.lastStatus.isSuccess()) {
                    probeInfoImpl.lastSucceedDate = probeInfoImpl.lastRunnedDate;
                    probeInfoImpl.lastSuccessStatus = probeInfoImpl.lastStatus;
                    probeInfoImpl.successCount += 1;
                } else {
                    probeInfoImpl.lastFailureStatus = probeInfoImpl.lastStatus;
                    probeInfoImpl.failureCount += 1;
                    probeInfoImpl.lastFailureDate = probeInfoImpl.lastRunnedDate;
                }
            } catch (RuntimeException e) {
                probeInfoImpl.failureCount += 1;
                probeInfoImpl.lastFailureDate = new Date();
                probeInfoImpl.lastFailureStatus = ProbeStatus.newError(e);
                // then swallow exception
            } finally {
                probeInfoImpl.lastDuration = doGetDuration(probeInfoImpl.lastRunnedDate, new Date());
                currentThread.setContextClassLoader(lastLoader);
            }

            if (probe.isInError()) {
                succeed.remove(probe);
                failed.add(probe);
            } else {
                failed.remove(probe);
                succeed.add(probe);
            }
            ok = true;
        } finally {
            if (!ok) {
                succeed.remove(probe);
                failed.add(probe);
            }
        }
    }

    @Override
    public void registerProbeForHealthCheck(HealthCheckProbesDescriptor descriptor) {
        String name = descriptor.getName();
        if (!descriptor.isEnabled()) {
            if (probesForHealthCheck.containsKey(name)) {
                probesForHealthCheck.remove(name);
                return;
            }
        }
        if (infosByShortcuts.containsKey(name)) {
            probesForHealthCheck.put(name, getProbeInfo(name));
        }
    }

    @Override
    public Collection<ProbeInfo> getHealthCheckProbes() {
        return Collections.unmodifiableCollection(probesForHealthCheck.values());
    }

    @Override
    public HealthCheckResult getOrRunHealthChecks() {
        for (Entry<String, ProbeInfo> es : probesForHealthCheck.entrySet()) {
            String probeName = es.getKey();
            ProbeInfo probe = es.getValue();
            if (probe == null) {
                log.warn("Probe:" + probeName + " does not exist, skipping it for the health check");
                continue;
            }
            getStatusOrRunProbe(probe, getDefaultCheckInterval());
        }
        return new HealthCheckResult(probesForHealthCheck.values());
    }

    @Override
    public HealthCheckResult getOrRunHealthCheck(String name) throws IllegalArgumentException {

        if (!probesForHealthCheck.containsKey(name)) {
            throw new IllegalArgumentException("Probe:" + name + " does not exist, or not registed for the healthCheck");
        }
        ProbeInfo probe = probesForHealthCheck.get(name);
        getStatusOrRunProbe(probe, getDefaultCheckInterval());
        return new HealthCheckResult(Collections.singletonList(probe));
    }

    protected void getStatusOrRunProbe(ProbeInfo probe, int refreshSeconds) {
        LocalDateTime now = LocalDateTime.now();
        Date lastRunDate = probe.getLastRunnedDate();
        LocalDateTime lastRunDateTime = lastRunDate != null ? LocalDateTime.ofInstant(lastRunDate.toInstant(),
                ZoneId.systemDefault()) : LocalDateTime.MIN;
        if (ChronoUnit.SECONDS.between(lastRunDateTime, now) > refreshSeconds) {
            doRunProbe(probe);
        }
    }

    private int getDefaultCheckInterval() {
        return Integer.parseInt(Framework.getProperty(DEFAULT_HEALTH_CHECK_INTERVAL_SECONDS_PROPERTY,
                DEFAULT_HEALTH_CHECK_INTERVAL_SECONDS));

    }
}
