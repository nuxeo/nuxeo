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
 *     Nuno Cunha <ncunha@nuxeo.com>
 */

package org.nuxeo.ecm.platform.comment.api;

import java.util.List;

import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.platform.comment.api.exceptions.CommentNotFoundException;
import org.nuxeo.ecm.platform.comment.api.exceptions.CommentSecurityException;

/**
 * Annotation API to perform CRUD operations on annotations.
 *
 * @since 10.1
 */
public interface AnnotationService {

    /**
     * Creates an annotation on a document.
     *
     * @param session the core session
     * @param annotation the annotation to create
     * @return the created annotation
     * @throws CommentSecurityException if the current user does have the right permissions on the document to annotate.
     */
    Annotation createAnnotation(CoreSession session, Annotation annotation) throws CommentSecurityException;

    /**
     * Gets an annotation.
     *
     * @param session the core session
     * @param annotationId the annotation document model id
     * @return the annotation, or {@code null} if the annotation with the given id was not found
     * @throws CommentNotFoundException if no annotation was found with the given id
     * @throws CommentSecurityException if the current user does have the right permissions on the annotated document.
     */
    Annotation getAnnotation(CoreSession session, String annotationId)
            throws CommentNotFoundException, CommentSecurityException;

    /**
     * Gets all annotations for a document.
     *
     * @param session the core session
     * @param documentId the annotated document id
     * @param xpath the annotated blob xpath in the document
     * @throws CommentNotFoundException if no annotation was found with the given id
     * @throws CommentSecurityException if the current user does have the right permissions on the annotated document.
     * @return the list of annotations, or an empty list if no annotation is found
     */
    List<Annotation> getAnnotations(CoreSession session, String documentId, String xpath)
            throws CommentNotFoundException, CommentSecurityException;

    /**
     * Updates an annotation for a document.
     *
     * @param session the core session
     * @param annotationId the annotation document model id
     * @param annotation the annotation containing the modifications
     * @throws CommentNotFoundException if no annotation was found with the given id
     * @throws CommentSecurityException if the current user does have the right permissions on the annotated document.
     */
    void updateAnnotation(CoreSession session, String annotationId, Annotation annotation)
            throws CommentNotFoundException, CommentSecurityException;

    /**
     * Deletes an annotation for a document.
     *
     * @param session the core session
     * @param annotationId the annotation document model id
     * @throws CommentNotFoundException if no annotation was found with the given id
     * @throws CommentSecurityException if the current user does have the right permissions on the annotated document.
     */
    void deleteAnnotation(CoreSession session, String annotationId)
            throws CommentNotFoundException, CommentSecurityException;

    /**
     * @since 10.3
     * @deprecated since 11.1, use {@link #getExternalAnnotation(CoreSession, String, String)} instead
     */
    @Deprecated
    Annotation getExternalAnnotation(CoreSession session, String entityId)
            throws CommentNotFoundException, CommentSecurityException;

    /**
     * Gets an external annotation by its {@code entityId} under the document with {@code documentId}.
     *
     * @return the annotation with given {@code entityId} under the document with given {@code documentId}
     * @throws CommentNotFoundException if no annotation was found with the given external entity id
     * @throws CommentSecurityException if the current user does have the right permissions on the annotated document.
     * @since 11.1
     */
    default Annotation getExternalAnnotation(CoreSession session, String documentId, String entityId) {
        throw new UnsupportedOperationException();
    }

    /**
     * @since 10.3
     * @deprecated since 11.1, use {@link #updateExternalAnnotation(CoreSession, String, String, Annotation)} instead
     */
    @Deprecated
    void updateExternalAnnotation(CoreSession session, String entityId, Annotation annotation)
            throws CommentNotFoundException, CommentSecurityException;

    /**
     * Updates an external annotation by its {@code entityId} under the document with {@code documentId}.
     *
     * @return the updated annotation with given {@code entityId} under the document with given {@code documentId}
     * @throws CommentNotFoundException if no annotation was found with the given external entity id
     * @throws CommentSecurityException if the current user does have the right permissions on the annotated document.
     * @since 11.1
     */
    default Annotation updateExternalAnnotation(CoreSession session, String documentId, String entityId,
            Annotation annotation) {
        throw new UnsupportedOperationException();
    }

    /**
     * @since 10.3
     * @deprecated since 11.1, use {@link #deleteExternalAnnotation(CoreSession, String, String)} instead
     */
    @Deprecated
    void deleteExternalAnnotation(CoreSession session, String entityId)
            throws CommentNotFoundException, CommentSecurityException;

    /**
     * Deletes an external annotation by its {code entityId} under the document with {@code documentId}.
     *
     * @throws CommentNotFoundException if no annotation was found with the given external entity id
     * @throws CommentSecurityException if the current user does have the right permissions on the annotated document.
     * @since 11.1
     */
    default void deleteExternalAnnotation(CoreSession session, String documentId, String entityId) {
        throw new UnsupportedOperationException();
    }

}
