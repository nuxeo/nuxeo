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
 */

package org.nuxeo.ecm.automation.core.trace;

import java.util.LinkedList;
import java.util.Map;
import java.util.Stack;

import org.nuxeo.ecm.automation.OperationCallback;
import org.nuxeo.ecm.automation.OperationContext;
import org.nuxeo.ecm.automation.OperationException;
import org.nuxeo.ecm.automation.OperationType;
import org.nuxeo.ecm.automation.core.impl.ChainTypeImpl;
import org.nuxeo.ecm.automation.core.impl.InvokableMethod;

/**
 * Automation tracer recording lightweight automation execution traces when mode
 * deactivated
 *
 * @since 5.7.3
 */
public class TracerLite implements OperationCallback {

    protected final TracerFactory factory;

    protected final LinkedList<Call> calls = new LinkedList<Call>();

    protected Stack<Trace> callingStacks = new Stack<Trace>();

    protected Call parent;

    protected OperationType chain;

    protected Trace trace;

    protected String factoryIndex;

    protected TracerLite(TracerFactory factory) {
        this.factory = factory;
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
        Trace stack = callingStacks.pop();
        parent = stack.parent;
        chain = stack.chain;
        calls.addAll(stack.operations);

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

    public String getFactoryIndex() {
        return factoryIndex;
    }

    @Override
    public void onChain(OperationType chain) {
        pushContext(chain);
    }

    @Override
    public void onOperation(OperationContext context, OperationType type,
            InvokableMethod method, Map<String, Object> parms) {
        if (type instanceof ChainTypeImpl) {
            pushContext(type);
            return;
        }
        Call call = new Call(chain, null, type, null, null);
        calls.add(call);
    }

    @Override
    public void onOutput(Object output) {
        // In case of lightweight tracer, doesn't record anything onOutput
        return;
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
        return trace != null ? trace.getLiteFormattedText()
                : "You can activate automation trace mode to get more informations on automation executions";
    }
}
