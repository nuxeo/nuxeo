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
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.transaction.xa.XAException;
import javax.transaction.xa.Xid;

/**
 * A {@link RowMapper} maps {@link Row}s to and from the database.
 * <p>
 * These are the operations that can benefit from a cache.
 *
 * @see SoftRefCachingRowMapper
 */
public interface RowMapper {

    /**
     * Computes a new unique id.
     *
     * @return a new unique id
     */
    Serializable generateNewId();

    /*
     * ----- Batch -----
     */

    /**
     * Reads a set of rows for the given {@link RowId}s.
     * <p>
     * For each requested row, either a {@link Row} is found and returned, or a {@link RowId} (not implementing
     * {@link Row}) is returned to signify an absent row.
     *
     * @param rowIds the row ids (including their table name)
     * @param cacheOnly if {@code true}, only hit memory
     * @return the collection of {@link Row}s (or {@link RowId}s if the row was absent from the database). Order is not
     *         the same as the input {@code rowIds}
     */
    List<? extends RowId> read(Collection<RowId> rowIds, boolean cacheOnly);

    /**
     * A {@link Row} and a list of its keys that have to be updated.
     */
    public static final class RowUpdate implements Serializable {
        private static final long serialVersionUID = 1L;

        public final Row row;

        // used for simple fragments
        public final Collection<String> keys;

        // used for collection fragment right push, the pos at which to start to insert
        // if -1 then a full update must be done
        public final int pos;

        /** Constructor for simple fragment update. */
        public RowUpdate(Row row, Collection<String> keys) {
            this.row = row;
            this.keys = keys;
            pos = -1;
        }

        /** Constructor for collection fragment full update. */
        public RowUpdate(Row row) {
            this(row, -1);
        }

        /** Constructor for collection fragment right push update. */
        public RowUpdate(Row row, int pos) {
            this.row = row;
            keys = null;
            this.pos = pos;
        }

        @Override
        public int hashCode() {
            return row.hashCode();
        }

        @Override
        public boolean equals(Object other) {
            if (other instanceof RowUpdate) {
                return equal((RowUpdate) other);
            }
            return false;
        }

        private boolean equal(RowUpdate other) {
            return other.row.equals(row);
        }

        @Override
        public String toString() {
            return getClass().getSimpleName() + '(' + row + ", keys=" + keys + ')';
        }
    }

    /**
     * The description of a set of rows to create, update or delete.
     */
    public static class RowBatch implements Serializable {
        private static final long serialVersionUID = 1L;

        /**
         * Creates are done first and are ordered.
         */
        public final List<Row> creates;

        /**
         * Updates.
         */
        public final Set<RowUpdate> updates;

        /**
         * Deletes are done last.
         */
        public final Set<RowId> deletes;

        /**
         * Dependent deletes aren't executed in the database but still trigger invalidations.
         */
        public final Set<RowId> deletesDependent;

        public RowBatch() {
            creates = new LinkedList<Row>();
            updates = new HashSet<RowUpdate>();
            deletes = new HashSet<RowId>();
            deletesDependent = new HashSet<RowId>();
        }

        public boolean isEmpty() {
            return creates.isEmpty() && updates.isEmpty() && deletes.isEmpty() && deletesDependent.isEmpty();
        }

        @Override
        public String toString() {
            return getClass().getSimpleName() + "(creates=" + creates + ", updates=" + updates + ", deletes=" + deletes
                    + ", deletesDependent=" + deletesDependent + ')';
        }
    }

    /**
     * Writes a set of rows. This includes creating, updating and deleting rows.
     *
     * @param batch the set of rows and the operations to do on them
     */
    void write(RowBatch batch);

    /*
     * ----- Read -----
     */

    /**
     * Gets a row for a {@link SimpleFragment} from the database, given its table name and id. If the row doesn't exist,
     * {@code null} is returned.
     *
     * @param rowId the row id
     * @return the row, or {@code null}
     */
    Row readSimpleRow(RowId rowId);

    /**
     * Gets the fulltext extracted from the binary fields.
     *
     * @since 5.9.3
     * @param rowId the row id
     * @return the fulltext string representation or {@code null} if unsupported
     */
    Map<String, String> getBinaryFulltext(RowId rowId);

    /**
     * Gets an array for a {@link CollectionFragment} from the database, given its table name and id. If no rows are
     * found, an empty array is returned.
     *
     * @param rowId the row id
     * @return the array
     */
    Serializable[] readCollectionRowArray(RowId rowId);

    /**
     * Reads the rows corresponding to a selection.
     *
     * @param selType the selection type
     * @param selId the selection id (parent id for a hierarchy selection)
     * @param filter the filter value (name for a hierarchy selection)
     * @param criterion an optional additional criterion depending on the selection type (complex prop flag for a
     *            hierarchy selection)
     * @param limitToOne whether to stop after one row retrieved
     * @return the list of rows
     */
    List<Row> readSelectionRows(SelectionType selType, Serializable selId, Serializable filter, Serializable criterion,
            boolean limitToOne);

    /**
     * Gets all the selection ids for a given list of values.
     *
     * @since 9.2
     */
    Set<Serializable> readSelectionsIds(SelectionType selType, List<Serializable> values);

    /*
     * ----- Copy -----
     */

    /**
     * A document id and its primary type and mixin types.
     */
    public static final class IdWithTypes implements Serializable {
        private static final long serialVersionUID = 1L;

        public final Serializable id;

        public final String primaryType;

        public final String[] mixinTypes;

        public IdWithTypes(Serializable id, String primaryType, String[] mixinTypes) {
            this.id = id;
            this.primaryType = primaryType;
            this.mixinTypes = mixinTypes;
        }

        public IdWithTypes(Node node) {
            this.id = node.getId();
            this.primaryType = node.getPrimaryType();
            this.mixinTypes = node.getMixinTypes();
        }

        public IdWithTypes(SimpleFragment hierFragment) {
            this.id = hierFragment.getId();
            this.primaryType = hierFragment.getString(Model.MAIN_PRIMARY_TYPE_KEY);
            this.mixinTypes = (String[]) hierFragment.get(Model.MAIN_MIXIN_TYPES_KEY);
        }

        @Override
        public String toString() {
            return getClass().getSimpleName() + "(id=" + id + ",primaryType=" + primaryType + ",mixinTypes="
                    + Arrays.toString(mixinTypes) + ")";
        }
    }

    public static final class CopyResult implements Serializable {
        private static final long serialVersionUID = 1L;

        /** The id of the root of the copy. */
        public final Serializable copyId;

        /** The invalidations generated by the copy. */
        public final Invalidations invalidations;

        /** The ids of newly created proxies. */
        public final Set<Serializable> proxyIds;

        public CopyResult(Serializable copyId, Invalidations invalidations, Set<Serializable> proxyIds) {
            this.copyId = copyId;
            this.invalidations = invalidations;
            this.proxyIds = proxyIds;
        }
    }

    /**
     * Copies the hierarchy starting from a given row to a new parent with a new name.
     * <p>
     * If the new parent is {@code null}, then this is a version creation, which doesn't recurse in regular children.
     * <p>
     * If {@code overwriteRow} is passed, the copy is done onto this existing node as its root (version restore) instead
     * of creating a new node in the parent.
     *
     * @param source the id, primary type and mixin types of the row to copy
     * @param destParentId the new parent id, or {@code null}
     * @param destName the new name
     * @param overwriteRow when not {@code null}, the copy is done onto this existing row, and the values are set in
     *            hierarchy
     * @return info about the copy
     */
    CopyResult copy(IdWithTypes source, Serializable destParentId, String destName, Row overwriteRow);

    /**
     * A document id, parent id and primary type, along with the version and proxy information (the potentially impacted
     * selections).
     * <p>
     * Used to return info about a descendants tree for removal.
     */
    public static final class NodeInfo implements Serializable {
        private static final long serialVersionUID = 1L;

        public final Serializable id;

        public final Serializable parentId;

        public final String primaryType;

        public final Boolean isProperty;

        public final Serializable versionSeriesId;

        public final Serializable targetId;

        /**
         * Creates node info for a node that may also be a proxy.
         */
        public NodeInfo(Serializable id, Serializable parentId, String primaryType, Boolean isProperty,
                Serializable versionSeriesId, Serializable targetId) {
            this.id = id;
            this.parentId = parentId;
            this.primaryType = primaryType;
            this.isProperty = isProperty;
            this.versionSeriesId = versionSeriesId;
            this.targetId = targetId;
        }

        /**
         * Creates node info for a node that may also be a proxy or a version.
         */
        public NodeInfo(SimpleFragment hierFragment, SimpleFragment versionFragment, SimpleFragment proxyFragment) {
            id = hierFragment.getId();
            parentId = hierFragment.get(Model.HIER_PARENT_KEY);
            primaryType = hierFragment.getString(Model.MAIN_PRIMARY_TYPE_KEY);
            isProperty = (Boolean) hierFragment.get(Model.HIER_CHILD_ISPROPERTY_KEY);
            Serializable ps = proxyFragment == null ? null : proxyFragment.get(Model.PROXY_VERSIONABLE_KEY);
            if (ps == null) {
                versionSeriesId = versionFragment == null ? null : versionFragment.get(Model.VERSION_VERSIONABLE_KEY);
                // may still be null
                targetId = null; // marks it as a version if versionableId not
                                 // null
            } else {
                versionSeriesId = ps;
                targetId = proxyFragment.get(Model.PROXY_TARGET_KEY);
            }
        }
    }

    /**
     * Gets descendants infos from a given root node. This does not include information about the root node itself.
     *
     * @param rootId the root node id from which to get descendants info
     * @return the list of descendant nodes info
     * @since 9.2
     */
    List<NodeInfo> getDescendantsInfo(Serializable rootId);

    /**
     * Deletes a hierarchy.
     *
     * @param rootId the id of the root node to be deleted with its children
     * @param nodeInfos the information about all descendants being deleted along the root node
     * @since 9.2
     */
    void remove(Serializable rootId, List<NodeInfo> nodeInfos);

    /**
     * Processes and returns the invalidations queued for processing by the cache (if any).
     * <p>
     * Called pre-transaction by session start or transactionless save;
     *
     * @return the invalidations, or {@code null}
     */
    Invalidations receiveInvalidations();

    /**
     * Post-transaction invalidations notification.
     * <p>
     * Called post-transaction by session commit/rollback or transactionless save.
     *
     * @param invalidations the known invalidations to send to others, or {@code null}
     */
    void sendInvalidations(Invalidations invalidations);

    /**
     * Clears the mapper's cache (if any)
     * <p>
     * Called after a rollback, or a manual clear through RepositoryStatus MBean.
     */
    void clearCache();

    /**
     * Evaluate the cached elements size
     *
     * @since 5.7.2
     */
    long getCacheSize();

    /**
     * Rollback the XA Resource.
     * <p>
     * This is in the {@link RowMapper} interface because on rollback the cache must be invalidated.
     */
    void rollback(Xid xid) throws XAException;

}
