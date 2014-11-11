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
 *     bstefanescu
 */
package org.nuxeo.osgi;

import java.util.Dictionary;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Set;

import org.osgi.framework.Bundle;
import org.osgi.framework.ServiceFactory;
import org.osgi.framework.ServiceReference;

/**
 * Dummy service reference. servicefactory not supported.
 *
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class OSGiServiceReference<S> implements ServiceReference<S> {

    protected final Bundle bundle;
    protected S service;
    protected ServiceFactory<S> factory;
    protected final Hashtable<String, Object> dict = new Hashtable<String,Object>();

    @SuppressWarnings("unchecked")
	public OSGiServiceReference(Bundle bundle, Object service) {
        this.bundle = bundle;
        if (service instanceof ServiceFactory) {
            factory = (ServiceFactory<S>)service;
        } else {
            this.service = (S)service;
        }
    }

    @Override
    public synchronized Object getProperty(String key) {
        return dict.get(key);
    }

    @Override
    public synchronized String[] getPropertyKeys() {
        Set<String> keySet = dict.keySet();
        return keySet.toArray(new String[keySet.size()]);
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

    public S getService() {
        return service == null ? factory.getService(bundle, null) : service;
    }

    public synchronized void setProperties(Dictionary<String,?> dict) {
        Enumeration<String> en = dict.keys();
        while (en.hasMoreElements()) {
            String key = en.nextElement();
            this.dict.put(key, dict.get(key));
        }
    }
}
