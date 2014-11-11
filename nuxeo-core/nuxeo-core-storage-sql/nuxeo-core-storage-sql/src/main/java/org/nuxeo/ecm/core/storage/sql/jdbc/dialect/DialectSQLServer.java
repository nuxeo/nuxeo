/*
 * Copyright (c) 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Florent Guillaume
 *     Benoit Delbosc
 */

package org.nuxeo.ecm.core.storage.sql.jdbc.dialect;

import java.io.Serializable;
import java.net.SocketException;
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
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.common.utils.StringUtils;
import org.nuxeo.ecm.core.NXCore;
import org.nuxeo.ecm.core.api.security.SecurityConstants;
import org.nuxeo.ecm.core.storage.StorageException;
import org.nuxeo.ecm.core.storage.sql.BinaryManager;
import org.nuxeo.ecm.core.storage.sql.ColumnType;
import org.nuxeo.ecm.core.storage.sql.Model;
import org.nuxeo.ecm.core.storage.sql.RepositoryDescriptor;
import org.nuxeo.ecm.core.storage.sql.jdbc.db.Column;
import org.nuxeo.ecm.core.storage.sql.jdbc.db.Database;
import org.nuxeo.ecm.core.storage.sql.jdbc.db.Join;
import org.nuxeo.ecm.core.storage.sql.jdbc.db.Table;
import org.nuxeo.ecm.core.storage.sql.jdbc.db.Table.IndexType;

/**
 * Microsoft SQL Server-specific dialect.
 *
 * @author Florent Guillaume
 */
public class DialectSQLServer extends Dialect {

    private static final Log log = LogFactory.getLog(DialectSQLServer.class);

    private static final String DEFAULT_FULLTEXT_ANALYZER = "english";

    private static final String DEFAULT_FULLTEXT_CATALOG = "nuxeo";

    private static final String CLUSTERED = "CLUSTERED";

    protected final String fulltextAnalyzer;

    protected final String fulltextCatalog;

    private static final String DEFAULT_USERS_SEPARATOR = "|";

    protected final String usersSeparator;

    protected boolean pathOptimizationsEnabled;

    /** 9 = SQL Server 2005, 10 = SQL Server 2008, 11 = SQL Server 2012 / Azure */
    protected int majorVersion;

    // http://msdn.microsoft.com/en-us/library/ms174396.aspx
    /** 5 = Azure */
    protected int engineEdition;

    protected boolean azure;

    public DialectSQLServer(DatabaseMetaData metadata,
            BinaryManager binaryManager,
            RepositoryDescriptor repositoryDescriptor) throws StorageException {
        super(metadata, binaryManager, repositoryDescriptor);
        try {
            checkDatabaseConfiguration(metadata.getConnection());
            majorVersion = metadata.getDatabaseMajorVersion();
            engineEdition = getEngineEdition(metadata.getConnection());

        } catch (SQLException e) {
            throw new StorageException(e);
        }
        if (engineEdition == 5) { // 5 = SQL Azure
            azure = true;
            fulltextDisabled = true;
            if (repositoryDescriptor != null) {
                repositoryDescriptor.fulltextDisabled = true;
            }
        }
        fulltextAnalyzer = repositoryDescriptor == null ? null
                : repositoryDescriptor.fulltextAnalyzer == null ? DEFAULT_FULLTEXT_ANALYZER
                        : repositoryDescriptor.fulltextAnalyzer;
        fulltextCatalog = repositoryDescriptor == null ? null
                : repositoryDescriptor.fulltextCatalog == null ? DEFAULT_FULLTEXT_CATALOG
                        : repositoryDescriptor.fulltextCatalog;
        usersSeparator = repositoryDescriptor == null ? null
                : repositoryDescriptor.usersSeparatorKey == null ? DEFAULT_USERS_SEPARATOR
                        : repositoryDescriptor.usersSeparatorKey;
        pathOptimizationsEnabled = repositoryDescriptor == null ? false
                : repositoryDescriptor.pathOptimizationsEnabled;
    }

    protected int getEngineEdition(Connection connection)
            throws SQLException {
        Statement st = connection.createStatement();
        try {
            ResultSet rs = st.executeQuery("SELECT CONVERT(NVARCHAR(100), SERVERPROPERTY('EngineEdition'))");
            rs.next();
            return rs.getInt(1);
        } finally {
            st.close();
        }
    }

    protected void checkDatabaseConfiguration(Connection connection)
            throws SQLException {
        Statement stmt = connection.createStatement();
        try {
            String sql = "SELECT is_read_committed_snapshot_on FROM sys.databases WHERE name = db_name()";
            if (log.isTraceEnabled()) {
                log.trace("SQL: " + sql);
            }
            ResultSet rs = stmt.executeQuery(sql);
            if (!rs.next()) {
                throw new SQLException(
                        "Cannot detect whether READ_COMMITTED_SNAPSHOT is on");
            }
            int on = rs.getInt(1);
            if (on != 1) {
                throw new SQLException(
                        "Incorrect database configuration, you must enable READ_COMMITTED_SNAPSHOT");
            }
            rs.close();
        } finally {
            stmt.close();
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
    public String getNoColumnsInsertString() {
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
            return jdbcInfo("DATETIME", Types.TIMESTAMP);
        case BLOBID:
            return jdbcInfo("VARCHAR(40)", Types.VARCHAR);
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
            return jdbcInfo("VARCHAR(256)", Types.VARCHAR);
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
            return jdbcInfo("VARCHAR(8000)", Types.VARCHAR);
        }
        throw new AssertionError(type);
    }

    @Override
    public boolean isAllowedConversion(int expected, int actual,
            String actualName, int actualSize) {
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
        case Types.TINYINT:
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
    public String getCreateIndexPrefixSql(IndexType indexType,
            List<Column> columns) {
        if (!azure) {
            return "";
        }
        if (indexType == IndexType.MAIN_NON_PRIMARY) {
            return CLUSTERED;
        }
        if (columns.size() == 1 && columns.get(0).getKey().equals("id")) {
            // creates index on id for collection tables
            // as there is no primary key, create a clustered index for this one
            return CLUSTERED;
        }
        return "";
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
    public String getCreateFulltextIndexSql(String indexName,
            String quotedIndexName, Table table, List<Column> columns,
            Model model) {
        StringBuilder buf = new StringBuilder();
        buf.append(String.format("CREATE FULLTEXT INDEX ON %s (",
                table.getQuotedName()));
        Iterator<Column> it = columns.iterator();
        while (it.hasNext()) {
            buf.append(String.format("%s LANGUAGE %s",
                    it.next().getQuotedName(), getQuotedFulltextAnalyzer()));
            if (it.hasNext()) {
                buf.append(", ");
            }
        }
        String fulltextUniqueIndex = "[fulltext_pk]";
        buf.append(String.format(") KEY INDEX %s ON [%s]", fulltextUniqueIndex,
                fulltextCatalog));
        return buf.toString();
    }

    @Override
    public String getDialectFulltextQuery(String query) {
        query = query.replace("%", "*");
        FulltextQuery ft = analyzeFulltextQuery(query);
        if (ft == null) {
            return "DONTMATCHANYTHINGFOREMPTYQUERY";
        }
        return translateFulltext(ft, "OR", "AND", "AND NOT", "\"", "\"",
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
    public FulltextMatchInfo getFulltextScoredMatchInfo(String fulltextQuery,
            String indexName, int nthMatch, Column mainColumn, Model model,
            Database database) {
        // TODO multiple indexes
        Table ft = database.getTable(model.FULLTEXT_TABLE_NAME);
        Column ftMain = ft.getColumn(model.MAIN_KEY);
        String nthSuffix = nthMatch == 1 ? "" : String.valueOf(nthMatch);
        String tableAlias = "_nxfttbl" + nthSuffix;
        FulltextMatchInfo info = new FulltextMatchInfo();
        // there are two left joins here
        info.joins = new ArrayList<Join>();
        if (nthMatch == 1) {
            // Need only one JOIN involving the fulltext table
            info.joins.add(new Join(Join.LEFT, ft.getQuotedName(), null, null,
                    ftMain.getFullQuotedName(), mainColumn.getFullQuotedName()));
        }
        info.joins.add(new Join(
                Join.LEFT, //
                String.format("CONTAINSTABLE(%s, *, ?, LANGUAGE %s)",
                        ft.getQuotedName(), getQuotedFulltextAnalyzer()),
                tableAlias, // alias
                fulltextQuery, // param
                ftMain.getFullQuotedName(), // on1
                String.format("%s.[KEY]", tableAlias) // on2
        ));
        info.whereExpr = String.format("%s.[KEY] IS NOT NULL", tableAlias);
        info.scoreExpr = String.format("(%s.RANK / 1000.0)", tableAlias);
        info.scoreAlias = "_nxscore" + nthSuffix;
        info.scoreCol = new Column(mainColumn.getTable(), null,
                ColumnType.DOUBLE, null);
        return info;
    }

    protected String getQuotedFulltextAnalyzer() {
        if (!Character.isDigit(fulltextAnalyzer.charAt(0))) {
            return String.format("'%s'", fulltextAnalyzer);
        }
        return fulltextAnalyzer;
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
        return String.format("dbo.NX_ACCESS_ALLOWED(%s, ?, ?) = 1",
                idColumnName);
    }

    @Override
    public String getInTreeSql(String idColumnName) {
        if (pathOptimizationsEnabled) {
            return String.format(
                    "EXISTS(SELECT 1 FROM ancestors WHERE hierarchy_id = %s AND ancestor = ?)",
                    idColumnName);
        }
        return String.format("dbo.NX_IN_TREE(%s, ?) = 1", idColumnName);
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
    public Map<String, Serializable> getSQLStatementsProperties(Model model,
            Database database) {
        Map<String, Serializable> properties = new HashMap<String, Serializable>();
        properties.put("idType", "VARCHAR(36)");
        properties.put("clusteredIndex", azure ? CLUSTERED : "");
        properties.put("md5HashString", getMd5HashString());
        properties.put("reseedAclrModified", azure ? ""
                : "DBCC CHECKIDENT('aclr_modified', RESEED, 0);");
        properties.put("fulltextEnabled", Boolean.valueOf(!fulltextDisabled));
        properties.put("fulltextCatalog", fulltextCatalog);
        properties.put("aclOptimizationsEnabled",
                Boolean.valueOf(aclOptimizationsEnabled));
        properties.put("pathOptimizationsEnabled",
                Boolean.valueOf(pathOptimizationsEnabled));
        properties.put("clusteringEnabled", Boolean.valueOf(clusteringEnabled));
        String[] permissions = NXCore.getSecurityService().getPermissionsToCheck(
                SecurityConstants.BROWSE);
        List<String> permsList = new LinkedList<String>();
        for (String perm : permissions) {
            permsList.add(String.format("  SELECT '%s' ", perm));
        }
        properties.put("readPermissions", StringUtils.join(permsList,
                " UNION ALL "));
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

    @Override
    public boolean supportsReadAcl() {
        return aclOptimizationsEnabled;
    }

    @Override
    public String getReadAclsCheckSql(String idColumnName) {
        return String.format(
                "%s IN (SELECT acl_id FROM dbo.nx_get_read_acls_for(?))",
                idColumnName);
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
    public boolean isClusteringSupported() {
        return true;
    }

    @Override
    public String getClusterInsertInvalidations() {
        return "EXEC dbo.NX_CLUSTER_INVAL ?, ?, ?";
    }

    @Override
    public String getClusterGetInvalidations() {
        return "DELETE I OUTPUT DELETED.[id], DELETED.[fragments], DELETED.[kind] "
                + "FROM [cluster_invals] AS I WHERE I.[nodeid] = @@SPID";
    }

    @Override
    public boolean isConnectionClosedException(Throwable t) {
        while (t.getCause() != null) {
            t = t.getCause();
        }
        if (t instanceof SocketException) {
            return true;
        }
        // java.sql.SQLException: Invalid state, the Connection object is
        // closed.
        String message = t.getMessage();
        if (message != null
                && message.contains("the Connection object is closed")) {
            return true;
        }
        return false;
    }

    @Override
    public String getBlobLengthFunction() {
        return "DATALENGTH";
    }

    @Override
    public String getPrepareUserReadAclsSql() {
        return "EXEC nx_prepare_user_read_acls ?";
    }

    @Override
    public boolean needsPrepareUserReadAcls() {
        return true;
    }

    public String getUsersSeparator() {
        if (usersSeparator == null) {
            return DEFAULT_USERS_SEPARATOR;
        }
        return usersSeparator;
    }

    /**
     * Set transaction isolation level to snapshot
     *
     */
    @Override
    public void performPostOpenStatements(Connection connection)
            throws SQLException {
        Statement stmt = connection.createStatement();
        try {
            stmt.execute("SET TRANSACTION ISOLATION LEVEL READ COMMITTED");
        } finally {
            stmt.close();
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

}
