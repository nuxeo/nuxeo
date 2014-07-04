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

import javax.transaction.Synchronization;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.AbstractSession;
import org.nuxeo.ecm.core.api.ClientException;
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

    /**
     * Thread-local session allocated.
     */
    private final ThreadLocal<SessionInfo> sessionHolder = new ThreadLocal<SessionInfo>();

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
            throw new LocalException("CoreSession already connected");
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
        SessionInfo si = sessionHolder.get();
        if (si == null || !si.session.isLive()) {
            // close old one, previously completed
            closeInThisThread();
            if (!TransactionHelper.isTransactionActive()) {
                throw new LocalException(
                        "No transaction active, cannot reconnect: " + sessionId);
            }
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
        Repository repository = repositoryService.getRepository(repositoryName);
        if (repository == null) {
            throw new LocalException("No such repository: "
                    + repositoryName);
        }
        Session session;
        try {
            session = repository.getSession(sessionId);
        } catch (DocumentException e) {
            throw new LocalException("Failed to load repository "
                    + repositoryName + ": " + e.getMessage(), e);
        }
        TransactionHelper.registerSynchronization(this);
        SessionInfo si = new SessionInfo(session);
        sessionHolder.set(si);
        allSessions.add(si);
        if (log.isDebugEnabled()) {
            log.debug("Adding thread " + Thread.currentThread().getName()
                    + " for CoreSession: " + sessionId);
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
            log.debug("Removing thread " + Thread.currentThread().getName()
                    + " for CoreSession: " + sessionId);
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
        return getSession().isStateSharedByAllThreadSessions();
    }

}
