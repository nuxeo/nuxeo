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

import java.io.IOException;
import java.io.Reader;
import java.io.Serializable;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.net.SocketException;
import java.sql.Array;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.common.utils.StringUtils;
import org.nuxeo.ecm.core.NXCore;
import org.nuxeo.ecm.core.api.security.SecurityConstants;
import org.nuxeo.ecm.core.storage.StorageException;
import org.nuxeo.ecm.core.storage.sql.Binary;
import org.nuxeo.ecm.core.storage.sql.BinaryManager;
import org.nuxeo.ecm.core.storage.sql.ColumnType;
import org.nuxeo.ecm.core.storage.sql.Model;
import org.nuxeo.ecm.core.storage.sql.ModelFulltext;
import org.nuxeo.ecm.core.storage.sql.RepositoryDescriptor;
import org.nuxeo.ecm.core.storage.sql.jdbc.db.Column;
import org.nuxeo.ecm.core.storage.sql.jdbc.db.Database;
import org.nuxeo.ecm.core.storage.sql.jdbc.db.Table;

/**
 * Oracle-specific dialect.
 *
 * @author Florent Guillaume
 */
public class DialectOracle extends Dialect {

    private static final Log log = LogFactory.getLog(DialectOracle.class);

    protected final String fulltextParameters;

    public DialectOracle(DatabaseMetaData metadata,
            BinaryManager binaryManager,
            RepositoryDescriptor repositoryDescriptor) throws StorageException {
        super(metadata, binaryManager, repositoryDescriptor);
        fulltextParameters = repositoryDescriptor.fulltextAnalyzer == null ? ""
                : repositoryDescriptor.fulltextAnalyzer;
    }

    @Override
    public String getConnectionSchema(Connection connection)
            throws SQLException {
        Statement st = connection.createStatement();
        String sql = "SELECT SYS_CONTEXT('USERENV', 'SESSION_USER') FROM DUAL";
        log.trace("SQL: " + sql);
        ResultSet rs = st.executeQuery(sql);
        rs.next();
        String user = rs.getString(1);
        log.trace("SQL:   -> " + user);
        st.close();
        return user;
    }

    @Override
    public String getCascadeDropConstraintsString() {
        return " CASCADE CONSTRAINTS";
    }

    @Override
    public String getAddColumnString() {
        return "ADD";
    }

    @Override
    public JDBCInfo getJDBCTypeAndString(ColumnType type) {
        switch (type) {
        case VARCHAR:
            return jdbcInfo("NVARCHAR2(2000)", Types.VARCHAR);
        case CLOB:
            return jdbcInfo("NCLOB", Types.CLOB);
        case BOOLEAN:
            return jdbcInfo("NUMBER(1,0)", Types.BIT);
        case LONG:
            return jdbcInfo("NUMBER(19,0)", Types.BIGINT);
        case DOUBLE:
            return jdbcInfo("DOUBLE PRECISION", Types.DOUBLE);
        case TIMESTAMP:
            return jdbcInfo("TIMESTAMP", Types.TIMESTAMP);
        case BLOBID:
            return jdbcInfo("VARCHAR2(40)", Types.VARCHAR);
            // -----
        case NODEID:
        case NODEIDFK:
        case NODEIDFKNP:
        case NODEIDFKMUL:
        case NODEIDFKNULL:
        case NODEVAL:
            return jdbcInfo("VARCHAR2(36)", Types.VARCHAR);
        case SYSNAME:
            return jdbcInfo("VARCHAR2(250)", Types.VARCHAR);
        case TINYINT:
            return jdbcInfo("NUMBER(3,0)", Types.TINYINT);
        case INTEGER:
            return jdbcInfo("NUMBER(10,0)", Types.INTEGER);
        case FTINDEXED:
            return jdbcInfo("CLOB", Types.CLOB);
        case FTSTORED:
            return jdbcInfo("NCLOB", Types.CLOB);
        case CLUSTERNODE:
            return jdbcInfo("NUMBER(10,0)", Types.INTEGER);
        case CLUSTERFRAGS:
            return jdbcInfo("VARCHAR2(4000)", Types.VARCHAR);
        }
        throw new AssertionError(type);
    }

    @Override
    public boolean isAllowedConversion(int expected, int actual,
            String actualName, int actualSize) {
        // Oracle internal conversions
        if (expected == Types.DOUBLE && actual == Types.FLOAT) {
            return true;
        }
        if (expected == Types.VARCHAR && actual == Types.OTHER
                && actualName.equals("NVARCHAR2")) {
            return true;
        }
        if (expected == Types.CLOB && actual == Types.OTHER
                && actualName.equals("NCLOB")) {
            return true;
        }
        if (expected == Types.BIT && actual == Types.DECIMAL
                && actualName.equals("NUMBER") && actualSize == 1) {
            return true;
        }
        if (expected == Types.TINYINT && actual == Types.DECIMAL
                && actualName.equals("NUMBER") && actualSize == 3) {
            return true;
        }
        if (expected == Types.INTEGER && actual == Types.DECIMAL
                && actualName.equals("NUMBER") && actualSize == 10) {
            return true;
        }
        if (expected == Types.BIGINT && actual == Types.DECIMAL
                && actualName.equals("NUMBER") && actualSize == 19) {
            return true;
        }
        // CLOB vs VARCHAR compatibility
        if (expected == Types.VARCHAR && actual == Types.OTHER
                && actualName.equals("NCLOB")) {
            return true;
        }
        if (expected == Types.CLOB && actual == Types.OTHER
                && actualName.equals("NVARCHAR2")) {
            return true;
        }
        return false;
    }

    @Override
    public void setToPreparedStatement(PreparedStatement ps, int index,
            Serializable value, Column column) throws SQLException {
        switch (column.getJdbcType()) {
        case Types.VARCHAR:
        case Types.CLOB:
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
        case Types.SMALLINT:
            ps.setInt(index, ((Long) value).intValue());
            return;
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
            // case Types.OTHER:
            // if (column.getType() == Type.FTSTORED) {
            // ps.setString(index, (String) value);
            // return;
            // }
            // throw new SQLException("Unhandled type: " + column.getType());
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
            String string = rs.getString(index);
            if (column.getType() == ColumnType.BLOBID && string != null) {
                return getBinaryManager().getBinary(string);
            } else {
                return string;
            }
        case Types.CLOB:
            // Oracle cannot read CLOBs using rs.getString when the ResultSet is
            // a ScrollableResultSet (the standard OracleResultSetImpl works
            // fine).
            Reader r = rs.getCharacterStream(index);
            if (r == null) {
                return null;
            }
            StringBuilder sb = new StringBuilder();
            char[] buffer = new char[4096];
            try {
                int n;
                while ((n = r.read(buffer)) != -1) {
                    sb.append(new String(buffer, 0, n));
                }
            } catch (IOException e) {
                log.error("Cannot read CLOB", e);
            }
            return sb.toString();
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
    protected int getMaxNameSize() {
        return 30;
    }

    @Override
    /* Avoid DRG-11439: index name length exceeds maximum of 25 bytes */
    protected int getMaxIndexNameSize() {
        return 25;
    }

    @Override
    public String getCreateFulltextIndexSql(String indexName,
            String quotedIndexName, Table table, List<Column> columns,
            Model model) {
        return String.format(
                "CREATE INDEX %s ON %s(%s) INDEXTYPE IS CTXSYS.CONTEXT "
                        + "PARAMETERS('%s SYNC (ON COMMIT) TRANSACTIONAL')",
                quotedIndexName, table.getQuotedName(),
                columns.get(0).getQuotedName(), fulltextParameters);
    }

    @Override
    public String getDialectFulltextQuery(String query) {
        query = query.replaceAll(" +", " ").trim();
        query = query.replace("*", "%");
        List<String> pos = new LinkedList<String>();
        List<String> neg = new LinkedList<String>();
        for (String word : StringUtils.split(query, ' ', false)) {
            if (word.startsWith("-")) {
                neg.add(word.substring(1));
            } else if (word.startsWith("+")) {
                pos.add(word.substring(1));
            } else {
                pos.add(word);
            }
        }
        if (pos.isEmpty()) {
            return "DONTMATCHANYTHINGFOREMPTYQUERY";
        }
        String res = StringUtils.join(pos, " & ");
        if (!neg.isEmpty()) {
            res += " ~ " + StringUtils.join(neg, " ~ ");
        }
        return res;
    }

    // SELECT ..., SCORE(1) / 100
    // FROM ... LEFT JOIN fulltext ON fulltext.id = hierarchy.id
    // WHERE ... AND CONTAINS(fulltext.fulltext, ?, 1) > 0
    // ORDER BY SCORE(1) DESC
    @Override
    public FulltextMatchInfo getFulltextScoredMatchInfo(String fulltextQuery,
            String indexName, int nthMatch, Column mainColumn, Model model,
            Database database) {
        String indexSuffix = model.getFulltextIndexSuffix(indexName);
        Table ft = database.getTable(model.FULLTEXT_TABLE_NAME);
        Column ftMain = ft.getColumn(model.MAIN_KEY);
        Column ftColumn = ft.getColumn(model.FULLTEXT_FULLTEXT_KEY
                + indexSuffix);
        String score = String.format("SCORE(%d)", nthMatch);
        FulltextMatchInfo info = new FulltextMatchInfo();
        info.join = String.format(
                "%s ON %s = %s", //
                ft.getQuotedName(), ftMain.getFullQuotedName(),
                mainColumn.getFullQuotedName());
        info.whereExpr = String.format("CONTAINS(%s, ?, %d) > 0",
                ftColumn.getFullQuotedName(), nthMatch);
        info.whereExprParam = fulltextQuery;
        info.scoreExpr = String.format("%s / 100", score);
        info.scoreAlias = score;
        info.scoreCol = new Column(mainColumn.getTable(), null,
                ColumnType.DOUBLE, null);
        return info;
    }

    @Override
    public boolean getMaterializeFulltextSyntheticColumn() {
        return true;
    }

    @Override
    public int getFulltextIndexedColumns() {
        return 1;
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
    public String getClobCast(boolean inOrderBy) {
        return "CAST(%s AS NVARCHAR2(%d))";
    }

    @Override
    public boolean supportsReadAcl() {
        return aclOptimizationsEnabled;
    }

    @Override
    public String getReadAclsCheckSql(String idColumnName) {
        return String.format(
                "%s IN (SELECT COLUMN_VALUE FROM TABLE(nx_get_read_acls_for(?)))",
                idColumnName);
    }

    @Override
    public String getUpdateReadAclsSql() {
        return "{CALL nx_update_read_acls}";
    }

    @Override
    public String getRebuildReadAclsSql() {
        return "{CALL nx_rebuild_read_acls}";
    }

    @Override
    public String getSecurityCheckSql(String idColumnName) {
        return String.format("NX_ACCESS_ALLOWED(%s, ?, ?) = 1", idColumnName);
    }

    @Override
    public String getInTreeSql(String idColumnName) {
        return String.format("NX_IN_TREE(%s, ?) = 1", idColumnName);
    }

    @Override
    public boolean isClusteringSupported() {
        return true;
    }

    /*
     * For Oracle we don't use a function to return values and delete them at
     * the same time, because pipelined functions that need to do DML have to do
     * it in an autonomous transaction which could cause consistency issues.
     */
    @Override
    public boolean isClusteringDeleteNeeded() {
        return true;
    }

    @Override
    public String getClusterInsertInvalidations() {
        return "{CALL NX_CLUSTER_INVAL(?, ?, ?)}";
    }

    @Override
    public String getClusterGetInvalidations() {
        return "SELECT id, fragments, kind FROM cluster_invals "
                + "WHERE nodeid = SYS_CONTEXT('USERENV','SID')";
    }

    @Override
    public String getClusterDeleteInvalidations() {
        return "DELETE FROM cluster_invals WHERE nodeid = SYS_CONTEXT('USERENV','SID')";
    }

    @Override
    public boolean supportsWith() {
        return !aclOptimizationsEnabled;
    }

    @Override
    public boolean supportsArrays() {
        return true;
    }

    private static boolean initialized;

    private static Constructor<?> arrayDescriptorConstructor;

    private static Constructor<?> arrayConstructor;

    private static void init() throws SQLException {
        if (!initialized) {
            try {
                Class<?> arrayDescriptorClass = Class.forName("oracle.sql.ArrayDescriptor");
                arrayDescriptorConstructor = arrayDescriptorClass.getConstructor(
                        String.class, Connection.class);
                Class<?> arrayClass = Class.forName("oracle.sql.ARRAY");
                arrayConstructor = arrayClass.getConstructor(
                        arrayDescriptorClass, Connection.class, Object.class);
            } catch (Exception e) {
                throw new SQLException(e.toString());
            }
            initialized = true;
        }
    }

    // use reflection to avoid linking dependencies
    @Override
    public Array createArrayOf(int type, Object[] elements,
            Connection connection) throws SQLException {
        if (elements == null || elements.length == 0) {
            return null;
        }
        init();
        try {
            Object arrayDescriptor = arrayDescriptorConstructor.newInstance(
                    "NX_STRING_TABLE", connection);
            return (Array) arrayConstructor.newInstance(arrayDescriptor,
                    connection, elements);
        } catch (Exception e) {
            throw new SQLException(e.toString());
        }
    }

    @Override
    public String getSQLStatementsFilename() {
        return "nuxeovcs/oracle.sql.txt";
    }

    @Override
    public String getTestSQLStatementsFilename() {
        return "nuxeovcs/oracle.test.sql.txt";
    }

    @Override
    public Map<String, Serializable> getSQLStatementsProperties(Model model,
            Database database) {
        Map<String, Serializable> properties = new HashMap<String, Serializable>();
        properties.put("idType", "VARCHAR2(36)");
        properties.put("argIdType", "VARCHAR2"); // in function args
        properties.put("aclOptimizationsEnabled",
                Boolean.valueOf(aclOptimizationsEnabled));
        properties.put("fulltextEnabled", Boolean.valueOf(!fulltextDisabled));
        if (!fulltextDisabled) {
            Table ft = database.getTable(model.FULLTEXT_TABLE_NAME);
            properties.put("fulltextTable", ft.getQuotedName());
            ModelFulltext fti = model.getFulltextInfo();
            List<String> lines = new ArrayList<String>(fti.indexNames.size());
            for (String indexName : fti.indexNames) {
                String suffix = model.getFulltextIndexSuffix(indexName);
                Column ftft = ft.getColumn(model.FULLTEXT_FULLTEXT_KEY + suffix);
                Column ftst = ft.getColumn(model.FULLTEXT_SIMPLETEXT_KEY
                        + suffix);
                Column ftbt = ft.getColumn(model.FULLTEXT_BINARYTEXT_KEY
                        + suffix);
                String line = String.format(
                        "  :NEW.%s := :NEW.%s || :NEW.%s; ",
                        ftft.getQuotedName(), ftst.getQuotedName(),
                        ftbt.getQuotedName());
                lines.add(line);
            }
            properties.put("fulltextTriggerStatements", StringUtils.join(lines,
                    "\n"));
        }
        String[] permissions = NXCore.getSecurityService().getPermissionsToCheck(
                SecurityConstants.BROWSE);
        List<String> permsList = new LinkedList<String>();
        for (String perm : permissions) {
            permsList.add(String.format(
                    "  INTO READ_ACL_PERMISSIONS VALUES ('%s')", perm));
        }
        properties.put("readPermissions", StringUtils.join(permsList, "\n"));
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
        // oracle.jdbc.xa.OracleXAException
        Integer err = Integer.valueOf(0);
        try {
            Method m = t.getClass().getMethod("getOracleError");
            err = (Integer) m.invoke(t);
        } catch (Exception e) {
            // ignore
        }
        switch (err.intValue()) {
        case 17002: // ORA-17002 IO Exception
            return true;
        }
        return false;
    }

}
