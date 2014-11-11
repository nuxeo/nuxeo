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

package org.nuxeo.ecm.core.storage.sql.ra;

import java.io.FileInputStream;
import java.io.PrintWriter;
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
import org.nuxeo.common.xmap.XMap;
import org.nuxeo.ecm.core.NXCore;
import org.nuxeo.ecm.core.storage.StorageException;
import org.nuxeo.ecm.core.storage.sql.ConnectionSpecImpl;
import org.nuxeo.ecm.core.storage.sql.RepositoryDescriptor;
import org.nuxeo.ecm.core.storage.sql.RepositoryImpl;
import org.nuxeo.ecm.core.storage.sql.RepositoryManagement;
import org.nuxeo.ecm.core.storage.sql.SessionImpl;

/**
 * The managed connection factory receives requests from the application server
 * to create new {@link ManagedConnection} (the physical connection).
 * <p>
 * It also is a factory for {@link ConnectionFactory}s.
 *
 * @author Florent Guillaume
 */
public class ManagedConnectionFactoryImpl implements ManagedConnectionFactory,
        ResourceAdapterAssociation, RepositoryManagement {

    private static final Log log = LogFactory.getLog(ManagedConnectionFactoryImpl.class);

    private static final long serialVersionUID = 1L;

    private final RepositoryDescriptor repositoryDescriptor;

    private transient ResourceAdapter resourceAdapter;

    private transient PrintWriter out;

    /**
     * The instantiated repository.
     */
    private RepositoryImpl repository;

    public ManagedConnectionFactoryImpl() {
        this(new RepositoryDescriptor());
    }

    public ManagedConnectionFactoryImpl(
            RepositoryDescriptor repositoryDescriptor) {
        this.repositoryDescriptor = repositoryDescriptor;
        if (repositoryDescriptor.properties == null) {
            repositoryDescriptor.properties = new HashMap<String, String>();
        }
    }

    /*
     * ----- Java Bean -----
     */

    public void setName(String name) {
        repositoryDescriptor.name = name;
    }

    public String getName() {
        return repositoryDescriptor.name;
    }

    public void setXaDataSource(String xaDataSourceName) {
        repositoryDescriptor.xaDataSourceName = xaDataSourceName;
    }

    public String getXaDataSource() {
        return repositoryDescriptor.xaDataSourceName;
    }

    /**
     * Properties are specified in the format key=val1[;key2=val2;...]
     * <p>
     * If a value has to contain a semicolon, it can be escaped by doubling it.
     *
     * @see #parseProperties(String)
     * @param property
     */
    public void setProperty(String property) {
        repositoryDescriptor.properties.putAll(parseProperties(property));
    }

    public String getProperty() {
        return null;
    }

    /*
     * ----- javax.resource.spi.ResourceAdapterAssociation -----
     */

    /**
     * Called by the application server exactly once to associate this
     * ManagedConnectionFactory with a ResourceAdapter. The ResourceAdapter may
     * then be used to look up configuration.
     */
    public void setResourceAdapter(ResourceAdapter resourceAdapter)
            throws ResourceException {
        this.resourceAdapter = resourceAdapter;
    }

    public ResourceAdapter getResourceAdapter() {
        return resourceAdapter;
    }

    /*
     * ----- javax.resource.spi.ManagedConnectionFactory -----
     */

    public void setLogWriter(PrintWriter out) {
        this.out = out;
    }

    public PrintWriter getLogWriter() {
        return out;
    }

    /*
     * Used in non-managed scenarios.
     */
    public Object createConnectionFactory() throws ResourceException {
        return createConnectionFactory(new ConnectionManagerImpl());
    }

    /*
     * Used in managed scenarios.
     */
    public Object createConnectionFactory(ConnectionManager connectionManager)
            throws ResourceException {
        ConnectionFactoryImpl connectionFactory = new ConnectionFactoryImpl(
                this, connectionManager);
        log.debug("Created repository factory (" + connectionFactory + ')');
        return connectionFactory;
    }

    /*
     * Creates a new physical connection to the underlying storage. Called by
     * the application server pool (or the non-managed ConnectionManagerImpl)
     * when it needs a new connection.
     */
    /*
     * If connectionRequestInfo is null then it means that the call is made by
     * the application server for the recovery case (6.5.3.5).
     */
    public ManagedConnection createManagedConnection(Subject subject,
            ConnectionRequestInfo connectionRequestInfo)
            throws ResourceException {
        assert connectionRequestInfo instanceof ConnectionRequestInfoImpl;
        initialize();
        return new ManagedConnectionImpl(this,
                (ConnectionRequestInfoImpl) connectionRequestInfo);
    }

    /**
     * Returns a matched connection from the candidate set of connections.
     * <p>
     * Called by the application server when it's looking for an appropriate
     * connection to server from a pool.
     */
    @SuppressWarnings("unchecked")
    public ManagedConnection matchManagedConnections(Set set, Subject subject,
            ConnectionRequestInfo cri) throws ResourceException {
        for (Object candidate : set) {
            if (!(candidate instanceof ManagedConnectionImpl)) {
                continue;
            }
            ManagedConnectionImpl managedConnection = (ManagedConnectionImpl) candidate;
            if (!this.equals(managedConnection.getManagedConnectionFactory())) {
                continue;
            }
            return managedConnection;
        }
        return null;
    }

    /*
     * ----- org.nuxeo.ecm.core.storage.sql.RepositoryManagement -----
     */

    public int getActiveSessionsCount() {
        if (repository == null) {
            return 0;
        }
        return repository.getActiveSessionsCount();
    }

    public int clearCaches() {
        if (repository == null) {
            return 0;
        }
        return repository.clearCaches();
    }

    public void processClusterInvalidationsNext() {
        if (repository != null) {
            repository.processClusterInvalidationsNext();
        }
    }

    /*
     * ----- -----
     */

    private void initialize() throws StorageException {
        synchronized (this) {
            if (repository == null) {
                repositoryDescriptor.mergeFrom(getRepositoryDescriptor(repositoryDescriptor.name));
                repository = new RepositoryImpl(repositoryDescriptor);
            }
        }
    }

    public void shutdown() {
        synchronized (this) {
            if (repository != null) {
                try {
                    repository.close();
                } catch (StorageException e) {
                    log.error("Cannot close repository", e);
                }
            }
        }
    }

    /**
     * Gets the repository descriptor provided by the repository extension
     * point. It's where clustering, indexing, etc. are configured.
     */
    protected static RepositoryDescriptor getRepositoryDescriptor(String name)
            throws StorageException {
        org.nuxeo.ecm.core.repository.RepositoryDescriptor d = NXCore.getRepositoryService().getRepositoryManager().getDescriptor(
                name);
        try {
            XMap xmap = new XMap();
            xmap.register(RepositoryDescriptor.class);
            return (RepositoryDescriptor) xmap.load(new FileInputStream(
                    d.getConfigurationFile()));
        } catch (Exception e) {
            throw new StorageException(e);
        }
    }

    /**
     * Called by the {@link ManagedConnectionImpl} constructor to get a new
     * physical connection.
     */
    protected SessionImpl getConnection(ConnectionSpecImpl connectionSpec)
            throws StorageException {
        return repository.getConnection(connectionSpec);
    }

    private static final Pattern KEYVALUE = Pattern.compile("([^=]*)=(.*)");

    /**
     * Parses a string of the form: <code>key1=val1;key2=val2;...</code> and
     * collects the key/value pairs.
     * <p>
     * A ';' character may end the expression. If a value has to contain a ';',
     * it can be escaped by doubling it.
     * <p>
     * Examples of valid expressions: <code>key1=val1</code>,
     * <code>key1=val1;</code>, <code>key1=val1;key2=val2</code>,
     * <code>key1=a=b;;c=d;key2=val2</code>.
     * <p>
     * Syntax errors are reported using the logger and will stop the parsing but
     * already collected properties will be available. The ';' or '=' characters
     * cannot be escaped in keys.
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
