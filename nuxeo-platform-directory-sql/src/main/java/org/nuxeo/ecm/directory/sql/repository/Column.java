/*
 * (C) Copyright 2006-2007 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *
 * $Id: Column.java 18286 2007-05-06 02:18:58Z fguillaume $
 */

package org.nuxeo.ecm.directory.sql.repository;

import java.io.Serializable;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;

import org.hibernate.dialect.Dialect;

/**
 * An SQL {@code column}.
 *
 * @author Florent Guillaume
 *
 */
public class Column implements Serializable {

    private static final long serialVersionUID = 1L;

    private final String name;

    /** The {@link java.sql.Types} type */
    private final int sqlType;

    private final String key;

    private boolean identity;

    private boolean primary;

    private int length = 255; // Hibernate default

    private int precision = 19; // Hibernate default

    private int scale = 2; // Hibernate default

    private boolean nullable = true;

    private String defaultValue;

    /**
     * Creates a new column with the given name and SQL type.
     *
     * @param name the column name.
     * @param sqlType the SQL type.
     * @param key the associated field name.
     */
    public Column(String name, int sqlType, String key) {
        if (name == null) {
            throw new IllegalArgumentException("name cannot be null");
        }
        this.sqlType = sqlType;
        this.key = key;
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public String getQuotedName(Dialect dialect) {
        return dialect.openQuote() + name + dialect.closeQuote();
    }

    public int getSqlType() {
        return sqlType;
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

    public int getLength() {
        return length;
    }

    public void setLength(int length) {
        this.length = length;
    }

    public int getPrecision() {
        return precision;
    }

    public void setPrecision(int scale) {
        precision = scale;
    }

    public int getScale() {
        return scale;
    }

    public void setScale(int scale) {
        this.scale = scale;
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

    public String getSqlTypeString(Dialect dialect) {
        return dialect.getTypeName(sqlType, length, precision, scale);
    }

    // XXX this should be handled by specific type classes associated with the
    // column
    public Serializable getFromResultSet(ResultSet rs, int columnIndex)
            throws SQLException {
        // Here we make sure that we get the types we want, we don't let the
        // driver's getObject() return what it fancies.
        Serializable result;
        switch (sqlType) {
        case Types.BIGINT:
            result = rs.getLong(columnIndex);
            break;
        case Types.INTEGER:
            result = rs.getInt(columnIndex);
            break;
        case Types.VARCHAR:
        case Types.CLOB:
            result = rs.getString(columnIndex);
            break;
        case Types.TIMESTAMP:
            result = rs.getTimestamp(columnIndex);
            break;
        default:
            throw new SQLException("Unhandled SQL type " + sqlType);
        }
        if (rs.wasNull()) { // stupid JDBC API
            result = null;
        }
        return result;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + '(' + name + ')';
    }

}
