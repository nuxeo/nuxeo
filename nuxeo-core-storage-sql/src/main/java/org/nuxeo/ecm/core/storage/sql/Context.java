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

import java.io.Serializable;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.commons.collections.map.ReferenceMap;
import org.nuxeo.ecm.core.storage.StorageException;
import org.nuxeo.ecm.core.storage.sql.Fragment.State;

/**
 * This class holds persistence context information for one table.
 * <p>
 * All non-saved modified data is referenced here. At save time, the data is
 * sent to the database by the {@link Mapper}. The database will at some time
 * later be committed by the external transaction manager in effect.
 * <p>
 * Internally a fragment can be in at most one of the caches: pristine, created,
 * modified, deleted. The pristine cache survives a save(), and may be partially
 * invalidated after commit by other local or clustered contexts that committed
 * too.
 * <p>
 * Depending on the table, the context may hold {@link SimpleFragment}s, which
 * represent one row, {@link CollectionFragment}s, which represent several rows.
 * <p>
 * This class is not thread-safe, it should be tied to a single session and the
 * session itself should not be used concurrently.
 *
 * @author Florent Guillaume
 */
public class Context {

    private final String tableName;

    // also accessed by Fragment
    protected final Mapper mapper;

    protected final Model model;

    protected final PersistenceContext persistenceContext;

    /**
     * The pristine fragments. All held data is identical to what is present in
     * the database and could be refetched if needed.
     * <p>
     * This contains fragment that are {@link State#PRISTINE} or
     * {@link State#ABSENT}, or in some cases {@link State#INVALIDATED_MODIFIED}
     * or {@link State#INVALIDATED_DELETED}.
     * <p>
     * This cache is memory-sensitive, a fragment can always be refetched if the
     * GC collects it.
     */
    protected final Map<Serializable, Fragment> pristine;

    /**
     * The fragments changed by the session.
     * <p>
     * This contains fragment that are {@link State#CREATED},
     * {@link State#MODIFIED} or {@link State#DELETED}.
     */
    protected final Map<Serializable, Fragment> modified;

    /**
     * The set of modified/created fragments that should be invalidated in other
     * sessions at post-commit time.
     */
    private final Set<Serializable> modifiedInTransaction;

    /**
     * The set of deleted fragments that should be invalidated in other sessions
     * at post-commit time.
     */
    private final Set<Serializable> deletedInTransaction;

    /**
     * The set of fragments that have to be invalidated as modified in this
     * session at post-commit time.
     */
    private final Set<Serializable> modifiedInvalidations;

    /**
     * The set of fragments that have to be invalidated as deleted in this
     * session at post-commit time.
     */
    private final Set<Serializable> deletedInvalidations;

    protected final boolean isCollection;

    @SuppressWarnings("unchecked")
    Context(String tableName, Mapper mapper,
            PersistenceContext persistenceContext) {
        this.tableName = tableName;
        this.mapper = mapper;
        this.persistenceContext = persistenceContext;
        model = mapper.getModel();
        pristine = new ReferenceMap(ReferenceMap.HARD, ReferenceMap.SOFT);
        modified = new HashMap<Serializable, Fragment>();

        modifiedInTransaction = new HashSet<Serializable>();
        deletedInTransaction = new HashSet<Serializable>();
        modifiedInvalidations = new HashSet<Serializable>();
        deletedInvalidations = new HashSet<Serializable>();

        isCollection = model.isCollectionFragment(tableName);
    }

    public String getTableName() {
        return tableName;
    }

    /**
     * Clears all the caches. Called by RepositoryManagement.
     */
    protected int clearCaches() {
        // TODO there should be a synchronization here
        // but this is a rare operation and we don't call
        // it if a transaction is in progress
        int n = pristine.size();
        pristine.clear();
        return n;
    }

    /**
     * Closes the context. Keeps around the {@link #pristine} and
     * {@link #absent} caches (to avoid costly refills). These two caches are
     * nevertheless still invalidatable.
     */
    public void close() {
        detachAll();
    }

    private void detachAll() {
        for (Fragment fragment : modified.values()) {
            fragment.setDetached();
        }
        modified.clear();
    }

    /**
     * Creates a new row in the context.
     *
     * @param id the id
     * @param map the fragments map, or {@code null}
     * @return the created row
     * @throws StorageException if the row is already in the context
     */
    public SimpleFragment create(Serializable id, Map<String, Serializable> map)
            throws StorageException {
        if (pristine.containsKey(id) || modified.containsKey(id)) {
            throw new IllegalStateException("Row already registered: " + id);
        }
        return new SimpleFragment(id, State.CREATED, this, map);
    }

    /**
     * Gets a fragment.
     * <p>
     * If it's not in the context, fetch it from the mapper. If it's not in the
     * database, returns {@code null} or an absent fragment.
     *
     * @param id the fragment id
     * @param allowAbsent {@code true} to return an absent fragment as an object
     *            instead of {@code null}
     * @return the fragment, or {@code null} if none is found and {@value
     *         allowAbsent} was {@code false}
     * @throws StorageException
     */
    public Fragment get(Serializable id, boolean allowAbsent)
            throws StorageException {
        Fragment fragment = getIfPresent(id);
        if (fragment == null) {
            return getFromMapper(id, allowAbsent);
        }
        if (fragment.getState() == State.DELETED) {
            return null;
        }
        return fragment;
    }

    /**
     * Gets a fragment from the mapper.
     */
    protected Fragment getFromMapper(Serializable id, boolean allowAbsent)
            throws StorageException {
        if (persistenceContext.isIdNew(id)) {
            // the id has not been saved, so nothing exists yet in the database
            if (isCollection) {
                return model.newEmptyCollectionFragment(id, this);
            } else {
                return allowAbsent ? new SimpleFragment(id, State.ABSENT, this,
                        null) : null;
            }
        } else {
            if (isCollection) {
                Serializable[] array = mapper.readCollectionArray(id, this);
                return model.newCollectionFragment(id, array, this);
            } else {
                Map<String, Serializable> map = mapper.readSingleRowMap(
                        tableName, id, this);
                if (map == null) {
                    return allowAbsent ? new SimpleFragment(id, State.ABSENT,
                            this, null) : null;
                }
                return new SimpleFragment(id, State.PRISTINE, this, map);
            }
        }
    }

    /**
     * Gets a fragment, if present.
     * <p>
     * If it's not in the context, returns {@code null}.
     * <p>
     * Called by {@link #get}, and by the {@link Mapper} to reuse known
     * hierarchy fragments in lists of children.
     */
    protected Fragment getIfPresent(Serializable id) {
        Fragment fragment;
        fragment = pristine.get(id);
        if (fragment != null) {
            return fragment;
        }
        return modified.get(id);
    }

    /**
     * Removes a row from the context.
     *
     * @param fragment
     * @throws StorageException
     */
    public void remove(Fragment fragment) throws StorageException {
        fragment.markDeleted();
    }

    /**
     * Removes a fragment from the database.
     *
     * @param id the fragment id
     * @throws StorageException
     */
    protected void remove(Serializable id) throws StorageException {
        Fragment fragment = getIfPresent(id);
        if (fragment != null) {
            if (fragment.getState() != State.DELETED) {
                remove(fragment);
            }
        } else {
            // this registers it with the "modified" map
            new SimpleFragment(id, State.DELETED, this, null);
        }
    }

    /**
     * Allows for remapping a row upon save.
     *
     * @param fragment the fragment
     * @param idMap the map of old to new ids
     */
    protected void remapFragmentOnSave(Fragment fragment,
            Map<Serializable, Serializable> idMap) throws StorageException {
        // subclasses change this
        // TODO XXX there are other references to id (versionableid,
        // targetid, etc).
    }

    /**
     * Saves all the created, modified or deleted rows, except for the created
     * main rows which have already been done.
     *
     * @param idMap the map of temporary ids to final ids to use in translating
     *            secondary created rows
     * @throws StorageException
     */
    public void save(Map<Serializable, Serializable> idMap)
            throws StorageException {
        for (Fragment fragment : modified.values()) {
            Serializable id = fragment.getId();
            switch (fragment.getState()) {
            case CREATED:
                /*
                 * Map temporary to persistent ids.
                 */
                Serializable newId = idMap.get(id);
                if (newId != null) {
                    fragment.setId(newId);
                    id = newId;
                }
                /*
                 * Do the creation.
                 */
                if (isCollection) {
                    mapper.insertCollectionRows((CollectionFragment) fragment);
                } else {
                    mapper.insertSingleRow((SimpleFragment) fragment);
                }
                fragment.setPristine();
                // modified map cleared at end of loop
                pristine.put(id, fragment);
                modifiedInTransaction.add(id);
                break;
            case MODIFIED:
                if (isCollection) {
                    mapper.updateCollectionRows((CollectionFragment) fragment);
                } else {
                    mapper.updateSingleRow((SimpleFragment) fragment);
                }
                fragment.setPristine();
                // modified map cleared at end of loop
                pristine.put(id, fragment);
                modifiedInTransaction.add(id);
                break;
            case DELETED:
                // TODO deleting non-hierarchy fragments is done by the database
                // itself as their foreign key to hierarchy is ON DELETE CASCADE
                mapper.deleteFragment(fragment);
                fragment.setDetached();
                // modified map cleared at end of loop
                deletedInTransaction.add(id);
                break;
            default:
                throw new AssertionError(fragment);
            }
        }
        modified.clear();
    }

    /**
     * Called by the mapper when a fragment has been updated in the database.
     *
     * @param id the id
     * @param wasModified {@code true} for a modification, {@code false} for a
     *            deletion
     */
    protected void markInvalidated(Serializable id, boolean wasModified) {
        if (wasModified) {
            Fragment fragment = getIfPresent(id);
            if (fragment != null) {
                fragment.markInvalidatedModified();
            }
            modifiedInTransaction.add(id);
        } else { // deleted
            Fragment fragment = getIfPresent(id);
            if (fragment != null) {
                fragment.markInvalidatedDeleted();
            }
            deletedInTransaction.add(id);
        }
    }

    /**
     * Process invalidations notified from other sessions. Called
     * pre-transaction.
     */
    protected void processInvalidations() {
        synchronized (modifiedInvalidations) {
            for (Serializable id : modifiedInvalidations) {
                Fragment fragment = pristine.remove(id);
                if (fragment != null) {
                    fragment.setInvalidatedModified();
                }
            }
            modifiedInvalidations.clear();
        }
        synchronized (deletedInvalidations) {
            for (Serializable id : deletedInvalidations) {
                Fragment fragment = pristine.remove(id);
                if (fragment != null) {
                    fragment.setInvalidatedDeleted();
                }
            }
            deletedInvalidations.clear();
        }
    }

    /**
     * Notify invalidations to other sessions. Called post-transaction.
     *
     * @return true if there were invalidations to send
     */
    protected boolean notifyInvalidations() {
        if (!modifiedInTransaction.isEmpty() || !deletedInTransaction.isEmpty()) {
            persistenceContext.invalidateOthers(this);
            modifiedInTransaction.clear();
            deletedInTransaction.clear();
            return true;
        }
        return false;
    }

    /**
     * Called by cross-session invalidation when another session just committed.
     */
    protected void invalidate(Context other) {
        synchronized (modifiedInvalidations) {
            modifiedInvalidations.addAll(other.modifiedInTransaction);
        }
        synchronized (deletedInvalidations) {
            deletedInvalidations.addAll(other.deletedInTransaction);
        }
    }

}
