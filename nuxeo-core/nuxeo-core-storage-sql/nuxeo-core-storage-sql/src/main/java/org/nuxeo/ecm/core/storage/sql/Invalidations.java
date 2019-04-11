/*
 * (C) Copyright 2006-2017 Nuxeo (http://nuxeo.com/) and others.
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

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

import org.nuxeo.runtime.pubsub.SerializableAccumulableInvalidations;

/**
 * A set of invalidations.
 * <p>
 * Records both modified and deleted fragments, as well as "parents modified" fragments.
 */
public class Invalidations implements SerializableAccumulableInvalidations {

    private static final long serialVersionUID = 1L;

    /** Pseudo-table for children invalidation. */
    public static final String PARENT = "__PARENT__";

    /** Pseudo-table for series proxies invalidation. */
    public static final String SERIES_PROXIES = "__SERIES_PROXIES__";

    /** Pseudo-table for target proxies invalidation. */
    public static final String TARGET_PROXIES = "__TARGET_PROXIES__";

    public static final int MODIFIED = 1;

    public static final int DELETED = 2;

    /**
     * Maximum number of invalidations kept, after which only {@link #all} is set. This avoids accumulating too many
     * invalidations in memory, at the expense of more coarse-grained invalidations.
     */
    public static final int MAX_SIZE = 10000;

    /**
     * Used locally when invalidating everything, or when too many invalidations have been received.
     */
    public boolean all;

    /** null when empty */
    public Set<RowId> modified;

    /** null when empty */
    public Set<RowId> deleted;

    public Invalidations() {
    }

    public Invalidations(boolean all) {
        this.all = all;
    }

    @Override
    public boolean isEmpty() {
        return modified == null && deleted == null && !all;
    }

    public void clear() {
        all = false;
        modified = null;
        deleted = null;
    }

    protected void setAll() {
        all = true;
        modified = null;
        deleted = null;
    }

    protected void checkMaxSize() {
        if (modified != null && modified.size() > MAX_SIZE //
                || deleted != null && deleted.size() > MAX_SIZE) {
            setAll();
        }
    }

    /** only call this if it's to add at least one element in the set */
    public Set<RowId> getKindSet(int kind) {
        switch (kind) {
        case MODIFIED:
            if (modified == null) {
                modified = new HashSet<>();
            }
            return modified;
        case DELETED:
            if (deleted == null) {
                deleted = new HashSet<>();
            }
            return deleted;
        }
        throw new AssertionError();
    }

    @Override
    public void add(SerializableAccumulableInvalidations o) {
        Invalidations other = (Invalidations) o;
        if (other == null) {
            return;
        }
        if (all) {
            return;
        }
        if (other.all) {
            setAll();
            return;
        }
        if (other.modified != null) {
            if (modified == null) {
                modified = new HashSet<>();
            }
            modified.addAll(other.modified);
        }
        if (other.deleted != null) {
            if (deleted == null) {
                deleted = new HashSet<>();
            }
            deleted.addAll(other.deleted);
        }
        checkMaxSize();
    }

    public void addModified(RowId rowId) {
        if (all) {
            return;
        }
        if (modified == null) {
            modified = new HashSet<>();
        }
        modified.add(rowId);
        checkMaxSize();
    }

    public void addDeleted(RowId rowId) {
        if (all) {
            return;
        }
        if (deleted == null) {
            deleted = new HashSet<>();
        }
        deleted.add(rowId);
        checkMaxSize();
    }

    public void add(Serializable id, String[] tableNames, int kind) {
        if (tableNames.length == 0) {
            return;
        }
        Set<RowId> set = getKindSet(kind);
        for (String tableName : tableNames) {
            set.add(new RowId(tableName, id));
        }
        checkMaxSize();
    }

    // TODO do a more fine-grained serialization than using ObjectOutputStream

    @Override
    public void serialize(OutputStream out) throws IOException {
        try (ObjectOutputStream oout = new ObjectOutputStream(out)) {
            oout.writeObject(this);
        }
    }

    public static Invalidations deserialize(InputStream in) throws IOException {
        try (ObjectInputStream oin = new ObjectInputStream(in)) {
            return (Invalidations) oin.readObject();
        } catch (ClassNotFoundException | ClassCastException e) {
            throw new IOException(e);
        }
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder(this.getClass().getSimpleName() + '(');
        if (all) {
            sb.append("all=true");
        }
        if (modified != null) {
            sb.append("modified=");
            sb.append(modified);
            if (deleted != null) {
                sb.append(',');
            }
        }
        if (deleted != null) {
            sb.append("deleted=");
            sb.append(deleted);
        }
        sb.append(')');
        return sb.toString();
    }

}
