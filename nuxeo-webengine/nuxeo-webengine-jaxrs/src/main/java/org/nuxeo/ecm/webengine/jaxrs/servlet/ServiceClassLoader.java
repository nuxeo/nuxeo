/* 
 * Copyright (c) 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
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

import sun.misc.CompoundEnumeration;

import com.sun.jersey.api.uri.UriBuilderImpl;
import com.sun.jersey.server.impl.provider.RuntimeDelegateImpl;

/**
 * Support for jersey ServiceFinder lookups in an OSGi environment.
 *
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
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
        this.loaders = new ArrayList<ClassLoader>();
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
        ArrayList<Enumeration<URL>> enums = new ArrayList<Enumeration<URL>>();
        for (ClassLoader cl : loaders) {
            enums.add(cl.getResources(name));
        }
        return new CompoundEnumeration<URL>(enums.toArray(new Enumeration[enums.size()]));
    }

    @Override
    protected synchronized Class<?> loadClass(String name, boolean resolve)
            throws ClassNotFoundException {
        return bundle.loadClass(name);
    }
}
