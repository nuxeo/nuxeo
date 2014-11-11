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
import org.nuxeo.ecm.core.storage.sql.RepositoryDescriptor;

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

    protected final String fulltextAnalyzer;

    private static final String DEFAULT_FULLTEXT_ANALYSER_PG = "english";

    private static final String DEFAULT_FULLTEXT_ANALYSER_H2 = "org.apache.lucene.analysis.standard.StandardAnalyzer";

    /**
     * Creates a {@code Dialect} by connecting to the datasource to check what
     * database is used.
     *
     * @throws StorageException if a SQL connection problem occurs
     */
    public Dialect(Connection connection,
            RepositoryDescriptor repositoryDescriptor) throws StorageException {
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
        String analyzer = repositoryDescriptor.fulltextAnalyzer;
        if (analyzer == null) {
            // suitable defaults
            if (dialect instanceof PostgreSQLDialect) {
                analyzer = DEFAULT_FULLTEXT_ANALYSER_PG;
            }
            if (dialect instanceof H2Dialect) {
                analyzer = DEFAULT_FULLTEXT_ANALYSER_H2;
            }
        }
        fulltextAnalyzer = analyzer;
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
        if (sqlType == Column.ExtendedTypes.FULLTEXT) {
            if (dialect instanceof PostgreSQLDialect) {
                return "tsvector";
            }
            sqlType = Types.CLOB;
        }
        if (dialect instanceof DerbyDialect && sqlType == Types.CLOB) {
            return "clob"; // different from DB2Dialect
        }
        return dialect.getTypeName(sqlType, length, precision, scale);
    }

    /**
     * Gets the type of a fulltext column has known by JDBC.
     * <p>
     * This is used for setNull.
     */
    public int getFulltextType() {
        // see also getTypeName
        if (dialect instanceof PostgreSQLDialect) {
            return Types.OTHER;
        }
        return Types.CLOB;
    }

    /**
     * Gets the JDBC expression setting a free value for this column type.
     * <p>
     * Needed for columns that need an expression around the value being set,
     * usually for conversion (this is the case for PostgreSQL fulltext {@code
     * TSVECTOR} columns for instance).
     *
     * @param type the JDBC or extended type
     * @return the expression containing a free variable
     */
    public String getFreeVariableSetterForType(int type) {
        if (type == Column.ExtendedTypes.FULLTEXT &&
                dialect instanceof PostgreSQLDialect) {
            return "NX_TO_TSVECTOR(?)";
        }
        return "?";
    }

    /**
     * Gets the fulltext analyzer configured.
     * <p>
     * For PostgreSQL, it's a text search configuration name.
     */
    public String getFulltextAnalyzer() {
        return fulltextAnalyzer;
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
     * @param inOrderBy {@code true} if the expression is for an ORDER BY column
     * @return a pattern for String.format with one parameter for the column
     *         name and one for the width
     */
    public String getClobCast(boolean inOrderBy) {
        if (dialect instanceof DerbyDialect) {
            return "CAST(%s AS VARCHAR(%d))";
        }
        if (dialect instanceof H2Dialect && !inOrderBy) {
            return "CAST(%s AS VARCHAR)";
        }
        return null;
    }

    /**
     * Gets the expression to use to check security.
     *
     * @param the quoted name of the id column to use
     * @return an SQL expression with two parameters (principals and
     *         permissions) that is true if access is allowed
     */
    public String getSecurityCheckSql(String idColumnName) {
        String sql = String.format("NX_ACCESS_ALLOWED(%s, ?, ?)", idColumnName);
        if (dialect instanceof DerbyDialect) {
            // dialect has no boolean functions
            sql += " = 1";
        }
        return sql;
    }

    /**
     * Checks if the fulltext table is needed in queries.
     * <p>
     * This won't be the case if {@link #getFulltextMatch} returns a join that
     * already does the job.
     */
    public boolean isFulltextTableNeeded() {
        return !(dialect instanceof H2Dialect);
    }

    /**
     * Gets the information needed to do a a fulltext match, either with a
     * direct expression in the WHERE clause, or using a join with an additional
     * table.
     * <p>
     * Returns a String array with:
     * <ul>
     * <li>the expression to join with, or {@code null}; this expression has one
     * % parameters for the table alias,</li>
     * <li>a potential query paramenter for it,</li>
     * <li>the where expression to add to the WHERE clause (may be {@code null}
     * if none is required when a join is enough); this expression has one %
     * parameters for the table alias,</li>
     * <li>a potential query paramenter for it.</li>
     * </ul>
     *
     * @param ftColumn the column containing the fulltext to match
     * @param mainColumn the column with the main id, for joins
     * @param fulltextQuery the query to do
     * @return a String array with the table join expression, the join param,
     *         the where expression and the where parm
     *
     */
    public String[] getFulltextMatch(Column ftColumn, Column mainColumn,
            String fulltextQuery) {
        if (dialect instanceof DerbyDialect) {
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
        if (dialect instanceof H2Dialect) {
            String queryTable = String.format("NXFT_SEARCH('%s', '%s', ?)",
                    "PUBLIC", ftColumn.getTable().getName());
            String whereExpr = String.format("%%s.KEY = %s",
                    mainColumn.getFullQuotedName());
            return new String[] { (queryTable + " %s"), fulltextQuery,
                    whereExpr, null };
        }
        if (dialect instanceof PostgreSQLDialect) {
            String whereExpr = String.format("NX_CONTAINS(%s, ?)",
                    ftColumn.getFullQuotedName());
            return new String[] { null, null, whereExpr, fulltextQuery };
        }
        throw new UnsupportedOperationException();
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
