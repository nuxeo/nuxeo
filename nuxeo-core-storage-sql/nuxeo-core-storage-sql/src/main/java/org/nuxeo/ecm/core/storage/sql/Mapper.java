/*
 * (C) Copyright 2007-2010 Nuxeo SA (http://nuxeo.com/) and contributors.
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
import java.util.List;
import java.util.Map;

import javax.transaction.xa.XAResource;

import org.nuxeo.ecm.core.api.IterableQueryResult;
import org.nuxeo.ecm.core.query.QueryFilter;
import org.nuxeo.ecm.core.storage.PartialList;
import org.nuxeo.ecm.core.storage.StorageException;

/**
 * A {@link Mapper} maps {@link Row}s to and from the database.
 */
public interface Mapper extends XAResource {

    void close();

    // TODO
    int getTableSize(String tableName);

    /**
     * Creates the necessary structures in the database.
     */
    // TODO
    void createDatabase() throws StorageException;

    /*
     * ----- Root -----
     */

    /**
     * Gets the root id for a given repository, if registered.
     *
     * @param repositoryId the repository id, usually 0
     * @return the root id, or null if not found
     */
    Serializable getRootId(Serializable repositoryId) throws StorageException;

    /**
     * Records the newly generated root id for a given repository.
     *
     * @param repositoryId the repository id, usually 0
     * @param id the root id
     */
    void setRootId(Serializable repositoryId, Serializable id)
            throws StorageException;

    /*
     * ----- Read -----
     */

    /**
     * Gets a row from the database, given its table name and id. If the row
     * doesn't exist, {@code null} is returned.
     *
     * @param tableName the table name
     * @param id the id
     * @return the row, or {@code null}
     */
    Row readSingleRow(String tableName, Serializable id)
            throws StorageException;

    /**
     * Gets a list of rows from the database, given the table name and the ids.
     *
     * @param tableName the table name
     * @param ids the ids
     * @return the list of rows
     */
    List<Row> readMultipleRows(String tableName, List<Serializable> ids)
            throws StorageException;

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
     * @return the child hierarchy rows, or {@code null}
     */
    List<Row> readChildHierRows(Serializable parentId, boolean complexProp)
            throws StorageException;

    /**
     * Gets an array for a {@link CollectionFragment} from the database, given
     * its table name and id. If now rows are found, an empty array is returned.
     *
     * @param tableName the table name
     * @param id the id
     * @return the array
     */
    Serializable[] readCollectionArray(String tableName, Serializable id)
            throws StorageException;

    /**
     * Reads several collection rows, given a table name and the ids.
     *
     * @param ids the ids
     * @param a map from id to row
     */
    Map<Serializable, Serializable[]> readCollectionsArrays(String tableName,
            List<Serializable> ids) throws StorageException;

    /*
     * ----- Create -----
     */

    /**
     * Inserts a new {@link Row} in the storage.
     *
     * @param tableName the table name
     * @param row the row
     * @return the id (generated or not)
     */
    Serializable insertSingleRow(String tableName, Row row)
            throws StorageException;

    /**
     * Inserts a new set of collection rows in the database.
     *
     * @param tableName the table name
     * @param id the rows id
     * @param array the rows values
     */
    void insertCollectionRows(String tableName, Serializable id,
            Serializable[] array) throws StorageException;

    /*
     * ----- Update -----
     */

    /**
     * Updates a row in the database.
     *
     * @param tableName the table name
     * @param row the row
     * @param keys the columns to update
     * @throws StorageException
     */
    void updateSingleRow(String tableName, Row row, List<String> keys)
            throws StorageException;

    /**
     * Updates a set of collection rows in the database.
     * <p>
     * Does a simple delete + insert for now.
     *
     * @param tableName the table name
     * @param id the rows id
     * @param array the rows values
     * @throws StorageException
     */
    void updateCollectionRows(String tableName, Serializable id,
            Serializable[] array) throws StorageException;

    /*
     * ----- Delete-----
     */

    /**
     * Deletes one or several rows from the database.
     *
     * @param tableName the table name
     * @param id the id of the row(s)
     */
    void deleteRows(String tableName, Serializable id) throws StorageException;

    /*
     * ----- Copy -----
     */

    public static class CopyHierarchyResult {
        /** The id of the root of the copy. */
        public Serializable copyId;

        /** The invalidations generated by the copy. */
        public Invalidations invalidations;
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
     * @param sourceId the id of row to copy (with children)
     * @param typeName the type of the row to copy (to avoid refetching known
     *            info)
     * @param destParentId the new parent id, or {@code null}
     * @param destName the new name
     * @param overwriteRow when not {@code null}, the copy is done onto this
     *            existing row, and the values are set in hierarchy
     * @return info about the copy
     * @throws StorageException
     */
    CopyHierarchyResult copyHierarchy(Serializable sourceId, String typeName,
            Serializable destParentId, String destName, Row overwriteRow)
            throws StorageException;

    /*
     * ----- Version/Proxy -----
     */

    /**
     * Gets the id of a version given a versionableId and a label.
     *
     * @param versionableId the versionable id
     * @param label the label
     * @return the id of the version, or {@code null} if not found
     * @throws StorageException
     */
    Serializable getVersionIdByLabel(Serializable versionableId, String label)
            throws StorageException;

    /**
     * Gets the id of the last version given a versionable id.
     *
     * @param versionableId the versionable id
     * @return the id of the last version, or {@code null} if not found
     * @throws StorageException
     */
    Serializable getLastVersionId(Serializable versionableId)
            throws StorageException;

    /**
     * Gets the list of version rows for all the versions having a given
     * versionable id.
     *
     * @param versionableId the versionable id
     * @return the list of version rows
     * @throws StorageException
     */
    List<Row> getVersionsRows(Serializable versionableId)
            throws StorageException;

    /**
     * Finds proxies, maybe restricted to the children of a given parent.
     *
     * @param searchId the id to look for
     * @param byTarget {@code true} if the searchId is a proxy target id,
     *            {@code false} if the searchId is a versionable id
     * @param parentId the parent to which to restrict, if not {@code null}
     * @return the list of proxies rows
     * @throws StorageException
     */
    List<Row> getProxyRows(Serializable searchId, boolean byTarget,
            Serializable parentId) throws StorageException;

    /*
     * ----- Query -----
     */

    /**
     * Makes a NXQL query to the database.
     *
     * @param query the query
     * @param queryFilter the query filter
     * @param countTotal if {@code true}, count the total size without
     *            limit/offset
     * @param session the current session (to resolve paths)
     * @return the list of matching document ids
     * @throws StorageException
     */
    PartialList<Serializable> query(String query, QueryFilter queryFilter,
            boolean countTotal, Session session) throws StorageException;

    /**
     * Makes a query to the database and returns an iterable (which must be
     * closed when done).
     *
     * @param query the query
     * @param queryType the query type
     * @param queryFilter the query filter
     * @param session the current session (to resolve paths)
     * @param params optional query-type-dependent parameters
     * @return an iterable, which <b>must</b> be closed when done
     * @throws StorageException
     */
    // queryFilter used for principals and permissions
    public IterableQueryResult queryAndFetch(String query, String queryType,
            QueryFilter queryFilter, Session session, Object... params)
            throws StorageException;

    /*
     * ----- ACLs -----
     */

    void updateReadAcls() throws StorageException;

    void rebuildReadAcls() throws StorageException;

    /*
     * ----- Clustering -----
     */

    /**
     * Informs the cluster that this node exists.
     */
    void createClusterNode() throws StorageException;

    /**
     * Removes this node from the cluster.
     */
    void removeClusterNode() throws StorageException;

    /**
     * Inserts the invalidation rows for the other cluster nodes.
     */
    void insertClusterInvalidations(Invalidations invalidations)
            throws StorageException;

    /**
     * Gets the invalidations from other cluster nodes.
     */
    Invalidations getClusterInvalidations() throws StorageException;

}
