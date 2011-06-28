/*
 * (C) Copyright 2008-2010 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     Florent Guillaume, jcarsique
 */

package org.nuxeo.ecm.core.storage.sql;

import org.nuxeo.common.jndi.NamingContextFactory;
import org.nuxeo.runtime.jtajca.NuxeoContainer;
import org.nuxeo.runtime.transaction.TransactionHelper;

/**
 * Transactional SQL Repository TestCase.
 * <p>
 * The tests are run with a session that is open in a transaction.
 */
public class TXSQLRepositoryTestCase extends SQLRepositoryTestCase {

    protected void initJTAJCA() throws Exception {
        NuxeoContainer.initTransactionManagement();
    }

    /**
     * Overridden to use a pooling configuration.
     */
    @Override
    protected void deployRepositoryContrib() throws Exception {
        if (database instanceof DatabaseH2) {
            String contrib = "OSGI-INF/test-pooling-h2-contrib.xml";
            deployContrib("org.nuxeo.ecm.core.storage.sql.test", contrib);
        } else if (database instanceof DatabasePostgreSQL) {
            String contrib = "OSGI-INF/test-pooling-postgres-contrib.xml";
            deployContrib("org.nuxeo.ecm.core.storage.sql.test", contrib);
        } else {
            super.deployRepositoryContrib();
        }
    }

    @Override
    public void setUp() throws Exception {
        NamingContextFactory.setAsInitial();
        initJTAJCA();
        super.setUp(); // calls deployRepositoryConfig()
        deployBundle("org.nuxeo.runtime.jtajca");
        TransactionHelper.startTransaction();
        openSession();
    }

    @Override
    public void tearDown() throws Exception {
        session.cancel();
        closeSession();
        if (TransactionHelper.isTransactionActiveOrMarkedRollback()) {
            TransactionHelper.setTransactionRollbackOnly();
            TransactionHelper.commitOrRollbackTransaction();
        }
        super.tearDown();
        NamingContextFactory.revertSetAsInitial();
    }

}
