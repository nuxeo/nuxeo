/*
 * (C) Copyright 2006-2014 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Anahide Tchertchian
 *     Florent Guillaume
 *     Remi Cattiau
 */
package org.nuxeo.ecm.platform.relations.services;

import java.io.InputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.platform.relations.api.DocumentRelationManager;
import org.nuxeo.ecm.platform.relations.api.Graph;
import org.nuxeo.ecm.platform.relations.api.GraphDescription;
import org.nuxeo.ecm.platform.relations.api.GraphFactory;
import org.nuxeo.ecm.platform.relations.api.Node;
import org.nuxeo.ecm.platform.relations.api.QueryResult;
import org.nuxeo.ecm.platform.relations.api.RelationManager;
import org.nuxeo.ecm.platform.relations.api.Resource;
import org.nuxeo.ecm.platform.relations.api.ResourceAdapter;
import org.nuxeo.ecm.platform.relations.api.Statement;
import org.nuxeo.ecm.platform.relations.descriptors.GraphTypeDescriptor;
import org.nuxeo.ecm.platform.relations.descriptors.ResourceAdapterDescriptor;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.datasource.DataSourceComponent;
import org.nuxeo.runtime.model.ComponentContext;
import org.nuxeo.runtime.model.ComponentInstance;
import org.nuxeo.runtime.model.ComponentName;
import org.nuxeo.runtime.model.DefaultComponent;
import org.nuxeo.runtime.transaction.TransactionHelper;

/**
 * Relation service.
 * <p>
 * It handles a registry of graph instances through extension points.
 */
public class RelationService extends DefaultComponent implements
        RelationManager {

    public static final ComponentName NAME = new ComponentName(
            "org.nuxeo.ecm.platform.relations.services.RelationService");

    private static final long serialVersionUID = -4778456059717447736L;

    private static final Log log = LogFactory.getLog(RelationService.class);

    /** Graph type -> class. */
    protected final Map<String, Class<?>> graphTypes;

    /** Graph name -> description */
    protected final Map<String, GraphDescription> graphDescriptions;

    /** Graph name -> factory. */
    public final Map<String, GraphFactory> graphFactories;

    /** Graph name -> graph instance. */
    public final Map<String, Graph> graphRegistry;

    protected final Map<String, String> resourceAdapterRegistry;

    public RelationService() {
        // Hashtable to get implicit synchronization
        graphTypes = new Hashtable<String, Class<?>>();
        graphDescriptions = new Hashtable<String, GraphDescription>();
        graphRegistry = new Hashtable<String, Graph>();
        graphFactories = new Hashtable<String, GraphFactory>();
        resourceAdapterRegistry = new Hashtable<String, String>();
    }

    @Override
    public void registerContribution(Object contribution,
            String extensionPoint, ComponentInstance contributor) {
        if (extensionPoint.equals("graphtypes")) {
            registerGraphType(contribution);
        } else if (extensionPoint.equals("graphs")) {
            registerGraph(contribution);
        } else if (extensionPoint.equals("resourceadapters")) {
            registerResourceAdapter(contribution);
        } else {
            log.error(String.format(
                    "Unknown extension point %s, can't register !",
                    extensionPoint));
        }
    }

    @Override
    public void unregisterContribution(Object contribution,
            String extensionPoint, ComponentInstance contributor) {
        if (extensionPoint.equals("graphtypes")) {
            unregisterGraphType(contribution);
        } else if (extensionPoint.equals("graphs")) {
            unregisterGraph(contribution);
        } else if (extensionPoint.equals("resourceadapters")) {
            unregisterResourceAdapter(contribution);
        } else {
            log.error(String.format(
                    "Unknown extension point %s, can't unregister !",
                    extensionPoint));
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
    private void registerGraphType(Object contribution) {
        GraphTypeDescriptor graphTypeDescriptor = (GraphTypeDescriptor) contribution;
        String graphType = graphTypeDescriptor.getName();
        String className = graphTypeDescriptor.getClassName();

        if (graphTypes.containsKey(graphType)) {
            log.error(String.format(
                    "Graph type %s already registered using %s", graphType,
                    graphTypes.get(graphType)));
            return;
        }
        Class<?> klass;
        try {
            klass = getClass().getClassLoader().loadClass(className);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(String.format(
                    "Cannot register unknown class for graph type %s: %s",
                    graphType, className), e);
        }
        if (!Graph.class.isAssignableFrom(klass)
                && !GraphFactory.class.isAssignableFrom(klass)) {
            throw new RuntimeException("Invalid graph class/factory type: "
                    + className);
        }
        graphTypes.put(graphType, klass);
        log.info(String.format("Registered graph type: %s (%s)", graphType,
                className));
    }

    /**
     * Unregisters a graph type.
     */
    private void unregisterGraphType(Object contrib) {
        GraphTypeDescriptor graphTypeExtension = (GraphTypeDescriptor) contrib;
        String graphType = graphTypeExtension.getName();
        List<GraphDescription> list = new ArrayList<GraphDescription>(
                graphDescriptions.values()); // copy
        for (GraphDescription graphDescription : list) {
            if (graphType.equals(graphDescription.getGraphType())) {
                String name = graphDescription.getName();
                graphFactories.remove(name);
                graphRegistry.remove(name);
                graphDescriptions.remove(name);
                log.info("Unregistered graph: " + name);
            }
        }
        graphTypes.remove(graphType);
        log.info("Unregistered graph type: " + graphType);
    }

    public List<String> getGraphTypes() {
        List<String> res = new ArrayList<String>();
        for (String type : graphTypes.keySet()) {
            res.add(type);
        }
        return res;
    }

    /**
     * Registers a graph instance.
     * <p>
     * The graph has to be declared as using a type already registered in the
     * graph type registry.
     */
    protected void registerGraph(Object contribution) {
        GraphDescription graphDescription = (GraphDescription) contribution;
        String name = graphDescription.getName();
        if (graphDescriptions.containsKey(name)) {
            log.info(String.format("Overriding graph %s definition", name));
            graphDescriptions.remove(name);
        }
        graphDescriptions.put(name, graphDescription);
        log.info("Registered graph: " + name);

        // remove any existing graph instance in case its definition changed
        graphRegistry.remove(name);
    }

    /**
     * Unregisters a graph.
     */
    protected void unregisterGraph(Object contribution) {
        GraphDescription graphDescription = (GraphDescription) contribution;
        String name = graphDescription.getName();
        if (graphDescriptions.containsKey(name)) {
            graphFactories.remove(name);
            graphRegistry.remove(name);
            graphDescriptions.remove(name);
            log.info("Unregistered graph: " + name);
        }
    }

    // Resource adapters

    private void registerResourceAdapter(Object contribution) {
        ResourceAdapterDescriptor adapter = (ResourceAdapterDescriptor) contribution;
        String ns = adapter.getNamespace();
        String adapterClassName = adapter.getClassName();
        if (resourceAdapterRegistry.containsKey(ns)) {
            log.info("Overriding resource adapter config for namespace " + ns);
        }
        resourceAdapterRegistry.put(ns, adapterClassName);
        log.info(String.format("%s namespace registered using adapter %s", ns,
                adapterClassName));
    }

    private void unregisterResourceAdapter(Object contribution) {
        ResourceAdapterDescriptor adapter = (ResourceAdapterDescriptor) contribution;
        String ns = adapter.getNamespace();
        String adapterClassName = adapter.getClassName();
        String registered = resourceAdapterRegistry.get(ns);
        if (registered == null) {
            log.error(String.format("Namespace %s not found", ns));
        } else if (!registered.equals(adapterClassName)) {
            log.error(String.format("Namespace %s: wrong class %s", ns,
                    registered));
        } else {
            resourceAdapterRegistry.remove(ns);
            log.info(String.format("%s unregistered, was using %s", ns,
                    adapterClassName));
        }
    }

    private ResourceAdapter getResourceAdapterForNamespace(String namespace) {
        String adapterClassName = resourceAdapterRegistry.get(namespace);
        if (adapterClassName == null) {
            log.error(String.format("Cannot find adapter for namespace: %s",
                    namespace));
            return null;
        } else {
            try {
                // Thread context loader is not working in isolated EARs
                ResourceAdapter adapter = (ResourceAdapter) RelationService.class
                    .getClassLoader().loadClass(adapterClassName).newInstance();
                adapter.setNamespace(namespace);
                return adapter;
            } catch (Exception e) {
                String msg = String.format(
                        "Cannot instantiate generator with namespace '%s': %s",
                        namespace, e);
                log.error(msg);
                return null;
            }
        }
    }

    // RelationManager interface

    @Override
    public Graph getGraphByName(String name) throws ClientException {
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
                graph = (Graph) klass.newInstance();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
            graphRegistry.put(name, graph);
        } else { // GraphFactory.class.isAssignableFrom(klass)
            // factory
            GraphFactory factory;
            try {
                factory = (GraphFactory) klass.newInstance();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
            graphFactories.put(name, factory);
        }

        return getGraphFromRegistries(graphDescription, session);
    }

    /** Gets the graph from the registries. */
    protected Graph getGraphFromRegistries(GraphDescription graphDescription,
            CoreSession session) {
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
            return (Graph) klass.newInstance();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public Graph getTransientGraph(String type) throws ClientException {
        Class<?> klass = graphTypes.get(type);
        if (Graph.class.isAssignableFrom(klass)) {
            try {
                return (Graph) klass.newInstance();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
        throw new RuntimeException("Graph type cannot be transient: " + type);
    }

    @Override
    public Resource getResource(String namespace, Serializable object,
            Map<String, Object> context) throws ClientException {
        ResourceAdapter adapter = getResourceAdapterForNamespace(namespace);
        if (adapter == null) {
            log.error("Cannot find adapter for namespace: " + namespace);
            return null;
        } else {
            return adapter.getResource(object, context);
        }
    }

    @Override
    public Set<Resource> getAllResources(Serializable object,
            Map<String, Object> context) throws ClientException {
        // TODO OPTIM implement reverse map in registerContribution
        Set<Resource> res = new HashSet<Resource>();
        for (String ns : resourceAdapterRegistry.keySet()) {
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
    public Serializable getResourceRepresentation(String namespace,
            Resource resource, Map<String, Object> context)
            throws ClientException {
        ResourceAdapter adapter = getResourceAdapterForNamespace(namespace);
        if (adapter == null) {
            log.error("Cannot find adapter for namespace: " + namespace);
            return null;
        } else {
            return adapter.getResourceRepresentation(resource, context);
        }
    }

    @Override
    @Deprecated
    public void add(String graphName, List<Statement> statements)
            throws ClientException {
        getGraphByName(graphName).add(statements);
    }

    @Override
    @Deprecated
    public void clear(String graphName) throws ClientException {
        getGraphByName(graphName).clear();
    }

    @Override
    @Deprecated
    public List<Node> getObjects(String graphName, Node subject, Node predicate)
            throws ClientException {
        return getGraphByName(graphName).getObjects(subject, predicate);
    }

    @Override
    @Deprecated
    public List<Node> getPredicates(String graphName, Node subject, Node object)
            throws ClientException {
        return getGraphByName(graphName).getPredicates(subject, object);
    }

    @Override
    @Deprecated
    public List<Statement> getStatements(String graphName, Statement statement)
            throws ClientException {
        return getGraphByName(graphName).getStatements(statement);
    }

    @Override
    @Deprecated
    public List<Statement> getStatements(String graphName)
            throws ClientException {
        return getGraphByName(graphName).getStatements();
    }

    @Override
    @Deprecated
    public List<Node> getSubjects(String graphName, Node predicate, Node object)
            throws ClientException {
        return getGraphByName(graphName).getSubjects(predicate, object);
    }

    @Override
    @Deprecated
    public boolean hasResource(String graphName, Resource resource)
            throws ClientException {
        return getGraphByName(graphName).hasResource(resource);
    }

    @Override
    @Deprecated
    public boolean hasStatement(String graphName, Statement statement)
            throws ClientException {
        return getGraphByName(graphName).hasStatement(statement);
    }

    @Override
    @Deprecated
    public QueryResult query(String graphName, String queryString,
            String language, String baseURI) throws ClientException {
        return getGraphByName(graphName).query(queryString, language, baseURI);
    }

    @Override
    @Deprecated
    public boolean read(String graphName, InputStream in, String lang,
            String base) throws ClientException {
        return getGraphByName(graphName).read(in, lang, base);
    }

    @Override
    @Deprecated
    public void remove(String graphName, List<Statement> statements)
            throws ClientException {
        getGraphByName(graphName).remove(statements);
    }

    @Override
    @Deprecated
    public Long size(String graphName) throws ClientException {
        return getGraphByName(graphName).size();
    }

    @Override
    @Deprecated
    public boolean write(String graphName, OutputStream out, String lang,
            String base) throws ClientException {
        return getGraphByName(graphName).write(out, lang, base);
    }

    @Override
    public List<String> getGraphNames() throws ClientException {
        return new ArrayList<String>(graphDescriptions.keySet());
    }

    @Override
    public int getApplicationStartedOrder() {
        final DataSourceComponent ds = (DataSourceComponent) Framework.getRuntime().getComponent(
                "org.nuxeo.runtime.datasource");
        if (ds != null) {
            return ds.getApplicationStartedOrder() + 1;
        }
        return super.getApplicationStartedOrder();
    }

    @Override
    public void applicationStarted(ComponentContext context) throws Exception {
        Thread t = new Thread("relation-service-init") {
            @Override
            public void run() {
                Thread.currentThread().setContextClassLoader(
                        RelationService.class.getClassLoader());
                log.info("Relation Service initialization");

                // init jena Graph outside of Tx
                for (String graphName : graphDescriptions.keySet()) {
                    GraphDescription desc = graphDescriptions.get(graphName);
                    if (desc.getGraphType().equalsIgnoreCase("jena")) {
                        log.info("create RDF Graph " + graphName);
                        try {
                            Graph graph = getGraphByName(graphName);
                            graph.size();
                        } catch (ClientException e) {
                            log.error("Error while initializing graph "
                                    + graphName, e);
                        }
                    }
                }

                // init non jena Graph inside a Tx
                TransactionHelper.startTransaction();
                try {
                    for (String graphName : graphDescriptions.keySet()) {
                        GraphDescription desc = graphDescriptions
                            .get(graphName);
                        if (!desc.getGraphType().equalsIgnoreCase("jena")) {
                            log.info("create RDF Graph " + graphName);
                            try {
                                Graph graph = getGraphByName(graphName);
                                graph.size();
                            } catch (ClientException e) {
                                log.error("Error while initializing graph "
                                        + graphName, e);
                                TransactionHelper.setTransactionRollbackOnly();
                            }
                        }
                    }
                } finally {
                    TransactionHelper.commitOrRollbackTransaction();
                }
            }
        };
        t.start();
        try {
            t.join();
        } catch (InterruptedException e) {
            log.error("Cannot join init thread", e);
        }
    }

}
