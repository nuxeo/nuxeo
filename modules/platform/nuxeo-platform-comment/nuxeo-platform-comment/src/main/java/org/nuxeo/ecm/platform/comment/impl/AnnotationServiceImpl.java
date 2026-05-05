/*
 * (C) Copyright 2018-2026 Nuxeo (http://nuxeo.com/) and others.
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

import static org.nuxeo.ecm.platform.comment.api.CommentConstants.COMMENT_SCHEMA;
import static org.nuxeo.ecm.platform.comment.api.CommentManager.Feature.COMMENTS_ARE_SPECIAL_CHILDREN;
import static org.nuxeo.ecm.platform.comment.impl.AbstractCommentManager.COMMENTS_DIRECTORY;
import static org.nuxeo.ecm.platform.query.nxql.CoreQueryAndFetchPageProvider.CORE_SESSION_PROPERTY;

import java.io.Serializable;
import java.util.List;
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
import org.nuxeo.ecm.platform.query.api.PageProviderSpec;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.model.DefaultComponent;

/**
 * @since 10.1
 */
public class AnnotationServiceImpl extends DefaultComponent implements AnnotationService {

    /** @deprecated since 11.1, because unused. */
    @Deprecated(since = "11.1")
    protected static final String GET_ANNOTATION_PAGEPROVIDER_NAME = "GET_ANNOTATION_AS_EXTERNAL_ENTITY";

    /** @deprecated since 11.1, because unused. */
    @Deprecated(since = "11.1")
    @SuppressWarnings("DeprecatedIsStillUsed")
    protected static final String GET_ANNOTATIONS_FOR_DOC_PAGEPROVIDER_NAME = "GET_ANNOTATIONS_FOR_DOCUMENT";

    /** @since 11.1 */
    protected static final String GET_ANNOTATIONS_FOR_DOCUMENT_PAGE_PROVIDER_NAME = "GET_ANNOTATIONS_FOR_DOCUMENT_BY_ECM_PARENT";

    @Override
    public Annotation createAnnotation(CoreSession session, Annotation annotation) throws CommentSecurityException {
        return (Annotation) Framework.getService(CommentManager.class).createComment(session, annotation);
    }

    @Override
    public Annotation getAnnotation(CoreSession s, String annotationId)
            throws CommentNotFoundException, CommentSecurityException {
        return (Annotation) Framework.getService(CommentManager.class).getComment(s, annotationId);
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
            throw new CommentNotFoundException("The document %s does not exist.".formatted(docRef), dnfe);
        }
        return streamAnnotations(session, documentId, xpath).map(doc -> doc.getAdapter(Annotation.class)).toList();
    }

    protected Stream<DocumentModel> streamAnnotations(CoreSession session, String documentId, String xpath) {
        DocumentModel annotatedDoc = session.getDocument(new IdRef(documentId));
        CommentManager commentManager = Framework.getService(CommentManager.class);
        return CoreInstance.doPrivileged(session, s -> {
            if (commentManager.hasFeature(COMMENTS_ARE_SPECIAL_CHILDREN)) {
                // handle first comment/reply cases
                String parentId = documentId;
                if (!annotatedDoc.hasSchema(COMMENT_SCHEMA) && s.hasChild(annotatedDoc.getRef(), COMMENTS_DIRECTORY)) {
                    DocumentModel commentsFolder = s.getChild(annotatedDoc.getRef(), COMMENTS_DIRECTORY);
                    parentId = commentsFolder.getId();
                }
                // when comments are special children we can leverage inherited acls
                return getPageProviderPage(GET_ANNOTATIONS_FOR_DOCUMENT_PAGE_PROVIDER_NAME, session, parentId, xpath);
            } else {
                List<DocumentModel> docs = getPageProviderPage(GET_ANNOTATIONS_FOR_DOC_PAGEPROVIDER_NAME, s, documentId,
                        xpath);
                docs.forEach(doc -> doc.detach(true)); // due to privileged session
                return docs;
            }
        }).stream();
    }

    @SuppressWarnings("unchecked")
    protected List<DocumentModel> getPageProviderPage(String ppName, CoreSession session, Object... parameters) {
        var ppService = Framework.getService(PageProviderService.class);
        var pageProvider = (PageProvider<DocumentModel>) ppService.getPageProvider(
                PageProviderSpec.builder(ppName)
                                .property(CORE_SESSION_PROPERTY, (Serializable) session)
                                .parameters(parameters)
                                .build());
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
    public Annotation getExternalAnnotation(CoreSession session, String documentId, String entityId)
            throws CommentNotFoundException, CommentSecurityException {
        return (Annotation) Framework.getService(CommentManager.class)
                                     .getExternalComment(session, documentId, entityId);
    }

    @Override
    public Annotation updateExternalAnnotation(CoreSession session, String documentId, String entityId,
            Annotation annotation) throws CommentNotFoundException, CommentSecurityException {
        return (Annotation) Framework.getService(CommentManager.class)
                                     .updateExternalComment(session, documentId, entityId, annotation);
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
    @Deprecated(since = "11.1", forRemoval = true)
    protected DocumentModel getAnnotationModel(CoreSession session, String entityId) {
        PageProviderService ppService = Framework.getService(PageProviderService.class);
        List<DocumentModel> results = ((PageProvider<DocumentModel>) ppService.getPageProvider(
                PageProviderSpec.builder(GET_ANNOTATION_PAGEPROVIDER_NAME)
                                .pageSize(1L)
                                .currentPage(0L)
                                .property(CORE_SESSION_PROPERTY, (Serializable) session)
                                .parameters(entityId)
                                .build())).getCurrentPage();
        if (results.isEmpty()) {
            return null;
        }
        return results.get(0);
    }

}
