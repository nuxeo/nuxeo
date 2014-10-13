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

package org.nuxeo.ecm.webapp.contentbrowser;

import java.io.Serializable;

import org.nuxeo.ecm.core.api.ClientException;
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
     * Returns the edit view of a document.
     *
     * @deprecated since 5.3: edit views are managed through tabs, the edit
     *             view is not used.
     */
    @Deprecated
    String editDocument() throws ClientException;

    /**
     * Saves changes held by the changeableDocument document model.
     *
     * @deprecated since 5.4.2, currentDocument should be used in edition
     *             screens instead of changeableDocument, so
     *             {@link #updateCurrentDocument()} should be used instead
     */
    @Deprecated
    String updateDocument() throws ClientException;

    /**
     * Saves changes held by the given document, and updates the current
     * document context with the new version.
     * <p>
     * Makes it possible to specify whether current tabs should be restored or
     * not after edition.
     *
     * @since 5.7
     * @param restoreCurrentTabs
     * @return the JSF outcome for navigation after document edition.
     */
    String updateDocument(DocumentModel document, Boolean restoreCurrentTabs)
            throws ClientException;

    /**
     * Saves changes held by the changeableDocument document model in current
     * version and then create a new current one.
     */
    String updateDocumentAsNewVersion() throws ClientException;

    /**
     * Updates document considering that current document model holds edited
     * values.
     */
    String updateCurrentDocument() throws ClientException;

    /**
     * Creates a document with type given by {@link TypesTool} and stores it in
     * the context as the current changeable document.
     * <p>
     * Returns the create view of given document type.
     */
    String createDocument() throws ClientException;

    /**
     * Creates a document with given type and stores it in the context as the
     * current changeable document.
     * <p>
     * Returns the create view of given document type.
     */
    String createDocument(String typeName) throws ClientException;

    /**
     * Creates the document from the changeableDocument put in request.
     */
    String saveDocument() throws ClientException;

    /**
     * Creates the given document.
     */
    String saveDocument(DocumentModel newDocument) throws ClientException;

    @Deprecated
    String download() throws ClientException;

    /**
     * Downloads file as described by given document view.
     * <p>
     * To be used by url pattern descriptors performing a download.
     *
     * @param docView the document view as generated through the url service
     * @throws ClientException when document is not found or file is not
     *             retrieved correctly.
     */
    void download(DocumentView docView) throws ClientException;

    @Deprecated
    String downloadFromList() throws ClientException;

    /**
     * @return ecm type for current document, <code>null</code> if current doc
     *         is null.
     */
    Type getCurrentType();

    Type getChangeableDocumentType();

    /**
     * Checks the current document write permission.
     *
     * @return <code>true</code> if the user has WRITE permission on current
     *         document
     * @throws ClientException
     */
    boolean getWriteRight() throws ClientException;

    /**
     * Returns the comment to attach to the document
     *
     * @deprecated since 5.4: comment can be put directly in the document
     *             context data using key 'request/comment'.
     */
    @Deprecated
    String getComment();

    /**
     * Sets the comment to attach to a document
     *
     * @deprecated since 5.4: comment can be put directly in the document
     *             context data using key 'request/comment'.
     */
    @Deprecated
    void setComment(String comment);

    /**
     * This method is used to test whether the logged user has enough rights
     * for the unpublish support.
     *
     * @return true if the user can unpublish, false otherwise
     * @throws ClientException
     */
    boolean getCanUnpublish();

    /**
     * @deprecated since 5.6: nxl:documentLayout tag now offers the same
     *             features
     */
    @Deprecated
    String getCurrentDocumentSummaryLayout();

    void followTransition(DocumentModel changedDocument) throws ClientException;

}
