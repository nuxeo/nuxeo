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

import java.util.List;
import java.util.Properties;

import javax.persistence.EntityManager;
import javax.persistence.Query;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.datasource.ConnectionHelper;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.LocalDeploy;

@RunWith(FeaturesRunner.class)
@Features(CoreFeature.class) // to init properties for SQL datasources
@Deploy("org.nuxeo.ecm.core.persistence")
@LocalDeploy("org.nuxeo.ecm.core.persistence.test:OSGI-INF/test-persistence-config.xml")
public class TestPersistenceProvider {

    protected PersistenceProvider persistenceProvider;

    // subclass to test single-datasource mode has this returning true
    protected boolean useSingleDataSource() {
        return false;
    }

    @Before
    public void setUp() {
        final Properties properties = Framework.getProperties();
        if (useSingleDataSource()) {
            // the name doesn't actually matter, as code in
            // ConnectionHelper.getDataSource ignores it and uses
            // nuxeo.test.vcs.url etc. for connections in test mode
            properties.put(ConnectionHelper.SINGLE_DS, "jdbc/NuxeoTestDS");
        } else {
            properties.remove(ConnectionHelper.SINGLE_DS);
        }

        activatePersistenceProvider();
    }

    @After
    public void tearDown() {
        EntityManager em = persistenceProvider.acquireEntityManager();

        // clean all entities
        Query q = em.createQuery("select id from DummyEntity");
        @SuppressWarnings("unchecked")
        List<String> list = (List<String>) q.getResultList();
        for (String id : list) {
            DummyEntity entity = em.find(DummyEntity.class, id);
            em.remove(entity);
        }
        em.flush();
        em.clear();

        deactivatePersistenceProvider();
    }

    protected void activatePersistenceProvider() {
        ClassLoader last = Thread.currentThread().getContextClassLoader();
        try {
            // needs context classloader for Hibernate to find the
            // META-INF/persistence.xml file
            Thread.currentThread().setContextClassLoader(getClass().getClassLoader());
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
