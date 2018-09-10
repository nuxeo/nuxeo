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

package org.nuxeo.ecm.platform.comment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.junit.Assume.assumeTrue;
import static org.nuxeo.ecm.platform.comment.api.CommentManager.Feature.COMMENTS_LINKED_WITH_PROPERTY;
import static org.nuxeo.ecm.platform.comment.workflow.utils.CommentsConstants.COMMENT_AUTHOR;
import static org.nuxeo.ecm.platform.comment.workflow.utils.CommentsConstants.COMMENT_CREATION_DATE;
import static org.nuxeo.ecm.platform.comment.workflow.utils.CommentsConstants.COMMENT_DOC_TYPE;
import static org.nuxeo.ecm.platform.comment.workflow.utils.CommentsConstants.COMMENT_TEXT;

import java.util.Calendar;

import javax.inject.Inject;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.core.api.PathRef;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.ecm.core.test.annotations.Granularity;
import org.nuxeo.ecm.core.test.annotations.RepositoryConfig;
import org.nuxeo.ecm.platform.comment.api.Comment;
import org.nuxeo.ecm.platform.comment.api.CommentImpl;
import org.nuxeo.ecm.platform.comment.api.CommentManager;
import org.nuxeo.ecm.platform.comment.api.CommentableDocument;
import org.nuxeo.ecm.platform.comment.api.Comments;
import org.nuxeo.ecm.platform.comment.service.CommentServiceConfig;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;

/**
 * @since 10.3
 */
@RunWith(FeaturesRunner.class)
@Features(CoreFeature.class)
@RepositoryConfig(cleanup = Granularity.METHOD)
@Deploy("org.nuxeo.ecm.platform.comment.api")
@Deploy("org.nuxeo.ecm.platform.comment")
public abstract class AbstractTestCommentManager {

    public static final String FOLDER_COMMENT_CONTAINER = "/Folder/CommentContainer";

    @Inject
    protected CoreSession session;

    @Inject
    protected CommentManager commentManager;

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
        } catch (IllegalArgumentException e) {
            // ok
        }

        comment.setParentId(doc.getId());
        comment = commentManager.createComment(session, comment);
        assertEquals(author, comment.getAuthor());
        assertEquals(text, comment.getText());
        assertTrue(comment.getAncestorIds().contains(doc.getId()));

        // Create a comment in a specific location
        DocumentModel commentModel = session.createDocumentModel(null, "Comment", COMMENT_DOC_TYPE);
        commentModel.setPropertyValue("dc:created", Calendar.getInstance());
        Comments.commentToDocumentModel(comment, commentModel);
        commentModel = commentManager.createLocatedComment(doc, commentModel, FOLDER_COMMENT_CONTAINER);
        // Check if Comments folder has been created in the given container
        assertThat(session.getChildren(new PathRef(FOLDER_COMMENT_CONTAINER)).totalSize()).isEqualTo(1);

        // Create a comment linked to a parent in a specific location
        Comment newComment = new CommentImpl();
        newComment.setParentId(commentModel.getId());
        commentManager.createComment(session, newComment);
        session.save();

        // Check if both comments are linked and located accordingly
        assertEquals(2, commentManager.getComments(session, doc.getId()).size());
        assertEquals(1, commentManager.getComments(session, commentModel.getId()).size());
        assertThat(commentModel.getPathAsString()).contains(FOLDER_COMMENT_CONTAINER);

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
        } catch (IllegalArgumentException e) {
            // ok
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
        } catch (IllegalArgumentException e) {
            // ok
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
        comment.setPropertyValue(COMMENT_AUTHOR, "system");
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
