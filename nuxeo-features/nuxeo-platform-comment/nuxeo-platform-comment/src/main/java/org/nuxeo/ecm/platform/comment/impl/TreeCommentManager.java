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
import static java.util.Collections.singletonList;
import static java.util.Collections.singletonMap;
import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.collectingAndThen;
import static java.util.stream.Collectors.toList;
import static org.apache.commons.lang3.StringUtils.isEmpty;
import static org.nuxeo.ecm.core.api.security.SecurityConstants.EVERYTHING;
import static org.nuxeo.ecm.core.schema.FacetNames.HAS_RELATED_TEXT;
import static org.nuxeo.ecm.core.storage.BaseDocument.RELATED_TEXT;
import static org.nuxeo.ecm.core.storage.BaseDocument.RELATED_TEXT_ID;
import static org.nuxeo.ecm.core.storage.BaseDocument.RELATED_TEXT_RESOURCES;
import static org.nuxeo.ecm.platform.comment.api.CommentManager.Feature.COMMENTS_LINKED_WITH_PROPERTY;
import static org.nuxeo.ecm.platform.comment.impl.PropertyCommentManager.COMMENT_NAME;
import static org.nuxeo.ecm.platform.comment.workflow.utils.CommentsConstants.COMMENTS_DIRECTORY_NAME;
import static org.nuxeo.ecm.platform.comment.workflow.utils.CommentsConstants.COMMENTS_DIRECTORY_TYPE;
import static org.nuxeo.ecm.platform.comment.workflow.utils.CommentsConstants.COMMENT_ANCESTOR_IDS;
import static org.nuxeo.ecm.platform.comment.workflow.utils.CommentsConstants.COMMENT_AUTHOR;
import static org.nuxeo.ecm.platform.comment.workflow.utils.CommentsConstants.COMMENT_CREATION_DATE;
import static org.nuxeo.ecm.platform.comment.workflow.utils.CommentsConstants.COMMENT_SCHEMA;
import static org.nuxeo.ecm.platform.ec.notification.NotificationConstants.DISABLE_NOTIFICATION_SERVICE;
import static org.nuxeo.ecm.platform.query.nxql.CoreQueryAndFetchPageProvider.CORE_SESSION_PROPERTY;

import java.io.Serializable;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.nuxeo.ecm.core.api.CoreInstance;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentNotFoundException;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.ecm.core.api.PartialList;
import org.nuxeo.ecm.core.api.SortInfo;
import org.nuxeo.ecm.core.api.security.SecurityConstants;
import org.nuxeo.ecm.platform.comment.api.Comment;
import org.nuxeo.ecm.platform.comment.api.CommentEvents;
import org.nuxeo.ecm.platform.comment.api.CommentImpl;
import org.nuxeo.ecm.platform.comment.api.Comments;
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

    /** @since 11.1 **/
    public static final String COMMENT_RELATED_TEXT_ID = "commentRelatedTextId_%s";

    @Override
    public List<DocumentModel> getComments(CoreSession s, DocumentModel docModel) {
        // Check permissions
        checkReadCommentPermissions(s, docModel.getRef());
        return CoreInstance.doPrivileged(s, session -> {
            PageProvider<DocumentModel> pageProvider = getCommentsPageProvider(session, docModel, null, null, true);

            return pageProvider.getCurrentPage();
        });
    }

    @Override
    public Comment getComment(CoreSession s, String commentId) {
        checkReadCommentPermissions(s, new IdRef(commentId));
        return CoreInstance.doPrivileged(s, session -> {
            DocumentModel commentModel = getCommentDocumentModel(s, new IdRef(commentId));
            return Comments.toComment(commentModel);
        });
    }

    @Override
    public PartialList<Comment> getComments(CoreSession s, String documentId, Long pageSize, Long currentPageIndex,
            boolean sortAscending) {
        IdRef docRef = new IdRef(documentId);
        boolean exists = CoreInstance.doPrivileged(s, session -> {
            return session.exists(docRef);
        });
        if (!exists) {
            return new PartialList<>(Collections.emptyList(), 0);
        }
        // Check permissions
        checkReadCommentPermissions(s, new IdRef(documentId));
        DocumentModel documentModel = s.getDocument(docRef);
        return CoreInstance.doPrivileged(s, session -> {
            PageProvider<DocumentModel> pageProvider = getCommentsPageProvider(session, documentModel, pageSize,
                    currentPageIndex, sortAscending);

            List<DocumentModel> commentList = pageProvider.getCurrentPage();
            return commentList.stream()
                              .map(Comments::toComment)
                              .collect(collectingAndThen(toList(),
                                      list -> new PartialList<>(list, pageProvider.getResultsCount())));
        });
    }

    @Override
    public List<DocumentModel> getDocumentsForComment(DocumentModel comment) {
        throw new UnsupportedOperationException(SERVICE_WITHOUT_IMPLEMENTATION_MESSAGE);
    }

    @Override
    public Comment getExternalComment(CoreSession session, String entityId) {
        // Get the external comment
        DocumentModel commentModel = getExternalCommentModel(session, entityId);
        // Check the read permissions using the given session
        checkReadCommentPermissions(session, commentModel.getRef());
        return Framework.doPrivileged(() -> Comments.toComment(commentModel));
    }

    @Override
    public Comment createComment(CoreSession s, Comment comment) {
        checkCreateCommentPermissions(s, new IdRef(comment.getParentId()));

        // Initiate Creation Date if it is not done yet
        if (comment.getCreationDate() == null) {
            comment.setCreationDate(Instant.now());
        }

        return CoreInstance.doPrivileged(s, session -> {
            DocumentModel documentModel = getCommentDocumentModel(session, new IdRef(comment.getParentId()));
            // Get the location where to comment will be stored
            String path = getLocationOfCommentCreation(session, documentModel);

            DocumentModel commentDocModel = session.createDocumentModel(path, COMMENT_NAME,
                    Comments.getDocumentType(comment));
            Comments.toDocumentModel(comment, commentDocModel);

            commentDocModel.setPropertyValue(COMMENT_ANCESTOR_IDS,
                    (Serializable) computeAncestorIds(session, comment.getParentId()));

            // Create the comment document model
            commentDocModel = session.createDocument(commentDocModel);
            Comment createdComment = Comments.toComment(commentDocModel);

            manageRelatedTextOfTopLevelDocument(session, createdComment);

            notifyEvent(session, CommentEvents.COMMENT_ADDED, documentModel, commentDocModel);

            return createdComment;
        });
    }

    @Override
    public DocumentModel createComment(DocumentModel documentModel, DocumentModel commentDocModel) {
        // Check the right permissions on document that we want to comment
        checkCreateCommentPermissions(commentDocModel.getCoreSession(), documentModel.getRef());

        return CoreInstance.doPrivileged(commentDocModel.getCoreSession(), session -> {
            // Get the location to store the comment
            String path = getLocationOfCommentCreation(session, documentModel);

            DocumentModel commentModelToCreate = session.createDocumentModel(path, COMMENT_NAME,
                    commentDocModel.getType());
            commentModelToCreate.copyContent(commentDocModel);

            // Should compute ancestors and set comment:parentId for backward compatibility
            commentModelToCreate.setPropertyValue(COMMENT_ANCESTOR_IDS,
                    (Serializable) computeAncestorIds(session, documentModel.getId()));

            // Create the comment doc model
            commentModelToCreate = session.createDocument(commentModelToCreate);

            manageRelatedTextOfTopLevelDocument(session, Comments.newComment(commentModelToCreate));

            commentModelToCreate.detach(true);
            notifyEvent(session, CommentEvents.COMMENT_ADDED, documentModel, commentModelToCreate);
            return commentModelToCreate;
        });
    }

    @Override
    public DocumentModel createLocatedComment(DocumentModel docModel, DocumentModel comment, String path) {
        throw new UnsupportedOperationException(SERVICE_WITHOUT_IMPLEMENTATION_MESSAGE);
    }

    @Override
    public DocumentModel createComment(DocumentModel docModel, String text) {
        throw new UnsupportedOperationException(SERVICE_WITHOUT_IMPLEMENTATION_MESSAGE);
    }

    @Override
    public DocumentModel createComment(DocumentModel docModel, String text, String author) {
        throw new UnsupportedOperationException(SERVICE_WITHOUT_IMPLEMENTATION_MESSAGE);
    }

    @Override
    public DocumentModel createComment(DocumentModel docModel, DocumentModel parent, DocumentModel child) {
        throw new UnsupportedOperationException(SERVICE_WITHOUT_IMPLEMENTATION_MESSAGE);
    }

    @Override
    public Comment updateComment(CoreSession s, String commentId, Comment comment) {
        // Check the permissions
        checkUpdateCommentPermissions(s, comment);

        return CoreInstance.doPrivileged(s, session -> {
            // Get the comment doc model
            DocumentModel commentDocumentModel = getCommentDocumentModel(session, new IdRef(commentId));
            // Initiate Modification Date if it is not done yet
            if (comment.getModificationDate() == null) {
                comment.setModificationDate(Instant.now());
            }

            Comments.toDocumentModel(comment, commentDocumentModel);

            // Create the comment document model
            commentDocumentModel = session.saveDocument(commentDocumentModel);
            Comment updatedComment = Comments.toComment(commentDocumentModel);

            manageRelatedTextOfTopLevelDocument(session, updatedComment);
            notifyEvent(session, CommentEvents.COMMENT_UPDATED, commentDocumentModel);
            return updatedComment;
        });
    }

    @Override
    public Comment updateExternalComment(CoreSession s, String entityId, Comment comment) {
        // Check the permissions
        checkUpdateCommentPermissions(s, comment);

        return CoreInstance.doPrivileged(s, session -> {
            // Get the external comment doc model
            DocumentModel commentDocModel = getExternalCommentModel(session, entityId);
            Comments.toDocumentModel(comment, commentDocModel);
            commentDocModel = session.saveDocument(commentDocModel);
            Comment updatedComment = Comments.toComment(commentDocModel);

            manageRelatedTextOfTopLevelDocument(session, updatedComment);
            notifyEvent(session, CommentEvents.COMMENT_UPDATED, commentDocModel);
            return updatedComment;
        });
    }

    @Override
    public void deleteExternalComment(CoreSession s, String entityId) {
        DocumentModel commentDocModel = CoreInstance.doPrivileged(s, session -> {
            return getExternalCommentModel(session, entityId);
        });
        removeComment(s, commentDocModel.getRef());
    }

    @Override
    public void deleteComment(CoreSession s, String commentId) {
        removeComment(s, new IdRef(commentId));
    }

    @Override
    public void deleteComment(DocumentModel docModel, DocumentModel comment) {
        throw new UnsupportedOperationException(SERVICE_WITHOUT_IMPLEMENTATION_MESSAGE);
    }

    @Override
    public String getLocationOfCommentCreation(CoreSession session, DocumentModel documentModel) {
        // Get or create the comments folder
        DocumentModel commentsFolder = getOrCreateCommentsFolder(session, documentModel);

        // If this is the first comment of the thread then it will be created under the 'Comments' folder, otherwise
        // (we reply on a given comment) it will be under his comment
        return documentModel.hasSchema(COMMENT_SCHEMA) ? documentModel.getPathAsString()
                : commentsFolder.getPathAsString();
    }

    @Override
    public boolean hasFeature(Feature feature) {
        if (COMMENTS_LINKED_WITH_PROPERTY.equals(feature)) {
            return true;
        }

        throw new UnsupportedOperationException(feature.name());
    }

    @Override
    public DocumentRef getTopLevelCommentAncestor(CoreSession s, DocumentRef documentRef) {
        return CoreInstance.doPrivileged(s, session -> {
            if (!session.exists(documentRef)) {
                throw new CommentNotFoundException(String.format("The comment %s does not exist.", documentRef));
            }

            DocumentModel documentModel = session.getDocument(documentRef);
            while (documentModel.hasSchema(COMMENT_SCHEMA) || COMMENTS_DIRECTORY_TYPE.equals(documentModel.getType())) {
                documentModel = session.getDocument(documentModel.getParentRef());
            }

            NuxeoPrincipal principal = s.getPrincipal();
            if (!session.hasPermission(principal, documentModel.getRef(), SecurityConstants.READ)) {
                throw new CommentSecurityException("The user " + principal.getName()
                        + " does not have access to the comments of document " + documentModel.getRef().reference());
            }

            return documentModel.getRef();
        });
    }

    /**
     * Gets or creates the 'Comments' folder, this folder will be under the document being commented and it contains the
     * whole comments of the first document that we comment.
     *
     * @param session the core session
     * @param documentModel the document model to comment, it's can be the first document of the hierarchy or any
     *            comment that being replied
     * @return the comments folder for a given document model
     */
    protected DocumentModel getOrCreateCommentsFolder(CoreSession session, DocumentModel documentModel) {
        // Depending on the case, the given document model can be the document being commented (the root document, the
        // first comment of the tree) or the comment (case where we reply on existing comment)
        DocumentRef rootDocumentRef = getTopLevelCommentAncestor(session, documentModel.getRef());
        DocumentModel rootDocModel = session.getDocument(rootDocumentRef);

        DocumentModel commentsFolder = session.createDocumentModel(rootDocModel.getPathAsString(),
                COMMENTS_DIRECTORY_NAME, COMMENTS_DIRECTORY_TYPE);
        // No need to notify the creation of the Comments folder
        commentsFolder.putContextData(DISABLE_NOTIFICATION_SERVICE, TRUE);
        commentsFolder = session.getOrCreateDocument(commentsFolder);
        session.save();

        return commentsFolder;
    }

    /**
     * Checks if the user related to the {@code session} can comments the document linked to the {@code documentRef}.
     *
     * @return {@code true} if the user session can comments the given document model, otherwise throws a
     *         {@link CommentSecurityException}
     */
    protected void checkCreateCommentPermissions(CoreSession s, DocumentRef documentRef) {
        DocumentRef rootDocRef = CoreInstance.doPrivileged(s, session -> {
            return getTopLevelCommentAncestor(session, documentRef);
        });
        if (!s.hasPermission(rootDocRef, SecurityConstants.READ)) {
            throw new CommentSecurityException(String.format("The user %s can not create comments on document %s",
                    s.getPrincipal().getName(), rootDocRef));
        }
    }

    /**
     * Checks if the user related to the {@code session} can read the comments of the document linked to the given
     * {@code documentRef}.
     *
     * @return {@code true} if the user session can read the comments of the given document model, otherwise throws a
     *         {@link CommentSecurityException}
     */
    protected void checkReadCommentPermissions(CoreSession s, DocumentRef documentRef) {
        DocumentRef rootDocRef = CoreInstance.doPrivileged(s, session -> {
            return getTopLevelCommentAncestor(session, documentRef);
        });

        if (!s.hasPermission(rootDocRef, SecurityConstants.READ)) {
            throw new CommentSecurityException(
                    String.format("The user %s does not have access to the comments of document %s",
                            s.getPrincipal().getName(), rootDocRef));
        }
    }

    /**
     * Checks if the user related to the {@code session} can update the given {@code comment}.
     *
     * @return {@code true} if the user session can update the given {@code comment}, otherwise throws a
     *         {@link CommentSecurityException}
     */
    protected void checkUpdateCommentPermissions(CoreSession session, Comment comment) {
        NuxeoPrincipal principal = session.getPrincipal();
        if (!principal.isAdministrator() && !comment.getAuthor().equals(principal.getName())) {
            throw new CommentSecurityException(String.format("The user %s cannot edit comments of document %s",
                    principal.getName(), comment.getParentId()));
        }
    }

    /**
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
     * @param s the core session
     * @param documentRef the documentRef of the comment document model to delete
     */
    protected void removeComment(CoreSession s, DocumentRef documentRef) {
        NuxeoPrincipal principal = s.getPrincipal();
        CoreInstance.doPrivileged(s, session -> {
            DocumentRef ancestorRef = getTopLevelCommentAncestor(session, documentRef);
            DocumentModel commentDocModel = session.getDocument(documentRef);
            Serializable author = commentDocModel.getPropertyValue(COMMENT_AUTHOR);
            if (!(principal.isAdministrator() //
                    || author.equals(principal.getName()) //
                    || session.hasPermission(principal, ancestorRef, EVERYTHING))) {
                throw new CommentSecurityException(String.format(
                        "The user %s cannot delete comments of the document %s", principal.getName(), ancestorRef));
            }

            // Create a comment without a text to allow removing it from the related resources of the top level document
            Comment comment = new CommentImpl();
            comment.setText(null);
            comment.setId(commentDocModel.getId());
            comment.setParentId(ancestorRef.toString());

            manageRelatedTextOfTopLevelDocument(session, comment);

            DocumentModel parent = session.getDocument(commentDocModel.getParentRef());
            commentDocModel.detach(true);
            session.removeDocument(documentRef);
            notifyEvent(session, CommentEvents.COMMENT_REMOVED, commentDocModel);
        });

    }

    /**
     * @return the comment document model of the given {@code documentRef} if it exists, otherwise throws a
     *         {@link CommentNotFoundException}
     * @throws CommentNotFoundException
     */
    public DocumentModel getCommentDocumentModel(CoreSession session, DocumentRef documentRef) {
        try {
            return session.getDocument(documentRef);
        } catch (DocumentNotFoundException dnf) {
            throw new CommentNotFoundException(String.format("The comment %s does not exist.", documentRef));
        }
    }

    /**
     * @return the page provider that hold the comments documents model
     */
    @SuppressWarnings("unchecked")
    protected PageProvider<DocumentModel> getCommentsPageProvider(CoreSession session, DocumentModel documentModel,
            Long pageSize, Long currentPageIndex, boolean sortAscending) {

        PageProviderService ppService = Framework.getService(PageProviderService.class);

        Map<String, Serializable> props = Collections.singletonMap(CORE_SESSION_PROPERTY, (Serializable) session);
        List<SortInfo> sortInfos = singletonList(new SortInfo(COMMENT_CREATION_DATE, sortAscending));

        // Depending on the case, the `documentModel` can be a comment or not
        // if it's a not comment, then we should retrieve all comments under `Comments` folder
        // otherwise, it's a comment, then get all comments under it
        String documentId = documentModel.getId();
        if (!documentModel.hasSchema(COMMENT_SCHEMA)
                && session.hasChild(documentModel.getRef(), COMMENTS_DIRECTORY_NAME)) {
            DocumentModel commentsFolder = session.getChild(documentModel.getRef(), COMMENTS_DIRECTORY_NAME);
            documentId = commentsFolder.getId();
        }

        return (PageProvider<DocumentModel>) ppService.getPageProvider(GET_COMMENTS_FOR_DOCUMENT_PAGE_PROVIDER_NAME,
                sortInfos, pageSize, currentPageIndex, props, documentId);
    }

    /**
     * Manages (Add, Update or Remove) the related text {@link org.nuxeo.ecm.core.schema.FacetNames#HAS_RELATED_TEXT} of
     * the top level document ancestor {@link #getTopLevelCommentAncestor(CoreSession, DocumentRef)} for the given
     * comment / annotation. Each action of adding, updating or removing the comment / annotation text will call this
     * method, which allow us to make the right action on the related text of the top level document.
     * <ul>
     * <li>Add a new Comment / Annotation will create a separate entry</li>
     * <li>Update a text Comment / Annotation will update this specific entry</li>
     * <li>Remove a Comment / Annotation will remove this specific entry</li>
     * </ul>
     *
     * @since 11.1
     **/
    protected void manageRelatedTextOfTopLevelDocument(CoreSession session, Comment comment) {
        requireNonNull(comment.getId(), "Comment id is required");
        requireNonNull(comment.getParentId(), "Comment parent id is required");

        // Get the Top level document model (the first document of our comments tree)
        // which will contains the text of comments / annotations
        DocumentRef topLevelDocRef = getTopLevelCommentAncestor(session, new IdRef(comment.getParentId()));
        DocumentModel topLevelDoc = session.getDocument(topLevelDocRef);
        topLevelDoc.addFacet(HAS_RELATED_TEXT);

        // Get the related text id (the related text key is different in the case of Comment or Annotation)
        String relatedTextId = String.format(COMMENT_RELATED_TEXT_ID, comment.getId());

        @SuppressWarnings("unchecked")
        List<Map<String, String>> resources = (List<Map<String, String>>) topLevelDoc.getPropertyValue(
                RELATED_TEXT_RESOURCES);

        Optional<Map<String, String>> optional = resources.stream()
                                                          .filter(m -> relatedTextId.equals(m.get(RELATED_TEXT_ID)))
                                                          .findAny();

        if (isEmpty(comment.getText())) {
            // Remove
            optional.ifPresent(resources::remove);
        } else {
            optional.ifPresentOrElse( //
                    (map) -> map.put(RELATED_TEXT, comment.getText()), // Update
                    () -> resources.add(Map.of(RELATED_TEXT_ID, relatedTextId, RELATED_TEXT, comment.getText()))); // Creation
        }

        topLevelDoc.setPropertyValue(RELATED_TEXT_RESOURCES, (Serializable) resources);
        topLevelDoc.putContextData(DISABLE_NOTIFICATION_SERVICE, TRUE);
        session.saveDocument(topLevelDoc);
    }

    @Override
    public DocumentRef getCommentedDocumentRef(CoreSession session, DocumentModel commentDocumentModel) {
        // Case when commentDocumentModel is already the document being commented
        DocumentModel commentedDocModel = commentDocumentModel;

        // Case when commentDocumentModel is a comment (can be the first comment or any reply)
        if (commentDocumentModel.hasSchema(COMMENT_SCHEMA)) {
            commentedDocModel = session.getDocument(commentDocumentModel.getParentRef());
        }

        // Case when commentDocumentModel is the folder that contains the comments
        if (COMMENTS_DIRECTORY_TYPE.equals(commentedDocModel.getType())) {
            commentedDocModel = session.getDocument(commentedDocModel.getParentRef());
        }

        return commentedDocModel.getRef();
    }
}
