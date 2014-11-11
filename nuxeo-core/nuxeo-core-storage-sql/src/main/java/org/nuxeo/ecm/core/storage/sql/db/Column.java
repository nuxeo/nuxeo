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
 *
 * $Id: Column.java 18286 2007-2008-05-06 02:18:58Z fguillaume $
 */

package org.nuxeo.ecm.core.storage.sql.db;

import java.io.Serializable;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;

import org.nuxeo.ecm.core.storage.sql.Binary;
import org.nuxeo.ecm.core.storage.sql.Model;
import org.nuxeo.ecm.core.storage.sql.PropertyType;

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

    /** The backend type */
    private final PropertyType type;

    /** The {@link java.sql.Types} type */
    private final int sqlType;

    private final String key;

    private final Model model;

    private boolean identity;

    private boolean primary;

    private int length = 255; // Hibernate default

    private int precision = 19; // Hibernate default

    private int scale = 2; // Hibernate default

    private boolean nullable = true;

    private String defaultValue;

    /** For foreign key reference. */
    private Table foreignTable;

    private String foreignKey;

    /**
     * Creates a new column with the given name and type, with a specified SQL
     * type.
     *
     * @param table the column's table
     * @param physicalName the column name
     * @param type the backend type
     * @param sqlType the SQL type
     * @param key the associated field name
     * @param model the model (to fetch binaries)
     */
    public Column(Table table, String physicalName, PropertyType type,
            int sqlType, String key, Model model) {
        this.table = table;
        this.dialect = table.dialect;
        this.physicalName = physicalName;
        this.type = type;
        this.sqlType = sqlType;
        this.key = key;
        this.model = model;
    }

    public String getPhysicalName() {
        return physicalName;
    }

    public String getQuotedName() {
        return dialect.openQuote() + physicalName + dialect.closeQuote();
    }

    public String getFullQuotedName() {
        return table.getQuotedName() + '.' + getQuotedName();
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
        this.precision = scale;
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
        return dialect.getTypeName(sqlType, getLength(), getPrecision(),
                getScale());
    }

    public void setToPreparedStatement(PreparedStatement ps, int index,
            Serializable value) throws SQLException {
        if (value == null) {
            ps.setNull(index, sqlType);
            return;
        }
        switch (sqlType) {
        case Types.BIGINT:
            ps.setLong(index, ((Long) value).longValue());
            return;
        case Types.INTEGER:
            ps.setInt(index, ((Long) value).intValue());
            return;
        case Types.VARCHAR:
            String v;
            if (type == PropertyType.BINARY) {
                v = ((Binary) value).getDigest();
            } else {
                v = (String) value;
            }
            ps.setString(index, v);
            break;
        case Types.CLOB:
            ps.setString(index, (String) value);
            return;
        case Types.BIT:
            ps.setBoolean(index, ((Boolean) value).booleanValue());
            return;
        case Types.TIMESTAMP:
            Calendar cal = (Calendar) value;
            Timestamp ts = new Timestamp(cal.getTimeInMillis());
            ps.setTimestamp(index, ts, cal); // cal passed for timezone
            return;
        default:
            throw new SQLException("Unhandled SQL type: " + sqlType);
        }
    }

    // XXX this should be handled by specific type classes associated with the
    // column
    @SuppressWarnings("boxing")
    public Serializable getFromResultSet(ResultSet rs, int index)
            throws SQLException {
        // Here we make sure that we get the types we want, we don't let the
        // driver's getObject() return what it fancies.
        Serializable result;
        switch (sqlType) {
        case Types.BIGINT:
            result = rs.getLong(index);
            break;
        case Types.INTEGER:
            result = rs.getLong(index);
            break;
        case Types.VARCHAR:
            String string = rs.getString(index);
            if (type == PropertyType.BINARY && string != null) {
                result = model.getBinary(string);
            } else {
                result = string;
            }
            break;
        case Types.CLOB:
            result = rs.getString(index);
            break;
        case Types.TIMESTAMP:
            Timestamp ts = rs.getTimestamp(index);
            if (ts == null) {
                result = null;
            } else {
                result = new GregorianCalendar(); // XXX timezone
                ((Calendar) result).setTimeInMillis(ts.getTime());
            }
            break;
        case Types.BIT:
            result = rs.getBoolean(index);
            break;
        default:
            throw new SQLException("Unhandled SQL type: " + sqlType);
        }
        if (rs.wasNull()) {
            result = null;
        }
        return result;
    }

    public Serializable[] listToArray(List<Serializable> list) {
        return type.collectionToArray(list);
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + '(' + physicalName + ')';
    }

}
