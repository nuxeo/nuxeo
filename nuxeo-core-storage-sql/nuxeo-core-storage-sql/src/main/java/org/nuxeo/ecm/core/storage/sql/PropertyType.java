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

package org.nuxeo.ecm.core.storage.sql;

import java.io.Serializable;
import java.lang.reflect.Array;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.GregorianCalendar;

import org.nuxeo.ecm.core.api.model.DeltaLong;
import org.nuxeo.ecm.core.schema.types.SimpleTypeImpl;
import org.nuxeo.ecm.core.schema.types.Type;
import org.nuxeo.ecm.core.schema.types.primitives.BinaryType;
import org.nuxeo.ecm.core.schema.types.primitives.BooleanType;
import org.nuxeo.ecm.core.schema.types.primitives.DateType;
import org.nuxeo.ecm.core.schema.types.primitives.DoubleType;
import org.nuxeo.ecm.core.schema.types.primitives.IntegerType;
import org.nuxeo.ecm.core.schema.types.primitives.LongType;
import org.nuxeo.ecm.core.schema.types.primitives.StringType;
import org.nuxeo.ecm.core.storage.binary.Binary;

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

    PropertyType(Class<?> klass) {
        this.klass = klass;
        arrayBaseType = null;
        emptyArray = null;
    }

    PropertyType(PropertyType arrayBaseType, Serializable[] emptyArray) {
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

    public Serializable[] collectionToArray(Collection<Serializable> collection) {
        // contrary to list.toArray(), this creates an array
        // of the property type instead of an Object[]
        Serializable[] array = (Serializable[]) Array.newInstance(klass,
                collection.size());
        return collection.toArray(array);
    }

    /**
     * Normalizes a scalar value of this type.
     *
     * @param value the value to normalize
     * @return the normalized value
     * @throws IllegalArgumentException
     */
    public Serializable normalize(Object value) {
        if (value == null) {
            return null;
        }
        switch (this) {
        case STRING:
            if (value instanceof String) {
                return (String) value;
            }
            throw new IllegalArgumentException("value is not a String: "
                    + value);
        case BOOLEAN:
            if (value instanceof Boolean) {
                return (Boolean) value;
            }
            throw new IllegalArgumentException("value is not a Boolean: "
                    + value);
        case LONG:
            if (value instanceof Long) {
                return (Long) value;
            }
            if (value instanceof DeltaLong) {
                return (DeltaLong) value;
            }
            throw new IllegalArgumentException("value is not a Long: " + value);
        case DOUBLE:
            if (value instanceof Double) {
                return (Double) value;
            }
            throw new IllegalArgumentException("value is not a Double: "
                    + value);
        case DATETIME:
            if (value instanceof Calendar) {
                return (Calendar) value;
            }
            if (value instanceof Date) {
                Calendar cal = new GregorianCalendar(); // XXX timezone
                cal.setTime((Date) value);
                return cal;
            }
            throw new IllegalArgumentException("value is not a Calendar: "
                    + value);
        case BINARY:
            if (value instanceof Binary) {
                return (Binary) value;
            }
            throw new IllegalArgumentException("value is not a Binary: "
                    + value);
        case ACL:
            if (value instanceof ACLRow) {
                return (ACLRow) value;
            }
            throw new IllegalArgumentException("value is not a ACLRow: "
                    + value);
        default:
            throw new RuntimeException(this.toString());
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
    public Serializable[] normalize(Object[] value) {
        if (value == null) {
            return emptyArray;
        }
        Serializable[] newValue;
        if (value instanceof Serializable[]) {
            // update in place
            newValue = (Serializable[]) value;
        } else {
            newValue = new Serializable[value.length];
        }
        for (int i = 0; i < value.length; i++) {
            newValue[i] = arrayBaseType.normalize(value[i]);
        }
        return newValue;
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
            throw new RuntimeException("Unimplemented primitive type: "
                    + fieldType.getClass().getName());
        } else if (fieldType instanceof SimpleTypeImpl) {
            // simple type with constraints -- ignore constraints XXX
            return fromFieldType(fieldType.getSuperType(), array);
        } else {
            throw new RuntimeException("Invalid primitive type: "
                    + fieldType.getClass().getName());
        }
    }

}
