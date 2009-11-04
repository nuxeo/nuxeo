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

package org.nuxeo.ecm.core.storage.sql.db.dialect;

import java.io.Serializable;
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
import java.util.Collection;
import java.util.Collections;
import java.util.GregorianCalendar;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
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

    private static final Log log = LogFactory.getLog(DialectPostgreSQL.class);

    private static final String DEFAULT_FULLTEXT_ANALYZER = "english";

    protected final String fulltextAnalyzer;

    protected boolean hierarchyCreated;

    protected boolean pathOptimizationsEnabled;

    public DialectPostgreSQL(DatabaseMetaData metadata,
            RepositoryDescriptor repositoryDescriptor) throws StorageException {
        super(metadata);
        fulltextAnalyzer = repositoryDescriptor.fulltextAnalyzer == null ? DEFAULT_FULLTEXT_ANALYZER
                : repositoryDescriptor.fulltextAnalyzer;
        pathOptimizationsEnabled = repositoryDescriptor.pathOptimizationsEnabled;
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
            String quotedIndexName, Table table, List<Column> columns,
            Model model) {
        return String.format("CREATE INDEX %s ON %s USING GIN(%s)",
                quotedIndexName.toLowerCase(), table.getQuotedName(),
                columns.get(0).getQuotedName());
    }

    @Override
    public String getDialectFulltextQuery(String query) {
        query = query.replace(" & ", " "); // PostgreSQL compatibility BBB
        query = query.replaceAll(" +", " ");
        List<String> res = new LinkedList<String>();
        for (String word : StringUtils.split(query, ' ', false)) {
            if (word.startsWith("-")) {
                res.add("!" + word.substring(1));
            } else if (word.startsWith("+")) {
                res.add(word.substring(1));
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
    public boolean needsAliasForDerivedTable() {
        return true;
    }

    @Override
    public boolean supportsReadAcl() {
        return true;
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
    public boolean supportsDescendantsTable() {
        return true;
    }

    @Override
    public String getInTreeSql(String idColumnName) {
        if (pathOptimizationsEnabled) {
            return String.format(
                    "EXISTS(SELECT 1 FROM descendants WHERE id = ? AND descendantid = %s)",
                    idColumnName);
        } else {
            return String.format("NX_IN_TREE(%s, ?)", idColumnName);
        }

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
        Table ht = database.getTable(model.hierTableName);
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
                                + "COST 400 " //
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

        statements.add(new ConditionalStatement(
                true, // early
                Boolean.FALSE, // no drop needed
                null, //
                null, //
                String.format(
                        "CREATE OR REPLACE FUNCTION NX_TO_TSVECTOR(string VARCHAR) " //
                                + "RETURNS TSVECTOR " //
                                + "AS $$" //
                                + "  SELECT TO_TSVECTOR('%s', SUBSTR($1, 1, 250000)) " //
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

        statements.add(new ConditionalStatement(
                true, // early
                Boolean.FALSE, // no drop needed
                null, //
                null, //
                "CREATE OR REPLACE FUNCTION NX_DESCENDANTS_CREATE_TRIGGERS() " //
                        + "RETURNS void " //
                        + "AS $$ " //
                        + "  DROP TRIGGER IF EXISTS NX_TRIG_DESC_INSERT ON hierarchy;" //
                        + "  CREATE TRIGGER NX_TRIG_DESC_INSERT" //
                        + "    AFTER INSERT ON hierarchy" //
                        + "    FOR EACH ROW EXECUTE PROCEDURE NX_DESCENDANTS_INSERT();" //
                        + "  DROP TRIGGER IF EXISTS NX_TRIG_DESC_UPDATE ON hierarchy;" //
                        + "  CREATE TRIGGER NX_TRIG_DESC_UPDATE" //
                        + "    AFTER UPDATE ON hierarchy" //
                        + "    FOR EACH ROW EXECUTE PROCEDURE NX_DESCENDANTS_UPDATE(); " //
                        + "$$ " //
                        + "LANGUAGE sql " //
                        + "VOLATILE " //
        ));

        statements.add(new ConditionalStatement(
                true, // early
                Boolean.FALSE, // no drop needed
                null, //
                null, //
                "CREATE OR REPLACE FUNCTION NX_INIT_DESCENDANTS() " //
                        + "RETURNS void " //
                        + "AS $$ " //
                        + "DECLARE" //
                        + "  curid varchar(36); " //
                        + "  curparentid varchar(36); " //
                        + "BEGIN " //
                        + "  PERFORM NX_DESCENDANTS_CREATE_TRIGGERS(); " //
                        + "  CREATE TEMP TABLE nxtodo (id varchar(36), parentid varchar(36)) ON COMMIT DROP; " //
                        + "  CREATE INDEX nxtodo_idx ON nxtodo (id);" //
                        + "  INSERT INTO nxtodo SELECT id, NULL FROM repositories;" //
                        + "  TRUNCATE TABLE descendants;" //
                        + "  LOOP" //
                        + "    -- get next node in queue\n" //
                        + "    SELECT id, parentid INTO curid, curparentid FROM nxtodo LIMIT 1;" //
                        + "    IF NOT FOUND THEN" //
                        + "      EXIT;" //
                        + "    END IF;" //
                        + "    DELETE FROM nxtodo WHERE id = curid;" //
                        + "    -- add children to queue\n" //
                        + "    INSERT INTO nxtodo SELECT id, curid FROM hierarchy" //
                        + "      WHERE parentid = curid and NOT isproperty;" //
                        + "    IF curparentid IS NULL THEN" //
                        + "      CONTINUE;" //
                        + "    END IF;" //
                        + "    -- process the node\n" //
                        + "    INSERT INTO descendants (id, descendantid)" //
                        + "      SELECT id, curid FROM descendants WHERE descendantid = curparentid;" //
                        + "    INSERT INTO descendants (id, descendantid) VALUES (curparentid, curid);" //
                        + "  END LOOP;" //
                        + "END " //
                        + "$$ " //
                        + "LANGUAGE plpgsql " //
                        + "VOLATILE " //
        ));

        statements.add(new ConditionalStatement(
                true, // early
                Boolean.FALSE, // no drop needed
                null, //
                null, //
                "CREATE OR REPLACE FUNCTION NX_DESCENDANTS_INSERT() " //
                        + "RETURNS trigger " //
                        + "AS $$ " //
                        + "BEGIN " //
                        + "  IF NEW.isproperty THEN" //
                        + "    RETURN NULL; " //
                        + "  END IF;" //
                        + "  IF NEW.parentid IS NULL THEN" //
                        + "    RETURN NULL; " //
                        + "  END IF;" //
                        + "  IF NEW.id IS NULL THEN" //
                        + "    RAISE EXCEPTION 'Cannot have NULL id'; " //
                        + "  END IF;" //
                        + "  INSERT INTO descendants (id, descendantid)" //
                        + "    SELECT id, NEW.id FROM descendants WHERE descendantid = NEW.parentid;" //
                        + "  INSERT INTO descendants (id, descendantid) VALUES (NEW.parentid, NEW.id);" //
                        + "  RETURN NULL; " //
                        + "END " //
                        + "$$ " //
                        + "LANGUAGE plpgsql " //
                        + "VOLATILE " //
        ));

        statements.add(new ConditionalStatement(
                true, // early
                Boolean.FALSE, // no drop needed
                null, //
                null, //
                "CREATE OR REPLACE FUNCTION NX_DESCENDANTS_UPDATE() " //
                        + "RETURNS trigger " //
                        + "AS $$ " //
                        + "BEGIN " //
                        + "  IF NEW.isproperty THEN" //
                        + "    RETURN NULL; " //
                        + "  END IF;" //
                        + "  IF OLD.id IS DISTINCT FROM NEW.id THEN" //
                        + "    RAISE EXCEPTION 'Cannot change id'; " //
                        + "  END IF;" //
                        + "  IF OLD.parentid IS NOT DISTINCT FROM NEW.parentid THEN" //
                        + "    RETURN NULL; " //
                        + "  END IF;" //
                        + "  IF NEW.id IS NULL THEN" //
                        + "    RAISE EXCEPTION 'Cannot have NULL id'; " //
                        + "  END IF;" //
                        + "  IF OLD.parentid IS NOT NULL THEN" //
                        + "    IF NEW.parentid IS NOT NULL THEN" //
                        + "      IF NEW.parentid = NEW.id THEN" //
                        + "        RAISE EXCEPTION 'Cannot move a node under itself'; " //
                        + "      END IF;" //
                        + "      IF EXISTS(SELECT 1 FROM descendants WHERE id = NEW.id AND descendantid = NEW.parentid) THEN" //
                        + "        RAISE EXCEPTION 'Cannot move a node under one of its descendants'; " //
                        + "      END IF;" //
                        + "    END IF;" //
                        + "    -- the old parent and its ancestors lose some descendants\n" //
                        + "    DELETE FROM descendants" //
                        + "      WHERE id IN (SELECT id FROM descendants WHERE descendantid = NEW.id)" //
                        + "      AND descendantid IN (SELECT descendantid FROM descendants WHERE id = NEW.id" //
                        + "                           UNION ALL SELECT NEW.id);" //
                        + "  END IF;" //
                        + "  IF NEW.parentid IS NOT NULL THEN" //
                        + "    -- the new parent's ancestors gain as descendants\n" //
                        + "    -- the descendants of the moved node (cross join)\n" //
                        + "    INSERT INTO descendants (id, descendantid)" //
                        + "      (SELECT A.id, B.descendantid FROM descendants A CROSS JOIN descendants B" //
                        + "       WHERE A.descendantid = NEW.parentid AND B.id = NEW.id);" //
                        + "    -- the new parent's ancestors gain as descendant the moved node\n" //
                        + "    INSERT INTO descendants (id, descendantid)" //
                        + "      SELECT id, NEW.id FROM descendants WHERE descendantid = NEW.parentid;" //
                        + "    -- the new parent gains as descendants the descendants of the moved node\n" //
                        + "    INSERT INTO descendants (id, descendantid)" //
                        + "      SELECT NEW.parentid, descendantid FROM descendants WHERE id = NEW.id;" //
                        + "    -- the new parent gains as descendant the moved node\n" //
                        + "    INSERT INTO descendants (id, descendantid)" //
                        + "      VALUES (NEW.parentid, NEW.id);" //
                        + "  END IF;" //
                        + "  RETURN NULL; " //
                        + "END " //
                        + "$$ " //
                        + "LANGUAGE plpgsql " //
                        + "VOLATILE " //
        ));

        // read acls ----------------------------------------------------------
        // table to store canonical read acls
        statements.add(new ConditionalStatement(
                false, // late
                null, // perform a check
                "SELECT 1 WHERE NOT EXISTS(SELECT 1 FROM pg_tables WHERE tablename='read_acls');",
                "CREATE TABLE read_acls ("
                        + "  id character varying(34) PRIMARY KEY,"
                        + "  acl character varying(4096));", //
                "SELECT 1;"));
        // table to maintain a read acl for each hierarchy entry
        statements.add(new ConditionalStatement(
                false, // late
                null, // perform a check
                "SELECT 1 WHERE NOT EXISTS(SELECT 1 FROM pg_tables WHERE tablename='hierarchy_read_acl');",
                "CREATE TABLE hierarchy_read_acl ("
                        + "  id character varying(36) PRIMARY KEY,"
                        + "  acl_id character varying(34),"
                        + "  CONSTRAINT hierarchy_read_acl_id_fk FOREIGN KEY (id) REFERENCES hierarchy(id) ON DELETE CASCADE"
                        + ");", //
                "SELECT 1;"));
        // Add index
        statements.add(new ConditionalStatement(
                false, // late
                null, // perform a check
                "SELECT 1 WHERE NOT EXISTS(SELECT 1 FROM pg_indexes WHERE indexname='hierarchy_read_acl_acl_id_idx');",
                "CREATE INDEX hierarchy_read_acl_acl_id_idx ON hierarchy_read_acl USING btree (acl_id);",
                "SELECT 1;"));
        // Log hierarchy with updated read acl
        statements.add(new ConditionalStatement(
                false, // late
                null, // perform a check
                "SELECT 1 WHERE NOT EXISTS(SELECT 1 FROM pg_tables WHERE tablename='hierarchy_modified_acl');",
                "CREATE TABLE hierarchy_modified_acl ("
                        + "  id character varying(36)," //
                        + "  is_new boolean" //
                        + ");", //
                "SELECT 1;"));
        statements.add(new ConditionalStatement(
                false, // late
                Boolean.FALSE, // do a drop
                null, //
                null, //
                "CREATE OR REPLACE FUNCTION nx_get_local_read_acl(id character varying) RETURNS character varying AS $$\n" //
                        + " -- Compute the read acl for a hierarchy id using a local acl\n" //
                        + "DECLARE\n" //
                        + "  curid varchar(36) := id;\n" //
                        + "  read_acl varchar(4096) := NULL;\n" //
                        + "  r record;\n" //
                        + "BEGIN\n" //
                        + "  -- RAISE INFO 'call %', curid;\n" //
                        + "  FOR r in SELECT CASE\n" //
                        + "         WHEN (acls.grant AND\n" //
                        + "             acls.permission IN ('Read', 'ReadWrite', 'Everything', 'Browse')) THEN\n" //
                        + "           acls.user\n" //
                        + "         WHEN (NOT acls.grant AND\n" //
                        + "             acls.permission IN ('Read', 'ReadWrite', 'Everything', 'Browse')) THEN\n" //
                        + "           '-'|| acls.user\n" //
                        + "         ELSE NULL END AS op\n" //
                        + "       FROM acls WHERE acls.id = curid\n" //
                        + "       ORDER BY acls.pos LOOP\n" //
                        + "    IF r.op IS NULL THEN\n" //
                        + "      CONTINUE;\n" //
                        + "    END IF;\n" //
                        + "    IF read_acl IS NULL THEN\n" //
                        + "      read_acl := r.op;\n" //
                        + "    ELSE\n" //
                        + "      read_acl := read_acl || ',' || r.op;\n" //
                        + "    END IF;\n" //
                        + "  END LOOP;\n" //
                        + "  RETURN read_acl;\n" //
                        + "END $$\n" //
                        + "LANGUAGE plpgsql STABLE;"));
        statements.add(new ConditionalStatement(
                false, // late
                Boolean.FALSE, // do a drop
                null, //
                null, //
                "CREATE OR REPLACE FUNCTION nx_get_read_acl(id character varying) RETURNS character varying AS $$\n" //
                        + " -- Compute the read acl for a hierarchy id using inherited acl \n" //
                        + "DECLARE\n" //
                        + "  curid varchar(36) := id;\n" //
                        + "  newid varchar(36);\n" //
                        + "  first boolean := true;\n" //
                        + "  read_acl varchar(4096);\n" //
                        + "  ret varchar(4096);\n" //
                        + "BEGIN\n" //
                        + "  -- RAISE INFO 'call %', curid;\n" //
                        + "  WHILE curid IS NOT NULL LOOP\n" //
                        + "    -- RAISE INFO '  curid %', curid;\n" //
                        + "    SELECT nx_get_local_read_acl(curid) INTO read_acl;\n" //
                        + "    IF (read_acl IS NOT NULL) THEN\n" //
                        + "      IF (ret is NULL) THEN\n" //
                        + "        ret = read_acl;\n" //
                        + "      ELSE\n" //
                        + "        ret := ret || ',' || read_acl;\n" //
                        + "      END IF;\n" //
                        + "    END IF;\n" //
                        + "    SELECT parentid INTO newid FROM hierarchy WHERE hierarchy.id = curid;\n" //
                        + "    IF (first AND newid IS NULL) THEN\n" //
                        + "      SELECT versionableid INTO newid FROM versions WHERE versions.id = curid;\n" //
                        + "    END IF;\n" //
                        + "    first := false;\n" //
                        + "    curid := newid;\n" //
                        + "  END LOOP;\n" //
                        + "  IF (ret is NULL) THEN\n" //
                        + "    ret = '_empty';\n" //
                        + "  END IF;\n" //
                        + "  RETURN ret;\n" //
                        + "END $$\n" //
                        + "LANGUAGE plpgsql STABLE;"));
        statements.add(new ConditionalStatement(
                false, // late
                Boolean.FALSE, // do a drop
                null, //
                null, //
                "CREATE OR REPLACE FUNCTION nx_get_read_acls_for(users character varying[]) RETURNS SETOF text AS $$\n" //
                        + "-- List read acl ids for a list of user/groups\n" //
                        + "DECLARE\n" //
                        + "  r record;\n" //
                        + "  rr record;\n" //
                        + "  users_blacklist character varying[];\n" //
                        + "BEGIN\n" //
                        + "  RAISE INFO 'nx_get_read_acls_for called';\n" //
                        + "  -- Build a black list with negative users\n" //
                        + "  SELECT regexp_split_to_array('-' || array_to_string(users, ',-'), ',')\n" //
                        + "    INTO users_blacklist;\n" //
                        + "  <<acl_loop>>\n" //
                        + "  FOR r IN SELECT read_acls.id, read_acls.acl FROM read_acls LOOP\n" //
                        + "    -- RAISE INFO 'ACL %', r.id;\n" //
                        + "    -- split the acl into aces\n" //
                        + "    FOR rr IN SELECT ace FROM regexp_split_to_table(r.acl, ',') AS ace LOOP\n" //
                        + "       -- RAISE INFO '  ACE %', rr.ace;\n" //
                        + "       IF (rr.ace = ANY(users)) THEN\n" //
                        + "         -- RAISE INFO '  GRANT %', users;\n" //
                        + "         RETURN NEXT r.id;\n" //
                        + "         CONTINUE acl_loop;\n" //
                        + "         -- ok\n" //
                        + "       ELSEIF (rr.ace = ANY(users_blacklist)) THEN\n" //
                        + "         -- RAISE INFO '  DENY';\n" //
                        + "         CONTINUE acl_loop;\n" //
                        + "       END IF;\n" //
                        + "    END LOOP;\n" //
                        + "  END LOOP acl_loop;\n" //
                        + "  RETURN;\n" //
                        + "END $$\n" //
                        + "LANGUAGE plpgsql STABLE;"));
        statements.add(new ConditionalStatement(
                false, // late
                Boolean.FALSE, // do a drop
                null, //
                null, //
                "CREATE OR REPLACE FUNCTION nx_log_acls_modified() RETURNS trigger  AS $$\n" //
                        + "-- Trigger to log change in the acls table\n" //
                        + "DECLARE\n" //
                        + "  doc_id varchar(36);\n" //
                        + "BEGIN\n" //
                        + "  IF (TG_OP = 'DELETE') THEN\n" //
                        + "    doc_id := OLD.id;\n" //
                        + "  ELSE\n" //
                        + "    doc_id := NEW.id;\n" //
                        + "  END IF;\n" //
                        + "  INSERT INTO hierarchy_modified_acl VALUES(doc_id, 'f');\n" //
                        + "  RETURN NEW;\n" //
                        + "END $$\n" //
                        + "LANGUAGE plpgsql;"));
        statements.add(new ConditionalStatement(
                false, // late
                Boolean.TRUE, // do a drop
                null, //
                "DROP TRIGGER IF EXISTS nx_trig_acls_modified ON acls;",
                "CREATE TRIGGER nx_trig_acls_modified\n" //
                        + "  AFTER INSERT OR UPDATE OR DELETE ON acls\n" //
                        + "  FOR EACH ROW EXECUTE PROCEDURE nx_log_acls_modified();"));
        statements.add(new ConditionalStatement(
                false, // late
                Boolean.FALSE, // do a drop
                null, //
                null, //
                "CREATE OR REPLACE FUNCTION nx_log_hierarchy_modified() RETURNS trigger  AS $$\n" //
                        + "-- Trigger to log doc_id that need read acl update\n" //
                        + "DECLARE\n" //
                        + "  doc_id varchar(36);\n" //
                        + "BEGIN\n" //
                        + "  IF (TG_OP = 'INSERT') THEN\n" //
                        + "    IF (NEW.isproperty = 'f') THEN\n" //
                        + "      -- New document\n" //
                        + "      INSERT INTO hierarchy_modified_acl VALUES(NEW.id, 't');\n" //
                        + "    END IF;\n" //
                        + "  ELSEIF (TG_OP = 'UPDATE') THEN\n" //
                        + "    IF (NEW.isproperty = 'f' AND NEW.parentid != OLD.parentid) THEN\n" //
                        + "      -- New container\n" //
                        + "      INSERT INTO hierarchy_modified_acl VALUES(NEW.id, 'f');\n" //
                        + "    END IF;\n" //
                        + "  END IF;\n" //
                        + "  RETURN NEW;\n" //
                        + "END $$\n" //
                        + "LANGUAGE plpgsql;"));
        statements.add(new ConditionalStatement(
                false, // late
                Boolean.TRUE, // do a drop
                null, //
                "DROP TRIGGER IF EXISTS nx_trig_hierarchy_modified ON hierarchy;",
                "CREATE TRIGGER nx_trig_hierarchy_modified\n" //
                        + "  AFTER INSERT OR UPDATE OR DELETE ON hierarchy\n" //
                        + "  FOR EACH ROW EXECUTE PROCEDURE nx_log_hierarchy_modified();"));
        statements.add(new ConditionalStatement(
                false, // late
                Boolean.FALSE, // do a drop
                null, //
                null, //
                "CREATE OR REPLACE FUNCTION nx_rebuild_read_acls() RETURNS void AS $$\n" //
                        + "-- Rebuild the read acls tables\n" //
                        + "BEGIN\n" //
                        + "  RAISE INFO 'nx_rebuild_read_acls truncate hierarchy_read_acl';\n" //
                        + "  TRUNCATE TABLE hierarchy_read_acl;\n" //
                        + "  RAISE INFO 'nx_rebuild_read_acls update acl map';\n" //
                        + "  INSERT INTO hierarchy_read_acl\n" //
                        + "    SELECT id, md5(nx_get_read_acl(id))\n" //
                        + "    FROM (SELECT id FROM hierarchy WHERE isproperty='f') AS uids;\n" //
                        + "  RAISE INFO 'nx_rebuild_read_acls truncate read_acls';\n" //
                        + "  TRUNCATE TABLE read_acls;\n" //
                        + "  INSERT INTO read_acls\n" //
                        + "    SELECT md5(acl), acl\n" //
                        + "    FROM (SELECT DISTINCT(nx_get_read_acl(id)) AS acl\n" //
                        + "        FROM  (SELECT DISTINCT(id) AS id\n" //
                        + "           FROM acls) AS uids) AS read_acls_input;\n" //
                        + "  TRUNCATE TABLE hierarchy_modified_acl;\n" //
                        + "  RAISE INFO 'nx_rebuild_read_acls done';\n" //
                        + "  RETURN;\n" //
                        + "END $$\n" //
                        + "LANGUAGE plpgsql VOLATILE;"));
        statements.add(new ConditionalStatement(
                false, // late
                Boolean.FALSE, // do a drop
                null, //
                null, //
                "CREATE OR REPLACE FUNCTION nx_update_read_acls() RETURNS void AS $$\n" //
                        + "-- Rebuild only necessary read acls\n" //
                        + "DECLARE\n" //
                        + "  update_count integer;\n" //
                        + "BEGIN\n" //
                        + "  -- Rebuild read_acls\n" //
                        + "  RAISE INFO 'nx_update_read_acls REBUILD read_acls';\n" //
                        + "  TRUNCATE TABLE read_acls;\n" //
                        + "  INSERT INTO read_acls\n" //
                        + "    SELECT md5(acl), acl\n" //
                        + "    FROM (SELECT DISTINCT(nx_get_read_acl(id)) AS acl\n" //
                        + "        FROM (SELECT DISTINCT(id) AS id FROM acls) AS uids) AS read_acls_input;\n" //
                        + "\n" //
                        + "  -- New hierarchy_read_acl entry\n" //
                        + "  RAISE INFO 'nx_update_read_acls ADD NEW hierarchy_read_acl entry';\n" //
                        + "  INSERT INTO hierarchy_read_acl\n" //
                        + "    SELECT id, md5(nx_get_read_acl(id))\n" //
                        + "    FROM (SELECT DISTINCT(id) AS id\n" //
                        + "        FROM hierarchy_modified_acl \n" //
                        + "        WHERE is_new AND\n" //
                        + "            EXISTS (SELECT 1 FROM hierarchy WHERE hierarchy_modified_acl.id=hierarchy.id)) AS uids;\n" //
                        + "  GET DIAGNOSTICS update_count = ROW_COUNT;\n" //
                        + "  RAISE INFO 'nx_update_read_acls % hierarchy_read_acl ADDED', update_count;\n" //
                        + "  DELETE FROM hierarchy_modified_acl WHERE is_new;\n" //
                        + "\n" //
                        + "  -- Update hierarchy_read_acl entry\n" //
                        + "  RAISE INFO 'nx_update_read_acls UPDATE existing hierarchy_read_acl';\n" //
                        + "  -- Mark acl that need to be updated (set to NULL)\n" //
                        + "  UPDATE hierarchy_read_acl SET acl_id = NULL WHERE id IN (\n" //
                        + "    SELECT DISTINCT(id) AS id FROM hierarchy_modified_acl WHERE NOT is_new);\n" //
                        + "  GET DIAGNOSTICS update_count = ROW_COUNT;\n" //
                        + "  RAISE INFO 'nx_update_read_acls % hierarchy_read_acl MARKED', update_count;\n" //
                        + "  DELETE FROM hierarchy_modified_acl WHERE NOT is_new;\n" //
                        + "  -- Mark all childrens\n" //
                        + "  LOOP\n" //
                        + "    UPDATE hierarchy_read_acl SET acl_id = NULL WHERE id IN (\n" //
                        + "      SELECT h.id\n" //
                        + "      FROM hierarchy AS h\n" //
                        + "      JOIN hierarchy_read_acl AS r ON h.id = r.id\n" //
                        + "      WHERE r.acl_id IS NOT NULL\n" //
                        + "        AND h.parentid IN (SELECT id FROM hierarchy_read_acl WHERE acl_id IS NULL));\n" //
                        + "    GET DIAGNOSTICS update_count = ROW_COUNT;\n" //
                        + "    RAISE INFO 'nx_update_read_acls % hierarchy_read_acl MARKED for udpate', update_count;\n" //
                        + "    IF (update_count = 0) THEN\n" //
                        + "      EXIT;\n" //
                        + "    END IF;\n" //
                        + "  END LOOP;\n" //
                        + "  -- Update hierarchy_read_acl acl_ids\n" //
                        + "  UPDATE hierarchy_read_acl SET acl_id = md5(nx_get_read_acl(id)) WHERE acl_id IS NULL;\n" //
                        + "  GET DIAGNOSTICS update_count = ROW_COUNT;\n" //
                        + "  RAISE INFO 'nx_update_read_acls % hierarchy_read_acl UPDATED', update_count;\n" //
                        + "\n" //
                        + "  RETURN;\n" //
                        + "END $$\n" //
                        + "LANGUAGE plpgsql VOLATILE;"));
        // build the read acls if empty, this takes care of the upgrade
        statements.add(new ConditionalStatement(
                false, // late
                null, // perform a check
                "SELECT 1 WHERE NOT EXISTS(SELECT 1 FROM read_acls LIMIT 1);",
                "SELECT * FROM nx_rebuild_read_acls();", //
                "SELECT 1;"));

        return statements;
    }

    @Override
    public boolean preCreateTable(Connection connection, Table table,
            Model model, Database database) throws SQLException {
        if (table.getName().equals(model.hierTableName.toLowerCase())) {
            hierarchyCreated = true;
            return true;
        }
        if (table.getName().equals(Model.DESCENDANTS_TABLE_NAME.toLowerCase())) {
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
            if (count > 1000) {
                // if the hierarchy table is too big, tell the admin to do the
                // init by hand
                pathOptimizationsEnabled = false;
                log.error("Table DESCENDANTS not initialized automatically because table HIERARCHY is too big. "
                        + "Upgrade by hand by calling: SELECT NX_INIT_DESCENDANTS()");
            }
            return true;
        }
        return true;
    }

    @Override
    public List<String> getPostCreateTableSqls(Table table, Model model,
            Database database) {
        if (table.getName().equals(Model.DESCENDANTS_TABLE_NAME.toLowerCase())) {
            List<String> sqls = new ArrayList<String>();
            if (pathOptimizationsEnabled) {
                sqls.add("SELECT NX_INIT_DESCENDANTS()");
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
        if (table.getName().equals(Model.DESCENDANTS_TABLE_NAME.toLowerCase())) {
            if (!pathOptimizationsEnabled) {
                log.info("Path optimizations disabled");
                return;
            }
            // check if we want to initialize the descendants table now, or log
            // a warning if the hierarchy table is too big
            String sql = "SELECT COUNT(*) FROM descendants";
            Statement s = connection.createStatement();
            ResultSet rs = s.executeQuery(sql);
            rs.next();
            long count = rs.getLong(1);
            rs.close();
            s.close();
            if (count == 0) {
                pathOptimizationsEnabled = false;
                log.error("Table DESCENDANTS empty, must be upgraded by hand by calling: "
                        + "SELECT NX_INIT_DESCENDANTS()");
                log.info("Path optimizations disabled");
            }
        }
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

}
