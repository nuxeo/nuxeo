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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.GregorianCalendar;
import java.util.LinkedList;
import java.util.List;

import org.nuxeo.common.utils.StringUtils;
import org.nuxeo.ecm.core.storage.StorageException;
import org.nuxeo.ecm.core.storage.sql.Binary;
import org.nuxeo.ecm.core.storage.sql.Model;
import org.nuxeo.ecm.core.storage.sql.RepositoryDescriptor;
import org.nuxeo.ecm.core.storage.sql.RepositoryDescriptor.IdGenPolicy;
import org.nuxeo.ecm.core.storage.sql.db.Column;
import org.nuxeo.ecm.core.storage.sql.db.ColumnType;
import org.nuxeo.ecm.core.storage.sql.db.Database;
import org.nuxeo.ecm.core.storage.sql.db.Table;

/**
 * H2-specific dialect.
 *
 * @author Florent Guillaume
 */
public class DialectH2 extends Dialect {

    private static final String DEFAULT_FULLTEXT_ANALYZER = "org.apache.lucene.analysis.standard.StandardAnalyzer";

    public DialectH2(DatabaseMetaData metadata,
            RepositoryDescriptor repositoryDescriptor) throws StorageException {
        super(metadata, repositoryDescriptor);
    }

    @Override
    public boolean supportsIfExistsAfterTableName() {
        return true;
    }

    @Override
    public JDBCInfo getJDBCTypeAndString(ColumnType type) {
        switch (type) {
        case VARCHAR:
            return jdbcInfo("VARCHAR", Types.VARCHAR);
        case CLOB:
            return jdbcInfo("CLOB", Types.CLOB);
        case BOOLEAN:
            return jdbcInfo("BOOLEAN", Types.BOOLEAN);
        case LONG:
            return jdbcInfo("BIGINT", Types.BIGINT);
        case DOUBLE:
            return jdbcInfo("DOUBLE", Types.DOUBLE);
        case TIMESTAMP:
            return jdbcInfo("TIMESTAMP", Types.TIMESTAMP);
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
            return jdbcInfo("VARCHAR(250)", Types.VARCHAR);
        case TINYINT:
            return jdbcInfo("TINYINT", Types.TINYINT);
        case INTEGER:
            return jdbcInfo("INTEGER", Types.INTEGER);
        case FTINDEXED:
            throw new AssertionError(type);
        case FTSTORED:
            return jdbcInfo("CLOB", Types.CLOB);
        case CLUSTERNODE:
            return jdbcInfo("INTEGER", Types.INTEGER);
        case CLUSTERFRAGS:
            return jdbcInfo("VARCHAR", Types.VARCHAR);
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
        case Types.BOOLEAN:
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
        case Types.BOOLEAN:
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
            columnNames.add("'" + col.getPhysicalName() + "'");
        }
        String fullIndexName = String.format("PUBLIC_%s_%s", table.getName(),
                indexName);
        String analyzer = model.getFulltextInfo().indexAnalyzer.get(indexName);
        if (analyzer == null) {
            analyzer = DEFAULT_FULLTEXT_ANALYZER;
        }
        return String.format(
                "CALL NXFT_CREATE_INDEX('%s', 'PUBLIC', '%s', (%s), '%s')",
                fullIndexName, table.getName(), StringUtils.join(columnNames,
                        ", "), analyzer);
    }

    @Override
    // translate into Lucene-based syntax
    public String getDialectFulltextQuery(String query) {
        query = query.replaceAll(" +", " ");
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

    @Override
    public String[] getFulltextMatch(String indexName, String fulltextQuery,
            Column mainColumn, Model model, Database database) {
        String phftname = database.getTable(model.FULLTEXT_TABLE_NAME).getName(); // physical
        String fullIndexName = "PUBLIC_" + phftname + "_" + indexName;
        String queryTable = String.format(
                "NXFT_SEARCH('%s', ?) %%s ON %s = %%<s.KEY", fullIndexName,
                mainColumn.getFullQuotedName());
        String whereExpr = "%s.KEY IS NOT NULL";
        return new String[] { queryTable, fulltextQuery, whereExpr, null };
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
    public boolean supportsUpdateFrom() {
        return false; // check this, unused
    }

    @Override
    public boolean doesUpdateFromRepeatSelf() {
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
    public String getClobCast(boolean inOrderBy) {
        if (!inOrderBy) {
            return "CAST(%s AS VARCHAR)";
        }
        return null;
    }

    @Override
    public String getSecurityCheckSql(String idColumnName) {
        return String.format("NX_ACCESS_ALLOWED(%s, ?, ?)", idColumnName);
    }

    @Override
    public boolean supportsDescendantsTable() {
        return true;
    }

    @Override
    public String getInTreeSql(String idColumnName) {
        return String.format(
                "EXISTS(SELECT 1 FROM DESCENDANTS WHERE ID = ? AND DESCENDANTID = %s)",
                idColumnName);
        // return String.format("NX_IN_TREE(%s, ?)", idColumnName);
    }

    @Override
    public boolean isFulltextTableNeeded() {
        return false;
    }

    @Override
    public boolean supportsArrays() {
        return false;
    }

    private static final String h2Functions = "org.nuxeo.ecm.core.storage.sql.db.H2Functions";

    private static final String h2Fulltext = "org.nuxeo.ecm.core.storage.sql.db.H2Fulltext";

    private static final String h2TrigDesc = "org.nuxeo.ecm.core.storage.sql.db.H2TriggerDescendants";

    @Override
    public Collection<ConditionalStatement> getConditionalStatements(
            Model model, Database database) {
        assert model.idGenPolicy == IdGenPolicy.APP_UUID;
        Table ht = database.getTable(model.hierTableName);

        List<ConditionalStatement> statements = new LinkedList<ConditionalStatement>();

        statements.add(makeFunction("NX_IN_TREE", //
                "isInTreeString"));

        statements.add(makeFunction("NX_ACCESS_ALLOWED", //
                "isAccessAllowedString"));

        statements.add(makeFunction("NX_CLUSTER_INVAL", //
                "clusterInvalidateString"));

        statements.add(makeFunction("NX_CLUSTER_GET_INVALS", //
                "getClusterInvalidationsString"));

        // read acls ----------------------------------------------------------
        // table to store canonical read acls
        statements.add(new ConditionalStatement( //
                false, // late
                Boolean.FALSE, // no drop
                null, //
                null, //
                "CREATE TABLE IF NOT EXISTS read_acls (" //
                        + "  id character varying(4096) PRIMARY KEY," //
                        + "  acl character varying(4096));")); //
        // table to maintain a read acl for each hierarchy entry
        statements.add(new ConditionalStatement(
                false, // late
                Boolean.FALSE, // no drop
                null, //
                null, //
                "CREATE TABLE IF NOT EXISTS hierarchy_read_acl (" //
                        + "  id character varying(36) PRIMARY KEY," //
                        + "  acl_id character varying(4096)," //
                        + "  CONSTRAINT hierarchy_read_acl_id_fk FOREIGN KEY (id) REFERENCES hierarchy(id) ON DELETE CASCADE" //
                        + ");"));
        // Add index
        statements.add(new ConditionalStatement(
                false, // late
                Boolean.FALSE, // no drop
                null, //
                null, //
                "CREATE INDEX IF NOT EXISTS hierarchy_read_acl_acl_id_idx ON hierarchy_read_acl(acl_id);"));
        // Log hierarchy with updated read acl
        statements.add(new ConditionalStatement(false, // late
                Boolean.FALSE, // no drop
                null, //
                null, //
                "CREATE TABLE IF NOT EXISTS hierarchy_modified_acl ("
                        + "  id character varying(36)," //
                        + "  is_new boolean" //
                        + ");"));
        statements.add(makeFunction("nx_get_read_acl", //
                "getReadAcl"));
        statements.add(makeFunction("nx_get_read_acls_for", //
                "getReadAclsFor"));
        statements.add(makeFunction("nx_rebuild_read_acls", //
                "rebuildReadAcls"));
        statements.add(makeFunction("nx_update_read_acls", //
                "updateReadAcls"));
        statements.add(new ConditionalStatement(
                false, // late
                Boolean.TRUE, // do a drop
                null, //
                "DROP TRIGGER IF EXISTS nx_trig_acls_modified;",
                "CREATE TRIGGER nx_trig_acls_modified\n" //
                        + "  AFTER INSERT, UPDATE, DELETE ON acls\n" //
                        + "  FOR EACH ROW CALL \""
                        + h2Functions
                        + "$LogAclsModified\";"));
        statements.add(new ConditionalStatement(
                false, // late
                Boolean.TRUE, // do a drop
                null, //
                "DROP TRIGGER IF EXISTS nx_trig_hierarchy_modified;",
                "CREATE TRIGGER nx_trig_hierarchy_modified\n" //
                        + "  AFTER INSERT, UPDATE ON hierarchy\n" //
                        + "  FOR EACH ROW CALL \""
                        + h2Functions
                        + "$LogHierarchyModified\";"));
        // build the read acls if empty, this takes care of the upgrade
        statements.add(new ConditionalStatement(
                false, // late
                null, // perform a check
                "SELECT 1 WHERE NOT EXISTS(SELECT 1 FROM read_acls LIMIT 1);",
                "SELECT * FROM nx_rebuild_read_acls();", //
                "SELECT 1;"));

        statements.add(makeFunction("NX_INIT_DESCENDANTS", //
                "initDescendants"));

        if (!fulltextDisabled) {
            statements.add(new ConditionalStatement( //
                    true, // early
                    Boolean.FALSE, // no drop
                    null, //
                    null, //
                    String.format(
                            "CREATE ALIAS IF NOT EXISTS NXFT_INIT FOR \"%s.init\"; "
                                    + "CALL NXFT_INIT()", h2Fulltext)));
        }

        statements.add(makeTrigger("NX_TRIG_DESC", ht.getQuotedName(),
                h2TrigDesc));

        return statements;
    }

    @Override
    public List<String> getPostCreateTableSqls(Table table, Model model,
            Database database) {
        if (table.getName().equals(Model.DESCENDANTS_TABLE_NAME.toUpperCase())) {
            return Arrays.asList("CALL NX_INIT_DESCENDANTS()");
        }
        return Collections.emptyList();
    }

    private ConditionalStatement makeFunction(String functionName,
            String methodName) {
        return new ConditionalStatement( //
                true, // early
                Boolean.TRUE, // always drop
                null, //
                String.format("DROP ALIAS IF EXISTS %s", functionName), //
                String.format("CREATE ALIAS %s FOR \"%s.%s\"", functionName,
                        h2Functions, methodName));
    }

    private ConditionalStatement makeTrigger(String triggerName,
            String tableName, String className) {
        return new ConditionalStatement(
                false, // late
                Boolean.TRUE, // always drop
                null, //
                String.format("DROP TRIGGER IF EXISTS %s", triggerName),
                String.format("CREATE TRIGGER %s "
                        + "AFTER INSERT, UPDATE, DELETE ON %s "
                        + "FOR EACH ROW CALL \"%s\"", triggerName, tableName,
                        className));
    }

    @Override
    public boolean isClusteringSupported() {
        return true;
    }

    @Override
    public String getCleanupClusterNodesSql(Model model, Database database) {
        Table cln = database.getTable(model.CLUSTER_NODES_TABLE_NAME);
        Column clnid = cln.getColumn(model.CLUSTER_NODES_NODEID_KEY);
        // delete nodes for sessions don't exist anymore, and old node for this
        // session (session ids are recycled)
        return String.format(
                "DELETE FROM %s C WHERE "
                        + "NOT EXISTS(SELECT * FROM INFORMATION_SCHEMA.SESSIONS S WHERE C.%s = S.ID) "
                        + "OR C.%<s = SESSION_ID()", cln.getQuotedName(),
                clnid.getQuotedName());
    }

    @Override
    public String getCreateClusterNodeSql(Model model, Database database) {
        Table cln = database.getTable(model.CLUSTER_NODES_TABLE_NAME);
        Column clnid = cln.getColumn(model.CLUSTER_NODES_NODEID_KEY);
        Column clncr = cln.getColumn(model.CLUSTER_NODES_CREATED_KEY);
        return String.format(
                "INSERT INTO %s (%s, %s) VALUES (SESSION_ID(), CURRENT_TIMESTAMP)",
                cln.getQuotedName(), clnid.getQuotedName(),
                clncr.getQuotedName());
    }

    @Override
    public String getRemoveClusterNodeSql(Model model, Database database) {
        Table cln = database.getTable(model.CLUSTER_NODES_TABLE_NAME);
        Column clnid = cln.getColumn(model.CLUSTER_NODES_NODEID_KEY);
        return String.format("DELETE FROM %s WHERE %s = SESSION_ID()",
                cln.getQuotedName(), clnid.getQuotedName());
    }

    @Override
    public String getClusterInsertInvalidations() {
        return "CALL NX_CLUSTER_INVAL(?, ?, ?)";
    }

    @Override
    public String getClusterGetInvalidations() {
        return "SELECT * FROM NX_CLUSTER_GET_INVALS()";
    }

    @Override
    public Collection<ConditionalStatement> getTestConditionalStatements(
            Model model, Database database) {
        List<ConditionalStatement> statements = new LinkedList<ConditionalStatement>();
        statements.add(new ConditionalStatement(true, Boolean.FALSE, null,
                null,
                // here use a CLOB instead of a VARCHAR to test compatibility
                "CREATE TABLE TESTSCHEMA2 (ID VARCHAR(36) NOT NULL, TITLE CLOB)"));
        statements.add(new ConditionalStatement(true, Boolean.FALSE, null,
                null,
                "ALTER TABLE TESTSCHEMA2 ADD CONSTRAINT TESTSCHEMA2_PK PRIMARY KEY (ID)"));
        return statements;
    }

}
