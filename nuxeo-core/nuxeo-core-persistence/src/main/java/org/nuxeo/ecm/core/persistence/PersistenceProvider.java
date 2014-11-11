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
 *     "Stephane Lacoin (aka matic) <slacoin@nuxeo.org>"
 */
package org.nuxeo.ecm.core.persistence;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.EntityTransaction;
import javax.persistence.TransactionRequiredException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.ClientException;

/**
 * @author "Stephane Lacoin (aka matic) <slacoin@nuxeo.org>"
 */
public class PersistenceProvider {

    protected static final Log log = LogFactory.getLog(PersistenceProvider.class);

    protected EntityManagerFactory emf;

    protected final EntityManagerFactoryProvider emfProvider;

    public PersistenceProvider(EntityManagerFactoryProvider emfProvider) {
        this.emfProvider = emfProvider;
    }

    public void openPersistenceUnit() {
        if (emfProvider == null) {
            throw new IllegalArgumentException("emfProvider not set");
        }
        if (emf == null) {
            synchronized (PersistenceProvider.class) {
                if (emf == null) {
                    emf = emfProvider.getFactory();
                }
            }
        }
    }

    public void closePersistenceUnit() {
        if (emf == null) {
            return;
        }
        if (emf.isOpen()) {
            emf.close();
        }
        emf = null;
    }

    protected EntityManager doAcquireEntityManager() {
        if (emf == null) {
            openPersistenceUnit();
        }
        return emf.createEntityManager();
    }

    protected EntityTransaction getTransaction(EntityManager em) {
        try {
            return em.getTransaction();
        } catch (IllegalStateException e) {
            return null; // JTA container, no manual access to transaction
        }
    }

    public EntityManager acquireEntityManager() {
        return doAcquireEntityManager();
    }

    public EntityManager acquireEntityManagerWithActiveTransaction() {
        EntityManager em = doAcquireEntityManager();
        doBegin(em);
        return em;
    }

    protected void doBegin(EntityManager em) {
        EntityTransaction et = getTransaction(em);
        if (et != null) {
            et.begin();
        }
    }

    protected void doCommit(EntityManager em) {
        try {
            em.flush();
        } catch (TransactionRequiredException e) {
            // ignore
        }
        EntityTransaction et = getTransaction(em);
        if (et == null || !et.isActive()) {
            return;
        }
        et.commit();
    }

    protected void doRollback(EntityManager em) {
        try {
            em.flush();
        } catch (TransactionRequiredException e) {
            // ignore
        }
        EntityTransaction et = getTransaction(em);
        if (et == null || !et.isActive()) {
            return;
        }
        et.rollback();
    }

    protected void releaseEntityManager(EntityManager em) {
        if (!em.isOpen()) {
            return;
        }
        try {
            doCommit(em);
        } finally {
            if (em.isOpen()) {
                em.clear();
                em.close();
            }
        }
    }

    public void releaseEntityManagerWithRollback(EntityManager em) {
        if (!em.isOpen()) {
            return;
        }
        try {
            doRollback(em);
        } finally {
            if (em.isOpen()) {
                em.clear();
                em.close();
            }
        }
    }

    public interface RunCallback<T> {
        T runWith(EntityManager em) throws ClientException;
    }

    public <T> T run(Boolean needActiveSession, RunCallback<T> callback)
            throws ClientException {
        // needActiveSession now unused
        Thread myThread = Thread.currentThread();
        ClassLoader lastLoader = myThread.getContextClassLoader();
        myThread.setContextClassLoader(getClass().getClassLoader());
        try { // insure context class loader restoring
            EntityManager em = doAcquireEntityManager();
            doBegin(em);
            try { // insure entity manager releasing
                return callback.runWith(em);
            } finally {
                releaseEntityManager(em);
            }
        } finally {
            myThread.setContextClassLoader(lastLoader);
        }
    }

    public interface RunVoid {
        void runWith(EntityManager em) throws ClientException;
    }

    public void run(Boolean needActiveSession, RunVoid callback)
            throws ClientException {
        // needActiveSession now unused
        Thread myThread = Thread.currentThread();
        ClassLoader lastLoader = myThread.getContextClassLoader();
        myThread.setContextClassLoader(getClass().getClassLoader());
        try { // insure context class loader restoring
            EntityManager em = doAcquireEntityManager();
            doBegin(em);
            try { // insure entity manager releasing
                callback.runWith(em);
            } finally {
                releaseEntityManager(em);
            }
        } finally {
            myThread.setContextClassLoader(lastLoader);
        }
    }

}
