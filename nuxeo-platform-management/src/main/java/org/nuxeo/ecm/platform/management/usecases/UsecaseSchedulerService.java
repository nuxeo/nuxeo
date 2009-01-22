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
package org.nuxeo.ecm.platform.management.usecases;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.nuxeo.ecm.core.api.event.CoreEvent;
import org.nuxeo.ecm.core.listener.AbstractEventListener;
import org.nuxeo.ecm.core.listener.CoreEventListenerService;
import org.nuxeo.ecm.platform.scheduler.core.service.SchedulerRegistryService;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.management.ManagementRuntimeException;
import org.nuxeo.runtime.management.ObjectNameFactory;
import org.nuxeo.runtime.management.ResourcePublisherService;
import org.nuxeo.runtime.model.ComponentContext;
import org.nuxeo.runtime.model.ComponentInstance;
import org.nuxeo.runtime.model.ComponentName;
import org.nuxeo.runtime.model.DefaultComponent;

/**
 * @author Stephane Lacoin (Nuxeo EP Software Engineer)
 * 
 */
public class UsecaseSchedulerService extends DefaultComponent implements
        UsecaseScheduler {

    protected static ComponentName NAME = new ComponentName(
            UsecaseScheduler.class.getCanonicalName());

    protected static String SCHEDULE_ID = UsecaseScheduler.class.getSimpleName();

    public UsecaseSchedulerService() {
        super(); // enables breaking
    }

    protected class Schedule implements
            org.nuxeo.ecm.platform.scheduler.core.interfaces.Schedule {
        public String getCronExpression() {
            return "0 0/5 * * * ?";
        }

        public String getEventCategory() {
            return "default";
        }

        public String getEventId() {
            return SCHEDULE_ID;
        }

        public String getId() {
            return SCHEDULE_ID;
        }

        public String getPassword() {
            return "";
        }

        public String getUsername() {
            return SCHEDULE_ID;
        }

        protected SchedulerRegistryService registry;

        protected void doSchedule() {
            registry = (SchedulerRegistryService) Framework.getRuntime().getComponent(
                    SchedulerRegistryService.class.getCanonicalName());
            if (registry == null) {
                throw new ManagementRuntimeException(
                        "no scheduler registry service available");
            }
            registry.registerSchedule(this);
        }

        protected void doUnschedule() {
            if (registry == null)
                return;
            registry.unregisterSchedule(this);
        }
    }

    protected final Schedule schedule = new Schedule();

    protected class ScheduleEventListener extends AbstractEventListener {

        @Override
        public boolean accepts(String eventId) {
            return eventId.equals(SCHEDULE_ID);
        }

        @Override
        public void handleEvent(CoreEvent coreEvent) throws Exception {
            runnerRegistry.doRun();
        }

        protected CoreEventListenerService service;

        protected void doListen() {
            service = Framework.getLocalService(CoreEventListenerService.class);
            if (service == null) {
                throw new ManagementRuntimeException(
                        "no event listener service available");
            }
            service.addEventListener(this);
        }

        protected void doUnlisten() {
            if (service == null)
                return;
            service.removeEventListener(this);
        }
    }

    protected final ScheduleEventListener scheduleEventListener = new ScheduleEventListener();

    protected class ManagementPublisher {

        protected ResourcePublisherService service;

        protected void setService(ResourcePublisherService service) {
            this.service = service;
        }

        protected void doPublish() {
            service.registerResource("usecase-scheduler",
                    ObjectNameFactory.formatQualifiedName(NAME),
                    UsecaseSchedulerMBean.class, new UsecaseSchedulerMBean() {

                        protected Set<String> doExtractUseCasesName(
                                Collection<UsecaseContext> runners) {
                            Set<String> names = new HashSet<String>();
                            for (UsecaseContext runner : runners) {
                                names.add(runner.shortcutName);
                            }
                            return names;
                        }

                        public Set<String> getScheduledUseCases() {
                            return doExtractUseCasesName(runnerRegistry.scheduledUsecasesContext.values());
                        }

                        public int getScheduledUseCasesCount() {
                            return runnerRegistry.scheduledUsecasesContext.size();
                        }

                        public Set<String> getUseCasesInError() {
                            return doExtractUseCasesName(runnerRegistry.failedUsecasesContext);
                        }

                        public int getUseCasesInErrorCount() {
                            return runnerRegistry.failedUsecasesContext.size();
                        }

                        public Set<String> getUseCasesInSuccess() {
                            return doExtractUseCasesName(runnerRegistry.succeedUsecasesContext);
                        }

                        public int getUseCasesInSuccessCount() {
                            return runnerRegistry.succeedUsecasesContext.size();
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
                    });
            for (UsecaseContext context : runnerRegistry.scheduledUsecasesContext.values()) {
                doPublishContext(context);
            }
        }

        protected void doUnpublish() {
            if (service == null)
                return;
            service.unregisterResource("usecase-scheduler",
                    ObjectNameFactory.formatQualifiedName(NAME)
                            + ",management=quality");
            for (UsecaseContext context : runnerRegistry.scheduledUsecasesContext.values()) {
                doUnpublishContext(context);
            }
            service = null;
        }

        protected void doPublishContext(UsecaseContext context) {
            if (service == null) {
                return;
            }
            service.registerResource(context.shortcutName,
                    context.qualifiedName, UsecaseMBean.class,
                    context.getMBean());
        }

        protected void doUnpublishContext(UsecaseContext context) {
            if (service == null) {
                return;
            }
            service.unregisterResource(context.shortcutName,
                    context.qualifiedName);
        }

        protected void doQualifyNames(UsecaseContext context,
                UsecaseDescriptor descriptor) {
            context.shortcutName = ObjectNameFactory.formatUsecaseShortName(descriptor.getShortcutName());
            context.qualifiedName = descriptor.getQualifiedName();
            if (context.qualifiedName == null) {
                context.qualifiedName = ObjectNameFactory.formatUsecaseQualifiedName(new ComponentName(
                        descriptor.getServiceClass().getCanonicalName()));
            }
        }
    }

    protected ManagementPublisher managementPublisher = new ManagementPublisher();

    protected class RunnerRegistry {

        protected final Map<Class<? extends Usecase>, UsecaseContext> scheduledUsecasesContext = new HashMap<Class<? extends Usecase>, UsecaseContext>();

        protected Set<UsecaseContext> failedUsecasesContext = new HashSet<UsecaseContext>();

        protected Set<UsecaseContext> succeedUsecasesContext = new HashSet<UsecaseContext>();

        protected void doRegisterUseCase(UsecaseDescriptor descriptor) {
            Class<? extends Usecase> usecaseClass = descriptor.getUsecaseClass();
            Class<?> serviceClass = descriptor.getServiceClass();
            Object service = Framework.getLocalService(serviceClass);
            Usecase usecase;
            try {
                usecase = usecaseClass.newInstance();
            } catch (Exception e) {
                throw new ManagementRuntimeException(
                        "Cannot create management use case for " + descriptor);
            }
            usecase.init(service);
            UsecaseContext context = new UsecaseContext(
                    UsecaseSchedulerService.this, usecase, "default");
            managementPublisher.doQualifyNames(context, descriptor);
            managementPublisher.doPublishContext(context);
            scheduledUsecasesContext.put(usecaseClass, context);
        }

        protected void doUnregisterUseCase(UsecaseDescriptor descriptor) {
            Class<? extends Usecase> usecaseClass = descriptor.getUsecaseClass();
            UsecaseContext context = scheduledUsecasesContext.remove(usecaseClass);
            if (context == null) {
                throw new IllegalArgumentException("not registered use case"
                        + descriptor);
            }
            managementPublisher.doUnpublishContext(context);
        }

        protected void doRun() {

            if (isEnabled == false) {
                return;
            }

            for (UsecaseContext context : scheduledUsecasesContext.values()) {
                try {
                    context.runner.runWithSafeClassLoader();
                    failedUsecasesContext.remove(context);
                    succeedUsecasesContext.add(context);
                } catch (Exception e) {
                    succeedUsecasesContext.remove(context);
                    failedUsecasesContext.add(context);
                }
            }
        }

        protected Boolean isEnabled = true;

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

    protected RunnerRegistry runnerRegistry = new RunnerRegistry();

    @Override
    public void activate(ComponentContext context) throws Exception {
        scheduleEventListener.doListen();
    }

    @Override
    public void deactivate(ComponentContext context) throws Exception {
        scheduleEventListener.doUnlisten();
    }

    public void enable() {
        runnerRegistry.enable();
    }

    public void disable() {
        runnerRegistry.disable();
    }

    public Boolean isEnabled() {
        return runnerRegistry.isEnabled();
    }

    public static final String USECASES_EXT_KEY = "usecases";

    @Override
    public void registerContribution(Object contribution,
            String extensionPoint, ComponentInstance contributor)
            throws Exception {
        if (extensionPoint.equals(USECASES_EXT_KEY)) {
            runnerRegistry.doRegisterUseCase((UsecaseDescriptor) contribution);
        }
    }

    @Override
    public void unregisterContribution(Object contribution,
            String extensionPoint, ComponentInstance contributor)
            throws Exception {
        if (extensionPoint.equals(USECASES_EXT_KEY)) {
            runnerRegistry.doUnregisterUseCase((UsecaseDescriptor) contribution);
        }
    }

    public UsecaseContext getScheduledRunner(
            Class<? extends Usecase> usecaseClass) {
        UsecaseContext runner = runnerRegistry.scheduledUsecasesContext.get(usecaseClass);
        if (runner == null) {
            throw new IllegalArgumentException("no usecase scheduled for "
                    + usecaseClass);
        }
        return runner;
    }

    public Collection<UsecaseContext> getScheduledUsecasesContext() {
        return runnerRegistry.scheduledUsecasesContext.values();
    }

}
