/*
 * (C) Copyright 2012 Nuxeo SA (http://nuxeo.com/) and others.
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
package org.nuxeo.ecm.platform.routing.core.impl;

import java.io.Serializable;
import java.util.Collection;
import java.util.List;
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
     * Sets the variables of the workflow based on their JSON representation (especially for scalar lists). For example:
     *
     * <pre>
     * Map&lt;String, String&gt; map = new HashMap&lt;String, String&gt;();
     * map.put("contributors","[\"John Doe\", \"John Smith\"]");
     * map.put("title","Test Title");
     * </pre>
     *
     * @param map the map of variables
     * @since 5.9.3, 5.8.0-HF10
     */
    void setJSONVariables(Map<String, String> map);

    /**
     * Gets the node with the given id.
     *
     * @return the node
     * @throws IllegalArgumentException if there is no such node
     */
    GraphNode getNode(String id) throws IllegalArgumentException;

    /**
     * Gets a collection of the route nodes
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

    /**
     * Get the list of nodes of which the State is suspended.
     *
     * @since 7.4
     */
    List<GraphNode> getSuspendedNodes();

}
