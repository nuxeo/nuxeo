/*
 * (C) Copyright 2013-2016 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     vpasquier <vpasquier@nuxeo.com>
 *     slacoin <slacoin@nuxeo.com>
 */
package org.nuxeo.ecm.automation;

import java.security.Principal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.nuxeo.ecm.automation.core.impl.InvokableMethod;
import org.nuxeo.ecm.automation.core.trace.Trace;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.transaction.TransactionHelper;

/**
 * An operation context. Holds context objects, a context parameters map and a list of operations to run.
 * <p>
 * Context objects are:
 * <ul>
 * <li>The Operation Chain Input - optional. It will be used as the input for the first operation in the chain. If input
 * is null then only VOID methods in the first operation will be matched.
 * <li>A Core Session - which is optional and should be provided by the caller. (either at creation time as a
 * constructor argument, either using the {@link #setCoreSession(CoreSession)} method. When running the operation chain
 * in asynchronous mode another session will be created by preserving the current session credentials.
 * </ul>
 * <p>
 * Each entry in the operation list contains the ID of the operation to be run and a map of operation parameters to use
 * when initializing the operation.
 * <p>
 * The context parameters map can be filled with contextual information by the caller. Each operation will be able to
 * access the contextual data at runtime and to update it if needed.
 *
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
public class OperationContext implements Map<String, Object> {

    /**
     * The context variables map
     */
    protected Map<String, Object> vars;

    /**
     * Whether to save the session at the end of the chain execution. The default is true.
     */
    protected boolean commit = true;

    protected final transient List<CleanupHandler> cleanupHandlers;

    /**
     * Each stack use a key the type of the objects in the stack: document, documents, blob or blobs
     */
    protected final transient Map<String, List<Object>> stacks;

    /**
     * A logins stack manage multiple logins and sessions in a single chain execution
     */
    protected transient LoginStack loginStack;

    /**
     * The execution input that will be updated after an operation run with the operation output
     */
    protected Object input;

    /**
     * A list of trace. Since 5.7.3 messages is no longer useful for tracing. Use chain call backs to do it.
     */
    protected List<String> trace;

    /**
     * @since 5.7.3 Collect operation invokes.
     */
    protected ChainCallback chainCallback;

    public OperationContext() {
        this(null, null);
    }

    public OperationContext(OperationContext ctx) {
        if (ctx.loginStack == null) {
            this.loginStack = new LoginStack(ctx.getCoreSession());
        } else {
            this.loginStack = ctx.loginStack;
        }
        this.vars = ctx.vars;
        this.cleanupHandlers = ctx.cleanupHandlers;
        this.stacks = ctx.stacks;
        this.commit = ctx.commit;
        this.input = ctx.input;
        this.trace = ctx.trace;
        this.chainCallback = ctx.chainCallback;
    }

    public OperationContext(CoreSession session) {
        this(session, null);
    }

    public OperationContext(CoreSession session, Map<String, Object> vars) {
        stacks = new HashMap<>();
        cleanupHandlers = new ArrayList<>();
        loginStack = new LoginStack(session);
        trace = new ArrayList<>();
        chainCallback = new ChainCallback();
        this.vars = vars != null ? vars : new HashMap<>();
    }

    public void setCoreSession(CoreSession session) {
        loginStack.setSession(session);
    }

    public void setCommit(boolean commit) {
        this.commit = commit;
    }

    public boolean isCommit() {
        return commit;
    }

    public CoreSession getCoreSession() {
        return loginStack.getSession();
    }

    public LoginStack getLoginStack() {
        return loginStack;
    }

    public Principal getPrincipal() {
        CoreSession session = loginStack.getSession();
        return session != null ? session.getPrincipal() : null;
    }

    public void setInput(Object input) {
        this.input = input;
    }

    public Object getInput() {
        return input;
    }

    public Object peek(String type) {
        List<Object> stack = stacks.get(type);
        if (stack == null) {
            return null;
        }
        return stack.isEmpty() ? null : stack.get(stack.size() - 1);
    }

    public void push(String type, Object obj) {
        List<Object> stack = stacks.get(type);
        if (stack == null) {
            stack = new ArrayList<>();
            stacks.put(type, stack);
        }
        stack.add(obj);
    }

    public Object pop(String type) {
        List<Object> stack = stacks.get(type);
        if (stack == null) {
            return null;
        }
        return stack.isEmpty() ? null : stack.remove(stack.size() - 1);
    }

    public Object pull(String type) {
        List<Object> stack = stacks.get(type);
        if (stack == null) {
            return null;
        }
        return stack.isEmpty() ? null : stack.remove(0);
    }

    public <T> T getAdapter(Class<T> type) {
        if (type.isAssignableFrom(getClass())) {
            return type.cast(this);
        } else if (type.isAssignableFrom(CoreSession.class)) {
            return type.cast(getCoreSession());
        } else if (type.isAssignableFrom(Principal.class)) {
            return type.cast(getPrincipal());
        } else { // try nuxeo services
            return Framework.getService(type);
        }
    }

    public void addCleanupHandler(CleanupHandler handler) {
        cleanupHandlers.add(handler);
    }

    public void removeCleanupHandler(CleanupHandler handler) {
        cleanupHandlers.remove(handler);
    }

    public void dispose() throws OperationException {
        trace.clear();
        loginStack.clear();
        cleanupHandlers.forEach(CleanupHandler::cleanup);
    }

    /**
     * Set the rollback mark on the current tx. This will cause the transaction to rollback. Also this is setting the
     * session commit flag on false
     */
    public void setRollback() {
        setCommit(false);
        TransactionHelper.setTransactionRollbackOnly();
    }

    public Map<String, Object> getVars() {
        return vars;
    }

    /** the map API */

    @Override
    public int size() {
        return vars.size();
    }

    @Override
    public boolean isEmpty() {
        return vars.isEmpty();
    }

    @Override
    public boolean containsKey(Object key) {
        return vars.containsKey(key);
    }

    @Override
    public boolean containsValue(Object value) {
        return vars.containsValue(value);
    }

    @Override
    public Object get(Object key) {
        return vars.get(key);
    }

    @Override
    public Object put(String key, Object value) {
        return vars.put(key, value);
    }

    @Override
    public Object remove(Object key) {
        return vars.remove(key);
    }

    @Override
    public void putAll(Map<? extends String, ?> m) {
        vars.putAll(m);
    }

    @Override
    public void clear() {
        vars.clear();
    }

    @Override
    public Set<String> keySet() {
        return vars.keySet();
    }

    @Override
    public Collection<Object> values() {
        return vars.values();
    }

    @Override
    public Set<java.util.Map.Entry<String, Object>> entrySet() {
        return vars.entrySet();
    }

    /**
     * ChainCallback store all automation traces for execution
     *
     * @since 5.7.3
     */
    protected static class ChainCallback implements OperationCallback {

        public OperationCallback operationCallback;

        protected void set(OperationCallback callback) {
            operationCallback = callback;
        }

        @Override
        public void onChain(OperationType chain) {
            operationCallback.onChain(chain);
        }

        @Override
        public void onOperation(OperationContext context, OperationType type, InvokableMethod method,
                Map<String, Object> parms) {
            operationCallback.onOperation(context, type, method, parms);
        }

        @Override
        public void onError(OperationException error) {
            operationCallback.onError(error);
        }

        @Override
        public void onOutput(Object output) {
            operationCallback.onOutput(output);
        }

        @Override
        public Trace getTrace() {
            return operationCallback.getTrace();
        }

        @Override
        public String getFormattedText() {
            throw new UnsupportedOperationException("#getFormattedText is not available for: " + this);
        }

    }

    /**
     * @since 5.7.3
     */
    public OperationCallback getChainCallback() {
        return chainCallback.operationCallback;
    }

    /**
     * @since 5.7.3
     */
    public void addChainCallback(OperationCallback chainCallback) {
        this.chainCallback.set(chainCallback);
    }

    /**
     * @since 5.7.3
     * @param isolate define if keeps context variables for the subcontext
     * @return a subcontext
     */
    public OperationContext getSubContext(Boolean isolate, Object input) {
        Map<String, Object> vars = isolate ? new HashMap<>(getVars()) : getVars();
        OperationContext subctx = new OperationContext(getCoreSession(), vars);
        subctx.setInput(input);
        subctx.addChainCallback(getChainCallback());
        return subctx;
    }
}
