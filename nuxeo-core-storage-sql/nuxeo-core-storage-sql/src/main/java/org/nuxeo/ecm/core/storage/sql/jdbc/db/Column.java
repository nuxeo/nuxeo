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

package org.nuxeo.ecm.core.storage.sql.jdbc.db;

import java.io.Serializable;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.Calendar;

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
     * The JDBC {@link java.sql.Types} type. Used for:
     *
     * - comparison with database introspected type
     *
     * - switch() to get from result set or set to prepared statement
     *
     * - setNull to prepared statement
     */
    private int jdbcType;

    /** The JDBC type string. */
    private final String jdbcTypeString;
    
    /*
     * {@see java.sql.Array.getBaseType()
     * 
     * value is 0 if this is not an array column
     */
    private int jdbcBaseType;
    
    /*
     * {@see java.sql.Array.getBaseTypeName()
     * 
     * value is null if this is not an array column
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

    public boolean setJdbcType(int actual, String actualName, int actualSize) {
        int expected = jdbcType;
        if (actual == expected) {
            return true;
        }
        if (dialect.isAllowedConversion(expected, actual, actualName,
                actualSize)) {
            return true;
        }
        return false;
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

    public void setToPreparedStatement(PreparedStatement ps, int index,
            Serializable value) throws SQLException {
        if (value == null) {
            ps.setNull(index, jdbcType);
            return;
        }
        if ((jdbcType == Types.ARRAY) && !(value instanceof Object[])) {
            throw new SQLException("Expected an array value instead of: " + value);
        }
        dialect.setToPreparedStatement(ps, index, value, this);
    }

    public Serializable getFromResultSet(ResultSet rs, int index)
            throws SQLException {
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
