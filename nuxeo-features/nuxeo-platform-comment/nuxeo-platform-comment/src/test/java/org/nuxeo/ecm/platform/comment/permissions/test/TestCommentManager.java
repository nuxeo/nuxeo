/*
 * (C) Copyright 2019 Nuxeo (http://nuxeo.com/) and others.
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
 *     Nour AL KOTOB
 */

package org.nuxeo.ecm.platform.comment.permissions.test;

import static org.junit.Assert.assertEquals;

import java.util.List;

import javax.inject.Inject;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.security.ACE;
import org.nuxeo.ecm.core.api.security.ACL;
import org.nuxeo.ecm.core.api.security.ACP;
import org.nuxeo.ecm.core.api.security.SecurityConstants;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.ecm.platform.comment.api.CommentManager;
import org.nuxeo.ecm.platform.comment.workflow.utils.CommentsConstants;
import org.nuxeo.ecm.platform.usermanager.UserManager;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;

@RunWith(FeaturesRunner.class)
@Features(CommentFeature.class)
public class TestCommentManager {

    public static final String QUERY_COMMENTS_AS_DOCUMENTS = "SELECT * FROM " + CommentsConstants.COMMENT_DOC_TYPE;

    public static final String USERNAME = "Foo";

    public static final String COMMENT_CONTENT_PROPERTY_XPATH = "comment:text";

    public static final String COMMENT_CONTENT = "This is my comment";

    @Inject
    protected CoreSession systemSession;

    @Inject
    protected CommentManager commentManager;

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
        DocumentModel folder = systemSession.createDocumentModel("/", "folder", "Folder");
        systemSession.createDocument(folder);

        // Create a document
        DocumentModel doc = systemSession.createDocumentModel("/folder/", "TestFile", "File");
        doc = systemSession.createDocument(doc);

        // Set The right ACE so a user can read the document
        ACP acp = doc.getACP();
        ACE grantRead = new ACE(USERNAME, SecurityConstants.READ, true);
        acp.addACE(ACL.LOCAL_ACL, grantRead);
        doc.setACP(acp, true);

        try (CoreSession userSession = coreFeature.openCoreSession(USERNAME)) {
            // Get the document as the user
            DocumentModel userDoc = userSession.getDocument(doc.getRef());

            // Comment the document as the user
            DocumentModel comment = userSession.createDocumentModel(CommentsConstants.COMMENT_DOC_TYPE);
            comment.setPropertyValue(COMMENT_CONTENT_PROPERTY_XPATH, COMMENT_CONTENT);
            commentManager.createComment(userDoc, comment);

            // Check the comment document can be retrieved by a system session query
            List<DocumentModel> dml = systemSession.query(QUERY_COMMENTS_AS_DOCUMENTS);
            assertEquals(1, dml.size());

            // Check the comment document cannot be retrieved by the user session query
            dml = userSession.query(QUERY_COMMENTS_AS_DOCUMENTS);
            assertEquals(0, dml.size());

            // Check the comment can be retrieved by the user via the comment service
            List<DocumentModel> comments = commentManager.getComments(userDoc);
            assertEquals(1, comments.size());
            assertEquals(COMMENT_CONTENT,
                    comments.get(0).getPropertyValue(COMMENT_CONTENT_PROPERTY_XPATH));

            // Check the comment was deleted by the user
            commentManager.deleteComment(userDoc, comments.get(0));
            comments = commentManager.getComments(userDoc);
            assertEquals(0, comments.size());
        }
    }
}
