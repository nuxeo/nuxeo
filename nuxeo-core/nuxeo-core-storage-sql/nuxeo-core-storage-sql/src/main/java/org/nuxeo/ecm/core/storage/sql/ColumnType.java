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

import org.nuxeo.ecm.core.schema.types.Field;
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
 * The database-level column types, including per-type parameters like length.
 */
public class ColumnType implements Serializable {

    private static final long serialVersionUID = 1L;

    /** Length used internally to flag a string to use CLOB. */
    public static final int CLOB_LENGTH = 999999999;

    public static final ColumnType STRING = new ColumnType(ColumnSpec.STRING);

    public static final ColumnType CLOB = new ColumnType(ColumnSpec.STRING,
            CLOB_LENGTH);

    public static final ColumnType BOOLEAN = new ColumnType(ColumnSpec.BOOLEAN);

    public static final ColumnType LONG = new ColumnType(ColumnSpec.LONG);

    public static final ColumnType DOUBLE = new ColumnType(ColumnSpec.DOUBLE);

    public static final ColumnType TIMESTAMP = new ColumnType(
            ColumnSpec.TIMESTAMP);

    public static final ColumnType BLOBID = new ColumnType(ColumnSpec.BLOBID);

    public static final ColumnType ARRAY_STRING = new ColumnType(
            ColumnSpec.ARRAY_STRING, -1, true);

    public static final ColumnType ARRAY_CLOB = new ColumnType(
            ColumnSpec.ARRAY_STRING, CLOB_LENGTH, true);

    public static final ColumnType ARRAY_BOOLEAN = new ColumnType(
            ColumnSpec.ARRAY_BOOLEAN, -1, true);

    public static final ColumnType ARRAY_LONG = new ColumnType(
            ColumnSpec.ARRAY_LONG, -1, true);

    public static final ColumnType ARRAY_DOUBLE = new ColumnType(
            ColumnSpec.ARRAY_DOUBLE, -1, true);

    public static final ColumnType ARRAY_TIMESTAMP = new ColumnType(
            ColumnSpec.ARRAY_TIMESTAMP, -1, true);

    public static final ColumnType ARRAY_BLOBID = new ColumnType(
            ColumnSpec.ARRAY_BLOBID, -1, true);

    public static final ColumnType ARRAY_INTEGER = new ColumnType(
            ColumnSpec.ARRAY_INTEGER, -1, true);

    public static final ColumnType NODEID = new ColumnType(ColumnSpec.NODEID);

    public static final ColumnType NODEIDFK = new ColumnType(
            ColumnSpec.NODEIDFK);

    public static final ColumnType NODEIDFKNP = new ColumnType(
            ColumnSpec.NODEIDFKNP);

    public static final ColumnType NODEIDFKMUL = new ColumnType(
            ColumnSpec.NODEIDFKMUL);

    public static final ColumnType NODEIDFKNULL = new ColumnType(
            ColumnSpec.NODEIDFKNULL);

    public static final ColumnType NODEIDPK = new ColumnType(
            ColumnSpec.NODEIDPK);

    public static final ColumnType NODEVAL = new ColumnType(ColumnSpec.NODEVAL);

    public static final ColumnType NODEARRAY = new ColumnType(
            ColumnSpec.NODEARRAY, -1, true);

    public static final ColumnType SYSNAME = new ColumnType(ColumnSpec.SYSNAME);

    public static final ColumnType SYSNAMEARRAY = new ColumnType(
            ColumnSpec.SYSNAMEARRAY, -1, true);

    public static final ColumnType TINYINT = new ColumnType(ColumnSpec.TINYINT);

    public static final ColumnType INTEGER = new ColumnType(ColumnSpec.INTEGER);

    public static final ColumnType AUTOINC = new ColumnType(ColumnSpec.AUTOINC);

    public static final ColumnType FTINDEXED = new ColumnType(
            ColumnSpec.FTINDEXED);

    public static final ColumnType FTSTORED = new ColumnType(
            ColumnSpec.FTSTORED);

    public static final ColumnType CLUSTERNODE = new ColumnType(
            ColumnSpec.CLUSTERNODE);

    public static final ColumnType CLUSTERFRAGS = new ColumnType(
            ColumnSpec.CLUSTERFRAGS);

    public final ColumnSpec spec;

    public final int length;

    public final boolean array;

    public ColumnType(ColumnSpec spec, int length, boolean array) {
        this.spec = spec;
        this.length = length;
        this.array = array;
    }

    public ColumnType(ColumnSpec spec, int length) {
        this(spec, length, false);
    }

    public ColumnType(ColumnSpec spec) {
        this(spec, -1);
    }

    public boolean isUnconstrained() {
        return length == -1;
    }

    public boolean isClob() {
        return length == CLOB_LENGTH;
    }

    public boolean isArray() {
        return array;
    }

    /**
     * Checks if this column holds a Nuxeo unique id (usually UUID).
     */
    public boolean isId() {
        return spec.isId();
    }

    /**
     * Wraps a string that needs to be mapped to an id column in prepared
     * statements.
     *
     * @since 5.7
     */
    public static class WrappedId implements Serializable {
        private static final long serialVersionUID = 1L;

        public final String string;

        public WrappedId(String string) {
            this.string = string;
        }

        @Override
        public String toString() {
            return string;
        }
    }

    @Override
    public String toString() {
        if (isUnconstrained()) {
            return spec.toString();
        } else if (isClob()) {
            return  isArray() ? "ARRAY_CLOB" : "CLOB";
        } else {
            return spec.toString() + '(' + length + ')';
        }
    }

    /**
     * Gets the column type from a Nuxeo Schema field, including its constrained
     * length if any.
     */
    public static ColumnType fromField(Field field) {
        return fromFieldType(field.getType(), field.getMaxLength());
    }

    /**
     * Gets the column type from a Nuxeo Schema field type (unconstrained).
     */
    public static ColumnType fromFieldType(Type type) {
        return fromFieldType(type, -1);
    }

    /**
     * Gets the column type from a Nuxeo Schema field type (unconstrained)
     * with array {@code true} if an array type is required
     */
    public static ColumnType fromFieldType(Type type, boolean array) {
        return fromFieldType(type, -1, array);
    }

    protected static ColumnType fromFieldType(Type type, int maxLength) {
        return fromFieldType(type, maxLength, false);
    }

    protected static ColumnType fromFieldType(Type type, int maxLength, boolean array) {
        if (type instanceof StringType) {
            if (maxLength == -1) {
                return array ? ARRAY_STRING : STRING; // unconstrained
            } else {
                return new ColumnType(ColumnSpec.STRING, maxLength, array);
            }
        } else if (type instanceof BooleanType) {
            return array ? ARRAY_BOOLEAN : BOOLEAN;
        } else if (type instanceof LongType) {
            return array ? ARRAY_LONG : LONG;
        } else if (type instanceof DoubleType) {
            return array ? ARRAY_DOUBLE : DOUBLE;
        } else if (type instanceof DateType) {
            return array ? ARRAY_TIMESTAMP : TIMESTAMP;
        } else if (type instanceof BinaryType) {
            return array ? ARRAY_BLOBID : BLOBID;
        } else if (type instanceof IntegerType) {
            return array ? ARRAY_INTEGER : INTEGER;
        } else if (type instanceof SimpleTypeImpl) {
            // comes from a constraint
            return fromFieldType(type.getSuperType(), maxLength);
        } else {
            throw new RuntimeException("Invalid primitive type: "
                    + type.getClass().getName());
        }
    }

}
