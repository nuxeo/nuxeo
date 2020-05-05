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
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.nuxeo.ecm.platform.comment.CommentUtils.createUser;
import static org.nuxeo.ecm.platform.comment.CommentUtils.newComment;
import static org.nuxeo.ecm.platform.comment.CommentUtils.newExternalComment;
import static org.nuxeo.ecm.platform.comment.api.ExternalEntityConstants.EXTERNAL_ENTITY;
import static org.nuxeo.ecm.platform.comment.api.ExternalEntityConstants.EXTERNAL_ENTITY_ID;
import static org.nuxeo.ecm.platform.comment.api.ExternalEntityConstants.EXTERNAL_ENTITY_ORIGIN;
import static org.nuxeo.ecm.platform.comment.workflow.utils.CommentsConstants.COMMENT_AUTHOR_FIELD;
import static org.nuxeo.ecm.platform.comment.workflow.utils.CommentsConstants.COMMENT_CREATION_DATE_FIELD;
import static org.nuxeo.ecm.platform.comment.workflow.utils.CommentsConstants.COMMENT_ENTITY_TYPE;
import static org.nuxeo.ecm.platform.comment.workflow.utils.CommentsConstants.COMMENT_ID_FIELD;
import static org.nuxeo.ecm.platform.comment.workflow.utils.CommentsConstants.COMMENT_LAST_REPLY_DATE;
import static org.nuxeo.ecm.platform.comment.workflow.utils.CommentsConstants.COMMENT_MODIFICATION_DATE_FIELD;
import static org.nuxeo.ecm.platform.comment.workflow.utils.CommentsConstants.COMMENT_NUMBER_OF_REPLIES;
import static org.nuxeo.ecm.platform.comment.workflow.utils.CommentsConstants.COMMENT_PARENT_ID_FIELD;
import static org.nuxeo.ecm.platform.comment.workflow.utils.CommentsConstants.COMMENT_PERMISSIONS;
import static org.nuxeo.ecm.platform.comment.workflow.utils.CommentsConstants.COMMENT_TEXT_FIELD;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
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
import org.nuxeo.ecm.platform.comment.api.Comment;
import org.nuxeo.ecm.platform.comment.api.CommentManager;
import org.nuxeo.ecm.platform.comment.impl.CommentJsonWriter;
import org.nuxeo.ecm.restapi.test.BaseTest;
import org.nuxeo.jaxrs.test.CloseableClientResponse;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.inject.Inject;
import com.sun.jersey.core.util.MultivaluedMapImpl;

/**
 * @since 10.3
 */
@RunWith(FeaturesRunner.class)
@Features(CommentAdapterFeature.class)
public abstract class AbstractCommentAdapterTest extends BaseTest {

    protected static final String JDOE = "jdoe";

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
    public void testCreateComment() throws IOException {
        Comment comment = newComment(file.getId(), "Some text");

        String jsonComment = MarshallerHelper.objectToJson(comment, CtxBuilder.session(session).get());

        try (CloseableClientResponse response = getResponse(RequestType.POST, "id/" + file.getId() + "/@comment",
                jsonComment)) {
            assertEquals(Response.Status.CREATED.getStatusCode(), response.getStatus());
            JsonNode node = mapper.readTree(response.getEntityInputStream());
            assertEquals(file.getId(), node.get(COMMENT_PARENT_ID_FIELD).textValue());
            assertEquals(session.getPrincipal().getName(), node.get(COMMENT_AUTHOR_FIELD).textValue());
            assertEquals("Some text", node.get(COMMENT_TEXT_FIELD).textValue());
        }
    }

    /**
     * @since 11.1
     */
    @Test
    public void testCreateCommentSetCorrectAuthor() throws IOException {
        Comment comment = newComment(file.getId());
        String fakeAuthor = "fakeAuthor";
        comment.setAuthor(fakeAuthor);

        String jsonComment = MarshallerHelper.objectToJson(comment, CtxBuilder.session(session).get());

        try (CloseableClientResponse response = getResponse(RequestType.POST, "id/" + file.getId() + "/@comment",
                jsonComment)) {
            assertEquals(Response.Status.CREATED.getStatusCode(), response.getStatus());
            JsonNode node = mapper.readTree(response.getEntityInputStream());
            String author = node.get(COMMENT_AUTHOR_FIELD).textValue();
            assertNotEquals(fakeAuthor, author);
            assertEquals(session.getPrincipal().getName(), author);
        }
    }

    @Test
    public void testCreateCommentWithoutCreationDate() throws IOException {
        Comment comment = newComment(file.getId());
        comment.setCreationDate(null);

        String jsonComment = MarshallerHelper.objectToJson(comment, CtxBuilder.session(session).get());

        try (CloseableClientResponse response = getResponse(RequestType.POST, "id/" + file.getId() + "/@comment",
                jsonComment)) {
            assertEquals(Response.Status.CREATED.getStatusCode(), response.getStatus());
            JsonNode node = mapper.readTree(response.getEntityInputStream());
            assertEquals(file.getId(), node.get(COMMENT_PARENT_ID_FIELD).textValue());
            assertEquals(session.getPrincipal().getName(), node.get(COMMENT_AUTHOR_FIELD).textValue());
            assertEquals(comment.getText(), node.get(COMMENT_TEXT_FIELD).textValue());
            assertNotNull(node.get(COMMENT_CREATION_DATE_FIELD).textValue());
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
        Comment comment1 = commentManager.createComment(session, newComment(file.getId()));
        Comment comment2 = commentManager.createComment(session, newComment(file.getId()));
        Comment comment3 = commentManager.createComment(session, newComment(file.getId()));
        Comment comment4 = commentManager.createComment(session, newComment(file.getId()));
        Comment comment5 = commentManager.createComment(session, newComment(file.getId()));

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
        try (CloseableClientResponse response = getResponse(RequestType.GET,
                "id/" + file.getId() + "/@comment/" + "nonExistingId")) {
            assertEquals(Response.Status.NOT_FOUND.getStatusCode(), response.getStatus());
        }
    }

    @Test
    public void testGetComment() throws IOException {
        Comment comment = commentManager.createComment(session, newComment(file.getId(), "Some text"));
        String commentId = comment.getId();
        fetchInvalidations();

        JsonNode node = getResponseAsJson(RequestType.GET, "id/" + file.getId() + "/@comment/" + commentId);

        assertEquals(COMMENT_ENTITY_TYPE, node.get("entity-type").asText());
        assertEquals(comment.getId(), node.get(COMMENT_ID_FIELD).textValue());
        assertEquals(file.getId(), node.get(COMMENT_PARENT_ID_FIELD).textValue());
        assertEquals(comment.getAuthor(), node.get(COMMENT_AUTHOR_FIELD).textValue());
        assertEquals("Some text", node.get(COMMENT_TEXT_FIELD).textValue());
        assertEquals(comment.getCreationDate().toString(), node.get(COMMENT_CREATION_DATE_FIELD).textValue());

        // Get permissions
        Set<String> grantedPermissions = new HashSet<>(session.filterGrantedPermissions(session.getPrincipal(),
                file.getRef(), Arrays.asList(Framework.getService(PermissionProvider.class).getPermissions())));
        Set<String> permissions = StreamSupport.stream(node.get(COMMENT_PERMISSIONS).spliterator(), false)
                                               .map(JsonNode::textValue)
                                               .collect(Collectors.toSet());

        assertEquals(grantedPermissions, permissions);
    }

    @Test
    public void testGetCommentWithoutRepliesUsingRepliesFetcher() throws IOException {
        Comment comment = commentManager.createComment(session, newComment(file.getId(), "Some text"));
        String commentId = comment.getId();
        fetchInvalidations();

        MultivaluedMap<String, String> queryParams = new MultivaluedMapImpl();
        queryParams.putSingle("fetch." + COMMENT_ENTITY_TYPE, CommentJsonWriter.FETCH_REPLIES_SUMMARY);
        JsonNode node = getResponseAsJson(RequestType.GET, "id/" + file.getId() + "/@comment/" + commentId,
                queryParams);

        assertEquals(COMMENT_ENTITY_TYPE, node.get("entity-type").asText());
        assertEquals(comment.getId(), node.get(COMMENT_ID_FIELD).textValue());
        assertEquals(file.getId(), node.get(COMMENT_PARENT_ID_FIELD).textValue());
        assertEquals(comment.getAuthor(), node.get(COMMENT_AUTHOR_FIELD).textValue());
        assertEquals("Some text", node.get(COMMENT_TEXT_FIELD).textValue());
        assertEquals(comment.getCreationDate().toString(), node.get(COMMENT_CREATION_DATE_FIELD).textValue());
        assertEquals(0, node.get(COMMENT_NUMBER_OF_REPLIES).intValue());
        assertFalse(node.has(COMMENT_LAST_REPLY_DATE));
    }

    @Test
    public void testGetCommentWithRepliesUsingRepliesFetcher() throws IOException {
        Comment comment = commentManager.createComment(session, newComment(file.getId(), "Some text"));
        String commentId = comment.getId();
        fetchInvalidations();

        commentManager.createComment(session, newComment(commentId));
        commentManager.createComment(session, newComment(commentId));
        Comment reply3 = commentManager.createComment(session, newComment(commentId));
        fetchInvalidations();

        MultivaluedMap<String, String> queryParams = new MultivaluedMapImpl();
        queryParams.putSingle("fetch." + COMMENT_ENTITY_TYPE, CommentJsonWriter.FETCH_REPLIES_SUMMARY);
        JsonNode node = getResponseAsJson(RequestType.GET, "id/" + file.getId() + "/@comment/" + commentId,
                queryParams);

        assertEquals(COMMENT_ENTITY_TYPE, node.get("entity-type").asText());
        assertEquals(comment.getId(), node.get(COMMENT_ID_FIELD).textValue());
        assertEquals(file.getId(), node.get(COMMENT_PARENT_ID_FIELD).textValue());
        assertEquals(comment.getAuthor(), node.get(COMMENT_AUTHOR_FIELD).textValue());
        assertEquals("Some text", node.get(COMMENT_TEXT_FIELD).textValue());
        assertEquals(comment.getCreationDate().toString(), node.get(COMMENT_CREATION_DATE_FIELD).textValue());
        assertEquals(3, node.get(COMMENT_NUMBER_OF_REPLIES).intValue());
        assertTrue(node.has(COMMENT_LAST_REPLY_DATE));
        assertEquals(reply3.getCreationDate().toString(), node.get(COMMENT_LAST_REPLY_DATE).textValue());
    }

    @Test
    public void testUpdateComment() throws IOException {
        Comment comment = commentManager.createComment(session, newComment(file.getId()));
        String author = comment.getAuthor();
        String commentId = comment.getId();
        fetchInvalidations();

        comment.setText("And now I update it");
        comment.setAuthor("fakeAuthor");
        String jsonComment = MarshallerHelper.objectToJson(comment, CtxBuilder.session(session).get());

        try (CloseableClientResponse response = getResponse(RequestType.PUT,
                "id/" + file.getId() + "/@comment/" + commentId, jsonComment)) {
            assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());

            fetchInvalidations();

            Comment updatedComment = commentManager.getComment(session, commentId);
            assertEquals("And now I update it", updatedComment.getText());
            assertEquals(author, updatedComment.getAuthor());
        }
    }

    @Test
    public void testUpdateCommentWithoutModificationDate() throws IOException {
        Comment comment = commentManager.createComment(session, newComment(file.getId()));
        String commentId = comment.getId();
        fetchInvalidations();

        comment.setText("And now I update it");
        comment.setModificationDate(null);
        String jsonComment = MarshallerHelper.objectToJson(comment, CtxBuilder.session(session).get());

        try (CloseableClientResponse response = getResponse(RequestType.PUT,
                "id/" + file.getId() + "/@comment/" + commentId, jsonComment)) {
            assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());

            JsonNode node = mapper.readTree(response.getEntityInputStream());
            assertEquals("And now I update it", node.get(COMMENT_TEXT_FIELD).textValue());
            assertNotNull(node.get(COMMENT_MODIFICATION_DATE_FIELD).textValue());
        }
    }

    /*
     * NXP-28484
     */
    @Test
    public void testUpdateCommentWithPartialData() throws IOException {
        Comment comment = commentManager.createComment(session,
                newExternalComment(file.getId(), "an-id", "<entity/>", "Some text"));
        String commentId = comment.getId();
        fetchInvalidations();

        String jsonComment = String.format("{\"entity-type\":\"%s\",\"%s\":\"And now I update it\"}",
                COMMENT_ENTITY_TYPE, COMMENT_TEXT_FIELD);

        try (CloseableClientResponse response = getResponse(RequestType.PUT,
                "id/" + file.getId() + "/@comment/" + commentId, jsonComment)) {
            assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());

            JsonNode node = mapper.readTree(response.getEntityInputStream());
            assertEquals("And now I update it", node.get(COMMENT_TEXT_FIELD).textValue());
            assertEquals(commentId, node.get(COMMENT_ID_FIELD).textValue());
            assertEquals(file.getId(), node.get(COMMENT_PARENT_ID_FIELD).textValue());
            assertEquals("an-id", node.get(EXTERNAL_ENTITY_ID).textValue());
            assertEquals("<entity/>", node.get(EXTERNAL_ENTITY).textValue());
            assertEquals("Test", node.get(EXTERNAL_ENTITY_ORIGIN).textValue());
        }
    }

    /*
     * NXP-28483
     */
    @Test
    public void testUpdateCommentWithRegularUser() throws IOException {
        // create jdoe user as a regular user
        createUser(JDOE);
        // re-compute read acls
        fetchInvalidations();
        // use it in rest calls
        service = getServiceFor(JDOE, JDOE);

        String commentId;
        // use rest for creation in order to have the correct author
        Comment comment = newComment(file.getId());
        String jsonComment = MarshallerHelper.objectToJson(comment, CtxBuilder.session(session).get());
        try (CloseableClientResponse response = getResponse(RequestType.POST, "id/" + file.getId() + "/@comment",
                jsonComment)) {
            assertEquals(Response.Status.CREATED.getStatusCode(), response.getStatus());
            JsonNode node = mapper.readTree(response.getEntityInputStream());
            commentId = node.get(COMMENT_ID_FIELD).textValue();
        }

        // now update the comment
        comment.setText("And now I update it");
        jsonComment = MarshallerHelper.objectToJson(comment, CtxBuilder.session(session).get());
        try (CloseableClientResponse response = getResponse(RequestType.PUT,
                "id/" + file.getId() + "/@comment/" + commentId, jsonComment)) {
            assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
            JsonNode node = mapper.readTree(response.getEntityInputStream());
            // assert the response
            assertEquals("And now I update it", node.get(COMMENT_TEXT_FIELD).textValue());
            fetchInvalidations();
            // assert DB was updated
            Comment updatedComment = commentManager.getComment(session, commentId);
            assertEquals("And now I update it", updatedComment.getText());
        }
    }

    @Test
    public void testDeleteCommentWithNonExistingId() {
        try (CloseableClientResponse response = getResponse(RequestType.GET,
                "id/" + file.getId() + "/@comment/" + "nonExistingId")) {
            assertEquals(Response.Status.NOT_FOUND.getStatusCode(), response.getStatus());
        }
    }

    @Test
    public void testDeleteComment() {
        Comment comment = commentManager.createComment(session, newComment(file.getId()));
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
        String entityId = "foo";
        String entity = "<entity></entity>";

        Comment comment = newExternalComment(file.getId(), entityId, entity, "Some text");
        comment = commentManager.createComment(session, comment);
        fetchInvalidations();

        JsonNode node = getResponseAsJson(RequestType.GET, "id/" + file.getId() + "/@comment/external/" + entityId);

        assertEquals(COMMENT_ENTITY_TYPE, node.get("entity-type").asText());
        assertEquals(comment.getId(), node.get(COMMENT_ID_FIELD).textValue());
        assertEquals(file.getId(), node.get(COMMENT_PARENT_ID_FIELD).textValue());
        assertEquals(entityId, node.get(EXTERNAL_ENTITY_ID).textValue());
        assertEquals("Test", node.get(EXTERNAL_ENTITY_ORIGIN).textValue());
        assertEquals(entity, node.get(EXTERNAL_ENTITY).textValue());
    }

    @Test
    public void testUpdateExternalComment() throws IOException {
        fetchInvalidations();
        String entityId = "foo";
        String author = "toto";

        Comment comment = newExternalComment(file.getId(), entityId);
        comment.setAuthor(author);
        comment = commentManager.createComment(session, comment);

        fetchInvalidations();

        comment.setAuthor("titi");
        String jsonComment = MarshallerHelper.objectToJson(comment, CtxBuilder.session(session).get());

        try (CloseableClientResponse response = getResponse(RequestType.PUT,
                "id/" + file.getId() + "/@comment/external/" + entityId, jsonComment)) {
            assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());

            fetchInvalidations();
            Comment updatedComment = commentManager.getExternalComment(session, file.getId(), entityId);
            // Author should not be modified
            assertEquals(author, updatedComment.getAuthor());
        }
    }

    /*
     * NXP-28483
     */
    @Test
    public void testUpdateExternalCommentWithRegularUser() throws IOException {
        // create jdoe user as a regular user
        createUser(JDOE);
        // re-compute read acls
        fetchInvalidations();
        // use it in rest calls
        service = getServiceFor(JDOE, JDOE);

        String entityId = "foo";
        // use rest for creation in order to have the correct author
        Comment comment = newExternalComment(file.getId(), entityId, "<entity/>", "Some text");
        String jsonComment = MarshallerHelper.objectToJson(comment, CtxBuilder.session(session).get());
        try (CloseableClientResponse response = getResponse(RequestType.POST, "id/" + file.getId() + "/@comment",
                jsonComment)) {
            assertEquals(Response.Status.CREATED.getStatusCode(), response.getStatus());
        }

        // now update the comment
        comment.setText("And now I update it");
        jsonComment = MarshallerHelper.objectToJson(comment, CtxBuilder.session(session).get());
        try (CloseableClientResponse response = getResponse(RequestType.PUT,
                "id/" + file.getId() + "/@comment/external/" + entityId, jsonComment)) {
            assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
            JsonNode node = mapper.readTree(response.getEntityInputStream());
            // assert the response
            assertEquals("And now I update it", node.get(COMMENT_TEXT_FIELD).textValue());
            fetchInvalidations();
            // assert DB was updated
            Comment updatedComment = commentManager.getExternalComment(session, file.getId(), entityId);
            assertEquals("And now I update it", updatedComment.getText());
        }
    }

    @Test
    public void testDeleteExternalComment() {
        String entityId = "foo";
        Comment comment = commentManager.createComment(session, newExternalComment(file.getId(), entityId));

        fetchInvalidations();

        assertTrue(session.exists(new IdRef(comment.getId())));

        try (CloseableClientResponse response = getResponse(RequestType.DELETE,
                "id/" + file.getId() + "/@comment/external/" + entityId)) {
            assertEquals(Response.Status.NO_CONTENT.getStatusCode(), response.getStatus());
            fetchInvalidations();
            assertFalse(session.exists(new IdRef(comment.getId())));
        }
    }

}
