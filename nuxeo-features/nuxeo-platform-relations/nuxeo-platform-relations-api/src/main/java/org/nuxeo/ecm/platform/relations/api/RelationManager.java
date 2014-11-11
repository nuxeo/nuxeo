/*
 * (C) Copyright 2006-2007 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     Nuxeo - initial API and implementation
 *
 * $Id: RelationManager.java 22121 2007-07-06 16:33:15Z gracinet $
 */

package org.nuxeo.ecm.platform.relations.api;

import java.io.InputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;

/**
 * RelationService common interface.
 *
 * @author <a href="mailto:at@nuxeo.com">Anahide Tchertchian</a>
 *
 */
public interface RelationManager extends Serializable {

    /**
     * Gets a registered graph by name.
     *
     * @param name string name of the graph used at registration
     * @return the graph
     * @throws RuntimeException if the graph is not found
     */
    Graph getGraphByName(String name) throws ClientException;

    /**
     * Gets a transient graph.
     *
     * @param type The graph type.
     * @return the graph.
     * @throws ClientException
     */
    Graph getTransientGraph(String type) throws ClientException;

    /**
     * Gets a resource given a namespace and a serializable object.
     * <p>
     * There can be several resources with different namespaces associated to an
     * incoming object. A document can for instance be used to refer to itself
     * as a precise version as well as to the set of all versions.
     * <p>
     * Context can hold any object useful for the adapters, like a
     * {@link CoreSession}.
     *
     * @since 5.2-M1
     * @throws ClientException
     */
    Resource getResource(String namespace, Serializable object,
            Map<String, Serializable> context) throws ClientException;

    /**
     * Computes all resources corresponding to the given object.
     * <p>
     * Context can hold any object useful for the adapters, like a
     * {@link CoreSession}.
     *
     * @since 5.2-M1
     * @return the resources as a set
     * @throws ClientException
     */
    Set<Resource> getAllResources(Serializable object,
            Map<String, Serializable> context) throws ClientException;

    /**
     * Gets an object representing this resource given a namespace.
     * <p>
     * Context can hold any object useful for the adapters, like a
     * {@link CoreSession}.
     *
     * @since 5.2-M1
     * @throws ClientException
     */
    Serializable getResourceRepresentation(String namespace, Resource resource,
            Map<String, Serializable> context) throws ClientException;

    /**
     * Gets the list containing the graph names.
     *
     * @since 5.2-GA
     */
    List<String> getGraphNames() throws ClientException;

    /**
     * @see org.nuxeo.ecm.platform.relations.api.Graph#add
     */
    void add(String graphName, List<Statement> statements)
            throws ClientException;

    /**
     * @see org.nuxeo.ecm.platform.relations.api.Graph#remove
     */
    void remove(String graphName, List<Statement> statements)
            throws ClientException;

    /**
     * @see org.nuxeo.ecm.platform.relations.api.Graph#getStatements
     */
    List<Statement> getStatements(String graphName) throws ClientException;

    /**
     * @see org.nuxeo.ecm.platform.relations.api.Graph#getStatements
     */
    List<Statement> getStatements(String graphName, Statement statement)
            throws ClientException;

    /**
     * @see org.nuxeo.ecm.platform.relations.api.Graph#getSubjects
     */
    List<Node> getSubjects(String graphName, Node predicate, Node object)
            throws ClientException;

    /**
     * @see org.nuxeo.ecm.platform.relations.api.Graph#getPredicates
     */
    List<Node> getPredicates(String graphName, Node subject, Node object)
            throws ClientException;

    /**
     * @see org.nuxeo.ecm.platform.relations.api.Graph#getObjects
     */
    List<Node> getObjects(String graphName, Node subject, Node predicate)
            throws ClientException;

    /**
     * @see org.nuxeo.ecm.platform.relations.api.Graph#hasStatement
     */
    boolean hasStatement(String graphName, Statement statement)
            throws ClientException;

    /**
     * @see org.nuxeo.ecm.platform.relations.api.Graph#hasResource
     */
    boolean hasResource(String graphName, Resource resource)
            throws ClientException;

    /**
     * @see org.nuxeo.ecm.platform.relations.api.Graph#size
     */
    Long size(String graphName) throws ClientException;

    /**
     * @see org.nuxeo.ecm.platform.relations.api.Graph#clear
     */
    void clear(String graphName) throws ClientException;

    /**
     * @see org.nuxeo.ecm.platform.relations.api.Graph#query
     */
    QueryResult query(String graphName, String queryString, String language,
            String baseURI) throws ClientException;

    /**
     * @see org.nuxeo.ecm.platform.relations.api.Graph#read
     */
    boolean read(String graphName, InputStream in, String lang, String base)
            throws ClientException;

    /**
     * @see org.nuxeo.ecm.platform.relations.api.Graph#write
     */
    boolean write(String graphName, OutputStream out, String lang, String base)
            throws ClientException;

}
