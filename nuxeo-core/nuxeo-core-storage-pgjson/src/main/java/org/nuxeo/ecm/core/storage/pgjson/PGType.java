/*
 * (C) Copyright 2019 Nuxeo (http://nuxeo.com/) and others.
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
package org.nuxeo.ecm.core.storage.pgjson;

import java.io.Serializable;
import java.lang.reflect.Array;
import java.sql.JDBCType;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.Calendar;

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
 * The database-level column types.
 *
 * @since 11.1
 */
public enum PGType {

    TYPE_STRING("varchar", Types.VARCHAR),

    TYPE_STRING_ARRAY("varchar[]", Types.ARRAY, TYPE_STRING, String.class),

    TYPE_LONG("int8", Types.BIGINT),

    TYPE_LONG_ARRAY("int8[]", Types.ARRAY, TYPE_LONG, Long.class),

    TYPE_DOUBLE("float8", Types.DOUBLE),

    TYPE_DOUBLE_ARRAY("float8[]", Types.ARRAY, TYPE_DOUBLE, Double.class),

    TYPE_TIMESTAMP("int8", Types.BIGINT), // JSON compat

    TYPE_TIMESTAMP_ARRAY("int8[]", Types.ARRAY, TYPE_TIMESTAMP, Long.class),

    TYPE_BOOLEAN("bool", Types.BIT),

    TYPE_BOOLEAN_ARRAY("bool", Types.ARRAY, TYPE_BOOLEAN, Boolean.class),

    TYPE_JSON("jsonb", Types.OTHER);

    /** Database type name. */
    protected final String name;

    /** Type from {@link java.sql.Types} */
    protected final int type;

    /** {@link PGType} of array elements. */
    protected final PGType baseType;

    /** The component type to use when creating a Java array from SQL. */
    protected final Class<?> arrayComponentType;

    /** Creates a simple type. */
    private PGType(String string, int type) {
        this(string, type, null, null);
    }

    /** Creates an array type. */
    private PGType(String string, int type, PGType baseType, Class<?> arrayComponentType) {
        this.name = string;
        this.type = type;
        this.baseType = baseType;
        this.arrayComponentType = arrayComponentType;
    }

    public boolean isArray() {
        return baseType != null;
    }

    @Override
    public String toString() {
        if (baseType == null) {
            return getClass().getSimpleName() + '(' + name + ',' + JDBCType.valueOf(type) + ')';
        } else {
            return getClass().getSimpleName() + '(' + name + ',' + JDBCType.valueOf(type) + ',' + baseType + ')';
        }
    }

    public static PGType from(Type type, boolean array) {
        if (type instanceof StringType) {
            return array ? TYPE_STRING_ARRAY : TYPE_STRING;
        } else if (type instanceof BooleanType) {
            return array ? TYPE_BOOLEAN_ARRAY : TYPE_BOOLEAN;
        } else if (type instanceof LongType) {
            return array ? TYPE_LONG_ARRAY : TYPE_LONG;
        } else if (type instanceof DoubleType) {
            return array ? TYPE_DOUBLE_ARRAY : TYPE_DOUBLE;
        } else if (type instanceof DateType) {
            return array ? TYPE_TIMESTAMP_ARRAY : TYPE_TIMESTAMP;
        } else if (type instanceof IntegerType || type instanceof BinaryType) {
            throw new RuntimeException("Unimplemented primitive type: " + type.getClass().getName());
        } else if (type instanceof SimpleTypeImpl) {
            // simple type with constraints -- ignore constraints XXX
            return from(type.getSuperType(), array);
        } else {
            throw new RuntimeException("Invalid primitive type: " + type.getClass().getName());
        }
    }

    /**
     * Gets the value for this type from a {@link ResultSet}.
     */
    public Serializable getValue(ResultSet rs, int i, PGJSONConverter converter) throws SQLException {
        switch (this) {
        case TYPE_STRING:
            return rs.getString(i);
        case TYPE_LONG:
            long l = rs.getLong(i);
            if (rs.wasNull()) {
                return null;
            }
            return Long.valueOf(l);
        case TYPE_DOUBLE:
            double d = rs.getDouble(i);
            if (rs.wasNull()) {
                return null;
            }
            return Double.valueOf(d);
        case TYPE_TIMESTAMP:
            long millis = rs.getLong(i);
            if (rs.wasNull()) {
                return null;
            }
            Calendar cal = Calendar.getInstance();
            cal.setTimeInMillis(millis);
            return cal;
        case TYPE_BOOLEAN:
            boolean b = rs.getBoolean(i);
            if (rs.wasNull()) {
                return null;
            }
            return Boolean.valueOf(b);
        case TYPE_STRING_ARRAY:
        case TYPE_LONG_ARRAY:
        case TYPE_DOUBLE_ARRAY:
        case TYPE_TIMESTAMP_ARRAY:
        case TYPE_BOOLEAN_ARRAY:
            java.sql.Array array = rs.getArray(i);
            if (rs.wasNull()) {
                return null;
            }
            Object[] objectArray = (Object[]) array.getArray();
            if (objectArray.getClass().getComponentType() == arrayComponentType) {
                return objectArray;
            } else {
                // convert to typed array[]
                Object[] typedArray = (Object[]) Array.newInstance(arrayComponentType, objectArray.length);
                System.arraycopy(objectArray, 0, typedArray, 0, objectArray.length);
                return typedArray;
            }
        case TYPE_JSON:
            String json = rs.getString(i);
            if (rs.wasNull()) {
                return null;
            }
            return converter.jsonToValue(json);
        default:
            throw new UnsupportedOperationException("Unsupported type: " + type);
        }
    }

    public void setValue(PreparedStatement ps, int i, Object value, PGJSONConverter converter) throws SQLException {
        switch (this) {
        case TYPE_STRING:
            ps.setString(i, (String) value);
            break;
        case TYPE_LONG:
            ps.setLong(i, ((Long) value).longValue());
            break;
        case TYPE_DOUBLE:
            ps.setDouble(i, ((Double) value).doubleValue());
            break;
        case TYPE_TIMESTAMP:
            long millis = ((Calendar) value).getTimeInMillis();
            ps.setLong(i, millis);
            break;
        case TYPE_BOOLEAN:
            ps.setBoolean(i, ((Boolean) value).booleanValue());
            break;
        case TYPE_STRING_ARRAY:
        case TYPE_LONG_ARRAY:
        case TYPE_DOUBLE_ARRAY:
        case TYPE_TIMESTAMP_ARRAY:
        case TYPE_BOOLEAN_ARRAY:
            java.sql.Array array = ps.getConnection().createArrayOf(baseType.name, (Object[]) value);
            ps.setArray(i, array);
            break;
        case TYPE_JSON:
            String json = converter.valueToJson(value);
            ps.setString(i, json);
            break;
        default:
            throw new UnsupportedOperationException("Unsupported type: " + type);
        }
    }

    /**
     * The bundling of a type and an associated SQL name.
     */
    public static class PGTypeWithName {

        public final PGType type;

        public final String name;

        public PGTypeWithName(PGType type, String name) {
            this.type = type;
            this.name = name;
        }

        @Override
        public String toString() {
            return getClass().getSimpleName() + '(' + name + " (" + type+ "))";
        }
    }

    /**
     * The bundling of a value and its type.
     */
    public static class PGTypeAndValue {

        public final PGType type;

        public final Object value;

        public PGTypeAndValue(PGType type, Object value) {
            this.type = type;
            this.value = value;
        }

        @Override
        public String toString() {
            return getClass().getSimpleName() + '(' + type + ',' + value + ')';
        }
    }

}
