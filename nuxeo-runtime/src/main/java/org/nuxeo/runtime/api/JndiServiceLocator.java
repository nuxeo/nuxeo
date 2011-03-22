/*
 * Copyright (c) 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Nuxeo - initial API and implementation
 *
 * $Id$
 */

package org.nuxeo.runtime.api;

import java.util.Properties;

import javax.naming.InitialContext;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
public class JndiServiceLocator implements ServiceLocator {

    private static final long serialVersionUID = -8445946234540101788L;

    protected transient InitialContext context;

    /**
     * Initializes and creates the JNDI initial context.
     */
    public void initialize(String host, int port, Properties properties)
            throws Exception {
        context = new InitialContext();
    }

    @Override
    public void dispose() {
        context = null;
    }

    public InitialContext getContext() {
        return context;
    }

    @Override
    public Object lookup(ServiceDescriptor sd) throws Exception {
        String locator = sd.getLocator();
        if (locator == null) {
            locator = createLocator(sd);
            sd.setLocator(locator);
        }
        return lookup(locator);
    }

    @Override
    public Object lookup(String serviceId) throws Exception {
        ClassLoader oldcl = Thread.currentThread().getContextClassLoader();
        Thread.currentThread().setContextClassLoader(
                getClass().getClassLoader());
        try {
            return context.lookup(serviceId);
        } finally {
            Thread.currentThread().setContextClassLoader(oldcl);
        }
    }

    protected String createLocator(ServiceDescriptor sd) {
        return sd.getServiceClassName();
    }

}
