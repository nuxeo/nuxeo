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
    public void start(BundleContext context) throws Exception {
        this.context = context;
        instance = this;
    }

    @Override
    public void stop(BundleContext context) throws Exception {
        instance = null;
        this.context = null;
    }

}
