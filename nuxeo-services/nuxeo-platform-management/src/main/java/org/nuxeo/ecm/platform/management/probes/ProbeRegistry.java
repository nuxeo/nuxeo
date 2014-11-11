/*
 * (C) Copyright 2009 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     mariana
 */
package org.nuxeo.ecm.platform.management.probes;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.management.ManagementRuntimeException;
import org.nuxeo.ecm.platform.management.probes.ProbeInfo;

class ProbeRegistry {

    /**
     * 
     */
    private final ProbeComponent probeComponent;

    /**
     * @param probeComponent
     */
    ProbeRegistry(ProbeComponent probeComponent) {
        this.probeComponent = probeComponent;
    }

    protected final Map<Class<? extends Probe>, ProbeInfo> scheduledProbesContext = new HashMap<Class<? extends Probe>, ProbeInfo>();

    protected final Set<ProbeInfo> failedProbesContext = new HashSet<ProbeInfo>();

    protected final Set<ProbeInfo> succeedProbesContext = new HashSet<ProbeInfo>();

    public void registerProbe(ProbeDescriptor descriptor) {
        Class<? extends Probe> probeClass = descriptor.getProbeClass();
        Class<?> serviceClass = descriptor.getServiceClass();
        Object service = Framework.getLocalService(serviceClass);
        Probe probe;
        try {
            probe = probeClass.newInstance();
        } catch (Exception e) {
            throw new ManagementRuntimeException(
                    "Cannot create management probe for " + descriptor);
        }
        probe.init(service);
        ProbeInfo context = new ProbeInfo(this.probeComponent, probe, "default");
        this.probeComponent.managementPublisher.doQualifyNames(context,
                descriptor);
        this.probeComponent.managementPublisher.doPublishContext(context);
        scheduledProbesContext.put(probeClass, context);
    }

    public void unregisterProbe(ProbeDescriptor descriptor) {
        Class<? extends Probe> probeClass = descriptor.getProbeClass();
        ProbeInfo context = scheduledProbesContext.remove(probeClass);
        if (context == null) {
            throw new IllegalArgumentException("not registered probe"
                    + descriptor);
        }
        this.probeComponent.managementPublisher.doUnpublishContext(context);
    }

    protected void doRunWithSafeClassLoader() {
        if (!isEnabled) {
            return;
        }
        for (ProbeInfo context : scheduledProbesContext.values()) {
            try {
                context.runner.runWithSafeClassLoader();
                failedProbesContext.remove(context);
                succeedProbesContext.add(context);
            } catch (Exception e) {
                succeedProbesContext.remove(context);
                failedProbesContext.add(context);
            }
        }
    }

    protected void doRun() {
        if (!isEnabled) {
            return;
        }
        for (ProbeInfo context : scheduledProbesContext.values()) {
            try {
                context.runner.runUnrestricted();
                failedProbesContext.remove(context);
                succeedProbesContext.add(context);
            } catch (Exception e) {
                succeedProbesContext.remove(context);
                failedProbesContext.add(context);
            }
        }
    }
    
    protected void doRunProbe(ProbeInfo probe) {
        if (!isEnabled) {
            return;
        }
        try {
            probe.runner.runWithSafeClassLoader();
            failedProbesContext.remove(probe);
            succeedProbesContext.add(probe);
        } catch (Exception e) {
            succeedProbesContext.remove(probe);
            failedProbesContext.add(probe);
        }
    }

    protected boolean isEnabled = true;

    public void enable() {
        isEnabled = true;
    }

    public void disable() {
        isEnabled = false;
    }

    public boolean isEnabled() {
        return isEnabled;
    }
}