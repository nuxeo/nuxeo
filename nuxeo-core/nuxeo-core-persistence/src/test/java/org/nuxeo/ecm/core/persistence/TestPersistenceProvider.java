/*
 * Copyright (c) 2013 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Florent Guillaume
 */
package org.nuxeo.ecm.core.persistence;

import java.util.Properties;

import javax.persistence.EntityManager;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.nuxeo.ecm.core.storage.sql.DatabaseHelper;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.datasource.ConnectionHelper;
import org.nuxeo.runtime.test.NXRuntimeTestCase;
import org.nuxeo.runtime.transaction.TransactionHelper;

public class TestPersistenceProvider extends NXRuntimeTestCase {

    protected PersistenceProvider persistenceProvider;

    // subclass to test single-datasource mode has this returning true
    protected boolean useSingleDataSource() {
        return false;
    }

    @Before
    @Override
    public void setUp() throws Exception {
        super.setUp();
        final Properties properties = Framework.getProperties();
        DatabaseHelper.DATABASE.setUp();
        if (useSingleDataSource()) {
            // the name doesn't actually matter, as code in
            // ConnectionHelper.getDataSource ignores it and uses
            // nuxeo.test.vcs.url etc. for connections in test mode
            properties.put(ConnectionHelper.SINGLE_DS, "jdbc/NuxeoTestDS");
        } else {
            properties.remove(ConnectionHelper.SINGLE_DS);
        }

        deployBundle("org.nuxeo.runtime.jtajca");
        deployBundle("org.nuxeo.runtime.datasource");
        deployBundle("org.nuxeo.ecm.core.persistence");
        deployContrib("org.nuxeo.ecm.core.persistence.test",
                "OSGI-INF/test-persistence-config.xml");
        fireFrameworkStarted();
        TransactionHelper.startTransaction();
        activatePersistenceProvider();
    }

    @After
    @Override
    public void tearDown() throws Exception {
        deactivatePersistenceProvider();
        if (TransactionHelper.isTransactionActiveOrMarkedRollback()) {
            TransactionHelper.setTransactionRollbackOnly();
            TransactionHelper.commitOrRollbackTransaction();
        }
        super.tearDown();
    }

    protected void activatePersistenceProvider() {
        ClassLoader last = Thread.currentThread().getContextClassLoader();
        try {
            // needs context classloader for Hibernate to find the
            // META-INF/persistence.xml file
            Thread.currentThread().setContextClassLoader(
                    getClass().getClassLoader());
            PersistenceProviderFactory persistenceProviderFactory = Framework.getLocalService(PersistenceProviderFactory.class);
            persistenceProvider = persistenceProviderFactory.newProvider("nxtest");
            persistenceProvider.openPersistenceUnit();
        } finally {
            Thread.currentThread().setContextClassLoader(last);
        }
    }

    protected void deactivatePersistenceProvider() {
        if (persistenceProvider != null) {
            persistenceProvider.closePersistenceUnit();
            persistenceProvider = null;
        }
    }

    @Test
    public void testBase() throws Exception {
        EntityManager em = persistenceProvider.acquireEntityManager();
        DummyEntity entity = new DummyEntity("1");
        em.persist(entity);
        em.flush();
        em.clear();
        em.close();
    }

    @Test
    public void testAcquireTwiceInSameTx() throws Exception {
        EntityManager em;
        em = persistenceProvider.acquireEntityManager();
        em.persist(new DummyEntity("1"));
        em.flush();
        em.clear();
        em.close();
        em = persistenceProvider.acquireEntityManager();
        em.persist(new DummyEntity("2"));
        em.flush();
        em.clear();
        em.close();
    }

}
