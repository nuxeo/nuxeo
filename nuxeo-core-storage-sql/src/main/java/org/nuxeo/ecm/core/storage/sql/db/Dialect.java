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

import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.Array;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.hibernate.HibernateException;
import org.hibernate.dialect.DerbyDialect;
import org.hibernate.dialect.DialectFactory;
import org.hibernate.dialect.H2Dialect;
import org.hibernate.dialect.MySQL5InnoDBDialect;
import org.hibernate.dialect.MySQLDialect;
import org.hibernate.dialect.Oracle9Dialect;
import org.hibernate.dialect.PostgreSQLDialect;
import org.hibernate.dialect.SQLServerDialect;
import org.hibernate.exception.SQLExceptionConverter;
import org.nuxeo.common.utils.StringUtils;
import org.nuxeo.ecm.core.storage.StorageException;
import org.nuxeo.ecm.core.storage.sql.Model;
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

    protected final String fulltextCatalog;

    private static final String DEFAULT_FULLTEXT_ANALYZER_PG = "english";

    private static final String DEFAULT_FULLTEXT_ANALYZER_H2 = "org.apache.lucene.analysis.standard.StandardAnalyzer";

    private static final String DEFAULT_FULLTEXT_ANALYZER_MSSQL = "english";

    private static final String DEFAULT_FULLTEXT_CATALOG_MSSQL = "nuxeo";

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
                throw new StorageException("Cannot instantiate dialect for: "
                        + connection, e);
            }
        } else if ("MySQL".equals(databaseName)) {
            if (databaseMajor != 5) {
                throw new StorageException(String.format(
                        "MySQL version %s is not supported", databaseMajor));
            }
            dialect = new MySQL5InnoDBDialect();
        } else {
            try {
                dialect = DialectFactory.determineDialect(databaseName,
                        databaseMajor);
            } catch (HibernateException e) {
                throw new StorageException("Cannot determine dialect for: "
                        + connection, e);
            }
        }
        dialectName = dialect.getClass().getSimpleName();
        String ftAnalyzer = repositoryDescriptor.fulltextAnalyzer;
        if (ftAnalyzer == null) {
            // suitable defaults
            if (dialect instanceof PostgreSQLDialect) {
                ftAnalyzer = DEFAULT_FULLTEXT_ANALYZER_PG;
            }
            if (dialect instanceof H2Dialect) {
                ftAnalyzer = DEFAULT_FULLTEXT_ANALYZER_H2;
            }
            if (dialect instanceof SQLServerDialect) {
                ftAnalyzer = DEFAULT_FULLTEXT_ANALYZER_MSSQL;
            }
        }
        fulltextAnalyzer = ftAnalyzer;
        String ftCatalog = repositoryDescriptor.fulltextCatalog;
        if (ftCatalog == null) {
            if (dialect instanceof SQLServerDialect) {
                ftCatalog = DEFAULT_FULLTEXT_CATALOG_MSSQL;
            }
        }
        fulltextCatalog = ftCatalog;

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

    protected String makeName(String prefix, String string, String suffix) {
        int max = 999;
        if (dialect instanceof Oracle9Dialect) {
            max = 30;
        }
        if (prefix.length() + string.length() + suffix.length() > max) {
            MessageDigest digest;
            try {
                digest = MessageDigest.getInstance("MD5");
            } catch (NoSuchAlgorithmException e) {
                throw new RuntimeException(e.toString(), e);
            }
            byte[] bytes;
            try {
                bytes = string.getBytes("UTF-8");
            } catch (UnsupportedEncodingException e) {
                throw new RuntimeException(e.toString(), e);
            }
            digest.update(bytes, 0, bytes.length);
            string = toHexString(digest.digest()).substring(0, 8);
        }
        suffix = storesUpperCaseIdentifiers() ? suffix : suffix.toLowerCase();
        return prefix + string + suffix;
    }

    private static final char[] HEX_DIGITS = "0123456789ABCDEF".toCharArray();

    private static String toHexString(byte[] bytes) {
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
            if (dialect instanceof PostgreSQLDialect) {
                return "tsvector";
            }
            sqlType = Types.CLOB;
        }
        if (dialect instanceof DerbyDialect && sqlType == Types.CLOB) {
            return "clob"; // different from DB2Dialect
        }
        if (dialect instanceof SQLServerDialect) {
            if (sqlType == Types.VARCHAR) {
                return "NVARCHAR(" + length + ')';
            } else if (sqlType == Types.CLOB) {
                return "NVARCHAR(MAX)";
            }
        }
        if (dialect instanceof Oracle9Dialect) {
            if (sqlType == Types.VARCHAR) {
                if (length == 36) {
                    // uuid
                    return "VARCHAR2(36)";
                } else {
                    return "NVARCHAR2(" + length + ')';
                }
            } else if (sqlType == Types.CLOB) {
                return "NCLOB";
            }
        }
        return dialect.getTypeName(sqlType, length, precision, scale);
    }

    /**
     * Gets a CREATE INDEX statement for a normal index.
     */
    public String getCreateIndexSql(String indexName, String quotedName,
            List<String> qcols) {
        StringBuilder buf = new StringBuilder();
        buf.append("CREATE INDEX ");
        buf.append(indexName);
        buf.append(" ON ");
        buf.append(quotedName);
        buf.append(" (");
        buf.append(StringUtils.join(qcols, ", "));
        buf.append(')');
        return buf.toString();
    }

    /**
     * Gets a CREATE INDEX statement for a fulltext index.
     */
    public String getCreateFulltextIndexSql(String indexName, String tableName,
            List<String> columnNames) {
        StringBuilder buf = new StringBuilder();
        buf.append("CREATE");
        if (dialect instanceof MySQLDialect
                || dialect instanceof SQLServerDialect) {
            buf.append(" FULLTEXT");
        }
        buf.append(" INDEX");
        if (dialect instanceof PostgreSQLDialect
                || dialect instanceof MySQLDialect) {
            buf.append(' ');
            buf.append(indexName);
        }
        buf.append(" ON ");
        buf.append(tableName);

        // column names
        if (dialect instanceof MySQLDialect) {
            buf.append(" (");
            buf.append(StringUtils.join(columnNames, ", "));
            buf.append(')');
        }
        if (dialect instanceof SQLServerDialect) {
            buf.append(" (");
            Iterator<String> it = columnNames.iterator();
            while (it.hasNext()) {
                buf.append(it.next());
                buf.append(" LANGUAGE ");
                buf.append(getQuotedFulltextAnalyzer());
                if (it.hasNext()) {
                    buf.append(", ");
                }
            }
            buf.append(')');
        }

        // trailer
        if (dialect instanceof PostgreSQLDialect) {
            buf.append(String.format(" USING GIN(%s)", columnNames.get(0)));
        }
        if (dialect instanceof SQLServerDialect) {
            String fulltextUniqueIndex = "[fulltext_pk]";
            buf.append(String.format(" KEY INDEX %s ON [%s]",
                    fulltextUniqueIndex, fulltextCatalog));
        }

        return buf.toString();
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
        if (dialect instanceof MySQLDialect) {
            String whereExpr = "MATCH (`fulltext`.`simpletext`, `fulltext`.`binarytext`) AGAINST (?)";
            return new String[] { null, null, whereExpr, fulltextQuery };
        }
        if (dialect instanceof SQLServerDialect) {
            String whereExpr = String.format(
                    "FREETEXT([fulltext].*, ?, LANGUAGE %s)",
                    getQuotedFulltextAnalyzer());
            return new String[] { null, null, whereExpr, fulltextQuery };
        }
        throw new UnsupportedOperationException();
    }

    /**
     * Specifies what columns of the fulltext table have to be indexed.
     *
     * @return 0 for none, 1 for the synthetic one, 2 for the individual ones
     */
    public int getFulltextIndexedColumns() {
        if (dialect instanceof PostgreSQLDialect) {
            return 1;
        }
        if (dialect instanceof MySQLDialect
                || dialect instanceof SQLServerDialect) {
            return 2;
        }
        return 0;
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
        if (type == Column.ExtendedTypes.FULLTEXT
                && dialect instanceof PostgreSQLDialect) {
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

    public String getQuotedFulltextAnalyzer() {
        if (!Character.isDigit(fulltextAnalyzer.charAt(0))) {
            return String.format("'%s'", fulltextAnalyzer);
        }
        return fulltextAnalyzer;
    }

    public String getNoColumnsInsertString() {
        return dialect.getNoColumnsInsertString();
    }

    public String getNullColumnString() {
        return dialect.getNullColumnString();
    }

    public String getTableTypeString(Table table) {
        if (dialect instanceof MySQLDialect && table.hasFulltextIndex()) {
            // the fulltext table in MySQL needs to be MyISAM, doh!
            return " ENGINE=MyISAM";
        }
        // this is just for MySQL to add its ENGINE=InnoDB
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
        if (dialect instanceof SQLServerDialect) {
            return false;
        }
        return true;
    }

    // "ADD COLUMN" or "ADD"
    public String getAddColumnString() {
        return dialect.getAddColumnString().toUpperCase();
    }

    /**
     * Does the dialect support UPDATE t SET ... FROM t, u WHERE ... ?
     */
    public boolean supportsUpdateFrom() {
        if (dialect instanceof PostgreSQLDialect
                || dialect instanceof MySQLDialect
                || dialect instanceof SQLServerDialect) {
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
        if (dialect instanceof MySQLDialect
                || dialect instanceof SQLServerDialect) {
            return true;
        }
        // not reached
        return true;
    }

    public boolean needsOrderByKeysAfterDistinct() {
        return dialect instanceof PostgreSQLDialect
                || dialect instanceof H2Dialect
                || dialect instanceof SQLServerDialect;
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
     * @param idColumnName the quoted name of the id column to use
     * @return an SQL expression with two parameters (principals and
     *         permissions) that is true if access is allowed
     */
    public String getSecurityCheckSql(String idColumnName) {
        return getBooleanResult(String.format("NX_ACCESS_ALLOWED(%s, ?, ?)",
                idColumnName));
    }

    /**
     * Gets the expression to use to check tree membership.
     *
     * @param idColumnName the quoted name of the id column to use
     * @return an SQL expression with one parameters for the based id that is
     *         true if the document is under base id
     */
    public String getInTreeSql(String idColumnName) {
        return getBooleanResult(String.format("NX_IN_TREE(%s, ?)", idColumnName));
    }

    protected String getBooleanResult(String sql) {
        if (dialect instanceof DerbyDialect) {
            // dialect has no boolean functions
            sql += " = 1";
        }
        if (dialect instanceof SQLServerDialect) {
            sql = "dbo." + sql + " = 1";
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

    /**
     * Gets the additional statements to execute (stored procedures and
     * triggers) when creating the database.
     */
    public Collection<ConditionalStatement> getConditionalStatements(
            Model model, Database database) {
        List<ConditionalStatement> statements = new LinkedList<ConditionalStatement>();
        if ("Apache Derby".equals(databaseName)) {
            DerbyStoredProcedureInfoMaker maker = new DerbyStoredProcedureInfoMaker(
                    model, database);
            statements.add(maker.makeInTree());
            statements.add(maker.makeAccessAllowed());
            statements.add(maker.makeParseFullText());
            statements.add(maker.makeContainsFullText());
            statements.add(maker.makeFTInsertTrigger());
            statements.add(maker.makeFTUpdateTrigger());
        } else if ("H2".equals(databaseName)) {
            H2StoredProcedureInfoMaker maker = new H2StoredProcedureInfoMaker(
                    model, database);
            statements.add(maker.makeInTree());
            statements.add(maker.makeAccessAllowed());
            statements.add(maker.makeFTInit());
            statements.add(maker.makeFTIndex());
        } else if ("PostgreSQL".equals(databaseName)) {
            PostgreSQLstoredProcedureInfoMaker maker = new PostgreSQLstoredProcedureInfoMaker(
                    model, database);
            statements.add(maker.makeInTree());
            statements.add(maker.makeAccessAllowed());
            statements.add(maker.makeToTSVector());
            statements.add(maker.makeContainsFullText());
            statements.add(maker.makeConsolidateFullText());
            statements.add(maker.makeFTTrigger());
        } else if ("MySQL".equals(databaseName)) {
            MySQLstoredProcedureInfoMaker maker = new MySQLstoredProcedureInfoMaker(
                    model, database);
            // statements.add(maker.makeDebugTable());
            // statements.add(maker.makeNxDebug());
            statements.add(maker.makeInTree());
            statements.add(maker.makeAccessAllowed());
        } else if ("Microsoft SQL Server".equals(databaseName)) {
            MSSQLstoredProcedureInfoMaker maker = new MSSQLstoredProcedureInfoMaker(
                    model, database);
            statements.add(maker.makeCascadeDeleteTrigger());
            statements.add(maker.makeAccessAllowed());
            statements.add(maker.makeInTree());
            statements.add(maker.makeFTCatalog());
        }
        return statements;
    }

    /**
     * Class holding info about a conditional statement whose execution may
     * depend on a preceding one to check if it's needed.
     */
    public static class ConditionalStatement {

        /**
         * Does this have to be executed early or late?
         */
        public final boolean early;

        /**
         * If {@code TRUE}, then always to the {@link #preStatement}, if {@code
         * FALSE} never do it, if {@code null} then use {@link #checkStatement}
         * to decide.
         */
        public final Boolean doPre;

        /**
         * If this returns something, then do the {@link #preStatement}.
         */
        public final String checkStatement;

        /**
         * Statement to execute before the actual statement.
         */
        public final String preStatement;

        /**
         * Main statement.
         */
        public final String statement;

        public ConditionalStatement(boolean early, Boolean doPre,
                String checkStatement, String preStatement, String statement) {
            this.early = early;
            this.doPre = doPre;
            this.checkStatement = checkStatement;
            this.preStatement = preStatement;
            this.statement = statement;
        }
    }

    public class DerbyStoredProcedureInfoMaker {

        private final String idType;

        private final String methodSuffix;

        private final String className = "org.nuxeo.ecm.core.storage.sql.db.DerbyFunctions";

        private final Model model;

        private final Database database;

        public DerbyStoredProcedureInfoMaker(Model model, Database database) {
            this.model = model;
            this.database = database;
            switch (model.idGenPolicy) {
            case APP_UUID:
                idType = "VARCHAR(36)";
                methodSuffix = "String";
                break;
            case DB_IDENTITY:
                idType = "INTEGER";
                methodSuffix = "Long";
                break;
            default:
                throw new AssertionError(model.idGenPolicy);
            }
        }

        public ConditionalStatement makeInTree() {
            return makeFunction("NX_IN_TREE",
                    "(ID %s, BASEID %<s) RETURNS SMALLINT", "isInTree"
                            + methodSuffix, "READS SQL DATA");
        }

        public ConditionalStatement makeParseFullText() {
            return makeFunction(
                    "NX_PARSE_FULLTEXT",
                    "(S1 VARCHAR(10000), S2 VARCHAR(10000)) RETURNS VARCHAR(10000)",
                    "parseFullText", "");
        }

        public ConditionalStatement makeContainsFullText() {
            return makeFunction(
                    "NX_CONTAINS",
                    "(FT VARCHAR(10000), QUERY VARCHAR(10000)) RETURNS SMALLINT",
                    "matchesFullTextDerby", "");
        }

        public ConditionalStatement makeFTInsertTrigger() {
            Table ft = database.getTable(model.FULLTEXT_TABLE_NAME);
            Column ftft = ft.getColumn(model.FULLTEXT_FULLTEXT_KEY);
            Column ftst = ft.getColumn(model.FULLTEXT_SIMPLETEXT_KEY);
            Column ftbt = ft.getColumn(model.FULLTEXT_BINARYTEXT_KEY);
            Column ftid = ft.getColumn(model.MAIN_KEY);
            return makeTrigger(
                    "NX_TRIG_FT_INSERT", //
                    String.format(
                            "AFTER INSERT ON %1$s "//
                                    + "REFERENCING NEW AS NEW " //
                                    + "FOR EACH ROW "//
                                    + "UPDATE %1$s " //
                                    + "SET %2$s = NX_PARSE_FULLTEXT(CAST(%3$s AS VARCHAR(10000)), CAST(%4$s AS VARCHAR(10000))) " //
                                    + "WHERE %5$s = NEW.%5$s", //
                            ft.getQuotedName(), // 1 table "FULLTEXT"
                            ftft.getQuotedName(), // 2 column "TEXT"
                            ftst.getQuotedName(), // 3 column "SIMPLETEXT"
                            ftbt.getQuotedName(), // 4 column "BINARYTEXT"
                            ftid.getQuotedName() // 5 column "ID"
                    ));
        }

        public ConditionalStatement makeFTUpdateTrigger() {
            Table ft = database.getTable(model.FULLTEXT_TABLE_NAME);
            Column ftft = ft.getColumn(model.FULLTEXT_FULLTEXT_KEY);
            Column ftst = ft.getColumn(model.FULLTEXT_SIMPLETEXT_KEY);
            Column ftbt = ft.getColumn(model.FULLTEXT_BINARYTEXT_KEY);
            Column ftid = ft.getColumn(model.MAIN_KEY);
            return makeTrigger(
                    "NX_TRIG_FT_UPDATE", //
                    String.format(
                            "AFTER UPDATE OF %3$s, %4$s ON %1$s "//
                                    + "REFERENCING NEW AS NEW " //
                                    + "FOR EACH ROW "//
                                    + "UPDATE %1$s " //
                                    + "SET %2$s = NX_PARSE_FULLTEXT(CAST(%3$s AS VARCHAR(10000)), CAST(%4$s AS VARCHAR(10000))) " //
                                    + "WHERE %5$s = NEW.%5$s", //
                            ft.getQuotedName(), // 1 table "FULLTEXT"
                            ftft.getQuotedName(), // 2 column "TEXT"
                            ftst.getQuotedName(), // 3 column "SIMPLETEXT"
                            ftbt.getQuotedName(), // 4 column "BINARYTEXT"
                            ftid.getQuotedName() // 5 column "ID"
                    ));
        }

        public ConditionalStatement makeAccessAllowed() {
            return makeFunction(
                    "NX_ACCESS_ALLOWED",
                    "(ID %s, PRINCIPALS VARCHAR(10000), PERMISSIONS VARCHAR(10000)) RETURNS SMALLINT",
                    "isAccessAllowed" + methodSuffix, "READS SQL DATA");
        }

        protected ConditionalStatement makeFunction(String functionName,
                String proto, String methodName, String info) {
            proto = String.format(proto, idType);
            return new ConditionalStatement(
                    true, // early
                    null, // do a drop check
                    String.format(
                            "SELECT ALIAS FROM SYS.SYSALIASES WHERE ALIAS = '%s' AND ALIASTYPE = 'F'",
                            functionName), //
                    String.format("DROP FUNCTION %s", functionName), //
                    String.format("CREATE FUNCTION %s%s " //
                            + "LANGUAGE JAVA " //
                            + "PARAMETER STYLE JAVA " //
                            + "EXTERNAL NAME '%s.%s' " //
                            + "%s", //
                            functionName, proto, //
                            className, methodName, info));
        }

        public ConditionalStatement makeTrigger(String triggerName, String body) {
            return new ConditionalStatement(
                    false, // late
                    null, // do a drop check
                    String.format(
                            "SELECT TRIGGERNAME FROM SYS.SYSTRIGGERS WHERE TRIGGERNAME = '%s'",
                            triggerName), //
                    String.format("DROP TRIGGER %s", triggerName), //
                    String.format("CREATE TRIGGER %s %s", triggerName, body));

        }
    }

    public class H2StoredProcedureInfoMaker {

        private final String methodSuffix;

        private static final String h2Functions = "org.nuxeo.ecm.core.storage.sql.db.H2Functions";

        private static final String h2Fulltext = "org.nuxeo.ecm.core.storage.sql.db.H2Fulltext";

        private final Model model;

        private final Database database;

        public H2StoredProcedureInfoMaker(Model model, Database database) {
            this.model = model;
            this.database = database;
            switch (model.idGenPolicy) {
            case APP_UUID:
                methodSuffix = "String";
                break;
            case DB_IDENTITY:
                methodSuffix = "Long";
                break;
            default:
                throw new AssertionError(model.idGenPolicy);
            }
        }

        public ConditionalStatement makeInTree() {
            return makeFunction("NX_IN_TREE", "isInTree" + methodSuffix);
        }

        public ConditionalStatement makeAccessAllowed() {
            return makeFunction("NX_ACCESS_ALLOWED", "isAccessAllowed"
                    + methodSuffix);
        }

        public ConditionalStatement makeFTInit() {
            return new ConditionalStatement( //
                    false, // late
                    Boolean.FALSE, // no drop
                    null, //
                    null, //
                    String.format(
                            "CREATE ALIAS IF NOT EXISTS NXFT_INIT FOR \"%s.init\"; "
                                    + "CALL NXFT_INIT()", h2Fulltext));
        }

        public ConditionalStatement makeFTIndex() {
            Table ft = database.getTable(model.FULLTEXT_TABLE_NAME);
            Column ftst = ft.getColumn(model.FULLTEXT_SIMPLETEXT_KEY);
            Column ftbt = ft.getColumn(model.FULLTEXT_BINARYTEXT_KEY);
            return new ConditionalStatement(
                    false, // late
                    Boolean.FALSE, // no drop
                    null, //
                    null, //
                    String.format(
                            "CALL NXFT_CREATE_INDEX('PUBLIC', '%s', ('%s', '%s'), '%s')",
                            ft.getName(), ftst.getPhysicalName(),
                            ftbt.getPhysicalName(), getFulltextAnalyzer()));
        }

        protected ConditionalStatement makeFunction(String functionName,
                String methodName) {
            return new ConditionalStatement( //
                    true, // early
                    Boolean.TRUE, // always drop
                    null, //
                    String.format("DROP ALIAS IF EXISTS %s", functionName), //
                    String.format("CREATE ALIAS %s FOR \"%s.%s\"",
                            functionName, h2Functions, methodName));
        }
    }

    public class PostgreSQLstoredProcedureInfoMaker {

        private final String idType;

        private final Model model;

        private final Database database;

        public PostgreSQLstoredProcedureInfoMaker(Model model, Database database) {
            this.model = model;
            this.database = database;
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
        }

        public ConditionalStatement makeInTree() {
            return new ConditionalStatement(
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
                            , idType));
        }

        public ConditionalStatement makeAccessAllowed() {
            return new ConditionalStatement(
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
                            , idType));
        }

        public ConditionalStatement makeToTSVector() {
            return new ConditionalStatement( //
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
                            , getFulltextAnalyzer()));
        }

        public ConditionalStatement makeContainsFullText() {
            return new ConditionalStatement( //
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
                            , getFulltextAnalyzer()));
        }

        public ConditionalStatement makeFTTrigger() {
            Table ft = database.getTable(model.FULLTEXT_TABLE_NAME);
            String qname = ft.getQuotedName();
            return new ConditionalStatement(
                    false, // late
                    Boolean.TRUE, // do a drop
                    null, //
                    String.format(
                            "DROP TRIGGER IF EXISTS NX_TRIG_FT_UPDATE ON %s",
                            qname),
                    String.format(
                            "CREATE TRIGGER NX_TRIG_FT_UPDATE " //
                                    + "BEFORE INSERT OR UPDATE ON %s "
                                    + "FOR EACH ROW EXECUTE PROCEDURE NX_UPDATE_FULLTEXT()" //
                            , qname));
        }

        public ConditionalStatement makeConsolidateFullText() {
            Table ft = database.getTable(model.FULLTEXT_TABLE_NAME);
            Column ftft = ft.getColumn(model.FULLTEXT_FULLTEXT_KEY);
            Column ftst = ft.getColumn(model.FULLTEXT_SIMPLETEXT_KEY);
            Column ftbt = ft.getColumn(model.FULLTEXT_BINARYTEXT_KEY);
            return new ConditionalStatement( //
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
                            , ftft.getQuotedName(), ftst.getQuotedName(),
                            ftbt.getQuotedName()));
        }
    }

    public class MySQLstoredProcedureInfoMaker {

        private final String idType;

        private final Model model;

        private final Database database;

        public MySQLstoredProcedureInfoMaker(Model model, Database database) {
            this.model = model;
            this.database = database;
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
        }

        public ConditionalStatement makeDebugTable() {
            return new ConditionalStatement(
                    true, // early
                    Boolean.TRUE, // always drop
                    null, //
                    "DROP TABLE IF EXISTS NX_DEBUG_TABLE", //
                    "CREATE TABLE NX_DEBUG_TABLE (id INTEGER AUTO_INCREMENT PRIMARY KEY, log VARCHAR(10000))");
        }

        public ConditionalStatement makeNxDebug() {
            return new ConditionalStatement(
                    true, // early
                    Boolean.TRUE, // always drop
                    null, //
                    "DROP PROCEDURE IF EXISTS NX_DEBUG", //
                    String.format("CREATE PROCEDURE NX_DEBUG(line VARCHAR(10000)) " //
                            + "LANGUAGE SQL " //
                            + "BEGIN " //
                            + "  INSERT INTO NX_DEBUG_TABLE (log) values (line);" //
                            + "END" //
                    ));
        }

        public ConditionalStatement makeInTree() {
            return new ConditionalStatement(
                    true, // early
                    Boolean.TRUE, // always drop
                    null, //
                    "DROP FUNCTION IF EXISTS NX_IN_TREE", //
                    String.format(
                            "CREATE FUNCTION NX_IN_TREE(id %s, baseid %<s) " //
                                    + "RETURNS BOOLEAN " //
                                    + "LANGUAGE SQL " //
                                    + "READS SQL DATA " //
                                    + "BEGIN" //
                                    + "  DECLARE curid %<s DEFAULT id;" //
                                    + "  IF baseid IS NULL OR id IS NULL OR baseid = id THEN" //
                                    + "    RETURN FALSE;" //
                                    + "  END IF;" //
                                    + "  LOOP" //
                                    + "    SELECT parentid INTO curid FROM hierarchy WHERE hierarchy.id = curid;" //
                                    + "    IF curid IS NULL THEN" //
                                    + "      RETURN FALSE; " //
                                    + "    ELSEIF curid = baseid THEN" //
                                    + "      RETURN TRUE;" //
                                    + "    END IF;" //
                                    + "  END LOOP;" //
                                    + "END" //
                            , idType));
        }

        public ConditionalStatement makeAccessAllowed() {
            return new ConditionalStatement(
                    true, // early
                    Boolean.TRUE, // always drop
                    null, //
                    "DROP FUNCTION IF EXISTS NX_ACCESS_ALLOWED", //
                    String.format(
                            "CREATE FUNCTION NX_ACCESS_ALLOWED" //
                                    + "(id %s, users VARCHAR(10000), perms VARCHAR(10000)) " //
                                    + "RETURNS BOOLEAN " //
                                    + "BEGIN" //
                                    + "  DECLARE allusers VARCHAR(10000) DEFAULT CONCAT('|',users,'|');" //
                                    + "  DECLARE allperms VARCHAR(10000) DEFAULT CONCAT('|',perms,'|');" //
                                    + "  DECLARE first BOOLEAN DEFAULT TRUE;" //
                                    + "  DECLARE curid %<s DEFAULT id;" //
                                    + "  DECLARE newid %<s;" //
                                    + "  DECLARE gr BIT;" //
                                    + "  DECLARE pe VARCHAR(1000);" //
                                    + "  DECLARE us VARCHAR(1000);" //
                                    + "  WHILE curid IS NOT NULL DO" //
                                    + "    BEGIN" //
                                    + "      DECLARE done BOOLEAN DEFAULT FALSE;" //
                                    + "      DECLARE cur CURSOR FOR" //
                                    + "        SELECT `grant`, `permission`, `user` FROM `acls`" //
                                    + "        WHERE `acls`.`id` = curid ORDER BY `pos`;" //
                                    + "      DECLARE CONTINUE HANDLER FOR NOT FOUND SET done = TRUE;" //
                                    + "      OPEN cur;" //
                                    + "      REPEAT " //
                                    + "        FETCH cur INTO gr, pe, us;" //
                                    + "        IF NOT done THEN" //
                                    + "          IF LOCATE(CONCAT('|',us,'|'), allusers) <> 0 AND LOCATE(CONCAT('|',pe,'|'), allperms) <> 0 THEN" //
                                    + "            CLOSE cur;" //
                                    + "            RETURN gr;" //
                                    + "          END IF;" //
                                    + "        END IF;" //
                                    + "      UNTIL done END REPEAT;" //
                                    + "      CLOSE cur;" //
                                    + "    END;" //
                                    + "    SELECT parentid INTO newid FROM hierarchy WHERE hierarchy.id = curid;" //
                                    + "    IF first AND newid IS NULL THEN" //
                                    + "      SELECT versionableid INTO newid FROM versions WHERE versions.id = curid;" //
                                    + "    END IF;" //
                                    + "    SET first = FALSE;" //
                                    + "    SET curid = newid;" //
                                    + "  END WHILE;" //
                                    + "  RETURN FALSE; " //
                                    + "END" //
                            , idType));
        }
    }

    public class MSSQLstoredProcedureInfoMaker {

        private final String idType;

        private final Model model;

        private final Database database;

        public MSSQLstoredProcedureInfoMaker(Model model, Database database) {
            this.model = model;
            this.database = database;
            switch (model.idGenPolicy) {
            case APP_UUID:
                idType = "NVARCHAR(36)";
                break;
            case DB_IDENTITY:
                idType = "INTEGER";
                break;
            default:
                throw new AssertionError(model.idGenPolicy);
            }
        }

        public ConditionalStatement makeCascadeDeleteTrigger() {
            return new ConditionalStatement(
                    false, // late
                    Boolean.TRUE, // always drop
                    null, //
                    "IF OBJECT_ID('dbo.nxTrigCascadeDelete', 'TR') IS NOT NULL DROP TRIGGER dbo.nxTrigCascadeDelete", //
                    // FIXME: bad format string (parameter not used)
                    String.format(
                            "CREATE TRIGGER nxTrigCascadeDelete ON [hierarchy] " //
                                    + "INSTEAD OF DELETE AS " //
                                    + "BEGIN" //
                                    + "  SET NOCOUNT ON;" //
                                    + "  WITH cte(id, parentid) AS (" //
                                    + "    SELECT id, parentid" //
                                    + "    FROM deleted" //
                                    + "  UNION ALL" //
                                    + "    SELECT h.id, h.parentid" //
                                    + "    FROM [hierarchy] h" //
                                    + "    JOIN cte ON cte.id = h.parentid" //
                                    + "  )" //
                                    + "  DELETE FROM [hierarchy]" //
                                    + "    FROM [hierarchy] h" //
                                    + "    JOIN cte" //
                                    + "    ON cte.id = h.id; " //
                                    + "END" //
                            , idType));
        }

        public ConditionalStatement makeAccessAllowed() {
            return new ConditionalStatement(
                    false, // late
                    Boolean.TRUE, // always drop
                    null, //
                    "IF OBJECT_ID('dbo.NX_ACCESS_ALLOWED', 'FN') IS NOT NULL DROP FUNCTION dbo.NX_ACCESS_ALLOWED", //
                    String.format(
                            "CREATE FUNCTION NX_ACCESS_ALLOWED" //
                                    + "(@id %s, @users NVARCHAR(4000), @perms NVARCHAR(4000)) " //
                                    + "RETURNS TINYINT AS " //
                                    + "BEGIN" //
                                    + "  DECLARE @allusers NVARCHAR(4000);" //
                                    + "  DECLARE @allperms NVARCHAR(4000);" //
                                    + "  DECLARE @first TINYINT;" //
                                    + "  DECLARE @curid %<s;" //
                                    + "  DECLARE @newid %<s;" //
                                    + "  DECLARE @gr TINYINT;" //
                                    + "  DECLARE @pe VARCHAR(1000);" //
                                    + "  DECLARE @us VARCHAR(1000);" //
                                    + "  SET @allusers = N'|' + @users + N'|';" //
                                    + "  SET @allperms = N'|' + @perms + N'|';" //
                                    + "  SET @first = 1;" //
                                    + "  SET @curid = @id;" //
                                    + "  WHILE @curid IS NOT NULL BEGIN" //
                                    + "    DECLARE @cur CURSOR;" //
                                    + "    SET @cur = CURSOR FAST_FORWARD FOR" //
                                    + "      SELECT [grant], [permission], [user] FROM [acls]" //
                                    + "      WHERE [id] = @curid ORDER BY [pos];" //
                                    + "    OPEN @cur;" //
                                    + "    FETCH FROM @cur INTO @gr, @pe, @us;" //
                                    + "    WHILE @@FETCH_STATUS = 0 BEGIN" //
                                    + "      IF @allusers LIKE (N'%%|' + @us + N'|%%')" //
                                    + "        AND @allperms LIKE (N'%%|' + @pe + N'|%%')" //
                                    + "      BEGIN" //
                                    + "        CLOSE @cur;" //
                                    + "        RETURN @gr;" //
                                    + "      END;" //
                                    + "      FETCH FROM @cur INTO @gr, @pe, @us;" //
                                    + "    END;" //
                                    + "    CLOSE @cur;" //
                                    + "    SET @newid = (SELECT [parentid] FROM [hierarchy] WHERE [id] = @curid);" //
                                    + "    IF @first = 1 AND @newid IS NULL BEGIN" //
                                    + "      SET @newid = (SELECT [versionableid] FROM [versions] WHERE [id] = @curid);" //
                                    + "    END;" //
                                    + "    SET @first = 0;" //
                                    + "    SET @curid = @newid;" //
                                    + "  END;" //
                                    + "  RETURN 0; " //
                                    + "END" //
                            , idType));
        }

        public ConditionalStatement makeInTree() {
            return new ConditionalStatement(
                    false, // late
                    Boolean.TRUE, // always drop
                    null, //
                    "IF OBJECT_ID('dbo.NX_IN_TREE', 'FN') IS NOT NULL DROP FUNCTION dbo.NX_IN_TREE", //
                    String.format(
                            "CREATE FUNCTION NX_IN_TREE(@id %s, @baseid %<s) " //
                                    + "RETURNS TINYINT AS " //
                                    + "BEGIN" //
                                    + "  DECLARE @curid %<s;" //
                                    + "  IF @baseid IS NULL OR @id IS NULL OR @baseid = @id RETURN 0;" //
                                    + "  SET @curid = @id;" //
                                    + "  WHILE @curid IS NOT NULL BEGIN" //
                                    + "    SET @curid = (SELECT [parentid] FROM [hierarchy] WHERE [id] = @curid);" //
                                    + "    IF @curid = @baseid RETURN 1;" //
                                    + "  END;" //
                                    + "  RETURN 0;" //
                                    + "END" //
                            , idType));
        }

        public ConditionalStatement makeFTCatalog() {
            return new ConditionalStatement(true, // early
                    null, // do a check
                    // strange inverted condition because this is designed to
                    // test drops
                    String.format(
                            "IF EXISTS(SELECT name FROM sys.fulltext_catalogs WHERE name = '%s') "
                                    + "SELECT * FROM sys.tables WHERE 1 = 0 "
                                    + "ELSE SELECT 1", //
                            fulltextCatalog), //
                    String.format("CREATE FULLTEXT CATALOG [%s]",
                            fulltextCatalog), //
                    "SELECT 1");
        }
    }

}
