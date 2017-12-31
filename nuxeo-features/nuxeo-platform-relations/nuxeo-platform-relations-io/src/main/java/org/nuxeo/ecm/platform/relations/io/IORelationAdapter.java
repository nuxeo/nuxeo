/*
 * (C) Copyright 2006-2007 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     <a href="mailto:at@nuxeo.com">Anahide Tchertchian</a>
 *
 * $Id: IORelationAdapter.java 26168 2007-10-18 11:21:21Z dmihalache $
 */

package org.nuxeo.ecm.platform.relations.io;

import java.io.InputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.CloseableCoreSession;
import org.nuxeo.ecm.core.api.CoreInstance;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentNotFoundException;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.core.io.DocumentTranslationMap;
import org.nuxeo.ecm.platform.io.api.AbstractIOResourceAdapter;
import org.nuxeo.ecm.platform.io.api.IOResources;
import org.nuxeo.ecm.platform.relations.api.Graph;
import org.nuxeo.ecm.platform.relations.api.Literal;
import org.nuxeo.ecm.platform.relations.api.Node;
import org.nuxeo.ecm.platform.relations.api.QNameResource;
import org.nuxeo.ecm.platform.relations.api.RelationManager;
import org.nuxeo.ecm.platform.relations.api.Resource;
import org.nuxeo.ecm.platform.relations.api.ResourceAdapter;
import org.nuxeo.ecm.platform.relations.api.Statement;
import org.nuxeo.ecm.platform.relations.api.Subject;
import org.nuxeo.ecm.platform.relations.api.impl.RelationDate;
import org.nuxeo.ecm.platform.relations.api.impl.ResourceImpl;
import org.nuxeo.ecm.platform.relations.api.impl.StatementImpl;
import org.nuxeo.runtime.api.Framework;

/**
 * Adapter for import/export of relations
 *
 * @author <a href="mailto:at@nuxeo.com">Anahide Tchertchian</a>
 */
public class IORelationAdapter extends AbstractIOResourceAdapter {

    private static final Log log = LogFactory.getLog(IORelationAdapter.class);

    private static final long serialVersionUID = -3661302796286246086L;

    @Override
    public void setProperties(Map<String, Serializable> properties) {
        if (properties != null) {
            for (Map.Entry<String, Serializable> prop : properties.entrySet()) {
                String propName = prop.getKey();
                Serializable propValue = prop.getValue();
                if (IORelationAdapterProperties.GRAPH.equals(propName)) {
                    setStringProperty(propName, propValue);
                }
                if (IORelationAdapterProperties.IMPORT_GRAPH.equals(propName)) {
                    setStringProperty(propName, propValue);
                }
                if (IORelationAdapterProperties.IGNORE_EXTERNAL.equals(propName)) {
                    setBooleanProperty(propName, propValue);
                }
                if (IORelationAdapterProperties.IGNORE_LITERALS.equals(propName)) {
                    setBooleanProperty(propName, propValue);
                }
                if (IORelationAdapterProperties.IGNORE_SIMPLE_RESOURCES.equals(propName)) {
                    setBooleanProperty(propName, propValue);
                }
                if (IORelationAdapterProperties.FILTER_PREDICATES.equals(propName)) {
                    setStringArrayProperty(propName, propValue);
                }
                if (IORelationAdapterProperties.IGNORE_PREDICATES.equals(propName)) {
                    setStringArrayProperty(propName, propValue);
                }
                if (IORelationAdapterProperties.FILTER_METADATA.equals(propName)) {
                    setStringArrayProperty(propName, propValue);
                }
                if (IORelationAdapterProperties.IGNORE_METADATA.equals(propName)) {
                    setStringArrayProperty(propName, propValue);
                }
                if (IORelationAdapterProperties.IGNORE_ALL_METADATA.equals(propName)) {
                    setBooleanProperty(propName, propValue);
                }
                if (IORelationAdapterProperties.UPDATE_DATE_METADATA.equals(propName)) {
                    setStringArrayProperty(propName, propValue);
                }
            }
        }
        if (this.properties == null || getStringProperty(IORelationAdapterProperties.GRAPH) == null) {
            log.warn("No graph name given for relations adapter, " + "no IO will be performed with this adapter");
        }
    }

    protected RelationManager getRelationManager() {
        return Framework.getService(RelationManager.class);
    }

    protected List<Statement> getMatchingStatements(Graph graph, Resource resource) {
        // TODO filter using properties
        List<Statement> matching = new ArrayList<Statement>();
        Statement incomingPattern = new StatementImpl(null, null, resource);
        matching.addAll(graph.getStatements(incomingPattern));
        Statement outgoingPattern = new StatementImpl(resource, null, null);
        matching.addAll(graph.getStatements(outgoingPattern));
        return filterMatchingStatements(matching);
    }

    protected Statement getFilteredStatement(Statement statement) {
        Subject subject = statement.getSubject();
        Resource predicate = statement.getPredicate();
        Node object = statement.getObject();
        if (getBooleanProperty(IORelationAdapterProperties.IGNORE_LITERALS) && object.isLiteral()) {
            return null;
        }
        if (getBooleanProperty(IORelationAdapterProperties.IGNORE_SIMPLE_RESOURCES)) {
            if (!subject.isQNameResource() || !object.isQNameResource()) {
                return null;
            }
        }
        String[] filteredPredicates = getStringArrayProperty(IORelationAdapterProperties.FILTER_PREDICATES);
        if (filteredPredicates != null) {
            if (!Arrays.asList(filteredPredicates).contains(predicate.getUri())) {
                return null;
            }
        }
        String[] ignoredPredicates = getStringArrayProperty(IORelationAdapterProperties.IGNORE_PREDICATES);
        if (ignoredPredicates != null) {
            if (Arrays.asList(ignoredPredicates).contains(predicate.getUri())) {
                return null;
            }
        }
        if (getBooleanProperty(IORelationAdapterProperties.IGNORE_ALL_METADATA)) {
            Statement newStatement = (Statement) statement.clone();
            newStatement.deleteProperties();
            return newStatement;
        }
        String[] filterMetadata = getStringArrayProperty(IORelationAdapterProperties.FILTER_METADATA);
        if (filterMetadata != null) {
            Statement newStatement = (Statement) statement.clone();
            Map<Resource, Node[]> props = newStatement.getProperties();
            List<String> filter = Arrays.asList(filterMetadata);
            for (Map.Entry<Resource, Node[]> prop : props.entrySet()) {
                Resource propKey = prop.getKey();
                if (!filter.contains(propKey.getUri())) {
                    newStatement.deleteProperty(propKey);
                }
            }
            return newStatement;
        }
        String[] ignoreMetadata = getStringArrayProperty(IORelationAdapterProperties.IGNORE_METADATA);
        if (ignoreMetadata != null) {
            Statement newStatement = (Statement) statement.clone();
            Map<Resource, Node[]> props = newStatement.getProperties();
            List<String> filter = Arrays.asList(ignoreMetadata);
            for (Map.Entry<Resource, Node[]> prop : props.entrySet()) {
                Resource propKey = prop.getKey();
                if (filter.contains(propKey.getUri())) {
                    newStatement.deleteProperty(propKey);
                }
            }
            return newStatement;
        }
        return statement;
    }

    protected List<Statement> filterMatchingStatements(List<Statement> statements) {
        List<Statement> newStatements = null;
        if (statements != null) {
            newStatements = new ArrayList<Statement>();
            for (Statement stmt : statements) {
                Statement newStmt = getFilteredStatement(stmt);
                if (newStmt != null) {
                    newStatements.add(newStmt);
                }
            }
        }
        return newStatements;
    }

    protected DocumentRef getDocumentRef(RelationManager relManager, QNameResource resource) {
        String ns = resource.getNamespace();
        if ("http://www.nuxeo.org/document/uid/".equals(ns)) {
            // BS: Avoid using default resource resolver since it is not working
            // when
            // the resource document is not currently existing in the target
            // repository.
            // TODO This is a hack and should be fixed in the lower layers or by
            // changing
            // import logic.
            String id = resource.getLocalName();
            int p = id.indexOf('/');
            if (p > -1) {
                id = id.substring(p + 1);
            }
            return new IdRef(id);
        }
        return null;
    }

    /**
     * Extract relations involving given documents.
     * <p>
     * The adapter properties will filter which relations must be taken into account.
     */
    @Override
    public IOResources extractResources(String repo, Collection<DocumentRef> sources) {
        if (sources == null || sources.isEmpty()) {
            return null;
        }
        String graphName = getStringProperty(IORelationAdapterProperties.GRAPH);
        if (graphName == null) {
            log.error("Cannot extract resources, no graph supplied");
            return null;
        }
        try (CloseableCoreSession session = CoreInstance.openCoreSessionSystem(repo)) {
            RelationManager relManager = getRelationManager();
            Graph graph = relManager.getGraphByName(graphName);
            if (graph == null) {
                log.error("Cannot resolve graph " + graphName);
                return null;
            }
            Map<DocumentRef, Set<Resource>> docResources = new HashMap<DocumentRef, Set<Resource>>();
            List<Statement> statements = new ArrayList<Statement>();
            Set<Resource> allResources = new HashSet<Resource>();
            for (DocumentRef docRef : sources) {
                DocumentModel doc = session.getDocument(docRef);
                Map<String, Object> context = Collections.<String, Object> singletonMap(
                        ResourceAdapter.CORE_SESSION_CONTEXT_KEY, session);
                Set<Resource> resources = relManager.getAllResources(doc, context);
                docResources.put(docRef, resources);
                allResources.addAll(resources);
                for (Resource resource : resources) {
                    statements.addAll(getMatchingStatements(graph, resource));
                }
            }
            Map<String, String> namespaces = graph.getNamespaces();
            // filter duplicate statements + statements involving external
            // resources
            IORelationGraphHelper graphHelper = new IORelationGraphHelper(namespaces, statements);
            Graph memoryGraph = graphHelper.getGraph();
            List<Statement> toRemove = new ArrayList<Statement>();
            if (getBooleanProperty(IORelationAdapterProperties.IGNORE_EXTERNAL)) {
                for (Statement stmt : memoryGraph.getStatements()) {
                    Subject subject = stmt.getSubject();
                    if (subject.isQNameResource()) {
                        if (!allResources.contains(subject)) {
                            toRemove.add(stmt);
                            continue;
                        }
                    }
                    Node object = stmt.getObject();
                    if (object.isQNameResource()) {
                        if (!allResources.contains(subject)) {
                            toRemove.add(stmt);
                            continue;
                        }
                    }
                }
            }
            memoryGraph.remove(toRemove);
            return new IORelationResources(namespaces, docResources, memoryGraph.getStatements());
        }
    }

    @Override
    public void getResourcesAsXML(OutputStream out, IOResources resources) {
        if (!(resources instanceof IORelationResources)) {
            return;
        }
        IORelationResources relResources = (IORelationResources) resources;
        Map<String, String> namespaces = relResources.getNamespaces();
        List<Statement> statements = relResources.getStatements();
        IORelationGraphHelper graphHelper = new IORelationGraphHelper(namespaces, statements);
        graphHelper.write(out);
    }

    private void addResourceEntry(RelationManager relManager, Map<DocumentRef, Set<Resource>> map, Node node) {
        if (!node.isQNameResource()) {
            return;
        }
        QNameResource resource = (QNameResource) node;
        DocumentRef docRef = getDocumentRef(relManager, resource);
        if (docRef == null) {
            return;
        }
        if (map.containsKey(docRef)) {
            map.get(docRef).add(resource);
        } else {
            Set<Resource> set = new HashSet<Resource>();
            set.add(resource);
            map.put(docRef, set);
        }
    }

    @Override
    public IOResources loadResourcesFromXML(InputStream in) {
        RelationManager relManager = getRelationManager();
        String graphName = getStringProperty(IORelationAdapterProperties.IMPORT_GRAPH);
        if (graphName == null) {
            graphName = getStringProperty(IORelationAdapterProperties.GRAPH);
        }
        // XXX find target graph to retrieve namespaces
        Map<String, String> namespaces = null;
        if (graphName != null) {
            Graph graph = relManager.getGraphByName(graphName);
            if (graph != null) {
                namespaces = graph.getNamespaces();
            }
        }
        IORelationGraphHelper graphHelper = new IORelationGraphHelper(namespaces, null);
        graphHelper.read(in);
        // find documents related to given statements
        List<Statement> statements = filterMatchingStatements(graphHelper.getStatements());
        Map<DocumentRef, Set<Resource>> docResources = new HashMap<DocumentRef, Set<Resource>>();
        for (Statement statement : statements) {
            Subject subject = statement.getSubject();
            addResourceEntry(relManager, docResources, subject);
            Node object = statement.getObject();
            addResourceEntry(relManager, docResources, object);
        }
        return new IORelationResources(namespaces, docResources, statements);
    }

    @Override
    public void storeResources(IOResources resources) {
        if (!(resources instanceof IORelationResources)) {
            return;
        }
        IORelationResources relResources = (IORelationResources) resources;
        String graphName = getStringProperty(IORelationAdapterProperties.IMPORT_GRAPH);
        if (graphName == null) {
            graphName = getStringProperty(IORelationAdapterProperties.GRAPH);
        }
        if (graphName == null) {
            log.error("Cannot find graph name");
            return;
        }
        RelationManager relManager = getRelationManager();
        Graph graph = relManager.getGraphByName(graphName);
        if (graph == null) {
            log.error("Cannot find graph with name " + graphName);
            return;
        }
        graph.add(relResources.getStatements());
    }

    protected static Statement updateDate(Statement statement, Literal newDate, List<Resource> properties) {
        for (Resource property : properties) {
            // do not update if not present
            if (statement.getProperty(property) != null) {
                statement.setProperty(property, newDate);
            }
        }
        return statement;
    }

    @Override
    public IOResources translateResources(String repo, IOResources resources, DocumentTranslationMap map) {
        if (map == null) {
            return null;
        }
        if (!(resources instanceof IORelationResources)) {
            return resources;
        }
        try (CloseableCoreSession session = CoreInstance.openCoreSessionSystem(repo)) {
            IORelationResources relResources = (IORelationResources) resources;
            Map<String, String> namespaces = relResources.getNamespaces();
            IORelationGraphHelper graphHelper = new IORelationGraphHelper(namespaces, relResources.getStatements());
            Graph graph = graphHelper.getGraph();
            RelationManager relManager = getRelationManager();
            // variables for date update
            Literal newDate = RelationDate.getLiteralDate(new Date());
            String[] dateUris = getStringArrayProperty(IORelationAdapterProperties.UPDATE_DATE_METADATA);
            List<Resource> dateProperties = new ArrayList<Resource>();
            if (dateUris != null) {
                for (String dateUri : dateUris) {
                    dateProperties.add(new ResourceImpl(dateUri));
                }
            }
            for (Map.Entry<DocumentRef, Set<Resource>> entry : relResources.getResourcesMap().entrySet()) {
                DocumentRef oldRef = entry.getKey();
                DocumentRef newRef = map.getDocRefMap().get(oldRef);
                Set<Resource> docResources = relResources.getDocumentResources(oldRef);
                for (Resource resource : docResources) {
                    if (!resource.isQNameResource() || oldRef.equals(newRef)) {
                        // cannot translate or no change => keep same
                        continue;
                    }
                    Statement pattern = new StatementImpl(resource, null, null);
                    List<Statement> outgoing = graph.getStatements(pattern);
                    pattern = new StatementImpl(null, null, resource);
                    List<Statement> incoming = graph.getStatements(pattern);

                    // remove old statements
                    graph.remove(outgoing);
                    graph.remove(incoming);

                    if (newRef == null) {
                        // do not replace
                        continue;
                    }

                    DocumentModel newDoc;
                    try {
                        newDoc = session.getDocument(newRef);
                    } catch (DocumentNotFoundException e) {
                        // do not replace
                        continue;
                    }
                    QNameResource qnameRes = (QNameResource) resource;
                    Map<String, Object> context = Collections.<String, Object> singletonMap(
                            ResourceAdapter.CORE_SESSION_CONTEXT_KEY, session);
                    Resource newResource = relManager.getResource(qnameRes.getNamespace(), newDoc, context);
                    Statement newStatement;
                    List<Statement> newOutgoing = new ArrayList<Statement>();
                    for (Statement stmt : outgoing) {
                        newStatement = (Statement) stmt.clone();
                        newStatement.setSubject(newResource);
                        if (dateProperties != null) {
                            newStatement = updateDate(newStatement, newDate, dateProperties);
                        }
                        newOutgoing.add(newStatement);
                    }
                    graph.add(newOutgoing);
                    List<Statement> newIncoming = new ArrayList<Statement>();
                    for (Statement stmt : incoming) {
                        newStatement = (Statement) stmt.clone();
                        newStatement.setObject(newResource);
                        if (dateProperties != null) {
                            newStatement = updateDate(newStatement, newDate, dateProperties);
                        }
                        newIncoming.add(newStatement);
                    }
                    graph.add(newIncoming);
                }
            }
            return new IORelationResources(namespaces, relResources.getResourcesMap(), graph.getStatements());
        }
    }
}
