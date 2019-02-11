/*
 * (C) Copyright 2019 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     bdelbosc
 */
package org.nuxeo.ecm.core.management.jtajca.internal;

import java.io.IOException;
import java.security.Principal;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpFilter;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import io.opencensus.trace.AttributeValue;
import io.opencensus.trace.BlankSpan;
import io.opencensus.trace.Span;
import io.opencensus.trace.Tracing;

/**
 * Add some tags to span created by OcHttpServletFilter
 */
public class TracingWebFilter extends HttpFilter {

    protected FilterConfig config;

    protected static final String USER_KEY = "http.user";

    protected static final String THREAD_KEY = "http.thread";

    protected static final String SESSION_KEY = "http.session";

    @Override
    public void init(FilterConfig filterConfig) {
        config = filterConfig;
    }

    @Override
    protected void doFilter(HttpServletRequest req, HttpServletResponse res, FilterChain chain)
            throws IOException, ServletException {
        Span span = Tracing.getTracer().getCurrentSpan();
        addTags(span, req);
        chain.doFilter(req, res);
    }

    protected void addTags(Span span, HttpServletRequest httpRequest) {
        if (span instanceof BlankSpan) {
            return;
        }
        Map<String, AttributeValue> map = new HashMap<>();
        map.put(THREAD_KEY, AttributeValue.stringAttributeValue(Thread.currentThread().getName()));
        Principal principal = httpRequest.getUserPrincipal();
        if (principal != null) {
            map.put(USER_KEY, AttributeValue.stringAttributeValue(principal.getName()));
        }
        HttpSession session = httpRequest.getSession(false);
        if (session != null) {
            map.put(SESSION_KEY, AttributeValue.stringAttributeValue(session.getId()));
        }
        span.putAttributes(map);
    }
}
