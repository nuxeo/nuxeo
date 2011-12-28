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

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NoInitialContextException;

import org.nuxeo.common.Environment;
import org.nuxeo.common.jndi.NamingContextFactory;
import org.nuxeo.ecm.core.test.annotations.TransactionalConfig;
import org.nuxeo.runtime.jtajca.NuxeoContainer;
import org.nuxeo.runtime.test.runner.Defaults;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.SimpleFeature;
import org.nuxeo.runtime.transaction.TransactionHelper;

@Deploy({ "org.nuxeo.runtime.jtajca" })
public class TransactionalFeature extends SimpleFeature {

    protected TransactionalConfig config;

    protected boolean txStarted;

    @Override
    public void initialize(FeaturesRunner runner) throws Exception {
        config = runner.getDescription().getAnnotation(
                TransactionalConfig.class);
        if (config == null) {
            config = Defaults.of(TransactionalConfig.class);
        }
    }

    @Override
    public void start(FeaturesRunner runner) throws Exception {
        Environment.getDefault().setHostApplicationName(
                Environment.NXSERVER_HOST);
        try {
            Context comp = (Context) new InitialContext().lookup("java:comp/");
            if (comp == null) {
                NamingContextFactory.setAsInitial();
            }
        } catch (NoInitialContextException e) {
            NamingContextFactory.setAsInitial();
        }
        NuxeoContainer.install();
    }

    @Override
    public void beforeRun(FeaturesRunner runner) throws Exception {
        if (config.autoStart() == false) {
            return;
        }
        txStarted = TransactionHelper.startTransaction();
    }

    @Override
    public void afterRun(FeaturesRunner runner) throws Exception {
        if (txStarted == false) {
            return;
        }
        TransactionHelper.commitOrRollbackTransaction();
    }
}
