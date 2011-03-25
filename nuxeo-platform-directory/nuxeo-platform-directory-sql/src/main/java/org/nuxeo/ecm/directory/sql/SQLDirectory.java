/*
 * (C) Copyright 2006-2011 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     George Lefter
 *     Florent Guillaume
 */
package org.nuxeo.ecm.directory.sql;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.sql.DataSource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.schema.NXSchema;
import org.nuxeo.ecm.core.schema.types.Field;
import org.nuxeo.ecm.core.schema.types.Schema;
import org.nuxeo.ecm.core.storage.StorageException;
import org.nuxeo.ecm.core.storage.sql.ColumnType;
import org.nuxeo.ecm.core.storage.sql.jdbc.db.Column;
import org.nuxeo.ecm.core.storage.sql.jdbc.db.Table;
import org.nuxeo.ecm.core.storage.sql.jdbc.dialect.Dialect;
import org.nuxeo.ecm.directory.AbstractDirectory;
import org.nuxeo.ecm.directory.Directory;
import org.nuxeo.ecm.directory.DirectoryException;
import org.nuxeo.ecm.directory.DirectoryServiceImpl;
import org.nuxeo.ecm.directory.IdGenerator;
import org.nuxeo.ecm.directory.Session;
import org.nuxeo.ecm.directory.api.DirectoryService;
import org.nuxeo.runtime.RuntimeService;
import org.nuxeo.runtime.api.DataSourceHelper;
import org.nuxeo.runtime.api.Framework;

public class SQLDirectory extends AbstractDirectory {

    private static final Log log = LogFactory.getLog(SQLDirectory.class);

    private final SQLDirectoryDescriptor config;

    private final boolean nativeCase;

    private boolean managedSQLSession;

    private DataSource dataSource;

    private final SimpleIdGenerator idGenerator;
    
    private List<Session> sessions = new ArrayList<Session>();

    private final Table table;

    private final Schema schema;

    private final Map<String, Field> schemaFieldMap;

    private final List<String> storedFieldNames;

    private final Dialect dialect;

    public SQLDirectory(SQLDirectoryDescriptor config) throws ClientException {
        this.config = config;
        nativeCase = Boolean.TRUE.equals(config.nativeCase);

        // register the references to other directories
        addReferences(config.getInverseReferences());
        addReferences(config.getTableReferences());

        // cache parameterization
        cache.setMaxSize(config.getCacheMaxSize());
        cache.setTimeout(config.getCacheTimeout());

        Connection sqlConnection = getConnection();
        try {
            dialect = Dialect.createDialect(sqlConnection, null, null);

            if (config.initDependencies != null) {
                // initialize dependent directories first
                final RuntimeService runtime = Framework.getRuntime();
                DirectoryServiceImpl directoryService = (DirectoryServiceImpl) runtime.getComponent(DirectoryService.NAME);
                for (String dependency : config.initDependencies) {
                    log.debug("initializing dependencies first: " + dependency);
                    Directory dir = directoryService.getDirectory(dependency);
                    dir.getName();
                }
            }
            // setup table and fields maps
            table = SQLHelper.addTable(config.tableName, dialect,
                    useNativeCase());
            schema = NXSchema.getSchemaManager().getSchema(config.schemaName);
            if (schema == null) {
                throw new DirectoryException("schema not found: "
                        + config.schemaName);
            }
            schemaFieldMap = new LinkedHashMap<String, Field>();
            storedFieldNames = new LinkedList<String>();
            boolean hasPrimary = false;
            for (Field f : schema.getFields()) {
                String fieldName = f.getName().toString();
                schemaFieldMap.put(fieldName, f);

                if (!isReference(fieldName)) {
                    // list of fields that are actually stored in the table of
                    // the
                    // current directory and not read from an external reference
                    storedFieldNames.add(fieldName);

                    ColumnType type = ColumnType.fromField(f);
                    Column column = SQLHelper.addColumn(table, fieldName, type,
                            useNativeCase());
                    if (fieldName.equals(config.getIdField())) {
                        column.setPrimary(true);
                        hasPrimary = true;
                    }
                    Object defaultValue = f.getDefaultValue();
                    if (defaultValue != null) {
                        column.setDefaultValue(defaultValue.toString());
                    }
                }
            }
            if (!hasPrimary) {
                throw new DirectoryException(
                        String.format(
                                "Directory '%s' id field '%s' is not present in schema '%s'",
                                getName(), getIdField(), getSchema()));
            }

            SQLHelper helper = new SQLHelper(sqlConnection, table,
                    config.dataFileName,
                    config.getDataFileCharacterSeparator(),
                    config.createTablePolicy);
            helper.setupTable();

            if (config.autoincrementIdField) {
                idGenerator = new SimpleIdGenerator(sqlConnection, table,
                        config.getIdField());
            } else {
                idGenerator = null;
            }

            try {
                if (config.dataSourceName == null) {
                    sqlConnection.commit();
                }
            } catch (SQLException e) {
                throw new DirectoryException(e);
            }
        } catch (StorageException e) {
            throw new DirectoryException(e);
        } finally {
            try {
                sqlConnection.close();
            } catch (Exception e) {
                throw new DirectoryException(e);
            }
        }
    }

    public SQLDirectoryDescriptor getConfig() {
        // utility method to simplify testing
        return config;
    }

    public DataSource getDataSource() throws DirectoryException {
        if (dataSource != null) {
            return dataSource;
        }
        try {
            if (config.dataSourceName != null) {
                managedSQLSession = true;
                dataSource = DataSourceHelper.getDataSource(config.dataSourceName);
                // InitialContext context = new InitialContext();
                // dataSource = (DataSource)
                // context.lookup(config.dataSourceName);
            } else {
                managedSQLSession = false;
                dataSource = new SimpleDataSource(config.dbUrl,
                        config.dbDriver, config.dbUser, config.dbPassword);
            }
            log.trace("found datasource: " + dataSource);
            return dataSource;
        } catch (Exception e) {
            log.error("dataSource lookup failed", e);
            throw new DirectoryException("dataSource lookup failed", e);
        }
    }

    private Connection getConnection() throws DirectoryException {
        try {
            return getDataSource().getConnection();
        } catch (SQLException e) {
            throw new DirectoryException("could not obtain a connection", e);
        }
    }

    @Override
    public IdGenerator getIdGenerator() {
        return idGenerator;
    }

    @Override
    public String getName() {
        return config.getName();
    }

    @Override
    public String getSchema() {
        return config.getSchemaName();
    }

    @Override
    public String getParentDirectory() {
        return config.getParentDirectory();
    }

    @Override
    public String getIdField() {
        return config.getIdField();
    }

    @Override
    public String getPasswordField() {
        return config.getPasswordField();
    }

    @Override
    public Session getSession() throws DirectoryException {
        Session session = new SQLSession(this, config, idGenerator,
                managedSQLSession);
        sessions.add(session);
        return session;
    }

    void removeSession(Session session) {
        sessions.remove(session);
    }

    @Override
    public void shutdown() {
        synchronized(sessions) {
            if (sessions.isEmpty()) {
                return;
            }
            List<Session> lastSessions = sessions;
            sessions = new ArrayList<Session>();
            for (Session session:lastSessions) {
                try {
                    session.close();
                } catch (DirectoryException e) {
                   log.error("Error during " + this.getName() + " shutdown", e);
                }
            }
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

}
