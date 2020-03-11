/*
 * (C) Copyright 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     bstefanescu
 */
package org.nuxeo.osgi.services;

import java.util.Dictionary;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

import org.osgi.framework.Bundle;
import org.osgi.framework.ServiceFactory;
import org.osgi.framework.ServiceReference;

/**
 * Dummy service reference. servicefactory not supported.
 *
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
public class ServiceReferenceImpl implements ServiceReference {

    protected Bundle bundle;

    protected Object service;

    protected ServiceFactory factory;

    protected Map<String, Object> props;

    public ServiceReferenceImpl(Bundle bundle, Object service) {
        this.bundle = bundle;
        if (service instanceof ServiceFactory) {
            factory = (ServiceFactory) service;
        } else {
            this.service = service;
        }
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
        if (service == null) {
            return true;
        }
        try {
            return service.getClass() == bundle.loadClass(className);
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    @Override
    public int compareTo(Object reference) {
        throw new UnsupportedOperationException("Not implemented");
    }

    public Object getService() {
        return service == null ? factory.getService(bundle, null) : service;
    }

    public synchronized void setProperties(Dictionary<String, ?> dict) {
        if (props == null) {
            props = new HashMap<>();
        }
        Enumeration<String> en = dict.keys();
        while (en.hasMoreElements()) {
            String key = en.nextElement();
            props.put(key, dict.get(key));
        }
    }
}
