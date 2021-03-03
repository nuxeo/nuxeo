/*
 * (C) Copyright 2013 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     vpasquier <vpasquier@nuxeo.com>
 *     slacoin <slacoin@nuxeo.com>
 */
package org.nuxeo.ecm.automation.core.trace;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.automation.OperationCallback;
import org.nuxeo.ecm.automation.OperationChain;
import org.nuxeo.ecm.automation.OperationContext;
import org.nuxeo.ecm.automation.OperationType;
import org.nuxeo.ecm.automation.core.impl.InvokableMethod;
import org.nuxeo.runtime.api.Framework;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;

/**
 * @since 5.7.3 The Automation tracer factory service.
 */
public class TracerFactory implements TracerFactoryMBean {

    public static final String AUTOMATION_TRACE_PROPERTY = "org.nuxeo.automation.trace";

    public static final String AUTOMATION_TRACE_PRINTABLE_PROPERTY = "org.nuxeo.automation.trace.printable";

    protected static final Integer CACHE_CONCURRENCY_LEVEL = 10;

    protected static final Integer CACHE_MAXIMUM_SIZE = 1000;

    protected static final Integer CACHE_TIMEOUT = 10;

    protected String printable;

    protected Function<String, Boolean> printableAssertor;

    protected Cache<String, ChainTraces> tracesCache;

    protected boolean recording;

    protected Trace lastError;

    public TracerFactory() {
        tracesCache = CacheBuilder.newBuilder()
                                  .concurrencyLevel(CACHE_CONCURRENCY_LEVEL)
                                  .maximumSize(CACHE_MAXIMUM_SIZE)
                                  .expireAfterWrite(CACHE_TIMEOUT, TimeUnit.MINUTES)
                                  .build();
        recording = Framework.isBooleanPropertyTrue(AUTOMATION_TRACE_PROPERTY);
        setPrintableTraces(Framework.getProperty(AUTOMATION_TRACE_PRINTABLE_PROPERTY, "*"));
    }

    protected static class ChainTraces {

        protected OperationType chain;

        protected Map<Integer, Trace> traces = new HashMap<Integer, Trace>();

        protected ChainTraces(OperationType chain) {
            this.chain = chain;
        }

        protected String add(Trace trace) {
            int index = traces.size();
            traces.put(Integer.valueOf(index), trace);
            return formatKey(trace.chain, index);
        }

        protected Trace getTrace(int index) {
            return traces.get(index);
        }

        protected void removeTrace(int index) {
            traces.remove(index);
        }

        protected void clear() {
            traces.clear();
        }

        protected int size() {
            return traces.size();
        }

    }

    public OperationCallback newTracer() {
        return new Tracer(this);
    }

    /**
     * If trace mode is enabled, instantiate {@link Call}. If not, instantiate {@link LiteCall}.
     */
    public Call newCall(OperationType chain, OperationContext context, OperationType type, InvokableMethod method,
            Map<String, Object> params) {
        if (!recording) {
            return new LiteCall(chain, type);
        }
        return new Call(chain, context, type, method, params);
    }

    public Trace newTrace(Call parent, OperationType typeof, List<Call> calls, Object output, Exception error) {
        return new Trace(parent, typeof, calls, calls.get(0).details.input, output, error);
    }

    protected void recordTrace(Trace trace) {
        String chainId = trace.chain.getId();
        ChainTraces chainTraces = tracesCache.getIfPresent(chainId);
        if (chainTraces == null) {
            tracesCache.put(chainId, new ChainTraces(trace.chain));
        }
        if (trace.getError() != null) {
            lastError = trace;
        }
        chainTraces = tracesCache.getIfPresent(chainId);
        if (chainTraces.size() != 0) {
            chainTraces.removeTrace(1);
        }
        tracesCache.getIfPresent(chainId).add(trace);
    }

    public Trace getTrace(OperationChain chain, int index) {
        return tracesCache.getIfPresent(chain.getId()).getTrace(index);
    }

    /**
     * @param key The name of the chain.
     * @return The last trace of the given chain.
     */
    public Trace getTrace(String key) {
        return getTrace(key, -1);
    }

    public Trace getTrace(String key, int index) {
        ChainTraces chainTrace = tracesCache.getIfPresent(key);
        if (chainTrace == null) {
            return null;
        }
        if (index < 0) {
            index = chainTrace.traces.size() - 1;
        }
        return tracesCache.getIfPresent(key).getTrace(index);
    }

    public Trace getLastErrorTrace() {
        return lastError;
    }

    public void clearTrace(OperationChain chain, int index) {
        tracesCache.getIfPresent(chain).removeTrace(index);
    }

    public void clearTrace(OperationChain chain) {
        tracesCache.invalidate(chain);
    }

    @Override
    public void clearTraces() {
        tracesCache.invalidateAll();
    }

    protected static String formatKey(OperationType chain, int index) {
        return String.format("%s:%s", chain.getId(), index);
    }

    public void onTrace(Trace trace) {
        boolean containsError = trace.error != null;
        if (!(recording || containsError)) {
            return;
        }

        if (containsError) {
            trace.error.addSuppressed(new Throwable(print(trace)));
        }
        if (printableAssertor.apply(trace.chain.getId())) {
            Log log = LogFactory.getLog(Trace.class);
            if (containsError) {
                log.warn(print(trace));
            } else {
                log.info(print(trace));
            }
        }
        recordTrace(trace);
    }

    @Override
    public boolean toggleRecording() {
        return recording = !recording;
    }

    @Override
    public boolean getRecordingState() {
        return recording;
    }

    @Override
    public String getPrintableTraces() {
        return printable;
    }

    @Override
    public String setPrintableTraces(String option) {
        if ("*".equals(option)) {
            printableAssertor = s -> Boolean.TRUE;
        } else {
            List<String> patterns = Arrays.asList(option.split(","));
            printableAssertor = s -> {
                return Boolean.valueOf(patterns.contains(s));
            };
        }
        printable = option;
        return printable;
    }

    public String print(Trace trace) {
        return trace.print(!recording);
    }
}
