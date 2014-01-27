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
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.event.CoreEventConstants;
import org.nuxeo.ecm.core.event.EventProducer;
import org.nuxeo.ecm.core.event.impl.DocumentEventContext;
import org.nuxeo.ecm.platform.relations.api.DocumentRelationManager;
import org.nuxeo.ecm.platform.relations.api.Graph;
import org.nuxeo.ecm.platform.relations.api.GraphDescription;
import org.nuxeo.ecm.platform.relations.api.GraphFactory;
import org.nuxeo.ecm.platform.relations.api.Literal;
import org.nuxeo.ecm.platform.relations.api.Node;
import org.nuxeo.ecm.platform.relations.api.QNameResource;
import org.nuxeo.ecm.platform.relations.api.QueryResult;
import org.nuxeo.ecm.platform.relations.api.RelationManager;
import org.nuxeo.ecm.platform.relations.api.Resource;
import org.nuxeo.ecm.platform.relations.api.ResourceAdapter;
import org.nuxeo.ecm.platform.relations.api.Statement;
import org.nuxeo.ecm.platform.relations.api.event.RelationEvents;
import org.nuxeo.ecm.platform.relations.api.exceptions.RelationAlreadyExistsException;
import org.nuxeo.ecm.platform.relations.api.impl.LiteralImpl;
import org.nuxeo.ecm.platform.relations.api.impl.RelationDate;
import org.nuxeo.ecm.platform.relations.api.impl.ResourceImpl;
import org.nuxeo.ecm.platform.relations.api.impl.StatementImpl;
import org.nuxeo.ecm.platform.relations.api.util.RelationConstants;
import org.nuxeo.ecm.platform.relations.descriptors.GraphTypeDescriptor;
import org.nuxeo.ecm.platform.relations.descriptors.ResourceAdapterDescriptor;
import org.nuxeo.runtime.api.Framework;
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
        RelationManager, DocumentRelationManager {

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
            Map<String, Serializable> context) throws ClientException {
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

    @Override
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
                        GraphDescription desc = graphDescriptions.get(graphName);
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

    // for consistency for callers only
    private static void putStatements(Map<String, Serializable> options,
            List<Statement> statements) {
        options.put(RelationEvents.STATEMENTS_EVENT_KEY,
                (Serializable) statements);
    }

    private static void putStatements(Map<String, Serializable> options,
            Statement statement) {
        List<Statement> statements = new LinkedList<Statement>();
        statements.add(statement);
        options.put(RelationEvents.STATEMENTS_EVENT_KEY,
                (Serializable) statements);
    }

    private QNameResource getNodeFromDocumentModel(DocumentModel model)
            throws ClientException {
        return (QNameResource) getResource(
                RelationConstants.DOCUMENT_NAMESPACE, model, null);
    }

    @Override
    public void addDocumentRelation(CoreSession session, DocumentModel from,
            DocumentModel to, String predicate, boolean inverse)
            throws ClientException {
        addDocumentRelation(session, from, getNodeFromDocumentModel(to),
                predicate, inverse);
    }

    @Override
    public void addDocumentRelation(CoreSession session, DocumentModel from,
            Node to, String predicate) throws ClientException {
        addDocumentRelation(session, from, to, predicate, false);
    }

    @Override
    public void addDocumentRelation(CoreSession session, DocumentModel from,
            Node to, String predicate, boolean inverse) throws ClientException {
        addDocumentRelation(session, from, to, predicate, inverse, false);
    }

    @Override
    public void addDocumentRelation(CoreSession session, DocumentModel from,
            Node to, String predicate, boolean inverse,
            boolean includeStatementsInEvents) throws ClientException {
        addDocumentRelation(session, from, to, predicate, inverse,
                includeStatementsInEvents, null);
    }

    @Override
    public void addDocumentRelation(CoreSession session, DocumentModel from,
            Node toResource, String predicate, boolean inverse,
            boolean includeStatementsInEvents, String comment)
            throws ClientException {
        Graph graph = getGraphByName(RelationConstants.GRAPH_NAME);
        QNameResource fromResource = getNodeFromDocumentModel(from);

        Resource predicateResource = new ResourceImpl(predicate);
        Statement stmt = null;
        List<Statement> statements = null;
        if (inverse) {
            stmt = new StatementImpl(toResource, predicateResource,
                    fromResource);
            statements = graph.getStatements(toResource, predicateResource,
                    fromResource);
            if (statements != null && statements.size() > 0) {
                throw new RelationAlreadyExistsException();
            }
        } else {
            stmt = new StatementImpl(fromResource, predicateResource,
                    toResource);
            statements = graph.getStatements(fromResource, predicateResource,
                    toResource);
            if (statements != null && statements.size() > 0) {
                throw new RelationAlreadyExistsException();
            }
        }

        // Comment ?
        if (!StringUtils.isEmpty(comment)) {
            stmt.addProperty(RelationConstants.COMMENT,
                    new LiteralImpl(comment));
        }
        Literal now = RelationDate.getLiteralDate(new Date());
        if (stmt.getProperties(RelationConstants.CREATION_DATE) == null) {
            stmt.addProperty(RelationConstants.CREATION_DATE, now);
        }
        if (stmt.getProperties(RelationConstants.MODIFICATION_DATE) == null) {
            stmt.addProperty(RelationConstants.MODIFICATION_DATE, now);
        }

        if (session.getPrincipal() != null
                && stmt.getProperty(RelationConstants.AUTHOR) != null) {
            stmt.addProperty(RelationConstants.AUTHOR, new LiteralImpl(
                    session.getPrincipal().getName()));
        }

        // notifications

        Map<String, Serializable> options = new HashMap<String, Serializable>();
        String currentLifeCycleState = from.getCurrentLifeCycleState();
        options.put(CoreEventConstants.DOC_LIFE_CYCLE, currentLifeCycleState);
        if (includeStatementsInEvents) {
            putStatements(options, stmt);
        }
        options.put(RelationEvents.GRAPH_NAME_EVENT_KEY,
                RelationConstants.GRAPH_NAME);

        // before notification
        notifyEvent(RelationEvents.BEFORE_RELATION_CREATION, from, options,
                comment, session);

        // add statement
        graph.add(stmt);

        // XXX AT: try to refetch it from the graph so that resources are
        // transformed into qname resources: useful for indexing
        if (includeStatementsInEvents) {
            putStatements(options, graph.getStatements(stmt));
        }

        // after notification
        notifyEvent(RelationEvents.AFTER_RELATION_CREATION, from, options,
                comment, session);
    }

    protected void notifyEvent(String eventId, DocumentModel source,
            Map<String, Serializable> options, String comment,
            CoreSession session) {

        EventProducer evtProducer = null;

        try {
            evtProducer = Framework.getService(EventProducer.class);
        } catch (Exception e) {
            log.error("Unable to get EventProducer to send event notification",
                    e);
        }

        DocumentEventContext docCtx = new DocumentEventContext(session,
                session.getPrincipal(), source);
        options.put("category", RelationEvents.CATEGORY);
        options.put("comment", comment);

        try {
            evtProducer.fireEvent(docCtx.newEvent(eventId));
        } catch (ClientException e) {
            log.error("Error while trying to send notification message", e);
        }
    }

    @Override
    public void deleteDocumentRelation(CoreSession session, DocumentModel from,
            DocumentModel to, String predicate) throws ClientException {
        deleteDocumentRelation(session, from, to, predicate, false);
    }

    @Override
    public void deleteDocumentRelation(CoreSession session, DocumentModel from,
            DocumentModel to, String predicate,
            boolean includeStatementsInEvents) throws ClientException {
        QNameResource fromResource = (QNameResource) getResource(
                RelationConstants.DOCUMENT_NAMESPACE, from, null);
        QNameResource toResource = (QNameResource) getResource(
                RelationConstants.DOCUMENT_NAMESPACE, to, null);
        Resource predicateResource = new ResourceImpl(predicate);
        Graph graph = getGraphByName(RelationConstants.GRAPH_NAME);
        List<Statement> statements = graph.getStatements(fromResource,
                predicateResource, toResource);
        if (statements == null || statements.size() == 0) {
            // TODO Throw an exception or silent
            return;
        }
        for (Statement stmt : statements) {
            deleteDocumentRelation(session, stmt);
        }
    }

    @Override
    public void deleteDocumentRelation(CoreSession session, Statement stmt)
            throws ClientException {
        deleteDocumentRelation(session, stmt, false);
    }

    @Override
    public void deleteDocumentRelation(CoreSession session, Statement stmt,
            boolean includeStatementsInEvents) throws ClientException {

        // notifications
        Map<String, Serializable> options = new HashMap<String, Serializable>();
        // will throw a cast exception if bad statement is passed
        DocumentModel source = (DocumentModel) getResourceRepresentation(
                RelationConstants.DOCUMENT_NAMESPACE,
                (QNameResource) stmt.getObject(), null);
        String currentLifeCycleState = source.getCurrentLifeCycleState();
        options.put(CoreEventConstants.DOC_LIFE_CYCLE, currentLifeCycleState);
        options.put(RelationEvents.GRAPH_NAME_EVENT_KEY,
                RelationConstants.GRAPH_NAME);
        if (includeStatementsInEvents) {
            putStatements(options, stmt);
        }

        // before notification
        notifyEvent(RelationEvents.BEFORE_RELATION_REMOVAL, source, options,
                null, session);

        // remove statement
        getGraphByName(RelationConstants.GRAPH_NAME).remove(stmt);

        // after notification
        notifyEvent(RelationEvents.AFTER_RELATION_REMOVAL, source, options,
                null, session);
    }
}
