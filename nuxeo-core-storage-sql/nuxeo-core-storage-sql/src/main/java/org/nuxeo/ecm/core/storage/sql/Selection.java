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
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.storage.StorageException;

/**
 * A {@link Selection} holds information about row ids corresponding to a fixed
 * clause for a given table.
 * <p>
 * A clause has the form: column = fixed value. The column can be the parent id,
 * the versionable id, the target id.
 * <p>
 * The internal state of a {@link Selection} instance reflects:
 * <ul>
 * <li>corresponding rows known to exist in the database,</li>
 * <li>corresponding created rows not yet flushed to database,</li>
 * <li>corresponding rows not yet flushed to database.</li>
 * </ul>
 * Information about rows in the database may be complete, or just partial if
 * only individual rows corresponding to the clause have been retrieved from the
 * database.
 * <p>
 * Row ids are stored in no particular order.
 * <p>
 * When this structure holds information all flushed to the database, then it
 * can safely be GC'ed, so it lives in a memory-sensitive map (softMap),
 * otherwise it's moved to a normal map (hardMap).
 * <p>
 * This class is not thread-safe and should be used only from a single-threaded
 * session.
 */
public class Selection {

    private static final Log log = LogFactory.getLog(Selection.class);

    /**
     * The selection id, also the key which this instance has in the map holding
     * it.
     * <p>
     * For instance for a children selection this is the parent id.
     */
    private final Serializable selId;

    /**
     * The table name to fetch fragment.
     */
    private final String tableName;

    /**
     * The context used to fetch fragments.
     */
    protected final PersistenceContext context;

    /**
     * The key to use to filter.
     * <p>
     * For instance for a children selection this is the child name.
     */
    protected final String filterKey;

    /** The map where this is stored when GCable. */
    private final Map<Serializable, Selection> softMap;

    /** The map where this is stored when not GCable. */
    private final Map<Serializable, Selection> hardMap;

    /**
     * This is {@code true} when complete information about the existing ids is
     * known.
     * <p>
     * This is the case when a query to the database has been made to fetch all
     * rows with the clause, or when a new value for the clause has been created
     * (applies for instance to a new parent id appearing when a folder is
     * created).
     */
    protected boolean complete;

    /**
     * The row ids known in the database and not deleted. This list is not
     * ordered.
     */
    protected List<Serializable> existing;

    /** The row ids created and not yet flushed to database. */
    protected List<Serializable> created;

    /**
     * The row ids deleted (or for which the clause column changed value) and
     * not yet flushed to database.
     */
    protected Set<Serializable> deleted;

    /**
     * Constructs a {@link Selection} for the given selection id.
     * <p>
     * It is automatically put in the soft map.
     *
     * @param selId the selection key (used in the soft/hard maps)
     * @param tableName the table name to fetch fragments
     * @param empty if the new instance is created empty
     * @param filterKey the key to use to additionally filter on fragment values
     * @param context the context from which to fetch fragments
     * @param softMap the soft map, when the selection is pristine
     * @param hardMap the hard map, when there are modifications to flush
     */
    public Selection(Serializable selId, String tableName, boolean empty,
            String filterKey, PersistenceContext context,
            Map<Serializable, Selection> softMap,
            Map<Serializable, Selection> hardMap) {
        this.selId = selId;
        this.tableName = tableName;
        this.context = context;
        this.filterKey = filterKey;
        this.softMap = softMap;
        this.hardMap = hardMap;
        complete = empty;
        // starts its life in the soft map (no created or deleted)
        softMap.put(selId, this);
    }

    protected Serializable fragmentValue(SimpleFragment fragment) {
        try {
            return fragment.get(filterKey);
        } catch (StorageException e) {
            log.error("Could not fetch value: " + fragment.getId());
            return null;
        }
    }

    /**
     * Adds a known row corresponding to the clause.
     *
     * @param id the fragment id
     */
    public void addExisting(Serializable id) {
        if (existing == null) {
            existing = new LinkedList<Serializable>();
        }
        if (existing.contains(id) || (created != null && created.contains(id))) {
            // the id is already known here, this happens if the fragment was
            // GCed from pristine and we had to refetched it from the mapper
            return;
        }
        existing.add(id);
    }

    /**
     * Adds a created row corresponding to the clause.
     *
     * @param id the fragment id
     */
    public void addCreated(Serializable id) {
        if (created == null) {
            created = new LinkedList<Serializable>();
            // move to hard map
            softMap.remove(selId);
            hardMap.put(selId, this);
        }
        if ((existing != null && existing.contains(id)) || created.contains(id)) {
            // TODO remove sanity check if ok
            log.error("Creating already present id: " + id);
            return;
        }
        created.add(id);
    }

    /**
     * Adds ids actually read from the backend, and mark this complete.
     * <p>
     * Note that when adding a complete list of ids retrieved from the database,
     * the deleted ids have already been removed in the result set.
     *
     * @param actualExisting the existing database ids (the list must be
     *            mutable)
     */
    public void addExistingComplete(List<Serializable> actualExisting) {
        assert !complete;
        complete = true;
        existing = actualExisting;
    }

    /**
     * Marks as incomplete.
     * <p>
     * Called after a database operation added rows corresponding to the clause
     * with unknown ids (restore of complex properties).
     */
    public void setIncomplete() {
        complete = false;
    }

    /**
     * Removes a known child id.
     *
     * @param id the id to remove
     */
    public void remove(Serializable id) {
        if (created != null && created.remove(id)) {
            // don't add to deleted
            return;
        }
        if (existing != null) {
            existing.remove(id);
        }
        if (deleted == null) {
            deleted = new HashSet<Serializable>();
            // move to hard map
            softMap.remove(selId);
            hardMap.put(selId, this);
        }
        deleted.add(id);
    }

    /**
     * Flushes to database. Clears created and deleted map.
     * <p>
     * Puts this in the soft map. Caller must remove from hard map.
     */
    public void flush() {
        if (created != null) {
            if (existing == null) {
                existing = new LinkedList<Serializable>();
            }
            existing.addAll(created);
            created = null;
        }
        deleted = null;
        // move to soft map
        // caller responsible for removing from hard map
        softMap.put(selId, this);
    }

    public boolean isFlushed() {
        return created == null && deleted == null;
    }

    private SimpleFragment getFragmentIfPresent(Serializable id) {
        RowId rowId = new RowId(tableName, id);
        return (SimpleFragment) context.getIfPresent(rowId);
    }

    private SimpleFragment getFragment(Serializable id) throws StorageException {
        RowId rowId = new RowId(tableName, id);
        return (SimpleFragment) context.get(rowId, false);
    }

    /**
     * Gets a fragment given its filtered value.
     * <p>
     * Returns {@code null} if there is no such fragment.
     * <p>
     * Returns {@link SimpleFragment#UNKNOWN} if there's no info about it.
     *
     * @param filter the value to filter on (cannot be {@code null})
     * @return the fragment, or {@code null}, or {@link SimpleFragment#UNKNOWN}
     */
    public SimpleFragment getFragmentByValue(Serializable filter) {
        if (existing != null) {
            for (Serializable id : existing) {
                SimpleFragment fragment;
                try {
                    fragment = getFragment(id);
                } catch (StorageException e) {
                    log.warn("Failed refetch for: " + id, e);
                    continue;
                }
                if (fragment == null) {
                    log.warn("Existing fragment missing: " + id);
                    continue;
                }
                if (filter.equals(fragmentValue(fragment))) {
                    return fragment;
                }
            }
        }
        if (created != null) {
            for (Serializable id : created) {
                SimpleFragment fragment = getFragmentIfPresent(id);
                if (fragment == null) {
                    log.warn("Created fragment missing: " + id);
                    continue;
                }
                if (filter.equals(fragmentValue(fragment))) {
                    return fragment;
                }
            }
        }
        if (deleted != null) {
            for (Serializable id : deleted) {
                SimpleFragment fragment = getFragmentIfPresent(id);
                if (fragment == null) {
                    log.warn("Deleted fragment missing: " + id);
                    continue;
                }
                if (filter.equals(fragmentValue(fragment))) {
                    return null;
                }
            }
        }
        return complete ? null : SimpleFragment.UNKNOWN;
    }

    /**
     * Gets all the fragments, if the selection is complete.
     *
     * @param filter the value to filter on, or {@code null} for the whole
     *            selection
     * @return the fragments, or {@code null} if the list is not known to be
     *         complete
     */
    public List<SimpleFragment> getFragmentsByValue(Serializable filter) {
        if (!complete) {
            return null;
        }
        // fetch fragments and maybe filter
        List<SimpleFragment> filtered = new LinkedList<SimpleFragment>();
        if (existing != null) {
            for (Serializable id : existing) {
                SimpleFragment fragment;
                try {
                    fragment = getFragment(id);
                } catch (StorageException e) {
                    log.warn("Failed refetch for: " + id, e);
                    continue;
                }
                if (fragment == null) {
                    log.warn("Existing fragment missing: " + id);
                    continue;
                }
                if (filter == null || filter.equals(fragmentValue(fragment))) {
                    filtered.add(fragment);
                }
            }
        }
        if (created != null) {
            for (Serializable id : created) {
                SimpleFragment fragment = getFragmentIfPresent(id);
                if (fragment == null) {
                    log.warn("Created fragment missing: " + id);
                    continue;
                }
                if (filter == null || filter.equals(fragmentValue(fragment))) {
                    filtered.add(fragment);
                }
            }
        }
        return filtered;
    }

}
