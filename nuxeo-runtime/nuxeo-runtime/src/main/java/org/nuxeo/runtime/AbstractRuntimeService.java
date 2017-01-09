/*
 * (C) Copyright 2006-2015 Nuxeo SA (http://nuxeo.com/) and others.
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
 *
 */

package org.nuxeo.runtime;

import java.io.File;
import java.net.URL;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
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
import org.nuxeo.common.utils.TextTemplate;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.api.ServicePassivator;
import org.nuxeo.runtime.model.ComponentInstance;
import org.nuxeo.runtime.model.ComponentManager;
import org.nuxeo.runtime.model.ComponentName;
import org.nuxeo.runtime.model.Extension;
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

    /**
     * Property that controls whether or not to redirect JUL to JCL. By default is true (JUL will be redirected)
     */
    public static final String REDIRECT_JUL = "org.nuxeo.runtime.redirectJUL";

    public static final String REDIRECT_JUL_THRESHOLD = "org.nuxeo.runtime.redirectJUL.threshold";

    private static final Log log = LogFactory.getLog(RuntimeService.class);

    protected boolean isStarted = false;

    protected boolean isShuttingDown = false;

    protected File workingDir;

    protected CryptoProperties properties = new CryptoProperties(System.getProperties());

    protected ComponentManager manager;

    protected final RuntimeContext context;

    protected final List<RuntimeExtension> extensions = new ArrayList<>();

    protected AbstractRuntimeService(DefaultRuntimeContext context) {
        this(context, null);
    }

    // warnings during the deployment. Here are collected all errors occurred
    // during the startup
    protected final List<String> warnings = new ArrayList<>();

    protected AbstractRuntimeService(DefaultRuntimeContext context, Map<String, String> properties) {
        this.context = context;
        context.setRuntime(this);
        if (properties != null) {
            this.properties.putAll(properties);
        }
        // get errors set by NuxeoDeployer
        String errs = System.getProperty("org.nuxeo.runtime.deployment.errors");
        if (errs != null) {
            warnings.addAll(Arrays.asList(errs.split("\n")));
            System.clearProperty("org.nuxeo.runtime.deployment.errors");
        }
    }

    @Override
    public List<String> getWarnings() {
        return warnings;
    }

    protected ComponentManager createComponentManager() {
        return new ComponentManagerImpl(this);
    }

    protected static URL getBuiltinFeatureURL() {
        return Thread.currentThread().getContextClassLoader().getResource("org/nuxeo/runtime/nx-feature.xml");
    }

    @Override
    public synchronized void start() {
        if (isStarted) {
            return;
        }
        if (Boolean.parseBoolean(getProperty(REDIRECT_JUL, "true"))) {
            Level threshold = Level.parse(getProperty(REDIRECT_JUL_THRESHOLD, "INFO").toUpperCase());
            JavaUtilLoggingHelper.redirectToApacheCommons(threshold);
        }
        log.info("Starting Nuxeo Runtime service " + getName() + "; version: " + getVersion());
        // NXRuntime.setInstance(this);
        manager = createComponentManager();
        Framework.sendEvent(new RuntimeServiceEvent(RuntimeServiceEvent.RUNTIME_ABOUT_TO_START, this));
        ServicePassivator.passivate()
                         .withQuietDelay(Duration.ofSeconds(0))
                         .monitor()
                         .withTimeout(Duration.ofSeconds(0))
                         .withEnforceMode(false)
                         .await()
                         .proceed(() -> {
                             try {
                                 doStart();
                                 startExtensions();
                             } finally {
                                 Framework.sendEvent(
                                         new RuntimeServiceEvent(RuntimeServiceEvent.RUNTIME_STARTED, this));
                                 isStarted = true;
                             }
                         });
    }

    @Override
    public synchronized void stop() {
        if (!isStarted) {
            return;
        }
        isShuttingDown = true;
        try {
            log.info("Stopping Nuxeo Runtime service " + getName() + "; version: " + getVersion());
            Framework.sendEvent(new RuntimeServiceEvent(RuntimeServiceEvent.RUNTIME_ABOUT_TO_STOP, this));
            ServicePassivator.passivate()
                             .withQuietDelay(Duration.ofSeconds(0))
                             .monitor()
                             .withTimeout(Duration.ofSeconds(0))
                             .withEnforceMode(false)
                             .await()
                             .proceed(() -> {
                                 try {
                                     stopExtensions();
                                     doStop();
                                     manager.shutdown();
                                 } finally {
                                     isStarted = false;
                                     Framework.sendEvent(
                                             new RuntimeServiceEvent(RuntimeServiceEvent.RUNTIME_STOPPED, this));
                                     manager = null;
                                 }
                             });
        } finally {
            JavaUtilLoggingHelper.reset();
            isShuttingDown = false;
        }
    }

    @Override
    public boolean isStarted() {
        return isStarted;
    }

    @Override
    public boolean isShuttingDown() {
        return isShuttingDown;
    }

    protected void doStart() {
    }

    protected void doStop() {
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
        StringBuilder sb = new StringBuilder();
        return sb.append(getName()).append(" version ").append(getVersion().toString()).toString();
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
     * @param msg summary message about all components loading status
     * @return true if there was no detected error, else return false
     */
    @Override
    public boolean getStatusMessage(StringBuilder msg) {
        String hr = "======================================================================";
        if (!warnings.isEmpty()) {
            msg.append(hr).append("\n= Component Loading Errors:\n");
            for (String warning : warnings) {
                msg.append("  * ").append(warning).append('\n');
            }
        }
        Map<ComponentName, Set<ComponentName>> pendingRegistrations = manager.getPendingRegistrations();
        Map<ComponentName, Set<Extension>> missingRegistrations = manager.getMissingRegistrations();
        Collection<ComponentName> unstartedRegistrations = manager.getActivatingRegistrations();
        unstartedRegistrations.addAll(manager.getStartFailureRegistrations());
        msg.append(hr)
           .append("\n= Component Loading Status: Pending: ")
           .append(pendingRegistrations.size())
           .append(" / Missing: ")
           .append(missingRegistrations.size())
           .append(" / Unstarted: ")
           .append(unstartedRegistrations.size())
           .append(" / Total: ")
           .append(manager.getRegistrations().size())
           .append('\n');
        for (Entry<ComponentName, Set<ComponentName>> e : pendingRegistrations.entrySet()) {
            msg.append("  * ").append(e.getKey()).append(" requires ").append(e.getValue()).append('\n');
        }
        for (Entry<ComponentName, Set<Extension>> e : missingRegistrations.entrySet()) {
            msg.append("  * ")
               .append(e.getKey())
               .append(" references missing ")
               .append(e.getValue()
                        .stream()
                        .map(ext -> "target=" + ext.getTargetComponent().getName() + ";point="
                                + ext.getExtensionPoint())
                        .collect(Collectors.toList()))
               .append('\n');
        }
        for (ComponentName componentName : unstartedRegistrations) {
            msg.append("  - ").append(componentName).append('\n');
        }
        msg.append(hr);
        return (warnings.isEmpty() && pendingRegistrations.isEmpty() && missingRegistrations.isEmpty()
                && unstartedRegistrations.isEmpty());
    }

}
