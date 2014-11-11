/*
 * (C) Copyright 2006-2008 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     matic
 */
package org.nuxeo.ecm.platform.management.probes;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.management.ObjectNameFactory;
import org.nuxeo.runtime.management.ResourcePublisher;
import org.nuxeo.runtime.management.ResourcePublisherService;
import org.nuxeo.runtime.model.ComponentContext;
import org.nuxeo.runtime.model.ComponentInstance;
import org.nuxeo.runtime.model.ComponentName;
import org.nuxeo.runtime.model.DefaultComponent;

/**
 * @author Stephane Lacoin (Nuxeo EP Software Engineer)
 * 
 */
public class ProbeComponent extends DefaultComponent implements ProbeScheduler,
        ProbeSchedulerMBean, ProbeRunner {

    protected static final ComponentName NAME = new ComponentName(
            ProbeScheduler.class.getCanonicalName());

    protected static String SCHEDULE_ID = "ProbeSchedule";

    public ProbeComponent() {
        super(); // enables breaking
    }

    protected Set<String> doExtractProbesName(Collection<ProbeInfo> runners) {
        Set<String> names = new HashSet<String>();
        for (ProbeInfo runner : runners) {
            names.add(runner.shortcutName);
        }
        return names;
    }

    public Set<String> getProbeNames() {
        return doExtractProbesName(runnerRegistry.scheduledProbesContext.values());
    }

    public int getScheduledProbesCount() {
        return runnerRegistry.scheduledProbesContext.size();
    }

    public Set<String> getProbesInError() {
        return doExtractProbesName(runnerRegistry.failedProbesContext);
    }

    public int getProbesInErrorCount() {
        return runnerRegistry.failedProbesContext.size();
    }

    public Set<String> getProbesInSuccess() {
        return doExtractProbesName(runnerRegistry.succeedProbesContext);
    }

    public Set<ProbeInfo> getProbesInfoInSuccess() {
        return runnerRegistry.succeedProbesContext;
    }
    
    public int getProbesInSuccessCount() {
        return runnerRegistry.succeedProbesContext.size();
    }

    public void disable() {
        runnerRegistry.disable();
    }

    public void enable() {
        runnerRegistry.enable();
    }

    public boolean isEnabled() {
        return runnerRegistry.isEnabled();
    }

    protected class ManagementPublisher {

        protected ResourcePublisherService service;

        protected void doPublish() {
            service.registerResource("probe-scheduler",
                    ObjectNameFactory.formatProbeQualifiedName(NAME),
                    ProbeSchedulerMBean.class, ProbeComponent.this);
        }

        protected void doUnpublish() {
            service.unregisterResource("probe-scheduler",
                    ObjectNameFactory.formatProbeQualifiedName(NAME));
            for (ProbeInfo context : runnerRegistry.scheduledProbesContext.values()) {
                doUnpublishContext(context);
            }
            service = null;
        }

        protected void doPublishContext(ProbeInfo context) {
            if (service == null) {
                return;
            }
            service.registerResource(context.shortcutName,
                    context.qualifiedName, ProbeMBean.class, context);
        }

        protected void doUnpublishContext(ProbeInfo context) {
            if (service == null) {
                return;
            }
            service.unregisterResource(context.shortcutName,
                    context.qualifiedName);
        }

        protected void doQualifyNames(ProbeInfo context,
                ProbeDescriptor descriptor) {
            context.shortcutName = ObjectNameFactory.formatProbeShortName(descriptor.getShortcutName());
            context.qualifiedName = descriptor.getQualifiedName();
            if (context.qualifiedName == null) {
                context.qualifiedName = ObjectNameFactory.formatProbeQualifiedName(new ComponentName(
                        descriptor.getServiceClass().getCanonicalName()));
            }
        }
    }

    protected final ManagementPublisher managementPublisher = new ManagementPublisher();

    protected final ProbeRegistry runnerRegistry = new ProbeRegistry(this);

    @Override
    public void activate(ComponentContext context) throws Exception {
        managementPublisher.service = (ResourcePublisherService) Framework.getLocalService(ResourcePublisher.class);
        managementPublisher.doPublish();
    }

    @Override
    public void deactivate(ComponentContext context) throws Exception {
        runnerRegistry.isEnabled = false;
        managementPublisher.doUnpublish();
    }

    public static final String PROBES_EXT_KEY = "probes";

    @Override
    public void registerContribution(Object contribution,
            String extensionPoint, ComponentInstance contributor)
            throws Exception {
        if (extensionPoint.equals(PROBES_EXT_KEY)) {
            runnerRegistry.registerProbe((ProbeDescriptor) contribution);
        }
    }

    @Override
    public void unregisterContribution(Object contribution,
            String extensionPoint, ComponentInstance contributor)
            throws Exception {
        if (extensionPoint.equals(PROBES_EXT_KEY)) {
            runnerRegistry.unregisterProbe((ProbeDescriptor) contribution);
        }
    }

    public ProbeInfo getScheduledRunner(Class<? extends Probe> usecaseClass) {
        ProbeInfo runner = runnerRegistry.scheduledProbesContext.get(usecaseClass);
        if (runner == null) {
            throw new IllegalArgumentException("no probe scheduled for "
                    + usecaseClass);
        }
        return runner;
    }

    public Collection<ProbeInfo> getScheduledProbesContext() {
        return runnerRegistry.scheduledProbesContext.values();
    }

    public boolean run() {
        runnerRegistry.doRun();
        if (getProbesInErrorCount() > 0) {
            return false;
        }
        return true;
    }

    public boolean runProbe(ProbeInfo probe) {
        runnerRegistry.doRunProbe(probe);
        if (getProbesInSuccess().contains(probe.shortcutName)) {
            return true;
        }
        return false;
    }

    public Collection<ProbeInfo> getRunWithSucessProbesInfo() {
        return runnerRegistry.succeedProbesContext;
    }

    public ProbeInfo getProbeInfo(String probeQualifiedName) {
        Collection<ProbeInfo> runners = runnerRegistry.scheduledProbesContext.values();
        for (ProbeInfo runner : runners) {
            if (probeQualifiedName.equals(runner.shortcutName)) {
                return runner;
            }
        }
        return null;
    }

}
