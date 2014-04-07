/*
 * Copyright (c) 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
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

import javax.naming.NamingException;
import javax.transaction.RollbackException;
import javax.transaction.Synchronization;
import javax.transaction.SystemException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.AbstractSession;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.ClientRuntimeException;
import org.nuxeo.ecm.core.api.CoreInstance;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentException;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.ecm.core.api.TransactionalCoreSessionWrapper;
import org.nuxeo.ecm.core.model.Repository;
import org.nuxeo.ecm.core.model.Session;
import org.nuxeo.ecm.core.repository.RepositoryService;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.transaction.TransactionHelper;

/**
 * Local Session: implementation of {@link CoreSession} beyond
 * {@link AbstractSession}, dealing with low-level stuff.
 */
public class LocalSession extends AbstractSession implements Synchronization {

    private static final long serialVersionUID = 1L;

    private static final AtomicLong SID_COUNTER = new AtomicLong();

    private static final Log log = LogFactory.getLog(LocalSession.class);

    protected String repositoryName;

    protected NuxeoPrincipal principal;

    /** Defined once at connect time. */
    private String sessionId;

    public static final class SessionInfo {
        private final Session session;

        private Exception openException;

        public SessionInfo(Session session) {
            this.session = session;
            openException = new Exception("Open stack trace for "
                    + session.getSessionId() + " in thread "
                    + Thread.currentThread().getName());
        }
    }

    /**
     * Thread-local sessions allocated.
     */
    private final ThreadLocal<SessionInfo> threadSessions = new ThreadLocal<SessionInfo>();

    /**
     * All sessions allocated in all threads, in order to detect close leaks.
     */
    private final Set<SessionInfo> allSessions = Collections.newSetFromMap(new ConcurrentHashMap<SessionInfo, Boolean>());

    public static CoreSession createInstance() {
        return TransactionalCoreSessionWrapper.wrap(new LocalSession());
    }

    @Override
    public String getRepositoryName() {
        return repositoryName;
    }

    @Override
    public void connect(String repositoryName, NuxeoPrincipal principal)
            throws ClientException {
        if (sessionId != null) {
            throw new ClientException("CoreSession already connected");
        }
        this.repositoryName = repositoryName;
        this.principal = principal;
        createMetrics(); // needs repo name
        sessionId = newSessionId(repositoryName, principal);
        log.debug("Creating CoreSession: " + sessionId);
        createSession(); // create first session for current thread
    }

    protected static String newSessionId(String repositoryName,
            NuxeoPrincipal principal) {
        return repositoryName + '/' + principal.getName() + '#'
                + SID_COUNTER.incrementAndGet();
    }

    @Override
    public String getSessionId() {
        return sessionId;
    }

    @Override
    public Session getSession() {
        SessionInfo si = threadSessions.get();
        if (si == null || !si.session.isLive()) {
            // close old one, previously completed
            closeInThisThread();
            log.debug("Reconnecting CoreSession: " + sessionId);
            if (!TransactionHelper.isTransactionActive()) {
                throw new ClientRuntimeException(
                        "No transaction, cannot reconnect: " + sessionId);
            }
            try {
                TransactionHelper.lookupTransactionManager().getTransaction().registerSynchronization(
                        this);
            } catch (NamingException | SystemException | RollbackException e) {
                throw new ClientRuntimeException(
                        "Cannot register synchronization", e);
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
        Repository repository = repositoryService.getRepository(repositoryName);
        if (repository == null) {
            throw new ClientRuntimeException("No such repository: "
                    + repositoryName);
        }
        Session session;
        try {
            session = repository.getSession(principal, sessionId);
        } catch (DocumentException e) {
            throw new ClientRuntimeException("Failed to load repository "
                    + repositoryName + ": " + e.getMessage(), e);
        }
        SessionInfo si = new SessionInfo(session);
        threadSessions.set(si);
        allSessions.add(si);
        log.debug("Adding thread " + Thread.currentThread().getName()
                + " for CoreSession: " + sessionId);
        return si;
    }

    @Override
    public void close() {
        CoreInstance.closeCoreSession(this); // calls back destroy()
    }

    @Override
    public void beforeCompletion() {
    }

    /**
     * Synchronization registered only when reconnecting a session, because
     * {@link #close} will not be called explicitly.
     */
    @Override
    public void afterCompletion(int status) {
        closeInThisThread();
    }

    protected void closeInThisThread() {
        SessionInfo si = threadSessions.get();
        if (si != null) {
            si.session.close();
            threadSessions.remove();
            allSessions.remove(si);
            log.debug("Removing thread " + Thread.currentThread().getName()
                    + " for CoreSession: " + sessionId);
        }
    }

    // explicit close()
    @Override
    public void destroy() {
        log.debug("Closing CoreSession: " + sessionId);
        int size = allSessions.size();
        if (size > 1) {
            // multiple sessions
            Exception closeException = new Exception("Close stack trace for "
                    + sessionId + " in thread "
                    + Thread.currentThread().getName());
            log.warn("At close time there are still " + size
                    + " Session objects."
                    + " Dumping close() then open() stack traces.",
                    closeException);
            for (SessionInfo si : allSessions) {
                log.warn("Session open at", si.openException);
            }
        }
        closeInThisThread();
        if (!allSessions.isEmpty()) {
            // close leaks previously logged
            for (SessionInfo si : allSessions) {
                si.session.close();
                si.openException = null;
            }
            allSessions.clear();
        }
    }

    @Override
    public NuxeoPrincipal getPrincipal() {
        return principal;
    }

    @Override
    public boolean isStateSharedByAllThreadSessions() {
        return getSession().isStateSharedByAllThreadSessions();
    }

}
