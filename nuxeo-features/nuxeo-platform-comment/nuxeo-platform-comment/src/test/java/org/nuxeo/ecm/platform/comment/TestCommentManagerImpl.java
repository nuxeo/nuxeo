/*
 * (C) Copyright 2018-2020 Nuxeo (http://nuxeo.com/) and others.
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
 *     Nour AL KOTOB
 */
package org.nuxeo.ecm.platform.comment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.nuxeo.ecm.core.io.marshallers.json.document.DocumentModelJsonReader.applyDirtyPropertyValues;
import static org.nuxeo.ecm.platform.comment.api.CommentConstants.COMMENT_DOC_TYPE;

import java.util.Calendar;
import java.util.List;

import javax.inject.Inject;

import org.junit.Test;
import org.nuxeo.ecm.core.api.CloseableCoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.PathRef;
import org.nuxeo.ecm.core.api.security.ACE;
import org.nuxeo.ecm.core.api.security.ACL;
import org.nuxeo.ecm.core.api.security.ACP;
import org.nuxeo.ecm.core.api.security.SecurityConstants;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.ecm.platform.comment.api.Comment;
import org.nuxeo.ecm.platform.comment.api.CommentImpl;
import org.nuxeo.ecm.platform.comment.api.CommentManager;
import org.nuxeo.ecm.platform.comment.impl.CommentManagerImpl;
import org.nuxeo.ecm.platform.usermanager.UserManager;
import org.nuxeo.runtime.test.runner.Features;

/**
 * @deprecated since 11.1, this is a class test for a deprecated comment manager implementation
 *             {@link CommentManagerImpl}
 * @since 10.3
 */
@Deprecated(since = "11.1")
@Features(RelationCommentFeature.class)
public class TestCommentManagerImpl extends AbstractTestCommentManager {

    public static final String QUERY_COMMENTS_AS_DOCUMENTS = "SELECT * FROM " + COMMENT_DOC_TYPE;

    public static final String USERNAME = "Foo";

    public static final String COMMENT_CONTENT = "This is my comment";

    @Inject
    protected CoreFeature coreFeature;

    @Inject
    protected UserManager userManager;

    @Test
    public void testCreateReadDelete() {
        // Create a user
        DocumentModel userModel = userManager.getBareUserModel();
        String schemaName = userManager.getUserSchemaName();
        userModel.setProperty(schemaName, "username", USERNAME);

        // Create a folder
        DocumentModel folder = session.createDocumentModel("/", "folder", "Folder");
        session.createDocument(folder);

        // Create a document
        DocumentModel doc = session.createDocumentModel("/folder/", "TestFile", "File");
        doc = session.createDocument(doc);

        // Set The right ACE so a user can read the document
        ACP acp = doc.getACP();
        ACL acl = acp.getOrCreateACL(ACL.LOCAL_ACL);
        acl.add(new ACE(USERNAME, SecurityConstants.READ, true));
        doc.setACP(acp, true);

        try (CloseableCoreSession userSession = coreFeature.openCoreSession(USERNAME)) {
            // Get the document as the user
            DocumentModel userDoc = userSession.getDocument(doc.getRef());

            // Comment the document as the user
            Comment comment = new CommentImpl();
            comment.setAuthor(session.getPrincipal().getName());
            comment.setText(COMMENT_CONTENT);
            comment.setParentId(userDoc.getId());
            commentManager.createComment(session, comment);

            // Check the comment document can be retrieved by a system session query
            List<DocumentModel> dml = session.query(QUERY_COMMENTS_AS_DOCUMENTS);
            assertEquals(1, dml.size());

            // Check the comment document cannot be retrieved by the user session query
            dml = userSession.query(QUERY_COMMENTS_AS_DOCUMENTS);
            assertEquals(0, dml.size());

            // Check the comment can be retrieved by the user via the comment service
            List<Comment> comments = commentManager.getComments(userSession, userDoc.getId());
            assertEquals(1, comments.size());
            assertEquals(COMMENT_CONTENT, comments.get(0).getText());

            // Check the comment was deleted by the user
            commentManager.deleteComment(userSession, comments.get(0).getId());
            comments = commentManager.getComments(userSession, userDoc.getId());
            assertEquals(0, comments.size());
        }
    }

    @Test
    public void testCreateLocalComment() {
        DocumentModel domain = session.createDocumentModel("/", "domain", "Domain");
        session.createDocument(domain);
        DocumentModel doc = session.createDocumentModel("/domain", "test", "File");
        doc = session.createDocument(doc);
        session.save();

        String text = "I am a comment !";
        Comment comment = new CommentImpl();
        comment.setAuthor(AUTHOR_OF_COMMENT);
        comment.setText(text);

        // Create a comment in a specific location
        DocumentModel commentModel = session.createDocumentModel(null, "Comment", COMMENT_DOC_TYPE);
        commentModel = session.createDocument(commentModel);
        commentModel.setPropertyValue("dc:created", Calendar.getInstance());
        applyDirtyPropertyValues(comment.getDocument(), commentModel);
        commentModel = commentManager.createLocatedComment(doc, commentModel, FOLDER_COMMENT_CONTAINER);

        // Check if Comments folder has been created in the given container
        assertThat(session.getChildren(new PathRef(FOLDER_COMMENT_CONTAINER)).totalSize()).isEqualTo(1);

        assertThat(commentModel.getPathAsString()).contains(FOLDER_COMMENT_CONTAINER);
    }

    @Override
    public Class<? extends CommentManager> getType() {
        return CommentManagerImpl.class;
    }
}
