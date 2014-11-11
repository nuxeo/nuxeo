/*
 * Copyright (c) 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Florent Guillaume, jcarsique
 */

package org.nuxeo.ecm.core.storage.sql;

import org.junit.Ignore;
import org.nuxeo.common.Environment;
import org.nuxeo.runtime.api.ConnectionHelper;
import org.nuxeo.runtime.jtajca.NuxeoContainer;
import org.nuxeo.runtime.transaction.TransactionHelper;

/**
 * Transactional SQL Repository TestCase.
 * <p>
 * The tests are run with a session that is open in a transaction.
 */
public abstract class TXSQLRepositoryTestCase extends SQLRepositoryTestCase {

    @Override
    public void setUp() throws Exception {
        super.setUp(); // calls deployRepositoryConfig()
        Environment.getDefault().setHostApplicationName(
                Environment.NXSERVER_HOST);
        fireFrameworkStarted();
        TransactionHelper.startTransaction();
        openSession();
    }

    /** Can be subclassed to instantiate specific pool config. */
    protected void setUpContainer() throws Exception {
        NuxeoContainer.install();
    }

    /**
     * Overridden to use a pooling configuration.
     */
    @Override
    protected void deployRepositoryContrib() throws Exception {
        setUpContainer();
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

    protected boolean hasPoolingConfig() {
        return database instanceof DatabaseH2
                || database instanceof DatabasePostgreSQL;
    }

    protected boolean useSingleConnectionMode() {
        return ConnectionHelper.useSingleConnection(ConnectionHelper.getPseudoDataSourceNameForRepository(database.repositoryName));
    }

    @Override
    public void tearDown() throws Exception {
        try {
            session.cancel();
            closeSession();
            if (TransactionHelper.isTransactionActiveOrMarkedRollback()) {
                TransactionHelper.setTransactionRollbackOnly();
                TransactionHelper.commitOrRollbackTransaction();
            }
        } finally {
            if (NuxeoContainer.isInstalled()) {
                NuxeoContainer.uninstall();
            }
            super.tearDown();
        }
    }

}
