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
package org.nuxeo.ecm.webengine.jaxrs.session;

import java.io.IOException;

import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.nuxeo.ecm.webengine.jaxrs.HttpFilter;
import org.nuxeo.ecm.webengine.jaxrs.session.impl.PerSessionCoreProvider;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class SessionCleanupFilter extends HttpFilter {

    public final String STATEFUL_KEY = SessionCleanupFilter.class.getName() + ".stateful";

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
    }


    @Override
    public void run(HttpServletRequest request, HttpServletResponse response,
            FilterChain chain) throws IOException, ServletException {
        if (getBoolean(request, STATEFUL_KEY)) {
            PerSessionCoreProvider.install(request);
        }
        try {
            chain.doFilter(request, response);
        } finally {
            SessionFactory.dispose(request);
        }
    }

    @Override
    public void destroy() {
    }

}
