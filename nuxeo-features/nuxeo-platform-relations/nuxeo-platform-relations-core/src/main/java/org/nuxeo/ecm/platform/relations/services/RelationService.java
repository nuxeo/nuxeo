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
 * $Id: RelationService.java 25624 2007-10-02 15:14:38Z atchertchian $
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
import org.nuxeo.ecm.platform.relations.api.Graph;
import org.nuxeo.ecm.platform.relations.api.GraphDescription;
import org.nuxeo.ecm.platform.relations.api.Node;
import org.nuxeo.ecm.platform.relations.api.QueryResult;
import org.nuxeo.ecm.platform.relations.api.RelationManager;
import org.nuxeo.ecm.platform.relations.api.Resource;
import org.nuxeo.ecm.platform.relations.api.ResourceAdapter;
import org.nuxeo.ecm.platform.relations.api.Statement;
import org.nuxeo.ecm.platform.relations.descriptors.GraphDescriptor;
import org.nuxeo.ecm.platform.relations.descriptors.GraphTypeDescriptor;
import org.nuxeo.ecm.platform.relations.descriptors.ResourceAdapterDescriptor;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.model.ComponentContext;
import org.nuxeo.runtime.model.ComponentInstance;
import org.nuxeo.runtime.model.ComponentName;
import org.nuxeo.runtime.model.DefaultComponent;

/**
 * Relation service.
 * <p>
 * It handles a registry of graph instances through extension points.
 * 
 * @author <a href="mailto:at@nuxeo.com">Anahide Tchertchian</a>
 * 
 */
public class RelationService extends DefaultComponent implements
        RelationManager {

    public static final ComponentName NAME = new ComponentName(
            "org.nuxeo.ecm.platform.relations.services.RelationService");

    private static final long serialVersionUID = -4778456059717447736L;

    private static final Log log = LogFactory.getLog(RelationService.class);

    private final Map<String, String> graphTypeRegistry;

    private final Map<String, GraphDescription> graphDescriptionRegistry;

    private final transient Map<String, Graph> graphRegistry;

    private final Map<String, String> resourceAdapterRegistry;

    public RelationService() {
        graphTypeRegistry = new Hashtable<String, String>();
        graphDescriptionRegistry = new Hashtable<String, GraphDescription>();
        graphRegistry = new Hashtable<String, Graph>();
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
        GraphTypeDescriptor graphTypeExtension = (GraphTypeDescriptor) contribution;

        String name = graphTypeExtension.getName();
        String className = graphTypeExtension.getClassName();

        if (graphTypeRegistry.containsKey(name)) {
            log.error(String.format("%s already registered using %s", name,
                    className));
        } else {
            graphTypeRegistry.put(name, className);
            log.info(String.format("Registered graph type: %s (%s)", name,
                    className));
        }
    }

    /**
     * Unregisters a graph type.
     */
    private void unregisterGraphType(Object contrib) {
        GraphTypeDescriptor graphTypeExtension = (GraphTypeDescriptor) contrib;

        String name = graphTypeExtension.getName();
        String className = graphTypeExtension.getClassName();

        String registeredClassName = graphTypeRegistry.get(name);
        if (registeredClassName == null) {
            log.error(String.format("Graph type %s not found", name));
        } else if (registeredClassName != className) {
            log.error(String.format("Graph type %s: wrong class",
                    registeredClassName));
        } else {
            graphTypeRegistry.remove(name);
            log.debug("Unregistered graph type: " + name);
        }
    }

    /**
     * Gets a graph given a name.
     * <p>
     * This is used to instantiate graphs with a given name
     * 
     * @param graphType
     * @return the prefixed resource instance initialized with no value or null
     *         if prefix is not found
     */
    public Graph getGraphByType(String graphType) {
        String className = graphTypeRegistry.get(graphType);
        if (className == null) {
            log.error(String.format("Graph type %s not found", graphType));
            return null;
        } else {
            try {
                // Thread context loader is not working in isolated EARs
                return (Graph) RelationService.class.getClassLoader().loadClass(
                        className).newInstance();
            } catch (Exception e) {
                String msg = String.format(
                        "Cannot instantiate graph with type '%s': %s",
                        graphType, e);
                log.error(msg);
                return null;
            }
        }
    }

    public List<String> getGraphTypes() {
        List<String> res = new ArrayList<String>();
        for (String type : graphTypeRegistry.keySet()) {
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
    private void registerGraph(Object contribution) {
        GraphDescription graphDescription = (GraphDescription) contribution;
        String name = graphDescription.getName();

        if (graphDescriptionRegistry.containsKey(name)) {
            log.info(String.format("Overriding graph %s definition", name));
            graphDescriptionRegistry.remove(name);
        }
        graphDescriptionRegistry.put(name, graphDescription);
        log.info(String.format("Graph %s registered", name));

        // remove any existing graph instance in case its definition changed
        graphRegistry.remove(name);
    }

    /**
     * Unregisters a graph.
     */
    private void unregisterGraph(Object contribution) {
        GraphDescriptor graphExtension = (GraphDescriptor) contribution;

        String name = graphExtension.getName();
        GraphDescription registeredGraphDef = graphDescriptionRegistry.get(name);
        if (registeredGraphDef == null) {
            log.error(String.format("Graph %s not found", name));
        } else {
            graphDescriptionRegistry.remove(name);
            log.info(String.format("Graph %s unregistered", name));
        }
        // remove any existing graph instance
        graphRegistry.remove(name);
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
                ResourceAdapter adapter = (ResourceAdapter) RelationService.class.getClassLoader().loadClass(
                        adapterClassName).newInstance();
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

    protected Graph createGraph(String name) {
        GraphDescription graphDescription = graphDescriptionRegistry.get(name);
        if (graphDescription == null) {
            throw new RuntimeException(String.format(
                    "getGraphByName: %s *not found* amongst %s", name,
                    graphDescriptionRegistry.keySet()));
        }

        String graphType = graphDescription.getGraphType();
        Graph graph = getGraphByType(graphType);
        if (graph == null) {
            throw new RuntimeException(String.format(
                    "Caught error when instanciating graph %s", name));
        }
        Map<String, String> options = graphDescription.getOptions();
        Map<String, String> namespaces = graphDescription.getNamespaces();
        graph.setName(name);
        graph.setOptions(options);
        graph.setNamespaces(namespaces);
        return graph;
    }

    public Graph getTransientGraph(String type) throws ClientException {
        Graph graph = getGraphByType(type);
        if (graph == null) {
            throw new RuntimeException(String.format(
                    "Caught error when instanciating graph %s", type));
        }
        return graph;
    }

    // RelationManager interface

    public synchronized Graph getGraphByName(String name)
            throws ClientException {
        Graph registeredGraph = graphRegistry.get(name);
        if (registeredGraph == null) {
            // try to create it
            registeredGraph = createGraph(name);
            // put it in registry for later retrieval
            graphRegistry.put(name, registeredGraph);
        }
        return registeredGraph;
    }

    public Resource getResource(String namespace, Serializable object,
            Map<String, Serializable> context) throws ClientException {
        ResourceAdapter adapter = getResourceAdapterForNamespace(namespace);
        if (adapter == null) {
            log.error("Cannot find adapter for namespace: " + namespace);
            return null;
        } else {
            return adapter.getResource(object, context);
        }
    }

    public Set<Resource> getAllResources(Serializable object,
            Map<String, Serializable> context) throws ClientException {
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

    public Serializable getResourceRepresentation(String namespace,
            Resource resource, Map<String, Serializable> context)
            throws ClientException {
        ResourceAdapter adapter = getResourceAdapterForNamespace(namespace);
        if (adapter == null) {
            log.error("Cannot find adapter for namespace: " + namespace);
            return null;
        } else {
            return adapter.getResourceRepresentation(resource, context);
        }
    }

    public void add(String graphName, List<Statement> statements)
            throws ClientException {
        getGraphByName(graphName).add(statements);
    }

    public void clear(String graphName) throws ClientException {
        getGraphByName(graphName).clear();
    }

    public List<Node> getObjects(String graphName, Node subject, Node predicate)
            throws ClientException {
        return getGraphByName(graphName).getObjects(subject, predicate);
    }

    public List<Node> getPredicates(String graphName, Node subject, Node object)
            throws ClientException {
        return getGraphByName(graphName).getPredicates(subject, object);
    }

    public List<Statement> getStatements(String graphName, Statement statement)
            throws ClientException {
        return getGraphByName(graphName).getStatements(statement);
    }

    public List<Statement> getStatements(String graphName)
            throws ClientException {
        return getGraphByName(graphName).getStatements();
    }

    public List<Node> getSubjects(String graphName, Node predicate, Node object)
            throws ClientException {
        return getGraphByName(graphName).getSubjects(predicate, object);
    }

    public boolean hasResource(String graphName, Resource resource)
            throws ClientException {
        return getGraphByName(graphName).hasResource(resource);
    }

    public boolean hasStatement(String graphName, Statement statement)
            throws ClientException {
        return getGraphByName(graphName).hasStatement(statement);
    }

    public QueryResult query(String graphName, String queryString,
            String language, String baseURI) throws ClientException {
        return getGraphByName(graphName).query(queryString, language, baseURI);
    }

    public boolean read(String graphName, InputStream in, String lang,
            String base) throws ClientException {
        return getGraphByName(graphName).read(in, lang, base);
    }

    public void remove(String graphName, List<Statement> statements)
            throws ClientException {
        getGraphByName(graphName).remove(statements);
    }

    public Long size(String graphName) throws ClientException {
        return getGraphByName(graphName).size();
    }

    public boolean write(String graphName, OutputStream out, String lang,
            String base) throws ClientException {
        return getGraphByName(graphName).write(out, lang, base);
    }

    public List<String> getGraphNames() throws ClientException {
        return new ArrayList<String>(graphDescriptionRegistry.keySet());
    }

    @Override
    public void applicationStarted(ComponentContext context) throws Exception {
        if (!Boolean.parseBoolean(Framework.getProperty(
                "org.nuxeo.ecm.platform.relations.initOnStartup", "true"))) {
            return;
        }

        ClassLoader jbossCL = Thread.currentThread().getContextClassLoader();
        ClassLoader nuxeoCL = RelationService.class.getClassLoader();
        try {
            Thread.currentThread().setContextClassLoader(nuxeoCL);
            log.info("Relation Service initialization");

            for (String graphName : graphDescriptionRegistry.keySet()) {
                log.info("create RDF Graph " + graphName);
                try {
                    Graph graph = this.getGraphByName(graphName);
                    graph.size();
                } catch (Exception e) {
                    log.error("Error while initializing graph " + graphName, e);
                }
            }
        } finally {
            Thread.currentThread().setContextClassLoader(jbossCL);
            log.debug("JBoss ClassLoader restored");
        }
    }

}
