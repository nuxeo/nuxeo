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

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;
import java.sql.Types;

import org.hibernate.HibernateException;
import org.hibernate.dialect.DerbyDialect;
import org.hibernate.dialect.DialectFactory;
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
        try {
            dialect = DialectFactory.determineDialect(getDatabaseName(),
                    databaseMajor);
        } catch (HibernateException e) {
            throw new StorageException("Cannot determine dialect for: " +
                    connection, e);
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

    /**
     * When doing a comparison between a text field and something else, is some
     * casting required and with what pattern?
     * <p>
     * Needed for Derby where CLOB are not comparable by default.
     *
     * @return a pattern for String.format with one parameter for the column
     *         name and one for the width
     */
    public String textComparisonCasting() {
        if (dialect instanceof DerbyDialect) {
            return "CAST(%1$s AS VARCHAR(%2$d))";
        }
        return null;
    }

}
