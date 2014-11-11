/*
 * Copyright (c) 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Nuxeo - initial API and implementation
 *
 * $Id$
 */

package org.nuxeo.runtime;

import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Set;
import java.util.logging.Level;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.common.logging.JavaUtilLoggingHelper;
import org.nuxeo.common.utils.TextTemplate;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.model.ComponentInstance;
import org.nuxeo.runtime.model.ComponentManager;
import org.nuxeo.runtime.model.ComponentName;
import org.nuxeo.runtime.model.RuntimeContext;
import org.nuxeo.runtime.model.impl.ComponentManagerImpl;
import org.nuxeo.runtime.model.impl.DefaultRuntimeContext;
import org.osgi.framework.Bundle;

/**
 * Abstract implementation of the Runtime Service.
 * <p>
 * Implementors are encouraged to extend this class instead of directly
 * implementing the {@link RuntimeService} interface.
 *
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
public abstract class AbstractRuntimeService implements RuntimeService {

    /**
     * Property that controls whether or not to redirect JUL to JCL. By default
     * is true (JUL will be redirected)
     */
    public static final String REDIRECT_JUL = "org.nuxeo.runtime.redirectJUL";

    public static final String REDIRECT_JUL_THRESHOLD = "org.nuxeo.runtime.redirectJUL.threshold";

    private static final Log log = LogFactory.getLog(RuntimeService.class);

    protected boolean isStarted = false;

    protected boolean isShuttingDown = false;

    protected File workingDir;

    protected Properties properties = new Properties();

    protected ComponentManager manager;

    protected final RuntimeContext context;

    protected final List<RuntimeExtension> extensions = new ArrayList<RuntimeExtension>();

    protected AbstractRuntimeService(DefaultRuntimeContext context) {
        this(context, null);
    }

    // warnings during the deployment. Here are collected all errors occurred
    // during the startup
    protected final List<String> warnings = new ArrayList<String>();

    protected AbstractRuntimeService(DefaultRuntimeContext context,
            Map<String, String> properties) {
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
        return Thread.currentThread().getContextClassLoader().getResource(
                "org/nuxeo/runtime/nx-feature.xml");
    }

    @Override
    public synchronized void start() throws Exception {
        if (!isStarted) {
            if (Boolean.parseBoolean(getProperty(REDIRECT_JUL, "true"))) {
                Level threshold = Level.parse(getProperty(
                        REDIRECT_JUL_THRESHOLD, "INFO").toUpperCase());
                JavaUtilLoggingHelper.redirectToApacheCommons(threshold);
            }
            log.info("Starting Nuxeo Runtime service " + getName()
                    + "; version: " + getVersion());
            // NXRuntime.setInstance(this);
            manager = createComponentManager();
            Framework.sendEvent(new RuntimeServiceEvent(
                    RuntimeServiceEvent.RUNTIME_ABOUT_TO_START, this));
            doStart();
            startExtensions();
            isStarted = true;
            Framework.sendEvent(new RuntimeServiceEvent(
                    RuntimeServiceEvent.RUNTIME_STARTED, this));
        }
    }

    @Override
    public synchronized void stop() throws Exception {
        if (isStarted) {
            isShuttingDown = true;
            try {
                log.info("Stopping Nuxeo Runtime service " + getName()
                        + "; version: " + getVersion());
                Framework.sendEvent(new RuntimeServiceEvent(
                        RuntimeServiceEvent.RUNTIME_ABOUT_TO_STOP, this));
                stopExtensions();
                doStop();
                isStarted = false;
                Framework.sendEvent(new RuntimeServiceEvent(
                        RuntimeServiceEvent.RUNTIME_STOPPED, this));
                manager.shutdown();
                // NXRuntime.setRuntime(null);
                manager = null;
                JavaUtilLoggingHelper.reset();
            } finally {
                isShuttingDown = false;
            }
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

    protected void doStart() throws Exception {
    }

    protected void doStop() throws Exception {
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
    public Properties getProperties() {
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
        Properties props = new Properties(System.getProperties());
        props.putAll(properties);
        String value = props.getProperty(name, defValue);
        if (value != null && value.startsWith("$")
                && value.equals("${" + name + "}")) {
            // avoid loop, don't expand
            return value;
        }
        return expandVars(value);
    }

    public void setProperty(String name, Object value) {
        properties.put(name, value.toString());
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        return sb.append(getName()).append(" version ").append(
                getVersion().toString()).toString();
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
            try {
                ext.start();
            } catch (Exception e) {
                log.error("Failed to start runtime extension", e);
            }
        }
    }

    protected void stopExtensions() {
        for (RuntimeExtension ext : extensions) {
            try {
                ext.stop();
            } catch (Exception e) {
                log.error("Failed to start runtime extension", e);
            }
        }
    }

    @Override
    public <T> T getService(Class<T> serviceClass) {
        return manager.getService(serviceClass);
    }

    @Override
    public String expandVars(String expression) {
        Properties props = new Properties(System.getProperties());
        props.putAll(properties);
        return new TextTemplate(props).process(expression);
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
    public boolean getStatusMessage(StringBuilder msg) {
        String hr = "======================================================================";
        if (!warnings.isEmpty()) {
            msg.append(hr).append("\n= Component Loading Errors:\n");
            for (String warning : warnings) {
                msg.append("  * ").append(warning).append('\n');
            }
        }
        Map<ComponentName, Set<ComponentName>> pendingRegistrations = manager.getPendingRegistrations();
        Collection<ComponentName> activatingRegistrations = manager.getActivatingRegistrations();
        msg.append(hr).append("\n= Component Loading Status: Pending: ").append(
                pendingRegistrations.size()).append(" / Unstarted: ").append(
                activatingRegistrations.size()).append(" / Total: ").append(
                manager.getRegistrations().size()).append('\n');
        for (Entry<ComponentName, Set<ComponentName>> e : pendingRegistrations.entrySet()) {
            msg.append("  * ").append(e.getKey()).append(" requires ").append(
                    e.getValue()).append('\n');
        }
        for (ComponentName componentName : activatingRegistrations) {
            msg.append("  - ").append(componentName).append('\n');
        }
        msg.append(hr);
        return (warnings.isEmpty() && pendingRegistrations.isEmpty() && activatingRegistrations.isEmpty());
    }

}
