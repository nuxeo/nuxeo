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

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Dictionary;

import org.nuxeo.osgi.services.ServiceReferenceImpl;
import org.nuxeo.osgi.services.ServiceRegistrationImpl;
import org.osgi.framework.Bundle;
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
 * @author  <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class OSGiBundleContext implements BundleContext {

    final BundleImpl bundle;

    public OSGiBundleContext(BundleImpl bundle) {
        this.bundle = bundle;
    }

    public OSGiAdapter getOSGiAdapter() {
        return bundle.osgi;
    }

    @Override
    public void addBundleListener(BundleListener listener) {
        bundle.osgi.addBundleListener(listener);
    }

    @Override
    public void addFrameworkListener(FrameworkListener listener) {
        bundle.osgi.addFrameworkListener(listener);
    }

    @Override
    public void addServiceListener(ServiceListener listener) {
        bundle.osgi.addServiceListener(listener);
    }

    @Override
    public void addServiceListener(ServiceListener listener, String filter)
            throws InvalidSyntaxException {
        bundle.osgi.addServiceListener(listener, filter);
    }

    @Override
    public Filter createFilter(String filter) throws InvalidSyntaxException {
        throw new UnsupportedOperationException(
                "BundleContext.createFilter() was not yet implemented");
    }

    @Override
    public ServiceReference[] getAllServiceReferences(String clazz,
            String filter) throws InvalidSyntaxException {
        // TODO Auto-generated method stub
        return null;
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
        return new File(bundle.osgi.getWorkingDir(), filename);
    }

    @Override
    public String getProperty(String key) {
        return bundle.osgi.getProperty(key);
    }

    @Override
    public Object getService(ServiceReference reference) {
        return ((ServiceReferenceImpl)reference).getService();
    }

    @Override
    public ServiceReference getServiceReference(String clazz) {
        ServiceRegistration reg = bundle.osgi.services.get(clazz);
        return reg != null ? reg.getReference() : null;
    }

    @Override
    public ServiceReference[] getServiceReferences(String clazz, String filter)
            throws InvalidSyntaxException {
        ServiceRegistration reg = bundle.osgi.services.get(clazz);
        return reg != null ? new ServiceReference[] {reg.getReference()} : null;
    }

    @Override
    public Bundle installBundle(String location) throws BundleException {
        File file = new File(location);
        try {
            BundleFile bf = file.isDirectory() ? new DirectoryBundleFile(file) : new JarBundleFile(file);
            BundleImpl b = new BundleImpl(bundle.osgi, bf, bundle.loader);
            if (b.getSymbolicName() != null) {
                bundle.osgi.install(b);
            }
            return b;
        } catch (IOException e) {
            throw new BundleException("Failed to install bundle at "+location, e);
        }
    }

    @Override
    public Bundle installBundle(String location, InputStream input)
            throws BundleException {
        // TODO Auto-generated method stub
        return null;
    }

    @SuppressWarnings("rawtypes")
    @Override
    public ServiceRegistration registerService(String[] clazzes,
            Object service, Dictionary properties) {
        ServiceRegistrationImpl reg = new ServiceRegistrationImpl(bundle.osgi, bundle, clazzes, service);
        if (properties != null) {
            reg.setProperties(properties);
            return reg;
        }
        for (String c : clazzes) {
            bundle.osgi.services.put(c, reg);
        }
        return reg;
    }

    @SuppressWarnings("rawtypes")
    @Override
    public ServiceRegistration registerService(String clazz, Object service,
            Dictionary properties) {
        return registerService(new String[] {clazz}, service, properties);
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
    public boolean ungetService(ServiceReference reference) {
        // not impl.
        return false;
    }

}
