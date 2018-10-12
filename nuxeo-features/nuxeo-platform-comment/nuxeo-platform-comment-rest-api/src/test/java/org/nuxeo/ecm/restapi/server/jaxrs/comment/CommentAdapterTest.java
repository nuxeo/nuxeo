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
 *       Kevin Leturc <kleturc@nuxeo.com>
 *       Nuno Cunha <ncunha@nuxeo.com>
 */

package org.nuxeo.ecm.restapi.server.jaxrs.comment;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.nuxeo.ecm.platform.comment.api.ExternalEntityConstants.EXTERNAL_ENTITY;
import static org.nuxeo.ecm.platform.comment.api.ExternalEntityConstants.EXTERNAL_ENTITY_ID;
import static org.nuxeo.ecm.platform.comment.api.ExternalEntityConstants.EXTERNAL_ENTITY_ORIGIN;
import static org.nuxeo.ecm.platform.comment.workflow.utils.CommentsConstants.COMMENT_AUTHOR_FIELD;
import static org.nuxeo.ecm.platform.comment.workflow.utils.CommentsConstants.COMMENT_CREATION_DATE_FIELD;
import static org.nuxeo.ecm.platform.comment.workflow.utils.CommentsConstants.COMMENT_ENTITY_TYPE;
import static org.nuxeo.ecm.platform.comment.workflow.utils.CommentsConstants.COMMENT_ID_FIELD;
import static org.nuxeo.ecm.platform.comment.workflow.utils.CommentsConstants.COMMENT_PARENT_ID_FIELD;
import static org.nuxeo.ecm.platform.comment.workflow.utils.CommentsConstants.COMMENT_TEXT_FIELD;

import java.io.IOException;
import java.time.Instant;
import java.util.HashSet;
import java.util.Set;

import javax.ws.rs.core.Response;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.core.io.registry.MarshallerHelper;
import org.nuxeo.ecm.core.io.registry.context.RenderingContext.CtxBuilder;
import org.nuxeo.ecm.platform.comment.api.Comment;
import org.nuxeo.ecm.platform.comment.api.CommentImpl;
import org.nuxeo.ecm.platform.comment.api.CommentManager;
import org.nuxeo.ecm.platform.comment.api.ExternalEntity;
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
 * @since 10.3
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
public class CommentAdapterTest extends BaseTest {

    @Inject
    protected CommentManager commentManager;

    @Before
    public void setup() {
        DocumentModel domain = session.createDocumentModel("/", "testDomain", "Domain");
        session.createDocument(domain);
    }

    @Test
    public void testCreateComment() throws IOException {
        DocumentModel file = session.createDocumentModel("/testDomain", "testDoc", "File");
        file = session.createDocument(file);
        fetchInvalidations();

        Comment comment = instantiateComment(file.getId());

        String jsonComment = MarshallerHelper.objectToJson(comment, CtxBuilder.get());

        try (CloseableClientResponse response = getResponse(RequestType.POST, "id/" + file.getId() + "/@comment",
                jsonComment)) {
            assertEquals(Response.Status.CREATED.getStatusCode(), response.getStatus());
            JsonNode node = mapper.readTree(response.getEntityInputStream());
            assertEquals(file.getId(), node.get(COMMENT_PARENT_ID_FIELD).textValue());
            assertEquals(comment.getAuthor(), node.get(COMMENT_AUTHOR_FIELD).textValue());
            assertEquals(comment.getText(), node.get(COMMENT_TEXT_FIELD).textValue());
        }
    }

    @Test
    public void testGetCommentsForNonExistingDocument() {
        try (CloseableClientResponse response = getResponse(RequestType.GET, "id/nonExistingDocId/@comment",
                new MultivaluedMapImpl())) {
            assertEquals(Response.Status.NOT_FOUND.getStatusCode(), response.getStatus());
        }
    }

    @Test
    public void testGetComments() throws IOException {
        DocumentModel file = session.createDocumentModel("/testDomain", "testDoc", "File");
        file = session.createDocument(file);

        fetchInvalidations();

        Comment comment1 = createComment(file.getId());
        Comment comment2 = createComment(file.getId());
        Comment comment3 = createComment(file.getId());
        Comment comment4 = createComment(file.getId());
        Comment comment5 = createComment(file.getId());

        String comment1Id = comment1.getId();
        String comment2Id = comment2.getId();
        String comment3Id = comment3.getId();
        String comment4Id = comment4.getId();
        String comment5Id = comment5.getId();

        fetchInvalidations();

        // test without pagination
        JsonNode entries = getResponseAsJson(RequestType.GET, "id/" + file.getId() + "/@comment",
                new MultivaluedMapImpl()).get("entries");
        assertEquals(5, entries.size());
        Set<String> expectedIds = new HashSet<>(asList(comment1Id, comment2Id, comment3Id, comment4Id, comment5Id));
        Set<String> actualIds = new HashSet<>(entries.findValuesAsText("id"));
        assertEquals(expectedIds, actualIds);

        // test with pagination
        MultivaluedMapImpl queryParamsPage1 = new MultivaluedMapImpl();
        queryParamsPage1.add("pageSize", 2);
        queryParamsPage1.add("currentPageIndex", 0);
        JsonNode entriesPage1 = getResponseAsJson(RequestType.GET, "id/" + file.getId() + "/@comment",
                queryParamsPage1).get("entries");
        expectedIds = new HashSet<>(asList(comment5Id, comment4Id));
        actualIds = new HashSet<>(entriesPage1.findValuesAsText("id"));
        assertEquals(expectedIds, actualIds);

        MultivaluedMapImpl queryParamsPage2 = new MultivaluedMapImpl();
        queryParamsPage2.add("pageSize", 2);
        queryParamsPage2.add("currentPageIndex", 1);
        JsonNode entriesPage2 = getResponseAsJson(RequestType.GET, "id/" + file.getId() + "/@comment",
                queryParamsPage2).get("entries");
        expectedIds = new HashSet<>(asList(comment3Id, comment2Id));
        actualIds = new HashSet<>(entriesPage2.findValuesAsText("id"));
        assertEquals(expectedIds, actualIds);

        MultivaluedMapImpl queryParamsPage3 = new MultivaluedMapImpl();
        queryParamsPage3.add("pageSize", 2);
        queryParamsPage3.add("currentPageIndex", 2);
        JsonNode entriesPage3 = getResponseAsJson(RequestType.GET, "id/" + file.getId() + "/@comment",
                queryParamsPage3).get("entries");
        expectedIds = new HashSet<>(singletonList(comment1Id));
        actualIds = new HashSet<>(entriesPage3.findValuesAsText("id"));
        assertEquals(expectedIds, actualIds);
    }

    @Test
    public void testGetCommentWithNonExistingId() {
        DocumentModel file = session.createDocumentModel("/testDomain", "testDoc", "File");
        file = session.createDocument(file);
        fetchInvalidations();

        try (CloseableClientResponse response = getResponse(RequestType.GET,
                "id/" + file.getId() + "/@comment/" + "nonExistingId")) {
            assertEquals(Response.Status.NOT_FOUND.getStatusCode(), response.getStatus());
        }
    }

    @Test
    public void testGetComment() throws IOException {
        DocumentModel file = session.createDocumentModel("/testDomain", "testDoc", "File");
        file = session.createDocument(file);
        fetchInvalidations();

        Comment comment = createComment(file.getId());
        String commentId = comment.getId();
        fetchInvalidations();

        JsonNode node = getResponseAsJson(RequestType.GET, "id/" + file.getId() + "/@comment/" + commentId);

        assertEquals(COMMENT_ENTITY_TYPE, node.get("entity-type").asText());
        assertEquals(comment.getId(), node.get(COMMENT_ID_FIELD).textValue());
        assertEquals(file.getId(), node.get(COMMENT_PARENT_ID_FIELD).textValue());
        assertEquals(comment.getAuthor(), node.get(COMMENT_AUTHOR_FIELD).textValue());
        assertEquals(comment.getText(), node.get(COMMENT_TEXT_FIELD).textValue());
        assertEquals(comment.getCreationDate().toString(), node.get(COMMENT_CREATION_DATE_FIELD).textValue());
    }

    @Test
    public void testUpdateComment() throws IOException {
        DocumentModel file = session.createDocumentModel("/testDomain", "testDoc", "File");
        file = session.createDocument(file);
        fetchInvalidations();

        Comment comment = createComment(file.getId());
        String commentId = comment.getId();
        fetchInvalidations();

        comment.setText("And now I update it");
        String jsonComment = MarshallerHelper.objectToJson(comment, CtxBuilder.get());

        try (CloseableClientResponse response = getResponse(RequestType.PUT,
                "id/" + file.getId() + "/@comment/" + commentId, jsonComment)) {
            assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());

            fetchInvalidations();

            Comment updatedComment = commentManager.getComment(session, commentId);
            assertEquals("And now I update it", updatedComment.getText());
        }
    }

    @Test
    public void testDeleteCommentWithNonExistingId() {
        DocumentModel file = session.createDocumentModel("/testDomain", "testDoc", "File");
        file = session.createDocument(file);
        fetchInvalidations();

        try (CloseableClientResponse response = getResponse(RequestType.GET,
                "id/" + file.getId() + "/@comment/" + "nonExistingId")) {
            assertEquals(Response.Status.NOT_FOUND.getStatusCode(), response.getStatus());
        }
    }

    @Test
    public void testDeleteComment() {
        DocumentModel file = session.createDocumentModel("/testDomain", "testDoc", "File");
        file = session.createDocument(file);
        fetchInvalidations();

        Comment comment = createComment(file.getId());
        String commentId = comment.getId();
        fetchInvalidations();

        assertNotNull(commentManager.getComment(session, commentId));

        try (CloseableClientResponse response = getResponse(RequestType.DELETE,
                "id/" + file.getId() + "/@comment/" + commentId)) {
            assertEquals(Response.Status.NO_CONTENT.getStatusCode(), response.getStatus());
            fetchInvalidations();
            assertFalse(session.exists(new IdRef(commentId)));
        }
    }

    @Test
    public void testGetExternalComment() throws IOException {
        DocumentModel file = session.createDocumentModel("/testDomain", "testDoc", "File");
        file = session.createDocument(file);

        String entityId = "foo";
        String origin = "origin";
        String entity = "<entity></entity>";

        Comment comment = new CommentImpl();
        ((ExternalEntity) comment).setEntityId(entityId);
        ((ExternalEntity) comment).setOrigin(origin);
        ((ExternalEntity) comment).setEntity(entity);
        comment.setParentId(file.getId());
        comment = commentManager.createComment(session, comment);
        fetchInvalidations();

        JsonNode node = getResponseAsJson(RequestType.GET, "id/" + file.getId() + "/@comment/external/" + entityId);

        assertEquals(COMMENT_ENTITY_TYPE, node.get("entity-type").asText());
        assertEquals(comment.getId(), node.get(COMMENT_ID_FIELD).textValue());
        assertEquals(file.getId(), node.get(COMMENT_PARENT_ID_FIELD).textValue());
        assertEquals(entityId, node.get(EXTERNAL_ENTITY_ID).textValue());
        assertEquals(origin, node.get(EXTERNAL_ENTITY_ORIGIN).textValue());
        assertEquals(entity, node.get(EXTERNAL_ENTITY).textValue());
    }

    @Test
    public void testUpdateExternalComment() throws IOException {
        DocumentModel file = session.createDocumentModel("/testDomain", "testDoc", "File");
        file = session.createDocument(file);

        fetchInvalidations();
        String entityId = "foo";

        Comment comment = new CommentImpl();
        ((ExternalEntity) comment).setEntityId(entityId);
        comment.setParentId(file.getId());
        comment.setAuthor("toto");
        comment = commentManager.createComment(session, comment);

        fetchInvalidations();

        comment.setAuthor("titi");
        String jsonComment = MarshallerHelper.objectToJson(comment, CtxBuilder.get());

        try (CloseableClientResponse response = getResponse(RequestType.PUT,
                "id/" + file.getId() + "/@comment/external/" + entityId, jsonComment)) {
            assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());

            fetchInvalidations();
            Comment updatedComment = commentManager.getExternalComment(session, entityId);
            assertEquals("titi", updatedComment.getAuthor());
        }
    }

    @Test
    public void testDeleteExternalComment() {
        DocumentModel file = session.createDocumentModel("/testDomain", "testDoc", "File");
        file = session.createDocument(file);
        String entityId = "foo";

        Comment comment = instantiateComment(file.getId());
        ((ExternalEntity) comment).setEntityId(entityId);
        comment = commentManager.createComment(session, comment);

        fetchInvalidations();

        assertTrue(session.exists(new IdRef(comment.getId())));

        try (CloseableClientResponse response = getResponse(RequestType.DELETE,
                "id/" + file.getId() + "/@comment/external/" + entityId)) {
            assertEquals(Response.Status.NO_CONTENT.getStatusCode(), response.getStatus());
            fetchInvalidations();
            assertFalse(session.exists(new IdRef(comment.getId())));
        }
    }

    protected Comment createComment(String documentId) {
        return commentManager.createComment(session, instantiateComment(documentId));
    }

    protected Comment instantiateComment(String documentId) {
        Comment comment = new CommentImpl();
        comment.setParentId(documentId);
        comment.setAuthor(session.getPrincipal().getName());
        comment.setText("Here my wonderful comment on " + documentId + "!");
        comment.setCreationDate(Instant.now());
        comment.setModificationDate(Instant.now());
        return comment;
    }

}
