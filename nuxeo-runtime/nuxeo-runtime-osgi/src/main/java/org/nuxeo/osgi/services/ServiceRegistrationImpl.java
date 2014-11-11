/*
 * (C) Copyright 2006-2010 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
package org.nuxeo.osgi.services;

import java.util.Dictionary;

import org.nuxeo.osgi.OSGiAdapter;
import org.osgi.framework.Bundle;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;

/**
 * Dummy service registration impl.
 *
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class ServiceRegistrationImpl implements ServiceRegistration {

    protected OSGiAdapter osgi;
    protected String[] classes;
    protected ServiceReferenceImpl ref;

    public ServiceRegistrationImpl(OSGiAdapter osgi, Bundle bundle, String[] classes, Object service) {
        this.osgi = osgi;
        this.classes = classes;
        this.ref = new ServiceReferenceImpl(bundle, service);
    }

    @Override
    public ServiceReference getReference() {
        return ref;
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    @Override
    public void setProperties(Dictionary properties) {
        ref.setProperties(properties);
    }

    @Override
    public void unregister() {
        for (String c : classes) {
            osgi.removeService(c);
        }
    }

}
