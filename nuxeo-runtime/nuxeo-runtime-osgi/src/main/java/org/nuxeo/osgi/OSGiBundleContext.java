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
 *
 * $Id$
 */

package org.nuxeo.osgi;

import java.io.File;
import java.io.InputStream;
import java.util.Collection;
import java.util.Dictionary;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.framework.BundleListener;
import org.osgi.framework.Filter;
import org.osgi.framework.FrameworkListener;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceListener;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class OSGiBundleContext implements BundleContext {

    protected final OSGiSystemContext osgi;

    protected final OSGiBundleHost bundle;

    protected final OSGiBundle fragments[];

    protected OSGiLoader loader;

    protected BundleActivator activator;

    protected OSGiBundleContext(OSGiSystemBundle system) {
        osgi = (OSGiSystemContext) this;
        bundle = system;
        fragments = new OSGiBundle[0]; // ToDo add system fragment
    }

    public OSGiBundleContext(OSGiSystemContext osgi, OSGiBundleHost bundle)
            throws BundleException {
        this.osgi = osgi;
        this.bundle = bundle;
        fragments = osgi.registry.getFragments(bundle.symbolicName);
    }

    @Override
    public void addBundleListener(BundleListener listener) {
        bundle.osgi.bundleListeners.add(listener);
    }

    @Override
    public void addFrameworkListener(FrameworkListener listener) {
        bundle.osgi.frameworkListeners.add(listener);
    }

    @Override
    public void addServiceListener(ServiceListener listener) {
        bundle.osgi.serviceListeners.add(listener);
    }

    @Override
    public void addServiceListener(ServiceListener listener, String filter)
            throws InvalidSyntaxException {
        throw new UnsupportedOperationException();
    }

    @Override
    public Filter createFilter(String filter) throws InvalidSyntaxException {
        throw new UnsupportedOperationException(
                "BundleContext.createFilter() was not yet implemented");
    }

    @Override
    public ServiceReference<?>[] getAllServiceReferences(String clazz,
            String filter) throws InvalidSyntaxException {
        throw new UnsupportedOperationException();
    }

    @Override
    public <S> Collection<ServiceReference<S>> getServiceReferences(
            Class<S> clazz, String filter) throws InvalidSyntaxException {
        throw new UnsupportedOperationException();
    }

    @Override
    public Bundle getBundle() {
        return bundle;
    }

    @Override
    public Bundle getBundle(long id) {
        return bundle.osgi.registry.getBundle(id);
    }

    @Override
    public Bundle[] getBundles() {
        return bundle.osgi.registry.getInstalledBundles();
    }

    @Override
    public File getDataFile(String filename) {
        return new File(bundle.osgi.dataDir, filename);
    }

    @Override
    public String getProperty(String key) {
        return bundle.osgi.getProperty(key);
    }

    protected String getProperty(String key, String defaultValue) {
        return bundle.osgi.getProperty(key, defaultValue);
    }

    @Override
    public <S> S getService(ServiceReference<S> reference) {
        return ((OSGiServiceReference<S>) reference).getService();
    }

    @Override
    public ServiceReference<?> getServiceReference(String clazz) {
        ServiceRegistration<?> reg = bundle.osgi.services.get(clazz);
        return reg != null ? reg.getReference() : null;
    }

    @SuppressWarnings("unchecked")
    @Override
    public <S> ServiceReference<S> getServiceReference(Class<S> clazz) {
        return (ServiceReference<S>) getServiceReference(clazz.getName());
    }

    @Override
    public ServiceReference<?>[] getServiceReferences(String clazz,
            String filter) throws InvalidSyntaxException {
        ServiceRegistration<?> reg = bundle.osgi.services.get(clazz);
        return reg != null ? new ServiceReference[] { reg.getReference() }
                : null;
    }

    @Override
    public Bundle installBundle(String location) throws BundleException {
        return bundle.osgi.installBundle(location);
    }

    @Override
    public Bundle installBundle(String location, InputStream input)
            throws BundleException {
        return bundle.osgi.installBundle(location, input);
    }

    @Override
    public ServiceRegistration<?> registerService(String[] clazzes,
            Object service, Dictionary<String, ?> properties) {
        return bundle.osgi.registerService(clazzes, service, properties);
    }

    @Override
    public ServiceRegistration<?> registerService(String clazz, Object service,
            Dictionary<String, ?> properties) {
        return bundle.osgi.registerService(clazz, service, properties);
    }

    @Override
    public <S> ServiceRegistration<S> registerService(Class<S> clazz,
            S service, Dictionary<String, ?> properties) {
        return bundle.osgi.registerService(clazz, service, properties);
    }

    @Override
    public void removeBundleListener(BundleListener listener) {
        bundle.osgi.removeBundleListener(listener);
    }

    @Override
    public void removeFrameworkListener(FrameworkListener listener) {
        bundle.osgi.removeFrameworkListener(listener);
    }

    @Override
    public void removeServiceListener(ServiceListener listener) {
        bundle.osgi.removeServiceListener(listener);
    }

    @Override
    public boolean ungetService(ServiceReference<?> reference) {
        return bundle.osgi.ungetService(reference);
    }

    @Override
    public Bundle getBundle(String location) {
        return bundle.osgi.getBundle(location);
    }

    protected BundleActivator loadActivator() throws BundleException {
        String className = bundle.getActivatorClassName();
        if (className == null) {
            return OSGiNullActivator.INSTANCE;
        }
        try {
            Class<?> clazz = loader.loadClass(className);
            return (BundleActivator) clazz.newInstance();
        } catch (ClassNotFoundException e) {
            throw new BundleException("Activator not found: " + className, e);
        } catch (InstantiationException e) {
            throw new BundleException("Activator not instantiable: "
                    + className, e);
        } catch (IllegalAccessException e) {
            throw new BundleException("Activator not accessible: " + className,
                    e);
        }

    }

    public void start() throws BundleException {
        loader = osgi.factory.newLoader(this);
        loader.wire();
    }

    protected void activate() throws BundleException {
        try {
            activator = loadActivator();
            activator.start(this);
        } catch (InterruptedException e) {
            // restore interrupted status
            Thread.currentThread().interrupt();
            throw new RuntimeException(e);
        } catch (Exception e) { // InterruptedException caught above
            throw new BundleException("Failed to start bundle at: " + bundle
                    + " with activator: " + activator.getClass().getName(), e);
        }
    }

    protected void stop() throws BundleException {
        try {
            activator.stop(this);
        } catch (InterruptedException e) {
            // restore interrupted status
            Thread.currentThread().interrupt();
            throw new RuntimeException(e);
        } catch (Exception e) { // InterruptedException caught above
            throw new BundleException("Failed to stop bundle at: " + bundle
                    + " with activator: " + activator.getClass().getName(), e);
        } finally {
            activator = null;
        }
    }

    @Override
    public String toString() {
        return "OSGiBundleContext [bundle=" + bundle + "]";
    }

}
