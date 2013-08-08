/*
 * (C) Copyright 2013 Nuxeo SA (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     vpasquier <vpasquier@nuxeo.com>
 *     slacoin <slacoin@nuxeo.com>
 */
package org.nuxeo.ecm.automation.core.trace;

import java.util.HashMap;
import java.util.Map;

import org.nuxeo.ecm.automation.OperationCallback;
import org.nuxeo.ecm.automation.OperationChain;
import org.nuxeo.ecm.automation.OperationType;
import org.nuxeo.runtime.api.Framework;

/**
 * @since 5.7.3 The Automation tracer factory service
 */
public class TracerFactory implements TracerFactoryMBean {

    public static final String AUTOMATION_TRACE_PROPERTY = "org.nuxeo.automation.trace";

    protected Map<String, ChainTraces> traces = new HashMap<String, ChainTraces>();

    protected boolean recording;

    public TracerFactory() {
        this.recording = Boolean.parseBoolean(Framework.getProperty(
                AUTOMATION_TRACE_PROPERTY, "false"));
    }

    protected static class ChainTraces {

        protected OperationType chain;

        protected Map<Integer, Trace> traces = new HashMap<Integer, Trace>();

        protected ChainTraces(OperationType chain) {
            this.chain = chain;
        }

        protected String add(Trace trace) {
            final int index = Integer.valueOf(traces.size());
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

    }

    /**
     * If trace mode is enabled, instantiate {@link Tracer}. If not, instantiate
     * {@link TracerLite}.
     */
    public OperationCallback newTracer() {
        if (recording) {
            return new Tracer(this);
        }
        return new TracerLite(this);
    }

    public String recordTrace(Trace trace) {
        String chainId = trace.chain.getId();
        if (!traces.containsKey(chainId)) {
            traces.put(chainId, new ChainTraces(trace.chain));
        }
        return traces.get(chainId).add(trace);
    }

    public Trace getTrace(OperationChain chain, int index) {
        return traces.get(chain.getId()).getTrace(index);
    }

    public Trace getTrace(String key) {
        ChainTraces chainTrace = traces.get(key);
        return traces.get(key).getTrace(chainTrace.traces.size() - 1);
    }

    public void clearTrace(OperationChain chain, int index) {
        traces.get(chain).removeTrace(Integer.valueOf(index));
    }

    public void clearTrace(OperationChain chain) {
        traces.remove(chain);
    }

    @Override
    public void clearTraces() {
        traces.clear();
    }

    protected static String formatKey(OperationType chain, int index) {
        return String.format("%s:%s", chain.getId(), index);
    }

    public void onTrace(Trace popped) {
        recordTrace(popped);
    }

    @Override
    public boolean toggleRecording() {
        return recording = !recording;
    }

    @Override
    public boolean getRecordingState() {
        return recording;
    }
}
