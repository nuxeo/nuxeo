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
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import javax.transaction.xa.XAException;
import javax.transaction.xa.Xid;

import org.nuxeo.ecm.core.storage.StorageException;
import org.nuxeo.ecm.core.storage.sql.Invalidations.InvalidationsPair;

/**
 * A {@link RowMapper} maps {@link Row}s to and from the database.
 * <p>
 * These are the operations that can benefit from a cache.
 *
 * @see CachingRowMapper
 */
public interface RowMapper {

    /*
     * ----- Batch -----
     */

    /**
     * Reads a set of rows for the given {@link RowId}s.
     * <p>
     * For each requested row, either a {@link Row} is found and returned, or a
     * {@link RowId} (not implementing {@link Row}) is returned to signify an
     * absent row.
     *
     * @param rowIds the row ids (including their table name)
     * @return the collection of {@link Row}s (or {@link RowId}s if the row was
     *         absent from the database). Order is not the same as the input
     *         {@code rowIds}
     * @throws StorageException
     */
    List<? extends RowId> read(Collection<RowId> rowIds)
            throws StorageException;

    /**
     * A {@link Row} and a list of its keys that have to be updated.
     */
    public static final class RowUpdate implements Serializable {
        private static final long serialVersionUID = 1L;

        public final Row row;

        public final Collection<String> keys;

        public RowUpdate(Row row, Collection<String> keys) {
            this.row = row;
            this.keys = keys;
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
            return getClass().getSimpleName() + '(' + row + ", keys=" + keys
                    + ')';
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
         * Dependent deletes aren't executed in the database but still trigger
         * invalidations.
         */
        public final Set<RowId> deletesDependent;

        public RowBatch() {
            creates = new LinkedList<Row>();
            updates = new HashSet<RowUpdate>();
            deletes = new HashSet<RowId>();
            deletesDependent = new HashSet<RowId>();
        }

        public boolean isEmpty() {
            return creates.isEmpty() && updates.isEmpty() && deletes.isEmpty()
                    && deletesDependent.isEmpty();
        }

        @Override
        public String toString() {
            return getClass().getSimpleName() + "(creates=" + creates
                    + ", updates=" + updates + ", deletes=" + deletes
                    + ", deletesDependent=" + deletesDependent + ')';
        }
    }

    /**
     * Writes a set of rows. This includes creating, updating and deleting rows.
     *
     * @param batch the set of rows and the operations to do on them
     * @throws StorageException
     */
    void write(RowBatch batch) throws StorageException;

    /*
     * ----- Read -----
     */

    /**
     * Gets a row for a {@link SimpleFragment} from the database, given its
     * table name and id. If the row doesn't exist, {@code null} is returned.
     *
     * @param rowId the row id
     * @return the row, or {@code null}
     */
    Row readSimpleRow(RowId rowId) throws StorageException;

    /**
     * Gets an array for a {@link CollectionFragment} from the database, given
     * its table name and id. If no rows are found, an empty array is returned.
     *
     * @param rowId the row id
     * @return the array
     */
    Serializable[] readCollectionRowArray(RowId rowId) throws StorageException;

    /**
     * Reads the hierarchy row for a child, given its parent id and the child
     * name.
     *
     * @param parentId the parent id
     * @param childName the child name
     * @param complexProp whether to get complex properties ({@code true}) or
     *            regular children({@code false})
     * @return the child hierarchy row, or {@code null}
     */
    Row readChildHierRow(Serializable parentId, String childName,
            boolean complexProp) throws StorageException;

    /**
     * Reads the hierarchy rows for all the children of parent.
     * <p>
     * Depending on the boolean {@literal complexProp}, only the complex
     * properties or only the regular children are returned.
     *
     * @param parentId the parent id
     * @param complexProp whether to get complex properties ({@code true}) or
     *            regular children({@code false})
     * @return the child hierarchy rows
     */
    List<Row> readChildHierRows(Serializable parentId, boolean complexProp)
            throws StorageException;

    /**
     * Gets the list of version rows for all the versions in a given version
     * series id.
     *
     * @param versionSeriesId the version series id
     * @return the list of version rows
     * @throws StorageException
     */
    List<Row> getVersionRows(Serializable versionSeriesId)
            throws StorageException;

    /**
     * Finds proxies, maybe restricted to the children of a given parent.
     *
     * @param searchId the id to look for
     * @param byTarget {@code true} if the searchId is a proxy target id,
     *            {@code false} if the searchId is a version series id
     * @param parentId the parent to which to restrict, if not {@code null}
     * @return the list of proxies rows
     * @throws StorageException
     */
    List<Row> getProxyRows(Serializable searchId, boolean byTarget,
            Serializable parentId) throws StorageException;

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

        public IdWithTypes(Serializable id, String primaryType,
                String[] mixinTypes) {
            this.id = id;
            this.primaryType = primaryType;
            this.mixinTypes = mixinTypes;
        }

        public IdWithTypes(Node node) {
            this.id = node.getId();
            this.primaryType = node.getPrimaryType();
            this.mixinTypes = node.getMixinTypes();
        }

        public IdWithTypes(SimpleFragment hierFragment) throws StorageException {
            this.id = hierFragment.getId();
            this.primaryType = hierFragment.getString(Model.MAIN_PRIMARY_TYPE_KEY);
            this.mixinTypes = (String[]) hierFragment.get(Model.MAIN_MIXIN_TYPES_KEY);
        }
    }

    public static final class CopyHierarchyResult implements Serializable {
        private static final long serialVersionUID = 1L;

        /** The id of the root of the copy. */
        public final Serializable copyId;

        /** The invalidations generated by the copy. */
        public final Invalidations invalidations;

        public CopyHierarchyResult(Serializable copyId,
                Invalidations invalidations) {
            this.copyId = copyId;
            this.invalidations = invalidations;
        }
    }

    /**
     * Copies the hierarchy starting from a given row to a new parent with a new
     * name.
     * <p>
     * If the new parent is {@code null}, then this is a version creation, which
     * doesn't recurse in regular children.
     * <p>
     * If {@code overwriteRow} is passed, the copy is done onto this existing
     * node as its root (version restore) instead of creating a new node in the
     * parent.
     *
     * @param source the id, primary type and mixin types of the row to copy
     * @param destParentId the new parent id, or {@code null}
     * @param destName the new name
     * @param overwriteRow when not {@code null}, the copy is done onto this
     *            existing row, and the values are set in hierarchy
     * @return info about the copy
     * @throws StorageException
     */
    CopyHierarchyResult copyHierarchy(IdWithTypes source,
            Serializable destParentId, String destName, Row overwriteRow)
            throws StorageException;

    /**
     * A document id, parent id and primary type, along with the version and
     * proxy information (the potentially impacted selections).
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
        public NodeInfo(Serializable id, Serializable parentId,
                String primaryType, Boolean isProperty,
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
        public NodeInfo(SimpleFragment hierFragment,
                SimpleFragment versionFragment, SimpleFragment proxyFragment)
                throws StorageException {
            id = hierFragment.getId();
            parentId = hierFragment.get(Model.HIER_PARENT_KEY);
            primaryType = hierFragment.getString(Model.MAIN_PRIMARY_TYPE_KEY);
            isProperty = (Boolean) hierFragment.get(Model.HIER_CHILD_ISPROPERTY_KEY);
            Serializable ps = proxyFragment == null ? null
                    : proxyFragment.get(Model.PROXY_VERSIONABLE_KEY);
            if (ps == null) {
                versionSeriesId = versionFragment == null ? null
                        : versionFragment.get(Model.VERSION_VERSIONABLE_KEY);
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
     * Deletes a hierarchy and returns information to generate invalidations.
     *
     * @param rootInfo info about the root to be deleted with its children (root
     *            id, and the rest is for invalidations)
     * @return info about the descendants removed (including the root)
     */
    List<NodeInfo> remove(NodeInfo rootInfo) throws StorageException;

    /**
     * Processes and returns the invalidations queued for processing by the
     * cache (if any).
     * <p>
     * Called pre-transaction by session start or transactionless save;
     *
     * @return the invalidations (both for the mapper and the events), or
     *         {@code null}
     */
    InvalidationsPair receiveInvalidations() throws StorageException;

    /**
     * Post-transaction invalidations notification.
     * <p>
     * Called post-transaction by session commit/rollback or transactionless
     * save.
     *
     * @param invalidations the known invalidations to send to others, or
     *            {@code null}
     */
    void sendInvalidations(Invalidations invalidations) throws StorageException;

    /**
     * Clears the mapper's cache (if any)
     * <p>
     * Called after a rollback, or a manual clear through RepositoryStatus
     * MBean.
     */
    void clearCache();

    /**
     * Rollback the XA Resource.
     * <p>
     * This is in the {@link RowMapper} interface because on rollback the cache
     * must be invalidated.
     */
    void rollback(Xid xid) throws XAException;

}
