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

package org.nuxeo.ecm.platform.ui.web.api;

import java.util.List;

import javax.ejb.Remote;

import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.VersionModel;
import org.nuxeo.ecm.platform.ui.web.pathelements.PathElement;
import org.nuxeo.ecm.platform.util.RepositoryLocation;

/**
 * Stateful Seam component.
 * <ul>
 * <li> manages the navigation context variables
 * <li> outjects them for compatibility
 * <li> provides getters and setters for navigation context variables (ie :
 * hide what Seam scope is used for that)
 * <li> provides basic navigation features by leveraging Core API + Distributed
 * cache + internal cache
 * </ul>
 * This Seam component should ideally serve only DocumentModel, lists of DMs
 * and Trees of DMs: no UI related structure.
 */
@Remote
public interface NavigationContext {

    /**
     * Callback for component initialization.
     */
    void init();

    /**
     * @return current repository location or null if no server is connected
     */
    RepositoryLocation getCurrentServerLocation();

    /**
     * @return current selected repository location or null
     * @deprecated use getCurrentServerLocation instead
     */
    @Deprecated
    RepositoryLocation getSelectedServerLocation();

    /**
     * Current Domain, <strong>if user has read permission on it</strong>. Use
     *
     * @link{getCurrentDomainPath} if you are in a situation where it is not
     *                             guaranteed that the user has read permission
     *                             on the domain.
     * @return the current domain.
     */
    DocumentModel getCurrentDomain();

    /**
     * Finds the path to current domain.
     * <p>
     * This method tries hard to always returns an answer. If no current domain
     * has been selected, then it will choose one.
     *
     * @return the path
     */
    String getCurrentDomainPath() throws ClientException;

    void setCurrentDomain(DocumentModel currentDomain) throws ClientException;

    /**
     * Current Document other than Domain or Workspace.
     */
    DocumentModel getCurrentDocument();

    /**
     * Returns the currentSuperSpace (Section, Workspace...). Uses SuperSpace
     * facet for that.
     */
    DocumentModel getCurrentSuperSpace() throws ClientException;

    void setCurrentDocument(DocumentModel documentModel) throws ClientException;

    /**
     * This is used for documents about to be created, which are not yet
     * persisted thus not all their fields might be valid (like its reference).
     */
    DocumentModel getChangeableDocument();

    void setChangeableDocument(DocumentModel changeableDocument);

    /**
     * Saves the current copy of the document to server.
     */
    void saveCurrentDocument() throws ClientException;

    /**
     * Retrieves the documents contained in the current parent.
     *
     * @deprecated this method is not scalable, all the documents will be in
     *             memory
     */
    @Deprecated
    DocumentModelList getCurrentDocumentChildren() throws ClientException;

    /**
     * @return list of children for the current document composing the current
     *         page
     */
    DocumentModelList getCurrentDocumentChildrenPage() throws ClientException;

    List<PathElement> getCurrentPathList() throws ClientException;

    /**
     * Returns a list of the DocumentsModels (to build the BreadCrumb for
     * example).
     */
    DocumentModelList getCurrentPath() throws ClientException;

    /**
     * @return the URL that can be used to refer the current document from
     *         outside current context
     */
    String getCurrentDocumentUrl();

    /**
     * @return the URL that can be used to refer the current document from
     *         outside current context
     */
    String getCurrentDocumentFullUrl();

    /**
     * Updates the current document in the (session) context and resets the
     * other common structures (breadcrumbs, etc).
     *
     * @param doc DocumentModel that will be set as current document
     */
    void updateDocumentContext(DocumentModel doc) throws ClientException;

    /**
     * Resets all context variables.
     */
    void resetCurrentContext();

    /**
     * Performs context updates and returns the view associated with the
     * document and action passed.
     *
     * @return document view associated with the result
     */
    String getActionResult(DocumentModel doc, UserAction action)
            throws ClientException;

    /**
     * go to the root server, the root document or to the dashboard if the
     * latest document are not accessible.
     */
    String goHome();

    String goBack() throws ClientException;

    /**
     * Updates the context and returns the view for the given document id.
     */
    String navigateToId(String documentId) throws ClientException;

    /**
     * Updates the context and returns the view for the given document ref.
     */
    String navigateToRef(DocumentRef docRef) throws ClientException;

    /**
     * Updates the context and returns the view for the given document.
     */
    String navigateToDocument(DocumentModel doc) throws ClientException;

    /**
     * Initializes the context for the given doc and returns its given view.
     * <p>
     * If view is not found, use default view.
     *
     * @param doc
     * @param viewId for the document as defined in its type
     * @return navigation view for the document
     * @throws ClientException
     */
    String navigateToDocument(DocumentModel doc, String viewId)
            throws ClientException;

    /**
     * Initializes the context for the given doc and returns its given view.
     * <p>
     * If view is not found, use default view. Alias to resolve polymorphism
     * ambiguity when called from JSF EL.
     *
     * @param doc
     * @param viewId for the document as defined in its type
     * @return navigation view for the document
     * @throws ClientException
     */
    String navigateToDocumentWithView(DocumentModel doc, String viewId)
            throws ClientException;

    /**
     * Initializes the context for the given refs and returns the default view
     * for the doc.
     */
    String navigateTo(RepositoryLocation serverLocation, DocumentRef docRef)
            throws ClientException;

    /**
     * Initializes the context for the given refs and returns the default view
     * for the doc.
     */
    String navigateToURL(String documentUrl) throws ClientException;

    /**
     * Initializes the context for the given refs and returns the default view
     * for the doc.
     * <p>
     * Takes parameter String docRef from the Request.
     */
    String navigateToURL() throws ClientException;

    // --- do not call these methods directly
    // --- they are listeners - called by Seam
    void selectionChanged();

    /**
     * Switch to a new server location by updating the context and updating to
     * the CoreSession (aka the 'documentManager' Seam component).
     *
     * @param serverLocation new server location to connect to
     */
    void setCurrentServerLocation(RepositoryLocation serverLocation)
            throws ClientException;

    /**
     * Returns the current documentManager if any or create a new session to
     * the currently selected repository location.
     */
    CoreSession getOrCreateDocumentManager() throws ClientException;

    String navigateToDocument(DocumentModel docModel, VersionModel versionModel)
            throws ClientException;

    /**
     * Gets the currentDocument and puts it in Page context.
     */
    DocumentModel factoryCurrentDocument();

    /**
     * Gets the currentDomain and puts it in Page context.
     */
    DocumentModel factoryCurrentDomain();

    /**
     * Gets the currentWorkspace and puts it in Page context.
     */
    DocumentModel factoryCurrentWorkspace();

    /**
     * Gets the currentSuperSpace and puts it in Page context.
     */
    DocumentModel factoryCurrentSuperSpace() throws ClientException;

    /**
     * Gets the currentContentRootWorkspace and puts it in Page context.
     */
    DocumentModel factoryCurrentContentRoot();

    /**
     * Gets the current ServerLocation and puts it in Page Context.
     */
    RepositoryLocation factoryCurrentServerLocation();

    /**
     * Gets children of currentDocuments and put it in Page context.
     */
    DocumentModelList factoryCurrentDocumentChildren() throws ClientException;

    /**
     * Gets current document for edition.
     */
    DocumentModel factoryChangeableDocument();

    /**
     * Gets the current Content Root (Workspaces, Sections, Templates...).
     */
    DocumentModel getCurrentContentRoot();

    /**
     * Gets the current Workspace.
     */
    DocumentModel getCurrentWorkspace();

    /**
     * Sets the current ContentRoot.
     */
    void setCurrentContentRoot(DocumentModel currentContentRoot);

    /**
     * Invalidates children provider (temporarily).
     */
    void invalidateChildrenProvider();

    /**
     * Will trigger reloading of current document data from the server.
     */
    void invalidateCurrentDocument() throws ClientException;

}
