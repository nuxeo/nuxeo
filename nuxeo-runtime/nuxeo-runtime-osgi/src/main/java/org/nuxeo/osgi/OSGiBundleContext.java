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
import java.io.InputStream;
import java.util.Dictionary;

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

    public void addBundleListener(BundleListener listener) {
        bundle.osgi.addBundleListener(listener);
    }

    public void addFrameworkListener(FrameworkListener listener) {
        bundle.osgi.addFrameworkListener(listener);
    }

    public void addServiceListener(ServiceListener listener) {
        bundle.osgi.addServiceListener(listener);
    }

    public void addServiceListener(ServiceListener listener, String filter)
            throws InvalidSyntaxException {
        bundle.osgi.addServiceListener(listener, filter);
    }

    public Filter createFilter(String filter) throws InvalidSyntaxException {
        throw new UnsupportedOperationException(
                "BundleContext.createFilter() was not yet implemented");
    }

    public ServiceReference[] getAllServiceReferences(String clazz,
            String filter) throws InvalidSyntaxException {
        // TODO Auto-generated method stub
        return null;
    }

    public Bundle getBundle() {
        return bundle;
    }

    public Bundle getBundle(long id) {
        return bundle.osgi.registry.getBundle(id);
    }

    public Bundle[] getBundles() {
        return bundle.osgi.registry.getInstalledBundles();
    }

    public File getDataFile(String filename) {
        return new File(bundle.osgi.getWorkingDir(), filename);
    }

    public String getProperty(String key) {
        return bundle.osgi.getProperty(key);
    }

    public Object getService(ServiceReference reference) {
        // TODO Auto-generated method stub
        return null;
    }

    public ServiceReference getServiceReference(String clazz) {
        // TODO Auto-generated method stub
        return null;
    }

    public ServiceReference[] getServiceReferences(String clazz, String filter)
            throws InvalidSyntaxException {
        // TODO Auto-generated method stub
        return null;
    }

    public Bundle installBundle(String location) throws BundleException {
        // TODO Auto-generated method stub
        return null;
    }

    public Bundle installBundle(String location, InputStream input)
            throws BundleException {
        // TODO Auto-generated method stub
        return null;
    }

    public ServiceRegistration registerService(String[] clazzes,
            Object service, Dictionary properties) {
        // TODO Auto-generated method stub
        return null;
    }

    public ServiceRegistration registerService(String clazz, Object service,
            Dictionary properties) {
        // TODO Auto-generated method stub
        return null;
    }

    public void removeBundleListener(BundleListener listener) {
        bundle.osgi.removeBundleListener(listener);
    }

    public void removeFrameworkListener(FrameworkListener listener) {
        bundle.osgi.removeFrameworkListener(listener);
    }

    public void removeServiceListener(ServiceListener listener) {
        bundle.osgi.removeServiceListener(listener);
    }

    public boolean ungetService(ServiceReference reference) {
        // TODO Auto-generated method stub
        return false;
    }

}
