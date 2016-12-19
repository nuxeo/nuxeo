/*
 * (C) Copyright 2006-2016 Nuxeo SA (http://nuxeo.com/) and others.
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
import java.sql.Statement;
import java.sql.Types;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.nuxeo.ecm.core.storage.FulltextQueryAnalyzer;
import org.nuxeo.ecm.core.storage.FulltextQueryAnalyzer.FulltextQuery;
import org.nuxeo.ecm.core.storage.FulltextQueryAnalyzer.Op;
import org.nuxeo.ecm.core.storage.sql.ColumnType;
import org.nuxeo.ecm.core.storage.sql.Model;
import org.nuxeo.ecm.core.storage.sql.RepositoryDescriptor;
import org.nuxeo.ecm.core.storage.sql.jdbc.JDBCLogger;
import org.nuxeo.ecm.core.storage.sql.jdbc.db.Column;
import org.nuxeo.ecm.core.storage.sql.jdbc.db.Database;
import org.nuxeo.ecm.core.storage.sql.jdbc.db.Join;
import org.nuxeo.ecm.core.storage.sql.jdbc.db.Table;

/**
 * MySQL-specific dialect.
 *
 * @author Florent Guillaume
 */
public class DialectMySQL extends Dialect {

    public DialectMySQL(DatabaseMetaData metadata, RepositoryDescriptor repositoryDescriptor) {
        super(metadata, repositoryDescriptor);
    }

    @Override
    public char openQuote() {
        return '`';
    }

    @Override
    public char closeQuote() {
        return '`';
    }

    @Override
    public String getAddForeignKeyConstraintString(String constraintName, String[] foreignKeys, String referencedTable,
            String[] primaryKeys, boolean referencesPrimaryKey) {
        String cols = String.join(", ", foreignKeys);
        return String.format(" ADD INDEX %s (%s), ADD CONSTRAINT %s FOREIGN KEY (%s) REFERENCES %s (%s)",
                constraintName, cols, constraintName, cols, referencedTable, String.join(", ", primaryKeys));
    }

    @Override
    public boolean qualifyIndexName() {
        return false;
    }

    @Override
    public boolean supportsIfExistsBeforeTableName() {
        return true;
    }

    @Override
    public JDBCInfo getJDBCTypeAndString(ColumnType type) {
        switch (type.spec) {
        case STRING:
            if (type.isUnconstrained()) {
                // don't use the max 65535 because this max is actually for the
                // total size of all columns of a given table, so allow several
                // varchar columns in the same table
                // 255 is max for a column to be primary key in UTF8
                return jdbcInfo("VARCHAR(255)", Types.VARCHAR);
            } else if (type.isClob() || type.length > 65535) {
                return jdbcInfo("LONGTEXT", Types.LONGVARCHAR);
            } else {
                return jdbcInfo("VARCHAR(%d)", type.length, Types.VARCHAR);
            }
        case BOOLEAN:
            return jdbcInfo("BIT", Types.BIT);
        case LONG:
            return jdbcInfo("BIGINT", Types.BIGINT);
        case DOUBLE:
            return jdbcInfo("DOUBLE", Types.DOUBLE);
        case TIMESTAMP:
            return jdbcInfo("DATETIME(3)", Types.TIMESTAMP);
        case BLOBID:
            return jdbcInfo("VARCHAR(250) BINARY", Types.VARCHAR);
        // -----
        case NODEID:
        case NODEIDFK:
        case NODEIDFKNP:
        case NODEIDFKMUL:
        case NODEIDFKNULL:
        case NODEIDPK:
        case NODEVAL:
            return jdbcInfo("VARCHAR(36) BINARY", Types.VARCHAR);
        case SYSNAME:
        case SYSNAMEARRAY:
            // 255 is max for a column to have an index in UTF8
            return jdbcInfo("VARCHAR(255) BINARY", Types.VARCHAR);
        case TINYINT:
            return jdbcInfo("TINYINT", Types.TINYINT);
        case INTEGER:
            return jdbcInfo("INTEGER", Types.INTEGER);
        case AUTOINC:
            return jdbcInfo("INTEGER AUTO_INCREMENT PRIMARY KEY", Types.INTEGER);
        case FTINDEXED:
            throw new AssertionError(type);
        case FTSTORED:
            return jdbcInfo("LONGTEXT", Types.LONGVARCHAR);
        case CLUSTERNODE:
            return jdbcInfo("BIGINT", Types.BIGINT);
        case CLUSTERFRAGS:
            return jdbcInfo("TEXT", Types.VARCHAR);
        }
        throw new AssertionError(type);
    }

    @Override
    public boolean isAllowedConversion(int expected, int actual, String actualName, int actualSize) {
        // LONGVARCHAR vs VARCHAR compatibility
        if (expected == Types.VARCHAR && actual == Types.LONGVARCHAR) {
            return true;
        }
        if (expected == Types.LONGVARCHAR && actual == Types.VARCHAR) {
            return true;
        }
        // INTEGER vs BIGINT compatibility
        if (expected == Types.BIGINT && actual == Types.INTEGER) {
            return true;
        }
        if (expected == Types.INTEGER && actual == Types.BIGINT) {
            return true;
        }
        return false;
    }

    @Override
    public void setToPreparedStatement(PreparedStatement ps, int index, Serializable value, Column column)
            throws SQLException {
        switch (column.getJdbcType()) {
        case Types.VARCHAR:
        case Types.LONGVARCHAR:
            setToPreparedStatementString(ps, index, value, column);
            return;
        case Types.BIT:
            ps.setBoolean(index, ((Boolean) value).booleanValue());
            return;
        case Types.TINYINT:
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
        case Types.LONGVARCHAR:
            return getFromResultSetString(rs, index, column);
        case Types.BIT:
            return rs.getBoolean(index);
        case Types.TINYINT:
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
        return 64;
    }

    @Override
    public String getCreateFulltextIndexSql(String indexName, String quotedIndexName, Table table, List<Column> columns,
            Model model) {
        String indexedColumns = columns.stream().map(Column::getQuotedName).collect(Collectors.joining(", "));
        return String.format("CREATE FULLTEXT INDEX %s ON %s (%s)", quotedIndexName, table.getQuotedName(),
                indexedColumns);
    }

    @Override
    public String getDialectFulltextQuery(String query) {
        query = query.replace("%", "*");
        FulltextQuery ft = FulltextQueryAnalyzer.analyzeFulltextQuery(query);
        if (ft == null || ft.op == Op.NOTWORD) {
            return "DONTMATCHANYTHINGFOREMPTYQUERY";
        }
        StringBuilder buf = new StringBuilder();
        translateForMySQL(ft, null, buf);
        return buf.toString();
    }

    protected static void translateForMySQL(FulltextQuery ft, Op superOp, StringBuilder buf) {
        if (ft.op == Op.AND || ft.op == Op.OR) {
            if (superOp == Op.AND) {
                buf.append('+');
            }
            buf.append('(');
            for (int i = 0; i < ft.terms.size(); i++) {
                FulltextQuery term = ft.terms.get(i);
                if (i != 0) {
                    buf.append(' ');
                }
                translateForMySQL(term, ft.op, buf);
            }
            buf.append(')');
        } else {
            if (ft.op == Op.NOTWORD) {
                buf.append('-');
            } else { // Op.WORD
                if (superOp == Op.AND) {
                    buf.append('+');
                }
            }
            boolean isPhrase = ft.word.contains(" ");
            if (isPhrase) {
                buf.append('"');
            }
            buf.append(ft.word);
            if (isPhrase) {
                buf.append('"');
            }
        }
    }

    // SELECT ..., (MATCH(`fulltext`.`simpletext`, `fulltext`.`binarytext`)
    // .................. AGAINST (?) / 10) AS nxscore
    // FROM ... LEFT JOIN `fulltext` ON ``fulltext`.`id` = `hierarchy`.`id`
    // WHERE ... AND MATCH(`fulltext`.`simpletext`, `fulltext`.`binarytext`)
    // ................... AGAINST (? IN BOOLEAN MODE)
    // ORDER BY nxscore DESC
    @Override
    public FulltextMatchInfo getFulltextScoredMatchInfo(String fulltextQuery, String indexName, int nthMatch,
            Column mainColumn, Model model, Database database) {
        String nthSuffix = nthMatch == 1 ? "" : String.valueOf(nthMatch);
        String indexSuffix = model.getFulltextIndexSuffix(indexName);
        Table ft = database.getTable(Model.FULLTEXT_TABLE_NAME);
        Column ftMain = ft.getColumn(Model.MAIN_KEY);
        Column stColumn = ft.getColumn(Model.FULLTEXT_SIMPLETEXT_KEY + indexSuffix);
        Column btColumn = ft.getColumn(Model.FULLTEXT_BINARYTEXT_KEY + indexSuffix);
        String match = String.format("MATCH (%s, %s)", stColumn.getFullQuotedName(), btColumn.getFullQuotedName());
        FulltextMatchInfo info = new FulltextMatchInfo();
        if (nthMatch == 1) {
            // Need only one JOIN involving the fulltext table
            info.joins = Collections.singletonList(new Join(Join.INNER, ft.getQuotedName(), null, null,
                    ftMain.getFullQuotedName(), mainColumn.getFullQuotedName()));
        }
        info.whereExpr = String.format("%s AGAINST (? IN BOOLEAN MODE)", match);
        info.whereExprParam = fulltextQuery;
        // Note: using the boolean query in non-boolean mode gives approximate
        // results but it's the best we have as MySQL does not provide a score
        // in boolean mode.
        // Note: dividing by 10 is arbitrary, but MySQL cannot really
        // normalize scores.
        info.scoreExpr = String.format("(%s AGAINST (?) / 10)", match);
        info.scoreExprParam = fulltextQuery;
        info.scoreAlias = "_nxscore" + nthSuffix;
        info.scoreCol = new Column(mainColumn.getTable(), null, ColumnType.DOUBLE, null);
        return info;
    }

    @Override
    public boolean getMaterializeFulltextSyntheticColumn() {
        return false;
    }

    @Override
    public int getFulltextIndexedColumns() {
        return 2;
    }

    @Override
    public String getTableTypeString(Table table) {
        if (table.hasFulltextIndex()) {
            return " ENGINE=MyISAM";
        } else {
            return " ENGINE=InnoDB";
        }
    }

    @Override
    public boolean supportsUpdateFrom() {
        return true;
    }

    @Override
    public boolean doesUpdateFromRepeatSelf() {
        return true;
    }

    @Override
    public boolean needsOrderByKeysAfterDistinct() {
        return false;
    }

    @Override
    public boolean needsAliasForDerivedTable() {
        return true;
    }

    @Override
    public String getSecurityCheckSql(String idColumnName) {
        return String.format("NX_ACCESS_ALLOWED(%s, ?, ?)", idColumnName);
    }

    @Override
    public String getInTreeSql(String idColumnName, String id) {
        return String.format("NX_IN_TREE(%s, ?)", idColumnName);
    }

    @Override
    public String getSQLStatementsFilename() {
        return "nuxeovcs/mysql.sql.txt";
    }

    @Override
    public String getTestSQLStatementsFilename() {
        return "nuxeovcs/mysql.test.sql.txt";
    }

    @Override
    public Map<String, Serializable> getSQLStatementsProperties(Model model, Database database) {
        Map<String, Serializable> properties = new HashMap<>();
        properties.put("idType", "varchar(36)");
        properties.put("fulltextEnabled", Boolean.valueOf(!fulltextDisabled));
        properties.put("fulltextSearchEnabled", Boolean.valueOf(!fulltextSearchDisabled));
        properties.put("clusteringEnabled", Boolean.valueOf(clusteringEnabled));
        return properties;
    }

    @Override
    public boolean isConcurrentUpdateException(Throwable t) {
        while (t.getCause() != null) {
            t = t.getCause();
        }
        if (t instanceof SQLException) {
            String sqlState = ((SQLException) t).getSQLState();
            if ("23000".equals(sqlState)) {
                // Integrity constraint violation: 1452 Cannot add or update a
                // child row: a foreign key constraint fails
                return true;
            }
            if ("40001".equals(sqlState)) {
                // com.mysql.jdbc.exceptions.jdbc4.MySQLTransactionRollbackException:
                // Deadlock found when trying to get lock; try restarting
                // transaction
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean isClusteringSupported() {
        return true;
    }

    @Override
    public boolean isClusteringDeleteNeeded() {
        return true;
    }

    @Override
    public String getClusterInsertInvalidations() {
        return "CALL NX_CLUSTER_INVAL(?, ?, ?, ?)";
    }

    @Override
    public String getClusterGetInvalidations() {
        return "SELECT id, fragments, kind FROM cluster_invals WHERE nodeid = ?";
    }

    @Override
    public boolean supportsPaging() {
        return true;
    }

    @Override
    public String addPagingClause(String sql, long limit, long offset) {
        return sql + String.format(" LIMIT %d OFFSET %d", limit, offset);
    }

    @Override
    public boolean isIdentityAlreadyPrimary() {
        return true;
    }

    @Override
    public String getBinaryFulltextSql(List<String> columns) {
        return "SELECT " + String.join(", ", columns) + " FROM `fulltext` WHERE id=?";
    }

    @Override
    public List<String> checkStoredProcedure(String procName, String procCreate, String ddlMode, Connection connection,
            JDBCLogger logger, Map<String, Serializable> properties) throws SQLException {
        boolean compatCheck = ddlMode.contains(RepositoryDescriptor.DDL_MODE_COMPAT);
        String ifExists = compatCheck ? "IF EXISTS " : "";
        String procDrop;
        if (procCreate.toLowerCase().startsWith("create function ")) {
            procDrop = "DROP FUNCTION " + ifExists + procName;
        } else {
            procDrop = "DROP PROCEDURE " + ifExists + procName;
        }
        if (compatCheck) {
            return Arrays.asList(procDrop, procCreate);
        }
        try (Statement st = connection.createStatement()) {
            String getBody = "SELECT body FROM mysql.proc WHERE db = DATABASE() AND name = '" + procName + "'";
            logger.log(getBody);
            try (ResultSet rs = st.executeQuery(getBody)) {
                if (rs.next()) {
                    String body = rs.getString(1);
                    if (normalizeString(procCreate).contains(normalizeString(body))) {
                        logger.log("  -> exists, unchanged");
                        return Collections.emptyList();
                    } else {
                        logger.log("  -> exists, old");
                        return Arrays.asList(procDrop, procCreate);
                    }
                } else {
                    logger.log("  -> missing");
                    return Collections.singletonList(procCreate);
                }
            }
        }
    }

    protected static String normalizeString(String string) {
        // MySQL strips comments when recording a procedure's body
        return string.replaceAll("-- .*", " ").replaceAll("[ \n\r\t]+", " ").trim();
    }

    @Override
    public Collection<? extends String> getDumpStart() {
        return Collections.singleton("DELIMITER $$");
    }

    @Override
    public Collection<? extends String> getDumpStop() {
        return Collections.singleton("DELIMITER ;");
    }

    @Override
    public String getSQLForDump(String sql) {
        return sql + " $$";
    }

}
