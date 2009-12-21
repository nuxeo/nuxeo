/*
 * (C) Copyright 2007 Nuxeo SAS (http://nuxeo.com/) and contributors.
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

package org.nuxeo.ecm.platform.comment.api;

import java.io.Serializable;
import java.util.List;

import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.DocumentModel;

/**
 * @author <a href="mailto:glefter@nuxeo.com">George Lefter</a>
 *
 */
public interface CommentableDocument extends Serializable {

    /**
     * Returns all comments for this document.
     *
     * @return the list of comments
     */
    List<DocumentModel> getComments() throws ClientException;

    /**
     * Returns the comments for this document that are replied to a parent comment.
     *
     * @param parent the parent comment
     * @return the comments for the parent comment
     * @throws ClientException
     */
    List<DocumentModel> getComments(DocumentModel parent) throws ClientException;

    /**
     * Removes a comment from the document comment list.
     *
     * @param comment
     */
    void removeComment(DocumentModel comment) throws ClientException;

    /**
     * Creates a new comment.
     *
     * @param comment
     */
    DocumentModel addComment(DocumentModel comment) throws ClientException;

    /**
     * Creates a new comment as a reply to an existing comment.
     *
     * @param parent the parent comment, which must exist
     * @param comment the comment to be added
     * @throws ClientException
     */
    DocumentModel addComment(DocumentModel parent, DocumentModel comment) throws ClientException;

}
