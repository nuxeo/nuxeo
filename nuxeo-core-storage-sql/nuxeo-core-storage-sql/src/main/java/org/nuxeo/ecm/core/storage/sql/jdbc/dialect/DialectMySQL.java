/*
 * (C) Copyright 2008-2009 Nuxeo SA (http://nuxeo.com/) and contributors.
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

package org.nuxeo.ecm.core.storage.sql.jdbc.dialect;

import java.io.Serializable;
import java.net.SocketException;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.nuxeo.common.utils.StringUtils;
import org.nuxeo.ecm.core.storage.StorageException;
import org.nuxeo.ecm.core.storage.sql.Binary;
import org.nuxeo.ecm.core.storage.sql.BinaryManager;
import org.nuxeo.ecm.core.storage.sql.ColumnType;
import org.nuxeo.ecm.core.storage.sql.Model;
import org.nuxeo.ecm.core.storage.sql.RepositoryDescriptor;
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

    public DialectMySQL(DatabaseMetaData metadata, BinaryManager binaryManager,
            RepositoryDescriptor repositoryDescriptor) throws StorageException {
        super(metadata, binaryManager, repositoryDescriptor);
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
    public String getAddForeignKeyConstraintString(String constraintName,
            String[] foreignKeys, String referencedTable, String[] primaryKeys,
            boolean referencesPrimaryKey) {
        String cols = StringUtils.join(foreignKeys, ", ");
        String sql = String.format(
                " ADD INDEX %s (%s), ADD CONSTRAINT %s FOREIGN KEY (%s) REFERENCES %s (%s)",
                constraintName, cols, constraintName, cols, referencedTable,
                StringUtils.join(primaryKeys, ", "));
        return sql;
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
        switch (type) {
        case VARCHAR:
            // don't use the max 65535 because this max is actually for the
            // total size of all columns of a given table, so allow several
            // varchar columns in the same table
            return jdbcInfo("VARCHAR(500)", Types.VARCHAR);
        case CLOB:
            return jdbcInfo("LONGTEXT", Types.LONGVARCHAR);
        case BOOLEAN:
            return jdbcInfo("BIT", Types.BIT);
        case LONG:
            return jdbcInfo("BIGINT", Types.BIGINT);
        case DOUBLE:
            return jdbcInfo("DOUBLE", Types.DOUBLE);
        case TIMESTAMP:
            return jdbcInfo("DATETIME", Types.TIMESTAMP);
        case BLOBID:
            return jdbcInfo("VARCHAR(40) BINARY", Types.VARCHAR);
            // -----
        case NODEID:
        case NODEIDFK:
        case NODEIDFKNP:
        case NODEIDFKMUL:
        case NODEIDFKNULL:
        case NODEVAL:
            return jdbcInfo("VARCHAR(36) BINARY", Types.VARCHAR);
        case SYSNAME:
            return jdbcInfo("VARCHAR(256) BINARY", Types.VARCHAR);
        case TINYINT:
            return jdbcInfo("TINYINT", Types.TINYINT);
        case INTEGER:
            return jdbcInfo("INTEGER", Types.INTEGER);
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
    public boolean isAllowedConversion(int expected, int actual,
            String actualName, int actualSize) {
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
    public void setToPreparedStatement(PreparedStatement ps, int index,
            Serializable value, Column column) throws SQLException {
        switch (column.getJdbcType()) {
        case Types.VARCHAR:
        case Types.LONGVARCHAR:
            String v;
            if (column.getType() == ColumnType.BLOBID) {
                v = ((Binary) value).getDigest();
            } else {
                v = (String) value;
            }
            ps.setString(index, v);
            break;
        case Types.BIT:
            ps.setBoolean(index, ((Boolean) value).booleanValue());
            return;
        case Types.TINYINT:
        case Types.INTEGER:
        case Types.BIGINT:
            ps.setLong(index, ((Long) value).longValue());
            return;
        case Types.DOUBLE:
            ps.setDouble(index, ((Double) value).doubleValue());
            return;
        case Types.TIMESTAMP:
            Calendar cal = (Calendar) value;
            Timestamp ts = new Timestamp(cal.getTimeInMillis());
            ps.setTimestamp(index, ts, cal); // cal passed for timezone
            return;
        default:
            throw new SQLException("Unhandled JDBC type: "
                    + column.getJdbcType());
        }
    }

    @Override
    @SuppressWarnings("boxing")
    public Serializable getFromResultSet(ResultSet rs, int index, Column column)
            throws SQLException {
        switch (column.getJdbcType()) {
        case Types.VARCHAR:
        case Types.LONGVARCHAR:
            String string = rs.getString(index);
            if (column.getType() == ColumnType.BLOBID && string != null) {
                return getBinaryManager().getBinary(string);
            } else {
                return string;
            }
        case Types.BIT:
            return rs.getBoolean(index);
        case Types.TINYINT:
        case Types.INTEGER:
        case Types.BIGINT:
            return rs.getLong(index);
        case Types.DOUBLE:
            return rs.getDouble(index);
        case Types.TIMESTAMP:
            Timestamp ts = rs.getTimestamp(index);
            if (ts == null) {
                return null;
            } else {
                Serializable cal = new GregorianCalendar(); // XXX timezone
                ((Calendar) cal).setTimeInMillis(ts.getTime());
                return cal;
            }
        }
        throw new SQLException("Unhandled JDBC type: " + column.getJdbcType());
    }

    @Override
    public String getCreateFulltextIndexSql(String indexName,
            String quotedIndexName, Table table, List<Column> columns,
            Model model) {
        List<String> columnNames = new ArrayList<String>(columns.size());
        for (Column col : columns) {
            columnNames.add(col.getQuotedName());
        }
        return String.format("CREATE FULLTEXT INDEX %s ON %s (%s)",
                quotedIndexName, table.getQuotedName(), StringUtils.join(
                        columnNames, ", "));
    }

    @Override
    public String getDialectFulltextQuery(String query) {
        query = query.replaceAll(" +", " ").trim();
        List<String> pos = new LinkedList<String>();
        List<String> neg = new LinkedList<String>();
        for (String word : StringUtils.split(query, ' ', false)) {
            if (word.startsWith("-")) {
                neg.add(word);
            } else if (word.startsWith("+")) {
                pos.add(word);
            } else {
                pos.add("+" + word);
            }
        }
        if (pos.isEmpty()) {
            return "+DONTMATCHANYTHINGFOREMPTYQUERY";
        }
        String res = StringUtils.join(pos, " ");
        if (!neg.isEmpty()) {
            res += " " + StringUtils.join(neg, " ");
        }
        return res;
    }

    // SELECT ..., (MATCH(`fulltext`.`simpletext`, `fulltext`.`binarytext`)
    // .................. AGAINST (?) / 10) AS nxscore
    // FROM ... LEFT JOIN `fulltext` ON ``fulltext`.`id` = `hierarchy`.`id`
    // WHERE ... AND MATCH(`fulltext`.`simpletext`, `fulltext`.`binarytext`)
    // ................... AGAINST (? IN BOOLEAN MODE)
    // ORDER BY nxscore DESC
    @Override
    public FulltextMatchInfo getFulltextScoredMatchInfo(String fulltextQuery,
            String indexName, int nthMatch, Column mainColumn, Model model,
            Database database) {
        String nthSuffix = nthMatch == 1 ? "" : String.valueOf(nthMatch);
        String scoreAlias = "_nxscore" + nthSuffix;
        String indexSuffix = model.getFulltextIndexSuffix(indexName);
        Table ft = database.getTable(model.FULLTEXT_TABLE_NAME);
        Column ftMain = ft.getColumn(model.MAIN_KEY);
        Column stColumn = ft.getColumn(model.FULLTEXT_SIMPLETEXT_KEY
                + indexSuffix);
        Column btColumn = ft.getColumn(model.FULLTEXT_BINARYTEXT_KEY
                + indexSuffix);
        String match = String.format("MATCH (%s, %s)",
                stColumn.getFullQuotedName(), btColumn.getFullQuotedName());
        FulltextMatchInfo info = new FulltextMatchInfo();
        info.joins = Collections.singletonList(new Join(Join.INNER,
                ft.getQuotedName(), null, null, ftMain.getFullQuotedName(),
                mainColumn.getFullQuotedName()));
        info.whereExpr = String.format("%s AGAINST (? IN BOOLEAN MODE)", match);
        info.whereExprParam = fulltextQuery;
        // Note: using the boolean query in non-boolean mode gives approximate
        // results but it's the best we have as MySQL does not provide a score
        // in boolean mode.
        // Note: dividing by 10 is arbitrary, but MySQL cannot really
        // normalize scores.
        info.scoreExpr = String.format("(%s AGAINST (?) / 10) AS %s", match,
                scoreAlias);
        info.scoreExprParam = fulltextQuery;
        info.scoreAlias = scoreAlias;
        info.scoreCol = new Column(mainColumn.getTable(), null,
                ColumnType.DOUBLE, null);
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
    public String getInTreeSql(String idColumnName) {
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
    public Map<String, Serializable> getSQLStatementsProperties(Model model,
            Database database) {
        Map<String, Serializable> properties = new HashMap<String, Serializable>();
        properties.put("idType", "varchar(36)");
        properties.put("fulltextEnabled", Boolean.valueOf(!fulltextDisabled));
        return properties;
    }

    @Override
    public boolean connectionClosedByException(Throwable t) {
        while (t.getCause() != null) {
            t = t.getCause();
        }
        if (t instanceof SocketException) {
            return true;
        }
        // XAResource.start:
        // com.mysql.jdbc.jdbc2.optional.MysqlXAException
        // No operations allowed after connection closed. Connection was
        // implicitly closed due to underlying exception/error:
        // com.mysql.jdbc.exceptions.jdbc4.CommunicationsException:
        // Communications link failure
        String message = t.toString() + " " + t.getMessage();
        if (message.contains("Communications link failure")
                || message.contains("CommunicationsException")) {
            return true;
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
        return "CALL NX_CLUSTER_INVAL(?, ?, ?)";
    }

    @Override
    public String getClusterGetInvalidations() {
        return "SELECT id, fragments, kind FROM cluster_invals WHERE nodeid = @@PSEUDO_THREAD_ID";
    }

    @Override
    public String getClusterDeleteInvalidations() {
        return "DELETE FROM cluster_invals WHERE nodeid = @@PSEUDO_THREAD_ID";
    }

    @Override
    public boolean supportsPaging() {
        return true;
    }

    @Override
    public String getPagingClause(long limit, long offset) {
        return String.format("LIMIT %d OFFSET %d", limit, offset);
    }

}
