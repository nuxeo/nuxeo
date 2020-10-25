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
package org.nuxeo.ecm.core.storage.sql;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

/**
 * Needed to lookup local bundle resources - which should use Bundle API.
 *
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
public class Activator implements BundleActivator {

    private static volatile Activator instance;

    private BundleContext context;

    public static URL getResource(String path) {
        Activator a = instance;
        if (a != null) {
            return a.context.getBundle().getResource(path);
        }
        return Thread.currentThread().getContextClassLoader().getResource(path);
    }

    public static InputStream getResourceAsStream(String path) throws IOException {
        URL url = getResource(path);
        return url != null ? url.openStream() : null;
    }

    public static Activator getInstance() {
        return instance;
    }

    public BundleContext getContext() {
        return context;
    }

    @Override
    public void start(BundleContext context) {
        this.context = context;
        instance = this; // NOSONAR OSGi singleton
    }

    @Override
    public void stop(BundleContext context) {
        instance = null; // NOSONAR OSGi singleton
        this.context = null;
    }

}
