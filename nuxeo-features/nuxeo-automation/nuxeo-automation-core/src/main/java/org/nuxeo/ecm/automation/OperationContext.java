/* 
 * Copyright (c) 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     bstefanescu
 */
package org.nuxeo.ecm.automation;

import java.security.Principal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.transaction.TransactionHelper;

/**
 * An operation context. Holds context objects, a context parameters map and a
 * list of operations to run.
 * <p>
 * Context objects are:
 * <ul>
 * <li>The Operation Chain Input - optional. It will be used as the input for
 * the first operation in the chain. If input is null then only VOID methods in
 * the first operation will be matched.
 * <li>A Core Session - which is optional and should be provided by the caller.
 * (either at creation time as a constructor argument, either using the
 * {@link #setCoreSession(CoreSession)} method. When running the operation chain
 * in asynchronous mode another session will be created by preserving the
 * current session credentials.
 * </ul>
 * <p>
 * Each entry in the operation list contains the ID of the operation to be run
 * and a map of operation parameters to use when initializing the operation.
 * <p>
 * The context parameters map can be filled with contextual information by the
 * caller. Each operation will be able to access the contextual data at runtime
 * and to update it if needed.
 *
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
public class OperationContext extends HashMap<String, Object> {

    private static final Log log = LogFactory.getLog(OperationContext.class);

    private static final long serialVersionUID = 2944230823597903715L;

    /**
     * Whether to save the session at the end of the chain execution. The
     * default is true.
     */
    protected boolean commit = true;

    protected final transient List<CleanupHandler> cleanupHandlers;

    /**
     * Each stack use a key the type of the objects in the stack: document,
     * documents, blob or blobs
     */
    protected final transient Map<String, List<Object>> stacks;

    /**
     * A logins stack manage multiple logins and sessions in a single chain
     * execution
     */
    protected transient LoginStack loginStack;

    /**
     * The execution input that will be updated after an operation run with the
     * operation output
     */
    protected Object input;

    /**
     * A list of trace messages useful to use in exception details.
     */
    protected List<String> trace;

    public OperationContext() {
        this(null);
    }

    public OperationContext(CoreSession session) {
        stacks = new HashMap<String, List<Object>>();
        cleanupHandlers = new ArrayList<CleanupHandler>();
        loginStack = new LoginStack(session);
        trace = new ArrayList<String>();
    }

    public void setCoreSession(CoreSession session) {
        this.loginStack.setSession(session);
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
            stack = new ArrayList<Object>();
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
            try {
                return Framework.getService(type);
            } catch (Exception e) {
                throw new RuntimeException("Failed to lookup service: " + type,
                        e);
            }
        }
    }

    public void addTrace(String trace) {
        this.trace.add(trace);
    }

    public List<String> getTrace() {
        return trace;
    }
    public String getFormattedTrace() {
        String crlf = System.getProperty("line.separator");
        StringBuilder buf =new StringBuilder();
        for (String t: trace) {
            buf.append("> ").append(t).append(crlf);
        }
        return buf.toString();
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
        for (CleanupHandler handler : cleanupHandlers) {
            try {
                handler.cleanup();
            } catch (Exception e) {
                log.error(e, e);
            }
        }
    }

    /**
     * Set the rollback mark on the current tx. This will cause the transaction to rollback.
     * Also this is setting the session commit flag on false
     */
    public void setRollback() {
        setCommit(false);
        TransactionHelper.setTransactionRollbackOnly();
    }
}
