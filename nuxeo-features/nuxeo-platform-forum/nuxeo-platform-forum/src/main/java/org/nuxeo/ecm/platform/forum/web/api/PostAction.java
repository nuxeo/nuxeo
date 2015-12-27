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
 *     ${user}
 *
 * $Id
 */
package org.nuxeo.ecm.platform.forum.web.api;

import java.io.Serializable;

import org.nuxeo.ecm.core.api.Blob;
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
     */
    String addPost();

    /**
     * Deletes the Post and in a the Thread.
     *
     * @return the view id after delete operation
     */
    String deletePost();

    /**
     * Cancels the action of adding a Post.
     *
     * @return the view id to stay on thread view
     */
    String cancelPost();

    /**
     * Gets the Thread in which the Post is.
     */
    DocumentModel getParentThread();

    /**
     * Returns true if the post is published, false otherwise.
     */
    boolean isPostPublished(DocumentModel post);

    String approvePost(DocumentModel post);

    String rejectPost(DocumentModel post);

    // FIXME : all getters/setters on document metadata should be replaced by a
    // single getter/setter tupole using a document model

    /**
     * Gets the title of the post at creation time.
     */
    String getTitle();

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
