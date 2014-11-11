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
import java.util.Calendar;
import java.util.Collection;

import org.nuxeo.ecm.core.schema.types.SimpleTypeImpl;
import org.nuxeo.ecm.core.schema.types.primitives.BinaryType;
import org.nuxeo.ecm.core.schema.types.primitives.BooleanType;
import org.nuxeo.ecm.core.schema.types.primitives.DateType;
import org.nuxeo.ecm.core.schema.types.primitives.DoubleType;
import org.nuxeo.ecm.core.schema.types.primitives.IntegerType;
import org.nuxeo.ecm.core.schema.types.primitives.LongType;
import org.nuxeo.ecm.core.schema.types.primitives.StringType;
import org.nuxeo.ecm.core.storage.sql.Binary;

/**
 * Abstract representation of the database-level column types.
 */
public enum ColumnType {

    // ----- schema-based columns -----
    VARCHAR(String.class), // size-limited string (oracle max 2000)
    CLOB(String.class), // large text fields
    BOOLEAN(Boolean.class), //
    LONG(Long.class), //
    DOUBLE(Double.class), //
    TIMESTAMP(Calendar.class), //
    BLOBID(Binary.class), // attached files

    // ----- system columns -----
    NODEID, // node id primary generated key
    NODEIDFK, // fk to main node id, not nullable (frag id)
    NODEIDFKNP, // fk to main node id, not nullable, not primary
    NODEIDFKMUL, // fk to main node id, not nullable, non-unique
    NODEIDFKNULL, // fk to main node id, nullable
    NODEVAL, // same type as node id, not a fk (versionable, cluster...)
    SYSNAME, // system names (type names etc)
    TINYINT, // cluster inval kind
    INTEGER, // complex prop order, ordered doc
    FTINDEXED, // summary ft column being indexed
    FTSTORED, // individual ft column
    CLUSTERNODE, // cluster node id
    CLUSTERFRAGS; // list of fragments impacted, for clustering

    private final Class<?> klass;

    private ColumnType() {
        klass = null;
    }

    private ColumnType(Class<?> klass) {
        this.klass = klass;
    }

    public Serializable[] collectionToArray(Collection<Serializable> collection) {
        // contrary to list.toArray(), this creates an array
        // of the property type instead of an Object[]
        if (klass == null) {
            throw new IllegalStateException(this.toString());
        }
        Serializable[] array = (Serializable[]) java.lang.reflect.Array.newInstance(
                klass, collection.size());
        return collection.toArray(array);
    }

    public static ColumnType fromFieldType(
            org.nuxeo.ecm.core.schema.types.Type coreType) {
        if (coreType instanceof StringType) {
            return VARCHAR; // or CLOB XXX
        } else if (coreType instanceof BooleanType) {
            return BOOLEAN;
        } else if (coreType instanceof LongType) {
            return LONG;
        } else if (coreType instanceof DoubleType) {
            return DOUBLE;
        } else if (coreType instanceof DateType) {
            return TIMESTAMP;
        } else if (coreType instanceof BinaryType) {
            return BLOBID;
        } else if (coreType instanceof IntegerType) {
            return INTEGER;
        } else if (coreType instanceof SimpleTypeImpl) {
            // simple type with constraints -- ignore constraints XXX
            return fromFieldType(coreType.getSuperType());
        } else {
            throw new RuntimeException("Invalid primitive type: "
                    + coreType.getClass().getName());
        }
    }

}
