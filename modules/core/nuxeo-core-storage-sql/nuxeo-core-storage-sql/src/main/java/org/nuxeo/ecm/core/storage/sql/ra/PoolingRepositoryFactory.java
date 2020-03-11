/*
 * (C) Copyright 2006-2014 Nuxeo SA (http://nuxeo.com/) and others.
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
package org.nuxeo.ecm.core.storage.sql.ra;

import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.resource.ResourceException;
import javax.resource.spi.ConnectionManager;

import org.nuxeo.ecm.core.repository.RepositoryFactory;
import org.nuxeo.ecm.core.storage.sql.RepositoryDescriptor;
import org.nuxeo.ecm.core.storage.sql.coremodel.SQLRepositoryService;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.jtajca.NuxeoConnectionManagerConfiguration;
import org.nuxeo.runtime.jtajca.NuxeoContainer;

/**
 * Repository factory for VCS, the repository implements internally a JCA pool of sessions.
 */
public class PoolingRepositoryFactory implements RepositoryFactory {

    protected final String repositoryName;

    public PoolingRepositoryFactory(String repositoryName) {
        this.repositoryName = repositoryName;
    }

    @Override
    public Object call() {
        SQLRepositoryService sqlRepositoryService = Framework.getService(SQLRepositoryService.class);
        RepositoryDescriptor descriptor = sqlRepositoryService.getRepositoryDescriptor(repositoryName);
        ManagedConnectionFactoryImpl managedConnectionFactory = new ManagedConnectionFactoryImpl(repositoryName);
        try {
            NuxeoConnectionManagerConfiguration pool = descriptor.pool;
            if (pool == null) {
                pool = new NuxeoConnectionManagerConfiguration();
                pool.setName("repository/" + repositoryName);
            }
            ConnectionManager connectionManager = lookupConnectionManager(pool);
            return managedConnectionFactory.createConnectionFactory(connectionManager);
        } catch (NamingException | ResourceException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Various binding names for the ConnectionManager. They depend on the application server used and how the
     * configuration is done.
     */
    private static final String[] CM_NAMES_PREFIXES = { "java:comp/NuxeoConnectionManager/",
            "java:comp/env/NuxeoConnectionManager/", "java:NuxeoConnectionManager/" };

    protected static ConnectionManager lookupConnectionManager(NuxeoConnectionManagerConfiguration pool)
            throws NamingException {
        String name = pool.getName();
        // Check in container
        ConnectionManager cm = NuxeoContainer.getConnectionManager(name);
        if (cm != null) {
            return cm;
        }
        // Check in JNDI
        InitialContext context = new InitialContext();
        for (String prefix : CM_NAMES_PREFIXES) {
            try {
                cm = (ConnectionManager) context.lookup(prefix + name);
                if (cm != null) {
                    return cm;
                }
            } catch (NamingException e) {
                // try next one
            }
        }
        // Creation from descriptor pool config
        return NuxeoContainer.initConnectionManager(pool);
    }

}
