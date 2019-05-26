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

package org.nuxeo.ecm.webapp.contentbrowser;

import java.io.Serializable;

import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.platform.types.Type;
import org.nuxeo.ecm.platform.url.api.DocumentView;
import org.nuxeo.ecm.webapp.action.TypesTool;

/**
 * @author <a href="mailto:rcaraghin@nuxeo.com">Razvan Caraghin</a>
 */
public interface DocumentActions extends Serializable {

    String CHILDREN_DOCUMENT_LIST = "CHILDREN_DOCUMENT_LIST";

    /**
     * Saves changes held by the given document, and updates the current document context with the new version.
     * <p>
     * Makes it possible to specify whether current tabs should be restored or not after edition.
     *
     * @since 5.7
     * @param restoreCurrentTabs
     * @return the JSF outcome for navigation after document edition.
     */
    String updateDocument(DocumentModel document, Boolean restoreCurrentTabs);

    /**
     * Updates document considering that current document model holds edited values.
     */
    String updateCurrentDocument();

    /**
     * Creates a document with type given by {@link TypesTool} and stores it in the context as the current changeable
     * document.
     * <p>
     * Returns the create view of given document type.
     */
    String createDocument();

    /**
     * Creates a document with given type and stores it in the context as the current changeable document.
     * <p>
     * Returns the create view of given document type.
     */
    String createDocument(String typeName);

    /**
     * Creates the document from the changeableDocument put in request.
     */
    String saveDocument();

    /**
     * Creates the given document.
     */
    String saveDocument(DocumentModel newDocument);

    /**
     * Downloads file as described by given document view.
     * <p>
     * To be used by url pattern descriptors performing a download.
     *
     * @param docView the document view as generated through the url service
     */
    void download(DocumentView docView);

    /**
     * @return ecm type for current document, <code>null</code> if current doc is null.
     */
    Type getCurrentType();

    Type getChangeableDocumentType();

    /**
     * Checks the current document write permission.
     *
     * @return <code>true</code> if the user has WRITE permission on current document
     */
    boolean getWriteRight();

    /**
     * This method is used to test whether the logged user has enough rights for the unpublish support.
     *
     * @return true if the user can unpublish, false otherwise
     */
    boolean getCanUnpublish();

    void followTransition(DocumentModel changedDocument);

}
