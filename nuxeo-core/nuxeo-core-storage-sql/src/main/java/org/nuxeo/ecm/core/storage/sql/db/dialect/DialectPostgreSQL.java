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

import java.sql.Array;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.hibernate.dialect.PostgreSQLDialect;
import org.nuxeo.ecm.core.storage.StorageException;
import org.nuxeo.ecm.core.storage.sql.Model;
import org.nuxeo.ecm.core.storage.sql.RepositoryDescriptor;
import org.nuxeo.ecm.core.storage.sql.db.Column;
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
    public String getTypeName(int sqlType, int length, int precision, int scale) {
        if (sqlType == Column.ExtendedTypes.FULLTEXT) {
            return "tsvector";
        }
        return super.getTypeName(sqlType, length, precision, scale);
    }

    @Override
    public String getCreateFulltextIndexSql(String indexName, String tableName,
            List<String> columnNames) {
        return String.format("CREATE INDEX %s ON %s USING GIN(%s)", indexName,
                tableName, columnNames.get(0));
    }

    @Override
    public String[] getFulltextMatch(Column ftColumn, Column mainColumn,
            String fulltextQuery) {
        String whereExpr = String.format("NX_CONTAINS(%s, ?)",
                ftColumn.getFullQuotedName());
        return new String[] { null, null, whereExpr, fulltextQuery };
    }

    @Override
    public int getFulltextIndexedColumns() {
        return 1;
    }

    @Override
    public int getFulltextType() {
        return Types.OTHER;
    }

    @Override
    public String getFreeVariableSetterForType(int type) {
        if (type == Column.ExtendedTypes.FULLTEXT) {
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
    public boolean supportsIlike() {
        return true;
    }

    @Override
    public Array createArrayOf(int type, Object[] elements,
            Connection connection) throws SQLException {
        if (elements == null || elements.length == 0) {
            return null;
        }
        String typeName = getTypeName(type, 0, 0, 0);
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
        Column ftft = ft.getColumn(model.FULLTEXT_FULLTEXT_KEY);
        Column ftst = ft.getColumn(model.FULLTEXT_SIMPLETEXT_KEY);
        Column ftbt = ft.getColumn(model.FULLTEXT_BINARYTEXT_KEY);

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

        statements.add(new ConditionalStatement( //
                false, // late
                Boolean.FALSE, // no drop needed
                null, //
                null, //
                String.format(
                        "CREATE OR REPLACE FUNCTION NX_UPDATE_FULLTEXT() " //
                                + "RETURNS trigger " //
                                + "AS $$ " //
                                + "BEGIN" //
                                + "  NEW.%s := NEW.%s || NEW.%s;" //
                                + "  RETURN NEW; " //
                                + "END " //
                                + "$$ " //
                                + "LANGUAGE plpgsql " //
                                + "VOLATILE " //
                        , ftft.getQuotedName(), //
                        ftst.getQuotedName(), //
                        ftbt.getQuotedName())));

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

}
