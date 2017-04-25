/*
 * (C) Copyright 2006-2017 Nuxeo (http://nuxeo.com/) and others.
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
 *     Nuxeo - initial API and implementation
 */
package org.nuxeo.runtime;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.logging.Level;
import java.util.stream.Collectors;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.common.codec.CryptoProperties;
import org.nuxeo.common.logging.JavaUtilLoggingHelper;
import org.nuxeo.common.logging.Log4JHelper;
import org.nuxeo.common.logging.LoggingConfigWatchdog;
import org.nuxeo.common.utils.TextTemplate;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.model.ComponentInstance;
import org.nuxeo.runtime.model.ComponentManager;
import org.nuxeo.runtime.model.ComponentName;
import org.nuxeo.runtime.model.Extension;
import org.nuxeo.runtime.model.RegistrationInfo;
import org.nuxeo.runtime.model.RuntimeContext;
import org.nuxeo.runtime.model.impl.ComponentManagerImpl;
import org.nuxeo.runtime.model.impl.DefaultRuntimeContext;
import org.osgi.framework.Bundle;

/**
 * Abstract implementation of the Runtime Service.
 * <p>
 * Implementors are encouraged to extend this class instead of directly implementing the {@link RuntimeService}
 * interface.
 *
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
public abstract class AbstractRuntimeService implements RuntimeService {

    public static class RIApplicationStartedComparator implements Comparator<RegistrationInfo> {
        @Override
        public int compare(RegistrationInfo r1, RegistrationInfo r2) {
            int cmp = Integer.compare(r1.getApplicationStartedOrder(), r2.getApplicationStartedOrder());
            if (cmp == 0) {
                // fallback on name order, to be deterministic
                cmp = r1.getName().getName().compareTo(r2.getName().getName());
            }
            return cmp;
        }
    }

    /**
     * Property that controls whether or not to redirect JUL to JCL. By default is true (JUL will be redirected)
     */
    public static final String REDIRECT_JUL = "org.nuxeo.runtime.redirectJUL";

    public static final String REDIRECT_JUL_THRESHOLD = "org.nuxeo.runtime.redirectJUL.threshold";

    public static final String LOG4J_WATCH_DISABLED = "org.nuxeo.runtime.log4jwatch.disabled";

    public static final String LOG4J_WATCH_DELAY = "org.nuxeo.runtime.log4jwatch.delay";

    public static final long LOG4J_WATCH_DELAY_DEFAULT = 10;

    // package-private for subclass access without synthetic accessor
    static final Log log = LogFactory.getLog(RuntimeService.class);

    enum State {
        STOPPED, LOADING, UNLOADING, STANDBY, STARTING, STOPPING, STARTED
    };

    State state = State.STOPPED;

    protected File workingDir;

    protected CryptoProperties properties = new CryptoProperties(System.getProperties());

    protected ComponentManager manager;

    protected final RuntimeContext context;

    protected final List<RuntimeExtension> extensions = new ArrayList<>();

    protected AbstractRuntimeService(DefaultRuntimeContext context) {
        this(context, null);
    }

    /**
     * Warnings during the deployment. These messages don't block startup, even in strict mode.
     */
    protected final List<String> warnings = new ArrayList<>();

    /**
     * Errors during the deployment. Here are collected all errors occurred during the startup. These messages block
     * startup in strict mode.
     *
     * @since 9.1
     */
    protected final List<String> errors = new ArrayList<>();

    protected AbstractRuntimeService(DefaultRuntimeContext context, Map<String, String> properties) {
        this.context = context;
        context.setRuntime(this);
        if (properties != null) {
            this.properties.putAll(properties);
        }
        // get errors set by NuxeoDeployer
        String errs = System.getProperty("org.nuxeo.runtime.deployment.errors");
        if (errs != null) {
            errors.addAll(Arrays.asList(errs.split("\n")));
            System.clearProperty("org.nuxeo.runtime.deployment.errors");
        }
    }

    @Override
    public List<String> getWarnings() {
        return warnings;
    }

    /**
     * @since 9.1
     */
    @Override
    public List<String> getErrors() {
        return errors;
    }

    protected ComponentManager createComponentManager() {
        return new ComponentManagerImpl(this);
    }

    protected static URL getBuiltinFeatureURL() {
        return Thread.currentThread().getContextClassLoader().getResource("org/nuxeo/runtime/nx-feature.xml");
    }

    @Override
    public void start() {
        if (state != State.STOPPED) {
            return;
        }

        manager = createComponentManager();
        try {
            loadConfig();
        } catch (IOException e) {
            throw new RuntimeServiceException(e);
        }

        if (Boolean.parseBoolean(getProperty(REDIRECT_JUL, "true"))) {
            Level threshold = Level.parse(getProperty(REDIRECT_JUL_THRESHOLD, "INFO").toUpperCase());
            JavaUtilLoggingHelper.redirectToApacheCommons(threshold);
        }
        if (Boolean.parseBoolean(getProperty(LOG4J_WATCH_DISABLED, "false"))) {
            log.info("Disabled log4j.xml change detection");
        } else {
            Framework.addListener(new RuntimeServiceListener() {

                final long delay = loadDelay();

                long loadDelay() {
                    try {
                        return Long.parseLong(getProperty(LOG4J_WATCH_DELAY, Long.toString(LOG4J_WATCH_DELAY_DEFAULT)));
                    } catch (NumberFormatException e) {
                        return LOG4J_WATCH_DELAY_DEFAULT;
                    }
                }

                LoggingConfigWatchdog wdog;

                @Override
                public void handleEvent(RuntimeServiceEvent event) {
                   if (event.id == RuntimeServiceEvent.RUNTIME_ABOUT_TO_START) {
                       onStart();
                   } else if (event.id == RuntimeServiceEvent.RUNTIME_ABOUT_TO_RESUME) {
                       onResume();
                   } else if (event.id == RuntimeServiceEvent.RUNTIME_ABOUT_TO_STANDBY) {
                       onStandby();
                   }
                }

                void onStart() {
                    wdog = Log4JHelper.configureAndWatch(delay);
                    if (wdog == null) {
                        log.warn("Failed to configure log4j.xml change detection");
                    } else {
                        log.info("Configured log4j.xml change detection with a delay of " + delay + "s");
                    }
                }

                void onResume() {
                    if (wdog != null) {
                        return;
                    }
                    wdog = Log4JHelper.configureAndWatch(delay);
                }

                void onStandby() {
                    if (wdog == null) {
                        return;
                    }
                    try {
                        wdog.cancel();
                    } finally {
                        wdog = null;
                    }
                }
            });

        }
        log.info("Starting Nuxeo Runtime service " + getName() + "; version: " + getVersion());

        Framework.sendEvent(new RuntimeServiceEvent(RuntimeServiceEvent.RUNTIME_ABOUT_TO_START, this));
        try {
            state = State.LOADING;
            doStart();
            startExtensions();
        } finally {
            state = State.STANDBY;
            Framework.sendEvent(new RuntimeServiceEvent(RuntimeServiceEvent.RUNTIME_STARTED, this));
        }
    }

    @Override
    public void stop() {
        if (state.ordinal() < State.STANDBY.ordinal()) {
            return;
        }
        if (state == State.STARTED) {
            standby(Instant.now());
        }
        try {
            log.info("Stopping Nuxeo Runtime service " + getName() + "; version: " + getVersion());
            Framework.sendEvent(new RuntimeServiceEvent(RuntimeServiceEvent.RUNTIME_ABOUT_TO_STOP, this));
            try {
                state = State.UNLOADING;
                stopExtensions();
                doStop();
                manager.shutdown();
            } finally {
                state = State.STOPPED;
                Framework.sendEvent(new RuntimeServiceEvent(RuntimeServiceEvent.RUNTIME_STOPPED, this));
                manager = null;
            }
        } finally {
            JavaUtilLoggingHelper.reset();
        }
    }

    @Override
    public boolean isStarted() {
        return state.ordinal() >= State.STANDBY.ordinal();
    }

    @Override
    public void resume() {
        Framework.sendEvent(new RuntimeServiceEvent(RuntimeServiceEvent.RUNTIME_ABOUT_TO_RESUME, this));
        try {
            state = State.STARTING;
            resumeComponents();
            resumeExtensions();
        } finally {
            state = State.STARTED;
            Framework.sendEvent(new RuntimeServiceEvent(RuntimeServiceEvent.RUNTIME_RESUME, this));
        }
    }

    @Override
    public boolean isRunning() {
        return state == State.STARTED;
    }

    @Override
    public void standby(Instant limit) {
        Framework.sendEvent(new RuntimeServiceEvent(RuntimeServiceEvent.RUNTIME_ABOUT_TO_STANDBY, this));
        try {
            state = State.STOPPING;
            standbyExtensions();
            standbyComponents(limit);
        } finally {
            state = State.STANDBY;
            Framework.sendEvent(new RuntimeServiceEvent(RuntimeServiceEvent.RUNTIME_STANDBY, this));
        }
    }

    @Override
    public boolean isStandby() {
        return state == State.STANDBY;
    }

    @Override
    public boolean isShuttingDown() {
        return state == State.STOPPING;
    }

    protected void loadConfig() throws IOException {
    }

    protected void doStart() {
    }

    protected void doStop() {
    }

    /**
     * @since 9.2
     */
    protected void resumeComponents() {
        notifyComponentsOnStarted();
    }

    /**
     * @since 9.2
     */
    protected void standbyComponents(Instant limit) {
        notifyComponentsOnStandby(limit);
    }

    @Override
    public File getHome() {
        return workingDir;
    }

    public void setHome(File home) {
        workingDir = home;
    }

    @Override
    public String getDescription() {
        return toString();
    }

    @Override
    public CryptoProperties getProperties() {
        // do not unreference properties: some methods rely on this to set
        // variables here...
        return properties;
    }

    @Override
    public String getProperty(String name) {
        return getProperty(name, null);
    }

    @Override
    public String getProperty(String name, String defValue) {
        String value = properties.getProperty(name, defValue);
        if (value == null || ("${" + name + "}").equals(value)) {
            // avoid loop, don't expand
            return value;
        }
        return expandVars(value);
    }

    @Override
    public void setProperty(String name, Object value) {
        properties.setProperty(name, value.toString());
    }

    @Override
    public String toString() {
        return getName() + " version " + getVersion();
    }

    @Override
    public Object getComponent(String name) {
        ComponentInstance co = getComponentInstance(name);
        return co != null ? co.getInstance() : null;
    }

    @Override
    public Object getComponent(ComponentName name) {
        ComponentInstance co = getComponentInstance(name);
        return co != null ? co.getInstance() : null;
    }

    @Override
    public ComponentInstance getComponentInstance(String name) {
        return manager.getComponent(new ComponentName(name));
    }

    @Override
    public ComponentInstance getComponentInstance(ComponentName name) {
        return manager.getComponent(name);
    }

    @Override
    public ComponentManager getComponentManager() {
        return manager;
    }

    @Override
    public RuntimeContext getContext() {
        return context;
    }

    protected void startExtensions() {
        for (RuntimeExtension ext : extensions) {
            ext.start();
        }
    }

    protected void stopExtensions() {
        for (RuntimeExtension ext : extensions) {
            ext.stop();
        }
    }

    protected void resumeExtensions() {
        for (RuntimeExtension ext : extensions) {
            ext.resume();
        }
    }

    protected void standbyExtensions() {
        for (RuntimeExtension ext : extensions) {
            ext.standby();
        }
    }

    @Override
    public <T> T getService(Class<T> serviceClass) {
        return manager.getService(serviceClass);
    }

    @Override
    public String expandVars(String expression) {
        return new TextTemplate(properties).processText(expression);
    }

    @Override
    public File getBundleFile(Bundle bundle) {
        return null;
    }

    @Override
    public Bundle getBundle(String symbolicName) {
        throw new UnsupportedOperationException("Not implemented");
    }

    /**
     * @since 5.5
     * @param msg
     *            summary message about all components loading status
     * @return true if there was no detected error, else return false
     */
    @Override
    public boolean getStatusMessage(StringBuilder msg) {
        String hr = "======================================================================";
        if (!warnings.isEmpty()) {
            msg.append(hr).append("\n= Component Loading Warnings:\n");
            for (String warning : warnings) {
                msg.append("  * ").append(warning).append('\n');
            }
        }
        if (!errors.isEmpty()) {
            msg.append(hr).append("\n= Component Loading Errors:\n");
            for (String error : errors) {
                msg.append("  * ").append(error).append('\n');
            }
        }
        Map<ComponentName, Set<ComponentName>> pendingRegistrations = manager.getPendingRegistrations();
        Map<ComponentName, Set<Extension>> missingRegistrations = manager.getMissingRegistrations();
        Collection<ComponentName> unstartedRegistrations = manager.getActivatingRegistrations();
        unstartedRegistrations.addAll(manager.getStartFailureRegistrations());
        msg.append(hr).append("\n= Component Loading Status: Pending: ").append(pendingRegistrations.size())
                .append(" / Missing: ").append(missingRegistrations.size()).append(" / Unstarted: ")
                .append(unstartedRegistrations.size()).append(" / Total: ").append(manager.getRegistrations().size())
                .append('\n');
        for (Entry<ComponentName, Set<ComponentName>> e : pendingRegistrations.entrySet()) {
            msg.append("  * ").append(e.getKey()).append(" requires ").append(e.getValue()).append('\n');
        }
        for (Entry<ComponentName, Set<Extension>> e : missingRegistrations.entrySet()) {
            msg.append("  * ").append(e.getKey()).append(" references missing ")
                    .append(e.getValue().stream().map(
                            ext -> "target=" + ext.getTargetComponent().getName() + ";point=" + ext.getExtensionPoint())
                            .collect(Collectors.toList()))
                    .append('\n');
        }
        for (ComponentName componentName : unstartedRegistrations) {
            msg.append("  - ").append(componentName).append('\n');
        }
        msg.append(hr);
        return (errors.isEmpty() && pendingRegistrations.isEmpty() && missingRegistrations.isEmpty()
                && unstartedRegistrations.isEmpty());
    }

    /**
     * Error logger which does its logging from a separate thread, for thread isolation.
     *
     * @param message
     *            the message to log
     * @return a thread that can be started to do the logging
     * @since 9.2, 8.10-HF05
     */
    public static Thread getErrorLoggerThread(String message) {
        return new Thread() {
            @Override
            public void run() {
                log.error(message);
            }
        };
    }

    protected void notifyComponentsOnStarted() {
        List<RegistrationInfo> ris = new ArrayList<>(manager.getRegistrations());
        Collections.sort(ris, new RIApplicationStartedComparator());
        for (RegistrationInfo ri : ris) {
            try {
                ri.notifyApplicationStarted();
            } catch (RuntimeException e) {
                log.error("Failed to notify component '" + ri.getName() + "' on application started", e);
            }
        }
    }

    protected void notifyComponentsOnStandby(Instant instant) {
        List<RegistrationInfo> ris = new ArrayList<>(manager.getRegistrations());
        Collections.sort(ris, Collections.reverseOrder(new RIApplicationStartedComparator()));
        for (RegistrationInfo ri : ris) {
            try {
                ri.notifyApplicationStandby(instant);
            } catch (RuntimeException e) {
                log.error("Failed to notify component '" + ri.getName() + "' on application stand by", e);
            }
        }
    }

}
