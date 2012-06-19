/*
 * (C) Copyright 2012 Nuxeo SA (http://nuxeo.com/) and contributors.
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
package org.nuxeo.ecm.platform.routing.core.impl;

import java.io.Serializable;
import java.util.Collection;
import java.util.Map;

import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.platform.routing.api.exception.DocumentRouteException;

/**
 * A route graph, defining a workflow with arbitrarily complex transitions
 * between nodes.
 *
 * @since 5.6
 */
public interface GraphRoute {

    String PROP_VARIABLES_FACET = "docri:variablesFacet";

    /**
     * Gets the start node for this graph.
     *
     * @return the start node
     */
    GraphNode getStartNode() throws DocumentRouteException;

    /**
     * Gets the attached documents.
     *
     * @return a list of document
     */
    DocumentModelList getAttachedDocumentModels();

    /**
     * Gets the graph variables.
     *
     * @return the map of variables
     */
    Map<String, Serializable> getVariables();

    /**
     * Sets the graph variables.
     *
     * @param map the map of variables
     */
    void setVariables(Map<String, Serializable> map);

    /**
     * Gets the node with the given id.
     *
     * @param id
     * @return the node
     * @throws IllegalArgumentException if there is no such node
     */
    GraphNode getNode(String id) throws IllegalArgumentException;

    /**
     * Gets a collection of the route nodes
     *
     * @return
     */
    Collection<GraphNode> getNodes();

}
