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

package org.nuxeo.ecm.platform.annotations.http;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.ecm.platform.annotations.api.Annotation;
import org.nuxeo.ecm.platform.annotations.api.AnnotationManager;
import org.nuxeo.ecm.platform.annotations.api.AnnotationsService;
import org.nuxeo.ecm.platform.relations.api.Graph;
import org.nuxeo.ecm.platform.relations.api.RelationManager;
import org.nuxeo.ecm.platform.relations.api.Statement;
import org.nuxeo.runtime.api.Framework;

/**
 * @author Alexandre Russel
 */
public class AnnotationServiceFacade {

    private static final String TRANSIENT_GRAPH_TYPE = "jena";

    private final AnnotationManager manager = new AnnotationManager();

    public AnnotationServiceFacade() {
    }

    protected AnnotationsService getService() {
        return Framework.getService(AnnotationsService.class);
    }

    public void query(String uri, OutputStream outputStream, NuxeoPrincipal name) {
        List<Annotation> annotations;
        try {
            annotations = getService().queryAnnotations(new URI(uri), name);
        } catch (URISyntaxException e) {
            throw new NuxeoException(e);
        }
        List<Statement> statements = new ArrayList<>();
        for (Annotation annotation : annotations) {
            statements.addAll(annotation.getStatements());
        }
        RelationManager service = Framework.getService(RelationManager.class);
        Graph graph = service.getTransientGraph(TRANSIENT_GRAPH_TYPE);
        graph.add(statements);
        try {
            outputStream.write("<?xml version='1.0'?>\n".getBytes());
        } catch (IOException e) {
            throw new NuxeoException(e);
        }
        graph.write(outputStream, null, null);
    }

    public void getAnnotation(String annId, NuxeoPrincipal name, OutputStream os, String baseUrl) {
        Annotation annotation = getService().getAnnotation(annId, name, baseUrl);
        manager.writeAnnotation(os, annotation);
    }

    public void updateAnnotation(InputStream is, NuxeoPrincipal name, OutputStream outputStream, String baseUrl) {
        Annotation annotation = manager.getAnnotation(is);
        annotation = getService().updateAnnotation(annotation, name, baseUrl);
        manager.writeAnnotation(outputStream, annotation);
    }

    public String getAnnotationBody(String id, NuxeoPrincipal name, String baseUrl) {
        Annotation annotation = getService().getAnnotation(id, name, baseUrl);
        return annotation.getBodyAsText();
    }

    public void createAnnotation(InputStream inputStream, NuxeoPrincipal name, OutputStream outputStream,
            String baseUrl) {
        Annotation annotation = manager.getAnnotation(inputStream);
        annotation = getService().addAnnotation(annotation, name, baseUrl);
        manager.writeAnnotation(outputStream, annotation);
    }

    public void delete(String annId, NuxeoPrincipal name, String baseUrl) {
        Annotation annotation = getService().getAnnotation(annId, name, baseUrl);
        getService().deleteAnnotation(annotation, name);
    }

    public void deleteFor(String uri, String annId, NuxeoPrincipal name, String baseUrl) {
        try {
            Annotation annotation = getService().getAnnotation(annId, name, baseUrl);
            getService().deleteAnnotationFor(new URI(uri), annotation, name);
        } catch (URISyntaxException e) {
            throw new NuxeoException(e);
        }
    }

}
