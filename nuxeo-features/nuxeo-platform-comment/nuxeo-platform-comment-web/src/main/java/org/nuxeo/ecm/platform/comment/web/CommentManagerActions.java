/*
 * (C) Copyright 2006-2007 Nuxeo SAS (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     Nuxeo - initial API and implementation
 *
 * $Id$
 */

package org.nuxeo.ecm.platform.comment.web;

import java.util.List;

import javax.faces.event.ActionEvent;

import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.platform.actions.Action;

/**
 * Provides comment manager related operations.
 *
 * @author <a href="mailto:glefter@nuxeo.com">George Lefter</a>
 */
public interface CommentManagerActions {

    String CHILDREN_COMMENT_LIST = "CHILDREN_COMMENT_LIST";

    /**
     * @return available action for COMMENTS_ACTIONS
     */
    List<Action> getActionsForComment();

    /**
     * @return available action for given category
     */
    List<Action> getActionsForComment(String category);

    /**
     * Bean initialization method.
     */
    void initialize() throws Exception;

    /**
     * Clean all the bean's context variables.
     */
    void documentChanged();

    /**
     * Initializes uiComments with currentDocument.
     */
    void initComments() throws ClientException;

    /**
     * Initializes uiComments with given Document.
     */
    void initComments(DocumentModel commentedDoc) throws ClientException;

    /**
     * Initializes uiComments with currentDocument if uiComments is null.
     *
     * @return list of ThreadEntry for currentDocument
     */
    List<ThreadEntry> getCommentsAsThread() throws ClientException;

    /**
     * Initialize uiComments with given Document if uiComments is null.
     *
     * @return list of ThreadEntry for given Document.
     */
    List<ThreadEntry> getCommentsAsThread(DocumentModel commentedDoc)
            throws ClientException;

    /**
     * @return list of ThreadEntry for given Document.
     */
    List<ThreadEntry> getCommentsAsThreadOnDoc(DocumentModel doc)
            throws ClientException;

    String beginComment();

    String cancelComment();

    /**
     * creates a new comment from
     *
     * @return null to avoid navigation
     */
    String addComment() throws ClientException;

    /**
     * Same as addComment() method but using the given document instead of
     * currentDocument.
     *
     * @return null to avoid navigation
     */
    String createComment(DocumentModel docToComment) throws ClientException;

    /**
     * Add the given comment DocumentModel to commentableDoc.
     */
    DocumentModel addComment(DocumentModel comment) throws ClientException;

    String deleteComment() throws ClientException;

    String deleteComment(String commentId) throws ClientException;

    void destroy();

    String getNewContent();

    void setNewContent(String newContent);

    String getPrincipalName();

    boolean getPrincipalIsAdmin();

    boolean getCommentStarted();

    String getSavedReplyCommentId();

    void setSavedReplyCommentId(String savedReplyCommentId);

    boolean getShowCreateForm();

    void setShowCreateForm(boolean flag);

    void toggleCreateForm(ActionEvent event);

    // List<UIComment> getUiComments() throws ClientException;

    /***
     * Retrieves a given number of comments from currentDocument.
     *
     * @param commentNumber the number of comment to fetch
     */
    List<UIComment> getLastCommentsByDate(String commentNumber)
            throws ClientException;

    /***
     * Retrieves a given number of comments from the given Document.
     *
     * @param commentNumber the number of comment to fetch
     * @param commentedDoc
     */
    List<UIComment> getLastCommentsByDate(String commentNumber,
            DocumentModel commentedDoc) throws ClientException;
}
