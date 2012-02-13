/*
 * Copyright (c) 2006-2012 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 */
package org.nuxeo.ecm.core.repository;

import javax.transaction.Transaction;
import javax.transaction.TransactionManager;

import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.UnrestrictedSessionRunner;
import org.nuxeo.runtime.transaction.TransactionHelper;

/**
 * Invoke repository initialization handler with tx activated
 * and unrestricted permissions.
 *
 * @since 5.6
 * @author Stephane Lacoin
 *
 */
public class RepositoryInitializer extends UnrestrictedSessionRunner {

    protected final RepositoryInitializationHandler handler;

    public RepositoryInitializer(RepositoryInitializationHandler handler,
            String name) {
        super(name);
        this.handler = handler;
    }

    @Override
    public void run() throws ClientException {
        handler.initializeRepository(session);
    }

    public static void initialize(String name) throws ClientException {
        RepositoryInitializationHandler handler = RepositoryInitializationHandler.getInstance();
        if (handler == null) {
            return;
        }
        Transaction mainTx = TransactionHelper.requireNewTransaction();
        try {
            new RepositoryInitializer(handler, name).runUnrestricted();
        } finally {
            TransactionHelper.resumeTransaction(mainTx);
        }
    }
}