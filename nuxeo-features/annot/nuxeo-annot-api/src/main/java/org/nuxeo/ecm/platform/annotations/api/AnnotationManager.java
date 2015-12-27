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

package org.nuxeo.ecm.platform.annotations.api;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.platform.relations.api.Graph;
import org.nuxeo.ecm.platform.relations.api.Node;
import org.nuxeo.ecm.platform.relations.api.RelationManager;
import org.nuxeo.ecm.platform.relations.api.Resource;
import org.nuxeo.ecm.platform.relations.api.Statement;
import org.nuxeo.ecm.platform.relations.api.impl.ResourceImpl;
import org.nuxeo.runtime.api.Framework;

/**
 * @author Alexandre Russel
 */
public class AnnotationManager {

    private static final String TRANSIENT_GRAPH_TYPE = "jena";

    public void writeAnnotation(OutputStream os, Annotation annotation) {
        Graph graph = getTransientGraph();
        graph.add(annotation.getStatements());
        try {
            os.write("<?xml version='1.0'?>".getBytes());
        } catch (IOException e) {
            throw new NuxeoException(e);
        }
        graph.write(os, null, null);
    }

    public Annotation translateAnnotationFromRepo(UriResolver resolver, String baseUrl, Annotation annotation) {
        List<Statement> results = new ArrayList<Statement>();
        for (Statement statement : annotation.getStatements()) {
            Node node = statement.getSubject();
            if (node instanceof Resource) {
                Resource resource = getTranslatedResource(resolver, baseUrl, node);
                statement.setSubject(resource);
            }
            node = statement.getObject();
            if (node instanceof Resource) {
                Resource resource = getTranslatedResource(resolver, baseUrl, node);
                statement.setObject(resource);
            }
            results.add(statement);
        }
        return getAnnotation(results);
    }

    private static Resource getTranslatedResource(UriResolver resolver, String baseUrl, Node node) {
        String uri = ((Resource) node).getUri();
        Resource resource = null;
        try {
            URI newUri = resolver.translateFromGraphURI(new URI(uri), baseUrl);
            resource = new ResourceImpl(newUri.toString());
        } catch (URISyntaxException e) {
            throw new NuxeoException(e);
        }
        return resource;
    }

    public Annotation translateAnnotationToRepo(UriResolver resolver, Annotation annotation) {
        List<Statement> results = new ArrayList<Statement>();
        for (Statement statement : annotation.getStatements()) {
            Node node = statement.getSubject();
            if (node instanceof Resource) {
                String uri = ((Resource) node).getUri();
                URI u;
                try {
                    u = resolver.translateToGraphURI(new URI(uri));
                } catch (URISyntaxException e) {
                    throw new NuxeoException(e);
                }
                Resource resource = new ResourceImpl(u.toString());
                statement.setSubject(resource);
            }
            node = statement.getObject();
            if (node instanceof Resource) {
                String uri = ((Resource) node).getUri();
                URI u;
                try {
                    u = resolver.translateToGraphURI(new URI(uri));
                } catch (URISyntaxException e) {
                    throw new NuxeoException(e);
                }
                Resource resource = new ResourceImpl(u.toString());
                statement.setObject(resource);
            }
            results.add(statement);
        }
        return getAnnotation(results);
    }

    public Annotation getAnnotation(List<Statement> statements) {
        AnnotationImpl annotation = new AnnotationImpl();
        Graph graph = getTransientGraph();
        graph.add(statements);
        annotation.setGraph(graph);
        return annotation;
    }

    public Annotation getAnnotation(InputStream is) {
        Graph graph = getTransientGraph();
        graph.read(is, null, null);
        AnnotationImpl annotation = new AnnotationImpl();
        annotation.setGraph(graph);
        return annotation;
    }

    public Annotation getAnnotation(String is) {
        Graph graph = getTransientGraph();
        graph.read(is, null, null);
        AnnotationImpl annotation = new AnnotationImpl();
        annotation.setGraph(graph);
        return annotation;
    }

    private static Graph getTransientGraph() {
        return Framework.getService(RelationManager.class).getTransientGraph(TRANSIENT_GRAPH_TYPE);
    }

}
