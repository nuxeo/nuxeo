/*
 * Copyright (c) 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Florent Guillaume
 */

package org.nuxeo.ecm.core.storage.sql.jdbc.dialect;

import java.io.Serializable;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.net.SocketException;
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
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import org.nuxeo.common.utils.StringUtils;
import org.nuxeo.ecm.core.storage.StorageException;
import org.nuxeo.ecm.core.storage.sql.Binary;
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
import org.nuxeo.runtime.api.Framework;

/**
 * A Dialect encapsulates knowledge about database-specific behavior.
 *
 * @author Florent Guillaume
 */
public abstract class Dialect {

    /**
     * System property to override the dialect to use globally instead of the
     * one auto-detected. It can be suffixed by "." and the database name
     * (without spaces and as returned by the database itself) to override only
     * for a specific database.
     *
     * @since 5.6
     */
    public static final String DIALECT_CLASS = "nuxeo.vcs.dialect";

    public static final Map<String, Class<? extends Dialect>> DIALECTS = new HashMap<String, Class<? extends Dialect>>();
    static {
        DIALECTS.put("H2", DialectH2.class);
        DIALECTS.put("MySQL", DialectMySQL.class);
        DIALECTS.put("Oracle", DialectOracle.class);
        DIALECTS.put("PostgreSQL", DialectPostgreSQL.class);
        DIALECTS.put("Microsoft SQL Server", DialectSQLServer.class);
        DIALECTS.put("HSQL Database Engine", DialectHSQLDB.class);
        DIALECTS.put("Apache Derby", DialectDerby.class);
    }

    public static final class JDBCInfo {
        public final String string;

        public final int jdbcType;

        public JDBCInfo(String string, int jdbcType) {
            this.string = string;
            this.jdbcType = jdbcType;
        }
    }

    public static JDBCInfo jdbcInfo(String string, int jdbcType) {
        return new JDBCInfo(string, jdbcType);
    }

    public static JDBCInfo jdbcInfo(String string, int length, int jdbcType) {
        return new JDBCInfo(String.format(string, Integer.valueOf(length)),
                jdbcType);
    }

    protected final BinaryManager binaryManager;

    protected final boolean storesUpperCaseIdentifiers;

    protected boolean fulltextDisabled;

    protected final boolean aclOptimizationsEnabled;

    /**
     * @since 5.7
     */
    protected boolean clusteringEnabled;

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
        String dialectClassName = Framework.getProperty(DIALECT_CLASS);
        if (dialectClassName == null) {
            dialectClassName = Framework.getProperty(DIALECT_CLASS + '.'
                    + databaseName.replace(" ", ""));
        }
        Class<? extends Dialect> dialectClass;
        if (dialectClassName == null) {
            dialectClass = DIALECTS.get(databaseName);
            if (dialectClass == null) {
                throw new StorageException("Unsupported database: "
                        + databaseName);
            }
        } else {
            Class<?> klass;
            try {
                ClassLoader cl = Thread.currentThread().getContextClassLoader();
                klass = cl.loadClass(dialectClassName);
            } catch (ClassNotFoundException e) {
                throw new StorageException(e);
            }
            if (!Dialect.class.isAssignableFrom(klass)) {
                throw new StorageException("Not a Dialect: " + dialectClassName);
            }
            dialectClass = (Class<? extends Dialect>) klass;
        }
        Constructor<? extends Dialect> ctor;
        try {
            ctor = dialectClass.getConstructor(DatabaseMetaData.class,
                    BinaryManager.class, RepositoryDescriptor.class);
        } catch (Exception e) {
            throw new StorageException("Bad constructor signature for: "
                    + dialectClassName, e);
        }
        Dialect dialect;
        try {
            dialect = ctor.newInstance(metadata, binaryManager,
                    repositoryDescriptor);
        } catch (InvocationTargetException e) {
            Throwable t = e.getTargetException();
            if (t instanceof StorageException) {
                throw (StorageException) t;
            } else {
                throw new StorageException(t.getMessage(), t);
            }
        } catch (Exception e) {
            throw new StorageException("Cannot construct dialect: "
                    + dialectClassName, e);
        }
        return dialect;
    }

    public Dialect(DatabaseMetaData metadata, BinaryManager binaryManager,
            RepositoryDescriptor repositoryDescriptor) throws StorageException {
        try {
            storesUpperCaseIdentifiers = metadata.storesUpperCaseIdentifiers();
        } catch (SQLException e) {
            throw new StorageException("An error has occured.", e);
        }
        this.binaryManager = binaryManager;
        if (repositoryDescriptor == null) {
            fulltextDisabled = true;
            aclOptimizationsEnabled = false;
            readAclMaxSize = 0;
            clusteringEnabled = false;
        } else {
            fulltextDisabled = repositoryDescriptor.fulltextDisabled;
            aclOptimizationsEnabled = repositoryDescriptor.aclOptimizationsEnabled;
            readAclMaxSize = repositoryDescriptor.readAclMaxSize;
            clusteringEnabled = repositoryDescriptor.clusteringEnabled;
        }
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

    public static final String ARRAY_SEP = "|";

    protected void setToPreparedStatementString(PreparedStatement ps,
            int index, Serializable value, Column column) throws SQLException {
        String v;
        ColumnType type = column.getType();
        if (type == ColumnType.BLOBID) {
            v = ((Binary) value).getDigest();
        } else if (type == ColumnType.SYSNAMEARRAY) {
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

    protected void setToPreparedStatementTimestamp(PreparedStatement ps,
            int index, Serializable value, Column column) throws SQLException {
        Calendar cal = (Calendar) value;
        Timestamp ts = new Timestamp(cal.getTimeInMillis());
        ps.setTimestamp(index, ts, cal); // cal passed for timezone
    }

    public abstract Serializable getFromResultSet(ResultSet rs, int index,
            Column column) throws SQLException;

    protected Serializable getFromResultSetString(ResultSet rs, int index,
            Column column) throws SQLException {
        String string = rs.getString(index);
        if (string == null) {
            return null;
        }
        ColumnType type = column.getType();
        if (type == ColumnType.BLOBID) {
            return getBinaryManager().getBinary(string);
        } else if (type == ColumnType.SYSNAMEARRAY) {
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

    protected Serializable getFromResultSetTimestamp(ResultSet rs, int index,
            Column column) throws SQLException {
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
     * Gets a CREATE INDEX statement for an index.
     *
     * @param indexName the index name (for fulltext)
     * @param indexType the index type
     * @param table the table
     * @param columns the columns to index
     * @param model the model
     */
    public String getCreateIndexSql(String indexName,
            Table.IndexType indexType, Table table, List<Column> columns,
            Model model) {
        List<String> qcols = new ArrayList<String>(columns.size());
        List<String> pcols = new ArrayList<String>(columns.size());
        for (Column col : columns) {
            qcols.add(col.getQuotedName());
            pcols.add(col.getPhysicalName());
        }
        String quotedIndexName = openQuote()
                + getIndexName(table.getKey(), pcols) + closeQuote();
        if (indexType == Table.IndexType.FULLTEXT) {
            return getCreateFulltextIndexSql(indexName, quotedIndexName, table,
                    columns, model);
        } else {
            String prefix = getCreateIndexPrefixSql(indexType, columns);
            return String.format("CREATE %s INDEX %s ON %s (%s)", prefix,
                    quotedIndexName, table.getQuotedName(),
                    StringUtils.join(qcols, ", "));
        }
    }

    // overridden by SQL Server for Azure CLUSTERED indexes
    public String getCreateIndexPrefixSql(Table.IndexType indexType,
            List<Column> columns) {
        return "";
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

        public static final String SPACE = " ";

        public Op op;

        /** The list of terms, if op is OR or AND */
        public List<FulltextQuery> terms;

        /** The word, if op is WORD or NOTWORD */
        public String word;

        /**
         * Checks if the word is a phrase.
         */
        public boolean isPhrase() {
            return word != null && word.contains(SPACE);
        }
    }

    /**
     * Analyzes a fulltext query into a generic datastructure that can be used
     * for each specific database.
     * <p>
     * List of terms containing only negative words are suppressed. Otherwise
     * negative words are put at the end of the lists of terms.
     */
    public FulltextQuery analyzeFulltextQuery(String query) {
        return new FulltextQueryAnalyzer().analyze(query);
    }

    public static class FulltextQueryAnalyzer {

        public static final String PLUS = "+";

        public static final String MINUS = "-";

        public static final char CSPACE = ' ';

        public static final String DOUBLE_QUOTES = "\"";

        public static final String OR = "OR";

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
            String[] words = split(query);
            for (Iterator<String> it = Arrays.asList(words).iterator(); it.hasNext();) {
                boolean plus = false;
                boolean minus = false;
                String word = it.next();
                if (ignored(word)) {
                    continue;
                }
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
                } else if (word.equalsIgnoreCase(OR)) {
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

        protected static final Pattern SEPARATOR = Pattern.compile("[ ]");

        protected String[] split(String query) {
            return SEPARATOR.split(query);
        }

        protected static final Pattern IGNORED = Pattern.compile("\\p{Punct}+");

        protected boolean ignored(String word) {
            if ("-".equals(word) || "+".equals(word) || word.contains("\"")) {
                return false; // dealt with later, different error
            }
            return IGNORED.matcher(word).matches();
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
                String or, String and, String andNot, String wordStart,
                String wordEnd, Set<Character> wordCharsReserved,
                String phraseStart, String phraseEnd, boolean quotePhraseWords) {
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
                    translate(term, buf, or, and, andNot, wordStart, wordEnd,
                            wordCharsReserved, phraseStart, phraseEnd, quotePhraseWords);
                }
                buf.append(')');
                return;
            } else {
                String word = ft.word.toLowerCase();
                if (ft.isPhrase()) {
                    if (quotePhraseWords) {
                        boolean first = true;
                        for (String w : word.split(" ")) {
                            if (!first) {
                                buf.append(" ");
                            }
                            first = false;
                            appendWord(w, buf, wordStart, wordEnd,
                                    wordCharsReserved);
                        }
                    } else {
                        buf.append(phraseStart);
                        buf.append(word);
                        buf.append(phraseEnd);
                    }
                } else {
                    appendWord(word, buf, wordStart, wordEnd, wordCharsReserved);
                }
            }
        }

        protected static void appendWord(String word, StringBuilder buf,
                String start, String end, Set<Character> reserved) {
            boolean quote = true;
            if (!reserved.isEmpty()) {
                for (char c : word.toCharArray()) {
                    if (reserved.contains(Character.valueOf(c))) {
                        quote = false;
                        break;
                    }
                }
            }
            if (quote) {
                buf.append(start);
            }
            buf.append(word);
            if (quote) {
                buf.append(end);
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
                return ft.isPhrase();
            }
        }

    }

    /**
     * Translate fulltext into a common pattern used by many servers.
     */
    public static String translateFulltext(FulltextQuery ft, String or,
            String and, String andNot, String phraseQuote) {
        StringBuilder buf = new StringBuilder();
        FulltextQueryAnalyzer.translate(ft, buf, or, and, andNot, "", "",
                Collections.<Character> emptySet(), phraseQuote, phraseQuote,
                false);
        return buf.toString();
    }

    /**
     * Translate fulltext into a common pattern used by many servers.
     */
    public static String translateFulltext(FulltextQuery ft, String or,
            String and, String andNot, String wordStart, String wordEnd,
            Set<Character> wordCharsReserved, String phraseStart,
            String phraseEnd, boolean quotePhraseWords) {
        StringBuilder buf = new StringBuilder();
        FulltextQueryAnalyzer.translate(ft, buf, or, and, andNot, wordStart,
                wordEnd, wordCharsReserved, phraseStart, phraseEnd,
                quotePhraseWords);
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
     * Gets the SQL fragment to match a mixin type.
     */
    public String getMatchMixinType(Column mixinsColumn, String mixin,
            boolean positive, String[] returnParam) {
        returnParam[0] = "%" + ARRAY_SEP + mixin + ARRAY_SEP + "%";
        return String.format("%s %s ?", mixinsColumn.getFullQuotedName(),
                positive ? "LIKE" : "NOT LIKE");
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
     * The dialect need an extra SQL statement to populate a user read acl cache
     * before running the query.
     *
     * @since 5.5
     */
    public boolean needsPrepareUserReadAcls() {
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
     * Get the expression to use to cast a column to a DATE type.
     *
     * @return a pattern for String.format with one parameter for the column
     *         name
     * @since 5.6
     */
    public String getDateCast() {
        return "CAST(%s AS DATE)";
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
     * Does a stored function returning an result set need to access it as a
     * single array instead of iterating over a normal result set's rows.
     * <p>
     * Oracle needs this.
     */
    public boolean supportsArraysReturnInsteadOfRows() {
        return false;
    }

    /**
     * Checks if the dialect supports storing arrays of system names (for mixins
     * for instance).
     */
    public boolean supportsSysNameArray() {
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
     * Returns the cluster node id, for some databases where this info is needed
     * at the Java level.
     */
    public String getClusterNodeIdSql() {
        return null;
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
     * Gets the SQL expression to prepare the user read acls cache.
     *
     * This can be used to populate a table cache.
     *
     * @since 5.5
     * @return and SQL expression with one parameter (principals)
     */
    public String getPrepareUserReadAclsSql() {
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

    /**
     * Gets the SQL function that returns the length of a blob, in bytes.
     */
    public String getBlobLengthFunction() {
        // the SQL-standard function (PostgreSQL, MySQL)
        return "OCTET_LENGTH";
    }

    /**
     * Let the dialect perform additional statements just after the connection
     * is opened.
     */
    public void performPostOpenStatements(Connection connection)
            throws SQLException {
    }

    /**
     * Gets additional SQL statements to execute after the CREATE TABLE when
     * creating an identity column.
     * <p>
     * Oracle needs both a sequence and a trigger.
     */
    public List<String> getPostCreateIdentityColumnSql(Column column) {
        return Collections.emptyList();
    }

    /**
     * Checks if an identity column is already defined as a primary key and does
     * not need a separate index added.
     * <p>
     * MySQL defines the identity column directly as primary key.
     */
    public boolean isIdentityAlreadyPrimary() {
        return false;
    }

    /**
     * True if the dialect returns the generated key for the identity from the
     * insert statement.
     * <p>
     * Oracle needs a separate call to CURRVAL.
     */
    public boolean hasIdentityGeneratedKey() {
        return true;
    }

    /**
     * Gets the SQL query to execute to retrieve the last generated identity
     * key.
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
     * Gets the SQL descending sort direction with option to sort nulls last.
     *
     * Use to unify database behavior.
     *
     * @return DESC or DESC NULLS LAST depending on dialects.
     */
    public String getDescending() {
        return " DESC";
    }

}
