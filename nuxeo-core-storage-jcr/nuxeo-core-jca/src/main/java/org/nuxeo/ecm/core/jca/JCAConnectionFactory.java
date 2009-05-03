/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  The ASF licenses this file to You
 * under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.nuxeo.ecm.core.jca;

import java.io.Serializable;
import java.util.Map;

import javax.naming.Reference;
import javax.resource.Referenceable;
import javax.resource.ResourceException;
import javax.resource.spi.ConnectionManager;

import org.nuxeo.ecm.core.api.DocumentException;
import org.nuxeo.ecm.core.model.Repository;
import org.nuxeo.ecm.core.model.Session;
import org.nuxeo.ecm.core.schema.SchemaManager;
import org.nuxeo.ecm.core.security.SecurityManager;

/**
 * The connection factory is wrapping a Repository.
 * <p>
 * These sources are based on the JackRabbit JCA implementation
 * (http://jackrabbit.apache.org/)
 *
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
public final class JCAConnectionFactory
        implements Repository, Referenceable, Serializable {

    private static final long serialVersionUID = 5740688754674793932L;

    /**
     * Managed connection factory.
     */
    private final JCAManagedConnectionFactory mcf;

    /**
     * Connection manager.
     */
    private final ConnectionManager cm;

    /**
     * Reference.
     */
    private Reference reference;

    /**
     * Constructs the repository.
     */
    public JCAConnectionFactory(JCAManagedConnectionFactory mcf, ConnectionManager cm) {
        this.mcf = mcf;
        this.cm = cm;
    }

    public String getName() {
        return mcf.getRepository().getName();
    }

    public SchemaManager getTypeManager() {
        return mcf.getRepository().getTypeManager();
    }

    public SecurityManager getNuxeoSecurityManager() {
        return mcf.getRepository().getNuxeoSecurityManager();
    }

    public Session getSession(Map<String, Serializable> context) throws DocumentException {
        return mcf.getRepository().getSession(context);
    }

    public Session getSession(long sessionId) throws DocumentException {
        return mcf.getRepository().getSession(sessionId);
    }

    public Session[] getOpenedSessions() throws DocumentException {
        return mcf.getRepository().getOpenedSessions();
    }

    public int getActiveSessionsCount() {
        return mcf.getRepository().getActiveSessionsCount();
    }

    public int getStartedSessionsCount() {
        return mcf.getRepository().getStartedSessionsCount();
    }

    public int getClosedSessionsCount() {
        return mcf.getRepository().getClosedSessionsCount();
    }

    /**
     * Creates a new session.
     */
    // TODO: Never used. Remove?
    private Session getSession(JCAConnectionRequestInfo cri) throws DocumentException {
        try {
            return (Session) cm.allocateConnection(mcf, cri);
        } catch (ResourceException e) {
            Throwable cause = e.getCause();
            if (cause != null) {
                if (cause instanceof DocumentException) {
                    throw (DocumentException) cause;
                }
            }
            throw new DocumentException(e);
        }
    }

    public void shutdown() {
        mcf.shutdownRepository();
    }

    public void initialize() throws DocumentException {
        mcf.getRepository().initialize();
    }

    public Reference getReference() {
        return reference;
    }

    public void setReference(Reference reference) {
        this.reference = reference;
    }

}
