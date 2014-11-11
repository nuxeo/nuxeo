/*
 * Copyright (c) 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     mcedica
 */
package org.nuxeo.ecm.core.management.probes;

import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.management.api.Probe;
import org.nuxeo.ecm.core.management.api.ProbeInfo;
import org.nuxeo.ecm.core.management.api.ProbeManager;
import org.nuxeo.ecm.core.management.api.ProbeStatus;
import org.nuxeo.runtime.management.ManagementRuntimeException;

public class ProbeManagerImpl implements ProbeManager {

    protected static final Log log = LogFactory.getLog(ProbeManagerImpl.class);

    protected final Map<Class<? extends Probe>, ProbeInfo> infosByTypes = new HashMap<Class<? extends Probe>, ProbeInfo>();

    protected final Map<String, ProbeInfo> infosByShortcuts = new HashMap<String, ProbeInfo>();

    protected final Map<String, Probe> probesByShortcuts = new HashMap<String, Probe>();

    protected final Set<ProbeInfo> failed = new HashSet<ProbeInfo>();

    protected final Set<ProbeInfo> succeed = new HashSet<ProbeInfo>();

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
            throw new IllegalArgumentException("no probe registered for "
                    + probeClass);
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
        } catch (Exception e) {
            throw new ManagementRuntimeException(
                    "Cannot create management probe for " + descriptor);
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
            } catch (Throwable e) {
                probeInfoImpl.failureCount += 1;
                probeInfoImpl.lastFailureDate = new Date();
                probeInfoImpl.lastFailureStatus = ProbeStatus.newError(e);
            } finally {
                probeInfoImpl.lastDuration = doGetDuration(
                        probeInfoImpl.lastRunnedDate, new Date());
                currentThread.setContextClassLoader(lastLoader);
            }

            if (probe.isInError()) {
                succeed.remove(probe);
                failed.add(probe);
            } else {
                failed.remove(probe);
                succeed.add(probe);
            }
        } catch (Throwable e) {
            succeed.remove(probe);
            failed.add(probe);
        }
    }

}
