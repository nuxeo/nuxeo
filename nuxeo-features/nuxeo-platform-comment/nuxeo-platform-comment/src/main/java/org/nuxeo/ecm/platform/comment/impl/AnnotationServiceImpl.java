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

package org.nuxeo.ecm.platform.comment.impl;

import static org.nuxeo.ecm.platform.comment.api.AnnotationConstants.ANNOTATION_DOC_TYPE;
import static org.nuxeo.ecm.platform.comment.api.AnnotationConstants.ANNOTATION_XPATH_PROPERTY;
import static org.nuxeo.ecm.platform.query.nxql.CoreQueryAndFetchPageProvider.CORE_SESSION_PROPERTY;

import java.io.Serializable;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.platform.comment.api.Annotation;
import org.nuxeo.ecm.platform.comment.api.AnnotationImpl;
import org.nuxeo.ecm.platform.comment.api.AnnotationService;
import org.nuxeo.ecm.platform.comment.api.CommentManager;
import org.nuxeo.ecm.platform.comment.api.Comments;
import org.nuxeo.ecm.platform.query.api.PageProvider;
import org.nuxeo.ecm.platform.query.api.PageProviderService;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.model.DefaultComponent;

/**
 * @since 10.1
 */
public class AnnotationServiceImpl extends DefaultComponent implements AnnotationService {

    private static final Log log = LogFactory.getLog(AnnotationServiceImpl.class);

    protected static final String GET_ANNOTATION_PAGEPROVIDER_NAME = "GET_ANNOTATION";

    @Override
    public Annotation createAnnotation(CoreSession session, Annotation annotation) {
        // Create base comment in the annotation
        DocumentModel docToAnnotate = session.getDocument(new IdRef(annotation.getDocumentId()));
        DocumentModel annotationModel = session.createDocumentModel(ANNOTATION_DOC_TYPE);
        Comments.externalAnnotationToDocumentModel().accept(annotation, annotationModel);
        annotationModel = Framework.getService(CommentManager.class).createComment(docToAnnotate, annotationModel);

        Annotation createdAnnotation = new AnnotationImpl();
        Comments.documentModelToExternalAnnotation().accept(annotationModel, createdAnnotation);
        return createdAnnotation;
    }

    @Override
    public Annotation getAnnotation(CoreSession session, String annotationId) {
        DocumentRef commentRef = new IdRef(annotationId);
        if (!session.exists(commentRef)) {
            throw new IllegalArgumentException("The document " + annotationId + " does not exist.");
        }
        DocumentModel annotationModel = session.getDocument(commentRef);
        Annotation annotation = new AnnotationImpl();
        Comments.documentModelToExternalAnnotation().accept(annotationModel, annotation);
        return annotation;
    }

    @Override
    public List<Annotation> getAnnotations(CoreSession session, String documentId, String xpath) {
        if (!session.exists(new IdRef(documentId))) {
            throw new IllegalArgumentException("The document " + documentId + " does not exist.");
        }
        DocumentModel annotatedDoc = session.getDocument(new IdRef(documentId));
        return Framework.getService(CommentManager.class)
                        .getComments(annotatedDoc)
                        .stream()
                        .filter(annotationModel -> xpath.equals(
                                annotationModel.getPropertyValue(ANNOTATION_XPATH_PROPERTY)))
                        .map(annotationModel -> {
                            Annotation annotation = new AnnotationImpl();
                            Comments.documentModelToExternalAnnotation().accept(annotationModel, annotation);
                            return annotation;
                        })
                        .collect(Collectors.toList());
    }

    @Override
    public void updateAnnotation(CoreSession session, String annotationId, Annotation annotation) {
        Framework.getService(CommentManager.class).updateComment(session, annotationId, annotation);
    }

    @Override
    public void deleteAnnotation(CoreSession session, String annotationId) throws IllegalArgumentException {
        Framework.getService(CommentManager.class).deleteComment(session, annotationId);
    }

    @Override
    public Annotation getExternalAnnotation(CoreSession session, String entityId) throws IllegalArgumentException {
        DocumentModel annotationModel = getAnnotationModel(session, entityId);
        if (annotationModel == null) {
            throw new IllegalArgumentException("The external annotation " + entityId + " does not exist.");
        }
        Annotation annotation = new AnnotationImpl();
        Comments.documentModelToExternalAnnotation().accept(annotationModel, annotation);
        return annotation;
    }

    @Override
    public void updateExternalAnnotation(CoreSession session, String entityId, Annotation annotation) {
        DocumentModel annotationModel = getAnnotationModel(session, entityId);
        if (annotationModel == null) {
            throw new IllegalArgumentException("The external annotation " + entityId + " does not exist.");
        }
        Comments.externalAnnotationToDocumentModel().accept(annotation, annotationModel);
        session.saveDocument(annotationModel);
    }

    @Override
    public void deleteExternalAnnotation(CoreSession session, String entityId) throws IllegalArgumentException {
        DocumentModel annotationModel = getAnnotationModel(session, entityId);
        if (annotationModel == null) {
            throw new IllegalArgumentException("The external annotation " + entityId + " does not exist.");
        }
        Framework.getService(CommentManager.class).deleteComment(session, annotationModel.getId());
    }

    @SuppressWarnings("unchecked")
    protected DocumentModel getAnnotationModel(CoreSession session, String annotationId) {
        PageProviderService ppService = Framework.getService(PageProviderService.class);
        Map<String, Serializable> props = Collections.singletonMap(CORE_SESSION_PROPERTY, (Serializable) session);
        List<DocumentModel> results = ((PageProvider<DocumentModel>) ppService.getPageProvider(
                GET_ANNOTATION_PAGEPROVIDER_NAME, null, null, null, props, annotationId)).getCurrentPage();
        if (results.isEmpty()) {
            return null;
        }
        return results.get(0);
    }

}
