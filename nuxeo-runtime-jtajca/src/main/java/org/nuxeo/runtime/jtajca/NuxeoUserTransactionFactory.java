/*
 * Copyright (c) 2006-2013 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Florent Guillaume
 */

package org.nuxeo.runtime.jtajca;

import java.util.Hashtable;

import javax.naming.Context;
import javax.naming.Name;
import javax.naming.Reference;
import javax.naming.spi.ObjectFactory;
import javax.transaction.UserTransaction;

import org.nuxeo.runtime.transaction.TransactionHelper;

/**
 * Factory for the UserTransaction.
 */
public class NuxeoUserTransactionFactory implements ObjectFactory {

    @Override
    public Object getObjectInstance(Object obj, Name objName, Context nameCtx,
            Hashtable<?, ?> env) throws Exception {
        Reference ref = (Reference) obj;
        if (!UserTransaction.class.getName().equals(ref.getClassName())) {
            return null;
        }
        if (NuxeoContainer.transactionManager == null) {
            // initialize tx manager through the factory
            return TransactionHelper.lookupTransactionManager();
        }
        return NuxeoContainer.userTransaction;
    }

}
