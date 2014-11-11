/*
 * (C) Copyright 2006-2014 Nuxeo SA (http://nuxeo.com/) and contributors.
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

package org.nuxeo.osgi;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.Map;
import java.util.jar.Manifest;

import org.nuxeo.osgi.util.CompoundEnumeration;
import org.nuxeo.runtime.api.Framework;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleEvent;
import org.osgi.framework.BundleException;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.Version;
import org.osgi.service.packageadmin.PackageAdmin;

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

    protected boolean allowHostOverride;

    public BundleImpl(OSGiAdapter osgi, BundleFile file, ClassLoader loader)
            throws BundleException {
        this(osgi, file, loader, false);
    }

    public BundleImpl(OSGiAdapter osgi, BundleFile file, ClassLoader loader,
            boolean isSystemBundle) throws BundleException {
        this.osgi = osgi;
        this.loader = loader;
        this.file = file;
        Manifest mf = file.getManifest();
        if (mf == null) {
            headers = null;
            symbolicName = null;
            id = -1;
            context = null;
            return;
        }
        try {
            headers = BundleManifestReader.getHeaders(mf);
        } catch (BundleException e) {
            throw new BundleException("Invalid OSGi Manifest in file " + file
                    + " : " + e.getMessage(), e);
        }
        symbolicName = headers.get(Constants.BUNDLE_SYMBOLICNAME);
        allowHostOverride = Boolean.parseBoolean(headers.get(BundleManifestReader.ALLOW_HOST_OVERRIDE));
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

    @Override
    public BundleContext getBundleContext() {
        // ensure BundleContext is not visible in RESOLVED state - to ensure
        // OSGi compat. - in our component activate method.
        // TODO NXP-6035: disable for now the check until a better compatibility
        // mode is implemented.
        // if (state == RESOLVED) {
        // throw new IllegalStateException(
        // "You cannot use a BundleContext when in RESOLVED state. Do not use this in your component activate method!");
        // }
        return context;
    }

    @Override
    public void start(int options) throws BundleException {
        // TODO Auto-generated method stub
    }

    @Override
    public void stop(int options) throws BundleException {
        // TODO
    }

    @Override
    public String getLocation() {
        return file.getLocation();
    }

    @Override
    public URL getResource(String name) {
        return loader.getResource(name);
    }

    @Override
    public Enumeration<URL> getResources(String name) throws IOException {
        return loader.getResources(name);
    }

    @Override
    public Class<?> loadClass(String name) throws ClassNotFoundException {
        try {
            return loader.loadClass(name);
        } catch (NoClassDefFoundError e) {
            throw e;
        }
    }

    @Override
    public URL getEntry(String name) {
        return file.getEntry(name);
    }

    public static PackageAdmin getPackageAdmin() {
        BundleContext sysctx = Framework.getRuntime().getContext().getBundle().getBundleContext();
        ServiceReference ref = sysctx.getServiceReference(PackageAdmin.class.getName());
        return (PackageAdmin) sysctx.getService(ref);
    }

    protected static class CompoundEnumerationBuilder {

        protected final ArrayList<Enumeration<URL>> collected = new ArrayList<>();

        public CompoundEnumerationBuilder add(Enumeration<URL> e) {
            collected.add(e);
            return this;
        }

        public Enumeration<URL> build() {
            return new CompoundEnumeration<>(
                    collected.toArray(new Enumeration[collected.size()]));
        }

    }

    @Override
    public Enumeration<URL> findEntries(String path, String filePattern,
            boolean recurse) {
        Enumeration<URL> hostEntries = file.findEntries(path, filePattern,
                recurse);
        Bundle[] fragments = osgi.getRegistry().getFragments(symbolicName);
        if (fragments.length == 0) {
            return hostEntries;
        }

        CompoundEnumerationBuilder builder = new CompoundEnumerationBuilder();
        if (!allowHostOverride) {
            builder.add(hostEntries);
        }

        for (Bundle fragment : fragments) {
            Enumeration<URL> fragmentEntries = fragment.findEntries(path,
                    filePattern, recurse);
            builder.add(fragmentEntries);
        }

        if (allowHostOverride) {
            builder.add(hostEntries);
        }

        return builder.build();
    }

    @Override
    public Enumeration<String> getEntryPaths(String path) {
        return file.getEntryPaths(path);
    }

    @Override
    public long getBundleId() {
        return id;
    }

    @Override
    public Dictionary<String, String> getHeaders() {
        return headers;
    }

    @Override
    public Dictionary<String, String> getHeaders(String locale) {
        return headers; // TODO
    }

    @Override
    public long getLastModified() {
        return lastModified;
    }

    @Override
    public ServiceReference[] getRegisteredServices() {
        // RegistrationInfo ri =
        // (RegistrationInfo)di.context.get("RegistrationInfo");
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public ServiceReference[] getServicesInUse() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public int getState() {
        return state;
    }

    @Override
    public String getSymbolicName() {
        return symbolicName;
    }

    @Override
    public boolean hasPermission(Object permission) {
        return true; // TODO
    }

    protected String getActivatorClassName() {
        return headers == null ? null : headers.get(Constants.BUNDLE_ACTIVATOR);
    }

    public BundleActivator getActivator() throws BundleException {
        if (activator == null) {
            activator = NullActivator.INSTANCE;
            String className = getActivatorClassName();
            if (className == null) {
                return activator;
            }
            try {
                activator = (BundleActivator) loadClass(className).newInstance();
            } catch (ClassNotFoundException e) {
                throw new BundleException("Activator not found: " + className,
                        e);
            } catch (InstantiationException e) {
                throw new BundleException("Activator not instantiable: "
                        + className, e);
            } catch (IllegalAccessException e) {
                throw new BundleException("Activator not accessible: "
                        + className, e);
            }
        }
        return activator;
    }

    @Override
    public void start() throws BundleException {
        try {
            setStarting();
            getActivator().start(context); // stupid API throws Exception
            setStarted();
        } catch (InterruptedException e) {
            // restore interrupted status
            Thread.currentThread().interrupt();
            throw new RuntimeException(e);
        } catch (Exception e) { // InterruptedException caught above
            throw new BundleException("Failed to start bundle at: " + file
                    + " with activator: " + getActivatorClassName(), e);
        }
    }

    @Override
    public void stop() throws BundleException {
        try {
            setStopping();
            getActivator().stop(context); // stupid API throws Exception
            setStopped();
        } catch (InterruptedException e) {
            // restore interrupted status
            Thread.currentThread().interrupt();
            throw new RuntimeException(e);
        } catch (Exception e) { // InterruptedException caught above
            throw new BundleException("Failed to stop activator: "
                    + getActivatorClassName(), e);
        }
    }

    public void shutdown() throws BundleException {
        try {
            state = STOPPING;
            getActivator().stop(context); // stupid API throws Exception
            lastModified = System.currentTimeMillis();
            state = UNINSTALLED;
        } catch (InterruptedException e) {
            // restore interrupted status
            Thread.currentThread().interrupt();
            throw new RuntimeException(e);
        } catch (Exception e) { // InterruptedException caught above
            throw new BundleException("Failed to stop activator: "
                    + getActivatorClassName(), e);
        }
    }

    @Override
    public void uninstall() throws BundleException {
        osgi.uninstall(this);
        try {
            file.close(osgi);
        } catch (IOException e) {
            throw new BundleException("Cannot close underlying file resources "
                    + symbolicName, e);
        }
    }

    @Override
    public void update() throws BundleException {
        lastModified = System.currentTimeMillis();
        throw new UnsupportedOperationException(
                "Bundle.update() operations was not yet implemented");
    }

    @Override
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
            return symbolicName.equals(((Bundle) obj).getSymbolicName());
        }
        return false;
    }

    @Override
    public String toString() {
        return symbolicName;
    }

    @Override
    public Map getSignerCertificates(int signersType) {
        throw new UnsupportedOperationException("not yet implemented");
    }

    @Override
    public Version getVersion() {
        return Version.parseVersion(headers.get(Constants.BUNDLE_VERSION));
    }

}
