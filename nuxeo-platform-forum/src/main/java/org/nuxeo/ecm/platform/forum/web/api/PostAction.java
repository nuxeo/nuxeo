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
 *     ${user}
 *
 * $Id
 */
package org.nuxeo.ecm.platform.forum.web.api;

import java.io.Serializable;
import java.util.Collection;
import java.util.List;

import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.platform.workflow.api.client.wfmc.WMActivityInstance;
import org.nuxeo.ecm.platform.workflow.api.client.wfmc.WMWorkItemInstance;
import org.nuxeo.ecm.platform.workflow.api.client.wfmc.WMWorkflowException;

/**
 * Represents the Action Listener for a Post, created inside a Thread.
 *
 * @author <a href="bchaffangeon@nuxeo.com">Brice Chaffangeon</a>
 */
public interface PostAction extends Serializable {

    boolean checkWritePermissionOnThread();

    /**
     * Gets the title of the post at creation time.
     *
     * @return
     * @throws ClientException
     */
    String getTitle() throws ClientException;

    /**
     * Sets the title of the post at creation time.
     *
     * @param title
     */
    void setTitle(String title);

    /**
     * Gets the text of the post at creation time.
     *
     * @return
     */
    String getText();

    /**
     * Sets the text of the post at creation time.
     *
     * @param text
     */
    void setText(String text);

    /**
     * Gets the name of the attached file of the post at creation time.
     *
     * @return
     */
    String getFilename();

    /**
     * Sets the name of the attached file of the post at creation time.
     *
     */
    void setFilename(String filename);

    /**
     * Gets the content of the attached file of the post at creation time.
     *
     * @return
     */
    Blob getFileContent();

    /**
     * Sets the content of the attached file of the post at creation time.
     *
     */
    void setFileContent(Blob fileContent);

    /**
     * Creates the Post and add it inside the Thread.
     *
     * @return the view id
     * @throws ClientException
     * @throws WMWorkflowException
     */
    String addPost() throws ClientException, WMWorkflowException;

    /**
     * Deletes the Post and in a the Thread.
     *
     * @return the view id after delete operation
     * @throws ClientException
     * @throws WMWorkflowException
     */
    String deletePost() throws ClientException, WMWorkflowException;

    /**
     * Cancels the action of adding a Post.
     * @return the view id to stay on thread view
     * @throws ClientException
     */
    String cancelPost() throws ClientException;

    /**
     * Gets the Thread in which the Post is.
     *
     * @return
     */
    DocumentModel getParentThread();

    /**
     * Gets the parent post of the given post
     * @return the parent Post is exist, null otherwise (if no parent is not a post).
     * @throws ClientException
     */
    //DocumentModel getParentPost(String postNumber) throws ClientException;

    /**
     * Gets the moderators List set on the Thread containing the Post.
     *
     * @return
     */
    List<String> getModeratorsOnParentThread();

    /**
     * Starts the moderation process on a Post. Simply start the Workflow.
     *
     * @param post
     * @return the WMactivity instance created
     * @throws WMWorkflowException
     * @throws ClientException
     */
    WMActivityInstance startModeration(DocumentModel post)
            throws WMWorkflowException, ClientException;

    /**
     * Gets Moderation worklow Id.
     *
     * @return
     * @throws WMWorkflowException
     */
    String getModerationWorkflowId() throws WMWorkflowException;

    /**
     * Returns true if the post is published, false otherwise.
     *
     * @param post
     * @return
     * @throws ClientException
     */
    boolean isPostPublished(DocumentModel post) throws ClientException;

    /*
     * Returns true if the parent of the specified post, is a published Post.
     *
     * @return
     * @throws ClientException
     * @throws WMWorkflowException
     */
    //boolean isParentPostPublished(String postNumber) throws ClientException;

    Collection<WMWorkItemInstance> getPendingTasksForPrincipal()
            throws WMWorkflowException;

    Collection<WMWorkItemInstance> getCurrentTasksForPrincipal(String name)
            throws WMWorkflowException;

    String approvePost(DocumentModel post) throws WMWorkflowException,
            ClientException;

    String rejectPost(DocumentModel post) throws WMWorkflowException,
            ClientException;


}
