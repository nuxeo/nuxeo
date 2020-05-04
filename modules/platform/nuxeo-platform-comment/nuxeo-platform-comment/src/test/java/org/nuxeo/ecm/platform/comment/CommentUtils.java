/*
 * (C) Copyright 2019-2020 Nuxeo (http://nuxeo.com/) and others.
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
import static org.nuxeo.ecm.platform.comment.api.CommentConstants.COMMENT_AUTHOR_PROPERTY;
import static org.nuxeo.ecm.platform.comment.api.CommentConstants.COMMENT_DOCUMENT;
import static org.nuxeo.ecm.platform.comment.api.CommentConstants.COMMENT_SCHEMA;
import static org.nuxeo.ecm.platform.comment.api.CommentConstants.COMMENT_TEXT_PROPERTY;
import static org.nuxeo.ecm.platform.comment.api.CommentConstants.PARENT_COMMENT;
import static org.nuxeo.ecm.platform.comment.api.CommentConstants.TOP_LEVEL_DOCUMENT;
import static org.nuxeo.ecm.platform.comment.api.CommentEvents.COMMENT_ADDED;

import java.io.IOException;
import java.io.Serializable;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.nuxeo.common.utils.StringUtils;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.NuxeoException;
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
import org.nuxeo.mail.SmtpMailServerFeature.MailMessage;
import org.nuxeo.runtime.api.Framework;

import net.htmlparser.jericho.Renderer;
import net.htmlparser.jericho.Source;

/**
 * @since 11.1
 */
public class CommentUtils {

    private static final Logger log = LogManager.getLogger(CommentUtils.class);

    public static final SimpleDateFormat EVENT_DATE_FORMAT = new SimpleDateFormat("dd/MM/yyyy - HH:mm");

    private CommentUtils() {
        // utility class
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
        assertEquals(expectedCommentDocModel.getPropertyValue(COMMENT_TEXT_PROPERTY), properties.get(COMMENT));

        // The event source document must be the top level document as is the one linked in the notification.
        assertTrue(properties.containsKey(TOP_LEVEL_DOCUMENT));
        DocumentModel topLevelDocument = (DocumentModel) properties.get(TOP_LEVEL_DOCUMENT);
        assertEquals(expectedTopLevelDocumentModel.getRef(), topLevelDocument.getRef());
        DocumentModel sourceDoc = ((DocumentEventContext) event.getContext()).getSourceDocument();
        assertEquals(expectedCommentedDocModel.getRef(), sourceDoc.getRef());
    }

    public static String getExpectedMailContent(DocumentModel commentDocModel, DocumentModel commentedDocModel,
            Event event) {
        try {
            var model = new HashMap<String, Serializable>();
            model.put("COMMENT_AUTHOR", commentDocModel.getPropertyValue(COMMENT_AUTHOR_PROPERTY));
            model.put("COMMENT_ACTION", COMMENT_ADDED.equals(event.getName()) ? "added" : "updated");
            model.put("COMMENTED_DOCUMENT", commentedDocModel.getName());
            model.put("COMMENT_DATE", EVENT_DATE_FORMAT.format(Date.from(Instant.ofEpochMilli(event.getTime()))));
            model.put("COMMENT_TEXT", commentDocModel.getPropertyValue(COMMENT_TEXT_PROPERTY));
            model.put("COMMENT_SUBSCRIPTION_NAME", COMMENT_ADDED.equals(event.getName()) ? "New" : "Updated");

            String template = "commentNotificationMail.txt";
            DocumentModel parentComment = (DocumentModel) event.getContext().getProperties().get(PARENT_COMMENT);
            if (parentComment.hasSchema(COMMENT_SCHEMA)) {
                template = "commentReplyNotificationMail.txt";
                model.put("PARENT_COMMENT_AUTHOR", parentComment.getPropertyValue("comment:author"));
                model.put("PARENT_COMMENT_TEXT", parentComment.getPropertyValue("comment:text"));
            }
            String content = Files.readString(
                    Paths.get(CommentUtils.class.getResource(String.format("/templates/%s", template)).toURI()));
            return StringUtils.expandVars(content, model);
        } catch (URISyntaxException | IOException e) {
            throw new NuxeoException(e);
        }
    }

    public static String getMailContent(MailMessage mailMessage) {
        String content = mailMessage.getContent();
        if (mailMessage.getContentType().contains("text/html")) {
            Renderer renderer = new Source(mailMessage.getContent()).getRenderer();
            renderer.setIncludeHyperlinkURLs(false);
            renderer.setDecorateFontStyles(false);
            renderer.setNewLine("\n");
            // Set max line otherwise the parser (jericho library) will automatically add a `\n` when a line length is
            // greater than the default value Render#DEFAULT_LINE_LENGTH, the new value is computed from the ftl
            // template see `baseComment.ftl` and footer line
            renderer.setMaxLineLength(150);

            return renderer.toString();
        }
        return content;
    }

    public static void createUser(String userName) {
        try {
            UserManager userManager = Framework.getService(UserManager.class);
            DocumentModel userModel = userManager.getBareUserModel();
            userModel.setProperty("user", "username", userName);
            userModel.setProperty("user", "email", userName + "@nuxeo.com");
            userModel.setProperty("user", "password", userName);
            userModel.setProperty("user", "groups", List.of("members"));
            userManager.createUser(userModel);
        } catch (UserAlreadyExistsException e) {
            // Avoid failure in tests if the user already exists
            log.trace("User already exists", e);
        }
    }

    public static void addNotificationSubscriptions(NuxeoPrincipal principal, DocumentModel docToSubscribe,
            String... notifications) {
        var notificationManager = Framework.getService(NotificationManager.class);
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
        var annotation = new AnnotationImpl();
        annotation.setParentId(parentId);
        annotation.setXpath(xpath);
        annotation.setCreationDate(Instant.now());
        annotation.setModificationDate(Instant.now());
        return (A) annotation;
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
