/*
 * (C) Copyright 2006-2011 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 * $Id$
 */

package org.nuxeo.osgi;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.common.Environment;
import org.nuxeo.common.collections.ListenerList;
import org.nuxeo.osgi.services.PackageAdminImpl;
import org.nuxeo.osgi.util.jar.JarFileCloser;
import org.nuxeo.osgi.util.jar.URLJarFileIntrospector;
import org.nuxeo.osgi.util.jar.URLJarFileIntrospectionError;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleEvent;
import org.osgi.framework.BundleException;
import org.osgi.framework.BundleListener;
import org.osgi.framework.Constants;
import org.osgi.framework.FrameworkEvent;
import org.osgi.framework.FrameworkListener;
import org.osgi.framework.ServiceEvent;
import org.osgi.framework.ServiceListener;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.packageadmin.PackageAdmin;

/**
 *
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class OSGiAdapter {

    private static final Log log = LogFactory.getLog(OSGiAdapter.class);

    protected final File workingDir;

    protected final File dataDir;

    protected File idTableFile;

    protected BundleIdGenerator bundleIds;

    protected ListenerList frameworkListeners;

    protected ListenerList bundleListeners;

    protected ListenerList serviceListeners;

    protected Map<String, ServiceRegistration> services;

    protected BundleRegistry registry;

    protected Properties properties;

    protected SystemBundle systemBundle;

    protected JarFileCloser uRLJarFileCloser;

    public OSGiAdapter(File workingDir) {
        this(workingDir, new File(System.getProperty(
                Environment.NUXEO_DATA_DIR, workingDir + File.separator
                        + "data")), new Properties());
    }

    public OSGiAdapter(File workingDir, File dataDir, Properties properties) {
        services = new ConcurrentHashMap<String, ServiceRegistration>();
        this.workingDir = workingDir;
        this.dataDir = dataDir;
        this.dataDir.mkdirs();
        this.workingDir.mkdirs();
        initialize(properties);
    }

    public void removeService(String clazz) {
        services.remove(clazz);
    }

    protected void initialize(Properties properties) {
        this.properties = properties == null ? new Properties() : properties;
        registry = new BundleRegistry();
        frameworkListeners = new ListenerList();
        bundleListeners = new ListenerList();
        serviceListeners = new ListenerList();
        bundleIds = new BundleIdGenerator();
        idTableFile = new File(dataDir, "bundles.ids");
        bundleIds.load(idTableFile);
        // setting up default properties
        properties.put(Constants.FRAMEWORK_VENDOR, "Nuxeo");
        properties.put(Constants.FRAMEWORK_VERSION, "1.0.0");
    }

    public void setSystemBundle(SystemBundle systemBundle)
            throws BundleException {
        if (this.systemBundle != null) {
            throw new IllegalStateException("Cannot set system bundle");
        }
        install(systemBundle);
        registry.addBundleAlias("system.bundle", systemBundle.getSymbolicName());
        this.systemBundle = systemBundle;

        systemBundle.getBundleContext().registerService(
                PackageAdmin.class.getName(), new PackageAdminImpl(this), null);
    }

    public BundleRegistry getRegistry() {
        return registry;
    }

    public String getProperty(String key) {
        String value = properties.getProperty(key);
        if (value == null) {
            value = System.getProperty(key);
        }
        return value;
    }

    public String getProperty(String key, String defvalue) {
        String val = getProperty(key);
        if (val == null) {
            val = defvalue;
        }
        return val;
    }

    /**
     * @param name the property name.
     * @param value the property value.
     */
    public void setProperty(String name, String value) {
        properties.put(name, value);
    }

    public void shutdown() throws IOException {
        bundleIds.store(idTableFile);
        registry.shutdown();
        properties.clear();
        registry = null;
        frameworkListeners = null;
        bundleListeners = null;
        serviceListeners = null;
        properties = null;
        uRLJarFileCloser = null;
    }

    public long getBundleId(String symbolicName) {
        return bundleIds.getBundleId(symbolicName);
    }

    public File getWorkingDir() {
        return workingDir;
    }

    public File getDataDir() {
        return dataDir;
    }

    public BundleImpl getBundle(String symbolicName) {
        return registry.getBundle(symbolicName);
    }

    public BundleImpl[] getInstalledBundles() {
        return registry.getInstalledBundles();
    }

    public void install(BundleImpl bundle) throws BundleException {
        double s = System.currentTimeMillis();
        registry.install(bundle);
        bundle.startupTime = System.currentTimeMillis() - s;
    }

    public void uninstall(BundleImpl bundle) throws BundleException {
        registry.uninstall(bundle);
    }

    public void addFrameworkListener(FrameworkListener listener) {
        frameworkListeners.add(listener);
    }

    public void removeFrameworkListener(FrameworkListener listener) {
        frameworkListeners.remove(listener);
    }

    public void addServiceListener(ServiceListener listener) {
        serviceListeners.add(listener);
    }

    public void addServiceListener(ServiceListener listener, String filter) {
        // TODO?
        throw new UnsupportedOperationException(
                "This method is not implemented");
    }

    public void removeServiceListener(ServiceListener listener) {
        serviceListeners.remove(listener);
    }

    public void addBundleListener(BundleListener listener) {
        bundleListeners.add(listener);
    }

    public void removeBundleListener(BundleListener listener) {
        bundleListeners.remove(listener);
    }

    public void fireFrameworkEvent(FrameworkEvent event) {
        log.debug("Firing FrameworkEvent on " + frameworkListeners.size()
                + " listeners");
        if (event.getType() == FrameworkEvent.STARTED) {
            uRLJarFileCloser = newJarFileCloser();
        }
        Object[] listeners = frameworkListeners.getListeners();
        for (Object listener : listeners) {
            log.debug("Start execution of " + listener.getClass() + " listener");
            try {
                ((FrameworkListener) listener).frameworkEvent(event);
                log.debug("End execution of " + listener.getClass()
                        + " listener");
            } catch (RuntimeException e) {
                log.error("Error during Framework Listener execution : "
                        + listener.getClass(), e);
            }
        }
    }

    protected JarFileCloser newJarFileCloser() {
        try {
            URLJarFileIntrospector introspector = new URLJarFileIntrospector();
            return introspector.newJarFileCloser(systemBundle.loader);
        } catch (URLJarFileIntrospectionError cause) {
            log.warn("Cannot put URL jar file closer in place", cause);
            return JarFileCloser.NOOP;
        }
    }

    public void fireServiceEvent(ServiceEvent event) {
        Object[] listeners = serviceListeners.getListeners();
        for (Object listener : listeners) {
            ((ServiceListener) listener).serviceChanged(event);
        }
    }

    public void fireBundleEvent(BundleEvent event) {
        Object[] listeners = bundleListeners.getListeners();
        for (Object listener : listeners) {
            ((BundleListener) listener).bundleChanged(event);
        }
    }

    public Bundle getSystemBundle() {
        return systemBundle;
    }

    /**
     * helper for closing jar files during bundle uninstall
     * @since 5.6
     */
    public JarFileCloser getURLJarFileCloser() {
        if (uRLJarFileCloser == null) {
            uRLJarFileCloser = newJarFileCloser();
        }
        return uRLJarFileCloser;
    }
}
