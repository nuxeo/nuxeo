/*
 * (C) Copyright 2007-2018 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Nuxeo - initial API and implementation
 *     Nuno Cunha <ncunha@nuxeo.com>
 *
 * $Id$
 */

package org.nuxeo.ecm.platform.comment.api;

import java.util.List;

import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.PartialList;
import org.nuxeo.ecm.platform.comment.api.exceptions.CommentNotFoundException;
import org.nuxeo.ecm.platform.comment.api.exceptions.CommentSecurityException;

/**
 * Service to deal with {@link Comment}.
 * <p/>
 * We call comment the document model representing a comment.
 * <p/>
 * We call parent/commented document the document model being commented (regular document or comment).
 * <p/>
 * We call top level document the regular document which owns all comments.
 */
public interface CommentManager {

    /**
     * Features of the implementation of the service.
     *
     * @see CommentManager#hasFeature
     * @since 10.3
     */
    enum Feature {
        /** Comments are linked with the parent id property. */
        COMMENTS_LINKED_WITH_PROPERTY,
    }

    /**
     * Gets comments of a document.
     *
     * @param docModel the document model
     * @return the list of comments
     */
    List<DocumentModel> getComments(DocumentModel docModel);

    /**
     * Gets comments of a document.
     *
     * @param session the core session
     * @param docModel the document model
     * @return the list of comments
     * @throws CommentSecurityException if the current user does not have the right permissions on the commented
     *             document
     * @since 10.3
     */
    List<DocumentModel> getComments(CoreSession session, DocumentModel docModel) throws CommentSecurityException;

    /**
     * Get comments of a document.
     *
     * @param docModel the document model
     * @param parent the parent document model
     * @return the list of comments
     * @deprecated since 10.3, use {@link #getComments(DocumentModel)} instead.
     */
    @Deprecated
    List<DocumentModel> getComments(DocumentModel docModel, DocumentModel parent);

    /**
     * Creates a comment.
     *
     * @param docModel the document to comment
     * @param comment the comment content
     * @deprecated CommentManager cannot find the author if invoked remotely so one should use
     *             {@link #createComment(DocumentModel, String, String)}
     */
    @Deprecated
    DocumentModel createComment(DocumentModel docModel, String comment);

    /**
     * Creates a comment document model, filling its properties with given info and linking it to given document.
     *
     * @param docModel the document to comment
     * @param comment the comment content
     * @param author the comment author
     * @return the comment document model.
     * @deprecated since 10.3, use {@link #createComment(CoreSession, Comment)} instead.
     */
    @Deprecated
    DocumentModel createComment(DocumentModel docModel, String comment, String author);

    /**
     * Creates a comment document model, filling its properties with given info and linking it to given document.
     *
     * @param docModel the document to comment
     * @param comment the comment document model
     * @return the created comment document model.
     * @throws CommentSecurityException if the current user does not have the right permissions on the document to
     *             comment.
     */
    DocumentModel createComment(DocumentModel docModel, DocumentModel comment) throws CommentSecurityException;

    /**
     * Creates a comment document model, filling its properties with given info and linking it to given document.
     *
     * @param docModel the document to comment
     * @param parent the comment parent document model
     * @param child the comment child document model
     * @return the created comment document model.
     * @deprecated since 10.3, use {@link #createComment(CoreSession, Comment)} instead.
     */
    @Deprecated
    DocumentModel createComment(DocumentModel docModel, DocumentModel parent, DocumentModel child);

    /**
     * Deletes a comment.
     *
     * @param docModel the comment document model
     * @param comment the comment
     * @deprecated since 10.3, use {@link #deleteComment(CoreSession, String)} instead.
     */
    @Deprecated
    void deleteComment(DocumentModel docModel, DocumentModel comment);

    /**
     * Gets documents in relation with a particular comment.
     *
     * @param comment the comment
     * @return the list of documents
     * @deprecated since 10.3, only used with deprecated implementation, no replacement.
     */
    @Deprecated
    List<DocumentModel> getDocumentsForComment(DocumentModel comment);

    /**
     * Gets thread in relation with a given comment (post or comment).
     *
     * @param comment the comment
     * @return the thread
     * @throws CommentSecurityException if the current user does not have the right permissions on the commented
     *             document.
     * @since 5.5
     * @deprecated since 11.1. Use {@link #getTopLevelCommentAncestor(CoreSession, DocumentRef)} instead.
     */
    @Deprecated
    DocumentModel getThreadForComment(DocumentModel comment) throws CommentSecurityException;

    /**
     * Creates a comment document model. It gives opportunity to save the comments in a specified location.
     *
     * @param docModel the document to comment
     * @param comment the comment content
     * @param path the location path
     * @return the comment document model.
     * @throws CommentSecurityException if the current user does not have the right permissions on the document to
     *             comment.
     */
    DocumentModel createLocatedComment(DocumentModel docModel, DocumentModel comment, String path)
            throws CommentSecurityException;

    /**
     * Creates a comment.
     *
     * @param session the core session
     * @return the created comment
     * @throws CommentNotFoundException if the document to comment, i.e. comment's parent, does not exist.
     * @throws CommentSecurityException if the current user does not have the right permissions on the document to
     *             comment.
     * @since 10.3
     */
    Comment createComment(CoreSession session, Comment comment)
            throws CommentNotFoundException, CommentSecurityException;

    /**
     * Gets a comment.
     *
     * @param session the core session
     * @param commentId the comment id
     * @return the comment
     * @throws CommentNotFoundException if the comment does not exist
     * @throws CommentSecurityException if the current user does not have the right permissions on the commented
     *             document.
     * @since 10.3
     */
    Comment getComment(CoreSession session, String commentId) throws CommentNotFoundException, CommentSecurityException;

    /**
     * Gets all comments for a document.
     *
     * @param session the core session
     * @param documentId the document id
     * @return the list of comments, ordered ascending by comment's creation date, or an empty list if no comment is
     *         found.
     * @since 10.3
     */
    List<Comment> getComments(CoreSession session, String documentId);

    /**
     * Gets all comments for a document.
     *
     * @param session the core session
     * @param documentId the document id
     * @param sortAscending whether to sort ascending or descending
     * @return the list of comments, ordered by comment's creation date and according to sortAscending parameter, or an
     *         empty list if no comment is found.
     * @since 10.3
     */
    List<Comment> getComments(CoreSession session, String documentId, boolean sortAscending);

    /**
     * Gets all comments for a document.
     * 
     * @param session the core session
     * @param documentId the document id
     * @param pageSize the page size to query, give null or 0 to disable pagination
     * @param currentPageIndex the page index to query, give null or 0 to disable pagination
     * @return the list of comments, ordered ascending by comment's creation date, or an empty list if no comment is
     *         found.
     * @since 10.3
     */
    PartialList<Comment> getComments(CoreSession session, String documentId, Long pageSize, Long currentPageIndex);

    /**
     * Gets all comments for a document.
     *
     * @param session the core session
     * @param documentId the document id
     * @param pageSize the page size to query, give null or 0 to disable pagination
     * @param currentPageIndex the page index to query, give null or 0 to disable pagination
     * @param sortAscending whether to sort ascending or descending
     * @return the list of comments, ordered by comment's creation date and according to sortAscending parameter, or an
     *         empty list if no comment is found.
     * @throws CommentSecurityException if the current user does not have the right permissions on the commented
     *             document.
     * @since 10.3
     */
    PartialList<Comment> getComments(CoreSession session, String documentId, Long pageSize, Long currentPageIndex,
            boolean sortAscending) throws CommentSecurityException;

    /**
     * Updates a comment.
     * 
     * @param session the core session
     * @param commentId the comment id
     * @param comment the updated comment
     * @return the updated comment
     * @throws CommentNotFoundException if no comment was found with the given id.
     * @throws CommentSecurityException if the current user does not have the right permissions on the commented
     *             document.
     * @since 10.3
     */
    Comment updateComment(CoreSession session, String commentId, Comment comment)
            throws CommentNotFoundException, CommentSecurityException;

    /**
     * Deletes a comment.
     *
     * @param session the core session
     * @param commentId the comment id
     * @throws CommentNotFoundException if no comment was found with the given id.
     * @throws CommentSecurityException if the current user does not have the right permissions on the commented
     *             document.
     * @since 10.3
     */
    void deleteComment(CoreSession session, String commentId) throws CommentNotFoundException, CommentSecurityException;

    /**
     * Gets a comment generated by an external service.
     *
     * @param session the core session
     * @param entityId the external entity id
     * @return the comment
     * @throws CommentNotFoundException if no comment was found with the given external entity id.
     * @throws CommentSecurityException if the current user does not have the right permissions on the commented
     *             document.
     * @since 10.3
     */
    Comment getExternalComment(CoreSession session, String entityId)
            throws CommentNotFoundException, CommentSecurityException;

    /**
     * Updates an external comment.
     *
     * @param session the core session
     * @param entityId the external entity id
     * @param comment the comment containing the modifications
     * @return the updated comment
     * @throws CommentNotFoundException if no comment was found with the given external entity id.
     * @throws CommentSecurityException if the current user does not have the right permissions on the commented
     *             document.
     * @since 10.3
     */
    Comment updateExternalComment(CoreSession session, String entityId, Comment comment)
            throws CommentNotFoundException, CommentSecurityException;

    /**
     * Deletes an external comment.
     *
     * @param session the core session
     * @param entityId the external entity id
     * @throws CommentNotFoundException if no comment was found with the given external entity id.
     * @throws CommentSecurityException if the current user does not have the right permissions on the commented
     *             document.
     * @since 10.3
     */
    void deleteExternalComment(CoreSession session, String entityId)
            throws CommentNotFoundException, CommentSecurityException;

    /**
     * Checks if a feature is available.
     *
     * @since 10.3
     */
    boolean hasFeature(Feature feature);

    /**
     * Gets the top level ancestor document ref for the given document model comment ref. No matter how many levels of
     * comments there is.
     * <p>
     * Given a document fileOne, that we comment with commentOne which we reply on with replyOne
     * <p>
     * This method will return:
     * <ul>
     * <li>CommentManager#getAncestorRef(session, commentOne) = fileOne</li>
     * <li>CommentManager#getAncestorRef(session, replyOne) = fileOne</li>
     * </ul>
     *
     * @param session the CoreSession
     * @param commentRef the comment document model ref
     * @return the top level ancestor document ref
     * @since 11.1
     */
     DocumentRef getTopLevelDocumentRef(CoreSession session, DocumentRef commentRef);

    /**
     * Returns the comments location in repository for the given commented document model.
     *
     * @param session the session needs to be privileged
     * @return the comments location in repository for the given commented document model.
     * @apiNote This is dedicated to an internal usage (comment service/migration)
     * @since 11.1
     */
    default String getLocationOfCommentCreation(CoreSession session, DocumentModel commentedDocModel) {
        throw new UnsupportedOperationException();
    }


}
