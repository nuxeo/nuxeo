/*
 * (C) Copyright 2009 Nuxeo SA (http://nuxeo.com/) and contributors.
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

    public Object getObjectInstance(Object obj, Name objName, Context nameCtx,
            Hashtable<?, ?> env) throws Exception {
        Reference ref = (Reference) obj;
        if (!TransactionManager.class.getName().equals(ref.getClassName())) {
            return null;
        }
        if (NuxeoContainer.getTransactionManager() == null) {
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
            NuxeoContainer.initTransactionManager(config);
        }
        return NuxeoContainer.getTransactionManager();
    }

}
