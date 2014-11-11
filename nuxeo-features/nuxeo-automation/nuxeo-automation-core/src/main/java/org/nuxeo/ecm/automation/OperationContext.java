/*
 * (C) Copyright 2006-2008 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     bstefanescu
 */
package org.nuxeo.ecm.automation;

import java.security.Principal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.runtime.api.Framework;

/**
 * An operation context. Holds context objects, a context parameters map and a
 * list of operations to run.
 * <p>
 * Context objects are:
 * <ul>
 * <li> The Operation Chain Input - optional. It will be used as the input for
 * the first operation in the chain. If input is null then only VOID methods in
 * the first operation will be matched.
 * <li> A Core Session - which is optional and should be provided by the
 * caller. (either at creation time as a constructor argument, either using the
 * {@link #setCoreSession(CoreSession)} method. When running the operation
 * chain in asynchronous mode another session will be created by preserving the
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

    private static final long serialVersionUID = 2944230823597903715L;

    protected transient CoreSession session;

    /**
     * Whether to save the session at the end of the chain execution. The
     * default is true.
     */
    protected boolean commit = true;

    protected transient List<CleanupHandler> cleanupHandlers;

    /**
     * Each stack use a key the type of the objects in the stack: document,
     * documents, blob or blobs
     */
    protected transient Map<String, List<Object>> stacks;

    /**
     * The execution input that will be updated after an operation run with the
     * operation output
     */
    protected Object input;

    public OperationContext() {
        this(null);
    }

    public OperationContext(CoreSession session) {
        stacks = new HashMap<String, List<Object>>();
        setCoreSession(session);
        cleanupHandlers = new ArrayList<CleanupHandler>();
    }

    public void setCoreSession(CoreSession session) {
        this.session = session;
    }

    public void setCommit(boolean commit) {
        this.commit = commit;
    }

    public boolean isCommit() {
        return commit;
    }

    public CoreSession getCoreSession() {
        return session;
    }

    public Principal getPrincipal() {
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
            return type.cast(session);
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

    public void addCleanupHandler(CleanupHandler handler) {
        cleanupHandlers.add(handler);
    }

    public void removeCleanupHandler(CleanupHandler handler) {
        cleanupHandlers.remove(handler);
    }

    public void dispose() {
        for (CleanupHandler handler : cleanupHandlers) {
            try {
                handler.cleanup();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
