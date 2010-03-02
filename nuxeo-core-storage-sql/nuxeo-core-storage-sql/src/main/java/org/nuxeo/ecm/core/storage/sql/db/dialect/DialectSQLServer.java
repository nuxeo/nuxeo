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

package org.nuxeo.ecm.core.storage.sql.db.dialect;

import java.io.Serializable;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.Calendar;
import java.util.Collection;
import java.util.GregorianCalendar;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.nuxeo.common.utils.StringUtils;
import org.nuxeo.ecm.core.storage.StorageException;
import org.nuxeo.ecm.core.storage.sql.Binary;
import org.nuxeo.ecm.core.storage.sql.Model;
import org.nuxeo.ecm.core.storage.sql.RepositoryDescriptor;
import org.nuxeo.ecm.core.storage.sql.db.Column;
import org.nuxeo.ecm.core.storage.sql.db.ColumnType;
import org.nuxeo.ecm.core.storage.sql.db.Database;
import org.nuxeo.ecm.core.storage.sql.db.Table;

/**
 * Microsoft SQL Server-specific dialect.
 *
 * @author Florent Guillaume
 */
public class DialectSQLServer extends Dialect {

    private static final String DEFAULT_FULLTEXT_ANALYZER = "english";

    private static final String DEFAULT_FULLTEXT_CATALOG = "nuxeo";

    protected final String fulltextAnalyzer;

    protected final String fulltextCatalog;

    public DialectSQLServer(DatabaseMetaData metadata,
            RepositoryDescriptor repositoryDescriptor) throws StorageException {
        super(metadata, repositoryDescriptor);
        fulltextAnalyzer = repositoryDescriptor.fulltextAnalyzer == null ? DEFAULT_FULLTEXT_ANALYZER
                : repositoryDescriptor.fulltextAnalyzer;
        fulltextCatalog = repositoryDescriptor.fulltextCatalog == null ? DEFAULT_FULLTEXT_CATALOG
                : repositoryDescriptor.fulltextCatalog;

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
        switch (type) {
        case VARCHAR:
            return jdbcInfo("NVARCHAR(4000)", Types.VARCHAR);
        case CLOB:
            return jdbcInfo("NVARCHAR(MAX)", Types.CLOB);
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
        case NODEVAL:
            return jdbcInfo("VARCHAR(36)", Types.VARCHAR);
        case SYSNAME:
            return jdbcInfo("VARCHAR(256)", Types.VARCHAR);
        case TINYINT:
            return jdbcInfo("TINYINT", Types.TINYINT);
        case INTEGER:
            return jdbcInfo("INT", Types.INTEGER);
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
        case Types.CLOB:
            String string = rs.getString(index);
            if (column.getType() == ColumnType.BLOBID && string != null) {
                return column.getModel().getBinary(string);
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
    public boolean getMaterializeFulltextSyntheticColumn() {
        return false;
    }

    @Override
    public int getFulltextIndexedColumns() {
        return 2;
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
        query = query.replaceAll(" +", " ");
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
            res += " &! " + StringUtils.join(neg, " &! ");
        }
        return res;
    }

    @Override
    public String[] getFulltextMatch(String name, String fulltextQuery,
            Column mainColumn, Model model, Database database) {
        String whereExpr = String.format(
                "CONTAINS([fulltext].*, ?, LANGUAGE %s)",
                getQuotedFulltextAnalyzer());
        return new String[] { null, null, whereExpr, fulltextQuery };
    }

    protected String getQuotedFulltextAnalyzer() {
        if (!Character.isDigit(fulltextAnalyzer.charAt(0))) {
            return String.format("'%s'", fulltextAnalyzer);
        }
        return fulltextAnalyzer;
    }

    @Override
    public boolean supportsCircularCascadeDeleteConstraints() {
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
    public String getSecurityCheckSql(String idColumnName) {
        return String.format("dbo.NX_ACCESS_ALLOWED(%s, ?, ?) = 1",
                idColumnName);
    }

    @Override
    public String getInTreeSql(String idColumnName) {
        return String.format("dbo.NX_IN_TREE(%s, ?) = 1", idColumnName);
    }

    @Override
    public Collection<ConditionalStatement> getConditionalStatements(
            Model model, Database database) {
        String idType;
        switch (model.idGenPolicy) {
        case APP_UUID:
            idType = "NVARCHAR(36)";
            break;
        case DB_IDENTITY:
            idType = "INTEGER";
            break;
        default:
            throw new AssertionError(model.idGenPolicy);
        }

        List<ConditionalStatement> statements = new LinkedList<ConditionalStatement>();

        statements.add(new ConditionalStatement(
                false, // late
                Boolean.TRUE, // always drop
                null, //
                "IF OBJECT_ID('dbo.nxTrigCascadeDelete', 'TR') IS NOT NULL DROP TRIGGER dbo.nxTrigCascadeDelete", //
                "CREATE TRIGGER nxTrigCascadeDelete ON [hierarchy] " //
                        + "INSTEAD OF DELETE AS " //
                        + "BEGIN" //
                        + "  SET NOCOUNT ON;" //
                        + "  WITH subtree(id, parentid) AS (" //
                        + "    SELECT id, parentid" //
                        + "    FROM deleted" //
                        + "  UNION ALL" //
                        + "    SELECT h.id, h.parentid" //
                        + "    FROM [hierarchy] h" //
                        + "    JOIN subtree ON subtree.id = h.parentid" //
                        + "  )" //
                        + "  DELETE FROM [hierarchy]" //
                        + "    FROM [hierarchy] h" //
                        + "    JOIN subtree" //
                        + "    ON subtree.id = h.id; " //
                        + "END" //
        ));

        statements.add(new ConditionalStatement(
                false, // late
                Boolean.TRUE, // always drop
                null, //
                "IF OBJECT_ID('dbo.NX_CLUSTER_INVAL', 'P') IS NOT NULL DROP PROCEDURE dbo.NX_CLUSTER_INVAL", //
                String.format(
                        "CREATE PROCEDURE NX_CLUSTER_INVAL(@i %s, @f VARCHAR(8000), @k TINYINT) " //
                                + "AS " //
                                + "BEGIN" //
                                + "  DECLARE @nid SMALLINT;" //
                                + "  DECLARE @cur CURSOR;" //
                                + "  SET @cur = CURSOR FAST_FORWARD FOR" //
                                + "    SELECT [nodeid] FROM [cluster_nodes] WHERE [nodeid] <> @@SPID;" //
                                + "  OPEN @cur;" //
                                + "  FETCH FROM @cur INTO @nid;" //
                                + "  WHILE @@FETCH_STATUS = 0 BEGIN" //
                                + "    INSERT INTO [cluster_invals] ([nodeid], [id], [fragments], [kind]) VALUES (@nid, @i, @f, @k) "
                                + "    FETCH FROM @cur INTO @nid;" //
                                + "  END;" //
                                + "  CLOSE @cur; " //
                                + "END" //
                        , idType)));

        statements.add(new ConditionalStatement(
                false, // late
                Boolean.TRUE, // always drop
                null, //
                "IF OBJECT_ID('dbo.NX_ACCESS_ALLOWED', 'FN') IS NOT NULL DROP FUNCTION dbo.NX_ACCESS_ALLOWED", //
                String.format(
                        "CREATE FUNCTION NX_ACCESS_ALLOWED" //
                                + "(@id %s, @users NVARCHAR(4000), @perms NVARCHAR(4000)) " //
                                + "RETURNS TINYINT AS " //
                                + "BEGIN" //
                                + "  DECLARE @allusers NVARCHAR(4000);" //
                                + "  DECLARE @allperms NVARCHAR(4000);" //
                                + "  DECLARE @first TINYINT;" //
                                + "  DECLARE @curid %<s;" //
                                + "  DECLARE @newid %<s;" //
                                + "  DECLARE @gr TINYINT;" //
                                + "  DECLARE @pe VARCHAR(1000);" //
                                + "  DECLARE @us VARCHAR(1000);" //
                                + "  SET @allusers = N'|' + @users + N'|';" //
                                + "  SET @allperms = N'|' + @perms + N'|';" //
                                + "  SET @first = 1;" //
                                + "  SET @curid = @id;" //
                                + "  WHILE @curid IS NOT NULL BEGIN" //
                                + "    DECLARE @cur CURSOR;" //
                                + "    SET @cur = CURSOR FAST_FORWARD FOR" //
                                + "      SELECT [grant], [permission], [user] FROM [acls]" //
                                + "      WHERE [id] = @curid ORDER BY [pos];" //
                                + "    OPEN @cur;" //
                                + "    FETCH FROM @cur INTO @gr, @pe, @us;" //
                                + "    WHILE @@FETCH_STATUS = 0 BEGIN" //
                                + "      IF @allusers LIKE (N'%%|' + @us + N'|%%')" //
                                + "        AND @allperms LIKE (N'%%|' + @pe + N'|%%')" //
                                + "      BEGIN" //
                                + "        CLOSE @cur;" //
                                + "        RETURN @gr;" //
                                + "      END;" //
                                + "      FETCH FROM @cur INTO @gr, @pe, @us;" //
                                + "    END;" //
                                + "    CLOSE @cur;" //
                                + "    SET @newid = (SELECT [parentid] FROM [hierarchy] WHERE [id] = @curid);" //
                                + "    IF @first = 1 AND @newid IS NULL BEGIN" //
                                + "      SET @newid = (SELECT [versionableid] FROM [versions] WHERE [id] = @curid);" //
                                + "    END;" //
                                + "    SET @first = 0;" //
                                + "    SET @curid = @newid;" //
                                + "  END;" //
                                + "  RETURN 0; " //
                                + "END" //
                        , idType)));

        statements.add(new ConditionalStatement(
                false, // late
                Boolean.TRUE, // always drop
                null, //
                "IF OBJECT_ID('dbo.NX_IN_TREE', 'FN') IS NOT NULL DROP FUNCTION dbo.NX_IN_TREE", //
                String.format(
                        "CREATE FUNCTION NX_IN_TREE(@id %s, @baseid %<s) " //
                                + "RETURNS TINYINT AS " //
                                + "BEGIN" //
                                + "  DECLARE @curid %<s;" //
                                + "  IF @baseid IS NULL OR @id IS NULL OR @baseid = @id RETURN 0;" //
                                + "  SET @curid = @id;" //
                                + "  WHILE @curid IS NOT NULL BEGIN" //
                                + "    SET @curid = (SELECT [parentid] FROM [hierarchy] WHERE [id] = @curid);" //
                                + "    IF @curid = @baseid RETURN 1;" //
                                + "  END;" //
                                + "  RETURN 0;" //
                                + "END" //
                        , idType)));

        if (!fulltextDisabled) {
            statements.add(new ConditionalStatement( //
                    true, // early
                    null, // do a check
                    // strange inverted condition because this is designed to
                    // test drops
                    String.format(
                            "IF EXISTS(SELECT name FROM sys.fulltext_catalogs WHERE name = '%s') "
                                    + "SELECT * FROM sys.tables WHERE 1 = 0 "
                                    + "ELSE SELECT 1", //
                            fulltextCatalog), //
                    String.format("CREATE FULLTEXT CATALOG [%s]",
                            fulltextCatalog), //
                    "SELECT 1"));
        }

        return statements;
    }

    @Override
    public Collection<ConditionalStatement> getTestConditionalStatements(
            Model model, Database database) {
        List<ConditionalStatement> statements = new LinkedList<ConditionalStatement>();
        statements.add(new ConditionalStatement(true, Boolean.FALSE, null,
                null,
                // here use a NVARCHAR(MAX) instead of a NVARCHAR(4000) to test
                // compatibility
                "CREATE TABLE TESTSCHEMA2 (ID VARCHAR(36) NOT NULL, TITLE NVARCHAR(MAX) NULL)"));
        statements.add(new ConditionalStatement(true, Boolean.FALSE, null,
                null,
                "ALTER TABLE TESTSCHEMA2 ADD CONSTRAINT TESTSCHEMA2_PK PRIMARY KEY (ID)"));
        return statements;
    }

    @Override
    public boolean isClusteringSupported() {
        return true;
    }

    @Override
    public String getCleanupClusterNodesSql(Model model, Database database) {
        Table cln = database.getTable(model.CLUSTER_NODES_TABLE_NAME);
        Column clnid = cln.getColumn(model.CLUSTER_NODES_NODEID_KEY);
        return String.format("DELETE FROM N FROM %s N WHERE"
                + "  HAS_PERMS_BY_NAME(null, null, 'VIEW SERVER STATE') = 1"
                + "  AND NOT EXISTS(" //
                + "    SELECT 1 FROM sys.dm_exec_sessions S WHERE" //
                + "      S.is_user_process = 1 AND N.%s = S.session_id)", //
                cln.getQuotedName(), clnid.getQuotedName());
    }

    @Override
    public String getCreateClusterNodeSql(Model model, Database database) {
        Table cln = database.getTable(model.CLUSTER_NODES_TABLE_NAME);
        Column clnid = cln.getColumn(model.CLUSTER_NODES_NODEID_KEY);
        Column clncr = cln.getColumn(model.CLUSTER_NODES_CREATED_KEY);
        return String.format(
                "INSERT INTO %s (%s, %s) VALUES (@@SPID, CURRENT_TIMESTAMP)",
                cln.getQuotedName(), clnid.getQuotedName(),
                clncr.getQuotedName());
    }

    @Override
    public String getRemoveClusterNodeSql(Model model, Database database) {
        Table cln = database.getTable(model.CLUSTER_NODES_TABLE_NAME);
        Column clnid = cln.getColumn(model.CLUSTER_NODES_NODEID_KEY);
        return String.format("DELETE FROM %s WHERE %s = @@SPID",
                cln.getQuotedName(), clnid.getQuotedName());
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

}
