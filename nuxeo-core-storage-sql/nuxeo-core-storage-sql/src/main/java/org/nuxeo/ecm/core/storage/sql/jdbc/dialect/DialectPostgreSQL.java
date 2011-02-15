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
 *     Benoit Delbosc
 */

package org.nuxeo.ecm.core.storage.sql.jdbc.dialect;

import java.io.Serializable;
import java.net.SocketException;
import java.sql.Array;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.common.utils.StringUtils;
import org.nuxeo.ecm.core.NXCore;
import org.nuxeo.ecm.core.api.security.SecurityConstants;
import org.nuxeo.ecm.core.security.SecurityService;
import org.nuxeo.ecm.core.storage.StorageException;
import org.nuxeo.ecm.core.storage.sql.BinaryManager;
import org.nuxeo.ecm.core.storage.sql.ColumnType;
import org.nuxeo.ecm.core.storage.sql.Model;
import org.nuxeo.ecm.core.storage.sql.ModelFulltext;
import org.nuxeo.ecm.core.storage.sql.RepositoryDescriptor;
import org.nuxeo.ecm.core.storage.sql.jdbc.QueryMaker.QueryMakerException;
import org.nuxeo.ecm.core.storage.sql.jdbc.db.Column;
import org.nuxeo.ecm.core.storage.sql.jdbc.db.Database;
import org.nuxeo.ecm.core.storage.sql.jdbc.db.Join;
import org.nuxeo.ecm.core.storage.sql.jdbc.db.Table;

/**
 * PostgreSQL-specific dialect.
 *
 * @author Florent Guillaume
 */
public class DialectPostgreSQL extends Dialect {

    private static final Log log = LogFactory.getLog(DialectPostgreSQL.class);

    private static final String DEFAULT_FULLTEXT_ANALYZER = "english";

    private static final String DEFAULT_USERS_SEPARATOR = ",";

    protected final String fulltextAnalyzer;

    protected final boolean supportsWith;

    protected boolean hierarchyCreated;

    protected boolean pathOptimizationsEnabled;

    protected String usersSeparator;

    public DialectPostgreSQL(DatabaseMetaData metadata,
            BinaryManager binaryManager,
            RepositoryDescriptor repositoryDescriptor) throws StorageException {
        super(metadata, binaryManager, repositoryDescriptor);
        fulltextAnalyzer = repositoryDescriptor.fulltextAnalyzer == null ? DEFAULT_FULLTEXT_ANALYZER
                : repositoryDescriptor.fulltextAnalyzer;
        pathOptimizationsEnabled = repositoryDescriptor.pathOptimizationsEnabled;
        int major, minor;
        try {
            major = metadata.getDatabaseMajorVersion();
            minor = metadata.getDatabaseMinorVersion();
        } catch (SQLException e) {
            throw new StorageException(e);
        }
        supportsWith = major > 8 || (major == 8 && minor >= 4);
        usersSeparator = repositoryDescriptor.usersSeparatorKey == null ? DEFAULT_USERS_SEPARATOR
                : repositoryDescriptor.usersSeparatorKey;
    }

    @Override
    public String toBooleanValueString(boolean bool) {
        return bool ? "true" : "false";
    }

    @Override
    public String getNoColumnsInsertString() {
        return "DEFAULT VALUES";
    }

    @Override
    public String getCascadeDropConstraintsString() {
        return "CASCADE";
    }

    @Override
    public JDBCInfo getJDBCTypeAndString(ColumnType type) {
        switch (type) {
        case VARCHAR:
            return jdbcInfo("varchar", Types.VARCHAR);
        case CLOB:
            return jdbcInfo("text", Types.CLOB);
        case BOOLEAN:
            return jdbcInfo("bool", Types.BIT);
        case LONG:
            return jdbcInfo("int8", Types.BIGINT);
        case DOUBLE:
            return jdbcInfo("float8", Types.DOUBLE);
        case TIMESTAMP:
            return jdbcInfo("timestamp", Types.TIMESTAMP);
        case BLOBID:
            return jdbcInfo("varchar(40)", Types.VARCHAR);
            // -----
        case NODEID:
        case NODEIDFK:
        case NODEIDFKNP:
        case NODEIDFKMUL:
        case NODEIDFKNULL:
        case NODEIDPK:
        case NODEVAL:
            return jdbcInfo("varchar(36)", Types.VARCHAR);
        case NODEARRAY:
            return jdbcInfo("varchar(36)[]", Types.ARRAY);
        case SYSNAME:
            return jdbcInfo("varchar(250)", Types.VARCHAR);
        case SYSNAMEARRAY:
            return jdbcInfo("varchar(250)[]", Types.ARRAY);
        case TINYINT:
            return jdbcInfo("int2", Types.SMALLINT);
        case INTEGER:
            return jdbcInfo("int4", Types.INTEGER);
        case FTINDEXED:
            return jdbcInfo("tsvector", Types.OTHER);
        case FTSTORED:
            return jdbcInfo("tsvector", Types.OTHER);
        case CLUSTERNODE:
            return jdbcInfo("int4", Types.INTEGER);
        case CLUSTERFRAGS:
            return jdbcInfo("varchar[]", Types.ARRAY);
        }
        throw new AssertionError(type);
    }

    @Override
    public boolean isAllowedConversion(int expected, int actual,
            String actualName, int actualSize) {
        // CLOB vs VARCHAR compatibility
        if (expected == Types.VARCHAR && actual == Types.CLOB) {
            return true;
        }
        if (expected == Types.CLOB && actual == Types.VARCHAR) {
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
        case Types.CLOB:
            setToPreparedStatementString(ps, index, value, column);
            return;
        case Types.BIT:
            ps.setBoolean(index, ((Boolean) value).booleanValue());
            return;
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
            setToPreparedStatementTimestamp(ps, index, value, column);
            return;
        case Types.ARRAY:
            Array array = createArrayOf(Types.VARCHAR, (Object[]) value,
                    ps.getConnection());
            ps.setArray(index, array);
            return;
        case Types.OTHER:
            if (column.getType() == ColumnType.FTSTORED) {
                ps.setString(index, (String) value);
                return;
            }
            throw new SQLException("Unhandled type: " + column.getType());
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
        case Types.CLOB:
            return getFromResultSetString(rs, index, column);
        case Types.BIT:
            return rs.getBoolean(index);
        case Types.SMALLINT:
        case Types.INTEGER:
        case Types.BIGINT:
            return rs.getLong(index);
        case Types.DOUBLE:
            return rs.getDouble(index);
        case Types.TIMESTAMP:
            return getFromResultSetTimestamp(rs, index, column);
        case Types.ARRAY:
            Array array = rs.getArray(index);
            return array == null ? null : (Serializable) array.getArray();
        }
        throw new SQLException("Unhandled JDBC type: " + column.getJdbcType());
    }

    @Override
    public String getCreateFulltextIndexSql(String indexName,
            String quotedIndexName, Table table, List<Column> columns,
            Model model) {
        return String.format("CREATE INDEX %s ON %s USING GIN(%s)",
                quotedIndexName.toLowerCase(), table.getQuotedName(),
                columns.get(0).getQuotedName());
    }

    @Override
    public String getDialectFulltextQuery(String query) {
        query = query.replace(" & ", " "); // PostgreSQL compatibility BBB
        FulltextQuery ft = analyzeFulltextQuery(query);
        if (ft == null) {
            return ""; // won't match anything
        }
        if (fulltextHasPhrase(ft)) {
            throw new QueryMakerException(
                    "Invalid fulltext query (phrase search): " + query);
        }
        return translateFulltext(ft, "|", "&", "& !", "");
    }

    // SELECT ..., TS_RANK_CD(fulltext, nxquery, 32) as nxscore
    // FROM ... LEFT JOIN fulltext ON fulltext.id = hierarchy.id
    // , TO_TSQUERY('french', ?) as nxquery
    // WHERE ... AND fulltext @@ nxquery
    // ORDER BY nxscore DESC
    @Override
    public FulltextMatchInfo getFulltextScoredMatchInfo(String fulltextQuery,
            String indexName, int nthMatch, Column mainColumn, Model model,
            Database database) {
        String indexSuffix = model.getFulltextIndexSuffix(indexName);
        Table ft = database.getTable(model.FULLTEXT_TABLE_NAME);
        Column ftMain = ft.getColumn(model.MAIN_KEY);
        Column ftColumn = ft.getColumn(model.FULLTEXT_FULLTEXT_KEY
                + indexSuffix);
        String nthSuffix = nthMatch == 1 ? "" : String.valueOf(nthMatch);
        String queryAlias = "_nxquery" + nthSuffix;
        String scoreAlias = "_nxscore" + nthSuffix;
        FulltextMatchInfo info = new FulltextMatchInfo();
        info.joins = new ArrayList<Join>();
        if (nthMatch == 1) {
            // Need only one JOIN involving the fulltext table
            info.joins.add(new Join(Join.INNER, ft.getQuotedName(), null, null,
                    ftMain.getFullQuotedName(), mainColumn.getFullQuotedName()));
        }
        info.joins.add(new Join(
                Join.IMPLICIT, //
                String.format("TO_TSQUERY('%s', ?)", fulltextAnalyzer),
                queryAlias, // alias
                fulltextQuery, // param
                null, null));
        info.whereExpr = String.format("(%s @@ %s)", queryAlias,
                ftColumn.getFullQuotedName());
        info.scoreExpr = String.format("TS_RANK_CD(%s, %s, 32) AS %s",
                ftColumn.getFullQuotedName(), queryAlias, scoreAlias);
        info.scoreAlias = scoreAlias;
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
    public String getFreeVariableSetterForType(ColumnType type) {
        if (type == ColumnType.FTSTORED) {
            return "NX_TO_TSVECTOR(?)";
        }
        return "?";
    }

    @Override
    public boolean supportsUpdateFrom() {
        return true;
    }

    @Override
    public boolean doesUpdateFromRepeatSelf() {
        return false;
    }

    @Override
    public boolean needsAliasForDerivedTable() {
        return true;
    }

    @Override
    public boolean supportsIlike() {
        return true;
    }

    @Override
    public boolean supportsReadAcl() {
        return aclOptimizationsEnabled;
    }

    @Override
    public String getReadAclsCheckSql(String idColumnName) {
        return String.format("%s IN (SELECT * FROM nx_get_read_acls_for(?))",
                idColumnName);
    }

    @Override
    public String getUpdateReadAclsSql() {
        return "SELECT nx_update_read_acls();";
    }

    @Override
    public String getRebuildReadAclsSql() {
        return "SELECT nx_rebuild_read_acls();";
    }

    @Override
    public String getSecurityCheckSql(String idColumnName) {
        return String.format("NX_ACCESS_ALLOWED(%s, ?, ?)", idColumnName);
    }

    @Override
    public boolean supportsAncestorsTable() {
        return true;
    }

    @Override
    public String getInTreeSql(String idColumnName) {
        if (pathOptimizationsEnabled) {
            return String.format(
                    "EXISTS(SELECT 1 FROM ancestors WHERE id = %s AND ARRAY[?] <@ ancestors)",
                    idColumnName);
        } else {
            return String.format("NX_IN_TREE(%s, ?)", idColumnName);
        }
    }

    @Override
    public String getMatchMixinType(Column mixinsColumn, String mixin,
            boolean positive, String[] returnParam) {
        returnParam[0] = mixin;
        String sql = "ARRAY[?] <@ " + mixinsColumn.getFullQuotedName();
        return positive ? sql : "NOT(" + sql + ")";
    }

    @Override
    public boolean supportsSysNameArray() {
        return true;
    }

    @Override
    public boolean supportsArrays() {
        return true;
    }

    @Override
    public Array createArrayOf(int type, Object[] elements,
            Connection connection) throws SQLException {
        if (elements == null || elements.length == 0) {
            return null;
        }
        String typeName;
        switch (type) {
        case Types.VARCHAR:
            typeName = "varchar";
            break;
        default:
            // TODO others not used yet
            throw new RuntimeException("" + type);
        }
        return new PostgreSQLArray(type, typeName, elements);
    }

    public static class PostgreSQLArray implements Array {

        private static final String NOT_SUPPORTED = "Not supported";

        protected final int type;

        protected final String typeName;

        protected final Object[] elements;

        protected final String string;

        public PostgreSQLArray(int type, String typeName, Object[] elements) {
            this.type = type;
            if (type == Types.VARCHAR) {
                typeName = "varchar";
            }
            this.typeName = typeName;
            this.elements = elements;
            StringBuilder b = new StringBuilder();
            appendArray(b, elements);
            string = b.toString();
        }

        protected static void appendArray(StringBuilder b, Object[] elements) {
            b.append('{');
            for (int i = 0; i < elements.length; i++) {
                Object e = elements[i];
                if (i > 0) {
                    b.append(',');
                }
                if (e == null) {
                    b.append("NULL");
                } else if (e.getClass().isArray()) {
                    appendArray(b, (Object[]) e);
                } else {
                    // we always transform to a string, the postgres
                    // array parsing methods will then reparse this as needed
                    String s = e.toString();
                    b.append('"');
                    for (int j = 0; j < s.length(); j++) {
                        char c = s.charAt(j);
                        if (c == '"' || c == '\\') {
                            b.append('\\');
                        }
                        b.append(c);
                    }
                    b.append('"');
                }
            }
            b.append('}');
        }

        @Override
        public String toString() {
            return string;
        }

        @Override
        public int getBaseType() {
            return type;
        }

        @Override
        public String getBaseTypeName() {
            return typeName;
        }

        @Override
        public Object getArray() {
            return elements;
        }

        @Override
        public Object getArray(Map<String, Class<?>> map) throws SQLException {
            throw new SQLException(NOT_SUPPORTED);
        }

        @Override
        public Object getArray(long index, int count) throws SQLException {
            throw new SQLException(NOT_SUPPORTED);
        }

        @Override
        public Object getArray(long index, int count, Map<String, Class<?>> map)
                throws SQLException {
            throw new SQLException(NOT_SUPPORTED);
        }

        @Override
        public ResultSet getResultSet() throws SQLException {
            throw new SQLException(NOT_SUPPORTED);
        }

        @Override
        public ResultSet getResultSet(Map<String, Class<?>> map)
                throws SQLException {
            throw new SQLException(NOT_SUPPORTED);
        }

        @Override
        public ResultSet getResultSet(long index, int count)
                throws SQLException {
            throw new SQLException(NOT_SUPPORTED);
        }

        @Override
        public ResultSet getResultSet(long index, int count,
                Map<String, Class<?>> map) throws SQLException {
            throw new SQLException(NOT_SUPPORTED);
        }

        // this is needed by JDBC 4 (Java 6)
        @Override
        public void free() {
        }
    }

    @Override
    public String getSQLStatementsFilename() {
        return "nuxeovcs/postgresql.sql.txt";
    }

    @Override
    public String getTestSQLStatementsFilename() {
        return "nuxeovcs/postgresql.test.sql.txt";
    }

    @Override
    public Map<String, Serializable> getSQLStatementsProperties(Model model,
            Database database) {
        Map<String, Serializable> properties = new HashMap<String, Serializable>();
        properties.put("idType", "varchar(36)");
        properties.put("aclOptimizationsEnabled",
                Boolean.valueOf(aclOptimizationsEnabled));
        properties.put("pathOptimizationsEnabled",
                Boolean.valueOf(pathOptimizationsEnabled));
        properties.put("fulltextAnalyzer", fulltextAnalyzer);
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
                        "  NEW.%s := COALESCE(NEW.%s, ''::TSVECTOR) || COALESCE(NEW.%s, ''::TSVECTOR);",
                        ftft.getQuotedName(), ftst.getQuotedName(),
                        ftbt.getQuotedName());
                lines.add(line);
            }
            properties.put("fulltextTriggerStatements",
                    StringUtils.join(lines, "\n"));
        }
        String[] permissions = NXCore.getSecurityService().getPermissionsToCheck(
                SecurityConstants.BROWSE);
        List<String> permsList = new LinkedList<String>();
        for (String perm : permissions) {
            permsList.add("('" + perm + "')");
        }
        properties.put("readPermissions", StringUtils.join(permsList, ", "));
        properties.put("usersSeparator", getUsersSeparator());
        properties.put("everyone", SecurityConstants.EVERYONE);
        properties.put("readAclMaxSize", Integer.toString(readAclMaxSize));
        return properties;
    }

    @Override
    public boolean preCreateTable(Connection connection, Table table,
            Model model, Database database) throws SQLException {
        String tableKey = table.getKey();
        if (model.HIER_TABLE_NAME.equals(tableKey)) {
            hierarchyCreated = true;
            return true;
        }
        if (model.ANCESTORS_TABLE_NAME.equals(tableKey)) {
            if (hierarchyCreated) {
                // database initialization
                return true;
            }
            // upgrade of an existing database
            // check hierarchy size
            String sql = "SELECT COUNT(*) FROM hierarchy WHERE NOT isproperty";
            Statement s = connection.createStatement();
            ResultSet rs = s.executeQuery(sql);
            rs.next();
            long count = rs.getLong(1);
            rs.close();
            s.close();
            if (count > 100000) {
                // if the hierarchy table is too big, tell the admin to do the
                // init by hand
                pathOptimizationsEnabled = false;
                log.error("Table ANCESTORS not initialized automatically because table HIERARCHY is too big. "
                        + "Upgrade by hand by calling: SELECT nx_init_ancestors()");
            }
            return true;
        }
        return true;
    }

    @Override
    public List<String> getPostCreateTableSqls(Table table, Model model,
            Database database) {
        if (Model.ANCESTORS_TABLE_NAME.equals(table.getKey())) {
            List<String> sqls = new ArrayList<String>();
            if (pathOptimizationsEnabled) {
                sqls.add("SELECT nx_init_ancestors()");
            } else {
                log.info("Path optimizations disabled");
            }
            return sqls;
        }
        return Collections.emptyList();
    }

    @Override
    public void existingTableDetected(Connection connection, Table table,
            Model model, Database database) throws SQLException {
        if (Model.ANCESTORS_TABLE_NAME.equals(table.getKey())) {
            if (!pathOptimizationsEnabled) {
                log.info("Path optimizations disabled");
                return;
            }
            // check if we want to initialize the descendants table now, or log
            // a warning if the hierarchy table is too big
            String sql = "SELECT id FROM ancestors LIMIT 1";
            Statement s = connection.createStatement();
            ResultSet rs = s.executeQuery(sql);
            boolean empty = !rs.next();
            rs.close();
            s.close();
            if (empty) {
                pathOptimizationsEnabled = false;
                log.error("Table ANCESTORS empty, must be upgraded by hand by calling: "
                        + "SELECT nx_init_ancestors()");
                log.info("Path optimizations disabled");
            }
        }
    }

    @Override
    public boolean isClusteringSupported() {
        return true;
    }

    @Override
    public String getClusterInsertInvalidations() {
        return "SELECT NX_CLUSTER_INVAL(?, ?, ?)";
    }

    @Override
    public String getClusterGetInvalidations() {
        return "DELETE FROM cluster_invals WHERE nodeid = pg_backend_pid()"
                + " RETURNING id, fragments, kind";
    }

    @Override
    public boolean connectionClosedByException(Throwable t) {
        while (t.getCause() != null) {
            t = t.getCause();
        }
        // org.postgresql.util.PSQLException. message: An I/O error occured
        // while sending to the backend
        // Caused by: java.net.SocketException. message: Broken pipe
        if (t instanceof SocketException) {
            return true;
        }
        // org.postgresql.util.PSQLException. message: FATAL: terminating
        // connection due to administrator command
        String message = t.getMessage();
        if (message != null && message.contains("FATAL:")) {
            return true;
        }
        return false;
    }

    @Override
    public boolean supportsPaging() {
        return true;
    }

    @Override
    public String getPagingClause(long limit, long offset) {
        return String.format("LIMIT %d OFFSET %d", limit, offset);
    }

    @Override
    public boolean supportsWith() {
        return false; // don't activate until proven useful
        // return supportsWith;
    }

    @Override
    public void performAdditionalStatements(Connection connection)
            throws SQLException {
        // Warn user if BROWSE permissions has changed
        Set<String> dbPermissions = new HashSet<String>();
        String sql = "SELECT * FROM read_acl_permissions";
        Statement s = connection.createStatement();
        ResultSet rs = s.executeQuery(sql);
        while (rs.next()) {
            dbPermissions.add(rs.getString(1));
        }
        rs.close();
        s.close();
        Set<String> confPermissions = new HashSet<String>();
        SecurityService securityService = NXCore.getSecurityService();
        for (String perm : securityService.getPermissionsToCheck(SecurityConstants.BROWSE)) {
            confPermissions.add(perm);
        }
        if (!dbPermissions.equals(confPermissions)) {
            log.error("Security permission for BROWSE has changed, you need to rebuild the optimized read acls:"
                    + "DROP TABLE read_acl_permissions; DROP TABLE read_acls; then restart.");
        }
    }

    public String getUsersSeparator() {
        if (usersSeparator == null) {
            return DEFAULT_USERS_SEPARATOR;
        }
        return usersSeparator;
    }
}
