/*
 * (C) Copyright 2006-2008 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Alexandre Russel
 *
 * $Id$
 */

package org.nuxeo.ecm.platform.annotations.service;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.ecm.platform.annotations.api.Annotation;
import org.nuxeo.ecm.platform.annotations.api.AnnotationManager;
import org.nuxeo.ecm.platform.annotations.api.AnnotationsConstants;
import org.nuxeo.ecm.platform.annotations.api.AnnotationsService;
import org.nuxeo.ecm.platform.annotations.api.UriResolver;
import org.nuxeo.ecm.platform.relations.api.Graph;
import org.nuxeo.ecm.platform.relations.api.Literal;
import org.nuxeo.ecm.platform.relations.api.Node;
import org.nuxeo.ecm.platform.relations.api.QueryResult;
import org.nuxeo.ecm.platform.relations.api.RelationManager;
import org.nuxeo.ecm.platform.relations.api.Resource;
import org.nuxeo.ecm.platform.relations.api.Statement;
import org.nuxeo.ecm.platform.relations.api.impl.ResourceImpl;
import org.nuxeo.ecm.platform.relations.api.impl.StatementImpl;
import org.nuxeo.runtime.api.Framework;

/**
 * @author Alexandre Russel
 */
public class AnnotationsServiceImpl implements AnnotationsService {

    private static final String GET_ANN_QUERY = "SELECT ?p ?o WHERE { <source> ?p ?o }";

    private final RelationManager relationManager;

    private final AnnotationConfigurationService configuration;

    private final AnnotationIDGenerator idGenerator;

    private final MetadataMapper mapper;

    private final UriResolver resolver;

    private final AnnotationManager annotationManager = new AnnotationManager();

    public AnnotationsServiceImpl() {
        relationManager = Framework.getService(RelationManager.class);
        configuration = Framework.getService(AnnotationConfigurationService.class);
        idGenerator = configuration.getIDGenerator();
        mapper = configuration.getMetadataMapper();
        resolver = configuration.getUriResolver();
    }

    public Annotation addAnnotation(Annotation annotation, NuxeoPrincipal user, String baseUrl) {
        String id = idGenerator.getNext();
        return addAnnotation(annotation, user, baseUrl, id);
    }

    private Annotation addAnnotation(Annotation annotation, NuxeoPrincipal user, String baseUrl, String id) {
        Graph graph = relationManager.getGraphByName(AnnotationsConstants.DEFAULT_GRAPH_NAME);
        Resource resource = new ResourceImpl(AnnotationsConstants.DEFAULT_BASE_URI + id);
        annotation.setSubject(resource);
        mapper.updateMetadata(annotation, user);
        graph.add(annotation.getStatements());
        return annotation;

    }

    public void deleteAnnotation(Annotation annotation, NuxeoPrincipal user) {
        Graph graph = relationManager.getGraphByName(AnnotationsConstants.DEFAULT_GRAPH_NAME);
        graph.remove(annotation.getStatements());
    }

    public void deleteAnnotationFor(URI uri, Annotation annotation, NuxeoPrincipal user) {
        List<Statement> statementsToDelete = new ArrayList<Statement>();

        boolean removeAllAnnotationStatements = true;
        List<Statement> statements = annotation.getStatements();
        for (Statement statement : statements) {
            if (statement.getPredicate().equals(AnnotationsConstants.a_annotates)) {
                Resource resource = (Resource) statement.getObject();
                if (uri.toString().equals(resource.getUri())) {
                    statementsToDelete.add(statement);
                } else {
                    // we have another URI using these annotations statements
                    removeAllAnnotationStatements = false;
                }
            }
        }
        if (removeAllAnnotationStatements) {
            statementsToDelete.addAll(annotation.getStatements());
        }
        Graph graph = getAnnotationGraph();
        graph.remove(statementsToDelete);
    }

    public Annotation getAnnotation(String id, NuxeoPrincipal user, String baseUrl) {
        String uri = AnnotationsConstants.DEFAULT_BASE_URI + id;
        String query = GET_ANN_QUERY.replaceFirst("source", uri);
        Graph graph = relationManager.getGraphByName(AnnotationsConstants.DEFAULT_GRAPH_NAME);
        QueryResult result = graph.query(query, "sparql", null);
        List<Statement> statements = new ArrayList<Statement>();
        for (Map<String, Node> map : result.getResults()) {
            Statement statement = new StatementImpl(new ResourceImpl(uri), map.get("p"), map.get("o"));
            statements.add(statement);
        }
        return annotationManager.getAnnotation(statements);
    }

    public String getAnnotationBody(String id, NuxeoPrincipal name) {
        String uri = AnnotationsConstants.DEFAULT_BASE_URI + "body/" + id;
        Graph graph = relationManager.getGraphByName(AnnotationsConstants.DEFAULT_GRAPH_NAME);
        List<Node> result = graph.getObjects(new ResourceImpl(uri), AnnotationsConstants.nx_body_content);
        return ((Literal) result.get(0)).getValue();
    }

    public List<Annotation> queryAnnotations(URI uri, NuxeoPrincipal user) {
        AnnotationQuery query = new AnnotationQuery();
        Graph graph = relationManager.getGraphByName(AnnotationsConstants.DEFAULT_GRAPH_NAME);
        uri = resolver.getSearchURI(uri);
        return query.getAnnotationsForURIs(uri, graph);
    }

    @Override
    public int getAnnotationsCount(URI uri, NuxeoPrincipal user) {
        AnnotationQuery query = new AnnotationQuery();
        Graph graph = relationManager.getGraphByName(AnnotationsConstants.DEFAULT_GRAPH_NAME);
        uri = resolver.getSearchURI(uri);
        return query.getAnnotationsCountForURIs(uri, graph);
    }

    public Annotation updateAnnotation(Annotation annotation, NuxeoPrincipal user, String baseUrl) {
        String id = annotation.getId();
        deleteAnnotation(annotation, user);
        return addAnnotation(annotation, user, baseUrl, id);
    }

    public Graph getAnnotationGraph() {
        return relationManager.getGraphByName(AnnotationsConstants.DEFAULT_GRAPH_NAME);
    }

}
