/*
 * (C) Copyright 2011 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Florent Guillaume
 */
package org.nuxeo.runtime.datasource;

import javax.naming.NamingException;
import javax.transaction.HeuristicMixedException;
import javax.transaction.HeuristicRollbackException;
import javax.transaction.InvalidTransactionException;
import javax.transaction.NotSupportedException;
import javax.transaction.RollbackException;
import javax.transaction.SystemException;
import javax.transaction.Transaction;
import javax.transaction.TransactionManager;

import org.nuxeo.runtime.transaction.TransactionHelper;

/**
 * Transaction Manager that delegates to an actual one looked up only on first
 * access.
 */
public class LazyTransactionManager implements TransactionManager {

    protected TransactionManager transactionManager;

    // not synchronized, it's all right to do the lookup twice
    protected TransactionManager getTransactionManager() {
        if (transactionManager == null) {
            try {
                transactionManager = TransactionHelper.lookupTransactionManager();
            } catch (NamingException e) {
                throw new RuntimeException(e);
            }
        }
        return transactionManager;
    }

    @Override
    public void begin() throws NotSupportedException, SystemException {
        getTransactionManager().begin();
    }

    @Override
    public void commit() throws RollbackException, HeuristicMixedException,
            HeuristicRollbackException, SecurityException,
            IllegalStateException, SystemException {
        getTransactionManager().commit();
    }

    @Override
    public int getStatus() throws SystemException {
        return getTransactionManager().getStatus();
    }

    @Override
    public Transaction getTransaction() throws SystemException {
        return getTransactionManager().getTransaction();
    }

    @Override
    public void resume(Transaction tobj) throws InvalidTransactionException,
            IllegalStateException, SystemException {
        getTransactionManager().resume(tobj);
    }

    @Override
    public void rollback() throws IllegalStateException, SecurityException,
            SystemException {
        getTransactionManager().rollback();
    }

    @Override
    public void setRollbackOnly() throws IllegalStateException, SystemException {
        getTransactionManager().setRollbackOnly();
    }

    @Override
    public void setTransactionTimeout(int seconds) throws SystemException {
        getTransactionManager().setTransactionTimeout(seconds);
    }

    @Override
    public Transaction suspend() throws SystemException {
        return getTransactionManager().suspend();
    }

}
