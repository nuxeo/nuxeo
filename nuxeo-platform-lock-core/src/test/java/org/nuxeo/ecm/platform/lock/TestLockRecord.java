/*
 * (C) Copyright 2009 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     \Sun Seng David TAN (a.k.a. sunix) <stan@nuxeo.com>'
 */
package org.nuxeo.ecm.platform.lock;

import java.net.URI;
import java.net.URL;
import java.util.Calendar;

import javax.persistence.NoResultException;

import junit.framework.TestCase;

import org.nuxeo.ecm.core.persistence.HibernateConfiguration;
import org.nuxeo.ecm.core.persistence.PersistenceProvider;

/**
 * @author Sun Seng David TAN (a.k.a. sunix) <stan@nuxeo.com>
 * 
 */
public class TestLockRecord extends TestCase {

    LockRecordProvider provider;

    PersistenceProvider persistenceProvider;

    @Override
    public void setUp() throws Exception {
        super.setUp();
        URL resource = getClass().getResource("/hibernate-tests.xml");
        HibernateConfiguration config = HibernateConfiguration.load(resource);
        persistenceProvider = new PersistenceProvider(config);
        persistenceProvider.openPersistenceUnit();

        // The lockrecord provider will manage his own manager
        // entityManager =
        // persistenceProvider.acquireEntityManagerWithActiveTransaction();
        provider = new LockRecordProvider() {
            @Override
            public PersistenceProvider getOrCreatePersistenceProvider() {

                return persistenceProvider;

            }
        };

    }

    /**
     * Simple test of the lock record provider: insert, retrieve, remove.
     * 
     * @throws Exception
     */
    public void testLockRecordProvider() throws Exception {

        LockRecordProvider provider = new LockRecordProvider();

        URI owner = new URI("nxlockcompetitor://user@server");
        URI resourceUri = new URI("nxlockresource://server/11111111");

        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.MINUTE, 2);
        LockRecord record = provider.createRecord(owner, resourceUri, "test",
                2000);
        LockRecord retrievedRecord = provider.getRecord(resourceUri);
        assertEquals(record.resource, retrievedRecord.resource);

        provider.delete(resourceUri);

        NoResultException noResultException = null;
        try {
            provider.getRecord(resourceUri);
        } catch (NoResultException e) {
            noResultException = e;
        }
        assertNotNull(noResultException);
    }

}
