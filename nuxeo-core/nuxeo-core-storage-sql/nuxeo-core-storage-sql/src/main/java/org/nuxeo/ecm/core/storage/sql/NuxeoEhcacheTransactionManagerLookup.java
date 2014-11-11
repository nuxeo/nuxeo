/*
 * Copyright (c) 2013 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Benoit Delbosc
 */

package org.nuxeo.ecm.core.storage.sql;

import java.util.Properties;

import javax.naming.NamingException;
import javax.transaction.TransactionManager;

import net.sf.ehcache.transaction.manager.TransactionManagerLookup;
import net.sf.ehcache.transaction.xa.EhcacheXAResource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.runtime.transaction.TransactionHelper;

/**
 * Help ehcache to find the Nuxeo transaction manager
 *
 */
public class NuxeoEhcacheTransactionManagerLookup implements
        TransactionManagerLookup {
    private static final Log log = LogFactory.getLog(NuxeoEhcacheTransactionManagerLookup.class);

    @Override
    public TransactionManager getTransactionManager() {
        try {
            return TransactionHelper.lookupTransactionManager();
        } catch (NamingException e) {
            log.error(e.getMessage(), e);
            return null;
        }
    }

    @Override
    public void register(EhcacheXAResource resource) {
        log.info("register XA resource");
    }

    @Override
    public void unregister(EhcacheXAResource resource) {
        log.info("unregister XA resource");
    }

    @Override
    public void setProperties(Properties properties) {
        log.info("set properties");
    }

}
