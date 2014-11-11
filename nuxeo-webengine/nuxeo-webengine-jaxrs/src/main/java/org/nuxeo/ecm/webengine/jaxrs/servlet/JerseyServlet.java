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
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.nuxeo.ecm.webengine.jaxrs.Activator;

import com.sun.jersey.spi.container.servlet.ServletContainer;


/**
 * JAX-RS servlet based on jersey servlet to provide hot reloading.
 * <p>
 * Use it as the webengine servlet in web.xml if you want hot reload, otherwise
 * use {@link ServletContainer}.
 *
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
public class JerseyServlet extends ServletContainer {

    private static final long serialVersionUID = 1L;

    /**
     * A list of initialized servlets to be able to set the
     * dirty flag on them when reload is requested by the user.
     */
    protected static Set<JerseyServlet> servlets = Collections.synchronizedSet(new HashSet<JerseyServlet>());

    /**
     * Should be called by the application to set the dirty flag on all loaded servlets.
     */
    public static void invalidate() {
        JerseyServlet[] ar = servlets.toArray(new JerseyServlet[servlets.size()]);
        for (JerseyServlet servlet : ar) {
            servlet.isDirty = true;
        }
    }

    protected volatile boolean isDirty = false;

    public JerseyServlet() {
        super (Activator.getInstance().getApplication());
    }

    public boolean isDirty() {
        return isDirty;
    }

    public void setDirty(boolean isDirty) {
        this.isDirty = isDirty;
    }

    @Override
    public void init() throws ServletException {
        superInit();
        servlets.add(this);
    }

    @Override
    public void destroy() {
        servlets.remove(this);
        superDestroy();
    }

    @Override
    public void service(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String method = request.getMethod().toUpperCase();
        if (!"GET".equals(method)) {
            // force reading properties because jersey is consuming one
            // character
            // from the input stream - see WebComponent.isEntityPresent.
            request.getParameterMap();
        }
        superService(request, response);
    }


    protected void superInit() throws ServletException {
        Thread thread = Thread.currentThread();
        ClassLoader cl = thread.getContextClassLoader();
        thread.setContextClassLoader(ServiceClassLoader.getLoader());
        try {
            super.init();
        } finally {
            thread.setContextClassLoader(cl);
        }
    }

    protected void superDestroy() {
        Thread thread = Thread.currentThread();
        ClassLoader cl = thread.getContextClassLoader();
        thread.setContextClassLoader(ServiceClassLoader.getLoader());
        try {
            super.destroy();
        } finally {
            thread.setContextClassLoader(cl);
        }
    }

    protected synchronized void superReload() throws ServletException {
        if (isDirty) {
            Thread thread = Thread.currentThread();
            ClassLoader cl = thread.getContextClassLoader();
            thread.setContextClassLoader(ServiceClassLoader.getLoader());
            try {
            	// reload is not working correctly since old classes are still referenced
            	// for this to work we need a custom ResourceConfig but all fields in jersey
            	// classes are private so we cannot set it ...
                //super.reload();
            	super.destroy();
            	super.init();
            } finally {
                isDirty = false;
                thread.setContextClassLoader(cl);
            }
        }
    }

    protected void superService(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        Thread thread = Thread.currentThread();
        ClassLoader cl = thread.getContextClassLoader();
        thread.setContextClassLoader(JerseyServlet.class.getClassLoader());
        try {
            if (isDirty) {
                superReload();
            }
            super.service(request, response);
        } finally {
            thread.setContextClassLoader(cl);
        }
    }

}
