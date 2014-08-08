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
import java.util.HashSet;
import java.util.Set;

/**
 * A set of invalidations.
 * <p>
 * Records both modified and deleted fragments, as well as "parents modified"
 * fragments.
 */
public class Invalidations implements Serializable {

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
     * Maximum number of invalidations kept, after which only {@link #all} is
     * set. This avoids accumulating too many invalidations in memory, at the
     * expense of more coarse-grained invalidations.
     */
    public static final int MAX_SIZE = 10000;

    /**
     * Used locally when invalidating everything, or when too many invalidations
     * have been received.
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
                modified = new HashSet<RowId>();
            }
            return modified;
        case DELETED:
            if (deleted == null) {
                deleted = new HashSet<RowId>();
            }
            return deleted;
        }
        throw new AssertionError();
    }

    public void add(Invalidations other) {
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
                modified = new HashSet<RowId>();
            }
            modified.addAll(other.modified);
        }
        if (other.deleted != null) {
            if (deleted == null) {
                deleted = new HashSet<RowId>();
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
            modified = new HashSet<RowId>();
        }
        modified.add(rowId);
        checkMaxSize();
    }

    public void addDeleted(RowId rowId) {
        if (all) {
            return;
        }
        if (deleted == null) {
            deleted = new HashSet<RowId>();
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

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder(
                this.getClass().getSimpleName() + '(');
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

    public static final class InvalidationsPair implements Serializable {

        private static final long serialVersionUID = 1L;

        public final Invalidations cacheInvalidations;

        public final Invalidations eventInvalidations;

        public InvalidationsPair(Invalidations cacheInvalidations,
                Invalidations eventInvalidations) {
            this.cacheInvalidations = cacheInvalidations;
            this.eventInvalidations = eventInvalidations;
        }
    }

}
