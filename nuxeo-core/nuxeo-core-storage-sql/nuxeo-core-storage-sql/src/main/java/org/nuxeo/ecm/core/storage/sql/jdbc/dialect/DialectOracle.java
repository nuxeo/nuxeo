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
 *     Benoit Delbosc
 */
package org.nuxeo.ecm.core.storage.sql.jdbc.dialect;

import java.io.IOException;
import java.io.Reader;
import java.io.Serializable;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.sql.Array;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import javax.transaction.xa.XAException;

import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.common.utils.StringUtils;
import org.nuxeo.ecm.core.NXCore;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.core.api.security.SecurityConstants;
import org.nuxeo.ecm.core.storage.FulltextConfiguration;
import org.nuxeo.ecm.core.storage.FulltextQueryAnalyzer;
import org.nuxeo.ecm.core.storage.FulltextQueryAnalyzer.FulltextQuery;
import org.nuxeo.ecm.core.storage.sql.ColumnType;
import org.nuxeo.ecm.core.storage.sql.Model;
import org.nuxeo.ecm.core.storage.sql.RepositoryDescriptor;
import org.nuxeo.ecm.core.storage.sql.jdbc.JDBCLogger;
import org.nuxeo.ecm.core.storage.sql.jdbc.db.Column;
import org.nuxeo.ecm.core.storage.sql.jdbc.db.Database;
import org.nuxeo.ecm.core.storage.sql.jdbc.db.Join;
import org.nuxeo.ecm.core.storage.sql.jdbc.db.Table;
import org.nuxeo.runtime.datasource.ConnectionHelper;

/**
 * Oracle-specific dialect.
 *
 * @author Florent Guillaume
 */
public class DialectOracle extends Dialect {

    private static final Log log = LogFactory.getLog(DialectOracle.class);

    private Constructor<?> arrayDescriptorConstructor;

    private Constructor<?> arrayConstructor;

    private Method arrayGetLongArrayMethod;

    protected final String fulltextParameters;

    protected boolean pathOptimizationsEnabled;

    protected int pathOptimizationsVersion = 0;

    private static final String DEFAULT_USERS_SEPARATOR = "|";

    protected String usersSeparator;

    protected final DialectIdType idType;

    protected String idSequenceName;

    protected XAErrorLogger xaErrorLogger;

    protected static class XAErrorLogger {

        protected final Class<?> oracleXAExceptionClass;

        protected final Method m_xaError;

        protected final Method m_xaErrorMessage;

        protected final Method m_oracleError;

        protected final Method m_oracleSQLError;

        public XAErrorLogger() throws ReflectiveOperationException {
            oracleXAExceptionClass = Thread.currentThread()
                                           .getContextClassLoader()
                                           .loadClass("oracle.jdbc.xa.OracleXAException");
            m_xaError = oracleXAExceptionClass.getMethod("getXAError");
            m_xaErrorMessage = oracleXAExceptionClass.getMethod("getXAErrorMessage", m_xaError.getReturnType());
            m_oracleError = oracleXAExceptionClass.getMethod("getOracleError");
            m_oracleSQLError = oracleXAExceptionClass.getMethod("getOracleSQLError");
        }

        public void log(XAException e) throws ReflectiveOperationException {
            int xaError = ((Integer) m_xaError.invoke(e)).intValue();
            String xaErrorMessage = (String) m_xaErrorMessage.invoke(xaError);
            int oracleError = ((Integer) m_oracleError.invoke(e)).intValue();
            int oracleSQLError = ((Integer) m_oracleSQLError.invoke(e)).intValue();
            StringBuilder builder = new StringBuilder();
            builder.append("Oracle XA Error : ").append(xaError).append(" (").append(xaErrorMessage).append("),");
            builder.append("Oracle Error : ").append(oracleError).append(",");
            builder.append("Oracle SQL Error : ").append(oracleSQLError);
            log.warn(builder.toString(), e);
        }

    }

    public DialectOracle(DatabaseMetaData metadata, RepositoryDescriptor repositoryDescriptor) {
        super(metadata, repositoryDescriptor);
        fulltextParameters = repositoryDescriptor == null ? null
                : repositoryDescriptor.getFulltextAnalyzer() == null ? "" : repositoryDescriptor.getFulltextAnalyzer();
        pathOptimizationsEnabled = repositoryDescriptor != null && repositoryDescriptor.getPathOptimizationsEnabled();
        if (pathOptimizationsEnabled) {
            pathOptimizationsVersion = repositoryDescriptor.getPathOptimizationsVersion();
        }
        usersSeparator = repositoryDescriptor == null ? null
                : repositoryDescriptor.usersSeparatorKey == null ? DEFAULT_USERS_SEPARATOR
                        : repositoryDescriptor.usersSeparatorKey;
        String idt = repositoryDescriptor == null ? null : repositoryDescriptor.idType;
        if (idt == null || "".equals(idt) || "varchar".equalsIgnoreCase(idt)) {
            idType = DialectIdType.VARCHAR;
        } else if (idt.toLowerCase().startsWith("sequence")) {
            idType = DialectIdType.SEQUENCE;
            if (idt.toLowerCase().startsWith("sequence:")) {
                String[] split = idt.split(":");
                idSequenceName = split[1].toUpperCase(Locale.ENGLISH);
            } else {
                idSequenceName = "HIERARCHY_SEQ";
            }
        } else {
            throw new NuxeoException("Unknown id type: '" + idt + "'");
        }
        xaErrorLogger = newXAErrorLogger();
        initArrayReflection();
    }

    protected XAErrorLogger newXAErrorLogger() {
        try {
            return new XAErrorLogger();
        } catch (ReflectiveOperationException e) {
            log.warn("Cannot initialize xa error loggger", e);
            return null;
        }
    }

    // use reflection to avoid linking dependencies
    private void initArrayReflection() {
        try {
            Class<?> arrayDescriptorClass = Class.forName("oracle.sql.ArrayDescriptor");
            arrayDescriptorConstructor = arrayDescriptorClass.getConstructor(String.class, Connection.class);
            Class<?> arrayClass = Class.forName("oracle.sql.ARRAY");
            arrayConstructor = arrayClass.getConstructor(arrayDescriptorClass, Connection.class, Object.class);
            arrayGetLongArrayMethod = arrayClass.getDeclaredMethod("getLongArray");
        } catch (ClassNotFoundException e) {
            // query syntax unit test run without Oracle JDBC driver
            return;
        } catch (ReflectiveOperationException e) {
            throw new NuxeoException(e);
        }
    }

    @Override
    public String getNoColumnsInsertString(Column idColumn) {
        // INSERT INTO foo () VALUES () or DEFAULT VALUES is not legal for Oracle, you need at least one column
        return String.format("(%s) VALUES (DEFAULT)", idColumn.getQuotedName());
    }

    @Override
    public String getConnectionSchema(Connection connection) throws SQLException {
        Statement st = connection.createStatement();
        String sql = "SELECT SYS_CONTEXT('USERENV', 'SESSION_USER') FROM DUAL";
        log.trace("SQL: " + sql);
        ResultSet rs = st.executeQuery(sql);
        rs.next();
        String user = rs.getString(1);
        log.trace("SQL:   -> " + user);
        rs.close();
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
        switch (type.spec) {
        case STRING:
            if (type.isUnconstrained()) {
                return jdbcInfo("NVARCHAR2(2000)", Types.VARCHAR);
            } else if (type.isClob() || type.length > 2000) {
                return jdbcInfo("NCLOB", Types.CLOB);
            } else {
                return jdbcInfo("NVARCHAR2(%d)", type.length, Types.VARCHAR);
            }
        case BOOLEAN:
            return jdbcInfo("NUMBER(1,0)", Types.BIT);
        case LONG:
            return jdbcInfo("NUMBER(19,0)", Types.BIGINT);
        case DOUBLE:
            return jdbcInfo("DOUBLE PRECISION", Types.DOUBLE);
        case TIMESTAMP:
            return jdbcInfo("TIMESTAMP", Types.TIMESTAMP);
        case BLOBID:
            return jdbcInfo("VARCHAR2(250)", Types.VARCHAR);
        // -----
        case NODEID:
        case NODEIDFK:
        case NODEIDFKNP:
        case NODEIDFKMUL:
        case NODEIDFKNULL:
        case NODEIDPK:
        case NODEVAL:
            switch (idType) {
            case VARCHAR:
                return jdbcInfo("VARCHAR2(36)", Types.VARCHAR);
            case SEQUENCE:
                return jdbcInfo("NUMBER(10,0)", Types.INTEGER);
            default:
                throw new AssertionError("Unknown id type: " + idType);
            }
        case SYSNAME:
        case SYSNAMEARRAY:
            return jdbcInfo("VARCHAR2(250)", Types.VARCHAR);
        case TINYINT:
            return jdbcInfo("NUMBER(3,0)", Types.TINYINT);
        case INTEGER:
            return jdbcInfo("NUMBER(10,0)", Types.INTEGER);
        case AUTOINC:
            return jdbcInfo("NUMBER(10,0)", Types.INTEGER);
        case FTINDEXED:
            return jdbcInfo("CLOB", Types.CLOB);
        case FTSTORED:
            return jdbcInfo("NCLOB", Types.CLOB);
        case CLUSTERNODE:
            return jdbcInfo("VARCHAR(25)", Types.VARCHAR);
        case CLUSTERFRAGS:
            return jdbcInfo("VARCHAR2(4000)", Types.VARCHAR);
        }
        throw new AssertionError(type);
    }

    @Override
    public boolean isAllowedConversion(int expected, int actual, String actualName, int actualSize) {
        // Oracle internal conversions
        if (expected == Types.DOUBLE && actual == Types.FLOAT) {
            return true;
        }
        if (expected == Types.VARCHAR && actual == Types.OTHER && actualName.equals("NVARCHAR2")) {
            return true;
        }
        if (expected == Types.CLOB && actual == Types.OTHER && actualName.equals("NCLOB")) {
            return true;
        }
        if (expected == Types.BIT && actual == Types.DECIMAL && actualName.equals("NUMBER") && actualSize == 1) {
            return true;
        }
        if (expected == Types.TINYINT && actual == Types.DECIMAL && actualName.equals("NUMBER") && actualSize == 3) {
            return true;
        }
        if (expected == Types.INTEGER && actual == Types.DECIMAL && actualName.equals("NUMBER") && actualSize == 10) {
            return true;
        }
        if (expected == Types.BIGINT && actual == Types.DECIMAL && actualName.equals("NUMBER") && actualSize == 19) {
            return true;
        }
        // CLOB vs VARCHAR compatibility
        if (expected == Types.VARCHAR && actual == Types.OTHER && actualName.equals("NCLOB")) {
            return true;
        }
        if (expected == Types.CLOB && actual == Types.OTHER && actualName.equals("NVARCHAR2")) {
            return true;
        }
        return false;
    }

    @Override
    public Serializable getGeneratedId(Connection connection) throws SQLException {
        if (idType != DialectIdType.SEQUENCE) {
            return super.getGeneratedId(connection);
        }
        String sql = String.format("SELECT %s.NEXTVAL FROM DUAL", idSequenceName);
        Statement s = connection.createStatement();
        ResultSet rs = null;
        try {
            rs = s.executeQuery(sql);
            rs.next();
            return Long.valueOf(rs.getLong(1));
        } finally {
            if (rs != null) {
                rs.close();
            }
            s.close();
        }
    }

    @Override
    public void setId(PreparedStatement ps, int index, Serializable value) throws SQLException {
        switch (idType) {
        case VARCHAR:
            ps.setObject(index, value);
            break;
        case SEQUENCE:
            setIdLong(ps, index, value);
            break;
        default:
            throw new AssertionError("Unknown id type: " + idType);
        }
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
            ps.setBoolean(index, ((Boolean) value).booleanValue());
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
        case Types.OTHER:
            ColumnType type = column.getType();
            if (type.isId()) {
                setId(ps, index, value);
                return;
            } else if (type == ColumnType.FTSTORED) {
                ps.setString(index, (String) value);
                return;
            }
            throw new SQLException("Unhandled type: " + column.getType());
        default:
            throw new SQLException("Unhandled JDBC type: " + column.getJdbcType());
        }
    }

    @Override
    @SuppressWarnings("boxing")
    public Serializable getFromResultSet(ResultSet rs, int index, Column column) throws SQLException {
        switch (column.getJdbcType()) {
        case Types.VARCHAR:
            return getFromResultSetString(rs, index, column);
        case Types.CLOB:
            // Oracle cannot read CLOBs using rs.getString when the ResultSet is
            // a ScrollableResultSet (the standard OracleResultSetImpl works
            // fine).
            Reader r = rs.getCharacterStream(index);
            if (r == null) {
                return null;
            }
            StringBuilder sb = new StringBuilder();
            try {
                int n;
                char[] buffer = new char[4096];
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
            return getFromResultSetTimestamp(rs, index, column);
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
    public String getCreateFulltextIndexSql(String indexName, String quotedIndexName, Table table, List<Column> columns,
            Model model) {
        return String.format(
                "CREATE INDEX %s ON %s(%s) INDEXTYPE IS CTXSYS.CONTEXT "
                        + "PARAMETERS('%s SYNC (ON COMMIT) TRANSACTIONAL')",
                quotedIndexName, table.getQuotedName(), columns.get(0).getQuotedName(), fulltextParameters);
    }

    protected static final String CHARS_RESERVED_STR = "%${";

    protected static final Set<Character> CHARS_RESERVED = new HashSet<>(
            Arrays.asList(ArrayUtils.toObject(CHARS_RESERVED_STR.toCharArray())));

    @Override
    public String getDialectFulltextQuery(String query) {
        query = query.replace("*", "%"); // reserved, words with it not quoted
        FulltextQuery ft = FulltextQueryAnalyzer.analyzeFulltextQuery(query);
        if (ft == null) {
            return "DONTMATCHANYTHINGFOREMPTYQUERY";
        }
        return FulltextQueryAnalyzer.translateFulltext(ft, "OR", "AND", "NOT", "{", "}", CHARS_RESERVED, "", "", true);
    }

    // SELECT ..., (SCORE(1) / 100) AS "_nxscore"
    // FROM ... LEFT JOIN fulltext ON fulltext.id = hierarchy.id
    // WHERE ... AND CONTAINS(fulltext.fulltext, ?, 1) > 0
    // ORDER BY "_nxscore" DESC
    @Override
    public FulltextMatchInfo getFulltextScoredMatchInfo(String fulltextQuery, String indexName, int nthMatch,
            Column mainColumn, Model model, Database database) {
        String indexSuffix = model.getFulltextIndexSuffix(indexName);
        Table ft = database.getTable(Model.FULLTEXT_TABLE_NAME);
        Column ftMain = ft.getColumn(Model.MAIN_KEY);
        Column ftColumn = ft.getColumn(Model.FULLTEXT_FULLTEXT_KEY + indexSuffix);
        String score = String.format("SCORE(%d)", nthMatch);
        String nthSuffix = nthMatch == 1 ? "" : String.valueOf(nthMatch);
        FulltextMatchInfo info = new FulltextMatchInfo();
        if (nthMatch == 1) {
            // Need only one JOIN involving the fulltext table
            info.joins = Collections.singletonList(new Join(Join.INNER, ft.getQuotedName(), null, null,
                    ftMain.getFullQuotedName(), mainColumn.getFullQuotedName()));
        }
        info.whereExpr = String.format("CONTAINS(%s, ?, %d) > 0", ftColumn.getFullQuotedName(), nthMatch);
        info.whereExprParam = fulltextQuery;
        info.scoreExpr = String.format("(%s / 100)", score);
        info.scoreAlias = openQuote() + "_nxscore" + nthSuffix + closeQuote();
        info.scoreCol = new Column(mainColumn.getTable(), null, ColumnType.DOUBLE, null);
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
    public String getLikeEscaping() {
        return " ESCAPE '\\'";
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
    public boolean needsOriginalColumnInGroupBy() {
        // http://download.oracle.com/docs/cd/B19306_01/server.102/b14200/statements_10002.htm#i2080424
        // The alias can be used in the order_by_clause but not other clauses in
        // the query.
        return true;
    }

    @Override
    public boolean needsOracleJoins() {
        return true;
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
    public String getPrepareUserReadAclsSql() {
        return "{CALL nx_prepare_user_read_acls(?)}";
    }

    @Override
    public String getReadAclsCheckSql(String userIdCol) {
        return String.format("%s = nx_hash_users(?)", userIdCol);
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
    public boolean supportsFastDescendants() {
        return pathOptimizationsEnabled;
    }

    @Override
    public String getInTreeSql(String idColumnName, String id) {
        String idParam;
        switch (idType) {
        case VARCHAR:
            idParam = "?";
            break;
        case SEQUENCE:
            // check that it's really an integer
            if (id != null && !org.apache.commons.lang.StringUtils.isNumeric(id)) {
                return null;
            }
            idParam = "CAST(? AS NUMBER(10,0))";
            break;
        default:
            throw new AssertionError("Unknown id type: " + idType);
        }

        if (pathOptimizationsVersion == 2) {
            return String.format("EXISTS(SELECT 1 FROM ancestors WHERE hierarchy_id = %s AND ancestor = %s)",
                    idColumnName, idParam);
        } else if (pathOptimizationsVersion == 1) {
            // using nested table optim
            return String.format("EXISTS(SELECT 1 FROM ancestors WHERE hierarchy_id = %s AND %s MEMBER OF ancestors)",
                    idColumnName, idParam);
        } else {
            // no optimization
            return String.format(
                    "%s in (SELECT id FROM hierarchy WHERE LEVEL>1 AND isproperty = 0 START WITH id = %s CONNECT BY PRIOR id = parentid)",
                    idColumnName, idParam);
        }
    }

    @Override
    public boolean isClusteringSupported() {
        return true;
    }

    /*
     * For Oracle we don't use a function to return values and delete them at the same time, because pipelined functions
     * that need to do DML have to do it in an autonomous transaction which could cause consistency issues.
     */
    @Override
    public boolean isClusteringDeleteNeeded() {
        return true;
    }

    @Override
    public String getClusterInsertInvalidations() {
        return "{CALL NX_CLUSTER_INVAL(?, ?, ?, ?)}";
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
        return String.format(
                "SELECT * FROM (SELECT /*+ FIRST_ROWS(%d) */  a.*, ROWNUM rnum FROM (%s) a WHERE ROWNUM <= %d) WHERE rnum  > %d",
                limit, sql, limit + offset, offset);
    }

    @Override
    public boolean supportsWith() {
        return false;
    }

    @Override
    public boolean supportsArrays() {
        return true;
    }

    @Override
    public boolean supportsArraysReturnInsteadOfRows() {
        return true;
    }

    @Override
    public Serializable[] getArrayResult(Array array) throws SQLException {
        Serializable[] ids;
        if (array.getBaseType() == Types.NUMERIC) {
            long[] longs;
            try {
                longs = (long[]) arrayGetLongArrayMethod.invoke(array);
            } catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
                throw new RuntimeException(e);
            }
            ids = new Serializable[longs.length];
            for (int i = 0; i < ids.length; i++) {
                ids[i] = Long.valueOf(longs[i]);
            }
        } else {
            ids = (Serializable[]) array.getArray();
        }
        return ids;
    }

    @Override
    public boolean hasNullEmptyString() {
        return true;
    }

    @Override
    public Array createArrayOf(int type, Object[] elements, Connection connection) throws SQLException {
        if (elements == null || elements.length == 0) {
            return null;
        }
        String typeName;
        switch (type) {
        case Types.VARCHAR:
            typeName = "NX_STRING_TABLE";
            break;
        case Types.OTHER: // id
            switch (idType) {
            case VARCHAR:
                typeName = "NX_STRING_TABLE";
                break;
            case SEQUENCE:
                typeName = "NX_INT_TABLE";
                break;
            default:
                throw new AssertionError("Unknown id type: " + idType);
            }
            break;
        default:
            throw new AssertionError("Unknown type: " + type);
        }
        connection = ConnectionHelper.unwrap(connection);
        try {
            Object arrayDescriptor = arrayDescriptorConstructor.newInstance(typeName, connection);
            return (Array) arrayConstructor.newInstance(arrayDescriptor, connection, elements);
        } catch (ReflectiveOperationException e) {
            throw new SQLException(e);
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
    public Map<String, Serializable> getSQLStatementsProperties(Model model, Database database) {
        Map<String, Serializable> properties = new HashMap<>();
        switch (idType) {
        case VARCHAR:
            properties.put("idType", "VARCHAR2(36)");
            properties.put("idTypeParam", "VARCHAR2");
            properties.put("idArrayType", "NX_STRING_TABLE");
            properties.put("idNotPresent", "'-'");
            properties.put("sequenceEnabled", Boolean.FALSE);
            break;
        case SEQUENCE:
            properties.put("idType", "NUMBER(10,0)");
            properties.put("idTypeParam", "NUMBER");
            properties.put("idArrayType", "NX_INT_TABLE");
            properties.put("idNotPresent", "-1");
            properties.put("sequenceEnabled", Boolean.TRUE);
            properties.put("idSequenceName", idSequenceName);
            break;
        default:
            throw new AssertionError("Unknown id type: " + idType);
        }
        properties.put("aclOptimizationsEnabled", Boolean.valueOf(aclOptimizationsEnabled));
        properties.put("pathOptimizationsEnabled", Boolean.valueOf(pathOptimizationsEnabled));
        properties.put("pathOptimizationsVersion1", pathOptimizationsVersion == 1);
        properties.put("pathOptimizationsVersion2", pathOptimizationsVersion == 2);
        properties.put("fulltextEnabled", Boolean.valueOf(!fulltextDisabled));
        properties.put("fulltextSearchEnabled", Boolean.valueOf(!fulltextSearchDisabled));
        properties.put("clusteringEnabled", Boolean.valueOf(clusteringEnabled));
        properties.put("proxiesEnabled", Boolean.valueOf(proxiesEnabled));
        properties.put("softDeleteEnabled", Boolean.valueOf(softDeleteEnabled));
        if (!fulltextSearchDisabled) {
            Table ft = database.getTable(Model.FULLTEXT_TABLE_NAME);
            properties.put("fulltextTable", ft.getQuotedName());
            FulltextConfiguration fti = model.getFulltextConfiguration();
            List<String> lines = new ArrayList<>(fti.indexNames.size());
            for (String indexName : fti.indexNames) {
                String suffix = model.getFulltextIndexSuffix(indexName);
                Column ftft = ft.getColumn(Model.FULLTEXT_FULLTEXT_KEY + suffix);
                Column ftst = ft.getColumn(Model.FULLTEXT_SIMPLETEXT_KEY + suffix);
                Column ftbt = ft.getColumn(Model.FULLTEXT_BINARYTEXT_KEY + suffix);
                String line = String.format("  :NEW.%s := :NEW.%s || ' ' || :NEW.%s; ", ftft.getQuotedName(),
                        ftst.getQuotedName(), ftbt.getQuotedName());
                lines.add(line);
            }
            properties.put("fulltextTriggerStatements", String.join("\n", lines));
        }
        String[] permissions = NXCore.getSecurityService().getPermissionsToCheck(SecurityConstants.BROWSE);
        List<String> permsList = new LinkedList<>();
        for (String perm : permissions) {
            permsList.add(String.format("  INTO ACLR_PERMISSION VALUES ('%s')", perm));
        }
        properties.put("readPermissions", String.join("\n", permsList));
        properties.put("usersSeparator", getUsersSeparator());
        properties.put("everyone", SecurityConstants.EVERYONE);
        return properties;
    }

    protected int getOracleErrorCode(Throwable t) {
        try {
            Method m = t.getClass().getMethod("getOracleError");
            Integer oracleError = (Integer) m.invoke(t);
            if (oracleError != null) {
                int errorCode = oracleError.intValue();
                if (errorCode != 0) {
                    return errorCode;
                }
            }
        } catch (ReflectiveOperationException e) {
            // ignore
        }
        if (t instanceof SQLException) {
            return ((SQLException) t).getErrorCode();
        }
        return 0;
    }

    protected boolean isConnectionClosed(int oracleError) {
        switch (oracleError) {
        case 28: // your session has been killed.
        case 1033: // Oracle initialization or shudown in progress.
        case 1034: // Oracle not available
        case 1041: // internal error. hostdef extension doesn't exist
        case 1089: // immediate shutdown in progress - no operations are permitted
        case 1090: // shutdown in progress - connection is not permitted
        case 3113: // end-of-file on communication channel
        case 3114: // not connected to ORACLE
        case 12571: // TNS:packet writer failure
        case 17002: // IO Exception
        case 17008: // Closed Connection
        case 17410: // No more data to read from socket
        case 24768: // commit protocol error occured in the server
            return true;
        }
        return false;
    }

    @Override
    public boolean isConcurrentUpdateException(Throwable t) {
        while (t.getCause() != null) {
            t = t.getCause();
        }
        switch (getOracleErrorCode(t)) {
        case 1: // ORA-00001: unique constraint violated
        case 60: // ORA-00060: deadlock detected while waiting for resource
        case 2291: // ORA-02291: integrity constraint ... violated - parent key not found
            return true;
        }
        return false;
    }

    @Override
    public String getValidationQuery() {
        return "SELECT 1 FROM DUAL";
    }

    @Override
    public String getBlobLengthFunction() {
        return "LENGTHB";
    }

    @Override
    public List<String> getPostCreateIdentityColumnSql(Column column) {
        String table = column.getTable().getPhysicalName();
        String col = column.getPhysicalName();
        String seq = table + "_IDSEQ";
        String trig = table + "_IDTRIG";
        String createSeq = String.format("CREATE SEQUENCE \"%s\"", seq);
        String createTrig = String.format("CREATE TRIGGER \"%s\"\n" //
                + "  BEFORE INSERT ON \"%s\"\n" //
                + "  FOR EACH ROW WHEN (NEW.\"%s\" IS NULL)\n" //
                + "BEGIN\n" //
                + "  SELECT \"%s\".NEXTVAL INTO :NEW.\"%s\" FROM DUAL;\n" //
                + "END;", trig, table, col, seq, col);
        return Arrays.asList(createSeq, createTrig);
    }

    @Override
    public boolean hasIdentityGeneratedKey() {
        return false;
    }

    @Override
    public String getIdentityGeneratedKeySql(Column column) {
        String table = column.getTable().getPhysicalName();
        String seq = table + "_IDSEQ";
        return String.format("SELECT \"%s\".CURRVAL FROM DUAL", seq);
    }

    @Override
    public String getAncestorsIdsSql() {
        return "SELECT NX_ANCESTORS(?) FROM DUAL";
    }

    @Override
    public boolean needsNullsLastOnDescSort() {
        return true;
    }

    @Override
    public String getDateCast() {
        // CAST(%s AS DATE) doesn't work, it doesn't compare exactly to DATE
        // literals because the internal representation seems to be a float and
        // CAST AS DATE does not truncate it
        return "TRUNC(%s)";
    }

    @Override
    public String castIdToVarchar(String expr) {
        switch (idType) {
        case VARCHAR:
            return expr;
        case SEQUENCE:
            return "CAST(" + expr + " AS VARCHAR2(36))";
        default:
            throw new AssertionError("Unknown id type: " + idType);
        }
    }

    @Override
    public DialectIdType getIdType() {
        return idType;
    }

    public String getUsersSeparator() {
        if (usersSeparator == null) {
            return DEFAULT_USERS_SEPARATOR;
        }
        return usersSeparator;
    }

    @Override
    public String getSoftDeleteSql() {
        return "{CALL NX_DELETE(?, ?)}";
    }

    @Override
    public String getSoftDeleteCleanupSql() {
        return "{CALL NX_DELETE_PURGE(?, ?, ?)}";
    }

    @Override
    public List<String> getStartupSqls(Model model, Database database) {
        if (aclOptimizationsEnabled) {
            log.info("Vacuuming tables used by optimized acls");
            return Collections.singletonList("{CALL nx_vacuum_read_acls}");
        }
        return Collections.emptyList();
    }

    @Override
    public List<String> checkStoredProcedure(String procName, String procCreate, String ddlMode, Connection connection,
            JDBCLogger logger, Map<String, Serializable> properties) throws SQLException {
        boolean compatCheck = ddlMode.contains(RepositoryDescriptor.DDL_MODE_COMPAT);
        if (compatCheck) {
            procCreate = "CREATE OR REPLACE " + procCreate.substring("create ".length());
            return Collections.singletonList(procCreate);
        }
        try (Statement st = connection.createStatement()) {
            String getBody;
            if (procCreate.toLowerCase().startsWith("create trigger ")) {
                getBody = "SELECT TRIGGER_BODY FROM USER_TRIGGERS WHERE TRIGGER_NAME = '" + procName + "'";
            } else {
                // works for TYPE, FUNCTION, PROCEDURE
                getBody = "SELECT TEXT FROM ALL_SOURCE WHERE NAME = '" + procName + "' ORDER BY LINE";
            }
            logger.log(getBody);
            try (ResultSet rs = st.executeQuery(getBody)) {
                if (rs.next()) {
                    List<String> lines = new ArrayList<>();
                    do {
                        lines.add(rs.getString(1));
                    } while (rs.next());
                    String body = org.apache.commons.lang.StringUtils.join(lines, ' ');
                    if (normalizeString(procCreate).contains(normalizeString(body))) {
                        logger.log("  -> exists, unchanged");
                        return Collections.emptyList();
                    } else {
                        logger.log("  -> exists, old");
                        if (!procCreate.toLowerCase().startsWith("create ")) {
                            throw new NuxeoException("Should start with CREATE: " + procCreate);
                        }
                        procCreate = "CREATE OR REPLACE " + procCreate.substring("create ".length());
                        return Collections.singletonList(procCreate);
                    }
                } else {
                    logger.log("  -> missing");
                    return Collections.singletonList(procCreate);
                }
            }
        }
    }

    protected static String normalizeString(String string) {
        return string.replaceAll("[ \n\r\t]+", " ").trim();
    }

    @Override
    public String getSQLForDump(String sql) {
        String sqll = sql.toLowerCase();
        if (sqll.startsWith("{call ")) {
            // transform something used for JDBC calls into a proper SQL*Plus dump
            return "EXECUTE " + sql.substring("{call ".length(), sql.length() - 1); // without ; or /
        }
        if (sqll.endsWith("end")) {
            sql += ";";
        }
        return sql + "\n/";
    }

}
