/*
 * (C) Copyright 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
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
    public Object getObjectInstance(Object obj, Name objName, Context nameCtx, Hashtable<?, ?> env) {
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
            } catch (ReflectiveOperationException e) {
                log.error(String.format("NuxeoTransactionManagerFactory cannot set %s = %s", name, value));
            }
        }
        return NuxeoContainer.initTransactionManager(config);
    }

}
