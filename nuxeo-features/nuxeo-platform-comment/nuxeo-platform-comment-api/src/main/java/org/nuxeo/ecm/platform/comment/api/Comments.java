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

import static org.nuxeo.ecm.platform.comment.api.AnnotationConstants.ANNOTATION_DOC_TYPE;
import static org.nuxeo.ecm.platform.comment.api.AnnotationConstants.ANNOTATION_XPATH_PROPERTY;
import static org.nuxeo.ecm.platform.comment.api.ExternalEntityConstants.EXTERNAL_ENTITY_FACET;
import static org.nuxeo.ecm.platform.comment.api.ExternalEntityConstants.EXTERNAL_ENTITY_ID_PROPERTY;
import static org.nuxeo.ecm.platform.comment.api.ExternalEntityConstants.EXTERNAL_ENTITY_ORIGIN_PROPERTY;
import static org.nuxeo.ecm.platform.comment.api.ExternalEntityConstants.EXTERNAL_ENTITY_PROPERTY;
import static org.nuxeo.ecm.platform.comment.workflow.utils.CommentsConstants.COMMENT_ANCESTOR_IDS;
import static org.nuxeo.ecm.platform.comment.workflow.utils.CommentsConstants.COMMENT_AUTHOR;
import static org.nuxeo.ecm.platform.comment.workflow.utils.CommentsConstants.COMMENT_CREATION_DATE;
import static org.nuxeo.ecm.platform.comment.workflow.utils.CommentsConstants.COMMENT_DOC_TYPE;
import static org.nuxeo.ecm.platform.comment.workflow.utils.CommentsConstants.COMMENT_MODIFICATION_DATE;
import static org.nuxeo.ecm.platform.comment.workflow.utils.CommentsConstants.COMMENT_PARENT_ID;
import static org.nuxeo.ecm.platform.comment.workflow.utils.CommentsConstants.COMMENT_TEXT;

import java.time.Instant;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;

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

    /**
     * @deprecated since 11.1 because of public access, use {@link #toDocumentModel(Comment, DocumentModel)} instead.
     */
    @Deprecated(since = "11.1")
    public static void commentToDocumentModel(Comment comment, DocumentModel documentModel) {
        // Do not set ancestor ids as it is computed at document creation
        documentModel.setPropertyValue(COMMENT_AUTHOR, comment.getAuthor());
        documentModel.setPropertyValue(COMMENT_TEXT, comment.getText());
        documentModel.setPropertyValue(COMMENT_PARENT_ID, comment.getParentId());
        Instant creationDate = comment.getCreationDate();
        if (creationDate != null) {
            documentModel.setPropertyValue(COMMENT_CREATION_DATE, Date.from(creationDate));
        }
        Instant modificationDate = comment.getModificationDate();
        if (modificationDate != null) {
            documentModel.setPropertyValue(COMMENT_MODIFICATION_DATE, Date.from(modificationDate));
        }
    }

    /**
     * @deprecated since 11.1 because of public access, use {@link #toDocumentModel(Comment, DocumentModel)} instead.
     */
    @Deprecated(since = "11.1")
    public static void annotationToDocumentModel(Annotation annotation, DocumentModel documentModel) {
        commentToDocumentModel(annotation, documentModel);
        documentModel.setPropertyValue(ANNOTATION_XPATH_PROPERTY, annotation.getXpath());
    }

    /**
     * @deprecated since 11.1 because of public access.
     */
    @Deprecated(since = "11.1")
    public static void externalEntityToDocumentModel(ExternalEntity entity, DocumentModel documentModel) {
        documentModel.setPropertyValue(EXTERNAL_ENTITY_ID_PROPERTY, entity.getEntityId());
        documentModel.setPropertyValue(EXTERNAL_ENTITY_ORIGIN_PROPERTY, entity.getOrigin());
        documentModel.setPropertyValue(EXTERNAL_ENTITY_PROPERTY, entity.getEntity());
    }

    /**
     * @deprecated since 11.1 because of public access, use {@link #toComment(DocumentModel)} instead.
     */
    @Deprecated(since = "11.1")
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
     * @deprecated since 11.1 because of public access, use {@link #toComment(DocumentModel)} instead.
     */
    @Deprecated(since = "11.1")
    public static void documentModelToAnnotation(DocumentModel documentModel, Annotation annotation) {
        documentModelToComment(documentModel, annotation);
        annotation.setXpath((String) documentModel.getPropertyValue(ANNOTATION_XPATH_PROPERTY));
    }

    /**
     * @deprecated since 11.1 because of public access.
     */
    @Deprecated(since = "11.1")
    public static void documentModelToExternalEntity(DocumentModel documentModel, ExternalEntity entity) {
        if (documentModel.hasFacet(EXTERNAL_ENTITY_FACET)) {
            entity.setEntityId((String) documentModel.getPropertyValue(EXTERNAL_ENTITY_ID_PROPERTY));
            entity.setOrigin((String) documentModel.getPropertyValue(EXTERNAL_ENTITY_ORIGIN_PROPERTY));
            entity.setEntity((String) documentModel.getPropertyValue(EXTERNAL_ENTITY_PROPERTY));
        }
    }

    /**
     * @deprecated since 11.1 because of public access, use {@link #toComment(DocumentModel)} instead.
     */
    @Deprecated(since = "11.1")
    public static Comment newComment(DocumentModel commentModel) {
        Comment comment = new CommentImpl();
        documentModelToComment(commentModel, comment);
        documentModelToExternalEntity(commentModel, (ExternalEntity) comment);
        return comment;
    }

    /**
     * @deprecated since 11.1 because of public access, use {@link #toComment(DocumentModel)} instead.
     */
    @Deprecated(since = "11.1")
    public static Annotation newAnnotation(DocumentModel annotationModel) {
        Annotation annotation = new AnnotationImpl();
        documentModelToAnnotation(annotationModel, annotation);
        documentModelToExternalEntity(annotationModel, (ExternalEntity) annotation);
        return annotation;
    }

    /**
     * @return the comment {@link #newComment} or the annotation {@link #newAnnotation} depending on the document model
     *         type
     * @throws IllegalArgumentException if the doc model type is unknown
     * @since 11.1
     **/
    public static Comment toComment(DocumentModel documentModel) {
        String docType = documentModel.getType();
        switch (docType) {
        case COMMENT_DOC_TYPE:
            return newComment(documentModel);
        case ANNOTATION_DOC_TYPE:
            return newAnnotation(documentModel);
        default:
            throw new IllegalArgumentException(String.format("Undefined behaviour for doc type: %s", docType));
        }
    }

    /**
     * Builds the document model from {@code comment} depending on his class type {@link #commentToDocumentModel},
     * {@link #annotationToDocumentModel}.
     * 
     * @since 11.1
     **/
    public static void toDocumentModel(Comment comment, DocumentModel documentModel) {
        if (comment instanceof ExternalEntity) {
            documentModel.addFacet(EXTERNAL_ENTITY_FACET);
            externalEntityToDocumentModel((ExternalEntity) comment, documentModel);
        }

        if (comment instanceof Annotation) {
            annotationToDocumentModel((Annotation) comment, documentModel);
        } else {
            commentToDocumentModel(comment, documentModel);
        }
    }

    /**
     * @return the comment document type depending on the given {@code comment}
     * @since 11.1
     **/
    public static String getDocumentType(Comment comment) {
        if (comment instanceof Annotation) {
            return ANNOTATION_DOC_TYPE;
        }

        return COMMENT_DOC_TYPE;
    }
}
