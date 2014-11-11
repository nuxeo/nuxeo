/*
 * (C) Copyright 2008 Nuxeo SAS (http://nuxeo.com/) and contributors.
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

package org.nuxeo.ecm.core.storage.sql.coremodel;

import java.io.FileInputStream;
import java.io.Serializable;
import java.util.Map;

import org.nuxeo.common.xmap.XMap;
import org.nuxeo.ecm.core.api.DocumentException;
import org.nuxeo.ecm.core.model.Repository;
import org.nuxeo.ecm.core.model.Session;
import org.nuxeo.ecm.core.repository.RepositoryDescriptor;
import org.nuxeo.ecm.core.schema.SchemaManager;
import org.nuxeo.ecm.core.security.SecurityManager;
import org.nuxeo.ecm.core.storage.StorageException;
import org.nuxeo.ecm.core.storage.sql.RepositoryImpl;
import org.nuxeo.runtime.api.Framework;

/**
 * This is the {@link Session} factory when the repository is used outside of a
 * datasource.
 * <p>
 * (When repositories are looked up through JNDI, the class
 * {@link ConnectionFactoryImpl} is used instead of this one.)
 * <p>
 * This class is constructed by {@link SQLRepositoryFactory}.
 *
 * @author Florent Guillaume
 */
public class SQLRepository implements Repository {

    private final RepositoryImpl repository;

    private final SchemaManager schemaManager;

    private final SecurityManager securityManager;

    private final String name;

    private boolean initialized;

    public SQLRepository(RepositoryDescriptor descriptor) throws Exception {
        schemaManager = Framework.getService(SchemaManager.class);
        repository = new RepositoryImpl(getDescriptor(descriptor),
                schemaManager);
        if (descriptor.getSecurityManagerClass() == null) {
            securityManager = new SQLSecurityManager();
        } else {
            securityManager = descriptor.getSecurityManager();
        }
        name = descriptor.getName();
    }

    /**
     * Fetch SQL-level descriptor from Nuxeo repository descriptor.
     */
    private static org.nuxeo.ecm.core.storage.sql.RepositoryDescriptor getDescriptor(
            RepositoryDescriptor descriptor) throws Exception {
        String filename = descriptor.getConfigurationFile();
        XMap xmap = new XMap();
        xmap.register(org.nuxeo.ecm.core.storage.sql.RepositoryDescriptor.class);
        org.nuxeo.ecm.core.storage.sql.RepositoryDescriptor sqldescr = (org.nuxeo.ecm.core.storage.sql.RepositoryDescriptor) xmap.load(new FileInputStream(
                filename));
        sqldescr.name = descriptor.getName();
        return sqldescr;
    }

    /*
     * ----- org.nuxeo.ecm.core.model.Repository -----
     */

    public String getName() {
        return name;
    }

    /*
     * Called by LocalSession.createSession
     */
    public Session getSession(Map<String, Serializable> context)
            throws DocumentException {
        synchronized (this) {
            if (!initialized) {
                initialized = true;
                if (context != null) {
                    // Allow AbstractSession (our caller) to send an
                    // initialization event.
                    context.put("REPOSITORY_FIRST_ACCESS", Boolean.TRUE);
                }
            }
        }
        org.nuxeo.ecm.core.storage.sql.Session session;
        try {
            session = repository.getConnection();
        } catch (StorageException e) {
            throw new DocumentException(e);
        }
        return new SQLSession(session, this, context);
    }

    public SchemaManager getTypeManager() {
        return schemaManager;
    }

    public SecurityManager getNuxeoSecurityManager() {
        return securityManager;
    }

    /*
     * Used only by unit tests. Shouldn't be in public API.
     */
    public void initialize() {
    }

    /**
     * @deprecated unused
     */
    @Deprecated
    public Session getSession(long sessionId) {
        throw new UnsupportedOperationException("unused");
    }

    /*
     * Used only by JCR MBean.
     */
    public synchronized Session[] getOpenedSessions() {
        return new Session[0];
    }

    public void shutdown() {
        repository.close();
    }

    public int getStartedSessionsCount() {
        return 0;
    }

    public int getClosedSessionsCount() {
        return 0;
    }

    public int getActiveSessionsCount() {
        return 0;
    }

    public boolean supportsTags() {
        return true;
    }

    /*
     * ----- -----
     */

}
