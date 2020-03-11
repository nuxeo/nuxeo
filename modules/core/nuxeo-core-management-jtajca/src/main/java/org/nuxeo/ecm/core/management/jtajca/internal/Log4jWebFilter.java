/*
 * (C) Copyright 2011 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     matic
 */
package org.nuxeo.ecm.core.management.jtajca.internal;

import java.io.IOException;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.apache.commons.beanutils.PropertyUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.logging.log4j.ThreadContext;

/**
 * @author matic
 */
public class Log4jWebFilter implements Filter {

    private static final Log log = LogFactory.getLog(Log4jWebFilter.class);

    protected FilterConfig config;

    @Override
    public void init(FilterConfig filterConfig) {
        config = filterConfig;
    }

    @Override
    public void destroy() {

    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException,
            ServletException {
        try {
            putProperty(request, "RemoteAddr");
            putProperty(request, "PathInfo");
            putProperty(request, "RequestURL");
            putProperty(request, "ServletPath");
            putProperty(request, "UserPrincipal");
            final HttpSession session = ((HttpServletRequest) request).getSession(false);
            if (session != null) {
                ThreadContext.put("SessionID", session.getId());
            }
            chain.doFilter(request, response);
        } finally {
            ThreadContext.remove("RemoteAddr");
            ThreadContext.remove("PathInfo");
            ThreadContext.remove("RequestURL");
            ThreadContext.remove("ServletPath");
            ThreadContext.remove("UserPrincipal");
            ThreadContext.remove("SessionID");
        }

    }

    protected void putProperty(Object object, String propertyName) {
        try {
            if (object != null) {
                String name = propertyName.substring(0, 1).toLowerCase() + propertyName.substring(1);
                Object prop = PropertyUtils.getProperty(object, name);
                if (prop != null) {
                    ThreadContext.put(propertyName, prop.toString());
                }
            }
        } catch (ReflectiveOperationException e) {
            log.error(e, e);
        }
    }

}
