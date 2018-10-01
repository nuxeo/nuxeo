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

import static java.util.Collections.singletonList;
import static java.util.Collections.singletonMap;
import static java.util.stream.Collectors.collectingAndThen;
import static java.util.stream.Collectors.toList;
import static org.nuxeo.ecm.platform.comment.api.ExternalEntityConstants.EXTERNAL_ENTITY_FACET;
import static org.nuxeo.ecm.platform.comment.workflow.utils.CommentsConstants.COMMENT_ANCESTOR_IDS;
import static org.nuxeo.ecm.platform.comment.workflow.utils.CommentsConstants.COMMENT_CREATION_DATE;
import static org.nuxeo.ecm.platform.comment.workflow.utils.CommentsConstants.COMMENT_DOC_TYPE;
import static org.nuxeo.ecm.platform.comment.workflow.utils.CommentsConstants.COMMENT_PARENT_ID;
import static org.nuxeo.ecm.platform.query.nxql.CoreQueryAndFetchPageProvider.CORE_SESSION_PROPERTY;

import java.io.Serializable;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.CoreInstance;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.core.api.PartialList;
import org.nuxeo.ecm.core.api.PathRef;
import org.nuxeo.ecm.core.api.SortInfo;
import org.nuxeo.ecm.platform.comment.api.Comment;
import org.nuxeo.ecm.platform.comment.api.CommentEvents;
import org.nuxeo.ecm.platform.comment.api.Comments;
import org.nuxeo.ecm.platform.comment.api.ExternalEntity;
import org.nuxeo.ecm.platform.query.api.PageProvider;
import org.nuxeo.ecm.platform.query.api.PageProviderService;
import org.nuxeo.runtime.api.Framework;

/**
 * Comment service implementation. The comments are linked together thanks to a parent document id property.
 * 
 * @since 10.3
 */
public class PropertyCommentManager extends AbstractCommentManager {

    private static final Log log = LogFactory.getLog(PropertyCommentManager.class);

    protected static final String GET_COMMENT_PAGEPROVIDER_NAME = "GET_COMMENT_AS_EXTERNAL_ENTITY";

    protected static final String GET_COMMENTS_FOR_DOC_PAGEPROVIDER_NAME = "GET_COMMENTS_FOR_DOCUMENT";

    protected static final String HIDDEN_FOLDER_TYPE = "HiddenFolder";

    protected static final String COMMENT_NAME = "comment";

    @Override
    @SuppressWarnings("unchecked")
    public List<DocumentModel> getComments(CoreSession session, DocumentModel docModel) {
        PageProviderService ppService = Framework.getService(PageProviderService.class);
        Map<String, Serializable> props = Collections.singletonMap(CORE_SESSION_PROPERTY, (Serializable) session);
        PageProvider<DocumentModel> pageProvider = (PageProvider<DocumentModel>) ppService.getPageProvider(
                GET_COMMENTS_FOR_DOC_PAGEPROVIDER_NAME, singletonList(new SortInfo(COMMENT_CREATION_DATE, true)), null,
                null, props, docModel.getId());
        return pageProvider.getCurrentPage();
    }

    @Override
    public List<DocumentModel> getComments(DocumentModel docModel, DocumentModel parent) {
        throw new UnsupportedOperationException("This service implementation does not implement deprecated API.");
    }

    @Override
    public DocumentModel createComment(DocumentModel docModel, String comment) {
        throw new UnsupportedOperationException("This service implementation does not implement deprecated API.");
    }

    @Override
    public DocumentModel createComment(DocumentModel docModel, String text, String author) {
        throw new UnsupportedOperationException("This service implementation does not implement deprecated API.");
    }

    @Override
    public DocumentModel createComment(DocumentModel docModel, DocumentModel commentModel) {
        CoreSession session = docModel.getCoreSession();
        String path = getCommentContainerPath(session, docModel.getId());

        DocumentModel commentModelToCreate = session.createDocumentModel(path, COMMENT_NAME, commentModel.getType());
        commentModelToCreate.copyContent(commentModel);
        commentModelToCreate.setPropertyValue(COMMENT_ANCESTOR_IDS,
                (Serializable) computeAncestorIds(session, docModel.getId()));
        DocumentModel createdCommentModel = session.createDocument(commentModelToCreate);
        CoreInstance.doPrivileged(session, s -> {
            setCommentPermissions(s, createdCommentModel);
        });
        notifyEvent(session, CommentEvents.COMMENT_ADDED, docModel, createdCommentModel);
        return createdCommentModel;
    }

    @Override
    public DocumentModel createComment(DocumentModel docModel, DocumentModel parent, DocumentModel child) {
        throw new UnsupportedOperationException("This service implementation does not implement deprecated API.");
    }

    @Override
    public void deleteComment(DocumentModel docModel, DocumentModel comment) {
        throw new UnsupportedOperationException("This service implementation does not implement deprecated API.");
    }

    @Override
    public List<DocumentModel> getDocumentsForComment(DocumentModel comment) {
        throw new UnsupportedOperationException("This service implementation does not implement deprecated API.");
    }

    @Override
    public DocumentModel getThreadForComment(DocumentModel comment) {
        CoreSession session = comment.getCoreSession();
        DocumentModel parent = session.getDocument(new IdRef((String) comment.getPropertyValue(COMMENT_PARENT_ID)));
        if (COMMENT_DOC_TYPE.equals(parent.getType())) {
            return getThreadForComment(parent);
        }
        return comment;
    }

    @Override
    public DocumentModel createLocatedComment(DocumentModel docModel, DocumentModel comment, String path) {
        CoreSession session = docModel.getCoreSession();
        DocumentModel commentModel = session.createDocumentModel(path, COMMENT_NAME, comment.getType());
        commentModel.copyContent(comment);
        commentModel.setPropertyValue(COMMENT_ANCESTOR_IDS,
                (Serializable) computeAncestorIds(session, docModel.getId()));
        DocumentModel createdCommentModel = session.createDocument(commentModel);
        CoreInstance.doPrivileged(session, s -> {
            setCommentPermissions(s, createdCommentModel);
        });
        notifyEvent(session, CommentEvents.COMMENT_ADDED, docModel, createdCommentModel);
        return createdCommentModel;
    }

    @Override
    public Comment createComment(CoreSession session, Comment comment) throws IllegalArgumentException {
        String parentId = comment.getParentId();
        DocumentRef docRef = new IdRef(parentId);
        if (!session.exists(docRef)) {
            throw new IllegalArgumentException("The document " + comment.getParentId() + " does not exist.");
        }
        String path = getCommentContainerPath(session, parentId);
        DocumentModel commentModel = session.createDocumentModel(path, COMMENT_NAME, COMMENT_DOC_TYPE);
        Comments.commentToDocumentModel(comment, commentModel);
        if (comment instanceof ExternalEntity) {
            commentModel.addFacet(EXTERNAL_ENTITY_FACET);
            Comments.externalEntityToDocumentModel((ExternalEntity) comment, commentModel);
        }

        // Compute the list of ancestor ids
        commentModel.setPropertyValue(COMMENT_ANCESTOR_IDS, (Serializable) computeAncestorIds(session, parentId));

        DocumentModel createdCommentModel = session.createDocument(commentModel);
        CoreInstance.doPrivileged(session, s -> {
            setCommentPermissions(s, createdCommentModel);
        });

        notifyEvent(session, CommentEvents.COMMENT_ADDED, session.getDocument(docRef), createdCommentModel);
        return Comments.newComment(createdCommentModel);
    }

    @Override
    public Comment getComment(CoreSession session, String commentId) throws IllegalArgumentException {
        DocumentRef commentRef = new IdRef(commentId);
        if (!session.exists(commentRef)) {
            throw new IllegalArgumentException("The comment " + commentId + " does not exist.");
        }
        DocumentModel commentModel = session.getDocument(commentRef);
        return Comments.newComment(commentModel);
    }

    @Override
    @SuppressWarnings("unchecked")
    public PartialList<Comment> getComments(CoreSession session, String documentId, Long pageSize,
            Long currentPageIndex, boolean sortAscending) {
        PageProviderService ppService = Framework.getService(PageProviderService.class);
        Map<String, Serializable> props = Collections.singletonMap(CORE_SESSION_PROPERTY, (Serializable) session);
        PageProvider<DocumentModel> pageProvider = (PageProvider<DocumentModel>) ppService.getPageProvider(
                GET_COMMENTS_FOR_DOC_PAGEPROVIDER_NAME,
                singletonList(new SortInfo(COMMENT_CREATION_DATE, sortAscending)), pageSize, currentPageIndex, props,
                documentId);
        List<DocumentModel> commentList = pageProvider.getCurrentPage();
        return commentList.stream()
                          .map(Comments::newComment)
                          .collect(collectingAndThen(toList(),
                                  list -> new PartialList<>(list, pageProvider.getResultsCount())));
    }

    @Override
    public void updateComment(CoreSession session, String commentId, Comment comment) throws IllegalArgumentException {
        IdRef commentRef = new IdRef(commentId);
        if (!session.exists(commentRef)) {
            throw new IllegalArgumentException("The comment " + commentId + " does not exist.");
        }
        DocumentModel commentModel = session.getDocument(commentRef);
        Comments.commentToDocumentModel(comment, commentModel);
        if (comment instanceof ExternalEntity) {
            Comments.externalEntityToDocumentModel((ExternalEntity) comment, commentModel);
        }
        session.saveDocument(commentModel);
    }

    @Override
    public void deleteComment(CoreSession session, String commentId) throws IllegalArgumentException {
        IdRef commentRef = new IdRef(commentId);
        if (!session.exists(commentRef)) {
            throw new IllegalArgumentException("The comment " + commentId + " does not exist.");
        }
        DocumentModel comment = session.getDocument(commentRef);
        DocumentModel parent = session.getDocument(new IdRef((String) comment.getPropertyValue(COMMENT_PARENT_ID)));
        session.removeDocument(commentRef);
        notifyEvent(session, CommentEvents.COMMENT_REMOVED, parent, comment);
    }

    @Override
    public Comment getExternalComment(CoreSession session, String entityId) throws IllegalArgumentException {
        DocumentModel commentModel = getExternalCommentModel(session, entityId);
        if (commentModel == null) {
            throw new IllegalArgumentException("The external comment " + entityId + " does not exist.");
        }
        return Comments.newComment(commentModel);
    }

    @Override
    public void updateExternalComment(CoreSession session, String entityId, Comment comment)
            throws IllegalArgumentException {
        DocumentModel commentModel = getExternalCommentModel(session, entityId);
        if (commentModel == null) {
            throw new IllegalArgumentException("The external comment " + entityId + " does not exist.");
        }
        Comments.commentToDocumentModel(comment, commentModel);
        if (comment instanceof ExternalEntity) {
            Comments.externalEntityToDocumentModel((ExternalEntity) comment, commentModel);
        }
        session.saveDocument(commentModel);
    }

    @Override
    public void deleteExternalComment(CoreSession session, String entityId) throws IllegalArgumentException {
        DocumentModel commentModel = getExternalCommentModel(session, entityId);
        if (commentModel == null) {
            throw new IllegalArgumentException("The external comment " + entityId + " does not exist.");
        }
        DocumentModel comment = session.getDocument(commentModel.getRef());
        DocumentModel parent = session.getDocument(new IdRef((String) comment.getPropertyValue(COMMENT_PARENT_ID)));
        session.removeDocument(commentModel.getRef());
        notifyEvent(session, CommentEvents.COMMENT_REMOVED, parent, comment);
    }

    @Override
    public boolean hasFeature(Feature feature) {
        switch (feature) {
        case COMMENTS_LINKED_WITH_PROPERTY:
            return true;
        default:
            throw new UnsupportedOperationException(feature.name());
        }
    }

    @SuppressWarnings("unchecked")
    protected DocumentModel getExternalCommentModel(CoreSession session, String entityId) {
        PageProviderService ppService = Framework.getService(PageProviderService.class);
        Map<String, Serializable> props = singletonMap(CORE_SESSION_PROPERTY, (Serializable) session);
        List<DocumentModel> results = ((PageProvider<DocumentModel>) ppService.getPageProvider(
                GET_COMMENT_PAGEPROVIDER_NAME, null, 1L, 0L, props, entityId)).getCurrentPage();
        if (results.isEmpty()) {
            return null;
        }
        return results.get(0);
    }

    protected String getCommentContainerPath(CoreSession session, String commentedDocumentId) {
        return CoreInstance.doPrivileged(session, s -> {
            // Create or retrieve the folder to store the comment.
            // If the document is under a domain, the folder is a child of this domain.
            // Otherwise, it is a child of the root document.
            DocumentModel annotatedDoc = s.getDocument(new IdRef(commentedDocumentId));
            String parentPath = "/";
            if (annotatedDoc.getPath().segmentCount() > 1) {
                parentPath += annotatedDoc.getPath().segment(0);
            }
            PathRef ref = new PathRef(parentPath, COMMENTS_DIRECTORY);
            DocumentModel commentFolderDoc = s.createDocumentModel(parentPath, COMMENTS_DIRECTORY, HIDDEN_FOLDER_TYPE);
            commentFolderDoc = s.getOrCreateDocument(commentFolderDoc);
            setFolderPermissions(s, commentFolderDoc);
            s.save();
            return ref.toString();
        });
    }

}
