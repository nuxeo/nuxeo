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
import java.lang.reflect.Constructor;
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
 * Oracle-specific dialect.
 *
 * @author Florent Guillaume
 */
public class DialectOracle extends Dialect {

    protected final String fulltextParameters;

    public DialectOracle(DatabaseMetaData metadata,
            RepositoryDescriptor repositoryDescriptor) throws StorageException {
        super(metadata);
        fulltextParameters = repositoryDescriptor.fulltextAnalyzer == null ? ""
                : repositoryDescriptor.fulltextAnalyzer;
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
            return jdbcInfo("VARCHAR2(32)", Types.VARCHAR);
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
            String quotedIndexName, String tableName, List<String> columnNames) {
        return String.format(
                "CREATE INDEX %s ON %s(%s) INDEXTYPE IS CTXSYS.CONTEXT "
                        + "PARAMETERS('%s SYNC (ON COMMIT) TRANSACTIONAL')",
                quotedIndexName, tableName, columnNames.get(0),
                fulltextParameters);
    }

    @Override
    public String getDialectFulltextQuery(String query) {
        query = query.replaceAll(" +", " ");
        List<String> pos = new LinkedList<String>();
        List<String> neg = new LinkedList<String>();
        for (String word : StringUtils.split(query, ' ', false)) {
            if (word.startsWith("-")) {
                neg.add(word.substring(1));
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

    @Override
    public String[] getFulltextMatch(String indexName, String fulltextQuery,
            Column mainColumn, Model model, Database database) {
        String suffix = model.getFulltextIndexSuffix(indexName);
        Column ftColumn = database.getTable(model.FULLTEXT_TABLE_NAME).getColumn(
                model.FULLTEXT_FULLTEXT_KEY + suffix);
        String whereExpr = String.format("CONTAINS(%s, ?) > 0",
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
    public String getSecurityCheckSql(String idColumnName) {
        return String.format("NX_ACCESS_ALLOWED(%s, ?, ?) = 1", idColumnName);
    }

    @Override
    public String getInTreeSql(String idColumnName) {
        return String.format("NX_IN_TREE(%s, ?) = 1", idColumnName);
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
                    "NX_ARRAY", connection);
            return (Array) arrayConstructor.newInstance(arrayDescriptor,
                    connection, elements);
        } catch (Exception e) {
            throw new SQLException(e.toString());
        }
    }

    @Override
    public Collection<ConditionalStatement> getConditionalStatements(
            Model model, Database database) {
        String idType; // for function parameters
        String declaredType; // for PL/SQL declarations
        switch (model.idGenPolicy) {
        case APP_UUID:
            idType = "VARCHAR2";
            declaredType = "VARCHAR2(36)";
            break;
        case DB_IDENTITY:
            idType = "INTEGER";
            declaredType = "INTEGER";
            break;
        default:
            throw new AssertionError(model.idGenPolicy);
        }

        List<ConditionalStatement> statements = new LinkedList<ConditionalStatement>();

        statements.add(new ConditionalStatement( //
                true, // early
                Boolean.FALSE, // no drop needed
                null, //
                null, //
                "CREATE OR REPLACE TYPE NX_ARRAY AS VARRAY(99) OF VARCHAR2(100);"));

        statements.add(new ConditionalStatement(
                false, // late
                Boolean.FALSE, // no drop needed
                null, //
                null, //
                String.format(
                        "CREATE OR REPLACE FUNCTION NX_IN_TREE(id %s, baseid %<s) " //
                                + "RETURN NUMBER IS " //
                                + "  curid %s := id; " //
                                + "BEGIN" //
                                + "  IF baseid IS NULL OR id IS NULL OR baseid = id THEN" //
                                + "    RETURN 0;" //
                                + "  END IF;" //
                                + "  LOOP" //
                                + "    SELECT parentid INTO curid FROM hierarchy WHERE hierarchy.id = curid;" //
                                + "    IF curid IS NULL THEN" //
                                + "      RETURN 0; " //
                                + "    ELSIF curid = baseid THEN" //
                                + "      RETURN 1;" //
                                + "    END IF;" //
                                + "  END LOOP; " //
                                + "END;" //
                        , idType, declaredType)));

        statements.add(new ConditionalStatement(
                false, // late
                Boolean.FALSE, // no drop needed
                null, //
                null, //
                String.format(
                        "CREATE OR REPLACE FUNCTION NX_ACCESS_ALLOWED" //
                                + "(id %s, users NX_ARRAY, permissions NX_ARRAY) " //
                                + "RETURN NUMBER IS " //
                                + "  curid %s := id;" //
                                + "  newid %<s;" //
                                + "  first BOOLEAN := TRUE;" //
                                + "BEGIN" //
                                + "  WHILE curid IS NOT NULL LOOP" //
                                + "    FOR r IN (SELECT * FROM acls WHERE acls.id = curid ORDER BY acls.pos) LOOP" //
                                + "      FOR i IN permissions.FIRST .. permissions.LAST LOOP" //
                                + "        IF r.permission = permissions(i) THEN" //
                                + "          FOR j IN users.FIRST .. users.LAST LOOP" //
                                + "            IF r.user = users(j) THEN" //
                                + "              RETURN r.\"GRANT\";" //
                                + "            END IF;" //
                                + "          END LOOP;" //
                                + "          EXIT;" //
                                + "        END IF;" //
                                + "      END LOOP;" //
                                + "    END LOOP;" //
                                + "    SELECT parentid INTO newid FROM hierarchy WHERE hierarchy.id = curid;" //
                                + "    IF first AND newid IS NULL THEN" //
                                + "      SELECT versionableid INTO newid FROM versions WHERE versions.id = curid;" //
                                + "    END IF;" //
                                + "    first := FALSE;" //
                                + "    curid := newid;" //
                                + "  END LOOP;" //
                                + "  RETURN 0; " //
                                + "END;" //
                        , idType, declaredType)));

        Table ft = database.getTable(model.FULLTEXT_TABLE_NAME);
        FulltextInfo fti = model.getFulltextInfo();
        List<String> lines = new ArrayList<String>(fti.indexNames.size());
        for (String indexName : fti.indexNames) {
            String suffix = model.getFulltextIndexSuffix(indexName);
            Column ftft = ft.getColumn(model.FULLTEXT_FULLTEXT_KEY + suffix);
            Column ftst = ft.getColumn(model.FULLTEXT_SIMPLETEXT_KEY + suffix);
            Column ftbt = ft.getColumn(model.FULLTEXT_BINARYTEXT_KEY + suffix);
            String line = String.format("  :NEW.%s := :NEW.%s || :NEW.%s; ",
                    ftft.getQuotedName(), ftst.getQuotedName(),
                    ftbt.getQuotedName());
            lines.add(line);
        }
        statements.add(new ConditionalStatement( //
                false, // late
                Boolean.FALSE, // no drop
                null, //
                null, //
                "CREATE OR REPLACE TRIGGER NX_TRIG_FT_UPDATE " //
                        + "BEFORE INSERT OR UPDATE ON \"FULLTEXT\" "
                        + "FOR EACH ROW " //
                        + "BEGIN" //
                        + StringUtils.join(lines, "") //
                        + "END;" //
        ));

        return statements;
    }

    @Override
    public Collection<ConditionalStatement> getTestConditionalStatements(
            Model model, Database database) {
        List<ConditionalStatement> statements = new LinkedList<ConditionalStatement>();
        statements.add(new ConditionalStatement(true, Boolean.FALSE, null,
                null,
                // here use a NCLOB instead of a NVARCHAR2 to test compatibility
                "CREATE TABLE TESTSCHEMA2 (ID VARCHAR2(36) NOT NULL, TITLE NCLOB)"));
        statements.add(new ConditionalStatement(true, Boolean.FALSE, null,
                null,
                "ALTER TABLE TESTSCHEMA2 ADD CONSTRAINT TESTSCHEMA2_PK PRIMARY KEY (ID)"));
        return statements;
    }

}
