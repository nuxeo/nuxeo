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

import java.io.Serializable;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import javax.resource.cci.Connection;

import org.nuxeo.ecm.core.api.IterableQueryResult;
import org.nuxeo.ecm.core.api.PartialList;
import org.nuxeo.ecm.core.api.ScrollResult;
import org.nuxeo.ecm.core.model.LockManager;
import org.nuxeo.ecm.core.query.QueryFilter;

/**
 * The session is the main high level access point to data from the underlying database.
 *
 * @author Florent Guillaume
 */
public interface Session extends Connection {

    /**
     * Gets the low-level Mapper for this session.
     *
     * @return the mapper
     */
    Mapper getMapper();

    /**
     * Checks if the session is live (not closed).
     *
     * @return {@code true} if the session is live
     */
    boolean isLive();

    /**
     * Gets the session repository name.
     *
     * @return the repository name
     */
    String getRepositoryName();

    /**
     * Gets the {@link Model} associated to this session.
     *
     * @return the model
     */
    Model getModel();

    /**
     * Saves the modifications to persistent storage.
     * <p>
     * Modifications will be actually written only upon transaction commit.
     */
    void save();

    /**
     * Gets the root node of the repository.
     *
     * @return the root node
     */
    Node getRootNode();

    /**
     * Gets a node given its id.
     *
     * @param id the id
     * @return the node, or {@code null} if not found
     */
    Node getNodeById(Serializable id);

    /**
     * Gets several nodes given their ids.
     *
     * @param ids the ids
     * @return the nodes, in the same order as the ids, with elements being {@code null} if not found
     */
    List<Node> getNodesByIds(Collection<Serializable> ids);

    /**
     * Gets a node given its absolute path, or given an existing node and a relative path.
     *
     * @param path the path
     * @param node the node (ignored for absolute paths)
     * @return the node, or {@code null} if not found
     */
    Node getNodeByPath(String path, Node node);

    /**
     * Adds a mixin to a node.
     * <p>
     * Does nothing if the mixin was already present on the node.
     *
     * @param node the node
     * @param mixin the mixin name
     * @return {@code true} if the mixin was added, or {@code false} if it is already present
     * @since 5.8
     */
    boolean addMixinType(Node node, String mixin);

    /**
     * Removes a mixin from a node.
     * <p>
     * It's not possible to remove a mixin coming from the primary type.
     *
     * @param node the node
     * @param mixin the mixin
     * @return {@code true} if the mixin was removed, or {@code false} if it isn't present or is present on the type or
     *         does not exist
     * @since 5.8
     */
    boolean removeMixinType(Node node, String mixin);

    /**
     * Executes the given query and returns the first batch of results containing id of documents, next batch must be
     * requested within the {@code keepAliveSeconds} delay.
     *
     * @since 8.4
     */
    ScrollResult<String> scroll(String query, int batchSize, int keepAliveSeconds);

    /**
     * Get the next batch of results containing id of documents, the {@code scrollId} is part of the previous
     * {@link ScrollResult} response.
     *
     * @since 8.4
     */
    ScrollResult<String> scroll(String scrollId);

    /**
     * Interface for a class that knows how to resolve a node path into a node id.
     */
    interface PathResolver {
        /**
         * Returns the node id for a given path.
         *
         * @param path the node path
         * @return the node id, or {@code null}
         */
        Serializable getIdForPath(String path);
    }

    /**
     * Gets the parent of a node.
     * <p>
     * The root has a {@code null} parent.
     *
     * @param node the node
     * @return the parent node, or {@code null} for the root's parent
     */
    Node getParentNode(Node node);

    /**
     * Gets the absolute path of a node.
     *
     * @param node the node
     * @return the path
     */
    String getPath(Node node);

    /**
     * Checks if a child node with the given name exists.
     * <p>
     * There are two kinds of children, the regular children documents and the complex properties. The {@code boolean}
     * {@value #complexProp} allows a choice between those.
     *
     * @param parent the parent node
     * @param name the child name
     * @param complexProp whether to check complex properties or regular children
     * @return {@code true} if a child node with that name exists
     */
    boolean hasChildNode(Node parent, String name, boolean complexProp);

    /**
     * Gets a child node given its parent and name.
     *
     * @param parent the parent node
     * @param name the child name
     * @param complexProp whether to check complex properties or regular children
     * @return the child node, or {@code null} is not found
     */
    Node getChildNode(Node parent, String name, boolean complexProp);

    /**
     * Checks it a node has children.
     *
     * @param parent the parent node
     * @param complexProp whether to check complex properties or regular children
     * @return {@code true} if the parent has children
     */
    boolean hasChildren(Node parent, boolean complexProp);

    /**
     * Gets the children of a node.
     *
     * @param parent the parent node
     * @param name the children name to get (for lists of complex properties), or {@code null} for all
     * @param complexProp whether to check complex properties or regular children
     * @return the collection of children
     */
    List<Node> getChildren(Node parent, String name, boolean complexProp);

    /**
     * Creates a new child node.
     *
     * @param parent the parent to which the child is added
     * @param name the child name
     * @param pos the child position, or {@code null}
     * @param typeName the child type
     * @param complexProp whether this is a complex property ({@code true}) or a regular child ({@code false})
     * @return the new node
     */
    Node addChildNode(Node parent, String name, Long pos, String typeName, boolean complexProp);

    /**
     * Creates a new child node with given id (used for import).
     *
     * @param id the id
     * @param parent the parent to which the child is added
     * @param name the child name
     * @param pos the child position, or {@code null}
     * @param typeName the child type
     * @param complexProp whether this is a complex property ({@code true}) or a regular child ({@code false})
     * @return the new node
     */
    Node addChildNode(Serializable id, Node parent, String name, Long pos, String typeName, boolean complexProp);

    /**
     * Creates a proxy for a version node.
     *
     * @param targetId the target id
     * @param versionSeriesId the version series id
     * @param parent the parent to which the proxy is added
     * @param name the proxy name
     * @param pos the proxy position
     * @return the new proxy node
     */
    Node addProxy(Serializable targetId, Serializable versionSeriesId, Node parent, String name, Long pos);

    /**
     * Sets a proxies' target.
     *
     * @param proxy the proxy
     * @param targetId the new target id
     * @since 5.5
     */
    void setProxyTarget(Node proxy, Serializable targetId);

    /**
     * Removes a node from the storage.
     * <p>
     * This is much more complex that removing a property node ( {@link #removePropertyNode}).
     *
     * @param node the node to remove
     * @see {@link #removePropertyNode}
     */
    void removeNode(Node node);

    /**
     * Removes a property node from the storage.
     * <p>
     * This is much less complex that removing a generic document node ( {@link #removeNode}).
     *
     * @param node the property node to remove
     * @see {@link #removeNode}
     */
    void removePropertyNode(Node node);

    /**
     * Order the given source child node before the destination child node. The source node will be placed before the
     * destination one. If destination is {@code null}, the source node will be appended at the end of the children
     * list.
     *
     * @param parent the parent node
     * @param source the child node to move
     * @param dest the child node before which to place the source node, or {@code null} to move at the end
     */
    void orderBefore(Node parent, Node source, Node dest);

    /**
     * Moves a node to a new location with a new name.
     * <p>
     * A {@link #save} is automatically done first.
     *
     * @param source the node to move
     * @param parent the new parent to which the node is moved
     * @param name the new node name
     * @return the moved node
     */
    Node move(Node source, Node parent, String name);

    /**
     * Copies a node to a new location with a new name.
     * <p>
     * A {@link #save} is automatically done first.
     *
     * @param source the node to copy
     * @param parent the new parent to which the node is copied
     * @param name the new node name
     * @return the copied node
     */
    Node copy(Node source, Node parent, String name);

    /**
     * Checks in a checked-out node: creates a new version with a copy of its information.
     * <p>
     * A {@link #save} is automatically done first.
     *
     * @param node the node to check in
     * @param label the label for the version
     * @param checkinComment the description for the version
     * @return the created version
     */
    Node checkIn(Node node, String label, String checkinComment);

    /**
     * Checks out a checked-in node.
     *
     * @param node the node to check out
     */
    void checkOut(Node node);

    /**
     * Restores a node to a given version.
     * <p>
     * The restored node is checked in.
     *
     * @param node the node to restore
     * @param version the version to restore from
     */
    void restore(Node node, Node version);

    /**
     * Gets a version given its version series id and label.
     *
     * @param versionSeriesId the version series id
     * @param label the label
     * @return the version node, or {@code null} if not found
     */
    Node getVersionByLabel(Serializable versionSeriesId, String label);

    /**
     * Gets all the versions for a given version series id.
     * <p>
     * A {@link #save} is automatically done first.
     *
     * @param versionSeriesId the version series id
     * @return the list of versions
     */
    List<Node> getVersions(Serializable versionSeriesId);

    /**
     * Gets the last version for a given version series id.
     * <p>
     * A {@link #save} is automatically done first.
     *
     * @param versionSeriesId the version series id
     * @return the last version, or {@code null} if no versions exist
     */
    Node getLastVersion(Serializable versionSeriesId);

    /**
     * Finds the proxies for a document. If the parent is not null, the search will be limited to its direct children.
     * <p>
     * If the document is a version, then only proxies to that version will be looked up.
     * <p>
     * Otherwise all proxies to the same version series than the document are retrieved.
     * <p>
     * A {@link #save} is automatically done first.
     *
     * @param document the document
     * @param parent the parent, or {@code null}
     * @return the list of proxies
     */
    List<Node> getProxies(Node document, Node parent);

    /**
     * Finds the proxies for a document. (The document may be a version or a live document)
     * @param document the document
     * @return the list of proxies,  or an empty list
     * @since 10.2
     */
    List<Node> getProxies(Node document);

    /**
     * Makes a NXQL query to the database.
     *
     * @param query the query
     * @param queryFilter the query filter
     * @param countTotal if {@code true}, also count the total size without offset/limit
     * @return the resulting list with total size included
     */
    PartialList<Serializable> query(String query, QueryFilter queryFilter, boolean countTotal);

    /**
     * Makes a query to the database.
     *
     * @param query the query
     * @param queryType the query type
     * @param queryFilter the query filter
     * @param countUpTo if {@code -1}, also count the total size without offset/limit.<br>
     *            If {@code 0}, don't count the total size.<br>
     *            If {@code n}, count the total number if there are less than n documents otherwise set the size to
     *            {@code -1}.
     * @return the resulting list with total size included
     * @since 5.6
     */
    PartialList<Serializable> query(String query, String queryType, QueryFilter queryFilter, long countUpTo);

    /**
     * Makes a query to the database and returns an iterable (which must be closed when done).
     *
     * @param query the query
     * @param queryType the query type
     * @param queryFilter the query filter
     * @param params optional query-type-dependent parameters
     * @return an iterable, which <b>must</b> be closed when done
     */
    IterableQueryResult queryAndFetch(String query, String queryType, QueryFilter queryFilter, Object... params);

    /**
     * Makes a query to the database and returns an iterable (which must be closed when done).
     *
     * @param query the query
     * @param queryType the query type
     * @param queryFilter the query filter
     * @param distinctDocuments if {@code true} then a maximum of one row per document will be returned
     * @param params optional query-type-dependent parameters
     * @return an iterable, which <b>must</b> be closed when done
     * @since 7.10-HF04, 8.2
     */
    IterableQueryResult queryAndFetch(String query, String queryType, QueryFilter queryFilter,
            boolean distinctDocuments, Object... params);

    /**
     * Makes a query to the database.
     *
     * @param query the query
     * @param queryType the query type
     * @param queryFilter the query filter
     * @param distinctDocuments if {@code true} then a maximum of one row per document will be returned
     * @param countUpTo if {@code -1}, also count the total size without offset/limit.<br>
     *            If {@code 0}, don't count the total size.<br>
     *            If {@code n}, count the total number if there are less than n documents otherwise set the size to
     *            {@code -1}.
     * @param params optional query-type-dependent parameters
     * @return a projection
     * @since 7.10-HF-25, 8.10-HF06, 9.2
     */
    PartialList<Map<String,Serializable>> queryProjection(String query, String queryType, QueryFilter queryFilter, boolean distinctDocuments,
            long countUpTo, Object[] params);

    /**
     * Gets the lock manager for this session.
     *
     * @return the lock manager
     * @since 7.4
     */
    LockManager getLockManager();

    /**
     * Read ACLs are optimized ACLs for the read permission, they need to be updated after document creation or ACL
     * change.
     * <p>
     * This method flag the current session, the read ACLs update will be done automatically at save time.
     */
    void requireReadAclsUpdate();

    /**
     * Update only the read ACLs that have changed.
     */
    void updateReadAcls();

    /**
     * Rebuild the read ACLs for the whole repository.
     */
    void rebuildReadAcls();

    /**
     * Gets the fulltext extracted from the binary fields.
     *
     * @since 5.9.3
     */
    Map<String, String> getBinaryFulltext(Serializable id);

    /**
     * Checks if change token management is enabled.
     *
     * @since 9.1
     */
    boolean isChangeTokenEnabled();

    /**
     * Marks the document as being modified by a user change.
     * <p>
     * This causes an additional change token increment and check during save.
     *
     * @param id the document id
     * @since 9.2
     */
    void markUserChange(Serializable id);

}
