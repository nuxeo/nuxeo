/*******************************************************************************
 *  (C) Copyright 2013 Nuxeo SAS (http://nuxeo.com/) and contributors.
 *  
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the GNU Lesser General Public License
 *  (LGPL) version 2.1 which accompanies this distribution, and is available at
 *  http://www.gnu.org/licenses/lgpl.html
 *  
 *  This library is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 *  Lesser General Public License for more details.
 *******************************************************************************/
package org.nuxeo.ecm.core.api.critical;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.util.Calendar;

import org.apache.commons.logging.LogFactory;
import org.nuxeo.common.utils.Path;
import org.nuxeo.ecm.automation.core.operations.AtomicFolderCreator;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.ClientRuntimeException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentException;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.DocumentResolver;
import org.nuxeo.ecm.core.api.Lock;
import org.nuxeo.ecm.core.api.PathRef;
import org.nuxeo.ecm.core.api.TransactionalCoreSessionWrapper;
import org.nuxeo.ecm.core.api.UnrestrictedSessionRunner;
import org.nuxeo.ecm.core.api.local.LocalSession;
import org.nuxeo.ecm.core.model.Document;
import org.nuxeo.ecm.core.model.Session;
import org.nuxeo.runtime.transaction.TransactionHelper;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;

/**
 * Allow running a critical section of code against a repository. The abstract
 * {@link CriticalSectionRunner#enter} method is the place where the critical
 * section of code should be implemented. The abstract method
 * {@link CriticalSectionRunner#lockTarget} method defines the entry in the
 * repository that protect the critical section.
 * 
 * A typical use case of the runner is the {@link AtomicFolderCreator}.
 * 
 * Note that before entering the critical section, the calling transaction is
 * always committed. Note also that a new transaction is injected at the
 * critical section exit.
 * 
 * @since 5.7
 * @author "Stephane Lacoin at Nuxeo (aka matic)"
 * 
 */
public abstract class CriticalSectionRunner extends UnrestrictedSessionRunner {

    public CriticalSectionRunner(CoreSession session) throws ClientException {
        super(session);
    }

    protected static final Multimap<String, CriticalSectionRunner> pausedCreators = HashMultimap.create();

    protected static Field handlerField = getHandlerField();

    protected static Field getHandlerField() {
        Field field;
        try {
            field = TransactionalCoreSessionWrapper.class.getDeclaredField("session");
        } catch (Exception e) {
            return null;
        }
        field.setAccessible(true);
        return field;
    }

    protected static Session unwrapSession(CoreSession session)
            throws ClientException {
        if (Proxy.isProxyClass(session.getClass())) {
            InvocationHandler handler = Proxy.getInvocationHandler(session);
            if (handler instanceof TransactionalCoreSessionWrapper) {
                if (handlerField != null) {
                    try {
                        session = (CoreSession) handlerField.get(handler);
                    } catch (IllegalAccessException e) {
                        ;
                    }
                }
            }
        }
        if (!(session instanceof LocalSession)) {
            throw new ClientException("Not a local session "
                    + session.getClass());
        }
        return ((LocalSession) session).getSession();
    }

    @Override
    public void run() throws ClientException {
        String path = lock();
        try {
            LogFactory.getLog(this.toString()).trace(
                    "Entering critical section");
            enter(new PathRef(path));
        } finally {
            unlock(path);
        }
    }

    protected static void resetTransaction(CoreSession session) {
        try {
            TransactionHelper.commitOrRollbackTransaction();
            unwrapSession(session).dispose();
            TransactionHelper.startTransaction();
            session.getRootDocument();
        } catch (ClientException e) {
            throw new ClientRuntimeException(
                    "Cannot reset transaction context", e);
        }
    }

    protected void unlock(String path) {
        try {
            resetTransaction(session);
            Session repo = unwrapSession(session);
            Document doc = DocumentResolver.resolveReference(repo, new PathRef(
                    path));
            Lock lock = doc.removeLock(key());
            if (lock == null || lock.getFailed()) {
                throw new ClientRuntimeException("Cannot unlock " + path);
            }
        } catch (Exception e) {
            throw new ClientRuntimeException(
                    "should not occur, cannot remove lock on " + path, e);
        } finally {
            synchronized (pausedCreators) {
                for (CriticalSectionRunner paused : pausedCreators.removeAll(path)) {
                    synchronized (paused) {
                        paused.notify();
                    }
                }
            }
        }
    }

    protected String lock() throws ClientException {
        while (true) {
            try {
                return lock(lockTarget()); // locked
            } catch (AlreadyLockedException e) {
                trace("Already locked, paused");
                synchronized (pausedCreators) {
                    pausedCreators.put(e.path.toString(), this);
                }
                synchronized (this) {
                    try {
                        this.wait();
                    } catch (InterruptedException e1) {
                        throw new ClientException(
                                "Interrupted, cannot synchronize with other threads",
                                e);
                    }
                }
            } catch (DocumentException e) {
                throw new ClientException("Cannot lock target", e);
            } finally {
                resetTransaction(session); // refresh point of view
            }
        }
    }

    protected static class AlreadyLockedException extends Exception {

        private static final long serialVersionUID = 1L;

        protected final String path;

        protected AlreadyLockedException(String path) {
            this.path = path;
        }
    }

    protected String lock(DocumentRef ref) throws AlreadyLockedException,
            DocumentException, ClientException {
        Session repoSession = unwrapSession(session);
        Document doc = DocumentResolver.resolveReference(repoSession, ref);
        String path = doc.getPath();
        String key = key();
        Lock lock = new Lock(key, Calendar.getInstance());
        try {
            Lock oldLock = doc.setLock(lock);
            if (oldLock != null) {
                if (key.equals(oldLock.getOwner())) {
                    throw new AlreadyLockedException(doc.getPath());
                }
                throw new ClientException("Document already locked by "
                        + oldLock.getOwner());
            }
        } catch (DocumentException e) {
            throw new ClientRuntimeException("Cannot lock " + ref, e);
        }
        trace("locked " + doc.getPath());
        return path;
    }

    protected Path tryLock(Path path) throws ClientException,
            AlreadyLockedException {
        PathRef ref = new PathRef(path.toString());
        if (!session.exists(ref)) {
            return tryLock(path.removeLastSegments(1));
        }
        return path;
    }

    protected void trace(String message) {
        LogFactory.getLog(this.toString()).trace(message);
    }

    protected abstract DocumentRef lockTarget() throws ClientException;

    protected String key() {
        return getClass().getSimpleName();
    }

    protected abstract void enter(DocumentRef lockedRef) throws ClientException;
}
