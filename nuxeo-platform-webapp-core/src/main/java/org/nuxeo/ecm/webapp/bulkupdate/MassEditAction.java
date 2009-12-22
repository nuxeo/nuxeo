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
 * $Id: JOOoConvertPluginImpl.java 18651 2007-05-13 20:28:53Z sfermigier $
 */

package org.nuxeo.ecm.webapp.bulkupdate;

import java.util.List;

import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.platform.types.FieldWidget;

/**
 * Takes a list of DocumentModel as input and build a form "a-la" iTunes mp3
 * mass tagging utility.
 *
 * @author DM
 */
public interface MassEditAction {

    String putSelectionInWorkList() throws ClientException;

    /**
     * Documents for which mass edition will be performed.
     */
    List<DocumentModel> getDocumentsList();

    void setDocumentsList(List<DocumentModel> docsList) throws ClientException;

    /**
     * Gets common schemas for selected documents.
     */
    FieldWidget[] getCommonLayout() throws ClientException;

    /**
     * Gets the layout (widgets list) corresponding to properties that will
     * be set on documents. The list of documents that would be changed
     * can be retrieved with <code>getChangingDocuments</code> method.
     */
    FieldWidget[] getChangeLayout() throws ClientException;

    /**
     * @return the fields that will be changed for at least a document
     */
    FieldWidget[] getPreviewLayout() throws ClientException;

    /**
     * @return the list of selected documents that will be altered
     */
    List<DocumentModel> getChangingDocuments();

    /**
     * @return the list of selected documents that will remain unchanged
     */
    List<DocumentModel> getUnchangingDocuments();

    /**
     * Display confirmation page showing changes about to be made on
     * selected documents.
     *
     * @return view of the change confirmation page
     */
    String previewChanges();

    /**
     * @return the view associated with mass edit page
     */
    String cancelChanges();

    /**
     * Updates properties (only non-empty values for string properties) for
     * the currently edited documents.
     */
    String updateDocuments() throws ClientException;

    /**
     * Displays one document from the selected documents for bulk editing.
     *
     * @return the view associated with document page
     */
    String viewDocument() throws ClientException;

    /**
     * Mass editing for documents in the clipboard.
     *
     * @return the view for mass edition
     */
    String massEditWorkList() throws ClientException;

    /**
     * A flag that tells to remove edited documents from selection list after saving.
     */
    boolean getRemoveFromList();

    void setRemoveFromList(boolean remove);

    boolean getMapFlag(String key);

}
