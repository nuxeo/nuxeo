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

package org.nuxeo.ecm.platform.comment.api;

import static org.nuxeo.ecm.platform.comment.api.ExternalEntityConstants.EXTERNAL_ENTITY_ID_PROPERTY;
import static org.nuxeo.ecm.platform.comment.api.ExternalEntityConstants.EXTERNAL_ENTITY_PROPERTY;
import static org.nuxeo.ecm.platform.comment.api.ExternalEntityConstants.EXTERNAL_ENTITY_ORIGIN_PROPERTY;
import static org.nuxeo.ecm.platform.comment.api.AnnotationConstants.ANNOTATION_XPATH_PROPERTY;
import static org.nuxeo.ecm.platform.comment.api.ExternalEntityConstants.EXTERNAL_ENTITY_FACET;
import static org.nuxeo.ecm.platform.comment.workflow.utils.CommentsConstants.COMMENT_AUTHOR;
import static org.nuxeo.ecm.platform.comment.workflow.utils.CommentsConstants.COMMENT_CREATION_DATE;
import static org.nuxeo.ecm.platform.comment.workflow.utils.CommentsConstants.COMMENT_DOCUMENT_ID;
import static org.nuxeo.ecm.platform.comment.workflow.utils.CommentsConstants.COMMENT_MODIFICATION_DATE;
import static org.nuxeo.ecm.platform.comment.workflow.utils.CommentsConstants.COMMENT_TEXT;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.function.BiConsumer;

import org.nuxeo.ecm.core.api.DocumentModel;

/**
 * Utility class to convert document model from/to comments, annotations or external entities.
 * 
 * @since 10.3
 */
public class Comments {

    private Comments() {
        // no instance allowed
    }

    public static BiConsumer<Comment, DocumentModel> commentToDocumentModel() {
        return (comment, commentModel) -> {
            commentModel.setPropertyValue(COMMENT_AUTHOR, comment.getAuthor());
            commentModel.setPropertyValue(COMMENT_TEXT, comment.getText());
            commentModel.setPropertyValue(COMMENT_DOCUMENT_ID, comment.getDocumentId());
            Instant creationDate = comment.getCreationDate();
            if (creationDate != null) {
                commentModel.setPropertyValue(COMMENT_CREATION_DATE,
                        GregorianCalendar.from(ZonedDateTime.ofInstant(creationDate, ZoneId.systemDefault())));
            }
            Instant modificationDate = comment.getModificationDate();
            if (modificationDate != null) {
                commentModel.setPropertyValue(COMMENT_MODIFICATION_DATE,
                        GregorianCalendar.from(ZonedDateTime.ofInstant(modificationDate, ZoneId.systemDefault())));
            }
        };
    }

    public static BiConsumer<Comment, DocumentModel> annotationToDocumentModel() {
        return commentToDocumentModel().andThen((comment, commentModel) -> commentModel.setPropertyValue(
                ANNOTATION_XPATH_PROPERTY, ((Annotation) comment).getXpath()));
    }

    public static BiConsumer<Comment, DocumentModel> externalEntityToDocumentModel() {
        return (comment, commentModel) -> {
            if (((ExternalEntity) comment).getEntityId() != null) {
                commentModel.addFacet(EXTERNAL_ENTITY_FACET);
                commentModel.setPropertyValue(EXTERNAL_ENTITY_ID_PROPERTY, ((ExternalEntity) comment).getEntityId());
                commentModel.setPropertyValue(EXTERNAL_ENTITY_ORIGIN_PROPERTY, ((ExternalEntity) comment).getOrigin());
                commentModel.setPropertyValue(EXTERNAL_ENTITY_PROPERTY, ((ExternalEntity) comment).getEntity());
            }
        };
    }

    public static BiConsumer<Comment, DocumentModel> externalCommentToDocumentModel() {
        return commentToDocumentModel().andThen(externalEntityToDocumentModel());
    }

    public static BiConsumer<Comment, DocumentModel> externalAnnotationToDocumentModel() {
        return annotationToDocumentModel().andThen(externalEntityToDocumentModel());
    }

    public static BiConsumer<DocumentModel, Comment> documentModelToComment() {
        return (commentModel, comment) -> {
            comment.setId(commentModel.getId());
            comment.setAuthor((String) commentModel.getPropertyValue(COMMENT_AUTHOR));
            comment.setText((String) commentModel.getPropertyValue(COMMENT_TEXT));
            comment.setDocumentId((String) commentModel.getPropertyValue(COMMENT_DOCUMENT_ID));
            Calendar creationDate = (Calendar) commentModel.getPropertyValue(COMMENT_CREATION_DATE);
            if (creationDate != null) {
                comment.setCreationDate(creationDate.toInstant());
            }
            Calendar modificationDate = (Calendar) commentModel.getPropertyValue(COMMENT_MODIFICATION_DATE);
            if (modificationDate != null) {
                comment.setModificationDate(modificationDate.toInstant());
            }
        };
    }

    public static BiConsumer<DocumentModel, Comment> documentModelToAnnotation() {
        return documentModelToComment().andThen(((commentModel, comment) -> ((Annotation) comment).setXpath(
                (String) commentModel.getPropertyValue(ANNOTATION_XPATH_PROPERTY))));
    }

    public static BiConsumer<DocumentModel, Comment> documentModelToExternalEntity() {
        return (commentModel, comment) -> {
            if (commentModel.hasFacet(EXTERNAL_ENTITY_FACET)) {
                ((ExternalEntity) comment).setEntityId(
                        (String) commentModel.getPropertyValue(EXTERNAL_ENTITY_ID_PROPERTY));
                ((ExternalEntity) comment).setOrigin(
                        (String) commentModel.getPropertyValue(EXTERNAL_ENTITY_ORIGIN_PROPERTY));
                ((ExternalEntity) comment).setEntity(
                        (String) commentModel.getPropertyValue(EXTERNAL_ENTITY_PROPERTY));
            }
        };
    }

    public static BiConsumer<DocumentModel, Comment> documentModelToExternalComment() {
        return documentModelToComment().andThen(documentModelToExternalEntity());
    }

    public static BiConsumer<DocumentModel, Comment>  documentModelToExternalAnnotation() {
        return documentModelToAnnotation().andThen(documentModelToExternalEntity());
    }
}
