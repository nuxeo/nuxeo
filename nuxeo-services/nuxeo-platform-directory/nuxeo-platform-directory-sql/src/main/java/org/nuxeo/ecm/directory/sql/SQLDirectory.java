/*
 * (C) Copyright 2006-2018 Nuxeo (http://nuxeo.com/) and others.
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
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

import javax.transaction.Synchronization;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.schema.SchemaManager;
import org.nuxeo.ecm.core.schema.types.Field;
import org.nuxeo.ecm.core.schema.types.Schema;
import org.nuxeo.ecm.core.storage.sql.ColumnType;
import org.nuxeo.ecm.core.storage.sql.jdbc.db.Column;
import org.nuxeo.ecm.core.storage.sql.jdbc.db.Table;
import org.nuxeo.ecm.core.storage.sql.jdbc.dialect.Dialect;
import org.nuxeo.ecm.directory.AbstractDirectory;
import org.nuxeo.ecm.directory.DirectoryException;
import org.nuxeo.ecm.directory.Reference;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.datasource.ConnectionHelper;
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

    private final boolean nativeCase;

    private Table table;

    private Schema schema;

    // columns to fetch when an entry is read (with the password)
    protected List<Column> readColumnsAll;

    // columns to fetch when an entry is read (excludes the password)
    protected List<Column> readColumns;

    // id column
    protected Column idColumn;

    // columns to fetch when an entry is read (with the password), as SQL
    protected String readColumnsAllSQL;

    // columns to fetch when an entry is read (excludes the password), as SQL
    protected String readColumnsSQL;

    private volatile Dialect dialect;

    public SQLDirectory(SQLDirectoryDescriptor descriptor) {
        super(descriptor, TableReference.class);

        nativeCase = Boolean.TRUE.equals(descriptor.nativeCase);

        // Cache fallback
        fallbackOnDefaultCache();
    }

    @Override
    public SQLDirectoryDescriptor getDescriptor() {
        return (SQLDirectoryDescriptor) descriptor;
    }

    @Override
    protected void addReferences() {
        super.addReferences();
        // add backward compat tableReferences
        TableReferenceDescriptor[] descs = getDescriptor().getTableReferences();
        if (descs != null) {
            Arrays.stream(descs).map(TableReference::new).forEach(this::addReference);
        }
    }

    @Override
    public void initialize() {
        super.initialize();
        SQLDirectoryDescriptor descriptor = getDescriptor();
        try (Connection sqlConnection = getConnection()) {
            dialect = Dialect.createDialect(sqlConnection, null);
            // setup table and fields maps
            String tableName = descriptor.tableName == null ? descriptor.name : descriptor.tableName;
            table = SQLHelper.addTable(tableName, dialect, useNativeCase());
            SchemaManager schemaManager = Framework.getService(SchemaManager.class);
            schema = schemaManager.getSchema(getSchema());
            if (schema == null) {
                throw new DirectoryException("schema not found: " + getSchema());
            }
            readColumnsAll = new LinkedList<>();
            readColumns = new LinkedList<>();
            boolean hasPrimary = false;
            for (Field f : schema.getFields()) {
                String fieldName = f.getName().getLocalName();

                if (!isReference(fieldName)) {
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
                        idColumn = column;
                        hasPrimary = true;
                    }
                    readColumnsAll.add(column);
                    if (!fieldName.equals(descriptor.passwordField)) {
                        readColumns.add(column);
                    }
                }
            }
            readColumnsAllSQL = readColumnsAll.stream().map(Column::getQuotedName).collect(Collectors.joining(", "));
            readColumnsSQL = readColumns.stream().map(Column::getQuotedName).collect(Collectors.joining(", "));
            if (!hasPrimary) {
                throw new DirectoryException(String.format("Directory '%s' id field '%s' is not present in schema '%s'",
                        getName(), getIdField(), getSchema()));
            }

            SQLHelper helper = new SQLHelper(sqlConnection, table, descriptor.getCreateTablePolicy());
            boolean tableExists = !helper.setupTable();
            // commit the transaction so that tables are committed
            if (TransactionHelper.isTransactionActiveOrMarkedRollback()) {
                TransactionHelper.commitOrRollbackTransaction();
                TransactionHelper.startTransaction();
            }
            loadDataOnInit(tableExists);

        } catch (SQLException e) {
            // exception on close
            throw new DirectoryException(e);
        }
    }

    @Override
    public void initializeReferences() {
        try (Connection connection = getConnection()) {
            for (Reference reference : getReferences()) {
                if (reference instanceof TableReference) {
                    ((TableReference) reference).initialize(connection);
                }
            }
        } catch (SQLException e) {
            throw new DirectoryException(e);
        }
    }

    public Connection getConnection() {
        SQLDirectoryDescriptor descriptor = getDescriptor();
        if (StringUtils.isBlank(descriptor.dataSourceName)) {
            throw new DirectoryException("Missing dataSource for SQL directory: " + getName());
        }
        try {
            return ConnectionHelper.getConnection(descriptor.dataSourceName);
        } catch (SQLException e) {
            throw new DirectoryException("Cannot connect to SQL directory '" + getName() + "': " + e.getMessage(), e);
        }
    }

    @Override
    public SQLSession getSession() {
        SQLSession session = new SQLSession(this, getDescriptor());
        addSession(session);
        return session;
    }

    protected void addSession(final SQLSession session) {
        super.addSession(session);
        registerInTx(session);
    }

    protected void registerInTx(final SQLSession session) {
        if (!TransactionHelper.isTransactionActive()) {
            return;
        }
        TransactionHelper.registerSynchronization(new TxSessionCleaner(session));
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
