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

import java.lang.reflect.Constructor;
import java.sql.Array;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;
import java.sql.Types;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import org.hibernate.dialect.Oracle9Dialect;
import org.nuxeo.ecm.core.storage.StorageException;
import org.nuxeo.ecm.core.storage.sql.Model;
import org.nuxeo.ecm.core.storage.sql.RepositoryDescriptor;
import org.nuxeo.ecm.core.storage.sql.db.Column;
import org.nuxeo.ecm.core.storage.sql.db.Database;

/**
 * Oracle-specific dialect.
 *
 * @author Florent Guillaume
 */
public class DialectOracle extends Dialect {

    public DialectOracle(DatabaseMetaData metadata,
            RepositoryDescriptor repositoryDescriptor) throws StorageException {
        super(new Oracle9Dialect(), metadata);
    }

    @Override
    protected int getMaxNameSize() {
        return 30;
    }

    @Override
    public String getTypeName(int sqlType, int length, int precision, int scale) {
        if (sqlType == Column.ExtendedTypes.FULLTEXT) {
            return "CLOB";
        }
        if (sqlType == Types.VARCHAR) {
            if (length == 36) {
                // uuid
                return "VARCHAR2(36)";
            } else {
                return "NVARCHAR2(" + length + ')';
            }
        }
        if (sqlType == Types.CLOB) {
            // return "NCLOB";
            return "NVARCHAR2(2000)"; // until we get finer-grained config
        }
        return super.getTypeName(sqlType, length, precision, scale);
    }

    @Override
    public String getCreateFulltextIndexSql(String indexName, String tableName,
            List<String> columnNames) {
        return String.format(
                "CREATE INDEX %s ON %s(%s) INDEXTYPE IS CTXSYS.CONTEXT "
                        + "PARAMETERS('SYNC (ON COMMIT) TRANSACTIONAL')",
                indexName, tableName, columnNames.get(0));
    }

    @Override
    public String[] getFulltextMatch(Column ftColumn, Column mainColumn,
            String fulltextQuery) {
        String whereExpr = String.format("CONTAINS(%s, ?) > 0",
                ftColumn.getFullQuotedName());
        return new String[] { null, null, whereExpr, fulltextQuery };
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
                true, // early
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
                true, // early
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

        statements.add(new ConditionalStatement(
                false, // late
                Boolean.FALSE, // no drop
                null, //
                null, //
                "CREATE OR REPLACE TRIGGER NX_TRIG_FT_UPDATE " //
                        + "BEFORE INSERT OR UPDATE ON \"FULLTEXT\" "
                        + "FOR EACH ROW " //
                        + "BEGIN" //
                        + "  :NEW.FULLTEXT := :NEW.SIMPLETEXT || :NEW.BINARYTEXT; " //
                        + "END;" //
        ));

        return statements;
    }

}
