/*
 * (C) Copyright 2013 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
 */
public class NuxeoEhcacheTransactionManagerLookup implements TransactionManagerLookup {
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
    public void init() {
        log.debug("init");
    }

    @Override
    public void register(EhcacheXAResource resource, boolean forRecovery) {
        log.info("register XA resource");
    }

    @Override
    public void unregister(EhcacheXAResource resource, boolean forRecovery) {
        log.info("unregister XA resource");
    }

    @Override
    public void setProperties(Properties properties) {
        log.info("set properties");
    }

}
