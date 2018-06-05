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
import java.util.AbstractMap;
import java.util.AbstractSet;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.function.Consumer;

import org.nuxeo.ecm.automation.core.Constants;
import org.nuxeo.ecm.automation.core.scripting.Expression;
import org.nuxeo.ecm.automation.core.trace.TracerFactory;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.NuxeoException;
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
public class OperationContext extends AbstractMap<String, Object> implements AutoCloseable {

    /**
     * Whether to save the session at the end of the chain execution. The default is true.
     */
    protected boolean commit = true;

    protected final List<CleanupHandler> cleanupHandlers;

    /**
     * The context variables.
     */
    protected final Map<String, Object> vars;

    /**
     * Each stack use a key the type of the objects in the stack: document, documents, blob or blobs
     */
    protected final Map<String, Deque<Object>> stacks = new HashMap<>();

    /**
     * A logins stack manage multiple logins and sessions in a single chain execution
     */
    protected LoginStack loginStack;

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
    protected OperationCallback callback;

    public OperationContext() {
        this(null);
    }

    public OperationContext(CoreSession session) {
        this(session, new HashMap<>());
    }

    protected OperationContext(CoreSession session, Map<String, Object> bindings) {
        vars = bindings;
        cleanupHandlers = new ArrayList<>();
        loginStack = new LoginStack(session);
        trace = new ArrayList<>();
        callback = Framework.getService(TracerFactory.class).newTracer();
    }

    public void setCoreSession(CoreSession session) {
        loginStack.setSession(session);
    }

    public void setCommit(boolean commit) {
        this.commit = commit;
    }

    public boolean isCommit() {
        return commit && !TransactionHelper.isTransactionMarkedRollback();
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

    public void push(String type, Object obj) {
        stacks.computeIfAbsent(type, key -> new LinkedList<>()).push(obj);
    }

    public Object peek(String type) {
        return Optional.ofNullable(stacks.get(type)).map(Deque::peek).orElse(null);
    }

    public Object pop(String type) {
        return Optional.ofNullable(stacks.get(type)).map(stack -> {
            Object obj = stack.pop();
            if (stack.isEmpty()) {
                stacks.remove(type);
            }
            return obj;
        }).orElse(null);
    }

    public Object pull(String type) {
        return Optional.ofNullable(stacks.get(type)).map(stack -> {
            Object obj = stack.removeLast();
            if (stack.isEmpty()) {
                stacks.remove(type);
            }
            return obj;
        }).orElse(null);
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

    @Override
    public void close() {
        if (getCoreSession() != null && isCommit()) {
            // auto save session if any.
            getCoreSession().save();
        }
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
    public Object get(Object key) {
        return resolve(vars.get(key));
    }

    @Override
    public Object put(String key, Object value) {
        if (Constants.VAR_RUNTIME_CHAIN.equals(key)) {
            throw new IllegalArgumentException(Constants.VAR_RUNTIME_CHAIN + " is reserved, not writable");
        }
        return resolve(vars.put(key, value));
    }

    @Override
    public Object remove(Object key) {
        if (Constants.VAR_RUNTIME_CHAIN.equals(key)) {
            throw new IllegalArgumentException(Constants.VAR_RUNTIME_CHAIN + " is reserved, not writable");
        }
        return resolve(vars.remove(key));
    }

    @SuppressWarnings("unchecked")
    public Map<String, Object> getChainParameters() {
        return (Map<String, Object>) vars.getOrDefault(Constants.VAR_RUNTIME_CHAIN, new HashMap<>());
    }

    @SuppressWarnings("unchecked")
    public Map<String, Object> putChainParameters(Map<String, ?> parameters) {
        return (Map<String, Object>) vars.put(Constants.VAR_RUNTIME_CHAIN, parameters);
    }

    @SuppressWarnings("unchecked")
    public Map<String, Object> removeChainParameters() {
        return (Map<String, Object>) vars.remove(Constants.VAR_RUNTIME_CHAIN);
    }

    /**
     * Gets the parameter associated with the given key from the chain parameters falling back on the global context.
     *
     * @since 10.2
     */
    public Object getChainParameter(String key) {
        return Optional.ofNullable(getChainParameters().get(key)).map(this::resolve).orElse(get(key));
    }

    /**
     * Calls the given {@code callable} after having merged the given {@code parameters} with the chain parameters
     * stored in the {@link Constants#VAR_RUNTIME_CHAIN} context variable and restores the initial chain parameters
     * afterwards.
     *
     * @since 10.2
     */
    public <T> T callWithChainParameters(Callable<T> callable, Map<String, Object> parameters)
            throws OperationException {
        Callable<Map<String, Object>> initialize = () -> {
            Map<String, Object> initialParameters = getChainParameters();
            Map<String, Object> mergedParameters = new HashMap<>();
            mergedParameters.putAll(initialParameters);
            mergedParameters.putAll(parameters);
            putChainParameters(mergedParameters);
            return initialParameters;
        };
        Consumer<Map<String, Object>> restore = this::putChainParameters;
        return call(callable, initialize, restore);

    }

    /**
     * Calls the given {@code callable} after having put a variable with {@code key=value} in the current context and
     * restores the initial context afterwards.
     *
     * @since 10.2
     */
    public <T> T callWithContextVar(Callable<T> callable, String key, Object value) throws OperationException {
        Callable<Object> initialize = () -> {
            Object initialValue = vars.get(key);
            vars.put(key, value);
            return initialValue;
        };
        Consumer<Object> restore = (Object initialValue) -> {
            if (initialValue == null) {
                vars.remove(key);
            } else {
                vars.put(key, initialValue);
            }
        };
        return call(callable, initialize, restore);
    }

    /**
     * Calls the given {@code callable} after having backed up the initial state as a result of {@code initialize} and
     * restores the initial state afterwards by calling {@code restore}.
     */
    protected <T, U> T call(Callable<T> callable, Callable<U> initialize, Consumer<U> restore)
            throws OperationException {
        U initialState = null;
        try {
            initialState = initialize.call();
            return callable.call();
        } catch (OperationException | NuxeoException e) {
            throw e;
        } catch (Exception e) {
            throw new OperationException(e);
        } finally {
            restore.accept(initialState);
        }
    }

    @Override
    public Set<Map.Entry<String, Object>> entrySet() {
        return new AbstractSet<Map.Entry<String, Object>>() {

            @Override
            public Iterator<Entry<String, Object>> iterator() {
                Iterator<Entry<String, Object>> iterator = vars.entrySet().iterator();
                return new Iterator<Entry<String, Object>>() {

                    @Override
                    public boolean hasNext() {
                        return iterator.hasNext();
                    }

                    @Override
                    public Entry<String, Object> next() {
                        Entry<String, Object> entry = iterator.next();
                        return new Entry<String, Object>() {

                            @Override
                            public String getKey() {
                                return entry.getKey();
                            }

                            @Override
                            public Object getValue() {
                                return resolve(entry.getValue());
                            }

                            @Override
                            public Object setValue(Object value) {
                                Object previous = entry.setValue(value);
                                return resolve(previous);
                            }

                        };
                    }

                    @Override
                    public void remove() {
                        iterator.remove();
                    }

                };
            }

            @Override
            public int size() {
                return vars.size();
            }
        };
    }

    /**
     * @since 5.7.3
     */
    public OperationCallback getCallback() {
        return callback;
    }

    /**
     * @since 5.7.3
     */
    public void setCallback(OperationCallback chainCallback) {
        callback = chainCallback;
    }

    /**
     * @since 5.7.3
     * @param isolate define if keeps context variables for the subcontext
     * @param input an input object
     * @return a subcontext
     */
    public OperationContext getSubContext(boolean isolate, Object input) {
        Map<String, Object> subVars = isolate ? new HashMap<>(getVars()) : getVars();
        OperationContext subctx = new OperationContext(getCoreSession(), subVars);
        subctx.setInput(input);
        subctx.setCallback(callback);
        return subctx;
    }

    /**
     * @since 9.1
     * @param isolate define if keeps context variables for the subcontext
     * @return a subcontext
     */
    public OperationContext getSubContext(boolean isolate) {
        return getSubContext(isolate, getInput());
    }

    /**
     * Evaluate the expression against this context if needed
     *
     * @param obj
     * @return the resolved value
     * @since 9.1
     */
    public Object resolve(Object obj) {
        if (!(obj instanceof Expression)) {
            return obj;
        }
        return ((Expression) obj).eval(this);
    }

}
