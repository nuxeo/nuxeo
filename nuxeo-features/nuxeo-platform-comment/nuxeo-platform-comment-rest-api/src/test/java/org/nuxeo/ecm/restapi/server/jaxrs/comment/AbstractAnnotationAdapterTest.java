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
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.nuxeo.ecm.platform.comment.CommentUtils.createUser;
import static org.nuxeo.ecm.platform.comment.CommentUtils.newAnnotation;
import static org.nuxeo.ecm.platform.comment.CommentUtils.newComment;
import static org.nuxeo.ecm.platform.comment.CommentUtils.newExternalAnnotation;
import static org.nuxeo.ecm.platform.comment.api.AnnotationConstants.ANNOTATION_PERMISSIONS;
import static org.nuxeo.ecm.platform.comment.api.AnnotationConstants.ANNOTATION_XPATH;
import static org.nuxeo.ecm.platform.comment.api.ExternalEntityConstants.EXTERNAL_ENTITY;
import static org.nuxeo.ecm.platform.comment.api.ExternalEntityConstants.EXTERNAL_ENTITY_ID;
import static org.nuxeo.ecm.platform.comment.api.ExternalEntityConstants.EXTERNAL_ENTITY_ORIGIN;
import static org.nuxeo.ecm.platform.comment.workflow.utils.CommentsConstants.COMMENT_ID_FIELD;
import static org.nuxeo.ecm.platform.comment.workflow.utils.CommentsConstants.COMMENT_PARENT_ID_FIELD;
import static org.nuxeo.ecm.platform.comment.workflow.utils.CommentsConstants.COMMENT_TEXT_FIELD;
import static org.nuxeo.ecm.restapi.server.jaxrs.comment.AbstractCommentAdapterTest.JDOE;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.core.api.security.PermissionProvider;
import org.nuxeo.ecm.core.io.registry.MarshallerHelper;
import org.nuxeo.ecm.core.io.registry.context.RenderingContext.CtxBuilder;
import org.nuxeo.ecm.platform.comment.api.Annotation;
import org.nuxeo.ecm.platform.comment.api.AnnotationService;
import org.nuxeo.ecm.platform.comment.api.Comment;
import org.nuxeo.ecm.platform.comment.api.CommentManager;
import org.nuxeo.ecm.platform.comment.impl.AnnotationJsonWriter;
import org.nuxeo.ecm.restapi.test.BaseTest;
import org.nuxeo.jaxrs.test.CloseableClientResponse;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.inject.Inject;
import com.sun.jersey.core.util.MultivaluedMapImpl;

/**
 * @since 10.1
 */
@RunWith(FeaturesRunner.class)
@Features(CommentAdapterFeature.class)
public abstract class AbstractAnnotationAdapterTest extends BaseTest {

    @Inject
    protected AnnotationService annotationService;

    @Inject
    protected CommentManager commentManager;

    protected DocumentModel file;

    @Before
    public void setup() {
        DocumentModel domain = session.createDocumentModel("/", "testDomain", "Domain");
        session.createDocument(domain);
        file = session.createDocumentModel("/testDomain", "testDoc", "File");
        file = session.createDocument(file);
        fetchInvalidations();
    }

    @Test
    public void testCreateAnnotation() throws IOException {
        String xpath = "file:content";
        Annotation annotation = newAnnotation(file.getId(), xpath);

        String jsonAnnotation = MarshallerHelper.objectToJson(annotation, CtxBuilder.session(session).get());

        try (CloseableClientResponse response = getResponse(RequestType.POST, "id/" + file.getId() + "/@annotation",
                jsonAnnotation)) {
            assertEquals(Response.Status.CREATED.getStatusCode(), response.getStatus());
            JsonNode node = mapper.readTree(response.getEntityInputStream());
            assertEquals(file.getId(), node.get(COMMENT_PARENT_ID_FIELD).textValue());
            assertEquals(xpath, node.get(ANNOTATION_XPATH).textValue());
        }
    }

    @Test
    public void testGetAnnotation() throws IOException {
        String xpath = "files:files/0/file";
        Annotation annotation = annotationService.createAnnotation(session, newAnnotation(file.getId(), xpath));
        fetchInvalidations();

        JsonNode node = getResponseAsJson(RequestType.GET, "id/" + file.getId() + "/@annotation/" + annotation.getId());

        assertEquals(AnnotationJsonWriter.ENTITY_TYPE, node.get("entity-type").asText());
        assertEquals(file.getId(), node.get(COMMENT_PARENT_ID_FIELD).textValue());
        assertEquals(xpath, node.get(ANNOTATION_XPATH).textValue());

        // Get permissions
        Set<String> grantedPermissions = new HashSet<>(session.filterGrantedPermissions(session.getPrincipal(),
                file.getRef(), Arrays.asList(Framework.getService(PermissionProvider.class)
                                                      .getPermissions())));
        Set<String> permissions = StreamSupport.stream(node.get(ANNOTATION_PERMISSIONS).spliterator(), false)
                                               .map(JsonNode::textValue)
                                               .collect(Collectors.toSet());

        assertEquals(grantedPermissions, permissions);
    }

    @Test
    public void testUpdateAnnotation() throws IOException {
        String xpath = "file:content";
        Annotation annotation = annotationService.createAnnotation(session, newAnnotation(file.getId(), xpath));

        fetchInvalidations();

        assertNull(annotation.getText());
        annotation.setText("test");
        annotation.setAuthor("fakeAuthor");
        String jsonAnnotation = MarshallerHelper.objectToJson(annotation, CtxBuilder.session(session).get());

        try (CloseableClientResponse response = getResponse(RequestType.PUT,
                "id/" + file.getId() + "/@annotation/" + annotation.getId(), jsonAnnotation)) {
            assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());

            fetchInvalidations();

            Annotation updatedAnnotation = annotationService.getAnnotation(session, annotation.getId());
            assertEquals("test", updatedAnnotation.getText());
            assertEquals(session.getPrincipal().getName(), updatedAnnotation.getAuthor());
        }
    }

    /*
     * NXP-28483
     */
    @Test
    public void testUpdateAnnotationWithRegularUser() throws IOException {
        // create jdoe user as a regular user
        createUser(JDOE);
        // re-compute read acls
        fetchInvalidations();
        // use it in rest calls
        service = getServiceFor(JDOE, JDOE);

        String annotationId;
        // use rest for creation in order to have the correct author
        Annotation annotation = newAnnotation(file.getId(), "file:content", "Some text");
        String jsonComment = MarshallerHelper.objectToJson(annotation, CtxBuilder.session(session).get());
        try (CloseableClientResponse response = getResponse(RequestType.POST, "id/" + file.getId() + "/@annotation",
                jsonComment)) {
            assertEquals(Response.Status.CREATED.getStatusCode(), response.getStatus());
            JsonNode node = mapper.readTree(response.getEntityInputStream());
            annotationId = node.get(COMMENT_ID_FIELD).textValue();
        }

        // now update the annotation
        annotation.setText("And now I update it");
        jsonComment = MarshallerHelper.objectToJson(annotation, CtxBuilder.session(session).get());
        try (CloseableClientResponse response = getResponse(RequestType.PUT,
                "id/" + file.getId() + "/@annotation/" + annotationId, jsonComment)) {
            assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
            JsonNode node = mapper.readTree(response.getEntityInputStream());
            // assert the response
            assertEquals("And now I update it", node.get(COMMENT_TEXT_FIELD).textValue());
            fetchInvalidations();
            // assert DB was updated
            Annotation updatedAnnotation = annotationService.getAnnotation(session, annotationId);
            assertEquals("And now I update it", updatedAnnotation.getText());
        }
    }

    @Test
    public void testDeleteAnnotation() {
        String xpath = "files:files/0/file";
        Annotation annotation = annotationService.createAnnotation(session, newAnnotation(file.getId(), xpath));
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
        DocumentModel file2 = session.createDocumentModel("/testDomain", "testDoc2", "File");
        file2 = session.createDocument(file2);

        String xpath1 = "files:files/0/file";
        String xpath2 = "files:files/1/file";
        String xpath3 = "file:content";
        Annotation annotation1 = annotationService.createAnnotation(session, newAnnotation(file1.getId(), xpath1));
        Annotation annotation2 = annotationService.createAnnotation(session, newAnnotation(file1.getId(), xpath2));
        Annotation annotation3 = annotationService.createAnnotation(session, newAnnotation(file1.getId(), xpath2));
        Annotation annotation4 = annotationService.createAnnotation(session, newAnnotation(file2.getId(), xpath3));
        Annotation annotation5 = annotationService.createAnnotation(session, newAnnotation(file2.getId(), xpath3));
        fetchInvalidations();

        MultivaluedMap<String, String> params1 = new MultivaluedMapImpl();
        params1.putSingle(ANNOTATION_XPATH, xpath1);
        MultivaluedMap<String, String> params2 = new MultivaluedMapImpl();
        params2.putSingle(ANNOTATION_XPATH, xpath2);
        MultivaluedMap<String, String> params3 = new MultivaluedMapImpl();
        params3.putSingle(ANNOTATION_XPATH, xpath3);

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
        String xpath = "files:files/0/file";
        String entityId = "foo";
        String entity = "<entity></entity>";
        annotationService.createAnnotation(session, newExternalAnnotation(file.getId(), xpath, entityId, entity));
        fetchInvalidations();

        JsonNode node = getResponseAsJson(RequestType.GET, "id/" + file.getId() + "/@annotation/external/" + entityId);

        assertEquals(AnnotationJsonWriter.ENTITY_TYPE, node.get("entity-type").asText());
        assertEquals(entityId, node.get(EXTERNAL_ENTITY_ID).textValue());
        assertEquals("Test", node.get(EXTERNAL_ENTITY_ORIGIN).textValue());
        assertEquals(entity, node.get(EXTERNAL_ENTITY).textValue());
        assertEquals(file.getId(), node.get(COMMENT_PARENT_ID_FIELD).textValue());
        assertEquals(xpath, node.get(ANNOTATION_XPATH).textValue());
    }

    @Test
    public void testUpdateExternalAnnotation() throws IOException {
        String xpath = "file:content";
        String entityId = "foo";
        String author = "toto";
        Annotation annotation = newExternalAnnotation(file.getId(), xpath, entityId);
        annotation.setAuthor(author);
        annotation = annotationService.createAnnotation(session, annotation);

        fetchInvalidations();

        annotation.setAuthor("titi");
        String jsonAnnotation = MarshallerHelper.objectToJson(annotation, CtxBuilder.session(session).get());

        try (CloseableClientResponse response = getResponse(RequestType.PUT,
                "id/" + file.getId() + "/@annotation/external/" + entityId, jsonAnnotation)) {
            assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());

            fetchInvalidations();
            Annotation updatedAnnotation = annotationService.getExternalAnnotation(session, file.getId(), entityId);
            assertEquals(author, updatedAnnotation.getAuthor());
        }
    }

    /*
     * NXP-28483
     */
    @Test
    public void testUpdateExternalAnnotationWithRegularUser() throws IOException {
        // create jdoe user as a regular user
        createUser(JDOE);
        // re-compute read acls
        fetchInvalidations();
        // use it in rest calls
        service = getServiceFor(JDOE, JDOE);

        String entityId = "foo";
        // use rest for creation in order to have the correct author
        Annotation annotation = newExternalAnnotation(file.getId(), "file:content", entityId);
        annotation.setText("Some text");
        String jsonComment = MarshallerHelper.objectToJson(annotation, CtxBuilder.session(session).get());
        try (CloseableClientResponse response = getResponse(RequestType.POST, "id/" + file.getId() + "/@annotation",
                jsonComment)) {
            assertEquals(Response.Status.CREATED.getStatusCode(), response.getStatus());
        }

        // now update the annotation
        annotation.setText("And now I update it");
        jsonComment = MarshallerHelper.objectToJson(annotation, CtxBuilder.session(session).get());
        try (CloseableClientResponse response = getResponse(RequestType.PUT,
                "id/" + file.getId() + "/@annotation/external/" + entityId, jsonComment)) {
            assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
            JsonNode node = mapper.readTree(response.getEntityInputStream());
            // assert the response
            assertEquals("And now I update it", node.get(COMMENT_TEXT_FIELD).textValue());
            fetchInvalidations();
            // assert DB was updated
            Annotation updatedAnnotation = annotationService.getExternalAnnotation(session, file.getId(), entityId);
            assertEquals("And now I update it", updatedAnnotation.getText());
        }
    }

    @Test
    public void testDeleteExternalAnnotation() {
        String xpath = "files:files/0/file";
        String entityId = "foo";
        Annotation annotation = annotationService.createAnnotation(session,
                newExternalAnnotation(file.getId(), xpath, entityId));
        fetchInvalidations();

        assertTrue(session.exists(new IdRef(annotation.getId())));

        try (CloseableClientResponse response = getResponse(RequestType.DELETE,
                "id/" + file.getId() + "/@annotation/external/" + entityId)) {
            assertEquals(Response.Status.NO_CONTENT.getStatusCode(), response.getStatus());
            fetchInvalidations();
            assertFalse(session.exists(new IdRef(annotation.getId())));
        }
    }

    @Test
    public void testGetCommentsOfAnnotations() throws IOException {
        Annotation annotation1 = annotationService.createAnnotation(session,
                newAnnotation(file.getId(), "file:content"));
        Annotation annotation2 = annotationService.createAnnotation(session,
                newAnnotation(file.getId(), "file:content"));
        fetchInvalidations();

        List<String> commentIds = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            Comment comment = newComment(i % 2 == 0 ? annotation1.getId() : annotation2.getId());
            comment = commentManager.createComment(session, comment);
            commentIds.add(comment.getId());

            Comment subComment = newComment(comment.getId());
            subComment = commentManager.createComment(session, subComment);
            commentIds.add(subComment.getId());
        }
        fetchInvalidations();

        MultivaluedMap<String, String> annotationIds = new MultivaluedMapImpl();
        annotationIds.put("annotationIds", Arrays.asList(annotation1.getId(), annotation2.getId()));
        JsonNode node = getResponseAsJson(RequestType.GET, "id/" + file.getId() + "/@annotation/comments",
                annotationIds);
        Set<String> expectedIds = new HashSet<>(commentIds);
        Set<String> actualIds = new HashSet<>(node.findValuesAsText("id"));
        assertEquals(10, actualIds.size());
        assertEquals(expectedIds, actualIds);
    }

}
