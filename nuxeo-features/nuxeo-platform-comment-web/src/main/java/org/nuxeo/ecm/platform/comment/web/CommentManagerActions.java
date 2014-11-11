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
public interface CommentManagerActions  {

    String CHILDREN_COMMENT_LIST = "CHILDREN_COMMENT_LIST";

    List<Action> getActionsForComment();

    void initialize() throws Exception;

    void documentChanged();

    void getComments() throws ClientException;

    List<ThreadEntry> getCommentsAsThread() throws ClientException;

    List<ThreadEntry> getCommentsAsThreadOnDoc(DocumentModel doc) throws ClientException;

    String beginComment() throws ClientException;

    String cancelComment() throws ClientException;

    String addComment() throws ClientException;

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

    //List<UIComment> getUiComments() throws ClientException;

    /***
     * Retrieves the last n comments.
     *
     * @param n
     * @return
     * @throws ClientException
     */
    List<UIComment> getLastCommentsByDate(String n) throws ClientException;

}
