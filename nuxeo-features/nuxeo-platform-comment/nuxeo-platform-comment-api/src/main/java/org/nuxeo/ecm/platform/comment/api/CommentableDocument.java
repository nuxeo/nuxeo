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

import java.io.Serializable;
import java.util.List;

import org.nuxeo.ecm.core.api.DocumentModel;

/**
 * @author <a href="mailto:glefter@nuxeo.com">George Lefter</a>
 */
public interface CommentableDocument extends Serializable {

    /**
     * Returns all comments for this document.
     *
     * @return the list of comments
     */
    List<DocumentModel> getComments();

    /**
     * Returns the comments for this document that are replied to a parent comment.
     *
     * @param parent the parent comment
     * @return the comments for the parent comment
     */
    List<DocumentModel> getComments(DocumentModel parent);

    /**
     * Removes a comment from the document comment list.
     *
     * @param comment
     */
    void removeComment(DocumentModel comment);

    /**
     * Creates a new comment.
     *
     * @param comment
     */
    DocumentModel addComment(DocumentModel comment);

    /**
     * Creates a new comment in a specific location.
     *
     * @since 10.2
     * @param comment the comment to be added
     * @param path the given location
     */
    DocumentModel addComment(DocumentModel comment, String path);

    /**
     * Creates a new comment as a reply to an existing comment.
     *
     * @param parent the parent comment, which must exist
     * @param comment the comment to be added
     */
    DocumentModel addComment(DocumentModel parent, DocumentModel comment);

}
