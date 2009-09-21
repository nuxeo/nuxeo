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
import java.sql.Array;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.GregorianCalendar;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.hibernate.dialect.PostgreSQLDialect;
import org.nuxeo.common.utils.StringUtils;
import org.nuxeo.ecm.core.storage.StorageException;
import org.nuxeo.ecm.core.storage.sql.Binary;
import org.nuxeo.ecm.core.storage.sql.Model;
import org.nuxeo.ecm.core.storage.sql.RepositoryDescriptor;
import org.nuxeo.ecm.core.storage.sql.Model.FulltextInfo;
import org.nuxeo.ecm.core.storage.sql.db.Column;
import org.nuxeo.ecm.core.storage.sql.db.ColumnType;
import org.nuxeo.ecm.core.storage.sql.db.Database;
import org.nuxeo.ecm.core.storage.sql.db.Table;

/**
 * PostgreSQL-specific dialect.
 *
 * @author Florent Guillaume
 */
public class DialectPostgreSQL extends Dialect {

    private static final String DEFAULT_FULLTEXT_ANALYZER = "english";

    protected final String fulltextAnalyzer;

    public DialectPostgreSQL(DatabaseMetaData metadata,
            RepositoryDescriptor repositoryDescriptor) throws StorageException {
        super(new PostgreSQLDialect(), metadata);
        fulltextAnalyzer = repositoryDescriptor.fulltextAnalyzer == null ? DEFAULT_FULLTEXT_ANALYZER
                : repositoryDescriptor.fulltextAnalyzer;
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
            return jdbcInfo("varchar(32)", Types.VARCHAR);
            // -----
        case NODEID:
        case NODEIDFK:
        case NODEIDFKNP:
        case NODEIDFKMUL:
        case NODEIDFKNULL:
        case NODEVAL:
            return jdbcInfo("varchar(36)", Types.VARCHAR);
        case SYSNAME:
            return jdbcInfo("varchar(250)", Types.VARCHAR);
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
            String string = rs.getString(index);
            if (column.getType() == ColumnType.BLOBID && string != null) {
                return column.getModel().getBinary(string);
            } else {
                return string;
            }
        case Types.BIT:
            return rs.getBoolean(index);
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
        case Types.ARRAY:
            return (Serializable) rs.getArray(index).getArray();
        }
        throw new SQLException("Unhandled JDBC type: " + column.getJdbcType());
    }

    @Override
    public String getCreateFulltextIndexSql(String indexName,
            String quotedIndexName, String tableName, List<String> columnNames) {
        return String.format("CREATE INDEX %s ON %s USING GIN(%s)",
                quotedIndexName.toLowerCase(), tableName, columnNames.get(0));
    }

    @Override
    public String getDialectFulltextQuery(String query) {
        query = query.replace(" & ", " "); // PostgreSQL compatibility BBB
        query = query.replaceAll(" +", " ");
        List<String> res = new LinkedList<String>();
        for (String word : StringUtils.split(query, ' ', false)) {
            if (word.startsWith("-")) {
                res.add("!" + word.substring(1));
            } else {
                res.add(word);
            }
        }
        return StringUtils.join(res, " & ");
    }

    @Override
    public String[] getFulltextMatch(String indexName, String fulltextQuery,
            Column mainColumn, Model model, Database database) {
        // TODO multiple indexes
        String suffix = model.getFulltextIndexSuffix(indexName);
        Column ftColumn = database.getTable(model.FULLTEXT_TABLE_NAME).getColumn(
                model.FULLTEXT_FULLTEXT_KEY + suffix);
        String whereExpr = String.format("NX_CONTAINS(%s, ?)",
                ftColumn.getFullQuotedName());
        return new String[] { null, null, whereExpr, fulltextQuery };
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
    public String getSecurityCheckSql(String idColumnName) {
        return String.format("NX_ACCESS_ALLOWED(%s, ?, ?)", idColumnName);
    }

    @Override
    public String getInTreeSql(String idColumnName) {
        return String.format("NX_IN_TREE(%s, ?)", idColumnName);
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
        String typeName = dialect.getTypeName(type);
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

        public int getBaseType() {
            return type;
        }

        public String getBaseTypeName() {
            return typeName;
        }

        public Object getArray() {
            return elements;
        }

        public Object getArray(Map<String, Class<?>> map) throws SQLException {
            throw new SQLException(NOT_SUPPORTED);
        }

        public Object getArray(long index, int count) throws SQLException {
            throw new SQLException(NOT_SUPPORTED);
        }

        public Object getArray(long index, int count, Map<String, Class<?>> map)
                throws SQLException {
            throw new SQLException(NOT_SUPPORTED);
        }

        public ResultSet getResultSet() throws SQLException {
            throw new SQLException(NOT_SUPPORTED);
        }

        public ResultSet getResultSet(Map<String, Class<?>> map)
                throws SQLException {
            throw new SQLException(NOT_SUPPORTED);
        }

        public ResultSet getResultSet(long index, int count)
                throws SQLException {
            throw new SQLException(NOT_SUPPORTED);
        }

        public ResultSet getResultSet(long index, int count,
                Map<String, Class<?>> map) throws SQLException {
            throw new SQLException(NOT_SUPPORTED);
        }

        // this is needed by JDBC 4 (Java 6)
        public void free() {
        }
    }

    @Override
    public Collection<ConditionalStatement> getConditionalStatements(
            Model model, Database database) {
        String idType;
        switch (model.idGenPolicy) {
        case APP_UUID:
            idType = "varchar(36)";
            break;
        case DB_IDENTITY:
            idType = "integer";
            break;
        default:
            throw new AssertionError(model.idGenPolicy);
        }
        Table ft = database.getTable(model.FULLTEXT_TABLE_NAME);

        List<ConditionalStatement> statements = new LinkedList<ConditionalStatement>();

        statements.add(new ConditionalStatement(
                true, // early
                Boolean.FALSE, // no drop needed
                null, //
                null, //
                String.format(
                        "CREATE OR REPLACE FUNCTION NX_IN_TREE(id %s, baseid %<s) " //
                                + "RETURNS boolean " //
                                + "AS $$ " //
                                + "DECLARE" //
                                + "  curid %<s := id; " //
                                + "BEGIN" //
                                + "  IF baseid IS NULL OR id IS NULL OR baseid = id THEN" //
                                + "    RETURN false;" //
                                + "  END IF;" //
                                + "  LOOP" //
                                + "    SELECT parentid INTO curid FROM hierarchy WHERE hierarchy.id = curid;" //
                                + "    IF curid IS NULL THEN" //
                                + "      RETURN false; " //
                                + "    ELSEIF curid = baseid THEN" //
                                + "      RETURN true;" //
                                + "    END IF;" //
                                + "  END LOOP;" //
                                + "END " //
                                + "$$ " //
                                + "LANGUAGE plpgsql " //
                                + "STABLE " //
                        , idType)));

        statements.add(new ConditionalStatement(
                true, // early
                Boolean.FALSE, // no drop needed
                null, //
                null, //
                String.format(
                        "CREATE OR REPLACE FUNCTION NX_ACCESS_ALLOWED" //
                                + "(id %s, users varchar[], permissions varchar[]) " //
                                + "RETURNS boolean " //
                                + "AS $$ " //
                                + "DECLARE" //
                                + "  curid %<s := id;" //
                                + "  newid %<s;" //
                                + "  r record;" //
                                + "  first boolean := true;" //
                                + "BEGIN" //
                                + "  WHILE curid IS NOT NULL LOOP" //
                                + "    FOR r in SELECT acls.grant, acls.permission, acls.user FROM acls WHERE acls.id = curid ORDER BY acls.pos LOOP"
                                + "      IF r.permission = ANY(permissions) AND r.user = ANY(users) THEN" //
                                + "        RETURN r.grant;" //
                                + "      END IF;" //
                                + "    END LOOP;" //
                                + "    SELECT parentid INTO newid FROM hierarchy WHERE hierarchy.id = curid;" //
                                + "    IF first AND newid IS NULL THEN" //
                                + "      SELECT versionableid INTO newid FROM versions WHERE versions.id = curid;" //
                                + "    END IF;" //
                                + "    first := false;" //
                                + "    curid := newid;" //
                                + "  END LOOP;" //
                                + "  RETURN false; " //
                                + "END " //
                                + "$$ " //
                                + "LANGUAGE plpgsql " //
                                + "STABLE " //
                                + "COST 500 " //
                        , idType)));

        statements.add(new ConditionalStatement( //
                true, // early
                Boolean.FALSE, // no drop needed
                null, //
                null, //
                String.format(
                        "CREATE OR REPLACE FUNCTION NX_TO_TSVECTOR(string VARCHAR) " //
                                + "RETURNS TSVECTOR " //
                                + "AS $$" //
                                + "  SELECT TO_TSVECTOR('%s', $1) " //
                                + "$$ " //
                                + "LANGUAGE sql " //
                                + "STABLE " //
                        , fulltextAnalyzer)));

        statements.add(new ConditionalStatement( //
                true, // early
                Boolean.FALSE, // no drop needed
                null, //
                null, //
                String.format(
                        "CREATE OR REPLACE FUNCTION NX_CONTAINS(ft TSVECTOR, query VARCHAR) " //
                                + "RETURNS boolean " //
                                + "AS $$" //
                                + "  SELECT $1 @@ TO_TSQUERY('%s', $2) " //
                                + "$$ " //
                                + "LANGUAGE sql " //
                                + "STABLE " //
                        , fulltextAnalyzer)));

        statements.add(new ConditionalStatement(
                true, // early
                Boolean.FALSE, // no drop needed
                null, //
                null, //
                String.format(
                        "CREATE OR REPLACE FUNCTION NX_CLUSTER_INVAL" //
                                + "(i %s, f varchar[], k int) " //
                                + "RETURNS VOID " //
                                + "AS $$ " //
                                + "DECLARE" //
                                + "  nid int; " //
                                + "BEGIN" //
                                + "  FOR nid IN SELECT nodeid FROM cluster_nodes WHERE nodeid <> pg_backend_pid() LOOP" //
                                + "  INSERT INTO cluster_invals (nodeid, id, fragments, kind) VALUES (nid, i, f, k);" //
                                + "  END LOOP; " //
                                + "END " //
                                + "$$ " //
                                + "LANGUAGE plpgsql" //
                        , idType)));

        statements.add(new ConditionalStatement(
                true, // early
                Boolean.FALSE, // no drop needed
                null, //
                null, //
                "CREATE OR REPLACE FUNCTION NX_CLUSTER_GET_INVALS() " //
                        + "RETURNS SETOF RECORD " //
                        + "AS $$ " //
                        + "DECLARE" //
                        + "  r RECORD; " //
                        + "BEGIN" //
                        + "  FOR r IN SELECT id, fragments, kind FROM cluster_invals WHERE nodeid = pg_backend_pid() LOOP" //
                        + "    RETURN NEXT r;" //
                        + "  END LOOP;" //
                        + "  DELETE FROM cluster_invals WHERE nodeid = pg_backend_pid();" //
                        + "  RETURN; " //
                        + "END " //
                        + "$$ " //
                        + "LANGUAGE plpgsql" //
        ));

        FulltextInfo fti = model.getFulltextInfo();
        List<String> lines = new ArrayList<String>(fti.indexNames.size());
        for (String indexName : fti.indexNames) {
            String suffix = model.getFulltextIndexSuffix(indexName);
            Column ftft = ft.getColumn(model.FULLTEXT_FULLTEXT_KEY + suffix);
            Column ftst = ft.getColumn(model.FULLTEXT_SIMPLETEXT_KEY + suffix);
            Column ftbt = ft.getColumn(model.FULLTEXT_BINARYTEXT_KEY + suffix);
            String line = String.format(
                    "  NEW.%s := COALESCE(NEW.%s, ''::TSVECTOR) || COALESCE(NEW.%s, ''::TSVECTOR);",
                    ftft.getQuotedName(), ftst.getQuotedName(),
                    ftbt.getQuotedName());
            lines.add(line);
        }
        statements.add(new ConditionalStatement( //
                false, // late
                Boolean.FALSE, // no drop needed
                null, //
                null, //
                "CREATE OR REPLACE FUNCTION NX_UPDATE_FULLTEXT() " //
                        + "RETURNS trigger " //
                        + "AS $$ " //
                        + "BEGIN" //
                        + StringUtils.join(lines, "") //
                        + "  RETURN NEW; " //
                        + "END " //
                        + "$$ " //
                        + "LANGUAGE plpgsql " //
                        + "VOLATILE " //
        ));

        statements.add(new ConditionalStatement(
                false, // late
                Boolean.TRUE, // do a drop
                null, //
                String.format("DROP TRIGGER IF EXISTS NX_TRIG_FT_UPDATE ON %s",
                        ft.getQuotedName()),
                String.format("CREATE TRIGGER NX_TRIG_FT_UPDATE " //
                        + "BEFORE INSERT OR UPDATE ON %s "
                        + "FOR EACH ROW EXECUTE PROCEDURE NX_UPDATE_FULLTEXT()" //
                , ft.getQuotedName())));

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
        // delete nodes for sessions don't exist anymore
        return String.format(
                "DELETE FROM %s N WHERE "
                        + "NOT EXISTS(SELECT * FROM pg_stat_activity S WHERE N.%s = S.procpid) ",
                cln.getQuotedName(), clnid.getQuotedName());
    }

    @Override
    public String getCreateClusterNodeSql(Model model, Database database) {
        Table cln = database.getTable(model.CLUSTER_NODES_TABLE_NAME);
        Column clnid = cln.getColumn(model.CLUSTER_NODES_NODEID_KEY);
        Column clncr = cln.getColumn(model.CLUSTER_NODES_CREATED_KEY);
        return String.format(
                "INSERT INTO %s (%s, %s) VALUES (pg_backend_pid(), CURRENT_TIMESTAMP)",
                cln.getQuotedName(), clnid.getQuotedName(),
                clncr.getQuotedName());
    }

    @Override
    public String getRemoveClusterNodeSql(Model model, Database database) {
        Table cln = database.getTable(model.CLUSTER_NODES_TABLE_NAME);
        Column clnid = cln.getColumn(model.CLUSTER_NODES_NODEID_KEY);
        return String.format("DELETE FROM %s WHERE %s = pg_backend_pid()",
                cln.getQuotedName(), clnid.getQuotedName());
    }

    @Override
    public String getClusterInsertInvalidations() {
        return "SELECT NX_CLUSTER_INVAL(?, ?, ?)";
    }

    @Override
    public String getClusterGetInvalidations() {
        // TODO id type
        return "SELECT * FROM NX_CLUSTER_GET_INVALS() "
                + "AS invals(id varchar(36), fragments varchar[], kind int2)";
    }

    @Override
    public Collection<ConditionalStatement> getTestConditionalStatements(
            Model model, Database database) {
        List<ConditionalStatement> statements = new LinkedList<ConditionalStatement>();
        statements.add(new ConditionalStatement(true, Boolean.FALSE, null,
                null,
                // here use a TEXT instead of a VARCHAR to test compatibility
                "CREATE TABLE testschema2 (id varchar(36) NOT NULL, title text)"));
        statements.add(new ConditionalStatement(true, Boolean.FALSE, null,
                null,
                "ALTER TABLE testschema2 ADD CONSTRAINT testschema2_pk PRIMARY KEY (id)"));
        return statements;
    }
}
