/*
 * (C) Copyright 2018-2019 Nuxeo (http://nuxeo.com/) and others.
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

import static org.junit.Assert.assertEquals;

import java.util.List;

import javax.inject.Inject;

import org.junit.Test;
import org.nuxeo.ecm.core.api.CloseableCoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.security.ACE;
import org.nuxeo.ecm.core.api.security.ACL;
import org.nuxeo.ecm.core.api.security.ACP;
import org.nuxeo.ecm.core.api.security.SecurityConstants;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.ecm.platform.comment.api.Comment;
import org.nuxeo.ecm.platform.comment.api.CommentImpl;
import org.nuxeo.ecm.platform.comment.workflow.utils.CommentsConstants;
import org.nuxeo.ecm.platform.test.PlatformFeature;
import org.nuxeo.ecm.platform.usermanager.UserManager;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;

/**
 * @since 10.3
 */
@Features(PlatformFeature.class)
@Deploy("org.nuxeo.ecm.relations.api")
@Deploy("org.nuxeo.ecm.relations")
@Deploy("org.nuxeo.ecm.relations.jena")
@Deploy("org.nuxeo.ecm.platform.comment.tests:OSGI-INF/comment-jena-contrib.xml")
@Deploy("org.nuxeo.ecm.platform.comment.tests:OSGI-INF/relation-comment-manager-override.xml")
public class TestCommentManagerImpl extends AbstractTestCommentManager {

    public static final String QUERY_COMMENTS_AS_DOCUMENTS = "SELECT * FROM " + CommentsConstants.COMMENT_DOC_TYPE;

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
}
