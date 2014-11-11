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
package org.nuxeo.theme.html.servlets;

import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;

import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

import org.nuxeo.theme.ResourceResolver;

/**
 * Resolver for resources that checks the servlet context first.
 *
 * @since 5.5
 */
public class ServletResourceResolver extends ResourceResolver implements
        ServletContextListener {

    protected ServletContext servletContext;

    @Override
    public URL getResource(String path) {
        try {
            URL url = servletContext.getResource("/" + path);
            if (url != null) {
                return url;
            }
        } catch (MalformedURLException e) {
            // continue
        }
        return super.getResource(path);
    }

    @Override
    public InputStream getResourceAsStream(String path) {
        InputStream is = servletContext.getResourceAsStream("/" + path);
        if (is != null) {
            return is;
        }
        return super.getResourceAsStream(path);
    }

    @Override
    public void contextInitialized(ServletContextEvent sce) {
        servletContext = sce.getServletContext();
        ResourceResolver.setInstance(this);
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {
        servletContext = null;
        ResourceResolver.setInstance(null);
    }

}
