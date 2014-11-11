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

package org.nuxeo.ecm.core.storage.mongodb;

import org.junit.After;
import org.nuxeo.common.Environment;
import org.nuxeo.runtime.transaction.TransactionHelper;

/**
 * Transactional TestCase.
 * <p>
 * The tests are run with a session that is open in a transaction.
 */
public abstract class MongoDBRepositoryTXTestCase extends
        MongoDBRepositoryTestCase {

    @Override
    protected void initRepository() throws Exception {
        deployBundle("org.nuxeo.runtime.jtajca");
        super.initRepository();
    }

    @Override
    protected void setUpTx() throws Exception {
        super.setUpTx();
        Environment.getDefault().setHostApplicationName(
                Environment.NXSERVER_HOST);
        fireFrameworkStarted();
        TransactionHelper.startTransaction();
    }

    protected boolean hasPoolingConfig() {
        return true;
    }

    @Override
    @After
    public void tearDown() throws Exception {
        try {
            if (session != null) {
                session.cancel();
                closeSession();
            }
            if (TransactionHelper.isTransactionActiveOrMarkedRollback()) {
                TransactionHelper.setTransactionRollbackOnly();
                TransactionHelper.commitOrRollbackTransaction();
            }
        } finally {
            super.tearDown();
        }
    }

}
