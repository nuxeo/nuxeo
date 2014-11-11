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
import java.util.LinkedList;
import java.util.List;

import org.nuxeo.ecm.core.storage.StorageException;
import org.nuxeo.ecm.core.storage.sql.Binary;
import org.nuxeo.ecm.core.storage.sql.Model;
import org.nuxeo.ecm.core.storage.sql.RepositoryDescriptor;
import org.nuxeo.ecm.core.storage.sql.db.Column;
import org.nuxeo.ecm.core.storage.sql.db.ColumnType;
import org.nuxeo.ecm.core.storage.sql.db.Database;
import org.nuxeo.ecm.core.storage.sql.db.Table;

/**
 * Derby-specific dialect.
 *
 * @author Florent Guillaume
 */
public class DialectDerby extends Dialect {

    public DialectDerby(DatabaseMetaData metadata,
            RepositoryDescriptor repositoryDescriptor) throws StorageException {
        super(metadata);
    }

    @Override
    public JDBCInfo getJDBCTypeAndString(ColumnType type) {
        switch (type) {
        case VARCHAR:
            return jdbcInfo("VARCHAR(32672)", Types.VARCHAR);
        case CLOB:
            return jdbcInfo("CLOB", Types.CLOB);
        case BOOLEAN:
            return jdbcInfo("SMALLINT", Types.SMALLINT);
        case LONG:
            return jdbcInfo("BIGINT", Types.BIGINT);
        case DOUBLE:
            return jdbcInfo("DOUBLE", Types.DOUBLE);
        case TIMESTAMP:
            return jdbcInfo("TIMESTAMP", Types.TIMESTAMP);
        case BLOBID:
            return jdbcInfo("VARCHAR(32)", Types.VARCHAR);
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
            return jdbcInfo("SMALLINT", Types.TINYINT);
        case INTEGER:
            return jdbcInfo("INTEGER", Types.INTEGER);
        case FTINDEXED:
            return jdbcInfo("CLOB", Types.CLOB);
        case FTSTORED:
            return jdbcInfo("CLOB", Types.CLOB);
        case CLUSTERNODE:
            return jdbcInfo("INTEGER", Types.INTEGER);
        case CLUSTERFRAGS:
            return jdbcInfo("VARCHAR(4000)", Types.VARCHAR);
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
        case Types.SMALLINT:
            ps.setBoolean(index, ((Boolean) value).booleanValue());
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
        case Types.SMALLINT:
            return rs.getBoolean(index);
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
    public int getFulltextIndexedColumns() {
        return 0;
    }

    @Override
    public boolean getMaterializeFulltextSyntheticColumn() {
        return true;
    }

    @Override
    public String getCreateFulltextIndexSql(String indexName,
            String quotedIndexName, Table table, List<Column> columns,
            Model model) {
        throw new UnsupportedOperationException();
    }

    @Override
    public String getDialectFulltextQuery(String query) {
        return query; // TODO
    }

    @Override
    public String[] getFulltextMatch(String indexName, String fulltextQuery,
            Column mainColumn, Model model, Database database) {
        // TODO multiple indexes
        Column ftColumn = database.getTable(model.FULLTEXT_TABLE_NAME).getColumn(
                model.FULLTEXT_FULLTEXT_KEY);
        String qname = ftColumn.getFullQuotedName();
        if (ftColumn.getJdbcType() == Types.CLOB) {
            String colFmt = getClobCast(false);
            if (colFmt != null) {
                qname = String.format(colFmt, qname, Integer.valueOf(255));
            }
        }
        String whereExpr = String.format("NX_CONTAINS(%s, ?) = 1", qname);
        return new String[] { null, null, whereExpr, fulltextQuery };
    }

    @Override
    public boolean supportsUpdateFrom() {
        return false;
    }

    @Override
    public boolean doesUpdateFromRepeatSelf() {
        throw new UnsupportedOperationException();
    }

    @Override
    public String getClobCast(boolean inOrderBy) {
        return "CAST(%s AS VARCHAR(%d))";
    }

    @Override
    public String getSecurityCheckSql(String idColumnName) {
        return String.format("NX_ACCESS_ALLOWED(%s, ?, ?) = 1", idColumnName);
    }

    @Override
    public String getInTreeSql(String idColumnName) {
        return String.format("NX_IN_TREE(%s, ?) = 1", idColumnName);
    }

    private final String className = "org.nuxeo.ecm.core.storage.sql.db.DerbyFunctions";

    @Override
    public Collection<ConditionalStatement> getConditionalStatements(
            Model model, Database database) {
        String idType;
        String methodSuffix;
        switch (model.idGenPolicy) {
        case APP_UUID:
            idType = "VARCHAR(36)";
            methodSuffix = "String";
            break;
        case DB_IDENTITY:
            idType = "INTEGER";
            methodSuffix = "Long";
            break;
        default:
            throw new AssertionError(model.idGenPolicy);
        }
        Table ft = database.getTable(model.FULLTEXT_TABLE_NAME);
        Column ftft = ft.getColumn(model.FULLTEXT_FULLTEXT_KEY);
        Column ftst = ft.getColumn(model.FULLTEXT_SIMPLETEXT_KEY);
        Column ftbt = ft.getColumn(model.FULLTEXT_BINARYTEXT_KEY);
        Column ftid = ft.getColumn(model.MAIN_KEY);

        List<ConditionalStatement> statements = new LinkedList<ConditionalStatement>();

        statements.add(makeFunction("NX_IN_TREE", //
                String.format("(ID %s, BASEID %<s) RETURNS SMALLINT", idType), //
                "isInTree" + methodSuffix, //
                "READS SQL DATA"));

        statements.add(makeFunction(
                "NX_ACCESS_ALLOWED",
                String.format(
                        "(ID %s, PRINCIPALS VARCHAR(10000), PERMISSIONS VARCHAR(10000)) RETURNS SMALLINT",
                        idType), //
                "isAccessAllowed" + methodSuffix, //
                "READS SQL DATA"));

        statements.add(makeFunction(
                "NX_PARSE_FULLTEXT",
                "(S1 VARCHAR(10000), S2 VARCHAR(10000)) RETURNS VARCHAR(10000)",
                "parseFullText", //
                ""));

        statements.add(makeFunction("NX_CONTAINS", //
                "(FT VARCHAR(10000), QUERY VARCHAR(10000)) RETURNS SMALLINT", //
                "matchesFullTextDerby", //
                ""));

        statements.add(makeTrigger(
                "NX_TRIG_FT_INSERT", //
                String.format(
                        "AFTER INSERT ON %1$s "//
                                + "REFERENCING NEW AS NEW " //
                                + "FOR EACH ROW "//
                                + "UPDATE %1$s " //
                                + "SET %2$s = NX_PARSE_FULLTEXT(CAST(%3$s AS VARCHAR(10000)), CAST(%4$s AS VARCHAR(10000))) " //
                                + "WHERE %5$s = NEW.%5$s", //
                        ft.getQuotedName(), // 1 table "FULLTEXT"
                        ftft.getQuotedName(), // 2 column "TEXT"
                        ftst.getQuotedName(), // 3 column "SIMPLETEXT"
                        ftbt.getQuotedName(), // 4 column "BINARYTEXT"
                        ftid.getQuotedName() // 5 column "ID"
                )));

        statements.add(makeTrigger(
                "NX_TRIG_FT_UPDATE", //
                String.format(
                        "AFTER UPDATE OF %3$s, %4$s ON %1$s "//
                                + "REFERENCING NEW AS NEW " //
                                + "FOR EACH ROW "//
                                + "UPDATE %1$s " //
                                + "SET %2$s = NX_PARSE_FULLTEXT(CAST(%3$s AS VARCHAR(10000)), CAST(%4$s AS VARCHAR(10000))) " //
                                + "WHERE %5$s = NEW.%5$s", //
                        ft.getQuotedName(), // 1 table "FULLTEXT"
                        ftft.getQuotedName(), // 2 column "TEXT"
                        ftst.getQuotedName(), // 3 column "SIMPLETEXT"
                        ftbt.getQuotedName(), // 4 column "BINARYTEXT"
                        ftid.getQuotedName() // 5 column "ID"
                )));

        return statements;
    }

    private ConditionalStatement makeFunction(String functionName,
            String proto, String methodName, String info) {
        return new ConditionalStatement(
                true, // early
                null, // do a drop check
                String.format(
                        "SELECT ALIAS FROM SYS.SYSALIASES WHERE ALIAS = '%s' AND ALIASTYPE = 'F'",
                        functionName), //
                String.format("DROP FUNCTION %s", functionName), //
                String.format("CREATE FUNCTION %s%s " //
                        + "LANGUAGE JAVA " //
                        + "PARAMETER STYLE JAVA " //
                        + "EXTERNAL NAME '%s.%s' " //
                        + "%s", //
                        functionName, proto, //
                        className, methodName, info));
    }

    private ConditionalStatement makeTrigger(String triggerName, String body) {
        return new ConditionalStatement(
                false, // late
                null, // do a drop check
                String.format(
                        "SELECT TRIGGERNAME FROM SYS.SYSTRIGGERS WHERE TRIGGERNAME = '%s'",
                        triggerName), //
                String.format("DROP TRIGGER %s", triggerName), //
                String.format("CREATE TRIGGER %s %s", triggerName, body));

    }

    @Override
    public Collection<ConditionalStatement> getTestConditionalStatements(
            Model model, Database database) {
        List<ConditionalStatement> statements = new LinkedList<ConditionalStatement>();
        statements.add(new ConditionalStatement(true, Boolean.FALSE, null,
                null,
                // here use a CLOB instead of a VARCHAR(32672) to test
                // compatibility
                "CREATE TABLE TESTSCHEMA2 (ID VARCHAR(36) NOT NULL, TITLE CLOB)"));
        statements.add(new ConditionalStatement(true, Boolean.FALSE, null,
                null,
                "ALTER TABLE TESTSCHEMA2 ADD CONSTRAINT TESTSCHEMA2_PK PRIMARY KEY (ID)"));
        return statements;
    }

}
