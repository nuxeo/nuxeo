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

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.Array;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Types;
import java.util.Collection;
import java.util.List;

import org.hibernate.exception.SQLExceptionConverter;
import org.nuxeo.common.utils.StringUtils;
import org.nuxeo.ecm.core.storage.StorageException;
import org.nuxeo.ecm.core.storage.sql.Model;
import org.nuxeo.ecm.core.storage.sql.RepositoryDescriptor;
import org.nuxeo.ecm.core.storage.sql.db.Column;
import org.nuxeo.ecm.core.storage.sql.db.Database;
import org.nuxeo.ecm.core.storage.sql.db.Table;

/**
 * A Dialect encapsulates knowledge about database-specific behavior.
 *
 * @author Florent Guillaume
 */
public abstract class Dialect {

    protected final org.hibernate.dialect.Dialect dialect;

    protected final boolean storesUpperCaseIdentifiers;

    /**
     * Creates a {@code Dialect} by connecting to the datasource to check what
     * database is used.
     *
     * @throws StorageException if a SQL connection problem occurs
     */
    public static Dialect createDialect(Connection connection,
            RepositoryDescriptor repositoryDescriptor) throws StorageException {
        DatabaseMetaData metadata;
        String databaseName;
        try {
            metadata = connection.getMetaData();
            databaseName = metadata.getDatabaseProductName();
        } catch (SQLException e) {
            throw new StorageException(e);
        }
        if ("Apache Derby".equals(databaseName)) {
            return new DialectDerby(metadata, repositoryDescriptor);
        }
        if ("H2".equals(databaseName)) {
            return new DialectH2(metadata, repositoryDescriptor);
        }
        if ("MySQL".equals(databaseName)) {
            return new DialectMySQL(metadata, repositoryDescriptor);
        }
        if ("Oracle".equals(databaseName)) {
            return new DialectOracle(metadata, repositoryDescriptor);
        }
        if ("PostgreSQL".equals(databaseName)) {
            return new DialectPostgreSQL(metadata, repositoryDescriptor);
        }
        if ("Microsoft SQL Server".equals(databaseName)) {
            return new DialectSQLServer(metadata, repositoryDescriptor);
        }
        throw new StorageException("Unsupported database: " + databaseName);
    }

    public Dialect(org.hibernate.dialect.Dialect dialect,
            DatabaseMetaData metadata) throws StorageException {
        this.dialect = dialect;
        try {
            storesUpperCaseIdentifiers = metadata.storesUpperCaseIdentifiers();
        } catch (SQLException e) {
            throw new StorageException(e);
        }
    }

    public boolean storesUpperCaseIdentifiers() {
        return storesUpperCaseIdentifiers;
    }

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

    protected int getMaxNameSize() {
        return 999;
    }

    protected String makeName(String prefix, String string, String suffix) {
        if (prefix.length() + string.length() + suffix.length() > getMaxNameSize()) {
            MessageDigest digest;
            try {
                digest = MessageDigest.getInstance("MD5");
            } catch (NoSuchAlgorithmException e) {
                throw new RuntimeException(e.toString(), e);
            }
            byte[] bytes = string.getBytes();
            digest.update(bytes, 0, bytes.length);
            string = toHexString(digest.digest()).substring(0, 8);
        }
        suffix = storesUpperCaseIdentifiers() ? suffix : suffix.toLowerCase();
        return prefix + string + suffix;
    }

    protected static final char[] HEX_DIGITS = "0123456789ABCDEF".toCharArray();

    protected static String toHexString(byte[] bytes) {
        StringBuilder buf = new StringBuilder(2 * bytes.length);
        for (byte b : bytes) {
            buf.append(HEX_DIGITS[(0xF0 & b) >> 4]);
            buf.append(HEX_DIGITS[0x0F & b]);
        }
        return buf.toString();
    }

    public String getForeignKeyConstraintName(String tableName,
            String foreignColumnName, String foreignTableName) {
        return makeName(tableName + '_', foreignColumnName + '_'
                + foreignTableName, "_FK");
    }

    public String getIndexName(String tableName, List<String> columnNames) {
        return makeName(qualifyIndexName() ? tableName + '_' : "",
                StringUtils.join(columnNames, '_'), "_IDX");
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
            sqlType = Types.CLOB;
        }
        return dialect.getTypeName(sqlType, length, precision, scale);
    }

    /**
     * Gets a CREATE INDEX statement for a normal index.
     */
    public String getCreateIndexSql(String indexName, String tableName,
            List<String> columnNames) {
        return String.format("CREATE INDEX %s ON %s (%s)", indexName,
                tableName, StringUtils.join(columnNames, ", "));
    }

    /**
     * Specifies what columns of the fulltext table have to be indexed.
     *
     * @return 0 for none, 1 for the synthetic one, 2 for the individual ones
     */
    public abstract int getFulltextIndexedColumns();

    /**
     * Gets a CREATE INDEX statement for a fulltext index.
     */
    public abstract String getCreateFulltextIndexSql(String indexName,
            String tableName, List<String> columnNames);

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
    public abstract String[] getFulltextMatch(Column ftColumn,
            Column mainColumn, String fulltextQuery);

    /**
     * Gets the type of a fulltext column has known by JDBC.
     * <p>
     * This is used for setNull.
     */
    public int getFulltextType() {
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
        return "?";
    }

    public String getNoColumnsInsertString() {
        return dialect.getNoColumnsInsertString();
    }

    public String getNullColumnString() {
        return dialect.getNullColumnString();
    }

    public String getTableTypeString(Table table) {
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

    public String getCascadeDropConstraintsString() {
        return dialect.getCascadeConstraintsString();
    }

    public boolean supportsCircularCascadeDeleteConstraints() {
        // false for MS SQL Server
        return true;
    }

    public String getAddColumnString() {
        // "ADD COLUMN" or "ADD"
        return dialect.getAddColumnString().toUpperCase();
    }

    /**
     * Does the dialect support UPDATE t SET ... FROM t, u WHERE ... ?
     */
    public abstract boolean supportsUpdateFrom();

    /**
     * When doing an UPDATE t SET ... FROM t, u WHERE ..., does the FROM clause
     * need to repeate the updated table (t).
     */
    public abstract boolean doesUpdateFromRepeatSelf();

    /**
     * When doing a SELECT DISTINCT that uses a ORDER BY, do the keys along
     * which we order have to be mentioned in the DISTINCT clause?
     */
    public boolean needsOrderByKeysAfterDistinct() {
        return true;
    }

    /**
     * When using a CLOB field in an expression, is some casting required and
     * with what pattern?
     * <p>
     * Needed for Derby and H2.
     *
     * @param inOrderBy {@code true} if the expression is for an ORDER BY column
     * @return a pattern for String.format with one parameter for the column
     *         name and one for the width, or {@code null} if no cast is
     *         required
     */
    public String getClobCast(boolean inOrderBy) {
        return null;
    }

    /**
     * Gets the expression to use to check security.
     *
     * @param idColumnName the quoted name of the id column to use
     * @return an SQL expression with two parameters (principals and
     *         permissions) that is true if access is allowed
     */
    public abstract String getSecurityCheckSql(String idColumnName);

    /**
     * Gets the expression to use to check tree membership.
     *
     * @param idColumnName the quoted name of the id column to use
     * @return an SQL expression with one parameters for the based id that is
     *         true if the document is under base id
     */
    public abstract String getInTreeSql(String idColumnName);

    /**
     * Checks if the fulltext table is needed in queries.
     * <p>
     * This won't be the case if {@link #getFulltextMatch} returns a join that
     * already does the job.
     */
    public boolean isFulltextTableNeeded() {
        return true;
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
        return false;
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
     * @param connection the connection
     * @return an Array holding the elements
     */
    public Array createArrayOf(int type, Object[] elements,
            Connection connection) throws SQLException {
        throw new SQLException("Not supported");
    }

    /**
     * Gets the additional statements to execute (stored procedures and
     * triggers) when creating the database.
     */
    public abstract Collection<ConditionalStatement> getConditionalStatements(
            Model model, Database database);

    /**
     * Gets the type of the column containing the cluster node id.
     */
    public int getClusterNodeType() throws StorageException {
        throw new StorageException("Clustering not implemented for "
                + dialect.getClass().getSimpleName());
    }

    /**
     * Gets the type of the column containing the cluster fragments.
     */
    public int getClusterFragmentsType() throws StorageException {
        return 0;
    }

    /**
     * Gets a dialect-specific string for the type of the cluster fragments
     * column.
     */
    public String getClusterFragmentsTypeString() {
        return null;
    }

    /**
     * Gets the SQL to cleanup info about old (crashed) cluster nodes.
     */
    public String getCleanupClusterNodesSql(Model model, Database database) {
        return null;
    }

    /**
     * Gets the SQL to create a cluster node.
     */
    public String getCreateClusterNodeSql(Model model, Database database) {
        return null;
    }

    /**
     * Gets the SQL to remove a node from the cluster.
     */
    public String getRemoveClusterNodeSql(Model model, Database database) {
        return null;
    }

    /**
     * Gets the SQL to send an invalidation to the cluster.
     *
     * @return an SQL statement with parameters for: id, fragments, kind
     */
    public String getClusterInsertInvalidations() {
        return null;
    }

    /**
     * Gets the SQL to query invalidations for this cluster node.
     *
     * @return an SQL statement returning a result set
     */
    public String getClusterGetInvalidations() {
        return null;
    }

}
