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
package org.nuxeo.ecm.platform.management.statuses;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.management.ManagementRuntimeException;

public class ProbeRegistry {

    protected final static Log log = LogFactory.getLog(ProbeRegistry.class);

    private final StatusesManagementComponent probeComponent;

    ProbeRegistry(StatusesManagementComponent probeComponent) {
        this.probeComponent = probeComponent;
    }

    protected final Map<Class<? extends Probe>, ProbeInfo> scheduled = new HashMap<Class<? extends Probe>, ProbeInfo>();

    protected final Set<ProbeInfo> failed = new HashSet<ProbeInfo>();

    protected final Set<ProbeInfo> succeed = new HashSet<ProbeInfo>();

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
        ProbeInfo context = new ProbeInfo(this.probeComponent, probe);
        probeComponent.managementPublisher.doQualifyNames(context,
                descriptor);
        probeComponent.managementPublisher.doPublishContext(context);
        scheduled.put(probeClass, context);
    }


    public void unregisterProbe(ProbeDescriptor descriptor) {
        Class<? extends Probe> probeClass = descriptor.getProbeClass();
        ProbeInfo context = scheduled.remove(probeClass);
        if (context == null) {
            throw new IllegalArgumentException("not registered probe"
                    + descriptor);
        }
        probeComponent.managementPublisher.doUnpublishContext(context);
    }

    protected void doRunWithSafeClassLoader() {
        if (!isEnabled) {
            return;
        }
        for (ProbeInfo context : scheduled.values()) {
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
        if (!isEnabled) {
            return;
        }
        for (ProbeInfo context : scheduled.values()) {
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
        if (!isEnabled || !probe.isEnabled) {
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