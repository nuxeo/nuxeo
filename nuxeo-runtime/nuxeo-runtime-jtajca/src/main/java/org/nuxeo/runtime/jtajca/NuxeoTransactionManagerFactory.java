/*
 * Copyright (c) 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
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

import java.util.Collections;
import java.util.Hashtable;

import javax.naming.Context;
import javax.naming.Name;
import javax.naming.RefAddr;
import javax.naming.Reference;
import javax.naming.spi.ObjectFactory;
import javax.transaction.TransactionManager;

import org.apache.commons.beanutils.BeanUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.runtime.jtajca.NuxeoContainer.TransactionManagerConfiguration;

/**
 * Factory for the TransactionManager.
 */
public class NuxeoTransactionManagerFactory implements ObjectFactory {

    private static final Log log = LogFactory.getLog(NuxeoTransactionManagerFactory.class);

    @Override
    public Object getObjectInstance(Object obj, Name objName, Context nameCtx,
            Hashtable<?, ?> env) throws Exception {
        Reference ref = (Reference) obj;
        if (!TransactionManager.class.getName().equals(ref.getClassName())) {
            return null;
        }
        if (NuxeoContainer.tm != null) {
            return NuxeoContainer.tm;
        }

        // initialize
        TransactionManagerConfiguration config = new TransactionManagerConfiguration();
        for (RefAddr addr : Collections.list(ref.getAll())) {
            String name = addr.getType();
            String value = (String) addr.getContent();
            try {
                BeanUtils.setProperty(config, name, value);
            } catch (Exception e) {
                log.error(String.format(
                        "NuxeoTransactionManagerFactory cannot set %s = %s",
                        name, value));
            }
        }
        return NuxeoContainer.initTransactionManager(config);
    }

}
