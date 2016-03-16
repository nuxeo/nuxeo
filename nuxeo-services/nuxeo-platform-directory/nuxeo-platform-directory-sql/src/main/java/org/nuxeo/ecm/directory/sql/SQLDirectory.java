/*
 * (C) Copyright 2006-2015 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     George Lefter
 *     Florent Guillaume
 */
package org.nuxeo.ecm.directory.sql;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.naming.NamingException;
import javax.sql.DataSource;
import javax.transaction.Synchronization;
import javax.transaction.SystemException;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.common.utils.JDBCUtils;
import org.nuxeo.ecm.core.cache.CacheService;
import org.nuxeo.ecm.core.schema.SchemaManager;
import org.nuxeo.ecm.core.schema.types.Field;
import org.nuxeo.ecm.core.schema.types.Schema;
import org.nuxeo.ecm.core.storage.sql.ColumnType;
import org.nuxeo.ecm.core.storage.sql.jdbc.db.Column;
import org.nuxeo.ecm.core.storage.sql.jdbc.db.Table;
import org.nuxeo.ecm.core.storage.sql.jdbc.dialect.Dialect;
import org.nuxeo.ecm.directory.AbstractDirectory;
import org.nuxeo.ecm.directory.Directory;
import org.nuxeo.ecm.directory.DirectoryException;
import org.nuxeo.ecm.directory.DirectoryServiceImpl;
import org.nuxeo.ecm.directory.Session;
import org.nuxeo.ecm.directory.api.DirectoryService;
import org.nuxeo.runtime.RuntimeService;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.datasource.ConnectionHelper;
import org.nuxeo.runtime.datasource.DataSourceHelper;
import org.nuxeo.runtime.transaction.TransactionHelper;

public class SQLDirectory extends AbstractDirectory {

    protected class TxSessionCleaner implements Synchronization {
        private final SQLSession session;

        Throwable initContext = captureInitContext();

        protected TxSessionCleaner(SQLSession session) {
            this.session = session;
        }

        protected Throwable captureInitContext() {
            if (!log.isDebugEnabled()) {
                return null;
            }
            return new Throwable("SQL directory session init context in " + SQLDirectory.this);
        }

        protected void checkIsNotLive() {
            try {
                if (!session.isLive()) {
                    return;
                }
                if (initContext != null) {
                    log.warn("Closing a sql directory session for you " + session, initContext);
                } else {
                    log.warn("Closing a sql directory session for you " + session);
                }
                if (!TransactionHelper.isTransactionActiveOrMarkedRollback()) {
                    log.warn("Closing sql directory session outside a transaction" + session);
                }
                session.close();
            } catch (DirectoryException e) {
                log.error("Cannot state on sql directory session before commit " + SQLDirectory.this, e);
            }

        }

        @Override
        public void beforeCompletion() {
            checkIsNotLive();
        }

        @Override
        public void afterCompletion(int status) {
            checkIsNotLive();
        }

    }

    public static final Log log = LogFactory.getLog(SQLDirectory.class);

    public static final String TENANT_ID_FIELD = "tenantId";

    private final boolean nativeCase;

    private DataSource dataSource;

    private Table table;

    private Schema schema;

    private Map<String, Field> schemaFieldMap;

    private List<String> storedFieldNames;

    private volatile Dialect dialect;

    public SQLDirectory(SQLDirectoryDescriptor descriptor) {
        super(descriptor);
        nativeCase = Boolean.TRUE.equals(descriptor.nativeCase);

        // register the references to other directories
        addReferences(descriptor.getInverseReferences());
        addReferences(descriptor.getTableReferences());

        // cache parameterization
        cache.setEntryCacheName(descriptor.cacheEntryName);
        cache.setEntryCacheWithoutReferencesName(descriptor.cacheEntryWithoutReferencesName);
        cache.setNegativeCaching(descriptor.negativeCaching);

        // Cache fallback
        CacheService cacheService = Framework.getLocalService(CacheService.class);
        if (cacheService != null) {
            if (descriptor.cacheEntryName == null && descriptor.getCacheMaxSize() != 0) {
                cache.setEntryCacheName("cache-" + getName());
                cacheService.registerCache("cache-" + getName(),
                        descriptor.getCacheMaxSize(),
                        descriptor.getCacheTimeout() / 60);
            }
            if (descriptor.cacheEntryWithoutReferencesName == null && descriptor.getCacheMaxSize() != 0) {
                cache.setEntryCacheWithoutReferencesName(
                        "cacheWithoutReference-" + getName());
                cacheService.registerCache("cacheWithoutReference-" + getName(),
                        descriptor.getCacheMaxSize(),
                        descriptor.getCacheTimeout() / 60);
            }
        }
    }

    @Override
    public SQLDirectoryDescriptor getDescriptor() {
        return (SQLDirectoryDescriptor) descriptor;
    }

    /**
     * Lazy init connection
     *
     * @since 6.0
     */
    protected void initConnection() {
        SQLDirectoryDescriptor descriptor = getDescriptor();

        Connection sqlConnection = getConnection();
        try {
            dialect = Dialect.createDialect(sqlConnection, null);

            if (descriptor.initDependencies != null) {
                // initialize dependent directories first
                final RuntimeService runtime = Framework.getRuntime();
                DirectoryServiceImpl directoryService = (DirectoryServiceImpl) runtime.getComponent(DirectoryService.NAME);
                for (String dependency : descriptor.initDependencies) {
                    log.debug("initializing dependencies first: " + dependency);
                    Directory dir = directoryService.getDirectory(dependency);
                    dir.getName();
                }
            }
            // setup table and fields maps
            table = SQLHelper.addTable(descriptor.tableName, dialect, useNativeCase());
            SchemaManager schemaManager = Framework.getLocalService(SchemaManager.class);
            schema = schemaManager.getSchema(getSchema());
            if (schema == null) {
                throw new DirectoryException("schema not found: " + getSchema());
            }
            schemaFieldMap = new LinkedHashMap<>();
            storedFieldNames = new LinkedList<>();
            boolean hasPrimary = false;
            for (Field f : schema.getFields()) {
                String fieldName = f.getName().getLocalName();
                schemaFieldMap.put(fieldName, f);

                if (!isReference(fieldName)) {
                    // list of fields that are actually stored in the table of
                    // the current directory and not read from an external
                    // reference
                    storedFieldNames.add(fieldName);

                    boolean isId = fieldName.equals(getIdField());
                    ColumnType type = ColumnType.fromField(f);
                    if (isId && descriptor.isAutoincrementIdField()) {
                        type = ColumnType.AUTOINC;
                    }
                    Column column = SQLHelper.addColumn(table, fieldName, type, useNativeCase());
                    if (isId) {
                        if (descriptor.isAutoincrementIdField()) {
                            column.setIdentity(true);
                        }
                        column.setPrimary(true);
                        column.setNullable(false);
                        hasPrimary = true;
                    }
                }
            }
            if (!hasPrimary) {
                throw new DirectoryException(String.format(
                        "Directory '%s' id field '%s' is not present in schema '%s'", getName(), getIdField(),
                        getSchema()));
            }

            SQLHelper helper = new SQLHelper(sqlConnection, table, descriptor.dataFileName,
                    descriptor.getDataFileCharacterSeparator(), descriptor.createTablePolicy);
            helper.setupTable();

        } finally {
            try {
                sqlConnection.close();
            } catch (SQLException e) {
                throw new DirectoryException(e);
            }
        }
    }

    /** DO NOT USE, use getConnection() instead. */
    protected DataSource getDataSource() throws DirectoryException {
        if (dataSource != null) {
            return dataSource;
        }
        SQLDirectoryDescriptor descriptor = getDescriptor();
        try {
            if (!StringUtils.isEmpty(descriptor.dataSourceName)) {
                dataSource = DataSourceHelper.getDataSource(descriptor.dataSourceName);
                // InitialContext context = new InitialContext();
                // dataSource = (DataSource)
                // context.lookup(config.dataSourceName);
            } else {
                dataSource = new SimpleDataSource(descriptor.dbUrl, descriptor.dbDriver, descriptor.dbUser,
                        descriptor.dbPassword);
            }
            log.trace("found datasource: " + dataSource);
            return dataSource;
        } catch (NamingException e) {
            log.error("dataSource lookup failed", e);
            throw new DirectoryException("dataSource lookup failed", e);
        }
    }

    public Connection getConnection() throws DirectoryException {
        SQLDirectoryDescriptor descriptor = getDescriptor();
        try {
            if (!StringUtils.isEmpty(descriptor.dataSourceName)) {
                // try single-datasource non-XA mode
                Connection connection = ConnectionHelper.getConnection(descriptor.dataSourceName);
                if (connection != null) {
                    if (ConnectionHelper.useSingleConnection(descriptor.dataSourceName)) {
                        connection.setAutoCommit(TransactionHelper.isNoTransaction());
                    }
                    return connection;
                }
            }
            return getConnection(getDataSource());
        } catch (SQLException e) {
            throw new DirectoryException("Cannot connect to SQL directory '" + getName() + "': " + e.getMessage(), e);
        }
    }

    /**
     * Gets a physical connection from a datasource.
     * <p>
     * A few retries are done to work around databases that have problems with many open/close in a row.
     *
     * @param aDataSource the datasource
     * @return the connection
     */
    protected Connection getConnection(DataSource aDataSource) throws SQLException {
        return JDBCUtils.getConnection(aDataSource);
    }

    @Override
    public Session getSession() throws DirectoryException {
        checkConnection();
        SQLSession session = new SQLSession(this, getDescriptor());
        addSession(session);
        return session;
    }

    protected void checkConnection() {
        // double checked locking with volatile pattern to ensure concurrent lazy init
        if (dialect == null) {
            synchronized (this) {
                if (dialect == null) {
                    initConnection();
                }
            }
        }
    }

    protected void addSession(final SQLSession session) throws DirectoryException {
        super.addSession(session);
        registerInTx(session);
    }

    protected void registerInTx(final SQLSession session) throws DirectoryException {
        if (!TransactionHelper.isTransactionActive()) {
            return;
        }
        try {
            ConnectionHelper.registerSynchronization(new TxSessionCleaner(session));
        } catch (SystemException e) {
            throw new DirectoryException("Cannot register in tx for session cleanup handling " + this, e);
        }
    }

    public Map<String, Field> getSchemaFieldMap() {
        return schemaFieldMap;
    }

    public List<String> getStoredFieldNames() {
        return storedFieldNames;
    }

    public Table getTable() {
        return table;
    }

    public Dialect getDialect() {
        return dialect;
    }

    public boolean useNativeCase() {
        return nativeCase;
    }

    @Override
    public boolean isMultiTenant() {
        return table.getColumn(TENANT_ID_FIELD) != null;
    }

    @Override
    public String toString() {
        return "SQLDirectory [name=" + descriptor.name + "]";
    }

}
