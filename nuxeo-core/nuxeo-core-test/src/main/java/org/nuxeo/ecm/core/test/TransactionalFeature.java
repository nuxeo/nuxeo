/*
 * Copyright (c) 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Stephane Lacoin
 */
package org.nuxeo.ecm.core.test;

import org.apache.log4j.Logger;
import org.nuxeo.ecm.core.repository.RepositoryFactory;
import org.nuxeo.ecm.core.storage.sql.ra.PoolingRepositoryFactory;
import org.nuxeo.ecm.core.test.annotations.Granularity;
import org.nuxeo.ecm.core.test.annotations.RepositoryConfig;
import org.nuxeo.ecm.core.test.annotations.TransactionalConfig;
import org.nuxeo.runtime.jtajca.NuxeoContainer;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.SimpleFeature;
import org.nuxeo.runtime.transaction.TransactionHelper;

@Deploy({ "org.nuxeo.runtime.jtajca" })
@RepositoryConfig(cleanup=Granularity.METHOD, repositoryFactoryClass=PoolingRepositoryFactory.class)
public class TransactionalFeature extends SimpleFeature {

    protected TransactionalConfig config;

    protected String autoactivationValue;

    protected boolean nsOwner;

    protected boolean txStarted;

    protected Class<? extends RepositoryFactory> defaultFactory;

    @Override
    public void initialize(FeaturesRunner runner) throws Exception {
        config = runner.getConfig(TransactionalConfig.class);
    }

    @Override
    public void start(FeaturesRunner runner) throws Exception {
        if (!NuxeoContainer.isInstalled()) { // before runtime start (lazy loading)
            NuxeoContainer.install();
            nsOwner = true;
        }
    }

    @Override
    public void stop(FeaturesRunner runner) throws Exception {
        if (nsOwner) {
            nsOwner = false;
            NuxeoContainer.uninstall(); // before runtime shutdown
        }
    }

    @Override
    public void beforeSetup(FeaturesRunner runner) throws Exception {
        if (config.autoStart() == false) {
            return;
        }
        txStarted = TransactionHelper.startTransaction();
    }

    @Override
    public void afterTeardown(FeaturesRunner runner) throws Exception {
        if (txStarted == false) {
            if (TransactionHelper.isTransactionActive()) {
                try {
                TransactionHelper.setTransactionRollbackOnly();
                TransactionHelper.commitOrRollbackTransaction();
                } finally {
                    Logger.getLogger(TransactionalFeature.class).warn("Committing a transaction for your, please do it yourself");
                }
            }
            return;
        }
        TransactionHelper.commitOrRollbackTransaction();
    }

}
