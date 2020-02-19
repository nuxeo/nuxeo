/*
 * (C) Copyright 2019 Nuxeo (http://nuxeo.com/) and others.
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
 *     Salem Aouana
 */

package org.nuxeo.ecm.platform.comment.impl;

import static java.lang.Boolean.TRUE;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static java.util.Collections.singletonMap;
import static java.util.stream.Collectors.collectingAndThen;
import static java.util.stream.Collectors.toList;
import static org.nuxeo.ecm.core.api.security.SecurityConstants.EVERYTHING;
import static org.nuxeo.ecm.core.io.marshallers.json.document.DocumentModelJsonReader.applyDirtyPropertyValues;
import static org.nuxeo.ecm.platform.comment.api.CommentManager.Feature.COMMENTS_LINKED_WITH_PROPERTY;
import static org.nuxeo.ecm.platform.comment.api.ExternalEntityConstants.EXTERNAL_ENTITY_FACET;
import static org.nuxeo.ecm.platform.comment.impl.PropertyCommentManager.COMMENT_NAME;
import static org.nuxeo.ecm.platform.comment.workflow.utils.CommentsConstants.COMMENTS_DIRECTORY_NAME;
import static org.nuxeo.ecm.platform.comment.workflow.utils.CommentsConstants.COMMENTS_DIRECTORY_TYPE;
import static org.nuxeo.ecm.platform.comment.workflow.utils.CommentsConstants.COMMENT_ANCESTOR_IDS;
import static org.nuxeo.ecm.platform.comment.workflow.utils.CommentsConstants.COMMENT_AUTHOR;
import static org.nuxeo.ecm.platform.comment.workflow.utils.CommentsConstants.COMMENT_CREATION_DATE;
import static org.nuxeo.ecm.platform.comment.workflow.utils.CommentsConstants.COMMENT_PARENT_ID;
import static org.nuxeo.ecm.platform.comment.workflow.utils.CommentsConstants.COMMENT_SCHEMA;
import static org.nuxeo.ecm.platform.ec.notification.NotificationConstants.DISABLE_NOTIFICATION_SERVICE;
import static org.nuxeo.ecm.platform.query.nxql.CoreQueryAndFetchPageProvider.CORE_SESSION_PROPERTY;

import java.io.Serializable;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.nuxeo.ecm.core.api.CoreInstance;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentNotFoundException;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.DocumentSecurityException;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.ecm.core.api.PartialList;
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
 * Comment service implementation. The comments are linked together as a tree under a folder related to the root
 * document that we comment.
 *
 * @since 11.1
 */
public class TreeCommentManager extends AbstractCommentManager {

    protected static final String GET_COMMENT_PAGE_PROVIDER_NAME = "GET_COMMENT_AS_EXTERNAL_ENTITY";

    protected static final String GET_COMMENTS_FOR_DOCUMENT_PAGE_PROVIDER_NAME = "GET_COMMENTS_FOR_DOCUMENT_BY_ECM_PARENT";

    public static final String SERVICE_WITHOUT_IMPLEMENTATION_MESSAGE = "This service implementation does not implement deprecated API.";

    @Override
    public List<DocumentModel> getComments(CoreSession session, DocumentModel doc) {
        return getCommentDocuments(session, doc.getId(), null, null, true);
    }

    @Override
    public Comment getComment(CoreSession session, String commentId) {
        DocumentModel commentDoc = getCommentDocumentModel(session, commentId);
        return commentDoc.getAdapter(Comment.class);
    }

    @Override
    public PartialList<Comment> getComments(CoreSession session, String documentId, Long pageSize,
            Long currentPageIndex, boolean sortAscending) {
        PartialList<DocumentModel> result = getCommentDocuments(session, documentId, pageSize, currentPageIndex,
                sortAscending);
        return result.stream()
                     .map(doc -> doc.getAdapter(Comment.class))
                     .collect(collectingAndThen(toList(), list -> new PartialList<>(list, result.totalSize())));
    }

    @Override
    public List<DocumentModel> getDocumentsForComment(DocumentModel comment) {
        throw new UnsupportedOperationException(SERVICE_WITHOUT_IMPLEMENTATION_MESSAGE);
    }

    @Override
    public DocumentModel getThreadForComment(DocumentModel comment) {
        throw new UnsupportedOperationException(SERVICE_WITHOUT_IMPLEMENTATION_MESSAGE);
    }

    @Override
    public Comment getExternalComment(CoreSession session, String entityId) {
        DocumentModel commentDoc = getExternalCommentModel(session, entityId);
        return commentDoc.getAdapter(Comment.class);
    }

    @Override
    public Comment createComment(CoreSession session, Comment comment) {
        DocumentRef parentRef = new IdRef(comment.getParentId());
        checkCreateCommentPermissions(session, parentRef);

        // Initiate Creation Date if it is not done yet
        if (comment.getCreationDate() == null) {
            comment.setCreationDate(Instant.now());
        }

        return CoreInstance.doPrivileged(session, s -> {
            DocumentModel commentedDoc = s.getDocument(parentRef);
            // Get the location where comment will be stored
            String path = getLocationOfCommentCreation(s, commentedDoc);

            DocumentModel commentDoc = s.createDocumentModel(path, COMMENT_NAME, comment.getDocument().getType());
            if (comment.getDocument().hasFacet(EXTERNAL_ENTITY_FACET)) {
                commentDoc.addFacet(EXTERNAL_ENTITY_FACET);
            }
            applyDirtyPropertyValues(comment.getDocument(), commentDoc);

            commentDoc.setPropertyValue(COMMENT_ANCESTOR_IDS, computeAncestorIds(session, comment.getParentId()));

            // Create the comment document model
            commentDoc = s.createDocument(commentDoc);
            notifyEvent(s, CommentEvents.COMMENT_ADDED, commentedDoc, commentDoc);

            return commentDoc.getAdapter(Comment.class);
        });
    }

    @Override
    public DocumentModel createComment(DocumentModel commentedDoc, DocumentModel commentDoc) {
        // Check the right permissions on document that we want to comment
        checkCreateCommentPermissions(commentDoc.getCoreSession(), commentedDoc.getRef());

        return CoreInstance.doPrivileged(commentDoc.getCoreSession(), session -> {
            // Get the location to store the comment
            String path = getLocationOfCommentCreation(session, commentedDoc);

            DocumentModel commentModelToCreate = session.createDocumentModel(path, COMMENT_NAME, commentDoc.getType());
            commentModelToCreate.copyContent(commentDoc);

            // Should compute ancestors and set comment:parentId for backward compatibility
            commentModelToCreate.setPropertyValue(COMMENT_PARENT_ID, commentedDoc.getId());
            commentModelToCreate.setPropertyValue(COMMENT_ANCESTOR_IDS,
                    computeAncestorIds(session, commentedDoc.getId()));

            commentModelToCreate = session.createDocument(commentModelToCreate);
            commentModelToCreate.detach(true);
            notifyEvent(session, CommentEvents.COMMENT_ADDED, commentedDoc, commentModelToCreate);
            return commentModelToCreate;
        });
    }

    @Override
    public DocumentModel createLocatedComment(DocumentModel doc, DocumentModel comment, String path) {
        throw new UnsupportedOperationException(SERVICE_WITHOUT_IMPLEMENTATION_MESSAGE);
    }

    @Override
    public DocumentModel createComment(DocumentModel doc, String text) {
        throw new UnsupportedOperationException(SERVICE_WITHOUT_IMPLEMENTATION_MESSAGE);
    }

    @Override
    public DocumentModel createComment(DocumentModel doc, String text, String author) {
        throw new UnsupportedOperationException(SERVICE_WITHOUT_IMPLEMENTATION_MESSAGE);
    }

    @Override
    public DocumentModel createComment(DocumentModel doc, DocumentModel parent, DocumentModel child) {
        throw new UnsupportedOperationException(SERVICE_WITHOUT_IMPLEMENTATION_MESSAGE);
    }

    @Override
    public Comment updateComment(CoreSession session, String commentId, Comment comment) {
        // Get the comment doc model
        DocumentModel commentDoc = getCommentDocumentModel(session, commentId);
        return update(session, comment, commentDoc);
    }

    @Override
    public Comment updateExternalComment(CoreSession session, String entityId, Comment comment) {
        // Get the external comment doc model
        DocumentModel commentDoc = getExternalCommentModel(session, entityId);
        return update(session, comment, commentDoc);
    }

    /**
     * @param session the user session, in order to check permissions
     * @param comment the comment holding new data
     * @param commentDoc the {@link DocumentModel} just retrieved from DB
     */
    protected Comment update(CoreSession session, Comment comment, DocumentModel commentDoc) {
        NuxeoPrincipal principal = session.getPrincipal();
        if (!principal.isAdministrator() && !commentDoc.getPropertyValue(COMMENT_AUTHOR).equals(principal.getName())) {
            throw new CommentSecurityException(String.format("The user %s cannot edit comments of document %s",
                    principal.getName(), comment.getParentId()));
        }
        return CoreInstance.doPrivileged(session, s -> {
            if (comment.getModificationDate() == null) {
                comment.setModificationDate(Instant.now());
            }
            if (comment.getDocument().hasFacet(EXTERNAL_ENTITY_FACET)) {
                commentDoc.addFacet(EXTERNAL_ENTITY_FACET);
            }
            applyDirtyPropertyValues(comment.getDocument(), commentDoc);
            DocumentModel updatedDoc = s.saveDocument(commentDoc);
            notifyEvent(session, CommentEvents.COMMENT_UPDATED, updatedDoc);
            return updatedDoc.getAdapter(Comment.class);
        });
    }

    @Override
    public void deleteExternalComment(CoreSession session, String entityId) {
        DocumentModel commentDoc = getExternalCommentModel(session, entityId);
        removeComment(session, commentDoc.getRef());
    }

    @Override
    public void deleteComment(CoreSession s, String commentId) {
        removeComment(s, new IdRef(commentId));
    }

    @Override
    public void deleteComment(DocumentModel doc, DocumentModel comment) {
        throw new UnsupportedOperationException(SERVICE_WITHOUT_IMPLEMENTATION_MESSAGE);
    }

    @Override
    public String getLocationOfCommentCreation(CoreSession session, DocumentModel commentedDoc) {
        if (commentedDoc.hasSchema(COMMENT_SCHEMA)) {
            // reply case, store the reply under the comment
            return commentedDoc.getPathAsString();
        } else {
            // regular document case, store the comment under a CommentRoot folder under the regular document
            DocumentModel commentsFolder = session.createDocumentModel(commentedDoc.getPathAsString(),
                    COMMENTS_DIRECTORY_NAME, COMMENTS_DIRECTORY_TYPE);
            // no need to notify the creation of the Comments folder
            commentsFolder.putContextData(DISABLE_NOTIFICATION_SERVICE, TRUE);
            commentsFolder = session.getOrCreateDocument(commentsFolder);
            session.save();
            return commentsFolder.getPathAsString();
        }
    }

    @Override
    public boolean hasFeature(Feature feature) {
        if (COMMENTS_LINKED_WITH_PROPERTY.equals(feature)) {
            return true;
        }

        throw new UnsupportedOperationException(feature.name());
    }

    @Override
    protected DocumentModel getTopLevelDocument(CoreSession session, DocumentModel commentDoc) {
        DocumentModel documentModel = commentDoc;
        while (documentModel.hasSchema(COMMENT_SCHEMA) || COMMENTS_DIRECTORY_TYPE.equals(documentModel.getType())) {
            documentModel = session.getDocument(documentModel.getParentRef());
        }
        return documentModel;
    }

    /**
     * Checks if the user related to the {@code session} can comments the document linked to the {@code documentRef}.
     */
    protected void checkCreateCommentPermissions(CoreSession session, DocumentRef documentRef) {
        try {
            if (!session.hasPermission(documentRef, SecurityConstants.READ)) {
                throw new CommentSecurityException(String.format("The user %s cannot create comments on document %s",
                        session.getPrincipal().getName(), documentRef));
            }
        } catch (DocumentNotFoundException dnfe) {
            throw new CommentNotFoundException(String.format("The comment %s does not exist.", documentRef), dnfe);
        }
    }

    /**
     * @param session the user session, in order to implicitly check permissions
     * @return the external document model for the given {@code entityId}, if it exists, otherwise throws a
     *         {@link CommentNotFoundException}
     */
    @SuppressWarnings("unchecked")
    protected DocumentModel getExternalCommentModel(CoreSession session, String entityId) {
        PageProviderService ppService = Framework.getService(PageProviderService.class);
        Map<String, Serializable> props = singletonMap(CORE_SESSION_PROPERTY, (Serializable) session);
        PageProvider<DocumentModel> pageProvider = (PageProvider<DocumentModel>) ppService.getPageProvider(
                GET_COMMENT_PAGE_PROVIDER_NAME, Collections.emptyList(), 1L, 0L, props, entityId);
        List<DocumentModel> documents = pageProvider.getCurrentPage();
        if (documents.isEmpty()) {
            throw new CommentNotFoundException(String.format("The external comment %s does not exist.", entityId));
        }
        return documents.get(0);
    }

    /**
     * Remove the comment of the given {@code documentRef}
     *
     * @param session the user session, in order to check permissions
     * @param documentRef the documentRef of the comment document model to delete
     */
    protected void removeComment(CoreSession session, DocumentRef documentRef) {
        NuxeoPrincipal principal = session.getPrincipal();
        CoreInstance.doPrivileged(session, s -> {
            DocumentRef ancestorRef = getTopLevelDocumentRef(s, documentRef);
            DocumentModel commentDoc = s.getDocument(documentRef);
            Serializable author = commentDoc.getPropertyValue(COMMENT_AUTHOR);
            if (!(principal.isAdministrator() //
                    || author.equals(principal.getName()) //
                    || s.hasPermission(principal, ancestorRef, EVERYTHING))) {
                throw new CommentSecurityException(String.format(
                        "The user %s cannot delete comments of the document %s", principal.getName(), ancestorRef));
            }
            // fetch parents before deleting document
            DocumentModel topLevelDoc = getTopLevelDocument(s, commentDoc);
            DocumentModel commentedDoc = getCommentedDocument(s, commentDoc);
            // finally delete document
            s.removeDocument(documentRef);
            notifyEvent(s, CommentEvents.COMMENT_REMOVED, topLevelDoc, commentedDoc, commentDoc);
        });
    }

    /**
     * @param session the user session, in order to implicitly check permissions
     * @return the comment document model of the given {@code documentRef} if it exists, otherwise throws a
     *         {@link CommentNotFoundException}
     * @throws CommentNotFoundException
     */
    protected DocumentModel getCommentDocumentModel(CoreSession session, String id) {
        try {
            return session.getDocument(new IdRef(id));
        } catch (DocumentNotFoundException dnfe) {
            throw new CommentNotFoundException(String.format("The comment %s does not exist.", id), dnfe);
        } catch (DocumentSecurityException dse) {
            throw new CommentSecurityException(String.format("The user %s does not have access to the comment %s",
                    session.getPrincipal().getName(), id), dse);
        }
    }

    /**
     * @return the page provider current page
     */
    @SuppressWarnings("unchecked")
    protected PartialList<DocumentModel> getCommentDocuments(CoreSession session, String documentId, Long pageSize,
            Long currentPageIndex, boolean sortAscending) {
        try {
            DocumentModel doc = session.getDocument(new IdRef(documentId));
            // Depending on the case, the `doc` can be a comment or the top level document
            // if it's the top level document, then we should retrieve all comments under `Comments` folder
            // if it's a comment, then get all comments under it
            if (!doc.hasSchema(COMMENT_SCHEMA) && session.hasChild(doc.getRef(), COMMENTS_DIRECTORY_NAME)) {
                DocumentModel commentsFolder = session.getChild(doc.getRef(), COMMENTS_DIRECTORY_NAME);
                documentId = commentsFolder.getId();
            }

            PageProviderService ppService = Framework.getService(PageProviderService.class);

            Map<String, Serializable> props = Collections.singletonMap(CORE_SESSION_PROPERTY, (Serializable) session);
            List<SortInfo> sortInfos = singletonList(new SortInfo(COMMENT_CREATION_DATE, sortAscending));
            PageProvider<DocumentModel> pageProvider = (PageProvider<DocumentModel>) ppService.getPageProvider(
                    GET_COMMENTS_FOR_DOCUMENT_PAGE_PROVIDER_NAME, sortInfos, pageSize, currentPageIndex, props,
                    documentId);
            return new PartialList<>(pageProvider.getCurrentPage(), pageProvider.getResultsCount());
        } catch (DocumentNotFoundException dnfe) {
            return new PartialList<>(emptyList(), 0);
        } catch (DocumentSecurityException dse) {
            throw new CommentSecurityException(
                    String.format("The user %s does not have access to the comments of document %s",
                            session.getPrincipal().getName(), documentId),
                    dse);
        }
    }

    @Override
    protected DocumentModel getCommentedDocument(CoreSession session, DocumentModel commentDoc) {
        // if comment is a reply then its direct parent is the commented document
        DocumentModel commentedDoc = session.getParentDocument(commentDoc.getRef());

        // if direct parent is the Comments folder then the commented document is Comments parent
        if (COMMENTS_DIRECTORY_TYPE.equals(commentedDoc.getType())) {
            commentedDoc = session.getDocument(commentedDoc.getParentRef());
        }
        return commentedDoc;
    }
}
