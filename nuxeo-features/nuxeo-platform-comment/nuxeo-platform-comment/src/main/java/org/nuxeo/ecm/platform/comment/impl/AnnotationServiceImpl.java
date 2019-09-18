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

package org.nuxeo.ecm.platform.comment.impl;

import static java.util.Collections.singletonMap;
import static org.nuxeo.ecm.platform.comment.api.AnnotationConstants.ANNOTATION_DOC_TYPE;
import static org.nuxeo.ecm.platform.comment.api.AnnotationConstants.ANNOTATION_XPATH_PROPERTY;
import static org.nuxeo.ecm.platform.comment.api.CommentManager.Feature.COMMENTS_LINKED_WITH_PROPERTY;
import static org.nuxeo.ecm.platform.comment.api.ExternalEntityConstants.EXTERNAL_ENTITY_FACET;
import static org.nuxeo.ecm.platform.comment.workflow.utils.CommentsConstants.COMMENT_AUTHOR;
import static org.nuxeo.ecm.platform.comment.workflow.utils.CommentsConstants.COMMENT_PARENT_ID;
import static org.nuxeo.ecm.platform.query.nxql.CoreQueryAndFetchPageProvider.CORE_SESSION_PROPERTY;

import java.io.Serializable;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.nuxeo.ecm.core.api.CoreInstance;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentNotFoundException;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.ecm.core.api.security.SecurityConstants;
import org.nuxeo.ecm.platform.comment.api.Annotation;
import org.nuxeo.ecm.platform.comment.api.AnnotationService;
import org.nuxeo.ecm.platform.comment.api.CommentManager;
import org.nuxeo.ecm.platform.comment.api.Comments;
import org.nuxeo.ecm.platform.comment.api.ExternalEntity;
import org.nuxeo.ecm.platform.comment.api.exceptions.CommentNotFoundException;
import org.nuxeo.ecm.platform.comment.api.exceptions.CommentSecurityException;
import org.nuxeo.ecm.platform.query.api.PageProvider;
import org.nuxeo.ecm.platform.query.api.PageProviderService;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.model.DefaultComponent;

/**
 * @since 10.1
 */
public class AnnotationServiceImpl extends DefaultComponent implements AnnotationService {

    private static final Logger log = LogManager.getLogger(AnnotationServiceImpl.class);

    protected static final String GET_ANNOTATION_PAGEPROVIDER_NAME = "GET_ANNOTATION_AS_EXTERNAL_ENTITY";

    protected static final String GET_ANNOTATIONS_FOR_DOC_PAGEPROVIDER_NAME = "GET_ANNOTATIONS_FOR_DOCUMENT";

    @Override
    public Annotation createAnnotation(CoreSession session, Annotation annotation) throws CommentSecurityException {
        String parentId = annotation.getParentId();
        if (!session.hasPermission(new IdRef(parentId), SecurityConstants.READ)) {
            throw new CommentSecurityException("The user " + session.getPrincipal().getName()
                    + " can not create annotations on document " + parentId);
        }
        return CoreInstance.doPrivileged(session, s -> {
            // Create base comment in the annotation
            DocumentModel docToAnnotate = s.getDocument(new IdRef(annotation.getParentId()));
            DocumentModel annotationModel = s.createDocumentModel(ANNOTATION_DOC_TYPE);
            Comments.annotationToDocumentModel(annotation, annotationModel);
            if (annotation instanceof ExternalEntity) {
                annotationModel.addFacet(EXTERNAL_ENTITY_FACET);
                Comments.externalEntityToDocumentModel((ExternalEntity) annotation, annotationModel);
            }
            annotationModel = Framework.getService(CommentManager.class).createComment(docToAnnotate, annotationModel);
            return Comments.newAnnotation(annotationModel);
        });
    }

    @Override
    public Annotation getAnnotation(CoreSession session, String annotationId)
            throws CommentNotFoundException, CommentSecurityException {
        DocumentRef annotationRef = new IdRef(annotationId);
        if (!session.exists(annotationRef)) {
            throw new CommentNotFoundException("The document " + annotationId + " does not exist.");
        }
        NuxeoPrincipal principal = session.getPrincipal();
        return CoreInstance.doPrivileged(session, s -> {
            DocumentModel annotationModel = s.getDocument(annotationRef);
            String parentId = (String) annotationModel.getPropertyValue(COMMENT_PARENT_ID);
            if (!s.hasPermission(principal, new IdRef(parentId), SecurityConstants.READ)) {
                throw new CommentSecurityException("The user " + principal.getName()
                        + " does not have access to the annotations of document " + parentId);
            }
            return Comments.newAnnotation(annotationModel);
        });
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<Annotation> getAnnotations(CoreSession session, String documentId, String xpath)
            throws CommentNotFoundException, CommentSecurityException {
        DocumentRef docRef = new IdRef(documentId);
        if (!session.exists(docRef)) {
            throw new CommentNotFoundException("The document " + documentId + " does not exist.");
        }
        if (!session.hasPermission(docRef, SecurityConstants.READ)) {
            throw new CommentSecurityException("The user " + session.getPrincipal().getName()
                    + " does not have access to the annotations of document " + documentId);
        }
        DocumentModel annotatedDoc = session.getDocument(docRef);
        CommentManager commentManager = Framework.getService(CommentManager.class);
        if (commentManager.hasFeature(COMMENTS_LINKED_WITH_PROPERTY)) {
            PageProviderService ppService = Framework.getService(PageProviderService.class);
            return CoreInstance.doPrivileged(session, s -> {
                Map<String, Serializable> props = Collections.singletonMap(CORE_SESSION_PROPERTY, (Serializable) s);
                PageProvider<DocumentModel> pageProvider = (PageProvider<DocumentModel>) ppService.getPageProvider(
                        GET_ANNOTATIONS_FOR_DOC_PAGEPROVIDER_NAME, null, null, null, props, documentId, xpath);
                return pageProvider.getCurrentPage().stream().map(Comments::newAnnotation).collect(Collectors.toList());
            });
        }
        return CoreInstance.doPrivileged(session, s -> {
            return commentManager.getComments(s, annotatedDoc)
                                 .stream()
                                 .filter(annotationModel -> ANNOTATION_DOC_TYPE.equals(annotationModel.getType())
                                         && xpath.equals(annotationModel.getPropertyValue(ANNOTATION_XPATH_PROPERTY)))
                                 .map(Comments::newAnnotation)
                                 .collect(Collectors.toList());
        });
    }

    @Override
    public void updateAnnotation(CoreSession session, String annotationId, Annotation annotation)
            throws CommentNotFoundException, CommentSecurityException {
        IdRef annotationRef = new IdRef(annotationId);
        if (!session.exists(annotationRef)) {
            throw new CommentNotFoundException("The annotation " + annotationId + " does not exist.");
        }
        NuxeoPrincipal principal = session.getPrincipal();
        if (!principal.isAdministrator() && !annotation.getAuthor().equals(principal.getName())) {
            throw new CommentSecurityException("The user " + principal.getName()
                    + " can not edit annotations of document " + annotation.getParentId());
        }
        CoreInstance.doPrivileged(session, s -> {
            DocumentModel annotationModel = s.getDocument(annotationRef);
            Comments.annotationToDocumentModel(annotation, annotationModel);
            if (annotation instanceof ExternalEntity) {
                Comments.externalEntityToDocumentModel((ExternalEntity) annotation, annotationModel);
            }
            s.saveDocument(annotationModel);
        });
    }

    @Override
    public void deleteAnnotation(CoreSession session, String annotationId) throws CommentNotFoundException {
        NuxeoPrincipal principal = session.getPrincipal();
        CoreInstance.doPrivileged(session, s -> {
            try {
                DocumentModel annotation = s.getDocument(new IdRef(annotationId));
                String parentId = (String) annotation.getPropertyValue(COMMENT_PARENT_ID);
                DocumentRef parentRef = new IdRef(parentId);
                if (!principal.isAdministrator()
                        && !annotation.getPropertyValue(COMMENT_AUTHOR).equals(principal.getName())
                        && !s.hasPermission(principal, parentRef, SecurityConstants.EVERYTHING)) {
                    throw new CommentSecurityException(
                            "The user " + principal.getName() + " can not delete annotations of document " + parentId);
                }
            } catch (DocumentNotFoundException e) {
                throw new CommentNotFoundException(e);
            }
            Framework.getService(CommentManager.class).deleteComment(s, annotationId);
        });
    }

    @Override
    public Annotation getExternalAnnotation(CoreSession session, String entityId)
            throws CommentNotFoundException, CommentSecurityException {
        DocumentModel annotationModel = getAnnotationModel(session, entityId);
        if (annotationModel == null) {
            throw new CommentNotFoundException("The external annotation " + entityId + " does not exist.");
        }
        String parentId = (String) annotationModel.getPropertyValue(COMMENT_PARENT_ID);
        if (!session.hasPermission(new IdRef(parentId), SecurityConstants.READ)) {
            throw new CommentSecurityException("The user " + session.getPrincipal().getName()
                    + " does not have access to the annotations of document " + parentId);
        }
        return Comments.newAnnotation(annotationModel);
    }

    @Override
    public void updateExternalAnnotation(CoreSession session, String entityId, Annotation annotation)
            throws CommentNotFoundException, CommentSecurityException {
        NuxeoPrincipal principal = session.getPrincipal();
        if (!principal.isAdministrator() && !annotation.getAuthor().equals(principal.getName())) {
            throw new CommentSecurityException("The user " + session.getPrincipal().getName()
                    + " can not edit annotations of document " + annotation.getParentId());
        }
        DocumentModel annotationModel = getAnnotationModel(session, entityId);
        if (annotationModel == null) {
            throw new CommentNotFoundException("The external annotation " + entityId + " does not exist.");
        }
        Comments.annotationToDocumentModel(annotation, annotationModel);
        if (annotation instanceof ExternalEntity) {
            Comments.externalEntityToDocumentModel((ExternalEntity) annotation, annotationModel);
        }
        session.saveDocument(annotationModel);
    }

    @Override
    public void deleteExternalAnnotation(CoreSession session, String entityId)
            throws CommentNotFoundException, CommentSecurityException {
        DocumentModel annotationModel = getAnnotationModel(session, entityId);
        if (annotationModel == null) {
            throw new CommentNotFoundException("The external annotation " + entityId + " does not exist.");
        }
        NuxeoPrincipal principal = session.getPrincipal();
        String parentId = (String) annotationModel.getPropertyValue(COMMENT_PARENT_ID);
        if (!principal.isAdministrator()
                && !annotationModel.getPropertyValue(COMMENT_AUTHOR).equals(principal.getName())
                && !session.hasPermission(new IdRef(parentId), SecurityConstants.EVERYTHING)) {
            throw new CommentSecurityException(
                    "The user " + principal.getName() + " can not delete annotations of document " + parentId);
        }
        Framework.getService(CommentManager.class).deleteComment(session, annotationModel.getId());
    }

    @SuppressWarnings("unchecked")
    protected DocumentModel getAnnotationModel(CoreSession session, String entityId) {
        PageProviderService ppService = Framework.getService(PageProviderService.class);
        Map<String, Serializable> props = singletonMap(CORE_SESSION_PROPERTY, (Serializable) session);
        List<DocumentModel> results = ((PageProvider<DocumentModel>) ppService.getPageProvider(
                GET_ANNOTATION_PAGEPROVIDER_NAME, null, 1L, 0L, props, entityId)).getCurrentPage();
        if (results.isEmpty()) {
            return null;
        }
        return results.get(0);
    }

}
