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
import java.time.Instant;
import java.util.Arrays;
import java.util.Map;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.ecm.core.event.Event;
import org.nuxeo.ecm.core.event.impl.DocumentEventContext;
import org.nuxeo.ecm.platform.comment.api.Annotation;
import org.nuxeo.ecm.platform.comment.api.AnnotationImpl;
import org.nuxeo.ecm.platform.comment.api.Comment;
import org.nuxeo.ecm.platform.comment.api.CommentImpl;
import org.nuxeo.ecm.platform.comment.api.ExternalEntity;
import org.nuxeo.ecm.platform.ec.notification.NotificationConstants;
import org.nuxeo.ecm.platform.notification.api.NotificationManager;
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

    public static void addNotificationSubscriptions(NuxeoPrincipal principal, DocumentModel docToSubscribe,
            String... notifications) {
        NotificationManager notificationManager = Framework.getService(NotificationManager.class);
        String subscriber = NotificationConstants.USER_PREFIX + principal.getName();
        Arrays.asList(notifications)
              .forEach(n -> notificationManager.addSubscription(subscriber, n, docToSubscribe, false, principal, n));
    }

    /**
     * @return a mutable {@link Comment} for update
     */
    @SuppressWarnings("unchecked")
    public static <C extends Comment> C emptyComment() {
        return (C) new CommentImpl();
    }

    /**
     * @return a mutable {@link Comment} for creation
     */
    public static <C extends Comment> C newComment(String parentId) {
        C comment = emptyComment();
        comment.setParentId(parentId);
        return comment;
    }

    /**
     * @return a mutable {@link Comment} for creation
     */
    public static <C extends Comment> C newComment(String parentId, String text) {
        C comment = newComment(parentId);
        comment.setText(text);
        return comment;
    }

    /**
     * @return a mutable {@link Comment} made by an external system for creation
     */
    public static <C extends Comment & ExternalEntity> C newExternalComment(String parentId, String entityId) {
        C comment = newComment(parentId);
        comment.setEntityId(entityId);
        comment.setOrigin("Test");
        return comment;
    }

    /**
     * @return a mutable {@link Comment} made by an external system for creation
     */
    public static <C extends Comment & ExternalEntity> C newExternalComment(String parentId, String entityId,
            String entity, String text) {
        C comment = newExternalComment(parentId, entityId);
        comment.setEntity(entity);
        comment.setText(text);
        return comment;
    }

    /**
     * @return a mutable {@link Annotation} for creation
     */
    @SuppressWarnings("unchecked")
    public static <A extends Annotation> A newAnnotation(String parentId, String xpath) {
        A annotation = (A) new AnnotationImpl();
        annotation.setParentId(parentId);
        annotation.setXpath(xpath);
        annotation.setCreationDate(Instant.now());
        annotation.setModificationDate(Instant.now());
        return annotation;
    }

    /**
     * @return a mutable {@link Annotation} for creation
     */
    public static <A extends Annotation> A newAnnotation(String parentId, String xpath, String text) {
        A annotation = newAnnotation(parentId, xpath);
        annotation.setText(text);
        return annotation;
    }

    /**
     * @return a mutable {@link Annotation} made by an external system for creation
     */
    public static <A extends Annotation & ExternalEntity> A newExternalAnnotation(String parentId, String xpath,
            String entityId) {
        A annotation = newAnnotation(parentId, xpath);
        annotation.setEntityId(entityId);
        annotation.setOrigin("Test");
        return annotation;
    }

    /**
     * @return a mutable {@link Annotation} made by an external system for creation
     */
    public static <A extends Annotation & ExternalEntity> A newExternalAnnotation(String parentId, String xpath,
            String entityId, String entity) {
        A annotation = newExternalAnnotation(parentId, xpath, entityId);
        annotation.setEntity(entity);
        return annotation;
    }

    /**
     * @return a mutable {@link Annotation} made by an external system for creation
     */
    public static <A extends Annotation & ExternalEntity> A newExternalAnnotation(String parentId, String xpath,
            String entityId, String entity, String text) {
        A annotation = newExternalAnnotation(parentId, xpath, entityId, entity);
        annotation.setText(text);
        return annotation;
    }

}
