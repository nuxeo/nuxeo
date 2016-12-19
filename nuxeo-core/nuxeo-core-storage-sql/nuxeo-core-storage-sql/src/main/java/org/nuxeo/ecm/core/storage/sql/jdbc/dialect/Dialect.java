/*
 * (C) Copyright 2006-2016 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Contributors:
 *     Florent Guillaume
 */
package org.nuxeo.ecm.core.storage.sql.jdbc.dialect;

import java.io.Serializable;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.Array;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;
import java.util.regex.Pattern;

import org.nuxeo.common.utils.StringUtils;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.core.query.QueryParseException;
import org.nuxeo.ecm.core.storage.FulltextDescriptor;
import org.nuxeo.ecm.core.storage.sql.ColumnType;
import org.nuxeo.ecm.core.storage.sql.Model;
import org.nuxeo.ecm.core.storage.sql.RepositoryDescriptor;
import org.nuxeo.ecm.core.storage.sql.jdbc.JDBCLogger;
import org.nuxeo.ecm.core.storage.sql.jdbc.db.Column;
import org.nuxeo.ecm.core.storage.sql.jdbc.db.Database;
import org.nuxeo.ecm.core.storage.sql.jdbc.db.Join;
import org.nuxeo.ecm.core.storage.sql.jdbc.db.Table;
import org.nuxeo.runtime.api.Framework;

/**
 * A Dialect encapsulates knowledge about database-specific behavior.
 *
 * @author Florent Guillaume
 */
public abstract class Dialect {

    // change to have deterministic pseudo-UUID generation for debugging
    public static final boolean DEBUG_UUIDS = false;

    // if true then debug UUIDs (above) are actual UUIDs, not short strings
    public static final boolean DEBUG_REAL_UUIDS = false;

    // for debug
    private final AtomicLong temporaryIdCounter = new AtomicLong(0);

    /**
     * Property used to disable NULLS LAST usage when sorting DESC. This increase performance for some dialects because
     * they can use an index for sorting when there are no NULL value.
     *
     * @since 5.9
     */
    public static final String NULLS_LAST_ON_DESC_PROP = "nuxeo.vcs.use-nulls-last-on-desc";

    /**
     * Store the SQL for descending order
     *
     * @since 5.9
     */
    protected String descending;

    /**
     * System property to override the dialect to use globally instead of the one auto-detected. It can be suffixed by
     * "." and the database name (without spaces and as returned by the database itself) to override only for a specific
     * database.
     *
     * @since 5.6
     */
    public static final String DIALECT_CLASS = "nuxeo.vcs.dialect";

    public static final Map<String, Class<? extends Dialect>> DIALECTS = new HashMap<>();

    static {
        DIALECTS.put("H2", DialectH2.class);
        DIALECTS.put("MySQL", DialectMySQL.class);
        DIALECTS.put("Oracle", DialectOracle.class);
        DIALECTS.put("PostgreSQL", DialectPostgreSQL.class);
        DIALECTS.put("Microsoft SQL Server", DialectSQLServer.class);
        DIALECTS.put("HSQL Database Engine", DialectHSQLDB.class);
        DIALECTS.put("Apache Derby", DialectDerby.class);
        DIALECTS.put("DB2", DialectDB2.class);
    }

    /**
     * Does the dialect support an scroll API
     *
     * @since 8.4
     */
    public boolean supportsScroll() {
        return true;
    }

    public static final class JDBCInfo {
        public final String string;

        public final int jdbcType;

        public final String jdbcBaseTypeString;

        public final int jdbcBaseType;

        public JDBCInfo(String string, int jdbcType) {
            this(string, jdbcType, null, 0);
        }

        public JDBCInfo(String string, int jdbcType, String jdbcBaseTypeString, int jdbcBaseType) {
            this.string = string;
            this.jdbcType = jdbcType;
            this.jdbcBaseTypeString = jdbcBaseTypeString;
            this.jdbcBaseType = jdbcBaseType;
        }
    }

    /** Type of id when stored in the database. */
    public enum DialectIdType {
        /** VARCHAR storing a UUID as a string. */
        VARCHAR,
        /** Native UUID. */
        UUID,
        /** Long from sequence generated by database. */
        SEQUENCE,
    }

    public static JDBCInfo jdbcInfo(String string, int jdbcType) {
        return new JDBCInfo(string, jdbcType);
    }

    public static JDBCInfo jdbcInfo(String string, int length, int jdbcType) {
        return new JDBCInfo(String.format(string, Integer.valueOf(length)), jdbcType);
    }

    public static JDBCInfo jdbcInfo(String string, int jdbcType, String jdbcBaseTypeString, int jdbcBaseType) {
        return new JDBCInfo(string, jdbcType, jdbcBaseTypeString, jdbcBaseType);
    }

    public static JDBCInfo jdbcInfo(String string, int length, int jdbcType, String jdbcBaseTypeString,
            int jdbcBaseType) {
        return new JDBCInfo(String.format(string, Integer.valueOf(length)), jdbcType,
                String.format(jdbcBaseTypeString, Integer.valueOf(length)), jdbcBaseType);
    }

    protected final boolean storesUpperCaseIdentifiers;

    protected boolean fulltextDisabled;

    protected boolean fulltextSearchDisabled;

    protected final boolean aclOptimizationsEnabled;

    /**
     * @since 5.7
     */
    protected boolean clusteringEnabled;

    /**
     * @since 5.7
     */
    protected boolean softDeleteEnabled;

    protected boolean proxiesEnabled;

    protected final int readAclMaxSize;

    /**
     * Creates a {@code Dialect} by connecting to the datasource to check what database is used.
     */
    public static Dialect createDialect(Connection connection, RepositoryDescriptor repositoryDescriptor) {
        DatabaseMetaData metadata;
        String databaseName;
        try {
            metadata = connection.getMetaData();
            databaseName = metadata.getDatabaseProductName();
        } catch (SQLException e) {
            throw new NuxeoException(e);
        }
        if (databaseName.contains("/")) {
            // DB2/LINUX, DB2/DARWIN, etc.
            databaseName = databaseName.substring(0, databaseName.indexOf('/'));
        }
        String dialectClassName = Framework.getProperty(DIALECT_CLASS);
        if (dialectClassName == null) {
            dialectClassName = Framework.getProperty(DIALECT_CLASS + '.' + databaseName.replace(" ", ""));
        }
        Class<? extends Dialect> dialectClass;
        if (dialectClassName == null) {
            dialectClass = DIALECTS.get(databaseName);
            if (dialectClass == null) {
                throw new NuxeoException("Unsupported database: " + databaseName);
            }
        } else {
            Class<?> klass;
            try {
                ClassLoader cl = Thread.currentThread().getContextClassLoader();
                klass = cl.loadClass(dialectClassName);
            } catch (ClassNotFoundException e) {
                throw new NuxeoException(e);
            }
            if (!Dialect.class.isAssignableFrom(klass)) {
                throw new NuxeoException("Not a Dialect: " + dialectClassName);
            }
            dialectClass = (Class<? extends Dialect>) klass;
        }
        Constructor<? extends Dialect> ctor;
        try {
            ctor = dialectClass.getConstructor(DatabaseMetaData.class, RepositoryDescriptor.class);
        } catch (ReflectiveOperationException e) {
            throw new NuxeoException("Bad constructor signature for: " + dialectClassName, e);
        }
        Dialect dialect;
        try {
            dialect = ctor.newInstance(metadata, repositoryDescriptor);
        } catch (InvocationTargetException e) {
            Throwable t = e.getTargetException();
            if (t instanceof NuxeoException) {
                throw (NuxeoException) t;
            } else {
                throw new NuxeoException(t);
            }
        } catch (ReflectiveOperationException e) {
            throw new NuxeoException("Cannot construct dialect: " + dialectClassName, e);
        }
        return dialect;
    }

    public Dialect(DatabaseMetaData metadata, RepositoryDescriptor repositoryDescriptor) {
        try {
            storesUpperCaseIdentifiers = metadata.storesUpperCaseIdentifiers();
        } catch (SQLException e) {
            throw new NuxeoException(e);
        }
        if (repositoryDescriptor == null) {
            fulltextDisabled = true;
            fulltextSearchDisabled = true;
            aclOptimizationsEnabled = false;
            readAclMaxSize = 0;
            clusteringEnabled = false;
            softDeleteEnabled = false;
            proxiesEnabled = true;
        } else {
            FulltextDescriptor fulltextDescriptor = repositoryDescriptor.getFulltextDescriptor();
            fulltextDisabled = fulltextDescriptor.getFulltextDisabled();
            fulltextSearchDisabled = fulltextDescriptor.getFulltextSearchDisabled();
            aclOptimizationsEnabled = repositoryDescriptor.getAclOptimizationsEnabled();
            readAclMaxSize = repositoryDescriptor.getReadAclMaxSize();
            clusteringEnabled = repositoryDescriptor.getClusteringEnabled();
            softDeleteEnabled = repositoryDescriptor.getSoftDeleteEnabled();
            proxiesEnabled = repositoryDescriptor.getProxiesEnabled();
        }
    }

    /**
     * Gets the schema to use to query metadata about existing tables.
     */
    public String getConnectionSchema(Connection connection) throws SQLException {
        return null;
    }

    /**
     * Gets the JDBC type and string from Nuxeo's type abstraction.
     */
    public abstract JDBCInfo getJDBCTypeAndString(ColumnType type);

    /**
     * Check mismatches between expected and actual JDBC types read from database introspection.
     */
    public boolean isAllowedConversion(int expected, int actual, String actualName, int actualSize) {
        return false;
    }

    /**
     * Gets a generated id if so configured, otherwise returns null.
     */
    public Serializable getGeneratedId(Connection connection) throws SQLException {
        if (DEBUG_UUIDS) {
            if (DEBUG_REAL_UUIDS) {
                return String.format("00000000-0000-0000-0000-%012x",
                        Long.valueOf(temporaryIdCounter.incrementAndGet()));
            } else {
                return "UUID_" + temporaryIdCounter.incrementAndGet();
            }
        } else {
            return UUID.randomUUID().toString();
        }
    }

    /**
     * Sets a prepared statement value that is a Nuxeo main id (usually UUID).
     *
     * @param ps the prepared statement
     * @param index the parameter index in the prepared statement
     * @param value the value to set
     */
    public void setId(PreparedStatement ps, int index, Serializable value) throws SQLException {
        ps.setObject(index, value);
    }

    /**
     * Sets a long id (sequence) from a value that may be a String or already a Long.
     */
    public void setIdLong(PreparedStatement ps, int index, Serializable value) throws SQLException {
        long l;
        if (value instanceof String) {
            try {
                l = Long.parseLong((String) value);
            } catch (NumberFormatException e) {
                throw new SQLException("Invalid long id: " + value);
            }
        } else if (value instanceof Long) {
            l = ((Long) value).longValue();
        } else {
            throw new SQLException("Unsupported class for long id, class: " + value.getClass() + " value: " + value);
        }
        ps.setLong(index, l);
    }

    public abstract void setToPreparedStatement(PreparedStatement ps, int index, Serializable value, Column column)
            throws SQLException;

    public static final String ARRAY_SEP = "|";

    protected void setToPreparedStatementString(PreparedStatement ps, int index, Serializable value, Column column)
            throws SQLException {
        String v;
        ColumnType type = column.getType();
        if (type == ColumnType.SYSNAMEARRAY) {
            // implementation when arrays aren't supported
            String[] strings = (String[]) value;
            if (strings == null) {
                v = null;
            } else {
                // use initial and final separator as terminator
                StringBuilder buf = new StringBuilder(ARRAY_SEP);
                for (String string : strings) {
                    buf.append(string);
                    buf.append(ARRAY_SEP);
                }
                v = buf.toString();
            }
        } else {
            v = (String) value;
        }
        ps.setString(index, v);
    }

    public void setToPreparedStatementTimestamp(PreparedStatement ps, int index, Serializable value, Column column)
            throws SQLException {
        Calendar cal = (Calendar) value;
        Timestamp ts = cal == null ? null : new Timestamp(cal.getTimeInMillis());
        ps.setTimestamp(index, ts, cal); // cal passed for timezone
    }

    public Timestamp getTimestampFromCalendar(Calendar value) {
        return new Timestamp(value.getTimeInMillis());
    }

    public Timestamp[] getTimestampFromCalendar(Serializable[] value) {
        if (value == null) {
            return null;
        }
        Timestamp[] ts = new Timestamp[value.length];
        for (int i = 0; i < value.length; i++) {
            ts[i] = getTimestampFromCalendar((Calendar) value[i]);
        }
        return ts;
    }

    public Calendar getCalendarFromTimestamp(Timestamp value) {
        if (value == null) {
            return null;
        }
        Calendar cal = new GregorianCalendar(); // XXX timezone
        cal.setTimeInMillis(value.getTime());
        return cal;
    }

    public Calendar[] getCalendarFromTimestamp(Timestamp[] value) {
        if (value == null) {
            return null;
        }
        Calendar[] cal = new GregorianCalendar[value.length];
        for (int i = 0; i < value.length; i++) {
            cal[i] = getCalendarFromTimestamp(value[i]);
        }
        return cal;
    }

    public abstract Serializable getFromResultSet(ResultSet rs, int index, Column column) throws SQLException;

    protected Serializable getFromResultSetString(ResultSet rs, int index, Column column) throws SQLException {
        String string = rs.getString(index);
        if (string == null) {
            return null;
        }
        ColumnType type = column.getType();
        if (type == ColumnType.SYSNAMEARRAY) {
            // implementation when arrays aren't supported
            // an initial separator is expected
            if (string.startsWith(ARRAY_SEP)) {
                string = string.substring(ARRAY_SEP.length());
            }
            // the final separator is dropped as split does not return final
            // empty strings
            return string.split(Pattern.quote(ARRAY_SEP));
        } else {
            return string;
        }
    }

    protected Serializable getFromResultSetTimestamp(ResultSet rs, int index, Column column) throws SQLException {
        Timestamp ts = rs.getTimestamp(index);
        if (ts == null) {
            return null;
        } else {
            Serializable cal = new GregorianCalendar(); // XXX timezone
            ((Calendar) cal).setTimeInMillis(ts.getTime());
            return cal;
        }
    }

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
        return getMaxNameSize();
    }

    /*
     * Needs to be deterministic and not change between Nuxeo EP releases. Turns "field_with_too_many_chars_for_oracle"
     * into "FIELD_WITH_TOO_MANY_C_58557BA3".
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
        name = storesUpperCaseIdentifiers() ? name.toUpperCase() : name.toLowerCase();
        name = name.replace(':', '_');
        return name;
    }

    /*
     * Used for one-time names (IDX, FK, PK), ok if algorithm changes. If too long, keeps 4 chars of the prefix and the
     * full suffix.
     */
    protected String makeName(String prefix, String string, String suffix, int maxNameSize) {
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
        name = storesUpperCaseIdentifiers() ? name.toUpperCase() : name.toLowerCase();
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

    public String getForeignKeyConstraintName(String tableName, String foreignColumnName, String foreignTableName) {
        return makeName(tableName + '_', foreignColumnName + '_' + foreignTableName, "_FK", getMaxNameSize());
    }

    public String getIndexName(String tableName, List<String> columnNames) {
        return makeName(qualifyIndexName() ? tableName + '_' : "", String.join("_", columnNames), "_IDX",
                getMaxIndexNameSize());
    }

    /**
     * Gets a CREATE INDEX statement for an index.
     *
     * @param indexName the index name (for fulltext)
     * @param indexType the index type
     * @param table the table
     * @param columns the columns to index
     * @param model the model
     */
    public String getCreateIndexSql(String indexName, Table.IndexType indexType, Table table, List<Column> columns,
            Model model) {
        List<String> qcols = new ArrayList<>(columns.size());
        List<String> pcols = new ArrayList<>(columns.size());
        for (Column col : columns) {
            qcols.add(col.getQuotedName());
            pcols.add(col.getPhysicalName());
        }
        String quotedIndexName = openQuote() + getIndexName(table.getKey(), pcols) + closeQuote();
        if (indexType == Table.IndexType.FULLTEXT) {
            return getCreateFulltextIndexSql(indexName, quotedIndexName, table, columns, model);
        } else {
            return String.format("CREATE INDEX %s ON %s (%s)", quotedIndexName, table.getQuotedName(),
                    String.join(", ", qcols));
        }
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
    public abstract String getCreateFulltextIndexSql(String indexName, String quotedIndexName, Table table,
            List<Column> columns, Model model);

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
     * Gets the SQL information needed to do a a fulltext match, either with a direct expression in the WHERE clause, or
     * using a join with an additional table.
     */
    public abstract FulltextMatchInfo getFulltextScoredMatchInfo(String fulltextQuery, String indexName, int nthMatch,
            Column mainColumn, Model model, Database database);

    /**
     * Gets the SQL fragment to add after a LIKE match to specify the escaping character.
     *
     * @since 7.4
     */
    public String getLikeEscaping() {
        return null;
    }

    /**
     * Gets the SQL fragment to match a mixin type.
     */
    public String getMatchMixinType(Column mixinsColumn, String mixin, boolean positive, String[] returnParam) {
        returnParam[0] = "%" + ARRAY_SEP + mixin + ARRAY_SEP + "%";
        return String.format("%s %s ?", mixinsColumn.getFullQuotedName(), positive ? "LIKE" : "NOT LIKE");
    }

    /**
     * Indicates if dialect supports paging
     *
     * @return true if the dialect supports paging
     */
    public boolean supportsPaging() {
        return false;
    }

    /**
     * Returns the SQL query with a paging clause
     *
     * @since 5.7 (replacing getPagingClause)
     */
    public String addPagingClause(String sql, long limit, long offset) {
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
     * Needed for columns that need an expression around the value being set, usually for conversion (this is the case
     * for PostgreSQL fulltext {@code TSVECTOR} columns for instance).
     *
     * @param type the column type
     * @return the expression containing a free variable
     */
    public String getFreeVariableSetterForType(ColumnType type) {
        return "?";
    }

    public String getNoColumnsInsertString(Column idColumn) {
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

    public String getAddForeignKeyConstraintString(String constraintName, String[] foreignKeys, String referencedTable,
            String[] primaryKeys, boolean referencesPrimaryKey) {
        String sql = String.format(" ADD CONSTRAINT %s FOREIGN KEY (%s) REFERENCES %s", constraintName,
                String.join(", ", foreignKeys), referencedTable);
        if (!referencesPrimaryKey) {
            sql += " (" + String.join(", ", primaryKeys) + ')';
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
     * When doing an UPDATE t SET ... FROM t, u WHERE ..., does the FROM clause need to repeate the updated table (t).
     */
    public abstract boolean doesUpdateFromRepeatSelf();

    /**
     * When doing a SELECT DISTINCT that uses a ORDER BY, do the keys along which we order have to be mentioned in the
     * DISTINCT clause?
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
     * Whether a GROUP BY can only be used with the original column name and not an alias.
     */
    public boolean needsOriginalColumnInGroupBy() {
        return false;
    }

    /**
     * Whether implicit Oracle joins (instead of explicit ANSI joins) are needed.
     */
    public boolean needsOracleJoins() {
        return false;
    }

    /**
     * The dialect need an extra SQL statement to populate a user read acl cache before running the query.
     *
     * @since 5.5
     */
    public boolean needsPrepareUserReadAcls() {
        return supportsReadAcl();
    }

    /**
     * True if the dialect need an extra NULLS LAST on DESC sort.
     *
     * @since 5.9
     */
    public boolean needsNullsLastOnDescSort() {
        return false;
    }

    /**
     * When using a CLOB field in an expression, is some casting required and with what pattern?
     * <p>
     * Needed for Derby and H2.
     *
     * @param inOrderBy {@code true} if the expression is for an ORDER BY column
     * @return a pattern for String.format with one parameter for the column name and one for the width, or {@code null}
     *         if no cast is required
     */
    public String getClobCast(boolean inOrderBy) {
        return null;
    }

    /**
     * Get the expression to use to cast a column to a DATE type.
     *
     * @return a pattern for String.format with one parameter for the column name
     * @since 5.6
     */
    public String getDateCast() {
        return "CAST(%s AS DATE)";
    }

    /**
     * Casts an id column to a VARCHAR type.
     * <p>
     * Used for uuid/varchar joins.
     *
     * @return the casted expression
     * @since 5.7
     */
    public String castIdToVarchar(String expr) {
        return expr;
    }

    /**
     * Gets the type of id when stored in the database.
     *
     * @since 5.7
     */
    public DialectIdType getIdType() {
        return DialectIdType.VARCHAR;
    }

    /**
     * Gets the expression to use to check security.
     *
     * @param idColumnName the quoted name of the id column to use
     * @return an SQL expression with two parameters (principals and permissions) that is true if access is allowed
     */
    public abstract String getSecurityCheckSql(String idColumnName);

    /**
     * Checks if the dialect supports an ancestors table.
     */
    public boolean supportsAncestorsTable() {
        return false;
    }

    /**
     * Checks whether {@link #getInTreeSql(String, String)} is optimized for fast results (using an ancestors or
     * descendants table).
     *
     * @since 7.10, 6.0-HF21
     */
    public boolean supportsFastDescendants() {
        return false;
    }

    /**
     * Gets the expression to use to check tree membership.
     *
     * @param idColumnName the quoted name of the id column to use
     * @param id the id, to check syntax with respect to specialized id column types
     * @return an SQL expression with one parameters for the based id that is true if the document is under base id, or
     *         {@code null} if the query cannot match
     */
    public abstract String getInTreeSql(String idColumnName, String id);

    /**
     * Does the dialect support passing ARRAY values (to stored procedures mostly).
     * <p>
     * If not, we'll simulate them using a string and a separator.
     *
     * @return true if ARRAY values are supported
     */
    public boolean supportsArrays() {
        return false;
    }

    /**
     * Does a stored function returning an result set need to access it as a single array instead of iterating over a
     * normal result set's rows.
     * <p>
     * Oracle needs this.
     */
    public boolean supportsArraysReturnInsteadOfRows() {
        return false;
    }

    /**
     * Gets the array result as a converted array of Serializable.
     *
     * @since 5.9.3
     */
    public Serializable[] getArrayResult(Array array) throws SQLException {
        throw new UnsupportedOperationException();
    }

    /**
     * Checks if the dialect supports storing arrays of system names (for mixins for instance).
     */
    public boolean supportsSysNameArray() {
        return false;
    }

    /**
     * Does the dialect support storing arrays in table columns.
     * <p>
     *
     * @return true if ARRAY columns are supported
     */
    public boolean supportsArrayColumns() {
        return false;
    }

    /**
     * Structured Array Subquery Abstract Class.
     */
    public static abstract class ArraySubQuery {
        protected Column arrayColumn;

        protected String subQueryAlias;

        public ArraySubQuery(Column arrayColumn, String subqueryAlias) {
            this.arrayColumn = arrayColumn;
            this.subQueryAlias = subqueryAlias;
        }

        public abstract Column getSubQueryIdColumn();

        public abstract Column getSubQueryValueColumn();

        public abstract String toSql();
    }

    /**
     * Gets the dialect-specific subquery for an array column.
     */
    public ArraySubQuery getArraySubQuery(Column arrayColumn, String subQueryAlias) {
        throw new QueryParseException("Array sub-query not supported");
    }

    /**
     * Get SQL Array Element Subscripted string.
     */
    public String getArrayElementString(String arrayColumnName, int arrayElementIndex) {
        throw new QueryParseException("Array element not supported");
    }

    /**
     * Gets the SQL string for an array column IN expression.
     */
    public String getArrayInSql(Column arrayColumn, String cast, boolean positive, List<Serializable> params) {
        throw new QueryParseException("Array IN not supported");
    }

    /**
     * Gets the SQL string for an array column LIKE expression.
     */
    public String getArrayLikeSql(Column arrayColumn, String refName, boolean positive, Table dataHierTable) {
        throw new QueryParseException("Array LIKE not supported");
    }

    /**
     * Gets the SQL string for an array column ILIKE expression.
     */
    public String getArrayIlikeSql(Column arrayColumn, String refName, boolean positive, Table dataHierTable) {
        throw new QueryParseException("Array ILIKE not supported");
    }

    /**
     * Factory method for creating Array objects, suitable for passing to {@link PreparedStatement#setArray}.
     * <p>
     * (An equivalent method is defined by JDBC4 on the {@link Connection} class.)
     *
     * @param type the SQL type of the elements
     * @param elements the elements of the array
     * @param connection the connection
     * @return an Array holding the elements
     */
    public Array createArrayOf(int type, Object[] elements, Connection connection) throws SQLException {
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
    public abstract Map<String, Serializable> getSQLStatementsProperties(Model model, Database database);

    /**
     * Checks that clustering is supported.
     */
    public boolean isClusteringSupported() {
        return false;
    }

    /**
     * Does clustering fetch of invalidations ( {@link #getClusterGetInvalidations}) need a separate delete for them.
     */
    public boolean isClusteringDeleteNeeded() {
        return false;
    }

    /**
     * Gets the SQL to send an invalidation to the cluster.
     *
     * @return an SQL statement with parameters for: nodeId, id, fragments, kind
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
     * Does the dialect support ILIKE operator
     */
    public boolean supportsIlike() {
        return false;
    }

    /**
     * Does the dialect support an optimized read security checks
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
     * Does the dialect have an empty string identical to NULL (Oracle).
     */
    public boolean hasNullEmptyString() {
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
     */
    public String getUpdateReadAclsSql() {
        return null;
    }

    /**
     * Gets the statement to rebuild the wall read acls
     */
    public String getRebuildReadAclsSql() {
        return null;
    }

    /**
     * Gets the expression to check if access is allowed using read acls. The dialect must suppportsReadAcl.
     *
     * @param userIdCol the quoted name of the aclr_user_map user_id column to use
     * @return an SQL expression with one parameter (principals) that is true if access is allowed
     */
    public String getReadAclsCheckSql(String userIdCol) {
        return null;
    }

    /**
     * Gets the SQL expression to prepare the user read acls cache. This can be used to populate a table cache.
     *
     * @since 5.5
     * @return and SQL expression with one parameter (principals)
     */
    public String getPrepareUserReadAclsSql() {
        return null;
    }

    /**
     * Gets the sql statements to execute after the repository init (at startup).
     * <p>
     * Used for vacuum-like operations.
     *
     * @since 6.0-HF24, 7.10-HF01, 8.1
     */
    public List<String> getStartupSqls(Model model, Database database) {
        return Collections.emptyList();
    }

    /**
     * Checks if an exception received means that a concurrent update was detected.
     *
     * @since 5.8
     */
    public boolean isConcurrentUpdateException(Throwable t) {
        return false;
    }

    /**
     * Let the dialect processes additional statements after tables creation and conditional statements. Can be used for
     * specific upgrade procedure.
     */
    public void performAdditionalStatements(Connection connection) throws SQLException {
    }

    /**
     * A query that, when executed, will make at least a round-trip to the server to check that the connection is alive.
     * <p>
     * The query should throw an error if the connection is dead.
     */
    public String getValidationQuery() {
        return "SELECT 1";
    }

    /**
     * Gets the SQL function that returns the length of a blob, in bytes.
     */
    public String getBlobLengthFunction() {
        // the SQL-standard function (PostgreSQL, MySQL)
        return "OCTET_LENGTH";
    }

    /**
     * Let the dialect perform additional statements just after the connection is opened.
     */
    public void performPostOpenStatements(Connection connection) throws SQLException {
    }

    /**
     * Gets additional SQL statements to execute after the CREATE TABLE when creating an identity column.
     * <p>
     * Oracle needs both a sequence and a trigger.
     */
    public List<String> getPostCreateIdentityColumnSql(Column column) {
        return Collections.emptyList();
    }

    /**
     * Checks if an identity column is already defined as a primary key and does not need a separate index added.
     * <p>
     * MySQL defines the identity column directly as primary key.
     */
    public boolean isIdentityAlreadyPrimary() {
        return false;
    }

    /**
     * True if the dialect returns the generated key for the identity from the insert statement.
     * <p>
     * Oracle needs a separate call to CURRVAL.
     */
    public boolean hasIdentityGeneratedKey() {
        return true;
    }

    /**
     * Gets the SQL query to execute to retrieve the last generated identity key.
     * <p>
     * Oracle needs a separate call to CURRVAL.
     */
    public String getIdentityGeneratedKeySql(Column column) {
        return null;
    }

    /**
     * Gets the SQL query to get the ancestors of a set of ids.
     *
     * @return null if not available
     */
    public String getAncestorsIdsSql() {
        return null;
    }

    /**
     * Gets the SQL descending sort direction with option to sort nulls last. Use to unify database behavior.
     *
     * @return DESC or DESC NULLS LAST depending on dialects.
     */
    public String getDescending() {
        if (descending == null) {
            if (needsNullsLastOnDescSort()
                    && Boolean.parseBoolean(Framework.getProperty(NULLS_LAST_ON_DESC_PROP, "true"))) {
                descending = " DESC NULLS LAST";
            } else {
                descending = " DESC";
            }
        }
        return descending;
    }

    /**
     * Columns ignored if we see them in existing tables.
     */
    public List<String> getIgnoredColumns(Table table) {
        return Collections.emptyList();
    }

    /**
     * Additional column definitions for CREATE TABLE.
     */
    public String getCustomColumnDefinition(Table table) {
        return null;
    }

    /**
     * Additional things to execute after CREATE TABLE.
     */
    public List<String> getCustomPostCreateSqls(Table table) {
        return Collections.emptyList();
    }

    /**
     * SQL to soft delete documents. SQL returned has free parameters for the array of ids and time.
     */
    public String getSoftDeleteSql() {
        throw new UnsupportedOperationException("Soft deletes not supported");
    }

    /**
     * SQL to clean soft-delete documents. SQL returned has free parameters max and beforeTime.
     */
    public String getSoftDeleteCleanupSql() {
        throw new UnsupportedOperationException("Soft deletes not supported");
    }

    /**
     * Return the SQL to get the columns fulltext fields
     *
     * @since 5.9.3
     */
    public String getBinaryFulltextSql(List<String> columns) {
        return "SELECT " + String.join(", ", columns) + " FROM fulltext WHERE id=?";
    }

    /**
     * Checks if a given stored procedure exists and is identical to the passed creation SQL.
     * <p>
     * There are 3 cases to deal with, and actions to perform:
     * <ul>
     * <li>the stored procedure doesn't exist, and must be created (create the stored procedure);
     * <li>the stored procedure exists but is not up to date (drop the old stored procedure and re-create it);
     * <li>the stored procedure exists and is up to date (nothing to do).
     * </ul>
     * <p>
     * When there is nothing to do, {@code null} is returned. Otherwise the returned value is a list of SQL statements
     * to execute. Note that the SQL statements will include also INSERT statements to be executed to remember the
     * creation SQL itself.
     *
     * @param procName the stored procedure name
     * @param procCreate the creation SQL for the stored procedure
     * @param ddlMode the DDL mode
     * @param connection the connection
     * @param logger the logger
     * @param properties the statement execution properties
     * @return a list of SQL statements
     * @since 6.0-HF24, 7.10-HF01, 8.1
     */
    public abstract List<String> checkStoredProcedure(String procName, String procCreate, String ddlMode,
            Connection connection, JDBCLogger logger, Map<String, Serializable> properties) throws SQLException;

    /**
     * Returns the initial DDL statements to add to a DDL dump.
     *
     * @return a list of SQL statements, usually empty
     * @since 6.0-HF24, 7.10-HF01, 8.1
     */
    public Collection<? extends String> getDumpStart() {
        return Collections.emptyList();
    }

    /**
     * Returns the final DDL statements to add to a DDL dump.
     *
     * @return a list of SQL statements, usually empty
     * @since 6.0-HF24, 7.10-HF01, 8.1
     */
    public Collection<? extends String> getDumpStop() {
        return Collections.emptyList();
    }

    /**
     * Returns the SQL statement with proper terminator to use in a dump.
     *
     * @return the SQL statement
     * @since 6.0-HF24, 7.10-HF01, 8.1
     */
    public String getSQLForDump(String sql) {
        return sql + ";";
    }

}
