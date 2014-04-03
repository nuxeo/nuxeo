/*
 * Copyright (c) 2006-2014 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
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
 * Pooling repository factory.
 * <p>
 * This class is mentioned in the repository extension point defining a given
 * repository.
 * <p>
 * To function properly, it needs the bundle nuxeo-runtime-jtajca to be depoyed.
 */
public class PoolingRepositoryFactory implements RepositoryFactory {

    private String repositoryName;

    @Override
    public void init(String repositoryName) {
        this.repositoryName = repositoryName;
    }

    @Override
    public Object call() {
        SQLRepositoryService sqlRepositoryService = Framework.getLocalService(SQLRepositoryService.class);
        RepositoryDescriptor descriptor = sqlRepositoryService.getRepositoryDescriptor(repositoryName);
        ManagedConnectionFactoryImpl managedConnectionFactory = new ManagedConnectionFactoryImpl();
        managedConnectionFactory.setName(descriptor.name);
        try {
            ConnectionManager connectionManager = lookupConnectionManager(descriptor.pool);
            return managedConnectionFactory.createConnectionFactory(connectionManager);
        } catch (NamingException | ResourceException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Various binding names for the ConnectionManager. They depend on the
     * application server used and how the configuration is done.
     */
    private static final String[] CM_NAMES_PREFIXES = {
            "java:comp/NuxeoConnectionManager/",
            "java:comp/env/NuxeoConnectionManager/",
            "java:NuxeoConnectionManager/" };

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
