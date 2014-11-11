/*
 * (C) Copyright 2008-2009 Nuxeo SA (http://nuxeo.com/) and contributors.
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

package org.nuxeo.ecm.core.storage.sql.ra;

import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.resource.spi.ConnectionManager;
import javax.resource.spi.ManagedConnectionFactory;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.model.Repository;
import org.nuxeo.ecm.core.repository.RepositoryDescriptor;
import org.nuxeo.ecm.core.repository.RepositoryFactory;
import org.nuxeo.ecm.core.storage.sql.coremodel.SQLRepository;
import org.nuxeo.ecm.core.storage.sql.ra.ManagedConnectionFactoryImpl;

/**
 * Pooling repository factory.
 * <p>
 * This class is mentioned in the repository extension point defining a given
 * repository.
 * <p>
 * To function properly, it needs the bundle nuxeo-runtime-jtajca to be depoyed.
 */
public class PoolingRepositoryFactory implements RepositoryFactory {

    private static final Log log = LogFactory.getLog(PoolingRepositoryFactory.class);

    public Repository createRepository(RepositoryDescriptor descriptor)
            throws Exception {
        log.info("Creating pooling repository: " + descriptor.getName());
        ManagedConnectionFactory managedConnectionFactory = new ManagedConnectionFactoryImpl(
                SQLRepository.getDescriptor(descriptor));
        return (Repository) managedConnectionFactory.createConnectionFactory(lookupConnectionManager());
    }

    /**
     * Various binding names for the ConnectionManager. They depend on the
     * application server used and how the configuration is done.
     */
    private static final String[] CM_NAMES = {
            "java:comp/NuxeoConnectionManager",
            "java:comp/env/NuxeoConnectionManager",
            "java:NuxeoConnectionManager" };

    protected static ConnectionManager lookupConnectionManager()
            throws NamingException {
        InitialContext context = new InitialContext();
        int i = 0;
        for (String name : CM_NAMES) {
            try {
                ConnectionManager connectionManager = (ConnectionManager) context.lookup(name);
                if (connectionManager != null) {
                    if (i != 0) {
                        // put successful name first for next time
                        CM_NAMES[i] = CM_NAMES[0];
                        CM_NAMES[0] = name;
                    }
                    return connectionManager;
                }
            } catch (NamingException e) {
                // try next one
            }
            i++;
        }
        throw new NamingException("NuxeoConnectionManager not found in JNDI");
    }
}
