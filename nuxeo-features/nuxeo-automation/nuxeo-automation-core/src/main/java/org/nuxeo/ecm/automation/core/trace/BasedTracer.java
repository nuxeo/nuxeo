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

import java.util.LinkedList;
import java.util.Stack;

import org.nuxeo.ecm.automation.OperationCallback;
import org.nuxeo.ecm.automation.OperationException;
import org.nuxeo.ecm.automation.OperationType;

/**
 * Automation Abstract tracer recording all automation execution traces.
 *
 * @since 5.9.1
 */
public abstract class BasedTracer implements OperationCallback {

    protected final TracerFactory factory;

    protected final LinkedList<Call> calls = new LinkedList<Call>();

    protected Stack<Trace> callingStacks = new Stack<Trace>();

    protected Call parent;

    protected OperationType chain;

    protected Trace trace;

    protected Boolean printable;

    protected BasedTracer(TracerFactory factory, Boolean printable) {
        this.factory = factory;
        this.printable = printable;
    }

    protected void pushContext(OperationType newChain) {
        if (chain != null) {
            callingStacks.push(new Trace(parent, chain, calls));
            parent = calls.isEmpty() ? null : calls.getLast();
            calls.clear();
        }
        chain = newChain;
    }

    protected void popContext() {
        calls.clear();
        if (callingStacks.isEmpty()) {
            parent = null;
            chain = null;
            return;
        }
        Trace trace = callingStacks.pop();
        parent = trace.parent;
        chain = trace.chain;
        calls.addAll(trace.operations);
    }

    protected void saveTrace(Trace popped) {
        if (parent == null) {
            trace = popped;
            chain = null;
            calls.clear();
            factory.onTrace(popped);
        } else {
            parent.nested.add(popped);
            popContext();
        }
    }

    @Override
    public void onChain(OperationType chain) {
        pushContext(chain);
    }

    @Override
    public void onOutput(Object output) {
        saveTrace(new Trace(parent, chain, calls, output));
    }

    @Override
    public void onError(OperationException error) {
        saveTrace(new Trace(parent, chain, calls, error));
    }

    @Override
    public Trace getTrace() {
        return trace;
    }

    @Override
    public String getFormattedText() {
        return printable ? trace.getFormattedText() : "";
    }
}
