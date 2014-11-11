/*
 * (C) Copyright 2009 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 */

package org.nuxeo.ecm.platform.tag.sql;

import java.io.Serializable;

import org.hibernate.dialect.Dialect;

/**
 * An SQL column.
 *
 * @author mcedica
 */
public class Column implements Serializable {

    private static final long serialVersionUID = 1L;

    private final String name;

    private final int sqlType;

    private boolean primary;

    private boolean nullable = true;

    private String defaultValue;

    private int length = 255; // Hibernate default

    private int precision = 19; // Hibernate default

    private int scale = 2; // Hibernate default

    /**
     * Creates a new column with the given name and SQL type.
     *
     * @param name the column name.
     * @param sqlType the SQL type.
     */
    public Column(String name, int sqlType) {
        if (name == null) {
            throw new IllegalArgumentException("name cannot be null");
        }
        this.sqlType = sqlType;
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

    public String getSqlTypeString(Dialect dialect) {
        return dialect.getTypeName(sqlType, length, precision, scale);
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + '(' + name + ')';
    }

}
