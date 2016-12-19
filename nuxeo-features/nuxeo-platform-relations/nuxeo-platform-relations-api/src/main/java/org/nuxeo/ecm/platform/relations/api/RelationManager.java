/*
 * (C) Copyright 2006-2016 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Anahide Tchertchian
 *     Florent Guillaume
 */
package org.nuxeo.ecm.platform.relations.api;

import java.io.Serializable;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.nuxeo.ecm.core.api.CoreSession;

/**
 * RelationService common interface.
 */
public interface RelationManager extends Serializable {

    /**
     * Gets a registered graph by name.
     * <p>
     * A {@link CoreSession} should be passed to provide a context in which to store relations when using a "core"
     * graph.
     *
     * @param name string name of the graph used at registration
     * @param session the core session
     * @return the graph
     * @throws RuntimeException if the graph is not found
     * @since 5.5
     */
    Graph getGraph(String name, CoreSession session);

    /**
     * Gets a registered graph by name.
     *
     * @param name string name of the graph used at registration
     * @return the graph
     * @throws RuntimeException if the graph is not found
     */
    Graph getGraphByName(String name);

    /**
     * Gets a transient graph.
     *
     * @param type The graph type.
     * @return the graph.
     */
    Graph getTransientGraph(String type);

    /**
     * Gets a resource given a namespace and a serializable object.
     * <p>
     * There can be several resources with different namespaces associated to an incoming object. A document can for
     * instance be used to refer to itself as a precise version as well as to the set of all versions.
     * <p>
     * Context can hold any object useful for the adapters, like a {@link CoreSession}.
     *
     * @since 5.2-M1
     */
    Resource getResource(String namespace, Serializable object, Map<String, Object> context);

    /**
     * Computes all resources corresponding to the given object.
     * <p>
     * Context can hold any object useful for the adapters, like a {@link CoreSession}.
     *
     * @since 5.2-M1
     * @return the resources as a set
     */
    Set<Resource> getAllResources(Serializable object, Map<String, Object> context);

    /**
     * Gets an object representing this resource given a namespace.
     * <p>
     * Context can hold any object useful for the adapters, like a {@link CoreSession}.
     *
     * @since 5.2-M1
     */
    Serializable getResourceRepresentation(String namespace, Resource resource, Map<String, Object> context);

    /**
     * Gets the list containing the graph names.
     *
     * @since 5.2-GA
     */
    List<String> getGraphNames();

}
