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
 *     Salem Aouana
 */

package org.nuxeo.ecm.platform.comment;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.nuxeo.ecm.platform.comment.api.CommentConstants.COMMENT;
import static org.nuxeo.ecm.platform.comment.api.CommentConstants.COMMENT_DOCUMENT;
import static org.nuxeo.ecm.platform.comment.api.CommentConstants.PARENT_COMMENT;
import static org.nuxeo.ecm.platform.comment.api.CommentConstants.TOP_LEVEL_DOCUMENT;
import static org.nuxeo.ecm.platform.comment.workflow.utils.CommentsConstants.COMMENT_TEXT;

import java.io.Serializable;
import java.util.Collections;
import java.util.Map;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.event.Event;
import org.nuxeo.ecm.core.event.impl.DocumentEventContext;
import org.nuxeo.ecm.platform.usermanager.UserManager;
import org.nuxeo.ecm.platform.usermanager.exceptions.UserAlreadyExistsException;
import org.nuxeo.runtime.api.Framework;

/**
 * @since 11.1
 */
public class CommentUtils {

    private static final Logger log = LogManager.getLogger(CommentUtils.class);

    // Suppress default constructor for noninstantiability
    private CommentUtils() {
    }

    /**
     * Checks the comment / annotation event context data (comment document, commented parent...).
     *
     * @param event the event, cannot be {@code null}
     * @param expectedCommentDocModel the document model of the comment, cannot be {@code null}
     * @param expectedCommentedDocModel the document being commented, cannot be {@code null}
     * @param expectedTopLevelDocumentModel the non comment document being commented,different from the
     *            commentedDocModel in the case of a reply, cannot be {@code null}
     */
    public static void checkDocumentEventContext(Event event, DocumentModel expectedCommentDocModel,
            DocumentModel expectedCommentedDocModel, DocumentModel expectedTopLevelDocumentModel) {
        Map<String, Serializable> properties = event.getContext().getProperties();
        assertFalse(properties.isEmpty());

        assertTrue(properties.containsKey(COMMENT_DOCUMENT));
        DocumentModel commentDocModel = (DocumentModel) properties.get(COMMENT_DOCUMENT);
        assertEquals(expectedCommentDocModel.getId(), commentDocModel.getId());

        assertTrue(properties.containsKey(PARENT_COMMENT));
        DocumentModel commentedDocModel = (DocumentModel) properties.get(PARENT_COMMENT);
        assertNotNull(expectedCommentedDocModel);
        assertEquals(expectedCommentedDocModel.getRef(), commentedDocModel.getRef());

        assertTrue(properties.containsKey(COMMENT));
        assertEquals(expectedCommentDocModel.getPropertyValue(COMMENT_TEXT), properties.get(COMMENT));

        // The event source document must be the top level document as is the one linked in the notification.
        assertTrue(properties.containsKey(TOP_LEVEL_DOCUMENT));
        DocumentModel topLevelDocument = (DocumentModel) properties.get(TOP_LEVEL_DOCUMENT);
        assertEquals(expectedTopLevelDocumentModel.getRef(), topLevelDocument.getRef());
        DocumentModel sourceDoc = ((DocumentEventContext) event.getContext()).getSourceDocument();
        assertEquals(expectedCommentedDocModel.getRef(), sourceDoc.getRef());
    }

    public static void createUser(String userName) {
        try {
            UserManager userManager = Framework.getService(UserManager.class);
            DocumentModel userModel = userManager.getBareUserModel();
            userModel.setProperty("user", "username", userName);
            userModel.setProperty("user", "email", userName + "@nuxeo.com");
            userModel.setProperty("user", "password", userName);
            userModel.setProperty("user", "groups", Collections.singletonList("members"));
            userManager.createUser(userModel);
        } catch (UserAlreadyExistsException e) {
            // Avoid failure in tests if the user already exists
            log.trace("User already exists", e);
        }
    }

}
