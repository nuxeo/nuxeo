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
import static org.nuxeo.ecm.platform.comment.api.CommentManager.Feature.COMMENTS_ARE_SPECIAL_CHILDREN;
import static org.nuxeo.ecm.platform.comment.api.CommentManager.Feature.COMMENTS_LINKED_WITH_PROPERTY;
import static org.nuxeo.ecm.platform.comment.impl.AbstractCommentManager.COMMENTS_DIRECTORY;
import static org.nuxeo.ecm.platform.comment.workflow.utils.CommentsConstants.COMMENT_SCHEMA;
import static org.nuxeo.ecm.platform.query.nxql.CoreQueryAndFetchPageProvider.CORE_SESSION_PROPERTY;

import java.io.Serializable;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.nuxeo.ecm.core.api.CoreInstance;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentNotFoundException;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.core.api.security.SecurityConstants;
import org.nuxeo.ecm.platform.comment.api.Annotation;
import org.nuxeo.ecm.platform.comment.api.AnnotationService;
import org.nuxeo.ecm.platform.comment.api.CommentManager;
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

    /** @deprecated since 11.1, because unused. */
    @Deprecated
    protected static final String GET_ANNOTATION_PAGEPROVIDER_NAME = "GET_ANNOTATION_AS_EXTERNAL_ENTITY";

    /** @deprecated since 11.1, because unused. */
    @Deprecated
    @SuppressWarnings("DeprecatedIsStillUsed")
    protected static final String GET_ANNOTATIONS_FOR_DOC_PAGEPROVIDER_NAME = "GET_ANNOTATIONS_FOR_DOCUMENT";

    /** @since 11.1 */
    protected static final String GET_ANNOTATIONS_FOR_DOCUMENT_PAGE_PROVIDER_NAME = "GET_ANNOTATIONS_FOR_DOCUMENT_BY_ECM_PARENT";

    @Override
    public Annotation createAnnotation(CoreSession session, Annotation annotation) throws CommentSecurityException {
        return (Annotation) Framework.getService(CommentManager.class).createComment(session, annotation);
    }

    @Override
    public Annotation getAnnotation(CoreSession session, String annotationId)
            throws CommentNotFoundException, CommentSecurityException {
        return (Annotation) Framework.getService(CommentManager.class).getComment(session, annotationId);
    }

    @Override
    public List<Annotation> getAnnotations(CoreSession session, String documentId, String xpath)
            throws CommentNotFoundException, CommentSecurityException {
        DocumentRef docRef = new IdRef(documentId);
        try {
            if (!session.hasPermission(docRef, SecurityConstants.READ)) {
                throw new CommentSecurityException("The user " + session.getPrincipal().getName()
                        + " does not have access to the annotations of document " + documentId);
            }
        } catch (DocumentNotFoundException dnfe) {
            throw new CommentNotFoundException(String.format("The document %s does not exist.", docRef), dnfe);
        }
        return streamAnnotations(session, documentId, xpath).map(doc -> doc.getAdapter(Annotation.class))
                                                            .collect(Collectors.toList());
    }

    protected Stream<DocumentModel> streamAnnotations(CoreSession session, String documentId, String xpath) {
        DocumentModel annotatedDoc = session.getDocument(new IdRef(documentId));
        CommentManager commentManager = Framework.getService(CommentManager.class);
        return CoreInstance.doPrivileged(session, s -> {
            if (commentManager.hasFeature(COMMENTS_LINKED_WITH_PROPERTY)) {
                Map<String, Serializable> props = Collections.singletonMap(CORE_SESSION_PROPERTY, (Serializable) s);
                List<DocumentModel> docs = getPageProviderPage(GET_ANNOTATIONS_FOR_DOC_PAGEPROVIDER_NAME, props,
                        documentId, xpath);
                docs.forEach(doc -> doc.detach(true)); // due to privileged session
                return docs;
            } else if (commentManager.hasFeature(COMMENTS_ARE_SPECIAL_CHILDREN)) {
                // handle first comment/reply cases
                String parentId = documentId;
                if (!annotatedDoc.hasSchema(COMMENT_SCHEMA) && s.hasChild(annotatedDoc.getRef(), COMMENTS_DIRECTORY)) {
                    DocumentModel commentsFolder = s.getChild(annotatedDoc.getRef(), COMMENTS_DIRECTORY);
                    parentId = commentsFolder.getId();
                }
                // when comments are special children we can leverage inherited acls
                Map<String, Serializable> props = Collections.singletonMap(CORE_SESSION_PROPERTY,
                        (Serializable) session);
                return getPageProviderPage(GET_ANNOTATIONS_FOR_DOCUMENT_PAGE_PROVIDER_NAME, props, parentId, xpath);
            }
            // provide a poor support for deprecated implementations
            return commentManager.getComments(s, annotatedDoc)
                                 .stream()
                                 .filter(annotationModel -> ANNOTATION_DOC_TYPE.equals(annotationModel.getType())
                                         && xpath.equals(annotationModel.getPropertyValue(ANNOTATION_XPATH_PROPERTY)))
                                 .collect(Collectors.toList());
        }).stream();
    }

    @SuppressWarnings("unchecked")
    protected List<DocumentModel> getPageProviderPage(String ppName, Map<String, Serializable> props,
            Object... parameters) {
        PageProviderService ppService = Framework.getService(PageProviderService.class);
        PageProvider<DocumentModel> pageProvider = (PageProvider<DocumentModel>) ppService.getPageProvider(ppName, null,
                null, null, props, parameters);
        return pageProvider.getCurrentPage();
    }

    @Override
    public void updateAnnotation(CoreSession session, String annotationId, Annotation annotation)
            throws CommentNotFoundException, CommentSecurityException {
        Framework.getService(CommentManager.class).updateComment(session, annotationId, annotation);
    }

    @Override
    public void deleteAnnotation(CoreSession session, String annotationId) throws CommentNotFoundException {
        Framework.getService(CommentManager.class).deleteComment(session, annotationId);
    }

    @Override
    public Annotation getExternalAnnotation(CoreSession session, String entityId)
            throws CommentNotFoundException, CommentSecurityException {
        return getExternalAnnotation(session, null, entityId);
    }

    @Override
    public Annotation getExternalAnnotation(CoreSession session, String documentId, String entityId)
            throws CommentNotFoundException, CommentSecurityException {
        return (Annotation) Framework.getService(CommentManager.class)
                                     .getExternalComment(session, documentId, entityId);
    }

    @Override
    public void updateExternalAnnotation(CoreSession session, String entityId, Annotation annotation)
            throws CommentNotFoundException, CommentSecurityException {
        updateExternalAnnotation(session, null, entityId, annotation);
    }

    @Override
    public Annotation updateExternalAnnotation(CoreSession session, String documentId, String entityId,
            Annotation annotation) throws CommentNotFoundException, CommentSecurityException {
        return (Annotation) Framework.getService(CommentManager.class)
                                     .updateExternalComment(session, documentId, entityId, annotation);
    }

    @Override
    public void deleteExternalAnnotation(CoreSession session, String entityId)
            throws CommentNotFoundException, CommentSecurityException {
        deleteExternalAnnotation(session, null, entityId);
    }

    @Override
    public void deleteExternalAnnotation(CoreSession session, String documentId, String entityId)
            throws CommentNotFoundException, CommentSecurityException {
        Framework.getService(CommentManager.class).deleteExternalComment(session, documentId, entityId);
    }

    /**
     * @deprecated since 11.1. No used any more.
     */
    @SuppressWarnings("unchecked")
    @Deprecated
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
