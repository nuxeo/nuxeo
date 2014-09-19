/*
 * (C) Copyright 2006-2014 Nuxeo SA (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl-2.1.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     George Lefter
 *     Florent Guillaume
 *     Julien Carsique
 */
package org.nuxeo.ecm.directory.sql;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Serializable;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.storage.sql.ColumnSpec;
import org.nuxeo.ecm.core.storage.sql.ColumnType;
import org.nuxeo.ecm.core.storage.sql.jdbc.JDBCLogger;
import org.nuxeo.ecm.core.storage.sql.jdbc.db.Column;
import org.nuxeo.ecm.core.storage.sql.jdbc.db.Insert;
import org.nuxeo.ecm.core.storage.sql.jdbc.db.Table;
import org.nuxeo.ecm.core.storage.sql.jdbc.db.TableImpl;
import org.nuxeo.ecm.core.storage.sql.jdbc.dialect.Dialect;
import org.nuxeo.ecm.directory.DirectoryException;
import org.nuxeo.runtime.api.Framework;

import au.com.bytecode.opencsv.CSVReader;

public class SQLHelper {

    private static final Log log = LogFactory.getLog(SQLHelper.class);

    public static final String SQL_NULL_MARKER = "__NULL__";

    private static final String SQL_SCRIPT_CHARSET = "UTF-8";

    private static final Object DIRECTORY_INIT_LOCK = new Object();

    private final Table table;

    private final String tableName;

    private final Connection connection;

    private final String policy;

    private final String dataFileName;

    protected final char characterSeparator;

    private JDBCLogger logger = new JDBCLogger("SQLDirectory");

    public SQLHelper(Connection connection, Table table, String dataFileName,
            char characterSeparator, String policy) {
        this.table = table;
        this.connection = connection;
        this.policy = policy;
        this.dataFileName = dataFileName;
        tableName = table.getPhysicalName();
        this.characterSeparator = characterSeparator;
    }

    public SQLHelper(Connection connection, Table table, String dataFileName,
            String policy) {
        this(connection, table, dataFileName, ',', policy);
    }

    public boolean setupTable() throws DirectoryException {
        log.debug(String.format("setting up table '%s', policy='%s'",
                tableName, policy));
        if (policy.equals("never")) {
            log.debug("policy='never', skipping setup");
            return false;
        }

        synchronized (DIRECTORY_INIT_LOCK) {

            boolean tableExists = tableExists();

            // check the field names match the column names
            if (policy.equals("on_missing_columns") && tableExists) {
                if (hasMatchingColumns()) {
                    // all required columns were found
                    log.debug("policy='on_missing_columns' and all column matched, skipping sql setup script");
                    return false;
                } else {
                    log.debug("policy='on_missing_columns' and some columns are missing");
                    addMissingColumns();
                    return true;
                }
            }

            createTable(tableExists);

            if (dataFileName == null) {
                // no dataFile found, do not try to execute it
                log.debug(String.format("Table '%s': no data file found",
                        tableName));
                return true;
            }

            loadData();
        }

        return true;
    }

    private void addMissingColumns() throws DirectoryException {
        try {
            Statement stmt = connection.createStatement();

            for (Column column : getMissingColumns(false)) {
                String alter = table.getAddColumnSql(column);
                if (logger.isLogEnabled()) {
                    logger.log(alter);
                }
                stmt.execute(alter);
            }
        } catch (SQLException e) {
            throw new DirectoryException(String.format(
                    "Table '%s' alteration failed: %s", table, e.getMessage()),
                    e);
        }
    }

    private void createTable(boolean tableExists) throws DirectoryException {
        try {
            Statement stmt = connection.createStatement();

            if (tableExists) {
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
            throw new DirectoryException(String.format(
                    "Table '%s' creation failed: %s", table, e.getMessage()), e);
        }
    }

    public boolean hasMatchingColumns() {
        Set<Column> missingColumns = getMissingColumns(true);
        if (missingColumns == null || missingColumns.size() > 0) {
            return false;
        } else {
            // all fields have a matching column, this looks not that bad
            log.debug(String.format("all fields matched for table '%s'",
                    tableName));
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
                    log.debug(String.format("required field: %s is missing",
                            fieldName));
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
            try {
                if (rs != null) {
                    rs.close();
                }
            } catch (Exception e) {
                log.warn("Error while trying to close result set", e);
            }
        }
        return columnNames;
    }

    private boolean tableExists() throws DirectoryException {
        try {
            // Check if table exists using metadata
            DatabaseMetaData metaData = connection.getMetaData();
            String schemaName = null;
            String productName = metaData.getDatabaseProductName();
            if ("Oracle".equals(productName)) {
                Statement st = connection.createStatement();
                String sql = "SELECT SYS_CONTEXT('USERENV', 'SESSION_USER') FROM DUAL";
                log.trace("SQL: " + sql);
                ResultSet rs = st.executeQuery(sql);
                rs.next();
                schemaName = rs.getString(1);
                log.trace("checking existing tables for oracle database, schema: "
                        + schemaName);
                st.close();
            }
            ResultSet rs = metaData.getTables(null, schemaName,
                    table.getPhysicalName(), new String[] { "TABLE" });
            boolean exists = rs.next();
            log.debug(String.format("checking if table %s exists: %s",
                    table.getPhysicalName(), Boolean.valueOf(exists)));
            return exists;
        } catch (SQLException e) {
            throw new DirectoryException(e);
        }
    }

    private static String formatColumnValues(String[] columnValues) {
        StringBuilder buffer = new StringBuilder();
        buffer.append('[');
        if (columnValues != null) {
            int i = 0;
            List<String> values = new ArrayList<String>();
            for (String columnValue : columnValues) {
                values.add(i + ": " + columnValue);
                i++;
            }
            buffer.append(StringUtils.join(values.iterator(), ", "));
        }
        buffer.append(']');
        return buffer.toString();
    }

    private void loadData() throws DirectoryException {
        log.debug("loading data file: " + dataFileName);
        CSVReader csvReader = null;
        String[] columnValues = null;
        PreparedStatement ps = null;
        try {
            InputStream is = getClass().getClassLoader().getResourceAsStream(
                    dataFileName);
            if (is == null) {
                is = Framework.getResourceLoader().getResourceAsStream(
                        dataFileName);
                if (is == null) {
                    throw new DirectoryException("data file not found: "
                            + dataFileName);
                }
            }

            csvReader = new CSVReader(new InputStreamReader(is,
                    SQL_SCRIPT_CHARSET), characterSeparator);

            String[] columnNames = csvReader.readNext();
            List<Column> columns = new ArrayList<Column>();
            Insert insert = new Insert(table);
            for (String columnName : columnNames) {
                String trimmedColumnName = columnName.trim();
                Column column = table.getColumn(trimmedColumnName);
                if (column == null) {
                    throw new DirectoryException("column not found: "
                            + trimmedColumnName);
                }
                columns.add(table.getColumn(trimmedColumnName));
                insert.addColumn(column);
            }
            String insertSql = insert.getStatement();
            log.debug("insert statement: " + insertSql);

            ps = connection.prepareStatement(insertSql);

            while ((columnValues = csvReader.readNext()) != null) {
                if (columnValues.length == 0
                        || (columnValues.length == 1 && "".equals(columnValues[0]))) {
                    // NXP-2538: allow columns with only one value but skip
                    // empty lines
                    continue;
                }
                if (columnValues.length != columnNames.length) {
                    log.error("invalid column count while reading csv file: "
                            + dataFileName + ", values: "
                            + formatColumnValues(columnValues));
                    continue;
                }

                if (logger.isLogEnabled()) {
                    List<Serializable> values = new ArrayList<Serializable>(
                            columnNames.length);
                    for (int i = 0; i < columnNames.length; i++) {
                        String value = columnValues[i];
                        if (SQL_NULL_MARKER.equals(value)) {
                            value = null;
                        }
                        values.add(value);
                    }
                    logger.logSQL(insertSql, values);
                }

                for (int i = 0; i < columnNames.length; i++) {
                    Column column = columns.get(i);
                    String value = columnValues[i];
                    // int columnLength = column.getLength();
                    // if (value != null && value.length() > columnLength) {
                    // log.warn(String.format(
                    // "Possible invalid value (length > %s): %s",
                    // columnLength, value));
                    // }
                    Serializable v;
                    if (SQL_NULL_MARKER.equals(value)) {
                        v = null;
                    } else if (column.getType().spec == ColumnSpec.STRING) {
                        v = value;
                    } else if (column.getType().spec == ColumnSpec.BOOLEAN) {
                        v = Boolean.valueOf(value);
                    } else if (column.getType().spec == ColumnSpec.LONG) {
                        try {
                            v = Long.valueOf(value);
                        } catch (NumberFormatException e) {
                            throw new DirectoryException(
                                    String.format(
                                            "failed to set column '%s' on table '%s', values: %s",
                                            column.getPhysicalName(),
                                            table.getPhysicalName(),
                                            formatColumnValues(columnValues)),
                                    e);
                        }
                    } else if (column.getType().spec == ColumnSpec.TIMESTAMP) {
                        try {
                            Calendar cal = new GregorianCalendar();
                            cal.setTime(Timestamp.valueOf(value));
                            v = cal;
                        } catch (IllegalArgumentException e) {
                            throw new DirectoryException(
                                    String.format(
                                            "failed to set column '%s' on table '%s', values: %s",
                                            column.getPhysicalName(),
                                            table.getPhysicalName(),
                                            formatColumnValues(columnValues)),
                                    e);
                        }
                    } else if (column.getType().spec == ColumnSpec.DOUBLE) {
                        try {
                            v = Double.valueOf(value);
                        } catch (NumberFormatException e) {
                            throw new DirectoryException(
                                    String.format(
                                            "failed to set column '%s' on table '%s', values: %s",
                                            column.getPhysicalName(),
                                            table.getPhysicalName(),
                                            formatColumnValues(columnValues)),
                                    e);
                        }
                    } else {
                        throw new DirectoryException(
                                "unrecognized column type: " + column.getType()
                                        + ", values: "
                                        + formatColumnValues(columnValues));
                    }
                    column.setToPreparedStatement(ps, i + 1, v);
                }
                ps.execute();
            }
        } catch (IOException e) {
            throw new DirectoryException("Read error while reading data file: "
                    + dataFileName, e);
        } catch (SQLException e) {
            throw new DirectoryException(String.format(
                    "Table '%s' initialization failed: %s, values: %s",
                    table.getPhysicalName(), e.getMessage(),
                    formatColumnValues(columnValues)), e);
        } finally {
            try {
                if (csvReader != null) {
                    csvReader.close();
                }
                if (ps != null) {
                    ps.close();
                }
            } catch (IOException e) {
                throw new DirectoryException("Error closing data file: "
                        + dataFileName, e);
            } catch (SQLException sqle) {
                throw new DirectoryException(sqle);
            }
        }
    }

    public static Table addTable(String name, Dialect dialect,
            boolean nativeCase) {
        String physicalName = dialect.getTableName(name);
        if (!nativeCase && name.length() == physicalName.length()) {
            // we can keep the name specified in the config
            physicalName = name;
        }
        return new TableImpl(dialect, physicalName, physicalName);
    }

    public static Column addColumn(Table table, String fieldName,
            ColumnType type, boolean nativeCase) {
        String physicalName = table.getDialect().getColumnName(fieldName);
        if (!nativeCase && fieldName.length() == physicalName.length()) {
            // we can keep the name specified in the config
            physicalName = fieldName;
        }
        Column column = new Column(table, physicalName, type, fieldName);
        return ((TableImpl) table).addColumn(fieldName, column);
    }

}
