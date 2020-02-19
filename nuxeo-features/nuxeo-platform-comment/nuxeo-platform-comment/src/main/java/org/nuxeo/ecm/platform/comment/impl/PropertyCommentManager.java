/*
 * (C) Copyright 2018-2020 Nuxeo (http://nuxeo.com/) and others.
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
import static org.nuxeo.ecm.core.io.marshallers.json.document.DocumentModelJsonReader.applyDirtyPropertyValues;
import static org.nuxeo.ecm.platform.comment.api.CommentConstants.COMMENT_ANCESTOR_IDS_PROPERTY;
import static org.nuxeo.ecm.platform.comment.api.CommentConstants.COMMENT_AUTHOR_PROPERTY;
import static org.nuxeo.ecm.platform.comment.api.CommentConstants.COMMENT_CREATION_DATE_PROPERTY;
import static org.nuxeo.ecm.platform.comment.api.CommentConstants.COMMENT_PARENT_ID_PROPERTY;
import static org.nuxeo.ecm.platform.comment.api.CommentConstants.COMMENT_SCHEMA;
import static org.nuxeo.ecm.platform.comment.api.ExternalEntityConstants.EXTERNAL_ENTITY_FACET;
import static org.nuxeo.ecm.platform.ec.notification.NotificationConstants.DISABLE_NOTIFICATION_SERVICE;
import static org.nuxeo.ecm.platform.query.nxql.CoreQueryAndFetchPageProvider.CORE_SESSION_PROPERTY;

import java.io.Serializable;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.nuxeo.ecm.core.api.CloseableCoreSession;
import org.nuxeo.ecm.core.api.CoreInstance;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.ecm.core.api.PartialList;
import org.nuxeo.ecm.core.api.PathRef;
import org.nuxeo.ecm.core.api.SortInfo;
import org.nuxeo.ecm.core.api.security.SecurityConstants;
import org.nuxeo.ecm.platform.comment.api.Comment;
import org.nuxeo.ecm.platform.comment.api.CommentEvents;
import org.nuxeo.ecm.platform.comment.api.exceptions.CommentNotFoundException;
import org.nuxeo.ecm.platform.comment.api.exceptions.CommentSecurityException;
import org.nuxeo.ecm.platform.query.api.PageProvider;
import org.nuxeo.ecm.platform.query.api.PageProviderService;
import org.nuxeo.runtime.api.Framework;

/**
 * Comment service implementation. The comments are linked together thanks to a parent document id property.
 *
 * @since 10.3
 * @deprecated since 11.1, use {@link TreeCommentManager} instead
 */
@Deprecated(since = "11.1")
public class PropertyCommentManager extends AbstractCommentManager {

    protected static final String GET_COMMENT_PAGEPROVIDER_NAME = "GET_COMMENT_AS_EXTERNAL_ENTITY";

    protected static final String GET_COMMENTS_FOR_DOC_PAGEPROVIDER_NAME = "GET_COMMENTS_FOR_DOCUMENT";

    protected static final String HIDDEN_FOLDER_TYPE = "HiddenFolder";

    protected static final String COMMENT_NAME = "comment";

    @Override
    @SuppressWarnings("unchecked")
    public List<DocumentModel> getComments(CoreSession session, DocumentModel docModel)
            throws CommentSecurityException {

        DocumentRef docRef = getAncestorRef(session, docModel);

        if (session.exists(docRef) && !session.hasPermission(docRef, SecurityConstants.READ)) {
            throw new CommentSecurityException("The user " + session.getPrincipal().getName()
                    + " does not have access to the comments of document " + docModel.getId());
        }
        PageProviderService ppService = Framework.getService(PageProviderService.class);
        return CoreInstance.doPrivileged(session, s -> {
            Map<String, Serializable> props = Collections.singletonMap(CORE_SESSION_PROPERTY, (Serializable) s);
            PageProvider<DocumentModel> pageProvider = (PageProvider<DocumentModel>) ppService.getPageProvider(
                    GET_COMMENTS_FOR_DOC_PAGEPROVIDER_NAME,
                    singletonList(new SortInfo(COMMENT_CREATION_DATE_PROPERTY, true)), null, null, props,
                    docModel.getId());
            return pageProvider.getCurrentPage();
        });
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
    public DocumentModel createComment(DocumentModel docModel, DocumentModel commentModel)
            throws CommentSecurityException {

        NuxeoPrincipal principal = commentModel.getCoreSession().getPrincipal();
        // Open a session as system user since the parent document model can be a comment
        try (CloseableCoreSession session = CoreInstance.openCoreSessionSystem(docModel.getRepositoryName())) {
            DocumentRef docRef = getTopLevelDocumentRef(session, docModel.getRef());
            if (!session.hasPermission(principal, docRef, SecurityConstants.READ)) {
                throw new CommentSecurityException(
                        "The user " + principal.getName() + " can not create comments on document " + docModel.getId());
            }

            String path = getCommentContainerPath(session, docModel.getId());

            DocumentModel commentModelToCreate = session.createDocumentModel(path, COMMENT_NAME,
                    commentModel.getType());
            commentModelToCreate.copyContent(commentModel);
            commentModelToCreate.setPropertyValue(COMMENT_PARENT_ID_PROPERTY, docModel.getId());
            commentModelToCreate.setPropertyValue(COMMENT_ANCESTOR_IDS_PROPERTY,
                    computeAncestorIds(session, docModel.getId()));
            DocumentModel comment = session.createDocument(commentModelToCreate);
            comment.detach(true);
            notifyEvent(session, CommentEvents.COMMENT_ADDED, docModel, comment);

            return comment;
        }
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
    public DocumentModel getThreadForComment(DocumentModel comment) throws CommentSecurityException {
        return getThreadForComment(comment.getCoreSession(), comment);
    }

    @Override
    public DocumentModel createLocatedComment(DocumentModel docModel, DocumentModel comment, String path) {
        CoreSession session = docModel.getCoreSession();
        DocumentRef docRef = getTopLevelDocumentRef(session, docModel.getRef());
        if (!session.hasPermission(docRef, SecurityConstants.READ)) {
            throw new CommentSecurityException("The user " + session.getPrincipal().getName()
                    + " can not create comments on document " + docModel.getId());
        }
        return CoreInstance.doPrivileged(session, s -> {
            DocumentModel commentModel = s.createDocumentModel(path, COMMENT_NAME, comment.getType());
            commentModel.copyContent(comment);
            commentModel.setPropertyValue(COMMENT_PARENT_ID_PROPERTY, docModel.getId());
            commentModel.setPropertyValue(COMMENT_ANCESTOR_IDS_PROPERTY, computeAncestorIds(s, docModel.getId()));
            commentModel = s.createDocument(commentModel);
            notifyEvent(session, CommentEvents.COMMENT_ADDED, docModel, commentModel);
            return commentModel;
        });
    }

    @Override
    public Comment createComment(CoreSession session, Comment comment)
            throws CommentNotFoundException, CommentSecurityException {
        String parentId = comment.getParentId();
        DocumentRef parentRef = new IdRef(parentId);
        // Parent document can be a comment, check existence as a privileged user
        if (!CoreInstance.doPrivileged(session, s -> {
            return s.exists(parentRef);
        })) {
            throw new CommentNotFoundException("The document or comment " + comment.getParentId() + " does not exist.");
        }
        DocumentRef ancestorRef = CoreInstance.doPrivileged(session, s -> {
            DocumentModel parentDocModel = s.getDocument(parentRef);
            if (parentDocModel.hasSchema(COMMENT_SCHEMA)) {
                return getAncestorRef(s, parentDocModel);
            }
            return parentDocModel.getRef();
        });
        if (!session.hasPermission(ancestorRef, SecurityConstants.READ)) {
            throw new CommentSecurityException("The user " + session.getPrincipal().getName()
                    + " can not create comments on document " + parentId);
        }

        // Initiate Creation Date if it is not done yet
        if (comment.getCreationDate() == null) {
            comment.setCreationDate(Instant.now());
        }

        return CoreInstance.doPrivileged(session, s -> {
            String path = getCommentContainerPath(s, parentId);
            DocumentModel commentModel = session.createDocumentModel(path, COMMENT_NAME,
                    comment.getDocument().getType());
            if (comment.getDocument().hasFacet(EXTERNAL_ENTITY_FACET)) {
                commentModel.addFacet(EXTERNAL_ENTITY_FACET);
            }
            applyDirtyPropertyValues(comment.getDocument(), commentModel);

            // Compute the list of ancestor ids
            commentModel.setPropertyValue(COMMENT_ANCESTOR_IDS_PROPERTY, computeAncestorIds(s, parentId));
            commentModel = s.createDocument(commentModel);
            notifyEvent(s, CommentEvents.COMMENT_ADDED, commentModel);
            return commentModel.getAdapter(Comment.class);
        });
    }

    @Override
    public Comment getComment(CoreSession session, String commentId)
            throws CommentNotFoundException, CommentSecurityException {
        DocumentRef commentRef = new IdRef(commentId);
        // Parent document can be a comment, check existence as a privileged user
        if (!CoreInstance.doPrivileged(session, s -> {
            return s.exists(commentRef);
        })) {
            throw new CommentNotFoundException("The comment " + commentId + " does not exist.");
        }
        NuxeoPrincipal principal = session.getPrincipal();
        return CoreInstance.doPrivileged(session, s -> {
            DocumentModel commentModel = s.getDocument(commentRef);
            DocumentRef documentRef = getTopLevelDocumentRef(s, commentModel.getRef());
            if (!s.hasPermission(principal, documentRef, SecurityConstants.READ)) {
                throw new CommentSecurityException("The user " + principal.getName()
                        + " does not have access to the comments of document " + documentRef.reference());
            }

            return commentModel.getAdapter(Comment.class);
        });
    }

    @Override
    @SuppressWarnings("unchecked")
    public PartialList<Comment> getComments(CoreSession session, String documentId, Long pageSize,
            Long currentPageIndex, boolean sortAscending) throws CommentSecurityException {
        DocumentRef docRef = new IdRef(documentId);
        PageProviderService ppService = Framework.getService(PageProviderService.class);
        NuxeoPrincipal principal = session.getPrincipal();
        return CoreInstance.doPrivileged(session, s -> {
            if (s.exists(docRef)) {
                DocumentRef ancestorRef = getTopLevelDocumentRef(s, docRef);
                if (s.exists(ancestorRef) && !s.hasPermission(principal, ancestorRef, SecurityConstants.READ)) {
                    throw new CommentSecurityException("The user " + principal.getName()
                            + " does not have access to the comments of document " + documentId);
                }
            }
            Map<String, Serializable> props = Collections.singletonMap(CORE_SESSION_PROPERTY, (Serializable) s);
            PageProvider<DocumentModel> pageProvider = (PageProvider<DocumentModel>) ppService.getPageProvider(
                    GET_COMMENTS_FOR_DOC_PAGEPROVIDER_NAME,
                    singletonList(new SortInfo(COMMENT_CREATION_DATE_PROPERTY, sortAscending)), pageSize,
                    currentPageIndex, props, documentId);
            List<DocumentModel> commentList = pageProvider.getCurrentPage();
            return commentList.stream()
                              .map(doc -> doc.getAdapter(Comment.class))
                              .collect(collectingAndThen(toList(),
                                      list -> new PartialList<>(list, pageProvider.getResultsCount())));
        });
    }

    @Override
    public Comment updateComment(CoreSession session, String commentId, Comment comment)
            throws CommentNotFoundException {
        IdRef commentRef = new IdRef(commentId);
        if (!CoreInstance.doPrivileged(session, s -> {
            return s.exists(commentRef);
        })) {
            throw new CommentNotFoundException("The comment " + commentId + " does not exist.");
        }
        NuxeoPrincipal principal = session.getPrincipal();
        if (!principal.isAdministrator() && !comment.getAuthor().equals(principal.getName())) {
            throw new CommentSecurityException(
                    "The user " + principal.getName() + " cannot edit comments of document " + comment.getParentId());
        }
        return CoreInstance.doPrivileged(session, s -> {
            // Initiate Modification Date if it is not done yet
            if (comment.getModificationDate() == null) {
                comment.setModificationDate(Instant.now());
            }

            DocumentModel commentModel = s.getDocument(commentRef);
            if (comment.getDocument().hasFacet(EXTERNAL_ENTITY_FACET)) {
                commentModel.addFacet(EXTERNAL_ENTITY_FACET);
            }
            applyDirtyPropertyValues(comment.getDocument(), commentModel);
            s.saveDocument(commentModel);
            notifyEvent(s, CommentEvents.COMMENT_UPDATED, commentModel);
            return commentModel.getAdapter(Comment.class);
        });
    }

    @Override
    public void deleteComment(CoreSession session, String commentId)
            throws CommentNotFoundException, CommentSecurityException {
        IdRef commentRef = new IdRef(commentId);
        // Document can be a comment, check existence as a privileged user
        if (!CoreInstance.doPrivileged(session, s -> {
            return s.exists(commentRef);
        })) {
            throw new CommentNotFoundException("The comment " + commentId + " does not exist.");
        }

        NuxeoPrincipal principal = session.getPrincipal();
        CoreInstance.doPrivileged(session, s -> {
            DocumentModel comment = s.getDocument(commentRef);
            String parentId = (String) comment.getPropertyValue(COMMENT_PARENT_ID_PROPERTY);
            DocumentRef ancestorRef = getTopLevelDocumentRef(s, commentRef);
            if (s.exists(ancestorRef) && !principal.isAdministrator()
                    && !comment.getPropertyValue(COMMENT_AUTHOR_PROPERTY).equals(principal.getName())
                    && !s.hasPermission(principal, ancestorRef, SecurityConstants.EVERYTHING)) {
                throw new CommentSecurityException(
                        "The user " + principal.getName() + " cannot delete comments of the document " + parentId);
            }
            // Allows the access to its data if needed in listeners
            comment.detach(true);
            s.removeDocument(commentRef);
            notifyEvent(s, CommentEvents.COMMENT_REMOVED, comment);
        });
    }

    @Override
    public Comment getExternalComment(CoreSession session, String entityId) throws CommentNotFoundException {
        DocumentModel commentModel = getExternalCommentModel(session, entityId);
        if (commentModel == null) {
            throw new CommentNotFoundException("The external comment " + entityId + " does not exist.");
        }
        String parentId = (String) commentModel.getPropertyValue(COMMENT_PARENT_ID_PROPERTY);
        if (!session.hasPermission(getTopLevelDocumentRef(session, commentModel.getRef()), SecurityConstants.READ)) {
            throw new CommentSecurityException("The user " + session.getPrincipal().getName()
                    + " does not have access to the comments of document " + parentId);
        }
        return commentModel.getAdapter(Comment.class);
    }

    @Override
    public Comment updateExternalComment(CoreSession session, String entityId, Comment comment)
            throws CommentNotFoundException {
        DocumentModel commentModel = getExternalCommentModel(session, entityId);
        if (commentModel == null) {
            throw new CommentNotFoundException("The external comment " + entityId + " does not exist.");
        }
        NuxeoPrincipal principal = session.getPrincipal();
        if (!principal.isAdministrator() && !comment.getAuthor().equals(principal.getName())) {
            throw new CommentSecurityException(
                    "The user " + principal.getName() + " can not edit comments of document " + comment.getParentId());
        }
        return CoreInstance.doPrivileged(session, s -> {
            applyDirtyPropertyValues(comment.getDocument(), commentModel);
            s.saveDocument(commentModel);
            notifyEvent(s, CommentEvents.COMMENT_UPDATED, commentModel);
            return commentModel.getAdapter(Comment.class);
        });
    }

    @Override
    public void deleteExternalComment(CoreSession session, String entityId) throws CommentNotFoundException {
        DocumentModel commentModel = getExternalCommentModel(session, entityId);
        if (commentModel == null) {
            throw new CommentNotFoundException("The external comment " + entityId + " does not exist.");
        }
        NuxeoPrincipal principal = session.getPrincipal();
        String parentId = (String) commentModel.getPropertyValue(COMMENT_PARENT_ID_PROPERTY);
        if (!principal.isAdministrator()
                && !commentModel.getPropertyValue(COMMENT_AUTHOR_PROPERTY).equals(principal.getName())
                && !session.hasPermission(principal, getTopLevelDocumentRef(session, commentModel.getRef()),
                        SecurityConstants.EVERYTHING)) {
            throw new CommentSecurityException(
                    "The user " + principal.getName() + " can not delete comments of document " + parentId);
        }
        CoreInstance.doPrivileged(session, s -> {
            DocumentModel comment = s.getDocument(commentModel.getRef());
            comment.detach(true);
            s.removeDocument(commentModel.getRef());
            notifyEvent(s, CommentEvents.COMMENT_REMOVED, comment);
        });
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

    @Override
    protected DocumentModel getTopLevelDocument(CoreSession s, DocumentModel commentDoc) {
        DocumentRef ancestorRef = getAncestorRef(s, commentDoc);
        return s.getDocument(ancestorRef);
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
            // No need to notify the creation of the Comments folder
            commentFolderDoc.putContextData(DISABLE_NOTIFICATION_SERVICE, true);
            s.getOrCreateDocument(commentFolderDoc);
            s.save();
            return ref.toString();
        });
    }

    // former top level document method
    protected DocumentRef getAncestorRef(CoreSession session, DocumentModel documentModel) {
        if (!documentModel.hasSchema(COMMENT_SCHEMA)) {
            return documentModel.getRef();
        }
        // thread is the higher comment in a succession of replies
        DocumentModel thread = getThreadForComment(session, documentModel);
        return new IdRef((String) thread.getPropertyValue(COMMENT_PARENT_ID_PROPERTY));
    }

    protected DocumentModel getThreadForComment(CoreSession s, DocumentModel comment) throws CommentSecurityException {
        NuxeoPrincipal principal = s.getPrincipal();
        return CoreInstance.doPrivileged(s, session -> {
            DocumentModel thread = comment;
            DocumentModel parent = s.getDocument(
                    new IdRef((String) thread.getPropertyValue(COMMENT_PARENT_ID_PROPERTY)));
            if (parent.hasSchema(COMMENT_SCHEMA)) {
                thread = getThreadForComment(parent);
            }
            DocumentRef ancestorRef = s.getDocument(
                    new IdRef((String) thread.getPropertyValue(COMMENT_PARENT_ID_PROPERTY))).getRef();
            if (!s.hasPermission(principal, ancestorRef, SecurityConstants.READ)) {
                throw new CommentSecurityException("The user " + principal.getName()
                        + " does not have access to the comments of document " + ancestorRef.reference());
            }
            return thread;
        });
    }

    @Override
    protected DocumentModel getCommentedDocument(CoreSession session, DocumentModel commentDoc) {
        String parentId = (String) commentDoc.getPropertyValue(COMMENT_PARENT_ID_PROPERTY);
        return session.getDocument(new IdRef(parentId));
    }
}
