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

    /** Pseudo-table to use to notify about children invalidated. */
    public static final String PARENT = "__PARENT__";

    public static final int MODIFIED = 1;

    public static final int DELETED = 2;

    /** null when empty */
    public Set<RowId> modified;

    /** null when empty */
    public Set<RowId> deleted;

    public boolean isEmpty() {
        return modified == null && deleted == null;
    }

    public void clear() {
        modified = null;
        deleted = null;
    }

    public boolean contains(RowId rowId) {
        // even if rowId or the maps contain a Row, only RowId has an equals()
        // so things comparisons will be correct
        return (modified != null && modified.contains(rowId))
                || (deleted != null && deleted.contains(rowId));
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
        if (other.modified != null) {
            addModified(other.modified);
        }
        if (other.deleted != null) {
            addDeleted(other.deleted);
        }
    }

    public void addModified(RowId rowId) {
        if (modified == null) {
            modified = new HashSet<RowId>();
        }
        modified.add(rowId);
    }

    public void addModified(Set<RowId> rowIds) {
        if (modified == null) {
            modified = new HashSet<RowId>();
        }
        modified.addAll(rowIds);
    }

    public void addDeleted(RowId rowId) {
        if (deleted == null) {
            deleted = new HashSet<RowId>();
        }
        deleted.add(rowId);
    }

    public void addDeleted(Set<RowId> rowIds) {
        if (deleted == null) {
            deleted = new HashSet<RowId>();
        }
        deleted.addAll(rowIds);
    }

    public void add(Serializable id, String[] tableNames, int kind) {
        if (tableNames.length == 0) {
            return;
        }
        Set<RowId> set = getKindSet(kind);
        for (String tableName : tableNames) {
            set.add(new RowId(tableName, id));
        }
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder(
                this.getClass().getSimpleName() + '(');
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
