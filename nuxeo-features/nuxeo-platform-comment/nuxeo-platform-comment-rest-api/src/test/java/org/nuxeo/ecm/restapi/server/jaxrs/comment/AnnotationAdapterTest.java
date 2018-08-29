/*
 * (C) Copyright 2018 Nuxeo (http://nuxeo.com/) and others.
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
 *     Funsho David
 */

package org.nuxeo.ecm.restapi.server.jaxrs.comment;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.core.io.registry.MarshallerHelper;
import org.nuxeo.ecm.core.io.registry.context.RenderingContext.CtxBuilder;
import org.nuxeo.ecm.platform.comment.api.Annotation;
import org.nuxeo.ecm.platform.comment.api.AnnotationImpl;
import org.nuxeo.ecm.platform.comment.api.AnnotationService;
import org.nuxeo.ecm.platform.comment.api.ExternalEntity;
import org.nuxeo.ecm.platform.comment.impl.AnnotationJsonWriter;
import org.nuxeo.ecm.restapi.test.BaseTest;
import org.nuxeo.ecm.restapi.test.RestServerFeature;
import org.nuxeo.jaxrs.test.CloseableClientResponse;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.ServletContainer;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.inject.Inject;
import com.sun.jersey.core.util.MultivaluedMapImpl;

/**
 * @since 10.1
 */
@RunWith(FeaturesRunner.class)
@Features({ RestServerFeature.class })
@ServletContainer(port = 18090)
@Deploy("org.nuxeo.ecm.platform.comment.api")
@Deploy("org.nuxeo.ecm.platform.comment")
@Deploy("org.nuxeo.ecm.platform.comment.restapi")
@Deploy("org.nuxeo.ecm.platform.comment.restapi.test:OSGI-INF/comment-jena-contrib.xml")
@Deploy("org.nuxeo.ecm.relations.api")
@Deploy("org.nuxeo.ecm.relations")
@Deploy("org.nuxeo.ecm.relations.jena")
public class AnnotationAdapterTest extends BaseTest {

    @Inject
    protected AnnotationService annotationService;

    @Before
    public void setup() {
        DocumentModel domain = session.createDocumentModel("/", "testDomain", "Domain");
        session.createDocument(domain);
    }

    @Test
    public void testCreateAnnotation() throws IOException {
        DocumentModel file = session.createDocumentModel("/testDomain", "testDoc", "File");
        file = session.createDocument(file);

        fetchInvalidations();
        String xpath = "file:content";

        Annotation annotation = new AnnotationImpl();
        annotation.setDocumentId(file.getId());
        annotation.setXpath(xpath);

        String jsonAnnotation = MarshallerHelper.objectToJson(annotation, CtxBuilder.get());

        try (CloseableClientResponse response = getResponse(RequestType.POST, "id/" + file.getId() + "/@annotation",
                jsonAnnotation)) {
            assertEquals(Response.Status.CREATED.getStatusCode(), response.getStatus());
            JsonNode node = mapper.readTree(response.getEntityInputStream());
            assertEquals(file.getId(), node.get("documentId").textValue());
            assertEquals(xpath, node.get("xpath").textValue());
        }
    }

    @Test
    public void testGetAnnotation() throws IOException {
        DocumentModel file = session.createDocumentModel("/testDomain", "testDoc", "File");
        file = session.createDocument(file);
        String xpath = "files:files/0/file";

        Annotation annotation = new AnnotationImpl();
        annotation.setDocumentId(file.getId());
        annotation.setXpath(xpath);
        annotation = annotationService.createAnnotation(session, annotation);
        fetchInvalidations();

        JsonNode node = getResponseAsJson(RequestType.GET, "id/" + file.getId() + "/@annotation/" + annotation.getId());

        assertEquals(AnnotationJsonWriter.ENTITY_TYPE, node.get("entity-type").asText());
        assertEquals(file.getId(), node.get("documentId").textValue());
        assertEquals(xpath, node.get("xpath").textValue());
    }

    @Test
    @Ignore("Not possible with current comment manager implementation")
    public void testUpdateAnnotation() throws IOException {
        DocumentModel file = session.createDocumentModel("/testDomain", "testDoc", "File");
        file = session.createDocument(file);

        fetchInvalidations();
        String xpath = "file:content";

        Annotation annotation = new AnnotationImpl();
        annotation.setId("foo");
        annotation.setDocumentId(file.getId());
        annotation.setXpath(xpath);
        annotation = annotationService.createAnnotation(session, annotation);

        fetchInvalidations();

        String jsonAnnotation = MarshallerHelper.objectToJson(annotation, CtxBuilder.get());

        try (CloseableClientResponse response = getResponse(RequestType.PUT, "id/" + file.getId() + "/@annotation",
                jsonAnnotation)) {
            assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());

            fetchInvalidations();

            Annotation updatedAnnotation = annotationService.getAnnotation(session, annotation.getId());
        }
    }

    @Test
    public void testDeleteAnnotation() {
        DocumentModel file = session.createDocumentModel("/testDomain", "testDoc", "File");
        file = session.createDocument(file);
        String xpath = "files:files/0/file";

        Annotation annotation = new AnnotationImpl();
        annotation.setId("foo");
        annotation.setDocumentId(file.getId());
        annotation.setXpath(xpath);
        annotation = annotationService.createAnnotation(session, annotation);

        fetchInvalidations();

        assertTrue(session.exists(new IdRef(annotation.getId())));

        try (CloseableClientResponse response = getResponse(RequestType.DELETE,
                "id/" + file.getId() + "/@annotation/" + annotation.getId())) {
            assertEquals(Response.Status.NO_CONTENT.getStatusCode(), response.getStatus());
            fetchInvalidations();
            assertFalse(session.exists(new IdRef(annotation.getId())));
        }
    }

    @Test
    public void testSearchAnnotations() throws IOException {
        DocumentModel file1 = session.createDocumentModel("/testDomain", "testDoc1", "File");
        file1 = session.createDocument(file1);
        String xpath1 = "files:files/0/file";
        String xpath2 = "files:files/1/file";

        DocumentModel file2 = session.createDocumentModel("/testDomain", "testDoc2", "File");
        file2 = session.createDocument(file2);
        String xpath3 = "file:content";

        Annotation annotation1 = new AnnotationImpl();
        annotation1.setDocumentId(file1.getId());
        annotation1.setXpath(xpath1);
        Annotation annotation2 = new AnnotationImpl();
        annotation2.setDocumentId(file1.getId());
        annotation2.setXpath(xpath2);
        Annotation annotation3 = new AnnotationImpl();
        annotation3.setDocumentId(file1.getId());
        annotation3.setXpath(xpath2);
        Annotation annotation4 = new AnnotationImpl();
        annotation4.setDocumentId(file2.getId());
        annotation4.setXpath(xpath3);
        Annotation annotation5 = new AnnotationImpl();
        annotation5.setDocumentId(file2.getId());
        annotation5.setXpath(xpath3);

        annotation1 = annotationService.createAnnotation(session, annotation1);
        annotation2 = annotationService.createAnnotation(session, annotation2);
        annotation3 = annotationService.createAnnotation(session, annotation3);
        annotation4 = annotationService.createAnnotation(session, annotation4);
        annotation5 = annotationService.createAnnotation(session, annotation5);

        fetchInvalidations();

        MultivaluedMap<String, String> params1 = new MultivaluedMapImpl();
        params1.putSingle("xpath", xpath1);
        MultivaluedMap<String, String> params2 = new MultivaluedMapImpl();
        params2.putSingle("xpath", xpath2);
        MultivaluedMap<String, String> params3 = new MultivaluedMapImpl();
        params3.putSingle("xpath", xpath3);

        JsonNode node1 = getResponseAsJson(RequestType.GET, "id/" + file1.getId() + "/@annotation", params1).get(
                "entries");
        JsonNode node2 = getResponseAsJson(RequestType.GET, "id/" + file1.getId() + "/@annotation", params2).get(
                "entries");
        JsonNode node3 = getResponseAsJson(RequestType.GET, "id/" + file2.getId() + "/@annotation", params3).get(
                "entries");

        assertEquals(1, node1.size());
        assertEquals(2, node2.size());
        assertEquals(2, node3.size());

        assertEquals(annotation1.getId(), node1.get(0).get("id").textValue());

        List<String> node2List = Arrays.asList(node2.get(0).get("id").textValue(),
                node2.get(1).get("id").textValue());
        assertTrue(node2List.contains(annotation2.getId()));
        assertTrue(node2List.contains(annotation3.getId()));
        List<String> node3List = Arrays.asList(node3.get(0).get("id").textValue(),
                node3.get(1).get("id").textValue());
        assertTrue(node3List.contains(annotation4.getId()));
        assertTrue(node3List.contains(annotation5.getId()));
    }

    @Test
    public void testGetExternalAnnotation() throws IOException {
        DocumentModel file = session.createDocumentModel("/testDomain", "testDoc", "File");
        file = session.createDocument(file);
        String xpath = "files:files/0/file";

        String entityId = "foo";
        String origin = "origin";
        String entity = "<entity></entity>";

        Annotation annotation = new AnnotationImpl();
        ((ExternalEntity) annotation).setEntityId(entityId);
        ((ExternalEntity) annotation).setOrigin(origin);
        ((ExternalEntity) annotation).setEntity(entity);
        annotation.setDocumentId(file.getId());
        annotation.setXpath(xpath);
        annotation = annotationService.createAnnotation(session, annotation);
        fetchInvalidations();

        JsonNode node = getResponseAsJson(RequestType.GET, "id/" + file.getId() + "/@annotation/external/" + entityId);

        assertEquals(AnnotationJsonWriter.ENTITY_TYPE, node.get("entity-type").asText());
        assertEquals(entityId, node.get("entityId").textValue());
        assertEquals(origin, node.get("origin").textValue());
        assertEquals(entity, node.get("entity").textValue());
        assertEquals(file.getId(), node.get("documentId").textValue());
        assertEquals(xpath, node.get("xpath").textValue());
    }

    @Test
    public void testUpdateExternalAnnotation() throws IOException {
        DocumentModel file = session.createDocumentModel("/testDomain", "testDoc", "File");
        file = session.createDocument(file);

        fetchInvalidations();
        String xpath = "file:content";
        String entityId = "foo";

        Annotation annotation = new AnnotationImpl();
        ((ExternalEntity) annotation).setEntityId(entityId);
        annotation.setDocumentId(file.getId());
        annotation.setAuthor("toto");
        annotation.setXpath(xpath);
        annotation = annotationService.createAnnotation(session, annotation);

        fetchInvalidations();

        annotation.setAuthor("titi");
        String jsonAnnotation = MarshallerHelper.objectToJson(annotation, CtxBuilder.get());

        try (CloseableClientResponse response = getResponse(RequestType.PUT,
                "id/" + file.getId() + "/@annotation/external/" + entityId, jsonAnnotation)) {
            assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());

            fetchInvalidations();
            Annotation updatedAnnotation = annotationService.getExternalAnnotation(session, entityId);
            assertEquals("titi", updatedAnnotation.getAuthor());
        }
    }

    @Test
    public void testDeleteExternalAnnotation() {
        DocumentModel file = session.createDocumentModel("/testDomain", "testDoc", "File");
        file = session.createDocument(file);
        String xpath = "files:files/0/file";
        String entityId = "foo";

        Annotation annotation = new AnnotationImpl();
        ((ExternalEntity) annotation).setEntityId(entityId);
        annotation.setDocumentId(file.getId());
        annotation.setXpath(xpath);
        annotation = annotationService.createAnnotation(session, annotation);

        fetchInvalidations();

        assertTrue(session.exists(new IdRef(annotation.getId())));

        try (CloseableClientResponse response = getResponse(RequestType.DELETE,
                "id/" + file.getId() + "/@annotation/external/" + entityId)) {
            assertEquals(Response.Status.NO_CONTENT.getStatusCode(), response.getStatus());
            fetchInvalidations();
            assertFalse(session.exists(new IdRef(annotation.getId())));
        }
    }

}
