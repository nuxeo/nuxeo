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
 *
 * $Id$
 */

package org.nuxeo.osgi;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Dictionary;
import java.util.Enumeration;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleEvent;
import org.osgi.framework.BundleException;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class BundleImpl implements Bundle {

    protected final long id;

    protected final String symbolicName;

    protected final Dictionary<String, String> headers;

    protected final BundleContext context;

    protected final OSGiAdapter osgi;

    protected final BundleFile file;

    protected final ClassLoader loader;

    protected int state;

    protected long lastModified;

    protected BundleActivator activator;

    protected double startupTime;

    public BundleImpl(OSGiAdapter osgi, BundleFile file, ClassLoader loader) {
        this(osgi, file, loader, false);
    }

    public BundleImpl(OSGiAdapter osgi, BundleFile file, ClassLoader loader,
            boolean isSystemBundle) {
        this.osgi = osgi;
        this.loader = loader;
        this.file = file;
        headers = BundleManifestReader.getHeaders(file.getManifest());
        symbolicName = headers.get(Constants.BUNDLE_SYMBOLICNAME);
        id = isSystemBundle ? 0 : osgi.getBundleId(symbolicName);
        context = createContext();
        state = UNINSTALLED;
    }

    public BundleFile getBundleFile() {
        return file;
    }

    protected final BundleContext createContext() {
        return new OSGiBundleContext(this);
    }

    public BundleContext getBundleContext() {
        return context;
    }

    public void start(int options) throws BundleException {
        // TODO Auto-generated method stub
    }

    public void stop(int options) throws BundleException {
        //TODO
    }

    public String getLocation() {
        return file.getLocation();
    }

    public URL getResource(String name) {
        return loader.getResource(name);
    }

    public Enumeration<URL> getResources(String name) throws IOException {
        return loader.getResources(name);
    }

    public Class<?> loadClass(String name) throws ClassNotFoundException {
        try {
            return loader.loadClass(name);
        } catch (NoClassDefFoundError e) {
            throw e;
        }
    }

    public URL getEntry(String name) {
        return file.getEntry(name);
    }

    public Enumeration<URL> findEntries(String path, String filePattern,
            boolean recurse) {
        return file.findEntries(path, filePattern, recurse);
    }

    public Enumeration<String> getEntryPaths(String path) {
        return file.getEntryPaths(path);
    }

    public long getBundleId() {
        return id;
    }

    public Dictionary<String, String> getHeaders() {
        return headers;
    }

    public Dictionary<String, String> getHeaders(String locale) {
        return headers; // TODO
    }

    public long getLastModified() {
        return lastModified;
    }

    public ServiceReference[] getRegisteredServices() {
        // RegistrationInfo ri =
        // (RegistrationInfo)di.context.get("RegistrationInfo");
        // TODO Auto-generated method stub
        return null;
    }

    public ServiceReference[] getServicesInUse() {
        // TODO Auto-generated method stub
        return null;
    }

    public int getState() {
        return state;
    }

    public String getSymbolicName() {
        return symbolicName;
    }

    public boolean hasPermission(Object permission) {
        return true; // TODO
    }

    public BundleActivator getActivator() throws IllegalAccessException,
            InstantiationException, ClassNotFoundException {
        if (activator == null) {
            String className = headers.get(Constants.BUNDLE_ACTIVATOR);
            if (className == null) {
                activator = NullActivator.INSTANCE;
            } else {
                try {
                    activator = (BundleActivator) loadClass(className).newInstance();
                } catch (ClassNotFoundException e) {
                    activator = NullActivator.INSTANCE;
                    throw e;
                }
            }
        }
        return activator;
    }

    public void start() throws BundleException {
        try {
            setStarting();
            getActivator().start(context);
            setStarted();
        } catch (Exception e) {
            throw new BundleException("Failed to start activator: "
                    + headers.get(Constants.BUNDLE_ACTIVATOR), e);
        }
    }

    public void stop() throws BundleException {
        try {
            setStopping();
            getActivator().stop(context);
            setStopped();
        } catch (Exception e) {
            throw new BundleException("Failed to stop activator: "
                    + headers.get(Constants.BUNDLE_ACTIVATOR), e);
        }
    }

    public void shutdown() throws BundleException {
        try {
            state = STOPPING;
            getActivator().stop(context);
            lastModified = System.currentTimeMillis();
            state = UNINSTALLED;
        } catch (Exception e) {
            throw new BundleException("Failed to stop activator: "
                    + headers.get(Constants.BUNDLE_ACTIVATOR), e);
        }
    }

    public void uninstall() throws BundleException {
        osgi.uninstall(this);
    }

    public void update() throws BundleException {
        lastModified = System.currentTimeMillis();
        throw new UnsupportedOperationException(
                "Bundle.update() operations was not yet implemented");
    }

    public void update(InputStream in) throws BundleException {
        lastModified = System.currentTimeMillis();
        throw new UnsupportedOperationException(
                "Bundle.update() operations was not yet implemented");
    }

    void setInstalled() {
        if (state == INSTALLED) {
            return;
        }
        lastModified = System.currentTimeMillis();
        state = INSTALLED;
        BundleEvent event = new BundleEvent(BundleEvent.INSTALLED, this);
        osgi.fireBundleEvent(event);
    }

    void setUninstalled() {
        if (state == UNINSTALLED) {
            return;
        }
        lastModified = System.currentTimeMillis();
        state = UNINSTALLED;
        BundleEvent event = new BundleEvent(BundleEvent.UNINSTALLED, this);
        osgi.fireBundleEvent(event);
    }

    void setResolved() {
        if (state == RESOLVED) {
            return;
        }
        state = RESOLVED;
        BundleEvent event = new BundleEvent(BundleEvent.RESOLVED, this);
        osgi.fireBundleEvent(event);
    }

    void setUnResolved() {
        state = INSTALLED;
        BundleEvent event = new BundleEvent(BundleEvent.UNRESOLVED, this);
        osgi.fireBundleEvent(event);
    }

    void setStarting() {
        if (state != RESOLVED) {
            return;
        }
        state = STARTING;
        BundleEvent event = new BundleEvent(BundleEvent.STARTING, this);
        osgi.fireBundleEvent(event);
    }

    void setStarted() {
        if (state != STARTING) {
            return;
        }
        state = ACTIVE;
        BundleEvent event = new BundleEvent(BundleEvent.STARTED, this);
        osgi.fireBundleEvent(event);
    }

    void setStopping() {
        if (state != ACTIVE) {
            return;
        }
        state = STOPPING;
        BundleEvent event = new BundleEvent(BundleEvent.STOPPING, this);
        osgi.fireBundleEvent(event);
    }

    void setStopped() {
        if (state != STOPPING) {
            return;
        }
        state = RESOLVED;
        BundleEvent event = new BundleEvent(BundleEvent.STOPPED, this);
        osgi.fireBundleEvent(event);
    }

    public double getStartupTime() {
        return startupTime;
    }

    @Override
    public int hashCode() {
        return symbolicName.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof Bundle) {
            return symbolicName.equals(((Bundle)obj).getSymbolicName());
        }
        return false;
    }

    @Override
    public String toString() {
        return symbolicName;
    }

}
