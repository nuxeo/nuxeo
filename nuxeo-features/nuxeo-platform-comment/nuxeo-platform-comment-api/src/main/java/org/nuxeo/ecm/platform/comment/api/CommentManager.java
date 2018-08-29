/*
 * (C) Copyright 2007 Nuxeo SA (http://nuxeo.com/) and others.
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
 *
 * $Id$
 */

package org.nuxeo.ecm.platform.comment.api;

import java.util.List;

import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.PartialList;

/**
 * @author <a href="mailto:glefter@nuxeo.com">George Lefter</a>
 */
public interface CommentManager {

    List<DocumentModel> getComments(DocumentModel docModel);

    List<DocumentModel> getComments(DocumentModel docModel, DocumentModel parent);

    /**
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
     */
    DocumentModel createComment(DocumentModel docModel, String comment, String author);

    DocumentModel createComment(DocumentModel docModel, DocumentModel comment);

    DocumentModel createComment(DocumentModel docModel, DocumentModel parent, DocumentModel child);

    void deleteComment(DocumentModel docModel, DocumentModel comment);

    /**
     * Gets documents in relation with a particular comment.
     *
     * @param comment the comment
     * @return the list of documents
     */
    List<DocumentModel> getDocumentsForComment(DocumentModel comment);

    /**
     * Gets thread in relation with a given comment (post or comment)
     *
     * @param comment
     * @return
     * @since 5.5
     */
    DocumentModel getThreadForComment(DocumentModel comment);

    /**
     * Creates a comment document model. It gives opportunity to save the comments in a specified location.
     *
     * @param docModel the document to comment
     * @param comment the comment content
     * @param path the location path
     * @return the comment document model.
     */
    DocumentModel createLocatedComment(DocumentModel docModel, DocumentModel comment, String path);

    /**
     * Creates a comment.
     *
     * @param session the core session
     * @return the created comment
     * @throws IllegalArgumentException if the document to comment does not exist
     * @since 10.3
     */
    Comment createComment(CoreSession session, Comment comment) throws IllegalArgumentException;

    /**
     * Gets a comment.
     *
     * @param session the core session
     * @param commentId the comment id
     * @return the comment
     * @throws IllegalArgumentException if the comment does not exist
     * @since 10.3
     */
    Comment getComment(CoreSession session, String commentId) throws IllegalArgumentException;

    /**
     * Gets all comments for a document.
     *
     * @param session the core session
     * @param documentId the document id
     * @return the list of comments, or an em pty list if no comment is found
     * @throws IllegalArgumentException if no document was found with the given id
     * @since 10.3
     */
    List<Comment> getComments(CoreSession session, String documentId) throws IllegalArgumentException;

    /**
     * Gets all comments for a document.
     * 
     * @param session the core session
     * @param documentId the document id
     * @param pageSize the page size to query, give null or 0 to disable pagination
     * @param currentPageIndex the page index to query, give null or 0 to disable pagination
     * @return the list of comments, or an empty list if no comment is found
     * @throws IllegalArgumentException if no document was found with the given id
     * @since 10.3
     */
    PartialList<Comment> getComments(CoreSession session, String documentId, Long pageSize, Long currentPageIndex)
            throws IllegalArgumentException;

    /**
     * Updates a comment.
     * 
     * @param session the core session
     * @param commentId the comment id
     * @param comment the updated comment
     * @throws IllegalArgumentException if the comment does not exist
     * @since 10.3
     */
    void updateComment(CoreSession session, String commentId, Comment comment) throws IllegalArgumentException;

    /**
     * Deletes a comment.
     *
     * @param session the core session
     * @param commentId the comment id
     * @throws IllegalArgumentException if no comment was found with the given id
     * @since 10.3
     */
    void deleteComment(CoreSession session, String commentId) throws IllegalArgumentException;

    /**
     * Gets a comment generated by an external service.
     *
     * @param session the core session
     * @param entityId the external entity id
     * @return the comment
     * @throws IllegalArgumentException if no comment was found with the given id
     * @since 10.3
     */
    Comment getExternalComment(CoreSession session, String entityId) throws IllegalArgumentException;

    /**
     * Updates an external comment.
     *
     * @param session the core session
     * @param entityId the external entity id
     * @param comment the comment containing the modifications
     * @since 10.3
     */
    void updateExternalComment(CoreSession session, String entityId, Comment comment)
            throws IllegalArgumentException;

    /**
     * Deletes an external comment.
     *
     * @param session the core session
     * @param entityId the external entity id
     * @throws IllegalArgumentException if no comment was found with the given id
     * @since 10.3
     */
    void deleteExternalComment(CoreSession session, String entityId) throws IllegalArgumentException;

}
