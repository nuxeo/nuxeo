/*
 * (C) Copyright 2006-2014 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Julien Carsique
 */
package org.nuxeo.ecm.directory.sql;

import static org.nuxeo.ecm.directory.BaseDirectoryDescriptor.CREATE_TABLE_POLICY_NEVER;
import static org.nuxeo.ecm.directory.BaseDirectoryDescriptor.CREATE_TABLE_POLICY_ON_MISSING_COLUMNS;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashSet;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.storage.sql.ColumnType;
import org.nuxeo.ecm.core.storage.sql.jdbc.JDBCLogger;
import org.nuxeo.ecm.core.storage.sql.jdbc.db.Column;
import org.nuxeo.ecm.core.storage.sql.jdbc.db.Table;
import org.nuxeo.ecm.core.storage.sql.jdbc.db.TableImpl;
import org.nuxeo.ecm.core.storage.sql.jdbc.dialect.Dialect;
import org.nuxeo.ecm.directory.DirectoryException;

public class SQLHelper {

    private static final Log log = LogFactory.getLog(SQLHelper.class);

    private static final Object DIRECTORY_INIT_LOCK = new Object();

    private final Table table;

    private final String tableName;

    private final Connection connection;

    private final String policy;

    private JDBCLogger logger = new JDBCLogger("SQLDirectory");

    public SQLHelper(Connection connection, Table table, String policy) {
        this.table = table;
        this.connection = connection;
        this.policy = policy;
        tableName = table.getPhysicalName();
    }

    /**
     * Sets up the table without loading the data in it.
     *
     * @return {@code true} if CSV data should be loaded
     */
    public boolean setupTable() {
        log.debug(String.format("setting up table '%s', policy='%s'", tableName, policy));
        if (policy.equals(CREATE_TABLE_POLICY_NEVER)) {
            log.debug("policy='" + CREATE_TABLE_POLICY_NEVER + "', skipping setup");
            return false;
        }
        synchronized (DIRECTORY_INIT_LOCK) {
            boolean tableExists = tableExists();
            // check the field names match the column names
            if (policy.equals(CREATE_TABLE_POLICY_ON_MISSING_COLUMNS) && tableExists) {
                if (hasMatchingColumns()) {
                    // all required columns were found
                    log.debug("policy='" + CREATE_TABLE_POLICY_ON_MISSING_COLUMNS
                            + "' and all column matched, skipping data load");
                } else {
                    log.debug("policy='" + CREATE_TABLE_POLICY_ON_MISSING_COLUMNS + "' and some columns are missing");
                    addMissingColumns();
                }
                return false;
            } // else policy=always or table doesn't exist
            createTable(tableExists);
            return true; // load data
        }
    }

    private void addMissingColumns() {
        try (Statement stmt = connection.createStatement()) {

            for (Column column : getMissingColumns(false)) {
                String alter = table.getAddColumnSql(column);
                if (logger.isLogEnabled()) {
                    logger.log(alter);
                }
                stmt.execute(alter);
            }
        } catch (SQLException e) {
            throw new DirectoryException(String.format("Table '%s' alteration failed: %s", table, e.getMessage()), e);
        }
    }

    private void createTable(boolean drop) {
        try (Statement stmt = connection.createStatement()) {
            if (drop) {
                // drop table
                String dropSql = table.getDropSql();
                if (logger.isLogEnabled()) {
                    logger.log(dropSql);
                }
                stmt.execute(dropSql);
            }

            String createSql = table.getCreateSql();
            if (logger.isLogEnabled()) {
                logger.log(createSql);
            }
            stmt.execute(createSql);
            for (String sql : table.getPostCreateSqls(null)) {
                if (logger.isLogEnabled()) {
                    logger.log(sql);
                }
                stmt.execute(sql);
            }
        } catch (SQLException e) {
            throw new DirectoryException(String.format("Table '%s' creation failed: %s", table, e.getMessage()), e);
        }
    }

    public boolean hasMatchingColumns() {
        Set<Column> missingColumns = getMissingColumns(true);
        if (missingColumns == null || missingColumns.size() > 0) {
            return false;
        } else {
            // all fields have a matching column, this looks not that bad
            log.debug(String.format("all fields matched for table '%s'", tableName));
            return true;
        }
    }

    public Set<Column> getMissingColumns(Boolean breakAtFirstMissing) {
        try {
            Set<Column> missingColumns = new HashSet<>();

            // Test whether there are new fields added in the schema that are
            // not present in the table schema. If so it is advised to
            // reinitialise the database.

            Set<String> columnNames = getPhysicalColumns();

            // check the field names match the column names (case-insensitive)
            for (Column column : table.getColumns()) {
                // TODO: check types as well
                String fieldName = column.getPhysicalName();
                if (!columnNames.contains(fieldName)) {
                    log.debug(String.format("required field: %s is missing", fieldName));
                    missingColumns.add(column);

                    if (breakAtFirstMissing) {
                        return null;
                    }
                }
            }

            return missingColumns;
        } catch (SQLException e) {
            log.warn("error while introspecting table: " + tableName, e);
            return null;
        }
    }

    private Set<String> getPhysicalColumns() throws SQLException {
        ResultSet rs = null;
        Set<String> columnNames = new HashSet<>();
        try {
            // fetch the database columns definitions
            DatabaseMetaData metadata = connection.getMetaData();
            rs = metadata.getColumns(null, "%", tableName, "%");

            while (rs.next()) {
                columnNames.add(rs.getString("COLUMN_NAME"));
            }
        } finally {
            if (rs != null) {
                try {
                    rs.close();
                } catch (SQLException e) {
                    log.warn("Error while trying to close result set", e);
                }
            }
        }
        return columnNames;
    }

    private boolean tableExists() {
        try {
            // Check if table exists using metadata
            DatabaseMetaData metaData = connection.getMetaData();
            String schemaName = null;
            String productName = metaData.getDatabaseProductName();
            if ("Oracle".equals(productName)) {
                try (Statement st = connection.createStatement()) {
                    String sql = "SELECT SYS_CONTEXT('USERENV', 'SESSION_USER') FROM DUAL";
                    log.trace("SQL: " + sql);
                    try (ResultSet rs = st.executeQuery(sql)) {
                        rs.next();
                        schemaName = rs.getString(1);
                        log.trace("checking existing tables for oracle database, schema: " + schemaName);
                    }
                }
            }
            try (ResultSet rs = metaData.getTables(null, schemaName, table.getPhysicalName(), new String[] { "TABLE" })) {
                boolean exists = rs.next();
                log.debug(String.format("checking if table %s exists: %s", table.getPhysicalName(), Boolean.valueOf(exists)));
                return exists;
            }
        } catch (SQLException e) {
            throw new DirectoryException(e);
        }
    }

    public static Table addTable(String name, Dialect dialect, boolean nativeCase) {
        String physicalName = dialect.getTableName(name);
        if (!nativeCase && name.length() == physicalName.length()) {
            // we can keep the name specified in the config
            physicalName = name;
        }
        return new TableImpl(dialect, physicalName, physicalName);
    }

    public static Column addColumn(Table table, String fieldName, ColumnType type, boolean nativeCase) {
        String physicalName = table.getDialect().getColumnName(fieldName);
        if (!nativeCase && fieldName.length() == physicalName.length()) {
            // we can keep the name specified in the config
            physicalName = fieldName;
        }
        Column column = new Column(table, physicalName, type, fieldName);
        return ((TableImpl) table).addColumn(fieldName, column);
    }

}
