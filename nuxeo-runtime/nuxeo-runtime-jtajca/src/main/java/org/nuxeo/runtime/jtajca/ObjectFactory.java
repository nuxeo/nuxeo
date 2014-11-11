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
 *     matic
 */
package org.nuxeo.runtime.jtajca;

import java.util.Hashtable;

import javax.naming.Context;
import javax.naming.Name;
import javax.naming.Reference;
import javax.resource.spi.ConnectionManager;

import org.apache.geronimo.transaction.manager.RecoverableTransactionManager;

/**
 * @author matic
 *
 */
public class ObjectFactory implements javax.naming.spi.ObjectFactory {

    @Override
    public Object getObjectInstance(Object obj, Name name, Context nameCtx,
            Hashtable<?, ?> environment) throws Exception {
         Reference ref = (Reference) obj;
        if (RecoverableTransactionManager.class.getName().equals(ref.getClassName())) {
            return NuxeoContainer.getTransactionManager();
        }
        if (ConnectionManager.class.getName().equals(ref.getClassName())) {
            return NuxeoContainer.getConnectionManager();
        }
        return null;
    }

}
