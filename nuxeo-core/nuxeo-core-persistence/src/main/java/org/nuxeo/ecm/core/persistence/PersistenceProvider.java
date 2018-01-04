/*
 * (C) Copyright 2006-2008 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     "Stephane Lacoin (aka matic) <slacoin@nuxeo.org>"
 */
package org.nuxeo.ecm.core.persistence;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.EntityTransaction;
import javax.persistence.TransactionRequiredException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * @author "Stephane Lacoin (aka matic) <slacoin@nuxeo.org>"
 */
public class PersistenceProvider {

    protected static final Log log = LogFactory.getLog(PersistenceProvider.class);

    protected volatile EntityManagerFactory emf;

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
        T runWith(EntityManager em);
    }

    public <T> T run(Boolean needActiveSession, RunCallback<T> callback) {
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
        void runWith(EntityManager em);
    }

    public void run(Boolean needActiveSession, RunVoid callback) {
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
