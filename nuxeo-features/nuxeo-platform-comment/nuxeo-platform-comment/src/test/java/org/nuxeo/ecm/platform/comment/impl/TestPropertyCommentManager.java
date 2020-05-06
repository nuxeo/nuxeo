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

package org.nuxeo.ecm.platform.comment.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.nuxeo.ecm.core.io.marshallers.json.document.DocumentModelJsonReader.applyDirtyPropertyValues;
import static org.nuxeo.ecm.core.schema.FacetNames.FOLDERISH;
import static org.nuxeo.ecm.core.schema.FacetNames.HIDDEN_IN_NAVIGATION;
import static org.nuxeo.ecm.platform.comment.CommentUtils.newComment;
import static org.nuxeo.ecm.platform.comment.impl.AbstractCommentManager.COMMENTS_DIRECTORY;
import static org.nuxeo.ecm.platform.comment.impl.PropertyCommentManager.HIDDEN_FOLDER_TYPE;
import static org.nuxeo.ecm.platform.comment.workflow.utils.CommentsConstants.COMMENT_DOC_TYPE;

import java.time.Instant;
import java.util.Calendar;

import org.junit.Ignore;
import org.junit.Test;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.PathRef;
import org.nuxeo.ecm.platform.comment.AbstractTestCommentManager;
import org.nuxeo.ecm.platform.comment.api.Comment;

/**
 * @since 10.3
 */
public class TestPropertyCommentManager extends AbstractTestCommentManager {

    public TestPropertyCommentManager() {
        super(PropertyCommentManager.class);
    }

    @Test
    @Override
    @Ignore("PropertyCommentManager doesn't support update by powerful user")
    public void testUpdateCommentByPowerfulUser() {
    }

    @Test
    @Override
    @Ignore("PropertyCommentManager doesn't support update by powerful user")
    public void testUpdateExternalCommentByPowerfulUser() {
    }

    // ----------------
    // Structure tests
    // ----------------

    @Test
    public void testCommentsStructure() {
        Comment c1 = commentManager.createComment(session, newComment(commentedDocModel.getId(), "I am a comment!"));
        Comment c2 = commentManager.createComment(session, newComment(c1.getId(), "I am a reply!"));
        Comment c3 = commentManager.createComment(session, newComment(c2.getId(), "Me too!"));

        // in PropertyCommentManager: domain > Comments (container) > c1 ~ c2 ~ c3
        DocumentModel domainDocModel = session.getDocument(new PathRef("/domain"));
        DocumentModel commentContainerDocModel = session.getChild(domainDocModel.getRef(), COMMENTS_DIRECTORY);

        DocumentRef[] c1ParentDocRefs = session.getParentDocumentRefs(c1.getDocument().getRef());
        assertEquals(commentContainerDocModel.getRef(), c1ParentDocRefs[0]);
        assertEquals(domainDocModel.getRef(), c1ParentDocRefs[1]);

        DocumentRef[] c2ParentDocRefs = session.getParentDocumentRefs(c2.getDocument().getRef());
        assertEquals(commentContainerDocModel.getRef(), c2ParentDocRefs[0]);
        assertEquals(domainDocModel.getRef(), c2ParentDocRefs[1]);

        DocumentRef[] c3ParentDocRefs = session.getParentDocumentRefs(c3.getDocument().getRef());
        assertEquals(commentContainerDocModel.getRef(), c3ParentDocRefs[0]);
        assertEquals(domainDocModel.getRef(), c3ParentDocRefs[1]);

        // check paths
        assertEquals("/domain/test", commentedDocModel.getPathAsString());
        assertEquals("/domain/Comments", commentContainerDocModel.getPathAsString());
        assertEquals("/domain/Comments/comment", c1.getDocument().getPathAsString());
        String c2Path = c2.getDocument().getPathAsString();
        assertTrue("Reply path: " + c2Path + " is not correct", c2Path.startsWith("/domain/Comments/comment."));
        String c3Path = c3.getDocument().getPathAsString();
        assertTrue("Sub reply path: " + c3Path + " is not correct", c3Path.startsWith("/domain/Comments/comment."));

        // check container
        assertEquals(HIDDEN_FOLDER_TYPE, commentContainerDocModel.getType());
        assertTrue(commentContainerDocModel.hasFacet(FOLDERISH));
        assertTrue(commentContainerDocModel.hasFacet(HIDDEN_IN_NAVIGATION));
        assertEquals(domainDocModel.getRef(), commentContainerDocModel.getParentRef());
    }

    /*
     * NXP-28719
     */
    @Test
    public void testCommentsStructureOnPlaceless() {
        DocumentModel placeless = session.createDocumentModel(null, "placeless", "File");
        placeless = session.createDocument(placeless);
        transactionalFeature.nextTransaction();

        Comment c1 = commentManager.createComment(session, newComment(placeless.getId(), "I am a comment!"));
        Comment c2 = commentManager.createComment(session, newComment(c1.getId(), "I am a reply!"));
        Comment c3 = commentManager.createComment(session, newComment(c2.getId(), "Me too!"));

        // in PropertyCommentManager in the case of placeless:
        // Root > Comments (container) > c1
        // Root > Comments (container) > Comments (container) > c2 ~ c3
        DocumentModel rootDocModel = session.getRootDocument();
        DocumentModel commentContainerDocModel = session.getChild(rootDocModel.getRef(), COMMENTS_DIRECTORY);
        DocumentModel subCommentContainerDocModel = session.getChild(commentContainerDocModel.getRef(),
                COMMENTS_DIRECTORY);

        DocumentRef[] c1ParentDocRefs = session.getParentDocumentRefs(c1.getDocument().getRef());
        assertEquals(commentContainerDocModel.getRef(), c1ParentDocRefs[0]);
        assertEquals(rootDocModel.getRef(), c1ParentDocRefs[1]);

        DocumentRef[] c2ParentDocRefs = session.getParentDocumentRefs(c2.getDocument().getRef());
        assertEquals(subCommentContainerDocModel.getRef(), c2ParentDocRefs[0]);
        assertEquals(commentContainerDocModel.getRef(), c2ParentDocRefs[1]);
        assertEquals(rootDocModel.getRef(), c2ParentDocRefs[2]);

        DocumentRef[] c3ParentDocRefs = session.getParentDocumentRefs(c3.getDocument().getRef());
        assertEquals(subCommentContainerDocModel.getRef(), c3ParentDocRefs[0]);
        assertEquals(commentContainerDocModel.getRef(), c3ParentDocRefs[1]);
        assertEquals(rootDocModel.getRef(), c3ParentDocRefs[2]);

        // check paths
        // PropertyCommentManager in the case of placeless will create this hierarchy
        // c1EcmParentDocModel = session.getDocument(replyDocModel.getParentRef());
        // assertEquals("/Comments/Comments", c1EcmParentDocModel.getPathAsString());
        assertEquals("/Comments", commentContainerDocModel.getPathAsString());
        assertEquals("/Comments/comment", c1.getDocument().getPathAsString());
        assertEquals("/Comments/Comments", subCommentContainerDocModel.getPathAsString());
        assertEquals("/Comments/Comments/comment", c2.getDocument().getPathAsString());
        String c3Path = c3.getDocument().getPathAsString();
        assertTrue("Sub reply path: " + c3Path + " is not correct", c3Path.startsWith("/Comments/Comments/comment."));
    }

    // -------------
    // Legacy tests
    // -------------

    @Test
    public void shouldCreateLocatedComment() {
        DocumentModel container = session.createDocumentModel("/domain", "CommentContainer", "Folder");
        session.createDocument(container);
        session.save();

        Comment comment = newComment(commentedDocModel.getId(), "some text");
        comment.setCreationDate(Instant.now());
        DocumentModel commentModel = session.createDocumentModel(null, "Comment", COMMENT_DOC_TYPE);
        applyDirtyPropertyValues(comment.getDocument(), commentModel);
        commentManager.createLocatedComment(commentedDocModel, commentModel, "/domain/CommentContainer");

        DocumentModelList children = session.getChildren(new PathRef("/domain/CommentContainer"), COMMENT_DOC_TYPE);
        assertNotNull(children);
        assertEquals(1, children.totalSize());
        assertEquals(comment.getAuthor(), children.get(0).getPropertyValue("comment:author"));
        assertEquals(comment.getCreationDate(),
                ((Calendar) children.get(0).getPropertyValue("comment:creationDate")).toInstant());
        assertEquals(comment.getText(), children.get(0).getPropertyValue("comment:text"));
    }
}
