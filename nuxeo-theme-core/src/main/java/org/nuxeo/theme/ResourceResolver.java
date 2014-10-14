/*
 * Copyright (c) 2011 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Florent Guillaume
 */
package org.nuxeo.theme;

import java.io.InputStream;
import java.net.URL;

/**
 * Resolver for resources.
 * <p>
 * This default implementation uses the thread context ClassLoader.
 *
 * @since 5.5
 */
public class ResourceResolver {

    private static final ResourceResolver DEFAULT = new ResourceResolver();

    private static ResourceResolver instance = DEFAULT;


    /**
     * Gets the current resolver (thread local).
     */
    public static ResourceResolver getInstance() {
        return instance;
    }

    /**
     * Called by the framework to set the current resolver or clear it.
     */
    public static void setInstance(ResourceResolver resolver) {
        instance = resolver == null ? DEFAULT : resolver;
    }

    /**
     * Gets a resource URL at the given path.
     *
     * @param path the path, which must not start with a /
     * @see javax.servlet.ServletContext#getResource
     * @see java.lang.ClassLoader#getResource
     */
    public URL getResource(String path) {
        ClassLoader cl = Thread.currentThread().getContextClassLoader();
        URL url = cl.getResource(path);
        if (url == null) {
            url = cl.getResource("nuxeo.war/" + path);
        }
        return url;
    }

    /**
     * Gets a resource at the given path.
     *
     * @param path the path, which must not start with a /
     * @see javax.servlet.ServletContext#getResourceAsStream
     * @see java.lang.ClassLoader#getResourceAsStream
     */
    public InputStream getResourceAsStream(String path) {
        ClassLoader cl = Thread.currentThread().getContextClassLoader();
        InputStream is = cl.getResourceAsStream(path);
        if (is == null) {
            is = cl.getResourceAsStream("nuxeo.war/" + path);
        }
        return is;
    }

}
