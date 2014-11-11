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

import org.nuxeo.common.jndi.NamingContextFactory;
import org.nuxeo.runtime.jtajca.NuxeoContainer;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.SimpleFeature;
import org.nuxeo.runtime.transaction.TransactionHelper;

@Deploy({
    "org.nuxeo.runtime.jtajca"
})
public class TransactionalFeature extends SimpleFeature {

    @Override
    public void initialize(FeaturesRunner runner) throws Exception {
        NamingContextFactory.setAsInitial();
        NuxeoContainer.install();
    }

    @Override
    public void beforeRun(FeaturesRunner runner) throws Exception {
        TransactionHelper.startTransaction();
    }

    @Override
    public void afterRun(FeaturesRunner runner) throws Exception {
        TransactionHelper.commitOrRollbackTransaction();
    }
}
