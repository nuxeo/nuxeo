/*
 * (C) Copyright 2006-2007 Nuxeo SA (http://nuxeo.com/) and others.
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

package org.nuxeo.ecm.platform.comment.web;

import java.util.List;

import javax.faces.event.ActionEvent;

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
    void initialize();

    /**
     * Clean all the bean's context variables.
     */
    void documentChanged();

    /**
     * Initializes uiComments with currentDocument.
     */
    void initComments();

    /**
     * Initializes uiComments with given Document.
     */
    void initComments(DocumentModel commentedDoc);

    /**
     * Initializes uiComments with currentDocument if uiComments is null.
     *
     * @return list of ThreadEntry for currentDocument
     */
    List<ThreadEntry> getCommentsAsThread();

    /**
     * Initialize uiComments with given Document if uiComments is null.
     *
     * @return list of ThreadEntry for given Document.
     */
    List<ThreadEntry> getCommentsAsThread(DocumentModel commentedDoc);

    /**
     * @return list of ThreadEntry for given Document.
     */
    List<ThreadEntry> getCommentsAsThreadOnDoc(DocumentModel doc);

    String beginComment();

    String cancelComment();

    /**
     * creates a new comment from
     *
     * @return null to avoid navigation
     */
    String addComment();

    /**
     * Same as addComment() method but using the given document instead of currentDocument.
     *
     * @return null to avoid navigation
     */
    String createComment(DocumentModel docToComment);

    /**
     * Add the given comment DocumentModel to commentableDoc.
     */
    DocumentModel addComment(DocumentModel comment);

    String deleteComment();

    String deleteComment(String commentId);

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

    // List<UIComment> getUiComments();

    /***
     * Retrieves a given number of comments from currentDocument.
     *
     * @param commentNumber the number of comment to fetch
     */
    List<UIComment> getLastCommentsByDate(String commentNumber);

    /***
     * Retrieves a given number of comments from the given Document.
     *
     * @param commentNumber the number of comment to fetch
     * @param commentedDoc
     */
    List<UIComment> getLastCommentsByDate(String commentNumber, DocumentModel commentedDoc);
}
