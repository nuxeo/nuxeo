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

import org.osgi.framework.Bundle;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;

/**
 * Dummy service registration impl.
 *
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class OSGIServiceRegistration<T> implements ServiceRegistration<T> {

    protected OSGiSystemContext osgi;
    protected String[] classes;
    protected OSGiServiceReference<T> ref;

    public OSGIServiceRegistration(OSGiSystemContext osgi, Bundle bundle, String[] classes, Object service) {
        this.osgi = osgi;
        this.classes = classes;
        ref = new OSGiServiceReference<T>(bundle, service);
    }

    @Override
    public ServiceReference<T> getReference() {
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
            osgi.services.remove(c);
        }
    }

}
