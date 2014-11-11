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

import javax.annotation.security.PermitAll;
import javax.ejb.Remove;

import org.jboss.seam.annotations.Destroy;
import org.jboss.seam.annotations.remoting.WebRemote;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.platform.types.Type;
import org.nuxeo.ecm.platform.ui.web.model.SelectDataModel;
import org.nuxeo.ecm.platform.ui.web.model.SelectDataModelListener;
import org.nuxeo.ecm.platform.url.api.DocumentView;
import org.nuxeo.ecm.webapp.base.StatefulBaseLifeCycle;

/**
 * @author <a href="mailto:rcaraghin@nuxeo.com">Razvan Caraghin</a>
 */
public interface DocumentActions extends StatefulBaseLifeCycle,
        SelectDataModelListener {

    String CHILDREN_DOCUMENT_LIST = "CHILDREN_DOCUMENT_LIST";

    void initialize();

    @Destroy
    @Remove
    @PermitAll
    void destroy();

    String editDocument() throws ClientException;

    String updateDocument() throws ClientException;

    String updateDocumentAsNewVersion() throws ClientException;

    String updateCurrentDocument() throws ClientException;

    String createDocument() throws ClientException;

    String createDocument(String typeName) throws ClientException;

    String saveDocument() throws ClientException;

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

    SelectDataModel getChildrenSelectModel() throws ClientException;

    SelectDataModel getSectionChildrenSelectModel() throws ClientException;

    /**
     * Checks the current document write permission.
     *
     * @return <code>true</code> if the user has WRITE permission on current
     *         document
     * @throws ClientException
     */
    boolean getWriteRight() throws ClientException;

    /**
     * Handle complete row selection event after having ensured that the
     * navigation context stills points to currentDocumentRef to protect against
     * browsers' back button errors
     *
     * @throws ClientException if currentDocRef is not a valid document
     */
    @WebRemote
    String checkCurrentDocAndProcessSelectRow(String docRef,
            String providerName, String listName, Boolean selection,
            String currentDocRef) throws ClientException;

    @WebRemote
    String processSelectRow(String docRef, String providerName,
            String listName, Boolean selection);

    /**
     * Handle complete table selection event after having ensured that the
     * navigation context stills points to currentDocumentRef to protect against
     * browsers' back button errors
     *
     * @throws ClientException if currentDocRef is not a valid document
     */
    @WebRemote
    String checkCurrentDocAndProcessSelectPage(String providerName,
            String listName, Boolean selection, String currentDocRef)
            throws ClientException;

    @WebRemote
    String processSelectPage(String providerName, String listName,
            Boolean selection);

    String getComment();

    void setComment(String comment);

    /**
     * This method is used to test wheter the looged user has enough rights for
     * the unpublish support.
     *
     * @return - true if the user can unpublish
     *         <p> - false otherwise
     * @throws ClientException
     */
    boolean getCanUnpublish();

}
