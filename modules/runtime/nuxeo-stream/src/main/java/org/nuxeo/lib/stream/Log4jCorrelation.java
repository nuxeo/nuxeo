/*
 * (C) Copyright 2022 Nuxeo (http://nuxeo.com/) and others.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.1
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
package org.nuxeo.lib.stream;

import java.util.List;

import org.apache.logging.log4j.ThreadContext;

import io.opencensus.implcore.trace.RecordEventsSpanImpl;
import io.opencensus.trace.Span;
import io.opencensus.trace.Tracing;

/**
 * Add tracing identifier to the log4j contextMap
 *
 * @since 2021.16
 */
public class Log4jCorrelation {

    public static final String TRACE_ID_CONTEXT_KEY = "traceId";

    public static final String SPAN_ID_CONTEXT_KEY = "spanId";

    public static final List<String> CONTEXT_KEYS = List.of(TRACE_ID_CONTEXT_KEY, SPAN_ID_CONTEXT_KEY);

    public static void start() {
        start(Tracing.getTracer().getCurrentSpan());
    }

    public static void start(Span span) {
        if (!(span instanceof RecordEventsSpanImpl)) {
            return;
        }
        ThreadContext.put(TRACE_ID_CONTEXT_KEY, span.getContext().getTraceId().toLowerBase16());
        ThreadContext.put(SPAN_ID_CONTEXT_KEY, span.getContext().getSpanId().toLowerBase16());
    }

    public static void end() {
        ThreadContext.removeAll(CONTEXT_KEYS);
    }

}
