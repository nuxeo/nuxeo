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

import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.DocumentModel;

/**
 * @author <a href="mailto:glefter@nuxeo.com">George Lefter</a>
 *
 */
public interface CommentConverter {

    /**
     * Transforms the comment object into the document model.
     *
     * @param docModel
     * @param comment
     * @throws ClientException
     */
    void updateDocumentModel(DocumentModel docModel, DocumentModel comment) throws ClientException;

    /**
     * Transforms a documentModel into a comment.
     *
     * @param docModel
     * @return
     * @throws ClientException
     */
    //public Comment getComment(DocumentModel docModel) throws ClientException;

    /**
     * Returns the type for comment document models.
     *
     * @return
     */
    String getDocumentType();

}
