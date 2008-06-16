/*
 * (C) Copyright 2007-2008 Nuxeo SAS (http://nuxeo.com/) and contributors.
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

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.atomic.AtomicLong;

import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.naming.Reference;
import javax.resource.ResourceException;
import javax.resource.cci.ConnectionSpec;
import javax.resource.cci.RecordFactory;
import javax.resource.cci.ResourceAdapterMetaData;
import javax.resource.spi.ConnectionRequestInfo;
import javax.sql.XAConnection;
import javax.sql.XADataSource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.HibernateException;
import org.hibernate.dialect.Dialect;
import org.hibernate.dialect.DialectFactory;
import org.nuxeo.ecm.core.schema.SchemaManager;
import org.nuxeo.ecm.core.storage.Credentials;
import org.nuxeo.ecm.core.storage.StorageException;
import org.nuxeo.ecm.core.storage.sql.db.Database;

/**
 * @author Florent Guillaume
 */
public class RepositoryImpl implements Repository {

    private static final long serialVersionUID = 1L;

    private static final Log log = LogFactory.getLog(RepositoryImpl.class);

    private final SchemaManager schemaManager;

    private final RepositoryDescriptor descriptor;

    // TODO check if really want something weak w.r.t. remoting
    /** The sessions in a weak hash map. Values are unused. A weak set really. */
    private final Map<Session, Object> sessions;

    // initialized at first login

    private boolean initialized;

    private Dialect dialect;

    private Model model;

    private SQLInfo sqlInfo;

    private Database database;

    private final AtomicLong temporaryIdCounter;

    protected RepositoryImpl(RepositoryDescriptor descriptor,
            SchemaManager schemaManager) {
        this.descriptor = descriptor;
        this.schemaManager = schemaManager;
        sessions = new WeakHashMap<Session, Object>();
        temporaryIdCounter = new AtomicLong(0);
    }

    /*
     * ----- javax.resource.cci.ConnectionFactory -----
     */

    /**
     * Gets a new connection by logging in to the repository with default
     * credentials.
     *
     * @return the session
     * @throws StorageException
     */
    public Session getConnection() throws StorageException {
        return getConnection(null);
    }

    /**
     * Gets a new connection by logging in to the repository with given
     * connection information (credentials).
     *
     * @param connectionSpec the parameters to use to connnect
     * @return the session
     * @throws StorageException
     */
    public Session getConnection(ConnectionSpec connectionSpec)
            throws StorageException {
        if (connectionSpec != null &&
                !(connectionSpec instanceof ConnectionSpecImpl)) {
            throw new RuntimeException("Invalid connectionSpec instance");
        }

        Credentials credentials = connectionSpec == null ? null
                : ((ConnectionSpecImpl) connectionSpec).credentials;

        // XXX synchronize initialization
        if (!initialized) {
            initialize();
            // initialized is set to true later
        }
        Mapper mapper = new Mapper(model, sqlInfo, getXAConnection());
        if (!initialized) {
            // first connection, initialize the database
            // XXX must check existing tables in the database XXX
            // XXX create always for now
            mapper.createDatabase();
            initialized = true;
        }
        // XXX put in sessions cache?
        Session session = new SessionImpl(this, schemaManager, mapper,
                credentials);
        // XXX synchronize map access
        sessions.put(session, null);
        return session;
    }

    public ResourceAdapterMetaData getMetaData() {
        throw new RuntimeException("Not implemented");
    }

    public RecordFactory getRecordFactory() {
        throw new RuntimeException("Not implemented");
    }

    /*
     * ----- javax.resource.Referenceable -----
     */

    private Reference reference;

    public Reference getReference() {
        return reference;
    }

    public void setReference(Reference reference) {
        this.reference = reference;
    }

    /*
     * ----- Repository -----
     */

    public long getNextTemporaryId() {
        return temporaryIdCounter.incrementAndGet();
    }

    public void close() {
        synchronized (sessions) {
            for (Session session : sessions.keySet()) {
                if (((SessionImpl) session).isLive()) {
                    try {
                        session.close();
                    } catch (ResourceException e) {
                        log.error("Error closing session", e);
                    }
                }
            }
            sessions.clear();
        }
    }

    /*
     * ----- -----
     */

    /**
     * Lazy initialization, to delay dialect detection until the first
     * connection is really needed.
     */
    // called by a synchronized method
    protected void initialize() throws StorageException {
        log.debug("Initializing");
        dialect = getDialect();
        model = new Model(schemaManager);
        sqlInfo = new SQLInfo(model, dialect);
    }

    /**
     * Gets the {@code Dialect}, by connecting to the datasource if needed to
     * check what database is used.
     *
     * @throws StorageException if a SQL connection problem occurs.
     */
    protected Dialect getDialect() throws StorageException {
        if (descriptor.dialectName != null) {
            try {
                return DialectFactory.buildDialect(descriptor.dialectName);
            } catch (HibernateException e) {
                throw new StorageException("Cannot build dialect  " +
                        descriptor.dialectName, e);
            }
        }
        // find a dialect from the datasource meta information
        final String dbname;
        final int dbmajor;
        final XAConnection xaconnection = getXAConnection();
        Connection connection = null;
        try {
            connection = xaconnection.getConnection();
            final DatabaseMetaData metadata = connection.getMetaData();
            dbname = metadata.getDatabaseProductName();
            dbmajor = metadata.getDatabaseMajorVersion();
        } catch (SQLException e) {
            throw new StorageException(e);
        } finally {
            if (connection != null) {
                try {
                    connection.close();
                } catch (SQLException e) {
                    throw new StorageException(e);
                }
            }
        }
        try {
            return DialectFactory.determineDialect(dbname, dbmajor);
        } catch (HibernateException e) {
            throw new StorageException(
                    "Cannot determine dialect for datasource " +
                            descriptor.dataSourceName, e);
        }
    }

    /**
     * Gets a new {@code XAConnection} from the underlying datasource.
     *
     * @return the connection.
     */
    protected XAConnection getXAConnection() throws StorageException {
        final XADataSource datasource;
        final XAConnection xaconnection;
        try {
            datasource = (XADataSource) new InitialContext().lookup(descriptor.dataSourceName);
        } catch (NamingException e) {
            throw new StorageException("Cannot get datasource " +
                    descriptor.dataSourceName, e);
        }
        try {
            xaconnection = datasource.getXAConnection();
        } catch (SQLException e) {
            throw new StorageException("Cannot get connection from datasource",
                    e);
        }
        return xaconnection;
    }

}
