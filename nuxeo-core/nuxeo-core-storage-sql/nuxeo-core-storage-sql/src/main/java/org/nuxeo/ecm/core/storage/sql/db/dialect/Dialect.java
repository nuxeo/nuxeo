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
import java.security.MessageDigest;
import java.sql.Array;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.Collection;
import java.util.List;

import org.nuxeo.common.utils.StringUtils;
import org.nuxeo.ecm.core.storage.StorageException;
import org.nuxeo.ecm.core.storage.sql.Model;
import org.nuxeo.ecm.core.storage.sql.RepositoryDescriptor;
import org.nuxeo.ecm.core.storage.sql.db.Column;
import org.nuxeo.ecm.core.storage.sql.db.ColumnType;
import org.nuxeo.ecm.core.storage.sql.db.Database;
import org.nuxeo.ecm.core.storage.sql.db.Table;

/**
 * A Dialect encapsulates knowledge about database-specific behavior.
 *
 * @author Florent Guillaume
 */
public abstract class Dialect {

    public static final class JDBCInfo {
        public final String string;

        public final int jdbcType;

        public JDBCInfo(String string, int jdbcType) {
            this.string = string;
            this.jdbcType = jdbcType;
        }
    }

    public JDBCInfo jdbcInfo(String string, int jdbcType) {
        return new JDBCInfo(string, jdbcType);
    }

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

    public Dialect(DatabaseMetaData metadata) throws StorageException {
        try {
            storesUpperCaseIdentifiers = metadata.storesUpperCaseIdentifiers();
        } catch (SQLException e) {
            throw new StorageException("An error has occured.", e);
        }
    }

    /**
     * Gets the JDBC type and string from Nuxeo's type abstraction.
     */
    public abstract JDBCInfo getJDBCTypeAndString(ColumnType type);

    /**
     * Check mismatches between expected and actual JDBC types read from
     * database introspection.
     */
    public boolean isAllowedConversion(int expected, int actual,
            String actualName, int actualSize) {
        return false;
    }

    public abstract void setToPreparedStatement(PreparedStatement ps,
            int index, Serializable value, Column column) throws SQLException;

    public abstract Serializable getFromResultSet(ResultSet rs, int index,
            Column column) throws SQLException;

    public boolean storesUpperCaseIdentifiers() {
        return storesUpperCaseIdentifiers;
    }

    public char openQuote() {
        return '"';
    }

    public char closeQuote() {
        return '"';
    }

    public String toBooleanValueString(boolean bool) {
        return bool ? "1" : "0";
    }

    protected int getMaxNameSize() {
        return 999;
    }

    protected int getMaxIndexNameSize() {
        return 999;
    }

    protected String makeName(String prefix, String string, String suffix,
            int maxNameSize) {
        int length = prefix.length() + string.length() + suffix.length();

        try {

            StringBuilder sb = new StringBuilder(length);

            if (length > maxNameSize) {

                MessageDigest digest = MessageDigest.getInstance("MD5");

                byte[] bytes = (prefix + string).getBytes();
                digest.update(bytes, 0, bytes.length);

                sb.append(prefix.substring(0, 4));
                sb.append('_');
                sb.append(toHexString(digest.digest()).substring(0, 8));
            }

            else {
                sb.append(prefix).append(string);
            }

            sb.append(storesUpperCaseIdentifiers() ? suffix
                    : suffix.toLowerCase());
            return sb.toString();
        }

        catch (Exception e) {
            throw new RuntimeException("Error", e);
        }
    }

    protected static final char[] HEX_DIGITS = "0123456789ABCDEF".toCharArray();

    public static String toHexString(byte[] bytes) {
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
                + foreignTableName, "_FK", getMaxNameSize());
    }

    public String getIndexName(String tableName, List<String> columnNames) {
        return makeName(qualifyIndexName() ? tableName + '_' : "",
                StringUtils.join(columnNames, '_'), "_IDX",
                getMaxIndexNameSize());
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
     * Does the fulltext synthetic column have to be materialized.
     */
    public abstract boolean getMaterializeFulltextSyntheticColumn();

    /**
     * Gets a CREATE INDEX statement for a fulltext index.
     */
    public abstract String getCreateFulltextIndexSql(String indexName,
            String quotedIndexName, String tableName, List<String> columnNames);

    /**
     * Get the dialect-specific version of a fulltext query.
     *
     * @param query the CMIS-syntax-based fulltext query string
     * @return the dialect native fulltext query string
     */
    public abstract String getDialectFulltextQuery(String query);

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
     * @param indexName the fulltext index name to match
     * @param fulltextQuery the query to do
     * @param mainColumn the column with the main id, for joins
     * @param model the model
     * @param database the database
     * @return a String array with the table join expression, the join param,
     *         the where expression and the where parm
     *
     */
    public abstract String[] getFulltextMatch(String indexName,
            String fulltextQuery, Column mainColumn, Model model,
            Database database);

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
     * @param type the column type
     * @return the expression containing a free variable
     */
    public String getFreeVariableSetterForType(ColumnType type) {
        return "?";
    }

    public String getNoColumnsInsertString() {
        return "VALUES ( )";
    }

    public String getNullColumnString() {
        return "";
    }

    public String getTableTypeString(Table table) {
        return "";
    }

    public String getAddPrimaryKeyConstraintString(String constraintName) {
        return " ADD CONSTRAINT" + constraintName + " PRIMARY KEY ";
    }

    public String getAddForeignKeyConstraintString(String constraintName,
            String[] foreignKeys, String referencedTable, String[] primaryKeys,
            boolean referencesPrimaryKey) {
        String sql = String.format(
                " ADD CONSTRAINT %s FOREIGN KEY (%s) REFERENCES %s",
                constraintName, StringUtils.join(foreignKeys, ", "),
                referencedTable);
        if (!referencesPrimaryKey) {
            sql += " (" + StringUtils.join(primaryKeys, ", ") + ')';
        }
        return sql;
    }

    public boolean qualifyIndexName() {
        return true;
    }

    public boolean supportsIfExistsBeforeTableName() {
        return false;
    }

    public boolean supportsIfExistsAfterTableName() {
        return false;
    }

    public String getCascadeDropConstraintsString() {
        return "";
    }

    public boolean supportsCircularCascadeDeleteConstraints() {
        // false for MS SQL Server
        return true;
    }

    public String getAddColumnString() {
        return "ADD COLUMN";
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
     * Whether a derived table (subselect in a FROM statement) needs an alias.
     */
    public boolean needsAliasForDerivedTable() {
        return false;
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

    public abstract Collection<ConditionalStatement> getTestConditionalStatements(
            Model model, Database database);

    /**
     * Checks that clustering is supported.
     */
    public boolean isClusteringSupported() {
        return false;
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

    /**
     * Does the dialect support an optimized read security checks
     *
     */
    public boolean supportsReadAcl() {
        return false;
    }

    /**
     * Gets the statement to update the read acls
     *
     */
    public String getUpdateReadAclsSql() {
        return null;
    }

    /**
     * Gets the statement to rebuild the wall read acls
     *
     */
    public String getRebuildReadAclsSql() {
        return null;
    }

    /**
     * Gets the expression to check if access is allowed using read acl the
     * dialect must suppportsReadAcl
     *
     * @param idColumnName the quoted name of the read acl_id column to use
     * @return an SQL expression with one parameter (principals) that is true if
     *         access is allowed
     */
    public String getReadAclsCheckSql(String idColumnName) {
        return null;
    }

}
