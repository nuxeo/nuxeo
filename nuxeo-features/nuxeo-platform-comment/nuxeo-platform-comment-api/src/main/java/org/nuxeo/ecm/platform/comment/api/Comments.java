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

import static org.nuxeo.ecm.platform.comment.api.AnnotationConstants.ANNOTATION_XPATH_PROPERTY;
import static org.nuxeo.ecm.platform.comment.api.ExternalEntityConstants.EXTERNAL_ENTITY_FACET;
import static org.nuxeo.ecm.platform.comment.api.ExternalEntityConstants.EXTERNAL_ENTITY_ID_PROPERTY;
import static org.nuxeo.ecm.platform.comment.api.ExternalEntityConstants.EXTERNAL_ENTITY_PROPERTY;
import static org.nuxeo.ecm.platform.comment.api.ExternalEntityConstants.EXTERNAL_ENTITY_ORIGIN_PROPERTY;
import static org.nuxeo.ecm.platform.comment.workflow.utils.CommentsConstants.COMMENT_ANCESTOR_IDS;
import static org.nuxeo.ecm.platform.comment.workflow.utils.CommentsConstants.COMMENT_AUTHOR;
import static org.nuxeo.ecm.platform.comment.workflow.utils.CommentsConstants.COMMENT_CREATION_DATE;
import static org.nuxeo.ecm.platform.comment.workflow.utils.CommentsConstants.COMMENT_PARENT_ID;
import static org.nuxeo.ecm.platform.comment.workflow.utils.CommentsConstants.COMMENT_MODIFICATION_DATE;
import static org.nuxeo.ecm.platform.comment.workflow.utils.CommentsConstants.COMMENT_TEXT;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Calendar;
import java.util.Collection;
import java.util.GregorianCalendar;

import org.nuxeo.ecm.core.api.DocumentModel;

/**
 * Utility class to convert document model from/to comments, annotations or external entities.
 * 
 * @since 10.3
 * @deprecated since 11.1, use {@link Comment#getDocument()} or {@link DocumentModel#getAdapter(Class)} instead
 */
@Deprecated
public class Comments {

    private Comments() {
        // no instance allowed
    }

    /**
     * @deprecated since 11.1, use {@link Comment#getDocument()} with {@code DocumentModelJsonReader} instead
     */
    @Deprecated
    public static void commentToDocumentModel(Comment comment, DocumentModel documentModel) {
        // Do not set ancestor ids as it is computed at document creation
        documentModel.setPropertyValue(COMMENT_AUTHOR, comment.getAuthor());
        documentModel.setPropertyValue(COMMENT_TEXT, comment.getText());
        documentModel.setPropertyValue(COMMENT_PARENT_ID, comment.getParentId());
        Instant creationDate = comment.getCreationDate();
        if (creationDate != null) {
            documentModel.setPropertyValue(COMMENT_CREATION_DATE,
                    GregorianCalendar.from(ZonedDateTime.ofInstant(creationDate, ZoneId.systemDefault())));
        }
        Instant modificationDate = comment.getModificationDate();
        if (modificationDate != null) {
            documentModel.setPropertyValue(COMMENT_MODIFICATION_DATE,
                    GregorianCalendar.from(ZonedDateTime.ofInstant(modificationDate, ZoneId.systemDefault())));
        }
    }

    /**
     * @deprecated since 11.1, unused
     */
    @Deprecated
    public static void annotationToDocumentModel(Annotation annotation, DocumentModel documentModel) {
        commentToDocumentModel(annotation, documentModel);
        documentModel.setPropertyValue(ANNOTATION_XPATH_PROPERTY, annotation.getXpath());
    }

    /**
     * @deprecated since 11.1, unused
     */
    @Deprecated
    public static void externalEntityToDocumentModel(ExternalEntity entity, DocumentModel documentModel) {
        documentModel.setPropertyValue(EXTERNAL_ENTITY_ID_PROPERTY, entity.getEntityId());
        documentModel.setPropertyValue(EXTERNAL_ENTITY_ORIGIN_PROPERTY, entity.getOrigin());
        documentModel.setPropertyValue(EXTERNAL_ENTITY_PROPERTY, entity.getEntity());
    }

    /**
     * @deprecated since 11.1, unused
     */
    @Deprecated
    @SuppressWarnings("unchecked")
    public static void documentModelToComment(DocumentModel documentModel, Comment comment) {
        comment.setId(documentModel.getId());
        comment.setAuthor((String) documentModel.getPropertyValue(COMMENT_AUTHOR));
        comment.setText((String) documentModel.getPropertyValue(COMMENT_TEXT));
        Collection<String> ancestorIds = (Collection<String>) documentModel.getPropertyValue(COMMENT_ANCESTOR_IDS);
        ancestorIds.forEach(comment::addAncestorId);
        String parentId = (String) documentModel.getPropertyValue(COMMENT_PARENT_ID);
        comment.setParentId(parentId);

        Calendar creationDate = (Calendar) documentModel.getPropertyValue(COMMENT_CREATION_DATE);
        if (creationDate != null) {
            comment.setCreationDate(creationDate.toInstant());
        }
        Calendar modificationDate = (Calendar) documentModel.getPropertyValue(COMMENT_MODIFICATION_DATE);
        if (modificationDate != null) {
            comment.setModificationDate(modificationDate.toInstant());
        }
    }

    /**
     * @deprecated since 11.1, unused
     */
    @Deprecated
    public static void documentModelToAnnotation(DocumentModel documentModel, Annotation annotation) {
        documentModelToComment(documentModel, annotation);
        annotation.setXpath((String) documentModel.getPropertyValue(ANNOTATION_XPATH_PROPERTY));
    }

    /**
     * @deprecated since 11.1, unused
     */
    @Deprecated
    public static void documentModelToExternalEntity(DocumentModel documentModel, ExternalEntity entity) {
        if (documentModel.hasFacet(EXTERNAL_ENTITY_FACET)) {
            entity.setEntityId((String) documentModel.getPropertyValue(EXTERNAL_ENTITY_ID_PROPERTY));
            entity.setOrigin((String) documentModel.getPropertyValue(EXTERNAL_ENTITY_ORIGIN_PROPERTY));
            entity.setEntity((String) documentModel.getPropertyValue(EXTERNAL_ENTITY_PROPERTY));
        }
    }

    /**
     * @deprecated since 11.1, use {@link DocumentModel#getAdapter(Class)} with {@link Comment} class instead
     */
    @Deprecated
    public static Comment newComment(DocumentModel commentModel) {
        Comment comment = new CommentImpl();
        documentModelToComment(commentModel, comment);
        documentModelToExternalEntity(commentModel, (ExternalEntity) comment);
        return comment;
    }

    /**
     * @deprecated since 11.1, use {@link DocumentModel#getAdapter(Class)} with {@link Annotation} class instead
     */
    @Deprecated
    public static Annotation newAnnotation(DocumentModel annotationModel) {
        Annotation annotation = new AnnotationImpl();
        documentModelToAnnotation(annotationModel, annotation);
        documentModelToExternalEntity(annotationModel, (ExternalEntity) annotation);
        return annotation;
    }
}
