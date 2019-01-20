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

import java.sql.JDBCType;
import java.sql.Types;

/**
 * The database-level column types.
 *
 * @since 11.1
 */
public class PGType {

    public static final PGType TYPE_STRING = new PGType("varchar", Types.VARCHAR);

    public static final PGType TYPE_STRING_ARRAY = new PGType("varchar[]", Types.ARRAY, TYPE_STRING);

    public static final PGType TYPE_LONG = new PGType("int8", Types.BIGINT);

    public static final PGType TYPE_LONG_ARRAY = new PGType("int8[]", Types.ARRAY, TYPE_LONG);

    public static final PGType TYPE_DOUBLE = new PGType("float8", Types.DOUBLE);

    public static final PGType TYPE_TIMESTAMP = new PGType("int8", Types.BIGINT); // JSON compat

    public static final PGType TYPE_BOOLEAN = new PGType("bool", Types.BIT);

    public static final PGType TYPE_JSON = new PGType("jsonb", Types.OTHER);

    /** Database type name. */
    protected final String name;

    /** Type from {@link java.sql.Types} */
    protected final int type;

    /** {@link PGType} of array elements. */
    protected final PGType baseType;

    /** Creates a simple type. */
    public PGType(String string, int type) {
        this(string, type, null);
    }

    /** Creates an array type. */
    public PGType(String string, int type, PGType baseType) {
        this.name = string;
        this.type = type;
        this.baseType = baseType;
    }

    public boolean isArray() {
        return type == Types.ARRAY;
    }

    @Override
    public String toString() {
        if (baseType == null) {
            return getClass().getSimpleName() + '(' + name + ',' + JDBCType.valueOf(type) + ')';
        } else {
            return getClass().getSimpleName() + '(' + name + ',' + JDBCType.valueOf(type) + ',' + baseType + ')';
        }
    }

}
