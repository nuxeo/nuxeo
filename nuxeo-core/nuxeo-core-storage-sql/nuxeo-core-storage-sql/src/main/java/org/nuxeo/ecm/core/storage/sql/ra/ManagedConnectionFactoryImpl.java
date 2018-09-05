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

package org.nuxeo.ecm.core.storage.sql.ra;

import java.io.PrintWriter;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.resource.ResourceException;
import javax.resource.cci.ConnectionFactory;
import javax.resource.spi.ConnectionManager;
import javax.resource.spi.ConnectionRequestInfo;
import javax.resource.spi.ManagedConnection;
import javax.resource.spi.ManagedConnectionFactory;
import javax.resource.spi.ResourceAdapter;
import javax.resource.spi.ResourceAdapterAssociation;
import javax.security.auth.Subject;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.repository.FulltextConfiguration;
import org.nuxeo.ecm.core.storage.sql.RepositoryDescriptor;
import org.nuxeo.ecm.core.storage.sql.RepositoryImpl;
import org.nuxeo.ecm.core.storage.sql.RepositoryManagement;
import org.nuxeo.ecm.core.storage.sql.SessionImpl;
import org.nuxeo.ecm.core.storage.sql.coremodel.SQLRepositoryService;
import org.nuxeo.runtime.api.Framework;

/**
 * The managed connection factory receives requests from the application server to create new {@link ManagedConnection}
 * (the physical connection).
 * <p>
 * It also is a factory for {@link ConnectionFactory}s.
 *
 * @author Florent Guillaume
 */
public class ManagedConnectionFactoryImpl implements ManagedConnectionFactory, ResourceAdapterAssociation,
        RepositoryManagement {

    private static final Log log = LogFactory.getLog(ManagedConnectionFactoryImpl.class);

    private static final long serialVersionUID = 1L;

    protected final String name;

    private transient ResourceAdapter resourceAdapter;

    private transient PrintWriter out;

    /**
     * The instantiated repository.
     */
    private RepositoryImpl repository;

    public ManagedConnectionFactoryImpl(String name) {
        this.name = name;
        RepositoryDescriptor repositoryDescriptor = getRepositoryDescriptor(name);
        repository = new RepositoryImpl(repositoryDescriptor);
    }

    @Override
    public String getName() {
        return name;
    }

    /*
     * ----- javax.resource.spi.ResourceAdapterAssociation -----
     */

    /**
     * Called by the application server exactly once to associate this ManagedConnectionFactory with a ResourceAdapter.
     * The ResourceAdapter may then be used to look up configuration.
     */
    @Override
    public void setResourceAdapter(ResourceAdapter resourceAdapter) throws ResourceException {
        this.resourceAdapter = resourceAdapter;
    }

    @Override
    public ResourceAdapter getResourceAdapter() {
        return resourceAdapter;
    }

    /*
     * ----- javax.resource.spi.ManagedConnectionFactory -----
     */

    @Override
    public void setLogWriter(PrintWriter out) {
        this.out = out;
    }

    @Override
    public PrintWriter getLogWriter() {
        return out;
    }

    /*
     * Used in non-managed scenarios.
     */
    @Override
    public Object createConnectionFactory() throws ResourceException {
        return createConnectionFactory(new ConnectionManagerImpl());
    }

    /*
     * Used in managed scenarios.
     */
    @Override
    public Object createConnectionFactory(ConnectionManager connectionManager) throws ResourceException {
        ConnectionFactoryImpl connectionFactory = new ConnectionFactoryImpl(this, connectionManager);
        log.debug("Created repository factory (" + connectionFactory + ')');
        return connectionFactory;
    }

    /*
     * Creates a new physical connection to the underlying storage. Called by the application server pool (or the
     * non-managed ConnectionManagerImpl) when it needs a new connection.
     */
    @Override
    public ManagedConnection createManagedConnection(Subject subject, ConnectionRequestInfo connectionRequestInfo)
            throws ResourceException {
        // subject unused
        // connectionRequestInfo unused
        return new ManagedConnectionImpl(this);
    }

    /**
     * Returns a matched connection from the candidate set of connections.
     * <p>
     * Called by the application server when it's looking for an appropriate connection to server from a pool.
     */
    @Override
    public ManagedConnection matchManagedConnections(Set set, Subject subject, ConnectionRequestInfo cri)
            throws ResourceException {
        for (Object candidate : set) {
            if (!(candidate instanceof ManagedConnectionImpl)) {
                continue;
            }
            ManagedConnectionImpl managedConnection = (ManagedConnectionImpl) candidate;
            if (!equals(managedConnection.getManagedConnectionFactory())) {
                continue;
            }
            log.debug("matched: " + managedConnection);
            return managedConnection;
        }
        return null;
    }

    /*
     * ----- org.nuxeo.ecm.core.storage.sql.RepositoryManagement -----
     */

    @Override
    public int getActiveSessionsCount() {
        if (repository == null) {
            return 0;
        }
        return repository.getActiveSessionsCount();
    }

    @Override
    public int clearCaches() {
        if (repository == null) {
            return 0;
        }
        return repository.clearCaches();
    }

    @Override
    public long getCacheSize() {
        return repository.getCacheSize();
    }

    @Override
    public long getCachePristineSize() {
        return repository.getCachePristineSize();
    }

    @Override
    public long getCacheSelectionSize() {
        return repository.getCacheSelectionSize();
    }

    @Override
    public void processClusterInvalidationsNext() {
        if (repository != null) {
            repository.processClusterInvalidationsNext();
        }
    }

    @Override
    public void markReferencedBinaries() {
        if (repository != null) {
            repository.markReferencedBinaries();
        }
    }

    @Override
    public int cleanupDeletedDocuments(int max, Calendar beforeTime) {
        if (repository == null) {
            return 0;
        }
        return repository.cleanupDeletedDocuments(max, beforeTime);
    }

    @Override
    public FulltextConfiguration getFulltextConfiguration() {
        if (repository == null) {
            return null;
        }
        return repository.getFulltextConfiguration();
    }

    /*
     * ----- -----
     */

    public void shutdown() {
        try {
            repository.close();
        } finally {
            repository = null;
        }
    }

    /**
     * Gets the repository descriptor provided by the repository extension point. It's where clustering, indexing, etc.
     * are configured.
     */
    protected static RepositoryDescriptor getRepositoryDescriptor(String name) {
        SQLRepositoryService sqlRepositoryService = Framework.getService(SQLRepositoryService.class);
        return sqlRepositoryService.getRepositoryDescriptor(name);
    }

    /**
     * Called by the {@link ManagedConnectionImpl} constructor to get a new physical connection.
     */
    protected SessionImpl getConnection() {
        return repository.getConnection();
    }

    private static final Pattern KEYVALUE = Pattern.compile("([^=]*)=(.*)");

    /**
     * Parses a string of the form: <code>key1=val1;key2=val2;...</code> and collects the key/value pairs.
     * <p>
     * A ';' character may end the expression. If a value has to contain a ';', it can be escaped by doubling it.
     * <p>
     * Examples of valid expressions: <code>key1=val1</code>, <code>key1=val1;</code>, <code>key1=val1;key2=val2</code>,
     * <code>key1=a=b;;c=d;key2=val2</code>.
     * <p>
     * Syntax errors are reported using the logger and will stop the parsing but already collected properties will be
     * available. The ';' or '=' characters cannot be escaped in keys.
     *
     * @param expr the expression to parse
     * @return a key/value map
     */
    public static Map<String, String> parseProperties(String expr) {
        String SPECIAL = "\u1fff"; // never present in the strings to parse
        Map<String, String> props = new HashMap<String, String>();
        for (String kv : expr.replace(";;", SPECIAL).split(";")) {
            kv = kv.replace(SPECIAL, ";");
            if ("".equals(kv)) {
                // empty starting string
                continue;
            }
            Matcher m = KEYVALUE.matcher(kv);
            if (m == null || !m.matches()) {
                log.error("Invalid property expression: " + kv);
                continue;
            }
            props.put(m.group(1), m.group(2));
        }
        return props;
    }

}
