/*
 * (C) Copyright 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
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

package org.nuxeo.ecm.core.storage.sql.jdbc.db;

import java.io.Serializable;
import java.sql.JDBCType;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import org.nuxeo.ecm.core.storage.sql.ColumnType;
import org.nuxeo.ecm.core.storage.sql.jdbc.dialect.Dialect;
import org.nuxeo.ecm.core.storage.sql.jdbc.dialect.Dialect.JDBCInfo;

/**
 * An SQL {@code column}.
 *
 * @author Florent Guillaume
 */
public class Column implements Serializable {

    private static final long serialVersionUID = 1L;

    protected final Table table;

    protected final Dialect dialect;

    protected final String physicalName;

    private final String quotedName;

    private final String freeVariableSetter;

    /** The abstract type. */
    private final ColumnType type;

    /**
     * The JDBC {@link java.sql.Types} type. Used for: - comparison with database introspected type - switch() to get
     * from result set or set to prepared statement - setNull to prepared statement
     */
    private int jdbcType;

    /** The JDBC type string. */
    private final String jdbcTypeString;

    /*
     * {@see java.sql.Array.getBaseType() value is 0 if this is not an array column
     */
    private int jdbcBaseType;

    /*
     * {@see java.sql.Array.getBaseTypeName() value is null if this is not an array column
     */
    private final String jdbcBaseTypeString;

    private final String key;

    private boolean identity;

    private boolean primary;

    private boolean nullable = true;

    private String defaultValue;

    /** For foreign key reference. */
    private Table foreignTable;

    private String foreignKey;

    /**
     * Creates a new column with the given name and type.
     *
     * @param table the column's table
     * @param physicalName the column physical name
     * @param type the column's type
     * @param key the associated field name
     */
    public Column(Table table, String physicalName, ColumnType type, String key) {
        this.table = table;
        dialect = table.getDialect();
        this.physicalName = physicalName;
        this.type = type;
        JDBCInfo jdbcInfo = dialect.getJDBCTypeAndString(type);
        jdbcType = jdbcInfo.jdbcType;
        jdbcTypeString = jdbcInfo.string;
        jdbcBaseType = jdbcInfo.jdbcBaseType;
        jdbcBaseTypeString = jdbcInfo.jdbcBaseTypeString;
        this.key = key;
        quotedName = dialect.openQuote() + physicalName + dialect.closeQuote();
        freeVariableSetter = dialect.getFreeVariableSetterForType(type);
    }

    /**
     * Creates a column from an existing column and an aliased table.
     */
    public Column(Column column, Table table) {
        this(table, column.physicalName, column.type, column.key);
    }

    public Table getTable() {
        return table;
    }

    public String getPhysicalName() {
        return physicalName;
    }

    public String getQuotedName() {
        return quotedName;
    }

    public String getFullQuotedName() {
        return table.getQuotedName() + '.' + quotedName;
    }

    public int getJdbcType() {
        return jdbcType;
    }

    public int getJdbcBaseType() {
        return jdbcBaseType;
    }

    public ColumnType getType() {
        return type;
    }

    public ColumnType getBaseType() {
        ColumnType baseType;
        if (type == ColumnType.ARRAY_BLOBID) {
            baseType = ColumnType.BLOBID;
        } else if (type == ColumnType.ARRAY_BOOLEAN) {
            baseType = ColumnType.BOOLEAN;
        } else if (type == ColumnType.ARRAY_CLOB) {
            baseType = ColumnType.CLOB;
        } else if (type == ColumnType.ARRAY_DOUBLE) {
            baseType = ColumnType.DOUBLE;
        } else if (type == ColumnType.ARRAY_INTEGER) {
            baseType = ColumnType.INTEGER;
        } else if (type == ColumnType.ARRAY_LONG) {
            baseType = ColumnType.LONG;
        } else if (type == ColumnType.ARRAY_STRING) {
            baseType = ColumnType.STRING;
        } else if (type == ColumnType.ARRAY_TIMESTAMP) {
            baseType = ColumnType.TIMESTAMP;
        } else {
            baseType = type;
        }
        return baseType;
    }

    public String getFreeVariableSetter() {
        return freeVariableSetter;
    }

    public boolean isArray() {
        return type.isArray();
    }

    public boolean isOpaque() {
        return type == ColumnType.FTINDEXED || type == ColumnType.FTSTORED;
    }

    public String checkJdbcType(int actual, String actualName, int actualSize) {
        int expected = jdbcType;
        if (actual == expected) {
            return null;
        }
        if (dialect.isAllowedConversion(expected, actual, actualName, actualSize)) {
            return null;
        }
        return String.format("SQL type mismatch for %s: expected %s, database has %s / %s(%s)", getFullQuotedName(),
                getJDBCTypeName(expected), getJDBCTypeName(actual), actualName, actualSize);
    }

    protected static String getJDBCTypeName(int expected) {
        try {
            return JDBCType.valueOf(expected).getName();
        } catch (IllegalArgumentException e) {
            return String.valueOf(expected);
        }
    }

    public String getKey() {
        return key;
    }

    public void setIdentity(boolean identity) {
        this.identity = identity;
    }

    public boolean isIdentity() {
        return identity;
    }

    public void setPrimary(boolean primary) {
        this.primary = primary;
    }

    public boolean isPrimary() {
        return primary;
    }

    public void setNullable(boolean nullable) {
        this.nullable = nullable;
    }

    public boolean isNullable() {
        return nullable;
    }

    public String getDefaultValue() {
        return defaultValue;
    }

    public void setDefaultValue(String defaultValue) {
        this.defaultValue = defaultValue;
    }

    public void setReferences(Table foreignTable, String foreignKey) {
        this.foreignTable = foreignTable;
        this.foreignKey = foreignKey;
    }

    public Table getForeignTable() {
        return foreignTable;
    }

    public String getForeignKey() {
        return foreignKey;
    }

    public String getSqlTypeString() {
        return jdbcTypeString;
    }

    public String getSqlBaseTypeString() {
        return jdbcBaseTypeString;
    }

    public void setToPreparedStatement(PreparedStatement ps, int index, Serializable value) throws SQLException {
        if (value == null) {
            ps.setNull(index, jdbcType);
            return;
        }
        if ((jdbcType == Types.ARRAY) && !(value instanceof Object[])) {
            throw new SQLException("Expected an array value instead of: " + value);
        }
        dialect.setToPreparedStatement(ps, index, value, this);
    }

    public Serializable getFromResultSet(ResultSet rs, int index) throws SQLException {
        Serializable result = dialect.getFromResultSet(rs, index, this);
        if (rs.wasNull()) {
            result = null;
        }
        return result;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + '(' + physicalName + ')';
    }

}
