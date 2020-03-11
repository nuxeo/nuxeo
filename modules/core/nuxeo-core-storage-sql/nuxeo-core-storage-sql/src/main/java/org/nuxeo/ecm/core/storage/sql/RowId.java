/*
 * (C) Copyright 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
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
package org.nuxeo.ecm.core.storage.sql;

import java.io.Serializable;

/**
 * The identification of a {@link Row} (table name and id) without the row content itself.
 * <p>
 * This class is sometimes used as a marker for an "absent" row in the database, when mixed with actual {@link Row}s.
 */
public class RowId implements Serializable, Comparable<RowId> {

    private static final long serialVersionUID = 1L;

    public final String tableName;

    public Serializable id;

    public RowId(RowId rowId) {
        tableName = rowId.tableName;
        id = rowId.id;
    }

    public RowId(String tableName, Serializable id) {
        this.tableName = tableName == null ? null : tableName.intern();
        this.id = id;
    }

    @Override
    public int hashCode() {
        int result = 31 + (id == null ? 0 : id.hashCode());
        return 31 * result + tableName.hashCode();
    }

    @Override
    public boolean equals(Object other) {
        if (other instanceof RowId) {
            return equals((RowId) other);
        }
        return false;
    }

    private boolean equals(RowId other) {
        if (other == this) {
            return true;
        }
        if (id == null) {
            if (other.id != null) {
                return false;
            }
        } else if (!id.equals(other.id)) {
            return false;
        }
        return tableName.equals(other.tableName);
    }

    @Override
    public int compareTo(RowId other) {
        int cmp = tableName.compareTo(other.tableName);
        if (cmp != 0) {
            return cmp;
        }
        if (id instanceof String && other.id instanceof String) {
            return ((String) id).compareTo((String) other.id);
        } else if (id instanceof Long && other.id instanceof Long) {
            return ((Long) id).compareTo((Long) other.id);
        } else {
            throw new UnsupportedOperationException("id=" + id + " other.id=" + other.id);
        }
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + '(' + tableName + ", " + id + ')';
    }

}
