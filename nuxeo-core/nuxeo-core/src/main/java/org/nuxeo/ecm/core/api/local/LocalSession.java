/*
 * (C) Copyright 2006-2015 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Bogdan Stefanescu
 *     Florent Guillaume
 */

package org.nuxeo.ecm.core.api.local;

import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

import javax.transaction.Synchronization;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.AbstractSession;
import org.nuxeo.ecm.core.api.CoreInstance;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.ecm.core.model.Session;
import org.nuxeo.ecm.core.repository.RepositoryService;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.transaction.TransactionHelper;

/**
 * Local Session: implementation of {@link CoreSession} beyond {@link AbstractSession}, dealing with low-level stuff.
 */
public class LocalSession extends AbstractSession implements Synchronization {

    private static final long serialVersionUID = 1L;

    private static final AtomicLong SID_COUNTER = new AtomicLong();

    private static final Log log = LogFactory.getLog(LocalSession.class);

    protected String repositoryName;

    protected NuxeoPrincipal principal;

    /** Defined once at connect time. */
    private String sessionId;

    /**
     * Thread-local session allocated.
     */
    private final ThreadLocal<SessionInfo> sessionHolder = new ThreadLocal<>();

    /**
     * All sessions allocated in all threads, in order to detect close leaks.
     */
    private final Set<SessionInfo> allSessions = Collections.newSetFromMap(new ConcurrentHashMap<SessionInfo, Boolean>());

    public LocalSession(String repositoryName, NuxeoPrincipal principal) {
        if (!TransactionHelper.isTransactionActiveOrMarkedRollback()) {
            throw new NuxeoException("Cannot create a CoreSession outside a transaction");
        }
        this.repositoryName = repositoryName;
        this.principal = principal;
        createMetrics(); // needs repo name
        sessionId = newSessionId(repositoryName, principal);
        if (log.isDebugEnabled()) {
            log.debug("Creating CoreSession: " + sessionId);
        }
        createSession(); // create first session for current thread
    }

    @Override
    public String getRepositoryName() {
        return repositoryName;
    }

    protected static String newSessionId(String repositoryName, NuxeoPrincipal principal) {
        return repositoryName + '/' + principal.getName() + '#' + SID_COUNTER.incrementAndGet();
    }

    @Override
    public String getSessionId() {
        return sessionId;
    }

    @Override
    public Session getSession() {
        if (!TransactionHelper.isTransactionActiveOrMarkedRollback()) {
            throw new NuxeoException("Cannot use a CoreSession outside a transaction");
        }
        SessionInfo si = sessionHolder.get();
        if (si == null || !si.session.isLive()) {
            // close old one, previously completed
            closeInThisThread();
            if (log.isDebugEnabled()) {
                log.debug("Reconnecting CoreSession: " + sessionId);
            }
            si = createSession();
        }
        return si.session;
    }

    /**
     * Creates the session. It will be destroyed by calling {@link #destroy}.
     */
    protected SessionInfo createSession() {
        RepositoryService repositoryService = Framework.getLocalService(RepositoryService.class);
        Session session = repositoryService.getSession(repositoryName);
        TransactionHelper.registerSynchronization(this);
        SessionInfo si = new SessionInfo(session);
        sessionHolder.set(si);
        allSessions.add(si);
        if (log.isDebugEnabled()) {
            log.debug("Adding thread " + Thread.currentThread().getName() + " for CoreSession: " + sessionId);
        }
        return si;
    }

    @Override
    public boolean isLive(boolean onThread) {
        if (!onThread) {
            return !allSessions.isEmpty();
        }
        return sessionHolder.get() != null;
    }

    @Override
    public void close() {
        CoreInstance.closeCoreSession(this); // calls back destroy()
    }

    @Override
    public void beforeCompletion() {
        // insure the connection is closed before commit
        closeInThisThread();
    }

    @Override
    public void afterCompletion(int status) {
    }

    protected void closeInThisThread() {
        SessionInfo si = sessionHolder.get();
        if (si == null) {
            return;
        }
        if (log.isDebugEnabled()) {
            log.debug("Removing thread " + Thread.currentThread().getName() + " for CoreSession: " + sessionId);
        }
        try {
            si.session.close();
        } finally {
            sessionHolder.remove();
            allSessions.remove(si);
        }
    }

    // explicit close()
    @Override
    public void destroy() {
        if (log.isDebugEnabled()) {
            log.debug("Destroying CoreSession: " + sessionId);
        }
        closeInThisThread();
    }

    @Override
    public NuxeoPrincipal getPrincipal() {
        return principal;
    }

    @Override
    public boolean isStateSharedByAllThreadSessions() {
        // by design we always share state when in the same thread (through the sessionHolder ThreadLocal)
        return true;
    }

}
