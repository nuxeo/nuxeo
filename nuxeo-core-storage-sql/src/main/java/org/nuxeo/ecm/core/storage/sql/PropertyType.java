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
    STRING, //
    LONG, //
    BOOLEAN, //
    DATETIME, //
    BINARY, //
    ARRAY_STRING(STRING), //
    ARRAY_LONG(LONG), //
    ARRAY_BOOLEAN(BOOLEAN), //
    ARRAY_DATETIME(DATETIME), //
    ARRAY_BINARY(BINARY);

    private final PropertyType arrayBaseType;

    private PropertyType() {
        arrayBaseType = null;
    }

    private PropertyType(PropertyType arrayBaseType) {
        this.arrayBaseType = arrayBaseType;
    }

    public boolean isArray() {
        return arrayBaseType != null;
    }

    public PropertyType getArrayBaseType() {
        return arrayBaseType;
    }

    public static PropertyType fromFieldType(Type fieldType, boolean array) {
        if (fieldType instanceof LongType) {
            return array ? ARRAY_LONG : LONG;
        } else if (fieldType instanceof StringType) {
            return array ? ARRAY_STRING : STRING;
        } else if (fieldType instanceof DateType) {
            return array ? ARRAY_DATETIME : DATETIME;
        } else if (fieldType instanceof BinaryType) {
            return array ? ARRAY_BINARY : BINARY;
        } else if (fieldType instanceof BooleanType) {
            return array ? ARRAY_BOOLEAN : BOOLEAN;
        } else if (fieldType instanceof DoubleType) {
            throw new RuntimeException("Unimplemented primitive type: " +
                    fieldType.getClass().getName());
        } else if (fieldType instanceof IntegerType) {
            throw new RuntimeException("Unimplemented primitive type: " +
                    fieldType.getClass().getName());
        } else {
            throw new RuntimeException("Invalid primitive type: " +
                    fieldType.getClass().getName());
        }
    }
};
