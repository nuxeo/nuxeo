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

import java.io.File;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.apache.commons.logging.LogFactory;
import org.apache.log4j.Logger;
import org.junit.Assert;
import org.nuxeo.ecm.core.repository.RepositoryFactory;
import org.nuxeo.ecm.core.test.annotations.Granularity;
import org.nuxeo.ecm.core.test.annotations.RepositoryConfig;
import org.nuxeo.ecm.core.test.annotations.TransactionalConfig;
import org.nuxeo.runtime.management.jvm.ThreadDeadlocksDetector;
import org.nuxeo.runtime.test.runner.ContainerFeature;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.SimpleFeature;
import org.nuxeo.runtime.transaction.TransactionHelper;

@RepositoryConfig(cleanup = Granularity.METHOD)
@Features(ContainerFeature.class)
public class TransactionalFeature extends SimpleFeature {

    protected TransactionalConfig config;

    protected String autoactivationValue;

    protected boolean nsOwner;

    protected boolean txStarted;

    final List<Waiter> waiters = new LinkedList<>();

    public interface Waiter {
        boolean await(long deadline) throws InterruptedException;
    }

    public void addWaiter(Waiter waiter) {
        waiters.add(waiter);
    }

    public void nextTransaction() {
        nextTransaction(2, TimeUnit.MINUTES);
    }

    public void nextTransaction(long duration, TimeUnit unit) {
        long deadline = System.currentTimeMillis() + TimeUnit.MILLISECONDS.convert(duration, unit);
        boolean tx = TransactionHelper.isTransactionActive();
        boolean rb = TransactionHelper.isTransactionMarkedRollback();
        if (tx || rb) {
            // there may be tx synchronizer pending, so we
            // have to commit the transaction
            TransactionHelper.commitOrRollbackTransaction();
        }
        try {
            for (Waiter provider : waiters) {
                try {
                    Assert.assertTrue(await(provider, deadline));
                } catch (InterruptedException cause) {
                    Thread.currentThread().interrupt();
                    throw new AssertionError("interrupted while awaiting for asynch completion", cause);
                }
            }
        } finally {
            if (tx || rb) {
                // restore previous tx status
                TransactionHelper.startTransaction();
                if (rb) {
                    TransactionHelper.setTransactionRollbackOnly();
                }
            }
        }
    }

    boolean await(Waiter waiter, long deadline) throws InterruptedException {
        if (waiter.await(deadline)) {
            return true;
        }
        try {
            File file = new ThreadDeadlocksDetector().dump(new long[0]);
            LogFactory.getLog(TransactionalFeature.class).warn("timed out in " + waiter.getClass() + ", thread dump available in " + file);
        } catch (IOException cause) {
            LogFactory.getLog(TransactionalFeature.class).warn("timed out in " + waiter.getClass() + ", cannot take thread dump", cause);
        }
        return false;
    }

    protected Class<? extends RepositoryFactory> defaultFactory;

    @Override
    public void initialize(FeaturesRunner runner) throws Exception {
        config = runner.getConfig(TransactionalConfig.class);
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
                    Logger.getLogger(TransactionalFeature.class).warn(
                            "Committing a transaction for your, please do it yourself");
                }
            }
            return;
        }
        TransactionHelper.commitOrRollbackTransaction();
    }

}
