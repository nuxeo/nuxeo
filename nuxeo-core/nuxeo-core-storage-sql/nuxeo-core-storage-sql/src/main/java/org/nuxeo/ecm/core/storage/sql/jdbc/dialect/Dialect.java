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

package org.nuxeo.ecm.core.storage.sql.jdbc.dialect;

import java.io.Serializable;
import java.net.SocketException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.Array;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.nuxeo.common.utils.StringUtils;
import org.nuxeo.ecm.core.storage.StorageException;
import org.nuxeo.ecm.core.storage.sql.BinaryManager;
import org.nuxeo.ecm.core.storage.sql.ColumnType;
import org.nuxeo.ecm.core.storage.sql.Model;
import org.nuxeo.ecm.core.storage.sql.RepositoryDescriptor;
import org.nuxeo.ecm.core.storage.sql.jdbc.QueryMaker.QueryMakerException;
import org.nuxeo.ecm.core.storage.sql.jdbc.db.Column;
import org.nuxeo.ecm.core.storage.sql.jdbc.db.Database;
import org.nuxeo.ecm.core.storage.sql.jdbc.db.Join;
import org.nuxeo.ecm.core.storage.sql.jdbc.db.Table;
import org.nuxeo.ecm.core.storage.sql.jdbc.dialect.Dialect.FulltextQuery.Op;

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

    protected final BinaryManager binaryManager;

    protected final boolean storesUpperCaseIdentifiers;

    protected final boolean fulltextDisabled;

    protected final boolean aclOptimizationsEnabled;

    protected final int readAclMaxSize;

    /**
     * Creates a {@code Dialect} by connecting to the datasource to check what
     * database is used.
     *
     * @throws StorageException if a SQL connection problem occurs
     */
    public static Dialect createDialect(Connection connection,
            BinaryManager binaryManager,
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
            return new DialectDerby(metadata, binaryManager,
                    repositoryDescriptor);
        }
        if ("H2".equals(databaseName)) {
            return new DialectH2(metadata, binaryManager, repositoryDescriptor);
        }
        if ("MySQL".equals(databaseName)) {
            return new DialectMySQL(metadata, binaryManager,
                    repositoryDescriptor);
        }
        if ("Oracle".equals(databaseName)) {
            return new DialectOracle(metadata, binaryManager,
                    repositoryDescriptor);
        }
        if ("PostgreSQL".equals(databaseName)) {
            return new DialectPostgreSQL(metadata, binaryManager,
                    repositoryDescriptor);
        }
        if ("Microsoft SQL Server".equals(databaseName)) {
            return new DialectSQLServer(metadata, binaryManager,
                    repositoryDescriptor);
        }
        throw new StorageException("Unsupported database: " + databaseName);
    }

    public Dialect(DatabaseMetaData metadata, BinaryManager binaryManager,
            RepositoryDescriptor repositoryDescriptor) throws StorageException {
        try {
            storesUpperCaseIdentifiers = metadata.storesUpperCaseIdentifiers();
        } catch (SQLException e) {
            throw new StorageException("An error has occured.", e);
        }
        this.binaryManager = binaryManager;
        fulltextDisabled = repositoryDescriptor.fulltextDisabled;
        aclOptimizationsEnabled = repositoryDescriptor.aclOptimizationsEnabled;
        readAclMaxSize = repositoryDescriptor.readAclMaxSize;
    }

    public BinaryManager getBinaryManager() {
        return binaryManager;
    }

    /**
     * Gets the schema to use to query metadata about existing tables.
     */
    public String getConnectionSchema(Connection connection)
            throws SQLException {
        return null;
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

    /*
     * Needs to be deterministic and not change between Nuxeo EP releases.
     *
     * Turns "field_with_too_many_chars_for_oracle" into
     * "FIELD_WITH_TOO_MANY_C_58557BA3".
     */
    protected String makeName(String name, int maxNameSize) {
        if (name.length() > maxNameSize) {
            MessageDigest digest;
            try {
                digest = MessageDigest.getInstance("MD5");
            } catch (NoSuchAlgorithmException e) {
                throw new RuntimeException(e.toString(), e);
            }
            byte[] bytes = name.getBytes();
            digest.update(bytes, 0, bytes.length);
            name = name.substring(0, maxNameSize - 1 - 8);
            name += '_' + toHexString(digest.digest()).substring(0, 8);
        }
        name = storesUpperCaseIdentifiers() ? name.toUpperCase()
                : name.toLowerCase();
        name = name.replace(':', '_');
        return name;
    }

    /*
     * Used for one-time names (IDX, FK, PK), ok if algorithm changes.
     *
     * If too long, keeps 4 chars of the prefix and the full suffix.
     */
    protected String makeName(String prefix, String string, String suffix,
            int maxNameSize) {
        String name = prefix + string + suffix;
        if (name.length() > maxNameSize) {
            MessageDigest digest;
            try {
                digest = MessageDigest.getInstance("MD5");
            } catch (NoSuchAlgorithmException e) {
                throw new RuntimeException(e.toString(), e);
            }
            byte[] bytes = (prefix + string).getBytes();
            digest.update(bytes, 0, bytes.length);
            name = prefix.substring(0, 4);
            name += '_' + toHexString(digest.digest()).substring(0, 8);
            name += suffix;
        }
        name = storesUpperCaseIdentifiers() ? name.toUpperCase()
                : name.toLowerCase();
        name = name.replace(':', '_');
        return name;
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

    public String getTableName(String name) {
        return makeName(name, getMaxNameSize());
    }

    public String getColumnName(String name) {
        return makeName(name, getMaxNameSize());
    }

    public String getPrimaryKeyConstraintName(String tableName) {
        return makeName(tableName, "", "_PK", getMaxNameSize());
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
     * SQL Server supports only one fulltext index.
     */
    public boolean supportsMultipleFulltextIndexes() {
        return true;
    }

    /**
     * Does the fulltext synthetic column have to be materialized.
     */
    public abstract boolean getMaterializeFulltextSyntheticColumn();

    /**
     * Gets a CREATE INDEX statement for a fulltext index.
     */
    public abstract String getCreateFulltextIndexSql(String indexName,
            String quotedIndexName, Table table, List<Column> columns,
            Model model);

    /**
     * Structured fulltext query.
     */
    public static class FulltextQuery {
        public enum Op {
            OR, AND, WORD, NOTWORD
        };

        public Op op;

        /** The list of terms, if op is OR or AND */
        public List<FulltextQuery> terms;

        /** The word, if op is WORD or NOTWORD */
        public String word;
    }

    /**
     * Analyzes a fulltext query into a generic datastructure that can be used
     * for each specific database.
     * <p>
     * List of terms containing only negative words are suppressed. Otherwise
     * negative words are put at the end of the lists of terms.
     */
    public static FulltextQuery analyzeFulltextQuery(String query) {
        return new FulltextQueryAnalyzer().analyze(query);
    }

    public static class FulltextQueryAnalyzer {

        public static final String PLUS = "+";

        public static final String MINUS = "-";

        public static final String SPACE = " ";

        public static final char CSPACE = ' ';

        public static final String DOUBLE_QUOTES = "\"";

        public FulltextQuery ft = new FulltextQuery();

        public List<FulltextQuery> terms = new LinkedList<FulltextQuery>();

        public FulltextQuery analyze(String query) {
            query = query.replaceAll(" +", " ").trim();
            if (query.trim().length() == 0) {
                return null;
            }
            ft.op = FulltextQuery.Op.OR;
            ft.terms = new LinkedList<FulltextQuery>();
            // current sequence of ANDed terms
            boolean wasOr = false;
            String[] words = StringUtils.split(query, CSPACE, true);
            for (Iterator<String> it = Arrays.asList(words).iterator(); it.hasNext();) {
                boolean plus = false;
                boolean minus = false;
                String word = it.next();
                if (word.startsWith(PLUS)) {
                    plus = true;
                    word = word.substring(1);
                } else if (word.startsWith(MINUS)) {
                    minus = true;
                    word = word.substring(1);
                }
                if (word.startsWith(DOUBLE_QUOTES)) {
                    // read phrase
                    word = word.substring(1);
                    StringBuilder phrase = null;
                    while (true) {
                        boolean end = word.endsWith(DOUBLE_QUOTES);
                        if (end) {
                            word = word.substring(0, word.length() - 1).trim();
                        }
                        if (word.contains(DOUBLE_QUOTES)) {
                            throw new QueryMakerException(
                                    "Invalid fulltext query (double quotes in word): "
                                            + query);
                        }
                        if (word.length() != 0) {
                            if (phrase == null) {
                                phrase = new StringBuilder();
                            } else {
                                phrase.append(CSPACE);
                            }
                            phrase.append(word);
                        }
                        if (end) {
                            break;
                        }
                        if (!it.hasNext()) {
                            throw new QueryMakerException(
                                    "Invalid fulltext query (unterminated phrase): "
                                            + query);
                        }
                        word = it.next();
                    }
                    if (phrase == null) {
                        continue;
                    }
                    word = phrase.toString();
                } else if (word.equalsIgnoreCase("OR")) {
                    if (wasOr) {
                        throw new QueryMakerException(
                                "Invalid fulltext query (OR OR): " + query);
                    }
                    if (terms.isEmpty()) {
                        throw new QueryMakerException(
                                "Invalid fulltext query (standalone OR): "
                                        + query);
                    }
                    wasOr = true;
                    continue;
                }
                FulltextQuery w = new FulltextQuery();
                if (minus) {
                    if (word.length() == 0) {
                        throw new QueryMakerException(
                                "Invalid fulltext query (standalone -): "
                                        + query);
                    }
                    w.op = FulltextQuery.Op.NOTWORD;
                } else {
                    if (plus) {
                        if (word.length() == 0) {
                            throw new QueryMakerException(
                                    "Invalid fulltext query (standalone +): "
                                            + query);
                        }
                    }
                    w.op = FulltextQuery.Op.WORD;
                }
                if (wasOr) {
                    endAnd();
                    wasOr = false;
                }
                w.word = word;
                terms.add(w);
            }
            if (wasOr) {
                throw new QueryMakerException(
                        "Invalid fulltext query (final OR): " + query);
            }
            // final terms
            endAnd();
            int size = ft.terms.size();
            if (size == 0) {
                // all terms were negative
                return null;
            } else if (size == 1) {
                // simplify when no OR
                ft = ft.terms.get(0);
            }
            return ft;
        }

        // add current ANDed terms to global OR
        protected void endAnd() {
            // put negative words at the end
            List<FulltextQuery> pos = new LinkedList<FulltextQuery>();
            List<FulltextQuery> neg = new LinkedList<FulltextQuery>();
            for (FulltextQuery term : terms) {
                if (term.op == FulltextQuery.Op.NOTWORD) {
                    neg.add(term);
                } else {
                    pos.add(term);
                }
            }
            if (!pos.isEmpty()) {
                terms = pos;
                terms.addAll(neg);
                if (terms.size() == 1) {
                    ft.terms.add(terms.get(0));
                } else {
                    FulltextQuery a = new FulltextQuery();
                    a.op = FulltextQuery.Op.AND;
                    a.terms = terms;
                    ft.terms.add(a);
                }
            }
            terms = new LinkedList<FulltextQuery>();
        }

        public static void translate(FulltextQuery ft, StringBuilder buf,
                String or, String and, String andNot, String phraseQuote) {
            if (ft.op == Op.AND || ft.op == Op.OR) {
                buf.append('(');
                for (int i = 0; i < ft.terms.size(); i++) {
                    FulltextQuery term = ft.terms.get(i);
                    if (i > 0) {
                        buf.append(' ');
                        if (ft.op == Op.OR) {
                            buf.append(or);
                        } else { // Op.AND
                            if (term.op == Op.NOTWORD) {
                                buf.append(andNot);
                            } else {
                                buf.append(and);
                            }
                        }
                        buf.append(' ');
                    }
                    translate(term, buf, or, and, andNot, phraseQuote);
                }
                buf.append(')');
                return;
            } else {
                boolean isPhrase = ft.word.contains(SPACE);
                if (isPhrase) {
                    buf.append(phraseQuote);
                }
                buf.append(ft.word);
                if (isPhrase) {
                    buf.append(phraseQuote);
                }
            }
        }

        public static boolean hasPhrase(FulltextQuery ft) {
            if (ft.op == Op.AND || ft.op == Op.OR) {
                for (FulltextQuery term : ft.terms) {
                    if (hasPhrase(term)) {
                        return true;
                    }
                }
                return false;
            } else {
                return ft.word.contains(SPACE);
            }
        }

    }

    /**
     * Translate fulltext into a common pattern used by many servers.
     */
    public static String translateFulltext(FulltextQuery ft, String or,
            String and, String andNot, String phraseQuote) {
        StringBuilder buf = new StringBuilder();
        FulltextQueryAnalyzer.translate(ft, buf, or, and, andNot, phraseQuote);
        return buf.toString();
    }

    /**
     * Checks if a fulltext search has a phrase in it.
     */
    public static boolean fulltextHasPhrase(FulltextQuery ft) {
        return FulltextQueryAnalyzer.hasPhrase(ft);
    }

    /**
     * Get the dialect-specific version of a fulltext query.
     *
     * @param query the CMIS-syntax-based fulltext query string
     * @return the dialect native fulltext query string
     */
    public abstract String getDialectFulltextQuery(String query);

    /**
     * Information needed to express fulltext search with scoring.
     */
    public static class FulltextMatchInfo {

        public List<Join> joins;

        public String whereExpr;

        public String whereExprParam;

        public String scoreExpr;

        public String scoreExprParam;

        public String scoreAlias;

        public Column scoreCol;
    }

    /**
     * Gets the SQL information needed to do a a fulltext match, either with a
     * direct expression in the WHERE clause, or using a join with an additional
     * table.
     */
    public abstract FulltextMatchInfo getFulltextScoredMatchInfo(
            String fulltextQuery, String indexName, int nthMatch,
            Column mainColumn, Model model, Database database);

    /**
     * Indicates if dialect supports paging
     *
     * @return true if the dialect supports paging
     */
    public boolean supportsPaging() {
        return false;
    }

    /**
     * Gets paging clause to be appended at the end of select statement
     */
    public String getPagingClause(long limit, long offset) {
        throw new UnsupportedOperationException("paging is not supported");
    }

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
     * usually for conversion (this is the case for PostgreSQL fulltext
     * {@code TSVECTOR} columns for instance).
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
        return String.format(" ADD CONSTRAINT %s PRIMARY KEY ", constraintName);
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
     * Whether a GROUP BY can only be used with the original column name and not
     * an alias.
     */
    public boolean needsOriginalColumnInGroupBy() {
        return false;
    }

    /**
     * Whether implicit Oracle joins (instead of explicit ANSI joins) are
     * needed.
     */
    public boolean needsOracleJoins() {
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
     * Checks if the dialect supports an ancestors table.
     */
    public boolean supportsAncestorsTable() {
        return false;
    }

    /**
     * Gets the expression to use to check tree membership.
     *
     * @param idColumnName the quoted name of the id column to use
     * @return an SQL expression with one parameters for the based id that is
     *         true if the document is under base id
     */
    public abstract String getInTreeSql(String idColumnName);

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
     * Gets the name of the file containing the SQL statements.
     */
    public abstract String getSQLStatementsFilename();

    public abstract String getTestSQLStatementsFilename();

    /**
     * Gets the properties to use with the SQL statements.
     */
    public abstract Map<String, Serializable> getSQLStatementsProperties(
            Model model, Database database);

    /**
     * Checks that clustering is supported.
     */
    public boolean isClusteringSupported() {
        return false;
    }

    /**
     * Does clustering fetch of invalidations (
     * {@link #getClusterGetInvalidations}) need a separate delete for them (
     * {@link #getClusterDeleteInvalidations}).
     */
    public boolean isClusteringDeleteNeeded() {
        return false;
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
     * Gets the SQL to delete invalidations for this cluster node.
     *
     * @return an SQL statement returning a result set
     */
    public String getClusterDeleteInvalidations() {
        return null;
    }

    /**
     * Does the dialect support ILIKE operator
     *
     */
    public boolean supportsIlike() {
        return false;
    }

    /**
     * Does the dialect support an optimized read security checks
     *
     */
    public boolean supportsReadAcl() {
        return false;
    }

    /**
     * Does the dialect support SQL-99 WITH common table expressions.
     */
    public boolean supportsWith() {
        return false;
    }

    /**
     * Maximum number of values in a IN (?, ?, ...) statement.
     * <p>
     * Beyond this size we'll do the query in several chunks.
     * <p>
     * PostgreSQL is limited to 65535 values in a prepared statement.
     * <p>
     * Oracle is limited to 1000 expressions in a list (ORA-01795).
     */
    public int getMaximumArgsForIn() {
        return 400;
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

    /**
     * Called before a table is created, when it's been determined that it
     * doesn't exist yet.
     *
     * @return {@code false} if the table must actually not be created
     */
    public boolean preCreateTable(Connection connection, Table table,
            Model model, Database database) throws SQLException {
        return true;
    }

    /**
     * Gets the sql statements to call after a table has been created.
     * <p>
     * Used for migrations/upgrades.
     */
    public List<String> getPostCreateTableSqls(Table table, Model model,
            Database database) {
        return Collections.emptyList();
    }

    /**
     * Called after an existing table has been detected in the database.
     * <p>
     * Used for migrations/upgrades.
     */
    public void existingTableDetected(Connection connection, Table table,
            Model model, Database database) throws SQLException {
    }

    /**
     * Checks if an exception received means that the low level connection has
     * been trashed and must be reset.
     */
    public boolean isConnectionClosedException(Throwable t) {
        while (t.getCause() != null) {
            t = t.getCause();
        }
        return t instanceof SocketException;
    }

    /**
     * Let the dialect processes additional statements after tables creation and
     * conditional statements. Can be used for specific upgrade procedure.
     *
     * @param connection
     */
    public void performAdditionalStatements(Connection connection)
            throws SQLException {
    }

    /**
     * A query that, when executed, will make at least a round-trip to the
     * server to check that the connection is alive.
     * <p>
     * The query should throw an error if the connection is dead.
     */
    public String getValidationQuery() {
        return "SELECT 1";
    }

}
