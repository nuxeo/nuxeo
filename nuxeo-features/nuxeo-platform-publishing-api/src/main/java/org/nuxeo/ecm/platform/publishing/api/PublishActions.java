/*
 * (C) Copyright 2007-2008 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     Narcis Paslaru
       Florent Guillaume
 */

package org.nuxeo.ecm.platform.publishing.api;

import java.io.Serializable;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jboss.seam.annotations.Destroy;
import org.jboss.seam.annotations.WebRemote;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.DocumentModelTreeNode;
import org.nuxeo.ecm.platform.actions.Action;
import org.nuxeo.ecm.platform.ui.web.model.SelectDataModel;
import org.nuxeo.ecm.platform.ui.web.model.SelectDataModelListener;

/**
 * Interface for publishing documents page action listener. Exposes methods for
 * handling user actions related to the publish button(s).
 *
 * @author Narcis Paslaru
 * @author Florent Guillaume
 */
// XXX shouldn't be here : Seam remoting bug
public interface PublishActions extends SelectDataModelListener {

    String SECTIONS_DOCUMENT_TREE = "SECTIONS_DOCUMENT_TREE";

    String CHILDREN_DOCUMENT_LIST = "CHILDREN_DOCUMENT_LIST";

    /**
     * Returns the list of available web actions for the currently selected
     * DocumentList - list of sections.
     *
     * @return the WebAction list
     */
    List<Action> getActionsForPublishDocument();

    /**
     * Retrieves all the sections in the domain.
     *
     * @return
     * @throws ClientException
     */
    SelectDataModel getSectionsModel() throws ClientException;

    /**
     * Retrieves all visible proxies for this document.
     *
     * @deprecated Unused externally, will be moved to protected
     */
    @Deprecated
    DocumentModelList getProxies(DocumentModel docModel) throws ClientException;

    /**
     * Retrieves all visible proxies for this document, and associated visible
     * sections.
     *
     * @deprecated Unused externally, will be moved to protected
     * @throws ClientException
     */
    @Deprecated
    List<PublishingInformation> getPublishingInformation(DocumentModel docModel)
            throws ClientException;

    /**
     * Publishes the current document to the selected sections.
     *
     * @throws ClientException
     */
    String publishDocument() throws ClientException;

    DocumentModel publishDocument(DocumentModel docToPublish,
            DocumentModel section) throws ClientException;

    String publishDocumentList(String listName) throws ClientException;

    String publishWorkList() throws ClientException;

    /**
     * Nullifies the sectionSelectModel when the document is changed.
     *
     */
    void cancelTheSections();

    String getComment();

    void setComment(String comment);

    /**
     * Unpublish a proxy of the current document, having as a request parameter
     * with the name 'unPublishSectionRef,' the name of the section from which
     * to unpublish the current document.
     *
     * @return the JSF outcome
     * @throws ClientException
     */
    String unPublishDocument() throws ClientException;

    /**
     * This method is used to unpublish the given proxy.
     *
     * @param proxy - the proxy to unpublish.
     * @throws ClientException
     * @deprecated Unused externally, will be moved to protected
     */
    @Deprecated
    void unPublishDocument(DocumentModel proxy) throws ClientException;

    /**
     * This method is used to unpublish the given list of document models.
     *
     * @param documentsList - the list of the document models which are going to
     *                be unpublish.
     * @throws ClientException
     * @deprecated Unused externally, will be moved to protected
     */
    @Deprecated
    void unPublishDocuments(List<DocumentModel> documentsList)
            throws ClientException;

    /**
     * This method is used to unpublish the current document list selection.
     *
     * @throws ClientException
     */
    void unPublishDocumentsFromCurrentSelection() throws ClientException;

    /*
     * Rux NXP-1879: Multiple types can be suitable for publishing. So use array
     * instead single element. Also better naming.
     */
    Set<String> getSectionRootTypes();

    Set<String> getSectionTypes();

    void notifyEvent(String eventId, Map<String, Serializable> properties,
            String comment, String category, DocumentModel dm)
            throws ClientException;

    @WebRemote
    // XXX shouldn't be here : Seam remoting bug
    String processRemoteSelectRowEvent(String docRef, Boolean selection)
            throws ClientException;

    /**
     * Returns the list of available web actions for the currently selected
     * Documents inside a section.
     *
     * @return the WebAction list
     */
    List<Action> getActionsForSectionSelection();

    /**
     * Returns selected sections.
     */
    List<DocumentModelTreeNode> getSelectedSections();

    /**
     * Checks if a document is already published in a section.
     *
     * @deprecated Unused externally, will be moved to protected
     * @throws ClientException
     */
    @Deprecated
    boolean isAlreadyPublishedInSection(DocumentModel doc, DocumentModel section)
            throws ClientException;

    /**
     * Returns true if authenticated user has all permissions on document.
     *
     * @throws ClientException
     */
    boolean isReviewer(DocumentModel dm) throws ClientException;

    // XXX annotations shouldn't be there but this interface used as Local
    // interface because of Seam remoting bug.
    @Destroy
    void destroy();
}
