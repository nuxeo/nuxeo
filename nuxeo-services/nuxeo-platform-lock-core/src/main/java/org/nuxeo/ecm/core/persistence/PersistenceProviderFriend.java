/*
 * (C) Copyright 2010 Nuxeo SA (http://nuxeo.com/) and contributors.
 * 
 * All rights reserved. This program and the accompanying materials are made
 * available under the terms of the GNU Lesser General Public License (LGPL)
 * version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl.html
 * 
 * This library is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License for more
 * details.
 * 
 * Contributors: Sun Seng David TAN (a.k.a. sunix) <stan@nuxeo.com>
 */
package org.nuxeo.ecm.core.persistence;

import javax.persistence.EntityManager;

/**
 * Nuxeo default persistence doesn't allow accessing doAcquireEntityManager();
 * (without starting the transaction). Friend that can access to it.
 * 
 * @author Sun Seng David TAN (a.k.a. sunix) <stan@nuxeo.com>
 * 
 */
public class PersistenceProviderFriend {

    private PersistenceProviderFriend() {
        ;
    }

    public static EntityManager acquireEntityManager(
            PersistenceProvider provider) {
        return provider.doAcquireEntityManager();
    }
}
