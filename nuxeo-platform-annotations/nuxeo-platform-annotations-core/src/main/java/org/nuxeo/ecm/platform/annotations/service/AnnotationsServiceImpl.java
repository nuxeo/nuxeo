/*
 * (C) Copyright 2006-2008 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     Alexandre Russel
 *
 * $Id$
 */

package org.nuxeo.ecm.platform.annotations.service;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.ecm.platform.annotations.api.Annotation;
import org.nuxeo.ecm.platform.annotations.api.AnnotationException;
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
 *
 */
public class AnnotationsServiceImpl implements AnnotationsService {

    private static final String GET_ANN_QUERY = "SELECT ?p ?o WHERE { <source> ?p ?o .}";

    private final RelationManager relationManager;

    private final AnnotationConfigurationService configuration;

    private final AnnotationIDGenerator idGenerator;

    private final MetadataMapper mapper;

    private final UriResolver resolver;

    private final AnnotationManager annotationManager = new AnnotationManager();

    public AnnotationsServiceImpl() throws Exception {
        relationManager = Framework.getService(RelationManager.class);
        configuration = Framework.getService(AnnotationConfigurationService.class);
        idGenerator = configuration.getIDGenerator();
        mapper = configuration.getMetadataMapper();
        resolver = configuration.getUriResolver();
    }

    public Annotation addAnnotation(Annotation annotation, NuxeoPrincipal user,
            String baseUrl) throws AnnotationException {
        String id = idGenerator.getNext();
        return addAnnotation(annotation, user, baseUrl, id);
    }

    private Annotation addAnnotation(Annotation annotation,
            NuxeoPrincipal user, String baseUrl, String id)
            throws AnnotationException {
        try {
            Graph graph = relationManager.getGraphByName(AnnotationsConstants.DEFAULT_GRAPH_NAME);
            Resource resource = new ResourceImpl(
                    AnnotationsConstants.DEFAULT_BASE_URI + id);
            annotation.setSubject(resource);
            mapper.updateMetadata(annotation, user);
            graph.add(annotation.getStatements());
        } catch (Exception e) {
            throw new AnnotationException(e);
        }
        return annotation;

    }

    public void deleteAnnotation(Annotation annotation, NuxeoPrincipal user)
            throws AnnotationException {
        try {
            Graph graph = relationManager.getGraphByName(AnnotationsConstants.DEFAULT_GRAPH_NAME);
            graph.remove(annotation.getStatements());
        } catch (Exception e) {
            throw new AnnotationException(e);
        }
    }

    public void deleteAnnotationFor(URI uri, Annotation annotation,
            NuxeoPrincipal user) throws AnnotationException {
        List<Statement> statementsToDelete = new ArrayList<Statement>();

        boolean removeAllAnnotationStatements = true;
        List<Statement> statements = annotation.getStatements();
        for (Statement statement : statements) {
            if (statement.getPredicate().equals(
                    AnnotationsConstants.a_annotates)) {
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

    public Annotation getAnnotation(String id, NuxeoPrincipal user,
            String baseUrl) throws AnnotationException {
        Annotation annotation = null;
        String uri = AnnotationsConstants.DEFAULT_BASE_URI + id;
        String query = GET_ANN_QUERY.replaceFirst("source", uri);
        try {
            Graph graph = relationManager.getGraphByName(AnnotationsConstants.DEFAULT_GRAPH_NAME);
            QueryResult result = graph.query(query, "sparql", null);
            List<Statement> statements = new ArrayList<Statement>();
            for (Map<String, Node> map : result.getResults()) {
                Statement statement = new StatementImpl(new ResourceImpl(uri),
                        map.get("p"), map.get("o"));
                statements.add(statement);
            }
            annotation = annotationManager.getAnnotation(statements);
        } catch (Exception e) {
            throw new AnnotationException(e);
        }
        return annotation;
    }

    public String getAnnotationBody(String id, NuxeoPrincipal name)
            throws AnnotationException {
        String uri = AnnotationsConstants.DEFAULT_BASE_URI + "body/" + id;
        try {
            Graph graph = relationManager.getGraphByName(AnnotationsConstants.DEFAULT_GRAPH_NAME);
            List<Node> result = graph.getObjects(new ResourceImpl(uri),
                    AnnotationsConstants.nx_body_content);
            return ((Literal) result.get(0)).getValue();
        } catch (Exception e) {
            throw new AnnotationException(e);
        }
    }

    public List<Annotation> queryAnnotations(URI uri,
            Map<String, String> filters, NuxeoPrincipal user)
            throws AnnotationException {
        AnnotationQuery query = new AnnotationQuery();
        Graph graph = null;
        try {
            graph = relationManager.getGraphByName(AnnotationsConstants.DEFAULT_GRAPH_NAME);
        } catch (ClientException e) {
            throw new AnnotationException(e);
        }
        List<URI> uris = resolver.getSearchURI(uri);
        List<Annotation> annotations = query.getAnnotationsForURIs(uris, graph,
                filters);
        return annotations;
    }

    public Annotation updateAnnotation(Annotation annotation,
            NuxeoPrincipal user, String baseUrl) throws AnnotationException {
        try {
            String id = annotation.getId();
            deleteAnnotation(annotation, user);
            return addAnnotation(annotation, user, baseUrl, id);
        } catch (Exception e) {
            throw new AnnotationException(e);
        }
    }

    public Graph getAnnotationGraph() throws AnnotationException {
        try {
            return relationManager.getGraphByName(AnnotationsConstants.DEFAULT_GRAPH_NAME);
        } catch (ClientException e) {
            throw new AnnotationException(e);
        }
    }
}
