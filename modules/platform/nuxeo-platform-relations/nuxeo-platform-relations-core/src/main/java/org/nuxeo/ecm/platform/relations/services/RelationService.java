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
 *     Anahide Tchertchian
 *     Florent Guillaume
 *     Remi Cattiau
 */
package org.nuxeo.ecm.platform.relations.services;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collector;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.nuxeo.common.xmap.registry.MapRegistry;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.repository.RepositoryService;
import org.nuxeo.ecm.platform.relations.api.DocumentRelationManager;
import org.nuxeo.ecm.platform.relations.api.Graph;
import org.nuxeo.ecm.platform.relations.api.GraphDescription;
import org.nuxeo.ecm.platform.relations.api.GraphFactory;
import org.nuxeo.ecm.platform.relations.api.RelationManager;
import org.nuxeo.ecm.platform.relations.api.Resource;
import org.nuxeo.ecm.platform.relations.api.ResourceAdapter;
import org.nuxeo.ecm.platform.relations.descriptors.GraphDescriptor;
import org.nuxeo.ecm.platform.relations.descriptors.GraphTypeDescriptor;
import org.nuxeo.ecm.platform.relations.descriptors.ResourceAdapterDescriptor;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.model.ComponentContext;
import org.nuxeo.runtime.model.ComponentName;
import org.nuxeo.runtime.model.DefaultComponent;
import org.nuxeo.runtime.transaction.TransactionHelper;

/**
 * Relation service.
 * <p>
 * It handles a registry of graph instances through extension points.
 */
public class RelationService extends DefaultComponent implements RelationManager {

    private static final Logger log = LogManager.getLogger(RelationService.class);

    public static final ComponentName NAME = new ComponentName(
            "org.nuxeo.ecm.platform.relations.services.RelationService");

    protected static final String TYPES_EP = "graphtypes";

    protected static final String GRAPHS_EP = "graphs";

    protected static final String ADAPTERS_EP = "resourceadapters";

    protected static final Collector<GraphDescription, ?, Map<String, GraphDescription>> COLLECTOR_TO_NAME_DESC_MAP = Collectors.toMap(
            GraphDescription::getName, Function.identity());

    /** Graph type -&gt; class. */
    protected Map<String, Class<?>> graphTypes;

    /** Graph name -&gt; description */
    protected Map<String, GraphDescription> graphDescriptions;

    /** Graph name -&gt; factory. */
    protected Map<String, GraphFactory> graphFactories;

    /** Graph name -&gt; graph instance. */
    protected Map<String, Graph> graphRegistry;

    @Override
    public void start(ComponentContext context) {
        graphTypes = new HashMap<>();
        graphDescriptions = this.<GraphDescriptor> getRegistryContributions(GRAPHS_EP)
                                .stream()
                                .collect(COLLECTOR_TO_NAME_DESC_MAP);
        this.<GraphTypeDescriptor> getRegistryContributions(TYPES_EP).forEach(this::registerGraphType);
        // Hashtable to get implicit synchronization
        graphRegistry = new Hashtable<>();
        graphFactories = new Hashtable<>();
        initGraphs();
    }

    @Override
    public void stop(ComponentContext context) throws InterruptedException {
        graphTypes = null;
        graphDescriptions = null;
        graphRegistry = null;
        graphFactories = null;
    }

    protected void initGraphs() {
        RepositoryService repositoryService = Framework.getService(RepositoryService.class);
        if (repositoryService == null) {
            // RepositoryService failed to start, no need to go further
            return;
        }
        log.info("Relation Service initialization");
        for (GraphDescription desc : graphDescriptions.values()) {
            String graphName = desc.getName();
            log.info("Create Graph {}", graphName);
            if (desc.getGraphType().equalsIgnoreCase("jena")) {
                // init jena Graph outside of Tx
                TransactionHelper.runWithoutTransaction(() -> {
                    Graph graph = getGraphByName(graphName);
                    graph.size();
                });
            } else {
                TransactionHelper.runInTransaction(() -> {
                    Graph graph = getGraphByName(graphName);
                    graph.size();
                });
            }
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getAdapter(Class<T> adapter) {
        if (adapter.isAssignableFrom(RelationManager.class)) {
            return (T) this;
        } else if (adapter.isAssignableFrom(DocumentRelationManager.class)) {
            return (T) new DocumentRelationService();
        }
        return null;
    }

    // Graph types

    /**
     * Registers a graph type, saving a name and a class name.
     * <p>
     * The name will be used when registering graphs.
     */
    private void registerGraphType(GraphTypeDescriptor graphTypeDescriptor) {
        String graphType = graphTypeDescriptor.getName();
        String className = graphTypeDescriptor.getClassName();
        Class<?> klass;
        try {
            klass = getClass().getClassLoader().loadClass(className);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(
                    String.format("Cannot register unknown class for graph type %s: %s", graphType, className), e);
        }
        if (!Graph.class.isAssignableFrom(klass) && !GraphFactory.class.isAssignableFrom(klass)) {
            throw new RuntimeException("Invalid graph class/factory type: " + className);
        }
        graphTypes.put(graphType, klass);
        log.info(String.format("Registered graph type: %s (%s)", graphType, className));
    }

    public List<String> getGraphTypes() {
        return new ArrayList<>(graphTypes.keySet());
    }

    // Resource adapters

    private ResourceAdapter getResourceAdapterForNamespace(String namespace) {
        return this.<ResourceAdapterDescriptor> getRegistryContribution(ADAPTERS_EP, namespace).map(desc -> {
            try {
                ResourceAdapter adapter = desc.getAdapterClass().getDeclaredConstructor().newInstance();
                adapter.setNamespace(namespace);
                return adapter;
            } catch (ReflectiveOperationException e) {
                log.error("Cannot instantiate generator with namespace '%s': %s", namespace, e);
                return null;
            }
        }).orElse(null);
    }

    // RelationManager interface

    @Override
    public Graph getGraphByName(String name) {
        return getGraph(name, null);
    }

    @Override
    public Graph getGraph(String name, CoreSession session) {
        GraphDescription graphDescription = graphDescriptions.get(name);
        if (graphDescription == null) {
            throw new RuntimeException("No such graph: " + name);
        }

        Graph graph = getGraphFromRegistries(graphDescription, session);
        if (graph != null) {
            return graph;
        }

        // check what we have for the graph type
        Class<?> klass = graphTypes.get(graphDescription.getGraphType());
        if (Graph.class.isAssignableFrom(klass)) {
            // instance
            try {
                graph = (Graph) klass.getDeclaredConstructor().newInstance();
            } catch (ReflectiveOperationException e) {
                throw new RuntimeException(e);
            }
            graphRegistry.put(name, graph);
        } else { // GraphFactory.class.isAssignableFrom(klass)
            // factory
            GraphFactory factory;
            try {
                factory = (GraphFactory) klass.getDeclaredConstructor().newInstance();
            } catch (ReflectiveOperationException e) {
                throw new RuntimeException(e);
            }
            graphFactories.put(name, factory);
        }

        return getGraphFromRegistries(graphDescription, session);
    }

    /** Gets the graph from the registries. */
    protected Graph getGraphFromRegistries(GraphDescription graphDescription, CoreSession session) {
        String name = graphDescription.getName();
        // check instances
        Graph graph = graphRegistry.get(name);
        if (graph != null) {
            graph.setDescription(graphDescription);
            return graph;
        }

        // check factories
        GraphFactory factory = graphFactories.get(name);
        if (factory != null) {
            return factory.createGraph(graphDescription, session);
        }

        return null;
    }

    protected Graph newGraph(String className) {
        try {
            Class<?> klass = getClass().getClassLoader().loadClass(className);
            return (Graph) klass.getDeclaredConstructor().newInstance();
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public Graph getTransientGraph(String type) {
        Class<?> klass = graphTypes.get(type);
        if (Graph.class.isAssignableFrom(klass)) {
            try {
                return (Graph) klass.getDeclaredConstructor().newInstance();
            } catch (ReflectiveOperationException e) {
                throw new RuntimeException(e);
            }
        }
        throw new RuntimeException("Graph type cannot be transient: " + type);
    }

    @Override
    public Resource getResource(String namespace, Serializable object, Map<String, Object> context) {
        ResourceAdapter adapter = getResourceAdapterForNamespace(namespace);
        if (adapter == null) {
            return null;
        } else {
            return adapter.getResource(object, context);
        }
    }

    @Override
    public Set<Resource> getAllResources(Serializable object, Map<String, Object> context) {
        Set<Resource> res = new HashSet<>();
        Set<String> namespaces = this.<MapRegistry> getExtensionPointRegistry(ADAPTERS_EP).getContributions().keySet();
        for (String ns : namespaces) {
            ResourceAdapter adapter = getResourceAdapterForNamespace(ns);
            if (adapter == null) {
                continue;
            }
            Class<?> klass = adapter.getKlass();
            if (klass == null) {
                continue;
            }
            if (klass.isAssignableFrom(object.getClass())) {
                res.add(adapter.getResource(object, context));
            }
        }
        return res;
    }

    @Override
    public Serializable getResourceRepresentation(String namespace, Resource resource, Map<String, Object> context) {
        ResourceAdapter adapter = getResourceAdapterForNamespace(namespace);
        if (adapter == null) {
            return null;
        } else {
            return adapter.getResourceRepresentation(resource, context);
        }
    }

    @Override
    public List<String> getGraphNames() {
        return new ArrayList<>(graphDescriptions.keySet());
    }

}
