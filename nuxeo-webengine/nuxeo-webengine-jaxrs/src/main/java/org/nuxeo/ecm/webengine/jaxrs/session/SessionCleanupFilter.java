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
 */
public class SessionCleanupFilter extends HttpFilter {

    public final String STATEFUL_KEY = SessionCleanupFilter.class.getName() + ".stateful";

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
    }

    @Override
    public void run(HttpServletRequest request, HttpServletResponse response, FilterChain chain) throws IOException,
            ServletException {
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
