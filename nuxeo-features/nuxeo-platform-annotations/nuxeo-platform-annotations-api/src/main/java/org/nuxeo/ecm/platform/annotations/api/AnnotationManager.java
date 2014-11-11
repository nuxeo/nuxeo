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

package org.nuxeo.ecm.platform.annotations.api;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

import org.nuxeo.ecm.platform.relations.api.Graph;
import org.nuxeo.ecm.platform.relations.api.Node;
import org.nuxeo.ecm.platform.relations.api.RelationManager;
import org.nuxeo.ecm.platform.relations.api.Resource;
import org.nuxeo.ecm.platform.relations.api.Statement;
import org.nuxeo.ecm.platform.relations.api.impl.ResourceImpl;
import org.nuxeo.runtime.api.Framework;

/**
 * @author Alexandre Russel
 *
 */
public class AnnotationManager {

    private static final String TRANSIENT_GRAPH_TYPE = "jena";

    public void writeAnnotation(OutputStream os, Annotation annotation)
            throws AnnotationException {
        Graph graph = getTransientGraph();
        graph.add(annotation.getStatements());
        try {
            os.write("<?xml version='1.0'?>".getBytes());
        } catch (IOException e) {
            throw new AnnotationException(e);
        }
        graph.write(os, null, null);
    }

    public Annotation translateAnnotationFromRepo(UriResolver resolver,
            String baseUrl, Annotation annotation) throws AnnotationException {
        List<Statement> results = new ArrayList<Statement>();
        for (Statement statement : annotation.getStatements()) {
            Node node = statement.getSubject();
            if (node instanceof Resource) {
                Resource resource = getTranslatedResource(resolver, baseUrl,
                        node);
                statement.setSubject(resource);
            }
            node = statement.getObject();
            if (node instanceof Resource) {
                Resource resource = getTranslatedResource(resolver, baseUrl,
                        node);
                statement.setObject(resource);
            }
            results.add(statement);
        }
        return getAnnotation(results);
    }

    private static Resource getTranslatedResource(UriResolver resolver,
            String baseUrl, Node node) throws AnnotationException {
        String uri = ((Resource) node).getUri();
        Resource resource = null;
        try {
            URI newUri = resolver.translateFromGraphURI(new URI(uri), baseUrl);
            resource = new ResourceImpl(newUri.toString());
        } catch (URISyntaxException e) {
            throw new AnnotationException(e);
        }
        return resource;
    }

    public Annotation translateAnnotationToRepo(UriResolver resolver,
            Annotation annotation) throws AnnotationException {
        List<Statement> results = new ArrayList<Statement>();
        for (Statement statement : annotation.getStatements()) {
            Node node = statement.getSubject();
            if (node instanceof Resource) {
                String uri = ((Resource) node).getUri();
                URI u;
                try {
                    u = resolver.translateToGraphURI(new URI(uri));
                } catch (URISyntaxException e) {
                    throw new AnnotationException(e);
                }
                Resource resource = new ResourceImpl(u.toString());
                statement.setSubject(resource);
            }
            node =  statement.getObject();
            if (node instanceof Resource) {
                String uri = ((Resource) node).getUri();
                URI u;
                try {
                    u = resolver.translateToGraphURI(new URI(uri));
                } catch (URISyntaxException e) {
                    throw new AnnotationException(e);
                }
                Resource resource = new ResourceImpl(u.toString());
                statement.setObject(resource);
            }
            results.add(statement);
        }
        return getAnnotation(results);
    }

    public Annotation getAnnotation(List<Statement> statements)
            throws AnnotationException {
        AnnotationImpl annotation = new AnnotationImpl();
        Graph graph = getTransientGraph();
        graph.add(statements);
        annotation.setGraph(graph);
        return annotation;
    }

    public Annotation getAnnotation(InputStream is) throws AnnotationException {
        Graph graph = getTransientGraph();
        graph.read(is, null, null);
        AnnotationImpl annotation = new AnnotationImpl();
        annotation.setGraph(graph);
        return annotation;
    }

    public Annotation getAnnotation(String is) throws AnnotationException {
        Graph graph = getTransientGraph();
        graph.read(is, null, null);
        AnnotationImpl annotation = new AnnotationImpl();
        annotation.setGraph(graph);
        return annotation;
    }

    private static Graph getTransientGraph() throws AnnotationException {
        Graph graph;
        try {
            RelationManager service = Framework.getService(RelationManager.class);
            graph = service.getTransientGraph(TRANSIENT_GRAPH_TYPE);
        } catch (Exception e) {
            throw new AnnotationException(e);
        }
        return graph;
    }

}
