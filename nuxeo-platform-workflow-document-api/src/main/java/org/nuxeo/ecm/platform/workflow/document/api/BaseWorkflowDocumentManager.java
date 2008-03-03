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
 *     <a href="mailto:ja@nuxeo.com">Julien Anguenot</a>
 *
 * $Id: BaseWorkflowDocumentManager.java 20600 2007-06-16 17:15:09Z sfermigier $
 */

package org.nuxeo.ecm.platform.workflow.document.api;

import java.io.Serializable;

import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentRef;

/**
 * Base interface for workflow document sessions beans.
 *
 * @author <a href="mailto:ja@nuxeo.com">Julien Anguenot</a>
 *
 */
public interface BaseWorkflowDocumentManager extends Serializable {

    /**
     * Unlock document if locked.
     *
     * @param docRef : the Nuxeo core document Reference.
     * @throws ClientException if exception Nuxeo core side.
     */
    void unlockDocument(DocumentRef docRef) throws ClientException;

    /**
     * Gets the repository URI.
     *
     * @return the repository URI
     */
    String getRepositoryUri();

    /**
     * Sets the repository URI.
     * <p>
     * Needed by the underlying document manager.
     *
     * @param repositoryUri the repository URI
     */
    void setRepositoryUri(String repositoryUri);

    /**
     * Returns a document model from core given a document ref.
     *
     * @param docRef the document reference.
     * @return a DocumentModel instance
     */
    DocumentModel getDocumentModelFor(DocumentRef docRef) throws ClientException;


}
