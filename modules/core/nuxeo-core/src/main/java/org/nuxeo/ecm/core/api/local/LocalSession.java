/*
 * (C) Copyright 2006-2020 Nuxeo (http://nuxeo.com/) and others.
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

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.transaction.Status;
import javax.transaction.Synchronization;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.AbstractSession;
import org.nuxeo.ecm.core.api.CloseableCoreSession;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentNotFoundException;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.ecm.core.model.Repository;
import org.nuxeo.ecm.core.model.Session;
import org.nuxeo.ecm.core.repository.RepositoryService;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.transaction.TransactionHelper;

/**
 * Local Session: implementation of {@link CoreSession} beyond {@link AbstractSession}, dealing with low-level stuff.
 */
public class LocalSession extends AbstractSession implements CloseableCoreSession, Synchronization {

    private static final long serialVersionUID = 1L;

    private static final Log log = LogFactory.getLog(LocalSession.class);

    protected String repositoryName;

    protected NuxeoPrincipal principal;

    /**
     * Thread-local sessions allocated, per repository.
     */
    private static final Map<String, ThreadLocal<Session<?>>> SESSIONS = new ConcurrentHashMap<>(1);

    public LocalSession(String repositoryName, NuxeoPrincipal principal) {
        if (TransactionHelper.isTransactionMarkedRollback()) {
            throw new NuxeoException("Cannot create a CoreSession when transaction is marked rollback-only");
        }
        if (!TransactionHelper.isTransactionActive()) {
            throw new NuxeoException("Cannot create a CoreSession outside a transaction");
        }
        this.repositoryName = repositoryName;
        this.principal = principal;
    }

    @Override
    public String getRepositoryName() {
        return repositoryName;
    }

    @Override
    public String getSessionId() {
        return toString();
    }

    @Override
    public String toString() {
        return repositoryName + "/" + principal;
    }

    @Override
    public Session<?> getSession() {
        if (!TransactionHelper.isTransactionActiveOrMarkedRollback()) {
            throw new NuxeoException("Cannot use a CoreSession outside a transaction");
        }
        TransactionHelper.checkTransactionTimeout();
        ThreadLocal<Session<?>> repoSessions = SESSIONS.computeIfAbsent(repositoryName, r -> new ThreadLocal<>());
        Session<?> session = repoSessions.get();
        if (session == null || !session.isLive()) {
            // close old one, previously completed
            closeInThisThread();
            if (log.isDebugEnabled()) {
                log.debug("Reconnecting CoreSession: " + toString());
            }
            if (TransactionHelper.isTransactionMarkedRollback()) {
                throw new NuxeoException("Cannot reconnect a CoreSession when transaction is marked rollback-only");
            }
            session = createSession();
            repoSessions.set(session);
            TransactionHelper.registerSynchronization(this);
        }
        return session;
    }

    /**
     * Creates the session.
     */
    protected Session<?> createSession() {
        RepositoryService repositoryService = Framework.getService(RepositoryService.class);
        Repository repository = repositoryService.getRepository(repositoryName);
        if (repository == null) {
            throw new DocumentNotFoundException("No such repository: " + repositoryName);
        }
        Session<?> session = repository.getSession();
        if (log.isDebugEnabled()) {
            log.debug("Adding thread " + Thread.currentThread().getName() + " for CoreSession: " + toString());
        }
        return session;
    }

    @Override
    public void close() {
        // nothing (the session holds no resources to close)
    }

    @Override
    public void beforeCompletion() {
        // insure the connection is closed before commit
        closeInThisThread();
    }

    @Override
    public void afterCompletion(int status) {
        if (status == Status.STATUS_ROLLEDBACK) {
            // insure the connection is closed on roll-back also
            closeInThisThread();
        }
    }

    protected void closeInThisThread() {
        ThreadLocal<Session<?>> repoSessions = SESSIONS.get(repositoryName);
        if (repoSessions == null) {
            // shouldn't happen
            return;
        }
        Session<?> session = repoSessions.get();
        if (session == null) {
            // already closed
            return;
        }
        if (log.isDebugEnabled()) {
            log.debug("Removing thread " + Thread.currentThread().getName() + " for CoreSession: " + repositoryName
                    + "/" + principal);
        }
        try {
            session.close();
        } finally {
            repoSessions.remove();
        }
    }

    @Override
    public void destroy() {
        // nothing (deprecated)
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
