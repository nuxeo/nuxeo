/*
 * (C) Copyright 2008 Nuxeo SAS (http://nuxeo.com/) and contributors.
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

package org.nuxeo.ecm.core.storage.sql.db;

import java.sql.Array;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.Map;

import org.hibernate.HibernateException;
import org.hibernate.dialect.DerbyDialect;
import org.hibernate.dialect.DialectFactory;
import org.hibernate.dialect.H2Dialect;
import org.hibernate.dialect.MySQLDialect;
import org.hibernate.dialect.PostgreSQLDialect;
import org.hibernate.dialect.SQLServerDialect;
import org.hibernate.exception.SQLExceptionConverter;
import org.nuxeo.ecm.core.storage.StorageException;

/**
 * A Dialect encapsulates knowledge about database-specific behavior.
 *
 * @author Florent Guillaume
 */
public class Dialect {

    private final String databaseName;

    public final int databaseMajor;

    protected final org.hibernate.dialect.Dialect dialect;

    public final String dialectName;

    protected final boolean storesUpperCaseIdentifiers;

    /**
     * Creates a {@code Dialect} by connecting to the datasource to check what
     * database is used.
     *
     * @throws StorageException if a SQL connection problem occurs
     */
    public Dialect(Connection connection) throws StorageException {
        try {
            DatabaseMetaData metadata = connection.getMetaData();
            databaseName = metadata.getDatabaseProductName();
            databaseMajor = metadata.getDatabaseMajorVersion();
            storesUpperCaseIdentifiers = metadata.storesUpperCaseIdentifiers();
        } catch (SQLException e) {
            throw new StorageException(e);
        }
        if ("H2".equals(databaseName)) {
            try {
                dialect = new H2Dialect();
            } catch (Exception e) {
                throw new StorageException("Cannot instantiate dialect for: " +
                        connection, e);
            }
        } else {
            try {
                dialect = DialectFactory.determineDialect(databaseName,
                        databaseMajor);
            } catch (HibernateException e) {
                throw new StorageException("Cannot determine dialect for: " +
                        connection, e);
            }
        }
        dialectName = dialect.getClass().getSimpleName();
    }

    public String getDatabaseName() {
        return databaseName;
    }

    @Override
    public String toString() {
        return dialectName;
    }

    /*
     * ----- DatabaseMetaData info -----
     */

    public boolean storesUpperCaseIdentifiers() {
        return storesUpperCaseIdentifiers;
    }

    /*
     * ----- Delegates to Hibernate -----
     */

    public char openQuote() {
        return dialect.openQuote();
    }

    public char closeQuote() {
        return dialect.closeQuote();
    }

    public SQLExceptionConverter buildSQLExceptionConverter() {
        return dialect.buildSQLExceptionConverter();
    }

    public String toBooleanValueString(boolean bool) {
        return dialect.toBooleanValueString(bool);
    }

    public String getIdentitySelectString(String table, String column,
            int sqlType) {
        return dialect.getIdentitySelectString(table, column, sqlType);
    }

    public boolean hasDataTypeInIdentityColumn() {
        return dialect.hasDataTypeInIdentityColumn();
    }

    public String getIdentityColumnString(int sqlType) {
        return dialect.getIdentityColumnString(sqlType);
    }

    public String getTypeName(int sqlType, int length, int precision, int scale) {
        String typeName;
        if (dialect instanceof DerbyDialect && sqlType == Types.CLOB) {
            typeName = "clob"; // skip size
        } else {
            typeName = dialect.getTypeName(sqlType, length, precision, scale);
        }
        return typeName;
    }

    public String getNoColumnsInsertString() {
        return dialect.getNoColumnsInsertString();
    }

    public String getNullColumnString() {
        return dialect.getNullColumnString();
    }

    // this is just for MySQL to add its ENGINE=InnoDB
    public String getTableTypeString() {
        return dialect.getTableTypeString();
    }

    public String getAddPrimaryKeyConstraintString(String constraintName) {
        return dialect.getAddPrimaryKeyConstraintString(constraintName);
    }

    public String getAddForeignKeyConstraintString(String constraintName,
            String[] foreignKeys, String referencedTable, String[] primaryKeys,
            boolean referencesPrimaryKey) {
        return dialect.getAddForeignKeyConstraintString(constraintName,
                foreignKeys, referencedTable, primaryKeys, referencesPrimaryKey);
    }

    public boolean qualifyIndexName() {
        return dialect.qualifyIndexName();
    }

    public boolean supportsIfExistsBeforeTableName() {
        return dialect.supportsIfExistsBeforeTableName();
    }

    public boolean supportsIfExistsAfterTableName() {
        return dialect.supportsIfExistsAfterTableName();
    }

    public String getCascadeConstraintsString() {
        return dialect.getCascadeConstraintsString();
    }

    // "ADD COLUMN" or "ADD"
    public String getAddColumnString() {
        return dialect.getAddColumnString().toUpperCase();
    }

    /**
     * Does the dialect support UPDATE t SET ... FROM t, u WHERE ... ?
     */
    public boolean supportsUpdateFrom() {
        if (dialect instanceof PostgreSQLDialect ||
                dialect instanceof MySQLDialect ||
                dialect instanceof SQLServerDialect) {
            return true;
        }
        if (dialect instanceof DerbyDialect) {
            return false;
        }
        // others unknown
        return false;
    }

    /**
     * When doing an UPDATE t SET ... FROM t, u WHERE ..., does the FROM clause
     * need to repeate the updated table (t).
     */
    public boolean doesUpdateFromRepeatSelf() {
        if (dialect instanceof PostgreSQLDialect) {
            return false;
        }
        if (dialect instanceof MySQLDialect ||
                dialect instanceof SQLServerDialect) {
            return true;
        }
        // not reached
        return true;
    }

    public boolean needsOrderByKeysAfterDistinct() {
        return dialect instanceof PostgreSQLDialect ||
                dialect instanceof H2Dialect;
    }

    /**
     * When using a CLOB field in an expression, is some casting required and
     * with what pattern?
     * <p>
     * Needed for Derby and H2.
     *
     * @return a pattern for String.format with one parameter for the column
     *         name and one for the width
     */
    public String clobCasting() {
        if (dialect instanceof DerbyDialect) {
            return "CAST(%s AS VARCHAR(%d))";
        }
        if (dialect instanceof H2Dialect) {
            return "CAST(%s AS VARCHAR)";
        }
        return null;
    }

    /**
     * When using a CLOB field in ORDER BY, is some casting required and with
     * what pattern?
     * <p>
     * Needed for Derby.
     *
     * @return a pattern for String.format with one parameter for the column
     *         name and one for the width
     */
    public String clobCastingInOrderBy() {
        if (dialect instanceof DerbyDialect) {
            return "CAST(%s AS VARCHAR(%d))";
        }
        return null;
    }

    /**
     * Does the dialect support passing ARRAY values (to stored procedures
     * mostly).
     * <p>
     * If not, we'll simulate them using a string and a separator.
     *
     * @return true if ARRAY values are supported
     */
    public boolean supportsArrays() {
        return dialect instanceof PostgreSQLDialect;
    }

    /**
     * Factory method for creating Array objects, suitable for passing to
     * {@link PreparedStatement#setArray}.
     * <p>
     * (An equivalent method is defined by JDBC4 on the {@link Connection}
     * class.)
     *
     * @param type the SQL type of the elements
     * @param elements the elements of the array
     * @return an Array holding the elements
     */
    public Array createArrayOf(int type, Object[] elements) throws SQLException {
        if (dialect instanceof PostgreSQLDialect) {
            if (elements == null || elements.length == 0) {
                return null;
            }
            String typeName = getTypeName(type, 0, 0, 0);
            return new PostgreSQLArray(type, typeName, elements);
        }
        throw new SQLException("Not supported");
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

        // this is needed by Java 6
        public void free() {
        }
    }

}
