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

import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.platform.routing.api.DocumentRoute;
import org.nuxeo.ecm.platform.routing.api.exception.DocumentRouteException;

/**
 * A route graph, defining a workflow with arbitrarily complex transitions between nodes.
 *
 * @since 5.6
 */
public interface GraphRoute extends DocumentRoute {

    String PROP_VARIABLES_FACET = "docri:variablesFacet";

    String PROP_AVAILABILITY_FILTER = "docri:availabilityFilter";

    /**
     * The id of the parent route instance from which this route was started.
     *
     * @since 5.7.2
     */
    String PROP_PARENT_ROUTE = "docri:parentRouteInstanceId";

    /**
     * The id of the node in the parent route from which this route was started.
     *
     * @since 5.7.2
     */
    String PROP_PARENT_NODE = "docri:parentRouteNodeId";

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
     * Gets the Json formatted graph variables.
     *
     * @return the map of variables
     * @since 7.2
     */
    Map<String, Serializable> getJsonVariables();

    /**
     * Sets the graph variables.
     *
     * @param map the map of variables
     */
    void setVariables(Map<String, Serializable> map);

    /**
     * Sets the variables of the workflow based on their JSON representation (especially for scalar lists). Eg.
     * Map<String, String> map = new HashMap<String, String>();
     * map.put("contributors","[\"John Doe\", \"John Smith\"]"); map.put("title","Test Title");
     *
     * @param map the map of variables
     * @since 5.9.3, 5.8.0-HF10
     */
    void setJSONVariables(Map<String, String> map);

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

    /**
     * Returns the availability filter name for this graph.
     */
    String getAvailabilityFilter();

    /**
     * Checks if this graph instance has been started from another graph.
     *
     * @return {@code true} if this is a sub-route instance
     * @since 5.7.2
     */
    boolean hasParentRoute();

    /**
     * Resumes execution of the parent route from which this graph was started.
     *
     * @param session the session
     * @since 5.7.2
     */
    void resumeParentRoute(CoreSession session);

}
