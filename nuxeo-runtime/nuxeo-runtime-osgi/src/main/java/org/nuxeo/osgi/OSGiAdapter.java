/*
 * (C) Copyright 2006-2007 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     Nuxeo - initial API and implementation
 * $Id$
 */

package org.nuxeo.osgi;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.Properties;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.common.collections.ListenerList;
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


/**
 * @author  <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class OSGiAdapter {

    private static final Log log = LogFactory.getLog(OSGiAdapter.class);

    protected final File workingDir;
    protected final File idTableFile;
    protected final BundleIdGenerator bundleIds;

    protected ListenerList frameworkListeners;
    protected ListenerList bundleListeners;
    protected ListenerList serviceListeners;

    protected Map<String, Bundle> bundles;
    protected Map<String, ServiceRegistration> services;

    protected BundleRegistry registry;

    protected Properties properties;

    protected SystemBundle systemBundle;


    public OSGiAdapter(File workingDir) {
        this.workingDir = workingDir;
        properties = new Properties();
        registry = new BundleRegistry();
        frameworkListeners = new ListenerList();
        bundleListeners = new ListenerList();
        serviceListeners = new ListenerList();
        bundleIds = new BundleIdGenerator();
        idTableFile = new File(workingDir, "bundles.ids");
        bundleIds.load(idTableFile);

        // setting up default properties
        properties.put(Constants.FRAMEWORK_VENDOR, "Nuxeo");
        properties.put(Constants.FRAMEWORK_VERSION, "1.0.0");
    }

    public void setSystemBundle(SystemBundle systemBundle) throws BundleException {
        if (this.systemBundle != null) {
            throw new IllegalStateException("Cannot set system bundle");
        }
        install(systemBundle);
        registry.addBundleAlias("system.bundle", systemBundle.getSymbolicName());
        this.systemBundle = systemBundle;
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
    }

    public long getBundleId(String symbolicName) {
        return bundleIds.getBundleId(symbolicName);
    }

    public File getWorkingDir() {
        return workingDir;
    }

    public BundleImpl[] getInstalledBundles() {
        return registry.getInstalledBundles();
    }

    public void install(BundleImpl bundle) throws BundleException {
        registry.install(bundle);
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
        throw new UnsupportedOperationException("This method is not implemented");
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
        Object[] listeners = frameworkListeners.getListeners();
        for (Object listener : listeners) {
            ((FrameworkListener) listener).frameworkEvent(event);
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

}
