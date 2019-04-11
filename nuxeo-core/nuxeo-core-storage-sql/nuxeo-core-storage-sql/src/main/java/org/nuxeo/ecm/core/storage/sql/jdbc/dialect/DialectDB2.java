/*
 * (C) Copyright 2006-2013 Nuxeo SA (http://nuxeo.com/) and others.
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
package org.nuxeo.ecm.core.storage.sql.jdbc.dialect;

import java.io.Serializable;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.nuxeo.ecm.core.storage.sql.ColumnType;
import org.nuxeo.ecm.core.storage.sql.Model;
import org.nuxeo.ecm.core.storage.sql.RepositoryDescriptor;
import org.nuxeo.ecm.core.storage.sql.jdbc.JDBCLogger;
import org.nuxeo.ecm.core.storage.sql.jdbc.db.Column;
import org.nuxeo.ecm.core.storage.sql.jdbc.db.Database;
import org.nuxeo.ecm.core.storage.sql.jdbc.db.Table;

/**
 * DB2-specific dialect.
 */
public class DialectDB2 extends Dialect {

    protected final String fulltextParameters;

    private static final String DEFAULT_USERS_SEPARATOR = "|";

    protected String usersSeparator;

    public DialectDB2(DatabaseMetaData metadata, RepositoryDescriptor repositoryDescriptor) {
        super(metadata, repositoryDescriptor);
        fulltextParameters = repositoryDescriptor == null ? null
                : repositoryDescriptor.getFulltextAnalyzer() == null ? "" : repositoryDescriptor.getFulltextAnalyzer();
        usersSeparator = repositoryDescriptor == null ? null
                : repositoryDescriptor.usersSeparatorKey == null ? DEFAULT_USERS_SEPARATOR
                        : repositoryDescriptor.usersSeparatorKey;
        fulltextDisabled = true;
        if (repositoryDescriptor != null) {
            repositoryDescriptor.setFulltextDisabled(true);
        }
    }

    @Override
    public String getCascadeDropConstraintsString() {
        return " CASCADE";
    }

    @Override
    public JDBCInfo getJDBCTypeAndString(ColumnType type) {
        switch (type.spec) {
        case STRING:
            if (type.isUnconstrained()) {
                return jdbcInfo("VARCHAR(255)", Types.VARCHAR);
            } else if (type.isClob() || type.length > 2000) {
                return jdbcInfo("CLOB", Types.CLOB);
            } else {
                return jdbcInfo("VARCHAR(%d)", type.length, Types.VARCHAR);
            }
        case BOOLEAN:
            return jdbcInfo("SMALLINT", Types.BIT);
        case LONG:
            return jdbcInfo("BIGINT", Types.BIGINT);
        case DOUBLE:
            return jdbcInfo("DOUBLE", Types.DOUBLE);
        case TIMESTAMP:
            return jdbcInfo("TIMESTAMP", Types.TIMESTAMP);
        case BLOBID:
            return jdbcInfo("VARCHAR(250)", Types.VARCHAR);
            // -----
        case NODEID:
        case NODEIDFK:
        case NODEIDFKNP:
        case NODEIDFKMUL:
        case NODEIDFKNULL:
        case NODEIDPK:
        case NODEVAL:
            return jdbcInfo("VARCHAR(36)", Types.VARCHAR);
        case SYSNAME:
        case SYSNAMEARRAY:
            return jdbcInfo("VARCHAR(250)", Types.VARCHAR);
        case TINYINT:
            return jdbcInfo("SMALLINT", Types.TINYINT);
        case INTEGER:
            return jdbcInfo("INTEGER", Types.INTEGER);
        case AUTOINC:
            return jdbcInfo("INTEGER", Types.INTEGER); // TODO
        case FTINDEXED:
            return jdbcInfo("CLOB", Types.CLOB);
        case FTSTORED:
            return jdbcInfo("CLOB", Types.CLOB);
        case CLUSTERNODE:
            return jdbcInfo("VARCHAR(25)", Types.VARCHAR);
        case CLUSTERFRAGS:
            return jdbcInfo("VARCHAR(4000)", Types.VARCHAR);
        default:
            throw new AssertionError(type);
        }
    }

    @Override
    public boolean isAllowedConversion(int expected, int actual, String actualName, int actualSize) {
        if (expected == Types.BIT && actual == Types.SMALLINT) {
            return true;
        }
        return false;
    }

    @Override
    public void setToPreparedStatement(PreparedStatement ps, int index, Serializable value, Column column)
            throws SQLException {
        switch (column.getJdbcType()) {
        case Types.VARCHAR:
        case Types.CLOB:
            setToPreparedStatementString(ps, index, value, column);
            return;
        case Types.BIT:
            ps.setInt(index, ((Boolean) value).booleanValue() ? 1 : 0);
            return;
        case Types.TINYINT:
        case Types.SMALLINT:
            ps.setInt(index, ((Long) value).intValue());
            return;
        case Types.INTEGER:
        case Types.BIGINT:
            ps.setLong(index, ((Number) value).longValue());
            return;
        case Types.DOUBLE:
            ps.setDouble(index, ((Double) value).doubleValue());
            return;
        case Types.TIMESTAMP:
            setToPreparedStatementTimestamp(ps, index, value, column);
            return;
        default:
            throw new SQLException("Unhandled JDBC type: " + column.getJdbcType());
        }
    }

    @Override
    @SuppressWarnings("boxing")
    public Serializable getFromResultSet(ResultSet rs, int index, Column column) throws SQLException {
        switch (column.getJdbcType()) {
        case Types.VARCHAR:
        case Types.CLOB:
            return getFromResultSetString(rs, index, column);
        case Types.BIT:
            return rs.getBoolean(index);
        case Types.TINYINT:
        case Types.SMALLINT:
        case Types.INTEGER:
        case Types.BIGINT:
            return rs.getLong(index);
        case Types.DOUBLE:
            return rs.getDouble(index);
        case Types.TIMESTAMP:
            return getFromResultSetTimestamp(rs, index, column);
        }
        throw new SQLException("Unhandled JDBC type: " + column.getJdbcType());
    }

    @Override
    protected int getMaxNameSize() {
        // since DB2 9.5
        // http://publib.boulder.ibm.com/infocenter/db2luw/v9r5/index.jsp?topic=%2Fcom.ibm.db2.luw.wn.doc%2Fdoc%2Fc0051391.html
        return 128;
    }

    @Override
    public boolean supportsReadAcl() {
        return false; // TODO
    }

    @Override
    public boolean isClusteringSupported() {
        return false;
    }

    @Override
    public boolean supportsPaging() {
        return false;
    }

    // check
    // http://www.channeldb2.com/profiles/blogs/porting-limit-and-offset
    // http://programmingzen.com/2010/06/02/enabling-limit-and-offset-in-db2-9-7-2/
    // https://www.ibm.com/developerworks/mydeveloperworks/blogs/SQLTips4DB2LUW/entry/limit_offset?lang=en
    @Override
    public String addPagingClause(String sql, long limit, long offset) {
        return null;
    }

    @Override
    public String getSQLStatementsFilename() {
        return "nuxeovcs/db2.sql.txt";
    }

    @Override
    public String getTestSQLStatementsFilename() {
        return "nuxeovcs/db2.test.sql.txt";
    }

    @Override
    public Map<String, Serializable> getSQLStatementsProperties(Model model, Database database) {
        Map<String, Serializable> properties = new HashMap<>();
        properties.put("idType", "VARCHAR(36)");
        properties.put("argIdType", "VARCHAR(36)"); // in function args
        return properties;
    }

    @Override
    public String getValidationQuery() {
        return "VALUES 1";
    }

    public String getUsersSeparator() {
        if (usersSeparator == null) {
            return DEFAULT_USERS_SEPARATOR;
        }
        return usersSeparator;
    }

    @Override
    public int getFulltextIndexedColumns() {
        return 2;
    }

    @Override
    public boolean getMaterializeFulltextSyntheticColumn() {
        return true;
    }

    @Override
    public String getCreateFulltextIndexSql(String indexName, String quotedIndexName, Table table,
            List<Column> columns, Model model) {
        throw new UnsupportedOperationException();
    }

    @Override
    public String getDialectFulltextQuery(String query) {
        throw new UnsupportedOperationException();
    }

    @Override
    public FulltextMatchInfo getFulltextScoredMatchInfo(String fulltextQuery, String indexName, int nthMatch,
            Column mainColumn, Model model, Database database) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean supportsUpdateFrom() {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean doesUpdateFromRepeatSelf() {
        throw new UnsupportedOperationException();
    }

    @Override
    public String getSecurityCheckSql(String idColumnName) {
        return String.format("NX_ACCESS_ALLOWED(%s, ?, ?) = 1", idColumnName);
    }

    @Override
    public String getInTreeSql(String idColumnName, String id) {
        return String.format("NX_IN_TREE(%s, ?) = 1", idColumnName);
    }

    @Override
    public String getUpsertSql(List<Column> columns, List<Serializable> values, List<Column> outColumns,
            List<Serializable> outValues) {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<String> checkStoredProcedure(String procName, String procCreate, String ddlMode, Connection connection,
            JDBCLogger logger, Map<String, Serializable> properties) throws SQLException {
        throw new UnsupportedOperationException();
    }

}
