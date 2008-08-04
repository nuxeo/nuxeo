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

package org.nuxeo.ecm.core.storage.sql;

import java.io.Serializable;
import java.lang.reflect.Array;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;

import org.nuxeo.ecm.core.schema.types.SimpleTypeImpl;
import org.nuxeo.ecm.core.schema.types.Type;
import org.nuxeo.ecm.core.schema.types.primitives.BinaryType;
import org.nuxeo.ecm.core.schema.types.primitives.BooleanType;
import org.nuxeo.ecm.core.schema.types.primitives.DateType;
import org.nuxeo.ecm.core.schema.types.primitives.DoubleType;
import org.nuxeo.ecm.core.schema.types.primitives.IntegerType;
import org.nuxeo.ecm.core.schema.types.primitives.LongType;
import org.nuxeo.ecm.core.schema.types.primitives.StringType;

/**
 * @author Florent Guillaume
 */
public enum PropertyType {
    STRING(String.class), //
    BOOLEAN(Boolean.class), //
    LONG(Long.class), //
    DOUBLE(Double.class), //
    DATETIME(Calendar.class), //
    BINARY(Binary.class), //
    ACL(ACLRow.class), //
    ARRAY_STRING(STRING, new String[0]), //
    ARRAY_BOOLEAN(BOOLEAN, new Boolean[0]), //
    ARRAY_LONG(LONG, new Long[0]), //
    ARRAY_DOUBLE(DOUBLE, new Double[0]), //
    ARRAY_DATETIME(DATETIME, new Calendar[0]), //
    ARRAY_BINARY(BINARY, new Binary[0]), //
    COLL_ACL(ACL, new ACLRow[0]);

    private final Class<?> klass;

    private final PropertyType arrayBaseType;

    private final Serializable[] emptyArray;

    private PropertyType(Class<?> klass) {
        this.klass = klass;
        arrayBaseType = null;
        emptyArray = null;
    }

    private PropertyType(PropertyType arrayBaseType, Serializable[] emptyArray) {
        klass = null;
        this.arrayBaseType = arrayBaseType;
        this.emptyArray = emptyArray;
    }

    public boolean isArray() {
        return arrayBaseType != null;
    }

    public PropertyType getArrayBaseType() {
        return arrayBaseType;
    }

    public Serializable[] getEmptyArray() {
        return emptyArray;
    }

    public Serializable[] listToArray(List<Serializable> list) {
        // contrary to list.toArray(), this creates an array
        // of the property type instead of an Object[]
        try {
            Serializable[] array = (Serializable[]) Array.newInstance(klass,
                    list.size());
            return list.toArray(array);
        } catch (NullPointerException e) {
            throw e;
        }
    }

    /**
     * Normalizes a scalar value of this type.
     *
     * @param value the value to normalize
     * @return the normalized value
     * @throws IllegalArgumentException
     */
    public Serializable normalize(Serializable value)
            throws IllegalArgumentException {
        if (value == null) {
            return null;
        }
        switch (this) {
        case STRING:
            if (value instanceof String) {
                return value;
            }
            throw new IllegalArgumentException("Value is not a String: " +
                    value);
        case BOOLEAN:
            if (value instanceof Boolean) {
                return value;
            }
            throw new IllegalArgumentException("Value is not a Boolean: " +
                    value);
        case LONG:
            if (value instanceof Long) {
                return value;
            }
            throw new IllegalArgumentException("Value is not a Long: " + value);
        case DOUBLE:
            if (value instanceof Double) {
                return value;
            }
            throw new IllegalArgumentException("Value is not a Double: " +
                    value);
        case DATETIME:
            if (value instanceof Calendar) {
                return value;
            }
            if (value instanceof Date) {
                Calendar cal = new GregorianCalendar(); // XXX timezone
                cal.setTime((Date) value);
                return cal;
            }
            throw new IllegalArgumentException("Value is not a Calendar: " +
                    value);
        case BINARY:
            if (value instanceof Binary) {
                return value;
            }
            throw new IllegalArgumentException("Value is not a Binary: " +
                    value);
        case ACL:
            if (value instanceof ACLRow) {
                return value;
            }
            throw new IllegalArgumentException("Value is not a ACLRow: " +
                    value);
        default:
            throw new AssertionError(this);
        }
    }

    /**
     * Normalizes an array value of this type.
     * <p>
     * A {@code null} value will be normalized to an empty array.
     *
     * @param value the array to normalize
     * @return the normalized array
     * @throws IllegalArgumentException
     */
    public Serializable[] normalize(Serializable[] value)
            throws IllegalArgumentException {
        if (value == null) {
            return emptyArray;
        }
        for (int i = 0; i < value.length; i++) {
            value[i] = arrayBaseType.normalize(value[i]);
        }
        return value;
    }

    /**
     * Converts a Nuxeo core schema field type into a property type.
     *
     * @param fieldType the field type to convert
     * @param array {@code true} if an array type is required
     * @return
     */
    public static PropertyType fromFieldType(Type fieldType, boolean array) {
        if (fieldType instanceof StringType) {
            return array ? ARRAY_STRING : STRING;
        } else if (fieldType instanceof BooleanType) {
            return array ? ARRAY_BOOLEAN : BOOLEAN;
        } else if (fieldType instanceof LongType) {
            return array ? ARRAY_LONG : LONG;
        } else if (fieldType instanceof DoubleType) {
            return array ? ARRAY_DOUBLE : DOUBLE;
        } else if (fieldType instanceof DateType) {
            return array ? ARRAY_DATETIME : DATETIME;
        } else if (fieldType instanceof BinaryType) {
            return array ? ARRAY_BINARY : BINARY;
        } else if (fieldType instanceof IntegerType) {
            throw new RuntimeException("Unimplemented primitive type: " +
                    fieldType.getClass().getName());
        } else if (fieldType instanceof SimpleTypeImpl) {
            // simple type with constraints -- ignore constraints XXX
            return fromFieldType(fieldType.getSuperType(), array);
        } else {
            throw new RuntimeException("Invalid primitive type: " +
                    fieldType.getClass().getName());
        }
    }

}
