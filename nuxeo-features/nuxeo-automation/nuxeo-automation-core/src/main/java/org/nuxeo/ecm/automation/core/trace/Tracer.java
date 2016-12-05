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
import java.util.Map;
import java.util.Stack;

import org.nuxeo.ecm.automation.OperationCallback;
import org.nuxeo.ecm.automation.OperationContext;
import org.nuxeo.ecm.automation.OperationException;
import org.nuxeo.ecm.automation.OperationType;
import org.nuxeo.ecm.automation.core.impl.InvokableMethod;

/**
 * Automation Abstract tracer recording all automation execution traces.
 *
 * @since 5.9.1
 */
public class Tracer implements OperationCallback {

    protected final TracerFactory factory;

    protected Stack<Context> stack = new Stack<Context>();

    class Context {
        final Call parent;
        final OperationType typeof;
        final LinkedList<Call> calls = new LinkedList<Call>();
        OperationException error;

        Context(Call parent, OperationType oftype) {
            this.parent = parent;
            typeof = oftype;
        }

    }

    protected Tracer(TracerFactory factory) {
        this.factory = factory;
    }

    protected void pushContext(OperationType chain) {
        stack.push(new Context(stack.isEmpty() ? null : stack.peek().calls.peekLast(), chain));
    }

    protected void popContext() {
        Context context = stack.pop();
        Trace trace = factory.newTrace(context.parent, context.typeof, context.calls, context.calls.getLast().getOutput(), context.error);
        if (stack.isEmpty()) {
            factory.onTrace(trace);
        } else {
            context.parent.nested.add(trace);
        }
    }

    @Override
    public void onChainEnter(OperationType chain) {
        pushContext(chain);
    }

    @Override
    public void onChainExit() {
        popContext();
    }

    @Override
    public void onOperationEnter(OperationContext context, OperationType type, InvokableMethod method,
            Map<String, Object> params) {
        stack.peek().calls.add(factory.newCall(stack.peek().typeof, context, type, method, params));
    }

    @Override
    public void onOperationExit(Object output) {
        stack.peek().calls.peekLast().details.output = output;
    }

    @Override
    public OperationException onError(OperationException error) {
        return stack.peek().error = error;
    }

}
