/*
 * (C) Copyright 2006-2007 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *
 * $Id$
 */

package org.nuxeo.ecm.directory.sql;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.dialect.Dialect;
import org.nuxeo.ecm.directory.DirectoryException;
import org.nuxeo.ecm.directory.sql.repository.Column;
import org.nuxeo.ecm.directory.sql.repository.Insert;
import org.nuxeo.ecm.directory.sql.repository.Table;

import au.com.bytecode.opencsv.CSVReader;

/**
 * @author <a href='mailto:glefter@nuxeo.com'>George Lefter</a>
 * @author Florent Guillaume
 *
 */
public class SQLHelper {

    private static final Log log = LogFactory.getLog(SQLHelper.class);

    private static final Object DIRECTORY_INIT_LOCK = new Object();

    private final Table table;

    private final String tableName;

    private final Connection connection;

    private final String policy;

    private final Dialect dialect;

    private final String dataFileName;

    public static final String SQL_NULL_MARKER = "__NULL__";

    private static final String SQL_SCRIPT_CHARSET = "UTF-8";

    public SQLHelper(Connection connection, Dialect dialect, Table table,
            String dataFileName, String policy) {
        this.table = table;
        this.connection = connection;
        this.policy = policy;
        this.dataFileName = dataFileName;
        this.dialect = dialect;
        tableName = table.getName();
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
            if (policy.equals("on_missing_columns") && tableExists
                    && hasMatchingColumns()) {
                // all required columns were found
                log.debug("policy='on_missing_columns' and all column matched, skipping sql setup script");
                return false;
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

    private void createTable(boolean tableExists) throws DirectoryException {
        try {
            Statement stmt = connection.createStatement();

            if (tableExists) {
                // drop table
                String dropSql = table.getDropSql(dialect);
                log.debug("dropping table: " + dropSql);
                stmt.execute(dropSql);
            }

            String createSql = table.getCreateSql(dialect);
            log.debug("creating table: " + createSql);
            stmt.execute(createSql);
        } catch (SQLException e) {
            throw new DirectoryException(String.format(
                    "Table '%s' creation failed: %s", table, e.getMessage()), e);
        }
    }

    public boolean hasMatchingColumns() {
        ResultSet rs = null;
        String tableName = this.tableName;
        try {

            // Test whether there are new fields added in the schema that are
            // not present in the table schema. If so it is advised to
            // reinitialise the database.

            // fetch the database columns definitions
            DatabaseMetaData metadata = connection.getMetaData();
            rs = metadata.getColumns(null, "%", tableName, "%");

            Set<String> columnNames = new HashSet<String>();
            while (rs.next()) {
                columnNames.add(rs.getString("COLUMN_NAME"));
            }

            // check the field names match the column names (case-insensitive)
            for (Column column : table.getColumns()) {
                // TODO: check types as well
                String fieldName = column.getName();
                if (!columnNames.contains(fieldName)) {
                    log.debug(String.format(
                            "required field: %s, available columns: [%s]",
                            fieldName, columnNames));
                    return false;
                }
            }
            // all fields have a matching column, this looks not that bad
            log.debug(String.format("all fields matched for table '%s'",
                    tableName));
            return true;
        } catch (SQLException e) {
            log.warn("error while introspecting table: " + tableName, e);
            return false;
        } finally {
            try {
                rs.close();
            } catch (Exception e2) {
            }
        }
    }

    private boolean tableExists() throws DirectoryException {
        try {
            // Check if table exists using metadata
            DatabaseMetaData metaData = connection.getMetaData();
            String schemaName = null;
            String productName = metaData.getDatabaseProductName();
            if("Oracle".equals(productName)) {
                Statement st = connection.createStatement();
                String sql = "SELECT SYS_CONTEXT('USERENV', 'SESSION_USER') FROM DUAL";
                log.trace("SQL: " + sql);
                ResultSet rs = st.executeQuery(sql);
                rs.next();
                schemaName = rs.getString(1);
                log.trace("checking existing tables for oracle database, schema: " + schemaName);
                st.close();
            }
            ResultSet rs = metaData.getTables(null, schemaName, table.getName(),
                    new String[] { "TABLE" });
            boolean exists = rs.next();
            log.debug(String.format("checking if table %s exists: %s",
                    table.getName(), exists));
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
                values.add(String.format("%s: %s", i, columnValue));
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
        try {
            InputStream is = getClass().getClassLoader().getResourceAsStream(
                    dataFileName);
            if (is == null) {
                throw new DirectoryException("data file not found: "
                        + dataFileName);
            }

            csvReader = new CSVReader(new InputStreamReader(is,
                    SQL_SCRIPT_CHARSET));

            String[] columnNames = csvReader.readNext();
            List<Column> columns = new ArrayList<Column>();
            for (String columnName : columnNames) {
                String trimmedColumnName = columnName.trim();
                Column column = table.getColumn(trimmedColumnName);
                if (column == null) {
                    throw new DirectoryException("column not found: "
                            + trimmedColumnName);
                }
                columns.add(table.getColumn(trimmedColumnName));
            }
            Insert insert = new Insert(dialect, table, columns);
            String insertSql = insert.getStatement();
            log.debug("insert statement: " + insertSql);

            PreparedStatement ps = connection.prepareStatement(insertSql);

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
                for (int i = 0; i < columnNames.length; i++) {
                    Column column = columns.get(i);
                    String value = columnValues[i];
                    int columnLength = column.getLength();
                    if (value != null && value.length() > columnLength) {
                        log.warn(String.format(
                                "Possible invalid value (length > %s): %s",
                                columnLength, value));
                    }
                    switch (column.getSqlType()) {
                    case Types.VARCHAR:
                        if (SQL_NULL_MARKER.equals(value)) {
                            value = null;
                        }
                        ps.setString(i + 1, value);
                        break;
                    case Types.INTEGER:
                    case Types.DECIMAL:
                        try {
                            ps.setInt(i + 1, Integer.parseInt(value));
                        } catch (NumberFormatException e) {
                            throw new DirectoryException(
                                    String.format(
                                            "failed to set column '%s' on table '%s', values: %s",
                                            column.getName(), table.getName(),
                                            formatColumnValues(columnValues)),
                                    e);
                        }
                        break;
                    case Types.TIMESTAMP:
                        try {
                            ps.setTimestamp(i + 1, Timestamp.valueOf(value));
                        } catch (IllegalArgumentException e) {
                            throw new DirectoryException(
                                    String.format(
                                            "failed to set column '%s' on table '%s', values: %s",
                                            column.getName(), table.getName(),
                                            formatColumnValues(columnValues)),
                                    e);
                        }
                        break;
                    default:
                        throw new DirectoryException(
                                "unrecognized column type: "
                                        + column.getSqlType() + ", values: "
                                        + formatColumnValues(columnValues));
                    }
                }
                ps.execute();
            }
        } catch (IOException e) {
            throw new DirectoryException("Read error while reading data file: "
                    + dataFileName, e);
        } catch (SQLException e) {
            throw new DirectoryException(String.format(
                    "Table '%s' initialization failed: %s, values: %s",
                    table.getName(), e.getMessage(),
                    formatColumnValues(columnValues)), e);
        } finally {
            try {
                if (csvReader != null) {
                    csvReader.close();
                }
            } catch (IOException e) {
                throw new DirectoryException("Error closing data file: "
                        + dataFileName, e);

            }
        }
    }
}
