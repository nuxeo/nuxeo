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
 *
 */

package org.nuxeo.ecm.annotation;

import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;

import java.util.List;

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
     * @param documentModel the document model to annotate
     * @param annotation the annotation to create
     * @return the created annotation
     */
    DocumentModel createAnnotation(CoreSession session, DocumentModel documentModel, DocumentModel annotation);

    /**
     * Gets an annotation.
     *
     * @param session the core session
     * @param annotationId the annotation id
     * @return the annotation
     */
    DocumentModel getAnnotation(CoreSession session, String annotationId);

    /**
     * Updates an annotation.
     *
     * @param session the core session
     * @param annotation the annotation document model containing the modifications
     * @return the updated annotation
     */
    DocumentModel updateAnnotation(CoreSession session, DocumentModel annotation);

    /**
     * Deletes an annotation.
     *
     * @param session the core session
     * @param annotationId the annotation id
     */
    void deleteAnnotation(CoreSession session, String annotationId);

    /**
     * Finds annotations based on a query builder.
     *
     * @param session the core session
     * @param queryBuilder the query builder
     * @return the list of results
     */
    List<DocumentModel> queryAnnotations(CoreSession session, AnnotationQueryBuilder queryBuilder);

}
