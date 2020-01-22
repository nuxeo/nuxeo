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
import static org.nuxeo.ecm.platform.comment.api.CommentEvents.COMMENT_ADDED;
import static org.nuxeo.ecm.platform.comment.api.CommentEvents.COMMENT_REMOVED;
import static org.nuxeo.ecm.platform.comment.workflow.utils.CommentsConstants.COMMENT_AUTHOR;
import static org.nuxeo.ecm.platform.comment.workflow.utils.CommentsConstants.COMMENT_TEXT;

import java.io.IOException;
import java.io.Serializable;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.nuxeo.common.utils.StringUtils;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.core.event.Event;
import org.nuxeo.ecm.core.event.impl.DocumentEventContext;
import org.nuxeo.mail.SmtpMailServerFeature.MailMessage;

import net.htmlparser.jericho.Renderer;
import net.htmlparser.jericho.Source;

/**
 * @since 11.1
 */
public class CommentUtils {

    public static final SimpleDateFormat EVENT_DATE_FORMAT = new SimpleDateFormat("dd/MM/yyyy - HH:mm");

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

    /**
     * Checks the received mail, sent when a comment / annotation is created or updated.
     *
     * @param mails the current mails
     * @param commentDocModel the document model of the comment, cannot be {@code null}
     * @param commentedDocModel the document being commented, cannot be {@code null}
     * @param event the event, cannot be {@code null}
     * @param commentEventType the type of comment event {@link org.nuxeo.ecm.platform.comment.api.CommentEvents},
     *            cannot be {@code null}
     */
    public static void checkReceivedMail(List<MailMessage> mails, DocumentModel commentDocModel,
            DocumentModel commentedDocModel, Event event, String commentEventType) {
        int expectedMailsCount = COMMENT_REMOVED.equals(commentEventType) ? 0 : 1;
        assertEquals(expectedMailsCount, mails.size());

        if (expectedMailsCount > 0) {
            String subject = String.format("[Nuxeo5]%s comment on '%s'",
                    COMMENT_ADDED.equals(event.getName()) ? "New" : "Updated", commentedDocModel.getName());
            List<MailMessage> mailsBySubject = mails.stream()
                                                    .filter(m -> subject.equals(m.getSubject()))
                                                    .collect(Collectors.toList());
            assertEquals(1, mailsBySubject.size());

            String expectedMailContent = getExpectedMailContent(commentDocModel, commentedDocModel, event,
                    commentEventType);
            assertEquals(expectedMailContent, getMailContent(mailsBySubject.get(0)));
        }
    }

    public static String getExpectedMailContent(DocumentModel commentDocModel, DocumentModel commentedDocModel,
            Event event, String commentEventType) {
        URL url = CommentUtils.class.getResource("/templates/commentNotificationMail.txt");
        try {
            String content = Files.readString(Paths.get(url.toURI()));
            var model = Map.of("COMMENT_AUTHOR", commentDocModel.getPropertyValue(COMMENT_AUTHOR), //
                    "COMMENT_ACTION", COMMENT_ADDED.equals(commentEventType) ? "added" : "updated", //
                    "COMMENTED_DOCUMENT", commentedDocModel.getName(), //
                    "COMMENT_DATE", EVENT_DATE_FORMAT.format(Date.from(Instant.ofEpochMilli(event.getTime()))), //
                    "COMMENT_TEXT", commentDocModel.getPropertyValue(COMMENT_TEXT), //
                    "COMMENT_SUBSCRIPTION_NAME", COMMENT_ADDED.equals(commentEventType) ? "New" : "Updated");
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
}
