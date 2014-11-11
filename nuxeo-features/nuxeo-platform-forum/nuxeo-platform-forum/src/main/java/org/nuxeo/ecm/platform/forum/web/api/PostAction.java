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

import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.DocumentModel;

/**
 * Represents the Action Listener for a Post, created inside a Thread.
 *
 * @author <a href="bchaffangeon@nuxeo.com">Brice Chaffangeon</a>
 * @author Anahide Tchertchian
 */
public interface PostAction extends Serializable {

    boolean checkWritePermissionOnThread();

    /**
     * Creates the Post and add it inside the Thread.
     *
     * @return the view id
     * @throws ClientException
     */
    String addPost() throws ClientException;

    /**
     * Deletes the Post and in a the Thread.
     *
     * @return the view id after delete operation
     * @throws ClientException
     */
    String deletePost() throws ClientException;

    /**
     * Cancels the action of adding a Post.
     *
     * @return the view id to stay on thread view
     * @throws ClientException
     */
    String cancelPost() throws ClientException;

    /**
     * Gets the Thread in which the Post is.
     */
    DocumentModel getParentThread();

    /**
     * Returns true if the post is published, false otherwise.
     */
    boolean isPostPublished(DocumentModel post) throws ClientException;

    String approvePost(DocumentModel post) throws ClientException;

    String rejectPost(DocumentModel post) throws ClientException;

    // FIXME : all getters/setters on document metadata should be replaced by a
    // single getter/setter tupole using a document model

    /**
     * Gets the title of the post at creation time.
     */
    String getTitle() throws ClientException;

    /**
     * Sets the title of the post at creation time.
     */
    void setTitle(String title);

    /**
     * Gets the text of the post at creation time.
     */
    String getText();

    /**
     * Sets the text of the post at creation time.
     */
    void setText(String text);

    /**
     * Gets the name of the attached file of the post at creation time.
     */
    String getFilename();

    /**
     * Sets the name of the attached file of the post at creation time.
     */
    void setFilename(String filename);

    /**
     * Gets the content of the attached file of the post at creation time.
     */
    Blob getFileContent();

    /**
     * Sets the content of the attached file of the post at creation time.
     */
    void setFileContent(Blob fileContent);

}
