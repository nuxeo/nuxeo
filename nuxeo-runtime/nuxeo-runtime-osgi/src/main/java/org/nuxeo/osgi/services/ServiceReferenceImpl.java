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
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

import org.osgi.framework.Bundle;
import org.osgi.framework.ServiceReference;

/**
 * Dummy service reference. servicefactory not supported.
 *
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class ServiceReferenceImpl implements ServiceReference {

    protected Bundle bundle;
    protected Object service;
    protected Map<String,Object> props;

    public ServiceReferenceImpl(Bundle bundle, Object service) {
        this.bundle = bundle;
        this.service = service;
    }

    @Override
    public synchronized Object getProperty(String key) {
        return props != null ? props.get(key) : null;
    }

    @Override
    public synchronized String[] getPropertyKeys() {
        return props != null ? props.keySet().toArray(new String[props.size()]) : null;
    }

    @Override
    public Bundle getBundle() {
        return bundle;
    }

    @Override
    public Bundle[] getUsingBundles() {
        // not impl.
        return new Bundle[] {};
    }

    @Override
    public boolean isAssignableTo(Bundle bundle, String className) {
        try {
            return service.getClass() == bundle.loadClass(className);
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public int compareTo(Object reference) {
        throw new UnsupportedOperationException("Not implemented");
    }

    public Object getService() {
        return service;
    }

    public synchronized void setProperties(Dictionary<String,?> dict) {
        if (props == null) {
            props = new HashMap<String, Object>();
        }
        Enumeration<String> en = dict.keys();
        while (en.hasMoreElements()) {
            String key = en.nextElement();
            props.put(key, dict.get(key));
        }
    }
}
