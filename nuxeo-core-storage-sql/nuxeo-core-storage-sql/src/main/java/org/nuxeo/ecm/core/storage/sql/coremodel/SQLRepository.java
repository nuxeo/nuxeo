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
package org.nuxeo.ecm.core.storage.sql.coremodel;

import java.io.Serializable;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.DocumentException;
import org.nuxeo.ecm.core.model.Repository;
import org.nuxeo.ecm.core.model.Session;
import org.nuxeo.ecm.core.schema.SchemaManager;
import org.nuxeo.ecm.core.storage.StorageException;
import org.nuxeo.ecm.core.storage.sql.RepositoryDescriptor;
import org.nuxeo.ecm.core.storage.sql.RepositoryImpl;
import org.nuxeo.runtime.api.Framework;

/**
 * This is the {@link Session} factory when the repository is used outside of a
 * datasource.
 * <p>
 * (When repositories are looked up through JNDI, the class
 * org.nuxeo.ecm.core.storage.sql.ra.ConnectionFactoryImpl is used instead of
 * this one.) [suppressed link for solving cycle dependencies in eclipse]
 * <p>
 * This class is constructed by {@link SQLRepositoryFactory}.
 *
 * @author Florent Guillaume
 */
public class SQLRepository implements Repository {

    private static final Log log = LogFactory.getLog(SQLRepository.class);

    public final RepositoryImpl repository;

    private final SchemaManager schemaManager;

    private final String name;

    public SQLRepository(RepositoryDescriptor descriptor) {
        schemaManager = Framework.getLocalService(SchemaManager.class);
        try {
            repository = new RepositoryImpl(descriptor);
            name = descriptor.name;
        } catch (StorageException e) {
            throw new RuntimeException(e);
        }
    }

    /*
     * ----- org.nuxeo.ecm.core.model.Repository -----
     */

    @Override
    public String getName() {
        return name;
    }

    /*
     * Called by LocalSession.createSession
     */
    @Override
    public Session getSession(Map<String, Serializable> context)
            throws DocumentException {
        org.nuxeo.ecm.core.storage.sql.Session session;
        try {
            session = repository.getConnection();
        } catch (StorageException e) {
            throw new DocumentException(e);
        }
        return new SQLSession(session, this, context);
    }

    @Override
    public SchemaManager getTypeManager() {
        return schemaManager;
    }

    /*
     * Used only by unit tests. Shouldn't be in public API.
     */
    @Override
    public void initialize() {
    }

    /*
     * Used only by JCR MBean.
     */
    @Override
    public synchronized Session[] getOpenedSessions() {
        return new Session[0];
    }

    @Override
    public void shutdown() {
        try {
            repository.close();
        } catch (StorageException e) {
            log.error("Cannot close repository", e);
        }
    }

    @Override
    public int getStartedSessionsCount() {
        return 0;
    }

    @Override
    public int getClosedSessionsCount() {
        return 0;
    }

    @Override
    public int getActiveSessionsCount() {
        return 0;
    }

}
