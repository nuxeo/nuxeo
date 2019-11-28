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
 *     Nuno Cunha <ncunha@nuxeo.com>
 */

package org.nuxeo.ecm.platform.comment;

import static org.apache.commons.lang3.BooleanUtils.toBoolean;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.nuxeo.ecm.core.api.event.DocumentEventTypes.DOCUMENT_CREATED;
import static org.nuxeo.ecm.core.api.event.DocumentEventTypes.DOCUMENT_REMOVED;
import static org.nuxeo.ecm.core.api.event.DocumentEventTypes.DOCUMENT_UPDATED;
import static org.nuxeo.ecm.platform.comment.api.CommentConstants.COMMENT_DOCUMENT;
import static org.nuxeo.ecm.platform.comment.api.CommentEvents.COMMENT_ADDED;
import static org.nuxeo.ecm.platform.comment.api.CommentEvents.COMMENT_REMOVED;
import static org.nuxeo.ecm.platform.comment.api.CommentEvents.COMMENT_UPDATED;
import static org.nuxeo.ecm.platform.comment.workflow.utils.CommentsConstants.COMMENT_AUTHOR;
import static org.nuxeo.ecm.platform.comment.workflow.utils.CommentsConstants.COMMENT_CREATION_DATE;
import static org.nuxeo.ecm.platform.comment.workflow.utils.CommentsConstants.COMMENT_DOC_TYPE;
import static org.nuxeo.ecm.platform.comment.workflow.utils.CommentsConstants.COMMENT_TEXT;
import static org.nuxeo.ecm.platform.ec.notification.NotificationConstants.DISABLE_NOTIFICATION_SERVICE;

import java.io.Serializable;
import java.util.Calendar;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.api.CloseableCoreSession;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.core.api.PathRef;
import org.nuxeo.ecm.core.api.security.ACE;
import org.nuxeo.ecm.core.api.security.ACL;
import org.nuxeo.ecm.core.api.security.ACP;
import org.nuxeo.ecm.core.api.security.SecurityConstants;
import org.nuxeo.ecm.core.event.Event;
import org.nuxeo.ecm.core.event.impl.DocumentEventContext;
import org.nuxeo.ecm.core.event.test.CapturingEventListener;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.ecm.core.test.annotations.Granularity;
import org.nuxeo.ecm.core.test.annotations.RepositoryConfig;
import org.nuxeo.ecm.platform.comment.api.Comment;
import org.nuxeo.ecm.platform.comment.api.CommentImpl;
import org.nuxeo.ecm.platform.comment.api.CommentManager;
import org.nuxeo.ecm.platform.comment.api.CommentableDocument;
import org.nuxeo.ecm.platform.comment.api.exceptions.CommentNotFoundException;
import org.nuxeo.ecm.platform.comment.api.exceptions.CommentSecurityException;
import org.nuxeo.ecm.platform.comment.notification.CommentCreationVeto;
import org.nuxeo.ecm.platform.comment.notification.CommentDeletionVeto;
import org.nuxeo.ecm.platform.comment.notification.CommentModificationVeto;
import org.nuxeo.ecm.platform.comment.notification.CommentNotificationVeto;
import org.nuxeo.ecm.platform.comment.service.CommentServiceConfig;
import org.nuxeo.ecm.platform.ec.notification.NotificationListenerVeto;
import org.nuxeo.ecm.platform.ec.notification.service.NotificationService;
import org.nuxeo.ecm.platform.notification.api.NotificationManager;
import org.nuxeo.ecm.platform.test.PlatformFeature;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.TransactionalFeature;

/**
 * @since 10.3
 */
@RunWith(FeaturesRunner.class)
@Features(PlatformFeature.class)
@RepositoryConfig(cleanup = Granularity.METHOD)
@Deploy("org.nuxeo.ecm.platform.comment.api")
@Deploy("org.nuxeo.ecm.platform.comment")
@Deploy("org.nuxeo.ecm.platform.notification.core")
public abstract class AbstractTestCommentManager {

    public static final String FOLDER_COMMENT_CONTAINER = "/Folder/CommentContainer";

    @Inject
    protected CoreSession session;

    @Inject
    protected CommentManager commentManager;

    @Inject
    protected TransactionalFeature transactionalFeature;

    @Inject
    protected CoreFeature coreFeature;

    @Inject
    protected NotificationManager notificationService;

    public abstract Class<? extends CommentManager> getType();

    @Before
    public void init() {
        DocumentModel domain = session.createDocumentModel("/", "Folder", "Folder");
        domain = session.createDocument(domain);
        DocumentModel doc = session.createDocumentModel(domain.getPathAsString(), "File", "File");
        session.createDocument(doc);
        DocumentModel container = session.createDocumentModel(domain.getPathAsString(), "CommentContainer", "Folder");
        session.createDocument(container);
        session.save();
    }

    @Test
    public void testCreateComment() {
        DocumentModel domain = session.createDocumentModel("/", "domain", "Domain");
        domain = session.createDocument(domain);
        DocumentModel doc = session.createDocumentModel("/domain", "test", "File");
        doc = session.createDocument(doc);
        session.save();

        String author = "toto";
        String text = "I am a comment !";
        Comment comment = new CommentImpl();
        comment.setAuthor(author);
        comment.setText(text);
        comment.setParentId("fakeId");

        try {
            commentManager.createComment(session, comment);
            fail("Creating a comment should have failed !");
        } catch (CommentNotFoundException e) {
            // ok
            assertEquals(404, e.getStatusCode());
            assertNotNull(e.getMessage());
        }

        comment.setParentId(doc.getId());
        comment = commentManager.createComment(session, comment);
        assertEquals(author, comment.getAuthor());
        assertEquals(text, comment.getText());
        assertEquals(doc.getRef(), commentManager.getTopLevelCommentAncestor(session, new IdRef(comment.getId())));
    }

    @Test
    public void testGetComment() {
        DocumentModel domain = session.createDocumentModel("/", "domain", "Domain");
        domain = session.createDocument(domain);
        DocumentModel doc = session.createDocumentModel("/domain", "test", "File");
        doc = session.createDocument(doc);
        session.save();

        String author = "toto";
        String text = "I am a comment !";
        Comment comment = new CommentImpl();
        comment.setAuthor(author);
        comment.setText(text);
        comment.setParentId(doc.getId());

        comment = commentManager.createComment(session, comment);

        try {
            commentManager.getComment(session, "fakeId");
            fail("Getting a comment should have failed !");
        } catch (CommentNotFoundException e) {
            // ok
            assertEquals(404, e.getStatusCode());
            assertNotNull(e.getMessage());
        }

        comment = commentManager.getComment(session, comment.getId());
        assertEquals(author, comment.getAuthor());
        assertEquals(text, comment.getText());
    }

    @Test
    public void testDeleteComment() {
        DocumentModel domain = session.createDocumentModel("/", "domain", "Domain");
        domain = session.createDocument(domain);
        DocumentModel doc = session.createDocumentModel("/domain", "test", "File");
        doc = session.createDocument(doc);
        session.save();

        String author = "toto";
        String text = "I am a comment !";
        Comment comment = new CommentImpl();
        comment.setAuthor(author);
        comment.setText(text);
        comment.setParentId(doc.getId());

        comment = commentManager.createComment(session, comment);
        assertTrue(session.exists(new IdRef(comment.getId())));

        try {
            commentManager.deleteComment(session, "fakeId");
            fail("Deleting a comment should have failed !");
        } catch (CommentNotFoundException e) {
            // ok
            assertEquals(404, e.getStatusCode());
            assertNotNull(e.getMessage());
        }

        commentManager.deleteComment(session, comment.getId());
        assertFalse(session.exists(new IdRef(comment.getId())));
    }

    @Test
    public void testCommentableDocumentAdapter() {
        DocumentModel doc = session.getDocument(new PathRef("/Folder/File"));
        CommentableDocument commentableDocument = doc.getAdapter(CommentableDocument.class);
        DocumentModel comment = session.createDocumentModel(COMMENT_DOC_TYPE);
        comment.setPropertyValue(COMMENT_TEXT, "Test");
        comment.setPropertyValue(COMMENT_AUTHOR, "bob");
        comment.setPropertyValue(COMMENT_CREATION_DATE, Calendar.getInstance());

        // Create a comment
        commentableDocument.addComment(comment);
        session.save();

        // Creation check
        assertEquals(1, commentableDocument.getComments().size());
        DocumentModel newComment = commentableDocument.getComments().get(0);
        assertThat(newComment.getPropertyValue(COMMENT_TEXT)).isEqualTo("Test");

        // Deletion check
        commentableDocument.removeComment(newComment);
        assertTrue(commentableDocument.getComments().isEmpty());
    }

    @Test
    public void testGetTopLevelCommentAncestor() {
        DocumentModel domain = session.createDocumentModel("/", "domain", "Domain");
        session.createDocument(domain);
        DocumentModel doc = session.createDocumentModel("/domain", "test", "File");
        doc = session.createDocument(doc);

        ACP acp = doc.getACP();
        ACL acl = acp.getOrCreateACL();
        acl.add(new ACE("james", SecurityConstants.READ, true));
        session.setACP(doc.getRef(), acp, false);
        session.save();

        String author = "toto";
        String text = "I am a comment !";
        Comment comment = new CommentImpl();
        comment.setAuthor(author);
        comment.setText(text);
        comment.setParentId(doc.getId());

        comment = commentManager.createComment(session, comment);
        assertEquals(doc.getRef(), commentManager.getTopLevelCommentAncestor(session, new IdRef(comment.getId())));

        try (CloseableCoreSession jamesSession = coreFeature.openCoreSession("james")) {
            assertEquals(doc.getRef(),
                    commentManager.getTopLevelCommentAncestor(jamesSession, new IdRef(comment.getId())));
        }

        try (CloseableCoreSession janeSession = coreFeature.openCoreSession("jane")) {
            assertEquals(doc.getRef(),
                    commentManager.getTopLevelCommentAncestor(janeSession, new IdRef(comment.getId())));
            fail("jane should not be able to get the top level comment ancestor");
        } catch (CommentSecurityException cse) {
            assertNotNull(cse);
            assertEquals(
                    String.format("The user jane does not have access to the comments of document %s", doc.getId()),
                    cse.getMessage());
        }
    }

    @Test
    public void testGetCommentThread() {
        DocumentModel domain = session.createDocumentModel("/", "domain", "Domain");
        session.createDocument(domain);
        DocumentModel doc = session.createDocumentModel("/domain", "test", "File");
        doc = session.createDocument(doc);
        session.save();

        String author = "toto";
        String text = "I am a comment !";
        Comment comment = new CommentImpl();
        comment.setAuthor(author);
        comment.setText(text);
        comment.setParentId(doc.getId());

        comment = commentManager.createComment(session, comment);

        // Add a reply
        Comment reply = new CommentImpl();
        reply.setAuthor(author);
        reply.setText("I am a reply");
        reply.setParentId(comment.getId());
        reply = commentManager.createComment(session, reply);

        // Another reply
        Comment anotherReply = new CommentImpl();
        anotherReply.setAuthor(author);
        anotherReply.setText("I am a 2nd reply");
        anotherReply.setParentId(reply.getId());
        anotherReply = commentManager.createComment(session, anotherReply);

        DocumentModel anotherReplyDocModel = session.getDocument(new IdRef(anotherReply.getId()));
        DocumentRef topLevelCommentAncestor = commentManager.getTopLevelCommentAncestor(session,
                anotherReplyDocModel.getRef());
        assertEquals(doc.getRef(), topLevelCommentAncestor);
    }

    @Test
    public void testGetEmptyComments() {
        DocumentModel domain = session.createDocumentModel("/", "domain", "Domain");
        session.createDocument(domain);
        DocumentModel doc = session.createDocumentModel("/domain", "test", "File");
        doc = session.createDocument(doc);
        session.save();

        List<Comment> comments = commentManager.getComments(session, doc.getId());
        assertTrue(comments.isEmpty());
    }

    @Test
    public void testCommentManagerType() {
        assertEquals(getType(), commentManager.getClass());
    }

    @Test
    public void shouldNotifyEventWhenCreateComment() {
        DocumentModel domain = session.createDocumentModel("/", "domain", "Domain");
        session.createDocument(domain);
        final DocumentModel doc = session.createDocumentModel(domain.getPathAsString(), "test", "File");
        DocumentModel documentModel = session.createDocument(doc);
        transactionalFeature.nextTransaction();

        publishAndVerifyEventNotification(() -> {
            Comment comment = new CommentImpl();
            comment.setAuthor("author");
            comment.setText("any Comment message");
            comment.setParentId(documentModel.getId());

            Comment createdComment = commentManager.createComment(session, comment);
            return session.getDocument(new IdRef(createdComment.getId()));

        }, COMMENT_ADDED, DOCUMENT_CREATED);
    }

    @Test
    public void shouldNotifyEventWhenUpdateComment() {
        DocumentModel domain = session.createDocumentModel("/", "domain", "Domain");
        session.createDocument(domain);
        DocumentModel doc = session.createDocumentModel(domain.getPathAsString(), "test", "File");
        doc = session.createDocument(doc);

        Comment comment = new CommentImpl();
        comment.setAuthor("author");
        comment.setText("any Comment message");
        comment.setParentId(doc.getId());

        Comment createdComment = commentManager.createComment(session, comment);
        transactionalFeature.nextTransaction();

        publishAndVerifyEventNotification(() -> {
            createdComment.setText("i update the message");
            commentManager.updateComment(session, createdComment.getId(), createdComment);
            return session.getDocument(new IdRef(createdComment.getId()));

        }, COMMENT_UPDATED, DOCUMENT_UPDATED);
    }

    @Test
    public void shouldNotifyEventWhenRemoveComment() {
        DocumentModel domain = session.createDocumentModel("/", "domain", "Domain");
        session.createDocument(domain);
        DocumentModel doc = session.createDocumentModel(domain.getPathAsString(), "test", "File");
        doc = session.createDocument(doc);

        Comment comment = new CommentImpl();
        comment.setAuthor("author");
        comment.setText("any Comment message");
        comment.setParentId(doc.getId());

        Comment createdComment = commentManager.createComment(session, comment);
        DocumentModel commentDocModel = session.getDocument(new IdRef(createdComment.getId()));
        commentDocModel.detach(true);
        transactionalFeature.nextTransaction();

        publishAndVerifyEventNotification(() -> {
            commentManager.deleteComment(session, createdComment.getId());
            return commentDocModel;

        }, COMMENT_REMOVED, DOCUMENT_REMOVED);
    }

    protected void publishAndVerifyEventNotification(Supplier<DocumentModel> supplier, String commentEventType,
            String documentEventType) {
        try (CapturingEventListener listener = new CapturingEventListener(commentEventType, documentEventType)) {
            DocumentModel expectedDocModel = supplier.get();

            assertTrue(listener.hasBeenFired(commentEventType));
            assertTrue(listener.hasBeenFired(documentEventType));

            // Depending on the case of the comment manager implementation, many notifications can be published
            // But we should handle and process (sending email...) one and only one
            Class<? extends CommentNotificationVeto> expectedVetoType = getVetoType(commentEventType);
            Collection<NotificationListenerVeto> notificationVetos = ((NotificationService) notificationService).getNotificationVetos();
            List<NotificationListenerVeto> expectedVetos = notificationVetos.stream()
                                                                            .filter(e -> expectedVetoType.isAssignableFrom(
                                                                                    e.getClass()))
                                                                            .collect(Collectors.toList());

            assertEquals(1, expectedVetos.size());

            NotificationListenerVeto veto = expectedVetos.get(0);
            List<Event> events = listener.getCapturedEvents()
                                         .stream()
                                         .filter(e -> veto.accept(e) //
                                                 && !toBoolean((Boolean) e.getContext()
                                                                          .getProperty(DISABLE_NOTIFICATION_SERVICE)))
                                         .collect(Collectors.toList());

            assertEquals(1, events.size());
            assertEquals(commentEventType, events.get(0).getName());

            Event expectedEvent = events.get(0);
            DocumentEventContext context = (DocumentEventContext) expectedEvent.getContext();

            Map<String, Serializable> properties = context.getProperties();
            assertFalse(properties.isEmpty());

            assertTrue(properties.containsKey(COMMENT_DOCUMENT));
            DocumentModel commentDocModel = (DocumentModel) properties.get(COMMENT_DOCUMENT);
            assertEquals(expectedDocModel.getId(), commentDocModel.getId());
        }
    }

    protected Class<? extends CommentNotificationVeto> getVetoType(String commentEventType) {
        switch (commentEventType) {
        case COMMENT_ADDED:
            return CommentCreationVeto.class;
        case COMMENT_UPDATED:
            return CommentModificationVeto.class;
        case COMMENT_REMOVED:
            return CommentDeletionVeto.class;
        }
        throw new IllegalArgumentException("Undefined veto for comment event type: " + commentEventType);
    }

    public static CommentServiceConfig newConfig() {
        CommentServiceConfig config = new CommentServiceConfig();
        config.graphName = "documentComments";
        config.commentConverterClassName = "org.nuxeo.ecm.platform.comment.impl.CommentConverterImpl";
        config.documentNamespace = "http://www.nuxeo.org/document/uid";
        config.commentNamespace = "http://www.nuxeo.org/comments/uid";
        config.predicateNamespace = "http://www.nuxeo.org/predicates/isCommentFor";
        return config;
    }

}
