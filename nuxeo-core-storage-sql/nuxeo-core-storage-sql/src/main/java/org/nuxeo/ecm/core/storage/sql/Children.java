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
 * Holds information about the children of a given parent node. The internal
 * state reflects:
 * <ul>
 * <li>children known to exist in the database,</li>
 * <li>created children not yet flushed to database,</li>
 * <li>deleted children not yet flushed to database.</li>
 * </ul>
 * Information about children in the database may be complete, or just partial
 * if only individual children have been retrieved from the database.
 * <p>
 * Children are stored in no particular order.
 * <p>
 * When this structure holds information all flushed to the database, then it
 * can safely be GC'ed, so it lives in a memory-sensitive map (softMap),
 * otherwise it's moved to a normal map (hardMap).
 * <p>
 * This class is not thread-safe and should be used only from a single-threaded
 * session.
 */
public class Children {

    private static final Log log = LogFactory.getLog(Children.class);

    /** The context from which to fetch hierarchy fragments. */
    protected final HierarchyContext hierContext;

    /** The key to use to filter on names. */
    protected final String filterKey;

    /**
     * This is {@code true} when complete information about the existing
     * children is known.
     * <p>
     * This is the case when a query to the database has been made to fetch all
     * children, or when a new parent node with no children has been created.
     */
    protected boolean complete;

    /** The ids known in the database and not deleted. This list is not ordered. */
    protected List<Serializable> existing;

    /** The ids created and not yet flushed to database. */
    protected List<Serializable> created;

    /**
     * The ids deleted (or which changed parents) and not yet flushed to
     * database.
     */
    protected Set<Serializable> deleted;

    /** The key which this has in the map holding it. */
    private final Serializable mapKey;

    /** The map where this is stored when GCable. */
    private final Map<Serializable, Children> softMap;

    /** The map where this is stored when not GCable. */
    private final Map<Serializable, Children> hardMap;

    /**
     * Constructs a Children cache.
     * <p>
     * It is automatically put in the soft map.
     *
     * @param hierContext the context from which to fetch hierarchy fragments
     * @param filterKey the key to use to filter on names
     * @param empty if the new instance is created empty
     * @param mapKey the key to use in the following maps
     * @param softMap the soft map, when the children are pristine
     * @param hardMap the hard map, when there are modifications to flush
     */
    public Children(HierarchyContext hierContext, String filterKey,
            boolean empty, Serializable mapKey,
            Map<Serializable, Children> softMap,
            Map<Serializable, Children> hardMap) {
        this.hierContext = hierContext;
        this.filterKey = filterKey;
        complete = empty;
        this.mapKey = mapKey;
        this.softMap = softMap;
        this.hardMap = hardMap;
        // starts its life in the soft map (no created or deleted)
        softMap.put(mapKey, this);
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
     * Adds a known child.
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
     * Adds a created child.
     *
     * @param id the fragment id
     */
    public void addCreated(Serializable id) {
        if (created == null) {
            created = new LinkedList<Serializable>();
            // move to hard map
            softMap.remove(mapKey);
            hardMap.put(mapKey, this);
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
     * Called after a database operation added children with unknown ids
     * (restore of complex properties).
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
            softMap.remove(mapKey);
            hardMap.put(mapKey, this);
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
        softMap.put(mapKey, this);
    }

    public boolean isFlushed() {
        return created == null && deleted == null;
    }

    /**
     * Gets a fragment given its name.
     * <p>
     * Returns {@code null} if there is no such child.
     * <p>
     * Returns {@link SimpleFragment#UNKNOWN} if there's no info about it.
     *
     * @param value the name
     * @return the fragment, or {@code null}, or {@link SimpleFragment#UNKNOWN}
     */
    public SimpleFragment getFragmentByValue(Serializable value) {
        if (existing != null) {
            for (Serializable id : existing) {
                SimpleFragment fragment;
                try {
                    fragment = hierContext.getHier(id, false);
                } catch (StorageException e) {
                    log.warn("Failed refetch for: " + id, e);
                    continue;
                }
                if (fragment == null) {
                    log.warn("Existing fragment missing: " + id);
                    continue;
                }
                if (value.equals(fragmentValue(fragment))) {
                    return fragment;
                }
            }
        }
        if (created != null) {
            for (Serializable id : created) {
                SimpleFragment fragment = hierContext.getHierIfPresent(id);
                if (fragment == null) {
                    log.warn("Created fragment missing: " + id);
                    continue;
                }
                if (value.equals(fragmentValue(fragment))) {
                    return fragment;
                }
            }
        }
        if (deleted != null) {
            for (Serializable id : deleted) {
                SimpleFragment fragment = hierContext.getHierIfPresent(id);
                if (fragment == null) {
                    log.warn("Deleted fragment missing: " + id);
                    continue;
                }
                if (value.equals(fragmentValue(fragment))) {
                    return null;
                }
            }
        }
        return complete ? null : SimpleFragment.UNKNOWN;
    }

    /**
     * Gets all the fragments, if the list of children is complete.
     *
     * @param value the name to filter on, or {@code null} for all children
     * @return the fragments, or {@code null} if the list is not known to be
     *         complete
     */
    public List<SimpleFragment> getFragmentsByValue(Serializable value) {
        if (!complete) {
            return null;
        }
        // fetch fragments and maybe filter by name
        List<SimpleFragment> filtered = new LinkedList<SimpleFragment>();
        if (existing != null) {
            for (Serializable id : existing) {
                SimpleFragment fragment;
                try {
                    fragment = hierContext.getHier(id, false);
                } catch (StorageException e) {
                    log.warn("Failed refetch for: " + id, e);
                    continue;
                }
                if (fragment == null) {
                    log.warn("Existing fragment missing: " + id);
                    continue;
                }
                if (value == null || value.equals(fragmentValue(fragment))) {
                    filtered.add(fragment);
                }
            }
        }
        if (created != null) {
            for (Serializable id : created) {
                SimpleFragment fragment = hierContext.getHierIfPresent(id);
                if (fragment == null) {
                    log.warn("Created fragment missing: " + id);
                    continue;
                }
                if (value == null || value.equals(fragmentValue(fragment))) {
                    filtered.add(fragment);
                }
            }
        }
        return filtered;
    }
}
