/*
 * (C) Copyright 2006-2010 Nuxeo SAS (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     mcedica
 */
package org.nuxeo.ecm.core.management.statuses;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.management.ManagementRuntimeException;

public class ProbeRunner implements ProbeRunnerMBean {

    protected Set<String> doExtractProbesName(Collection<ProbeInfo> runners) {
        Set<String> names = new HashSet<String>();
        for (ProbeInfo runner : runners) {
            names.add(runner.shortcutName);
        }
        return names;
    }

    public Collection<ProbeInfo> getProbeInfos() {
        return Collections.unmodifiableCollection(infosByTypes.values());
    }

    public Collection<String> getProbeNames() {
        return infosByShortcuts.keySet();
    }

    public int getProbesCount() {
        return infosByTypes.size();
    }

    public Collection<String> getProbesInError() {
        return doExtractProbesName(failed);
    }

    public int getProbesInErrorCount() {
        return failed.size();
    }

    public Collection<String> getProbesInSuccess() {
        return doExtractProbesName(succeed);
    }

    public Collection<ProbeInfo> getProbesInfoInSuccess() {
        return Collections.unmodifiableSet(succeed);
    }

    public int getProbesInSuccessCount() {
        return succeed.size();
    }

    public ProbeInfo getProbeInfo(Class<? extends Probe> probeClass) {
        ProbeInfo info = infosByTypes.get(probeClass);
        if (info == null) {
            throw new IllegalArgumentException("no probe registered for "
                    + probeClass);
        }
        return info;
    }

    public Collection<ProbeInfo> getInSuccessProbeInfos() {
        return Collections.unmodifiableCollection(succeed);
    }

    public Collection<ProbeInfo> getInFailureProbeInfos() {
            return Collections.unmodifiableCollection(failed);
    }

    public boolean run() {
        doRun();
        return getProbesInErrorCount() <= 0;
    }

    public boolean runProbe(ProbeInfo probe) {
        doRunProbe(probe);
        return getProbesInSuccess().contains(probe.shortcutName);
    }

    public ProbeInfo getProbeInfo(String name) {
        for (ProbeInfo info : infosByTypes.values()) {
            if (name.equals(info.shortcutName)) {
                return info;
            }
        }
        return null;
    }

    protected static final Log log = LogFactory.getLog(ProbeRunner.class);

    protected final Map<Class<? extends Probe>, ProbeInfo> infosByTypes = new HashMap<Class<? extends Probe>, ProbeInfo>();

    protected final Map<String, ProbeInfo> infosByShortcuts =  new HashMap<String, ProbeInfo>();

    protected final Set<ProbeInfo> failed = new HashSet<ProbeInfo>();

    protected final Set<ProbeInfo> succeed = new HashSet<ProbeInfo>();

    public void registerProbe(ProbeDescriptor descriptor) {
        Class<? extends Probe> probeClass = descriptor.getProbeClass();
        Probe probe;
        try {
            probe = probeClass.newInstance();
        } catch (Exception e) {
            throw new ManagementRuntimeException(
                    "Cannot create management probe for " + descriptor);
        }
        Class<?> serviceClass = descriptor.getServiceClass();
        if (serviceClass != null) {
            Object service = Framework.getLocalService(serviceClass);
            probe.init(service);
        }
        ProbeInfo info = new ProbeInfo(descriptor, probe);
        infosByTypes.put(probeClass, info);
        infosByShortcuts.put(descriptor.getShortcut(), info);
    }


    public void unregisterProbe(ProbeDescriptor descriptor) {
        Class<? extends Probe> probeClass = descriptor.getProbeClass();
        infosByTypes.remove(probeClass);
        infosByShortcuts.remove(descriptor.getShortcut());
    }

    protected void doRunWithSafeClassLoader() {
        for (ProbeInfo context : infosByTypes.values()) {
            try {
                context.run();
                failed.remove(context);
                succeed.add(context);
            } catch (Exception e) {
                succeed.remove(context);
                failed.add(context);
            }
        }
    }

    protected void doRun() {
        for (ProbeInfo context : infosByTypes.values()) {
            try {
                context.run();
                if (context.isInError()) {
                    succeed.remove(context);
                    failed.add(context);
                } else {
                    failed.remove(context);
                    succeed.add(context);
            }
            } catch (Throwable e) {
              log.error("Error caught while executing " + context.getShortcutName(), e);
            }
        }
    }

    protected void doRunProbe(ProbeInfo probe) {
        if (!probe.isEnabled) {
            return;
        }
        try {
            probe.run();
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
