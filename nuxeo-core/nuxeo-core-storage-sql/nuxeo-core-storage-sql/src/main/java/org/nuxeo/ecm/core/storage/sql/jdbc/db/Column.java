/*
 * (C) Copyright 2007-2008 Nuxeo SAS (http://nuxeo.com/) and contributors.
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

package org.nuxeo.ecm.core.storage.sql.jdbc.db;

import java.io.Serializable;
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

    public ColumnType getType() {
        return type;
    }

    public String getFreeVariableSetter() {
        return freeVariableSetter;
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

    public void setToPreparedStatement(PreparedStatement ps, int index,
            Serializable value) throws SQLException {
        if (value == null) {
            ps.setNull(index, jdbcType);
            return;
        }
        if (jdbcType == Types.ARRAY && !(value instanceof String[])) {
            throw new SQLException("Expected String[] instead of: " + value);
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
