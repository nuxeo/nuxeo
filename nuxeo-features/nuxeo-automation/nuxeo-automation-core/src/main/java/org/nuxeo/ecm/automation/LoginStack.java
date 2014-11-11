/*
 * (C) Copyright 2006-2010 Nuxeo SAS (http://nuxeo.com/) and contributors.
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

import java.util.ArrayList;
import java.util.List;

import javax.security.auth.login.LoginContext;

import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.repository.Repository;
import org.nuxeo.ecm.core.api.repository.RepositoryManager;
import org.nuxeo.runtime.api.Framework;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 * 
 */
public class LoginStack {

    protected List<Entry> stack = new ArrayList<Entry>();

    protected CoreSession originalSession;

    protected CoreSession currentSession;

    public LoginStack(CoreSession session) {
        setSession(session);
    }

    public void setSession(CoreSession session) {
        originalSession = session;
        currentSession = session;
    }

    /**
     * Get the current session
     * 
     * @return
     */
    public CoreSession getSession() {
        return currentSession;
    }

    public void push(LoginContext lc) throws OperationException {
        Entry entry = new Entry(lc);
        try {
            if (originalSession != null) {
                entry.session = Framework.getService(RepositoryManager.class).getRepository(
                        originalSession.getRepositoryName()).open();
                currentSession = entry.session;
            }
            stack.add(entry);
        } catch (Exception e) {
            throw new OperationException(
                    "Failed to create new core session for loginAs", e);
        }
    }

    public Entry peek() {
        if (!stack.isEmpty()) {
            return stack.get(stack.size() - 1);
        }
        return null;
    }

    /**
     * Remove the current login context from the stack.
     * <p>
     * If no login context in in the stack nothing is done. If the login context
     * has an associated CoreSession the session will be destroyed and the
     * previous session is restored as the active session of the operation
     * context.
     */
    public void pop() throws OperationException {
        if (!stack.isEmpty()) {
            Entry entry = stack.remove(stack.size() - 1);
            entry.dispose();
            entry = peek();
            if (entry != null) {
                currentSession = entry.session;
            } else {
                currentSession = originalSession;
            }
            if (currentSession != null) {
                refreshSession(currentSession);
            }
        }
    }

    protected void refreshSession(CoreSession session)
            throws OperationException {
        if (session != null && !session.isStateSharedByAllThreadSessions()) {
            try {
                // this will indirectly process refresh the session
                session.save();
            } catch (Exception e) {
                throw new OperationException(e);
            }
        }
    }

    /**
     * Remove the stacked logins if any. This is called when chain execution is
     * done.
     * 
     */
    protected void clear() throws OperationException {
        if (!stack.isEmpty()) {
            for (int i = stack.size() - 1; i > -1; i--) {
                stack.get(i).dispose();
            }
            stack.clear();
            currentSession = originalSession;
            if (currentSession != null) {
                refreshSession(currentSession);
            }
            stack.clear();
        }
    }

    public static class Entry {
        public LoginContext lc;

        public CoreSession session;

        public Entry(LoginContext lc) {
            this(lc, null);
        }

        public Entry(LoginContext lc, CoreSession session) {
            this.lc = lc;
            this.session = session;
        }

        public final boolean hasSession() {
            return session != null;
        }

        public final void dispose() throws OperationException {
            try {
                if (session != null) {
                    session.save();
                    Repository.close(session);
                }
            } catch (Exception e) {
                throw new OperationException(e);
            } finally {
                try {
                    session = null;
                    lc.logout();
                    lc = null;
                } catch (Exception e) {
                    throw new OperationException(e);
                }
            }
        }
    }

}
