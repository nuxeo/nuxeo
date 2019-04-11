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
package org.nuxeo.ecm.webengine.jaxrs.servlet;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

import org.nuxeo.ecm.webengine.jaxrs.Activator;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;

import com.sun.jersey.api.uri.UriBuilderImpl;
import com.sun.jersey.server.impl.provider.RuntimeDelegateImpl;

/**
 * Support for jersey ServiceFinder lookups in an OSGi environment.
 *
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 * @deprecated This class is deprecated since jersey 1.11 which fixed OSGi class loader problems.
 */
@Deprecated
public class ServiceClassLoader extends ClassLoader {

    public static ClassLoader getLoader() {
        BundleContext ctx = Activator.getInstance().getContext();
        String vendor = ctx.getProperty(Constants.FRAMEWORK_VENDOR);
        if (vendor != null && vendor.contains("Nuxeo")) {
            // support fake nuxeo osgi adapter
            return Activator.class.getClassLoader();
        } else {
            ServiceClassLoader loader = new ServiceClassLoader(ctx.getBundle());
            loader.addResourceLoader(RuntimeDelegateImpl.class.getClassLoader());
            loader.addResourceLoader(UriBuilderImpl.class.getClassLoader());
            return loader;
        }
    }

    protected Bundle bundle;

    protected List<ClassLoader> loaders;

    public ServiceClassLoader(Bundle bundle) {
        this.bundle = bundle;
        this.loaders = new ArrayList<>();
    }

    public void addResourceLoader(ClassLoader cl) {
        loaders.add(cl);
    }

    @Override
    protected URL findResource(String name) {
        URL url = null;
        for (ClassLoader cl : loaders) {
            url = cl.getResource(name);
            if (url != null) {
                break;
            }
        }
        return url;
    }

    @Override
    protected Enumeration<URL> findResources(String name) throws IOException {
        ArrayList<Enumeration<URL>> enums = new ArrayList<>();
        for (ClassLoader cl : loaders) {
            enums.add(cl.getResources(name));
        }
        return new CompoundEnumeration<>(enums.toArray(new Enumeration[enums.size()]));
    }

    @Override
    protected synchronized Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
        return bundle.loadClass(name);
    }
}
