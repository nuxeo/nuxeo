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
import java.util.List;

import javax.resource.cci.Connection;

import org.nuxeo.ecm.core.storage.StorageException;

/**
 * The session is the main high level access point to data from the underlying
 * database.
 *
 * @author Florent Guillaume
 */
public interface Session extends Connection {

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
     *
     * @throws StorageException
     */
    void save() throws StorageException;

    /**
     * Gets the root node of the repository.
     *
     * @return the root node
     */
    Node getRootNode() throws StorageException;

    /**
     * Gets a node given its id.
     *
     * @param id the id
     * @return the node, or {@code null} if not found
     * @throws StorageException
     */
    Node getNodeById(Serializable id) throws StorageException;

    /**
     * Gets a node given its absolute path, or given an existing node and a
     * relative path.
     *
     * @param path the path
     * @param node the node (ignored for absolute paths)
     *
     * @return the node, or {@code null} if not found
     * @throws StorageException
     */
    Node getNodeByPath(String path, Node node) throws StorageException;

    /**
     * Gets the parent of a node.
     * <p>
     * The root has a {@code null} parent.
     *
     * @param node the node
     * @return the parent node, or {@code null} for the root's parent
     * @throws StorageException
     */
    Node getParentNode(Node node) throws StorageException;

    /**
     * Gets the absolute path of a node.
     *
     * @param node the node
     * @return the path
     * @throws StorageException
     */
    String getPath(Node node) throws StorageException;

    /**
     * Gets a child node given its parent and name.
     *
     * @param parent the parent node
     * @param name the child name
     * @return the child node, or {@code null} is not found
     * @throws StorageException
     */
    Node getChildNode(Node parent, String name) throws StorageException;

    /**
     * Gets the children of a node.
     *
     * @param parent the parent node
     * @return the collection of children
     * @throws StorageException
     */
    List<Node> getChildren(Node parent) throws StorageException;

    /**
     * Creates a new child node.
     *
     * @param parent the parent to which the child is added
     * @param name the child name
     * @param typeName the child type
     * @return the new node
     * @throws StorageException
     */
    Node addChildNode(Node parent, String name, String typeName)
            throws StorageException;

    /**
     * Removes a node from the storage.
     *
     * @param node the node to remove
     * @throws StorageException
     */
    void removeNode(Node node) throws StorageException;

}
