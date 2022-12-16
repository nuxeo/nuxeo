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

import org.apache.logging.log4j.ThreadContext;
import org.nuxeo.lib.stream.Log4jCorrelation;

import datadog.trace.api.CorrelationIdentifier;
import io.opencensus.implcore.trace.RecordEventsSpanImpl;
import io.opencensus.trace.AttributeValue;
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

    // @since 2021.16
    protected static final String DD_TRACE_ID_CONTEXT_KEY = "dd.trace_id";

    // @since 2021.16
    protected static final String DD_SPAN_ID_CONTEXT_KEY = "dd.span_id";

    @Override
    public void init(FilterConfig filterConfig) {
        config = filterConfig;
    }

    @Override
    protected void doFilter(HttpServletRequest req, HttpServletResponse res, FilterChain chain)
            throws IOException, ServletException {
        Span span = Tracing.getTracer().getCurrentSpan();
        addTags(span, req);
        addTracingCorrelation(span);
        try {
            chain.doFilter(req, res);
        } finally {
            removeTracingCorrelation();
        }
    }

    protected void addTags(Span span, HttpServletRequest httpRequest) {
        if (!(span instanceof RecordEventsSpanImpl)) {
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

    protected void addTracingCorrelation(Span span) {
        addDatadogTracingCorrelation();
        Log4jCorrelation.start(span);
    }

    protected void removeTracingCorrelation() {
        removeDatadogTracingCorrelation();
        Log4jCorrelation.end();
    }

    protected void addDatadogTracingCorrelation() {
        String traceId = CorrelationIdentifier.getTraceId();
        if ("0".equals(traceId)) {
            return;
        }
        ThreadContext.put(DD_TRACE_ID_CONTEXT_KEY, traceId);
        ThreadContext.put(DD_SPAN_ID_CONTEXT_KEY, CorrelationIdentifier.getSpanId());
    }

    protected void removeDatadogTracingCorrelation() {
        ThreadContext.remove(DD_TRACE_ID_CONTEXT_KEY);
        ThreadContext.remove(DD_SPAN_ID_CONTEXT_KEY);
    }
}
