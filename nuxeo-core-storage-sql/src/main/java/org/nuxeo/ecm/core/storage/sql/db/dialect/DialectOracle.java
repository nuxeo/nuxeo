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
            sqlType = Types.CLOB;
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
            return "NCLOB";
        }
        return super.getTypeName(sqlType, length, precision, scale);
    }

    @Override
    public String getCreateFulltextIndexSql(String indexName, String tableName,
            List<String> columnNames) {
        StringBuilder buf = new StringBuilder();
        buf.append("CREATE INDEX ON ");
        buf.append(tableName);
        return buf.toString();
    }

    @Override
    public String[] getFulltextMatch(Column ftColumn, Column mainColumn,
            String fulltextQuery) {
        throw new UnsupportedOperationException();
    }

    @Override
    public int getFulltextIndexedColumns() {
        return 0; // TODO
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
                                + "(id %s, users VARCHAR2, permissions VARCHAR2) " //
                                + "RETURN NUMBER IS " //
                                + "BEGIN" //
                                + "  RETURN 1; " // TODO
                                + "END;" //
                        , idType)));

        return statements;
    }

}
