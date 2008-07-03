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

package org.nuxeo.ecm.core.storage.sql;

import java.io.FileInputStream;
import java.io.Serializable;
import java.util.Map;

import org.nuxeo.common.xmap.XMap;
import org.nuxeo.ecm.core.api.DocumentException;
import org.nuxeo.ecm.core.schema.SchemaManager;
import org.nuxeo.ecm.core.security.SecurityManager;
import org.nuxeo.runtime.api.Framework;

/**
 * This class is the bridge between the high-level Nuxeo view of the repository,
 * and the low-level SQL-specific implementation.
 * <p>
 * It is also the factory for {@link org.nuxeo.ecm.core.model.Session}s.
 * <p>
 * This class knows about all open sessions, and can retrieve them by id.
 *
 * @author Florent Guillaume
 */
public class SQLModelRepository extends RepositoryImpl implements
        org.nuxeo.ecm.core.model.Repository {

    private static final long serialVersionUID = 1L;

    private final SecurityManager securityManager;

    private final String name;

    private boolean initialized;

    public SQLModelRepository(
            org.nuxeo.ecm.core.repository.RepositoryDescriptor descriptor)
            throws Exception {
        super(getDescriptor(descriptor), getSchemaManager());
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
    private static RepositoryDescriptor getDescriptor(
            org.nuxeo.ecm.core.repository.RepositoryDescriptor descriptor)
            throws Exception {
        String filename = descriptor.getConfigurationFile();
        XMap xmap = new XMap();
        xmap.register(RepositoryDescriptor.class);
        return (RepositoryDescriptor) xmap.load(new FileInputStream(filename));
    }

    private static SchemaManager getSchemaManager() throws Exception {
        return Framework.getService(SchemaManager.class);
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
    public org.nuxeo.ecm.core.model.Session getSession(
            Map<String, Serializable> context) throws DocumentException {
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
        return new SQLModelSession(this, context);
    }

    public SchemaManager getTypeManager() {
        return schemaManager;
    }

    public SecurityManager getSecurityManager() {
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
    public org.nuxeo.ecm.core.model.Session getSession(long sessionId) {
        throw new UnsupportedOperationException("unused");
    }

    /*
     * Used only by JCR MBean.
     */
    public synchronized org.nuxeo.ecm.core.model.Session[] getOpenedSessions() {
        return new org.nuxeo.ecm.core.model.Session[0];
    }

    public void shutdown() {
        super.close();
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

    /*
     * ----- -----
     */

}
