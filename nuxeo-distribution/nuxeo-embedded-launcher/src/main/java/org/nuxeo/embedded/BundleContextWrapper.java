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
 *     bstefanescu
 */
package org.nuxeo.embedded;

import java.io.File;
import java.io.InputStream;
import java.util.Dictionary;
import java.util.List;
import java.util.Vector;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.framework.BundleListener;
import org.osgi.framework.Filter;
import org.osgi.framework.FrameworkEvent;
import org.osgi.framework.FrameworkListener;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceListener;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;

/**
 * This class exists only to fix an error in nuxeo framework start event handling.
 * When a real OSGI sends the framework started event nuxeo is not necessarily started yet.
 * This should be fixed in next versions of nuxeo by using a custom event.
 * 
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class BundleContextWrapper implements BundleContext {

    protected BundleContext delegate;
    protected List<FrameworkListener> listeners;
    
    public BundleContextWrapper(BundleContext context) {
        delegate = context;
        listeners = new Vector<FrameworkListener>();
    }
    
    public BundleContext getDelegate() {
        return delegate;
    }
    
    public void sendEvent(FrameworkEvent event) {
        for (FrameworkListener listener : listeners.toArray(new FrameworkListener[listeners.size()])) {
            listener.frameworkEvent(event);
        }
    }

    /**
     * @param listener
     * @see org.osgi.framework.BundleContext#addBundleListener(org.osgi.framework.BundleListener)
     */
    public void addBundleListener(BundleListener listener) {
        delegate.addBundleListener(listener);
    }

    /**
     * @param listener
     * @see org.osgi.framework.BundleContext#addFrameworkListener(org.osgi.framework.FrameworkListener)
     */
    public void addFrameworkListener(FrameworkListener listener) {
        delegate.addFrameworkListener(listener);
        listeners.add(listener);
    }

    /**
     * @param listener
     * @param filter
     * @throws InvalidSyntaxException
     * @see org.osgi.framework.BundleContext#addServiceListener(org.osgi.framework.ServiceListener, java.lang.String)
     */
    public void addServiceListener(ServiceListener listener, String filter)
            throws InvalidSyntaxException {
        delegate.addServiceListener(listener, filter);
    }

    /**
     * @param listener
     * @see org.osgi.framework.BundleContext#addServiceListener(org.osgi.framework.ServiceListener)
     */
    public void addServiceListener(ServiceListener listener) {
        delegate.addServiceListener(listener);
    }

    /**
     * @param filter
     * @return
     * @throws InvalidSyntaxException
     * @see org.osgi.framework.BundleContext#createFilter(java.lang.String)
     */
    public Filter createFilter(String filter) throws InvalidSyntaxException {
        return delegate.createFilter(filter);
    }

    /**
     * @param clazz
     * @param filter
     * @return
     * @throws InvalidSyntaxException
     * @see org.osgi.framework.BundleContext#getAllServiceReferences(java.lang.String, java.lang.String)
     */
    public ServiceReference[] getAllServiceReferences(String clazz,
            String filter) throws InvalidSyntaxException {
        return delegate.getAllServiceReferences(clazz, filter);
    }

    /**
     * @return
     * @see org.osgi.framework.BundleContext#getBundle()
     */
    public Bundle getBundle() {
        return delegate.getBundle();
    }

    /**
     * @param id
     * @return
     * @see org.osgi.framework.BundleContext#getBundle(long)
     */
    public Bundle getBundle(long id) {
        return delegate.getBundle(id);
    }

    /**
     * @return
     * @see org.osgi.framework.BundleContext#getBundles()
     */
    public Bundle[] getBundles() {
        return delegate.getBundles();
    }

    /**
     * @param filename
     * @return
     * @see org.osgi.framework.BundleContext#getDataFile(java.lang.String)
     */
    public File getDataFile(String filename) {
        return delegate.getDataFile(filename);
    }

    /**
     * @param key
     * @return
     * @see org.osgi.framework.BundleContext#getProperty(java.lang.String)
     */
    public String getProperty(String key) {
        return delegate.getProperty(key);
    }

    /**
     * @param reference
     * @return
     * @see org.osgi.framework.BundleContext#getService(org.osgi.framework.ServiceReference)
     */
    public Object getService(ServiceReference reference) {
        return delegate.getService(reference);
    }

    /**
     * @param clazz
     * @return
     * @see org.osgi.framework.BundleContext#getServiceReference(java.lang.String)
     */
    public ServiceReference getServiceReference(String clazz) {
        return delegate.getServiceReference(clazz);
    }

    /**
     * @param clazz
     * @param filter
     * @return
     * @throws InvalidSyntaxException
     * @see org.osgi.framework.BundleContext#getServiceReferences(java.lang.String, java.lang.String)
     */
    public ServiceReference[] getServiceReferences(String clazz, String filter)
            throws InvalidSyntaxException {
        return delegate.getServiceReferences(clazz, filter);
    }

    /**
     * @param location
     * @param input
     * @return
     * @throws BundleException
     * @see org.osgi.framework.BundleContext#installBundle(java.lang.String, java.io.InputStream)
     */
    public Bundle installBundle(String location, InputStream input)
            throws BundleException {
        return delegate.installBundle(location, input);
    }

    /**
     * @param location
     * @return
     * @throws BundleException
     * @see org.osgi.framework.BundleContext#installBundle(java.lang.String)
     */
    public Bundle installBundle(String location) throws BundleException {
        return delegate.installBundle(location);
    }

    /**
     * @param clazz
     * @param service
     * @param properties
     * @return
     * @see org.osgi.framework.BundleContext#registerService(java.lang.String, java.lang.Object, java.util.Dictionary)
     */
    public ServiceRegistration registerService(String clazz, Object service,
            Dictionary properties) {
        return delegate.registerService(clazz, service, properties);
    }

    /**
     * @param clazzes
     * @param service
     * @param properties
     * @return
     * @see org.osgi.framework.BundleContext#registerService(java.lang.String[], java.lang.Object, java.util.Dictionary)
     */
    public ServiceRegistration registerService(String[] clazzes,
            Object service, Dictionary properties) {
        return delegate.registerService(clazzes, service, properties);
    }

    /**
     * @param listener
     * @see org.osgi.framework.BundleContext#removeBundleListener(org.osgi.framework.BundleListener)
     */
    public void removeBundleListener(BundleListener listener) {
        delegate.removeBundleListener(listener);
    }

    /**
     * @param listener
     * @see org.osgi.framework.BundleContext#removeFrameworkListener(org.osgi.framework.FrameworkListener)
     */
    public void removeFrameworkListener(FrameworkListener listener) {
        delegate.removeFrameworkListener(listener);
    }

    /**
     * @param listener
     * @see org.osgi.framework.BundleContext#removeServiceListener(org.osgi.framework.ServiceListener)
     */
    public void removeServiceListener(ServiceListener listener) {
        delegate.removeServiceListener(listener);
    }

    /**
     * @param reference
     * @return
     * @see org.osgi.framework.BundleContext#ungetService(org.osgi.framework.ServiceReference)
     */
    public boolean ungetService(ServiceReference reference) {
        return delegate.ungetService(reference);
    }

}
