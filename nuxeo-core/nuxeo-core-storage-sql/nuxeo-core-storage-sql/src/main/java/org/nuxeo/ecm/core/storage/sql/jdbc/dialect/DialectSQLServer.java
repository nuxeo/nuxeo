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

import java.io.Serializable;
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
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.common.utils.StringUtils;
import org.nuxeo.ecm.core.NXCore;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.core.api.security.SecurityConstants;
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

/**
 * Microsoft SQL Server-specific dialect.
 *
 * @author Florent Guillaume
 */
public class DialectSQLServer extends Dialect {

    private static final Log log = LogFactory.getLog(DialectSQLServer.class);

    private static final String DEFAULT_FULLTEXT_ANALYZER = "english";

    private static final String DEFAULT_FULLTEXT_CATALOG = "nuxeo";

    /**
     * Column containing an IDENTITY used to create a clustered index.
     */
    public static final String CLUSTER_INDEX_COL = "_oid";

    protected final String fulltextAnalyzer;

    protected final String fulltextCatalog;

    private static final String DEFAULT_USERS_SEPARATOR = "|";

    protected final String usersSeparator;

    protected final DialectIdType idType;

    protected String idSequenceName;

    protected boolean pathOptimizationsEnabled;

    /** 9 = SQL Server 2005, 10 = SQL Server 2008, 11 = SQL Server 2012 / Azure */
    protected int majorVersion;

    // http://msdn.microsoft.com/en-us/library/ms174396.aspx
    /** 5 = Azure */
    protected int engineEdition;

    protected boolean azure;

    public DialectSQLServer(DatabaseMetaData metadata, RepositoryDescriptor repositoryDescriptor) {
        super(metadata, repositoryDescriptor);
        try {
            checkDatabaseConfiguration(metadata.getConnection());
            majorVersion = metadata.getDatabaseMajorVersion();
            engineEdition = getEngineEdition(metadata.getConnection());

        } catch (SQLException e) {
            throw new NuxeoException(e);
        }
        if (engineEdition == 5) { // 5 = SQL Azure
            azure = true;
            fulltextDisabled = true;
            fulltextSearchDisabled = true;
            if (repositoryDescriptor != null) {
                repositoryDescriptor.setFulltextDisabled(true);
            }
        }
        fulltextAnalyzer = repositoryDescriptor == null ? null
                : repositoryDescriptor.getFulltextAnalyzer() == null ? DEFAULT_FULLTEXT_ANALYZER
                        : repositoryDescriptor.getFulltextAnalyzer();
        fulltextCatalog = repositoryDescriptor == null ? null
                : repositoryDescriptor.getFulltextCatalog() == null ? DEFAULT_FULLTEXT_CATALOG
                        : repositoryDescriptor.getFulltextCatalog();
        usersSeparator = repositoryDescriptor == null ? null
                : repositoryDescriptor.usersSeparatorKey == null ? DEFAULT_USERS_SEPARATOR
                        : repositoryDescriptor.usersSeparatorKey;
        pathOptimizationsEnabled = repositoryDescriptor != null && repositoryDescriptor.getPathOptimizationsEnabled();
        String idt = repositoryDescriptor == null ? null : repositoryDescriptor.idType;
        if (idt == null || "".equals(idt) || "varchar".equalsIgnoreCase(idt)) {
            idType = DialectIdType.VARCHAR;
        } else if (idt.toLowerCase().startsWith("sequence")) {
            idType = DialectIdType.SEQUENCE;
            if (idt.toLowerCase().startsWith("sequence:")) {
                String[] split = idt.split(":");
                idSequenceName = split[1];
            } else {
                idSequenceName = "hierarchy_seq";
            }
        } else {
            throw new NuxeoException("Unknown id type: '" + idt + "'");
        }

    }

    @Override
    public boolean supportsPaging() {
        // available since SQL Server 2012
        return (majorVersion >= 11);
    }

    @Override
    public String addPagingClause(String sql, long limit, long offset) {
        if (!sql.contains("ORDER")) {
            // Order is required to use the offset operation
            sql += " ORDER BY 1";
        }
        return sql + String.format(" OFFSET %d ROWS FETCH NEXT %d ROWS ONLY", offset, limit);
    }

    protected int getEngineEdition(Connection connection) throws SQLException {
        try (Statement st = connection.createStatement()) {
            ResultSet rs = st.executeQuery("SELECT CONVERT(NVARCHAR(100), SERVERPROPERTY('EngineEdition'))");
            rs.next();
            return rs.getInt(1);
        }
    }

    protected void checkDatabaseConfiguration(Connection connection) throws SQLException {
        try (Statement stmt = connection.createStatement()) {
            String sql = "SELECT is_read_committed_snapshot_on FROM sys.databases WHERE name = db_name()";
            if (log.isTraceEnabled()) {
                log.trace("SQL: " + sql);
            }
            ResultSet rs = stmt.executeQuery(sql);
            if (!rs.next()) {
                throw new SQLException("Cannot detect whether READ_COMMITTED_SNAPSHOT is on");
            }
            int on = rs.getInt(1);
            if (on != 1) {
                throw new SQLException("Incorrect database configuration, you must enable READ_COMMITTED_SNAPSHOT");
            }
            rs.close();
        }
    }

    @Override
    public char openQuote() {
        return '[';
    }

    @Override
    public char closeQuote() {
        return ']';
    }

    @Override
    public String getNoColumnsInsertString(Column idColumn) {
        return "DEFAULT VALUES";
    }

    @Override
    public String getNullColumnString() {
        return " NULL";
    }

    @Override
    public boolean qualifyIndexName() {
        return false;
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
                return jdbcInfo("NVARCHAR(4000)", Types.VARCHAR);
            } else if (type.isClob() || type.length > 4000) {
                return jdbcInfo("NVARCHAR(MAX)", Types.CLOB);
            } else {
                return jdbcInfo("NVARCHAR(%d)", type.length, Types.VARCHAR);
            }
        case BOOLEAN:
            return jdbcInfo("BIT", Types.BIT);
        case LONG:
            return jdbcInfo("BIGINT", Types.BIGINT);
        case DOUBLE:
            return jdbcInfo("DOUBLE PRECISION", Types.DOUBLE);
        case TIMESTAMP:
            return jdbcInfo("DATETIME2(3)", Types.TIMESTAMP);
        case BLOBID:
            return jdbcInfo("NVARCHAR(250)", Types.VARCHAR);
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
                return jdbcInfo("NVARCHAR(36)", Types.VARCHAR);
            case SEQUENCE:
                return jdbcInfo("BIGINT", Types.BIGINT);
            }
        case SYSNAME:
        case SYSNAMEARRAY:
            return jdbcInfo("NVARCHAR(256)", Types.VARCHAR);
        case TINYINT:
            return jdbcInfo("TINYINT", Types.TINYINT);
        case INTEGER:
            return jdbcInfo("INT", Types.INTEGER);
        case AUTOINC:
            return jdbcInfo("INT IDENTITY", Types.INTEGER);
        case FTINDEXED:
            throw new AssertionError(type);
        case FTSTORED:
            return jdbcInfo("NVARCHAR(MAX)", Types.CLOB);
        case CLUSTERNODE:
            return jdbcInfo("SMALLINT", Types.SMALLINT);
        case CLUSTERFRAGS:
            return jdbcInfo("NVARCHAR(4000)", Types.VARCHAR);
        }
        throw new AssertionError(type);
    }

    @Override
    public boolean isAllowedConversion(int expected, int actual, String actualName, int actualSize) {
        // The jTDS JDBC driver uses VARCHAR / CLOB
        // The Microsoft JDBC driver uses NVARCHAR / LONGNVARCHAR
        if (expected == Types.VARCHAR && actual == Types.CLOB) {
            return true;
        }
        if (expected == Types.VARCHAR && actual == Types.NVARCHAR) {
            return true;
        }
        if (expected == Types.VARCHAR && actual == Types.LONGNVARCHAR) {
            return true;
        }
        if (expected == Types.CLOB && actual == Types.VARCHAR) {
            return true;
        }
        if (expected == Types.CLOB && actual == Types.NVARCHAR) {
            return true;
        }
        if (expected == Types.CLOB && actual == Types.LONGNVARCHAR) {
            return true;
        }
        if (expected == Types.BIGINT && actual == Types.INTEGER) {
            return true;
        }
        if (expected == Types.INTEGER && actual == Types.BIGINT) {
            return true;
        }
        return false;
    }

    @Override
    public void setId(PreparedStatement ps, int index, Serializable value) throws SQLException {
        switch (idType) {
        case VARCHAR:
            ps.setObject(index, value, Types.VARCHAR);
            break;
        case SEQUENCE:
            setIdLong(ps, index, value);
            break;
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
        return 128;
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
    public boolean supportsMultipleFulltextIndexes() {
        // With SQL Server, only one full-text index is allowed per table...
        return false;
    }

    @Override
    public String getCreateFulltextIndexSql(String indexName, String quotedIndexName, Table table, List<Column> columns,
            Model model) {
        StringBuilder buf = new StringBuilder();
        buf.append(String.format("CREATE FULLTEXT INDEX ON %s (", table.getQuotedName()));
        Iterator<Column> it = columns.iterator();
        while (it.hasNext()) {
            buf.append(String.format("%s LANGUAGE %s", it.next().getQuotedName(), getQuotedFulltextAnalyzer()));
            if (it.hasNext()) {
                buf.append(", ");
            }
        }
        String fulltextUniqueIndex = "[fulltext_pk]";
        buf.append(String.format(") KEY INDEX %s ON [%s]", fulltextUniqueIndex, fulltextCatalog));
        return buf.toString();
    }

    @Override
    public String getDialectFulltextQuery(String query) {
        query = query.replace("%", "*");
        FulltextQuery ft = FulltextQueryAnalyzer.analyzeFulltextQuery(query);
        if (ft == null) {
            return "DONTMATCHANYTHINGFOREMPTYQUERY";
        }
        return FulltextQueryAnalyzer.translateFulltext(ft, "OR", "AND", "AND NOT", "\"", "\"",
                Collections.<Character> emptySet(), "\"", "\"", false);
    }

    // SELECT ..., FTTBL.RANK / 1000.0
    // FROM ... LEFT JOIN [fulltext] ON [fulltext].[id] = [hierarchy].[id]
    // ........ LEFT JOIN CONTAINSTABLE([fulltext], *, ?, LANGUAGE 'english')
    // .................. AS FTTBL
    // .................. ON [fulltext].[id] = FTTBL.[KEY]
    // WHERE ... AND FTTBL.[KEY] IS NOT NULL
    // ORDER BY FTTBL.RANK DESC
    @Override
    public FulltextMatchInfo getFulltextScoredMatchInfo(String fulltextQuery, String indexName, int nthMatch,
            Column mainColumn, Model model, Database database) {
        // TODO multiple indexes
        Table ft = database.getTable(Model.FULLTEXT_TABLE_NAME);
        Column ftMain = ft.getColumn(Model.MAIN_KEY);
        String nthSuffix = nthMatch == 1 ? "" : String.valueOf(nthMatch);
        String tableAlias = "_nxfttbl" + nthSuffix;
        FulltextMatchInfo info = new FulltextMatchInfo();
        // there are two left joins here
        info.joins = new ArrayList<>();
        if (nthMatch == 1) {
            // Need only one JOIN involving the fulltext table
            info.joins.add(new Join(Join.LEFT, ft.getQuotedName(), null, null, ftMain.getFullQuotedName(),
                    mainColumn.getFullQuotedName()));
        }
        info.joins.add(
                new Join(Join.LEFT, //
                        String.format("CONTAINSTABLE(%s, *, ?, LANGUAGE %s)", ft.getQuotedName(),
                                getQuotedFulltextAnalyzer()),
                        tableAlias, // alias
                        fulltextQuery, // param
                        ftMain.getFullQuotedName(), // on1
                        String.format("%s.[KEY]", tableAlias) // on2
        ));
        info.whereExpr = String.format("%s.[KEY] IS NOT NULL", tableAlias);
        info.scoreExpr = String.format("(%s.RANK / 1000.0)", tableAlias);
        info.scoreAlias = "_nxscore" + nthSuffix;
        info.scoreCol = new Column(mainColumn.getTable(), null, ColumnType.DOUBLE, null);
        return info;
    }

    protected String getQuotedFulltextAnalyzer() {
        if (!Character.isDigit(fulltextAnalyzer.charAt(0))) {
            return String.format("'%s'", fulltextAnalyzer);
        }
        return fulltextAnalyzer;
    }

    @Override
    public String getLikeEscaping() {
        return " ESCAPE '\\'";
    }

    @Override
    public boolean supportsCircularCascadeDeleteConstraints() {
        // See http://support.microsoft.com/kb/321843
        // Msg 1785 Introducing FOREIGN KEY constraint
        // 'hierarchy_parentid_hierarchy_fk' on table 'hierarchy' may cause
        // cycles or multiple cascade paths. Specify ON DELETE NO ACTION or ON
        // UPDATE NO ACTION, or modify other FOREIGN KEY constraints.
        // Instead we use a trigger "INSTEAD OF DELETE" to do the recursion.
        return false;
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
    public boolean needsAliasForDerivedTable() {
        return true;
    }

    @Override
    public boolean needsOriginalColumnInGroupBy() {
        // http://msdn.microsoft.com/en-us/library/ms177673.aspx
        // A column alias that is defined in the SELECT list cannot be used to
        // specify a grouping column.
        return true;
    }

    @Override
    public String getSecurityCheckSql(String idColumnName) {
        return String.format("dbo.NX_ACCESS_ALLOWED(%s, ?, ?) = 1", idColumnName);
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
            idParam = "CONVERT(BIGINT, ?)";
            break;
        default:
            throw new AssertionError("Unknown id type: " + idType);
        }

        if (pathOptimizationsEnabled) {
            return String.format("EXISTS(SELECT 1 FROM ancestors WHERE hierarchy_id = %s AND ancestor = %s)",
                    idColumnName, idParam);
        }
        return String.format("%s IN (SELECT * FROM dbo.nx_children(%s))", idColumnName, idParam);
    }

    @Override
    public String getSQLStatementsFilename() {
        return "nuxeovcs/sqlserver.sql.txt";
    }

    @Override
    public String getTestSQLStatementsFilename() {
        return "nuxeovcs/sqlserver.test.sql.txt";
    }

    @Override
    public Map<String, Serializable> getSQLStatementsProperties(Model model, Database database) {
        Map<String, Serializable> properties = new HashMap<>();
        switch (idType) {
        case VARCHAR:
            properties.put("idType", "NVARCHAR(36)");
            properties.put("idTypeParam", "NVARCHAR");
            properties.put("idNotPresent", "'-'");
            properties.put("sequenceEnabled", Boolean.FALSE);
            break;
        case SEQUENCE:
            properties.put("idType", "BIGINT");
            properties.put("idTypeParam", "BIGINT");
            properties.put("idNotPresent", "-1");
            properties.put("sequenceEnabled", Boolean.TRUE);
            properties.put("idSequenceName", idSequenceName);
        }
        properties.put("lockEscalationDisabled", Boolean.valueOf(supportsLockEscalationDisable()));
        properties.put("md5HashString", getMd5HashString());
        properties.put("reseedAclrModified", azure ? "" : "DBCC CHECKIDENT('aclr_modified', RESEED, 0);");
        properties.put("fulltextEnabled", Boolean.valueOf(!fulltextDisabled));
        properties.put("fulltextSearchEnabled", Boolean.valueOf(!fulltextSearchDisabled));
        properties.put("fulltextCatalog", fulltextCatalog);
        properties.put("aclOptimizationsEnabled", Boolean.valueOf(aclOptimizationsEnabled));
        properties.put("pathOptimizationsEnabled", Boolean.valueOf(pathOptimizationsEnabled));
        properties.put("clusteringEnabled", Boolean.valueOf(clusteringEnabled));
        properties.put("proxiesEnabled", Boolean.valueOf(proxiesEnabled));
        properties.put("softDeleteEnabled", Boolean.valueOf(softDeleteEnabled));
        String[] permissions = NXCore.getSecurityService().getPermissionsToCheck(SecurityConstants.BROWSE);
        List<String> permsList = new LinkedList<>();
        for (String perm : permissions) {
            permsList.add(String.format("  SELECT '%s' ", perm));
        }
        properties.put("readPermissions", String.join(" UNION ALL ", permsList));
        properties.put("usersSeparator", getUsersSeparator());
        return properties;
    }

    protected String getMd5HashString() {
        if (majorVersion <= 9) {
            // this is an internal function and doesn't work on Azure
            return "SUBSTRING(master.dbo.fn_varbintohexstr(HashBytes('MD5', @string)), 3, 32)";
        } else {
            // this doesn't work on SQL Server 2005
            return "SUBSTRING(CONVERT(VARCHAR(34), HashBytes('MD5', @string), 1), 3, 32)";
        }
    }

    protected boolean supportsLockEscalationDisable() {
        // not supported on SQL Server 2005
        return majorVersion > 9;
    }

    @Override
    public boolean supportsReadAcl() {
        return aclOptimizationsEnabled;
    }

    @Override
    public String getPrepareUserReadAclsSql() {
        return "EXEC nx_prepare_user_read_acls ?";
    }

    @Override
    public String getReadAclsCheckSql(String userIdCol) {
        return String.format("%s = dbo.nx_md5(?)", userIdCol);
    }

    @Override
    public String getUpdateReadAclsSql() {
        return "EXEC dbo.nx_update_read_acls";
    }

    @Override
    public String getRebuildReadAclsSql() {
        return "EXEC dbo.nx_rebuild_read_acls";
    }

    @Override
    public List<String> getStartupSqls(Model model, Database database) {
        if (aclOptimizationsEnabled) {
            log.info("Vacuuming tables used by optimized acls");
            return Collections.singletonList("EXEC nx_vacuum_read_acls");
        }
        return Collections.emptyList();
    }

    @Override
    public boolean isClusteringSupported() {
        return true;
    }

    @Override
    public String getClusterInsertInvalidations() {
        return "EXEC dbo.NX_CLUSTER_INVAL ?, ?, ?, ?";
    }

    @Override
    public String getClusterGetInvalidations() {
        return "SELECT [id], [fragments], [kind] FROM [cluster_invals] WHERE [nodeid] = ?";
    }

    @Override
    public boolean isConcurrentUpdateException(Throwable t) {
        while (t.getCause() != null) {
            t = t.getCause();
        }
        if (t instanceof SQLException) {
            switch (((SQLException) t).getErrorCode()) {
            case 547: // The INSERT statement conflicted with the FOREIGN KEY
                      // constraint ...
            case 1205: // Transaction (Process ID ...) was deadlocked on ...
                       // resources with another process and has been chosen as
                       // the deadlock victim. Rerun the transaction
            case 2627: // Violation of UNIQUE KEY constraint
                       // Violation of PRIMARY KEY constraint
                return true;
            }
        }
        return false;
    }

    @Override
    public String getBlobLengthFunction() {
        return "DATALENGTH";
    }

    public String getUsersSeparator() {
        if (usersSeparator == null) {
            return DEFAULT_USERS_SEPARATOR;
        }
        return usersSeparator;
    }

    @Override
    public Serializable getGeneratedId(Connection connection) throws SQLException {
        if (idType != DialectIdType.SEQUENCE) {
            return super.getGeneratedId(connection);
        }
        String sql = String.format("SELECT NEXT VALUE FOR [%s]", idSequenceName);
        try (Statement s = connection.createStatement()) {
            ResultSet rs = s.executeQuery(sql);
            rs.next();
            return Long.valueOf(rs.getLong(1));
        }
    }

    /**
     * Set transaction isolation level to snapshot
     */
    @Override
    public void performPostOpenStatements(Connection connection) throws SQLException {
        try (Statement stmt = connection.createStatement()) {
            stmt.execute("SET TRANSACTION ISOLATION LEVEL READ COMMITTED");
        }
    }

    @Override
    public String getAncestorsIdsSql() {
        return "SELECT id FROM dbo.NX_ANCESTORS(?)";
    }

    @Override
    public String getDateCast() {
        if (majorVersion <= 9) {
            // SQL Server 2005 doesn't have a DATE type. At all. Sigh.
            // Style 112 is YYYYMMDD
            return "CONVERT(DATETIME, CONVERT(VARCHAR, %s, 112), 112)";
        }
        return super.getDateCast();
    }

    @Override
    public String castIdToVarchar(String expr) {
        switch (idType) {
        case VARCHAR:
            return expr;
        case SEQUENCE:
            return "CONVERT(VARCHAR, " + expr + ")";
        default:
            throw new AssertionError("Unknown id type: " + idType);
        }
    }

    @Override
    public DialectIdType getIdType() {
        return idType;
    }

    @Override
    public List<String> getIgnoredColumns(Table table) {
        return Collections.singletonList(CLUSTER_INDEX_COL);
    }

    /**
     * Tables created for directories don't need a clustered column automatically defined.
     */
    protected boolean needsClusteredColumn(Table table) {
        if (idType == DialectIdType.SEQUENCE) {
            // good enough for a clustered index
            // no need to add another column
            return false;
        }
        for (Column col : table.getColumns()) {
            if (col.getType().isId()) {
                return true;
            }
        }
        return false;
    }

    @Override
    public String getCustomColumnDefinition(Table table) {
        if (!needsClusteredColumn(table)) {
            return null;
        }
        return String.format("[%s] INT NOT NULL IDENTITY", CLUSTER_INDEX_COL);
    }

    @Override
    public List<String> getCustomPostCreateSqls(Table table) {
        if (!needsClusteredColumn(table)) {
            return Collections.emptyList();
        }
        String quotedIndexName = getIndexName(table.getKey(), Collections.singletonList(CLUSTER_INDEX_COL));
        String sql = String.format("CREATE UNIQUE CLUSTERED INDEX [%s] ON %s ([%s])", quotedIndexName,
                table.getQuotedName(), CLUSTER_INDEX_COL);
        return Collections.singletonList(sql);
    }

    @Override
    public String getSoftDeleteSql() {
        return "EXEC dbo.NX_DELETE ?, ?";
    }

    @Override
    public String getSoftDeleteCleanupSql() {
        return "{?= call dbo.NX_DELETE_PURGE(?, ?)}";
    }

    @Override
    public List<String> checkStoredProcedure(String procName, String procCreate, String ddlMode, Connection connection,
            JDBCLogger logger, Map<String, Serializable> properties) throws SQLException {
        boolean compatCheck = ddlMode.contains(RepositoryDescriptor.DDL_MODE_COMPAT);
        String procCreateLower = procCreate.toLowerCase();
        String procDrop;
        if (procCreateLower.startsWith("create function ")) {
            procDrop = "DROP FUNCTION " + procName;
        } else if (procCreateLower.startsWith("create procedure ")) {
            procDrop = "DROP PROCEDURE " + procName;
        } else {
            procDrop = "DROP TRIGGER " + procName;
        }
        if (compatCheck) {
            procDrop = "IF OBJECT_ID('" + procName + "') IS NOT NULL " + procDrop;
            return Arrays.asList(procDrop, procCreate);
        }
        try (Statement st = connection.createStatement()) {
            String getBody = "SELECT OBJECT_DEFINITION(OBJECT_ID('" + procName + "'))";
            logger.log(getBody);
            try (ResultSet rs = st.executeQuery(getBody)) {
                rs.next();
                String body = rs.getString(1);
                if (body == null) {
                    logger.log("  -> missing");
                    return Collections.singletonList(procCreate);
                } else if (normalizeString(procCreate).contains(normalizeString(body))) {
                    logger.log("  -> exists, unchanged");
                    return Collections.emptyList();
                } else {
                    logger.log("  -> exists, old");
                    return Arrays.asList(procDrop, procCreate);
                }
            }
        }
    }

    protected static String normalizeString(String string) {
        return string.replaceAll("[ \n\r\t]+", " ").trim();
    }

    @Override
    public String getSQLForDump(String sql) {
        return sql + "\nGO";
    }

}
