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

import java.sql.DatabaseMetaData;
import java.sql.Types;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import org.hibernate.dialect.DerbyDialect;
import org.nuxeo.ecm.core.storage.StorageException;
import org.nuxeo.ecm.core.storage.sql.Model;
import org.nuxeo.ecm.core.storage.sql.RepositoryDescriptor;
import org.nuxeo.ecm.core.storage.sql.db.Column;
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
        super(new DerbyDialect(), metadata);
    }

    @Override
    public String getTypeName(int sqlType, int length, int precision, int scale) {
        if (sqlType == Column.ExtendedTypes.FULLTEXT) {
            sqlType = Types.CLOB;
        }
        if (sqlType == Types.CLOB) {
            return "clob"; // different from DB2Dialect
        }
        return super.getTypeName(sqlType, length, precision, scale);
    }

    @Override
    public int getFulltextIndexedColumns() {
        return 0;
    }

    @Override
    public String getCreateFulltextIndexSql(String indexName, String tableName,
            List<String> columnNames) {
        throw new UnsupportedOperationException();
    }

    @Override
    public String[] getFulltextMatch(Column ftColumn, Column mainColumn,
            String fulltextQuery) {
        String qname = ftColumn.getFullQuotedName();
        if (ftColumn.getSqlType() == Types.CLOB) {
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

}
