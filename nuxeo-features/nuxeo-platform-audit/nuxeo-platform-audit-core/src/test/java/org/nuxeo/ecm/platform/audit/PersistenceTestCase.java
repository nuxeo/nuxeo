/*
 * (C) Copyright 2006-2009 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Stephane Lacoin
 */

package org.nuxeo.ecm.platform.audit;

import java.net.URL;

import javax.persistence.EntityManager;

import junit.framework.TestCase;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.persistence.HibernateConfiguration;
import org.nuxeo.ecm.core.persistence.PersistenceProvider;

/**
 * Base class for persistence
 *
 * @author Stephane Lacoin (Nuxeo EP Software Engineer)
 */
public abstract class PersistenceTestCase extends TestCase {

    protected static final Log log = LogFactory.getLog(PersistenceTestCase.class);

    protected PersistenceProvider persistenceProvider;

    protected EntityManager entityManager;

    protected void handleBeforeSetup(HibernateConfiguration config) {
    }

    protected void handleAfterSetup(EntityManager entityManager) {
    }

    @Override
    public void setUp() throws Exception {
        super.setUp();
        URL resource = getClass().getResource("/hibernate-tests.xml");
        HibernateConfiguration config = HibernateConfiguration.load(resource);
        persistenceProvider = new PersistenceProvider(config);
        handleBeforeSetup(config);
        persistenceProvider.openPersistenceUnit();
        entityManager = persistenceProvider.acquireEntityManagerWithActiveTransaction();
        handleAfterSetup(entityManager);
    }

    @Override
    public void tearDown() {
        persistenceProvider.releaseEntityManagerWithRollback(entityManager);
    }

}
