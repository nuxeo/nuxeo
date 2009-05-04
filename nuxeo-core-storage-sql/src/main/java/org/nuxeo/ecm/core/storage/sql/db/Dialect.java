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
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
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
import org.nuxeo.ecm.core.storage.sql.Model;
import org.nuxeo.ecm.core.storage.sql.RepositoryDescriptor;

/**
 * A Dialect encapsulates knowledge about database-specific behavior.
 * 
 * @author Florent Guillaume
 */
public abstract class Dialect {

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
     * 
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

        if ("H2".equals(databaseName)) {
            return new DialectH2(metadata, repositoryDescriptor);
        }

        if ("PostgreSQL".equals(databaseName)) {
            return new DialectPostgreSQL(metadata, repositoryDescriptor);
        }

        throw new StorageException("Unsupported database: " + databaseName);
    }

    protected Dialect(Connection connection,
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
    }

    protected Dialect(org.hibernate.dialect.Dialect dialect,
            DatabaseMetaData metadata) throws StorageException {

        this.dialect = dialect;
        try {
            databaseName = metadata.getDatabaseProductName();
            databaseMajor = metadata.getDatabaseMajorVersion();
            storesUpperCaseIdentifiers = metadata.storesUpperCaseIdentifiers();
        } catch (SQLException e) {
            throw new StorageException(e);
        }
        dialectName = dialect.getClass().getSimpleName();
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
        if (dialect instanceof DerbyDialect && sqlType == Types.CLOB) {
            return "clob"; // different from DB2Dialect
        }
        return dialect.getTypeName(sqlType, length, precision, scale);
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
                || dialect instanceof H2Dialect;
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
        } else if ("H2".equals(databaseName)) {
            H2StoredProcedureInfoMaker maker = new H2StoredProcedureInfoMaker(
                    model, database);
            statements.add(maker.makeInTree());
            statements.add(maker.makeAccessAllowed());
        } else if ("PostgreSQL".equals(databaseName)) {
            PostgreSQLstoredProcedureInfoMaker maker = new PostgreSQLstoredProcedureInfoMaker(
                    model, database);
            statements.add(maker.makeInTree());
            statements.add(maker.makeAccessAllowed());
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
                                    + "    ELSIF curid = baseid THEN" //
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
    }

}
