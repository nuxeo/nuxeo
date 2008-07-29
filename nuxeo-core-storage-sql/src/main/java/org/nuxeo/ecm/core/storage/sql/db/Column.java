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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.dialect.Dialect;

/**
 * An SQL {@code column}.
 *
 * @author Florent Guillaume
 */
public class Column implements Serializable {

    private static final Log log = LogFactory.getLog(Column.class);

    private static final long serialVersionUID = 1L;

    private String name;

    /** The {@link java.sql.Types} type */
    private int sqlType;

    private String key;

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
        this.name = name;
        this.sqlType = sqlType;
        this.key = key;
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

    public String getSqlTypeString(Dialect dialect) {
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
        case Types.INTEGER:
            result = rs.getLong(index);
            break;
        case Types.VARCHAR:
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
        case Types.BLOB:
            log.error("BLOB fetch unimplemented, returning null");
            result = null; // XXX TODO
            break;
        default:
            throw new SQLException("Unhandled SQL type: " + sqlType);
        }
        if (rs.wasNull()) {
            result = null;
        }
        return result;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + '(' + name + ')';
    }

}
