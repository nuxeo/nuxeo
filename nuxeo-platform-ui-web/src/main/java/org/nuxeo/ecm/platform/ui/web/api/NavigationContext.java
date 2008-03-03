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
import org.nuxeo.ecm.core.api.PagedDocumentsProvider;
import org.nuxeo.ecm.core.api.VersionModel;
import org.nuxeo.ecm.platform.ui.web.pathelements.PathElement;
import org.nuxeo.ecm.platform.util.RepositoryLocation;

/**
 * Stateful Seam component.
 *
 * <ul>
 * <li> manages the navigation context variables
 * <li> outjects them for compatibility
 * <li> provides getters and setters for navigation context variables (ie : hide
 * what Seam scope is used for that)
 * <li> provides basic navigation features by leveraging Core API + Distributed
 * cache + internal cache
 * </ul>
 *
 * This Seam component should ideally serve only DocumentModel, lists of DMs and
 * Trees of DMs: no UI related structure.
 */
@Remote
public interface NavigationContext extends ResultsProviderFarm {

    /**
     * Callback for component initialization.
     *
     */
    void init();

    /**
     *
     * @return current repository location or null if no server is connected
     */
    RepositoryLocation getCurrentServerLocation();

    /**
     *
     * @return current selected repository location or null
     * @deprecated use getCurrentServerLocation instead
     */
    @Deprecated
    RepositoryLocation getSelectedServerLocation();

    /**
     * Current Domain, <strong>if user has read permission on it</strong>.
     *
     * Use @link{getCurrentDomainPath} if you are in a situation where
     * it is not guaranteed that the user has read permission on the domain.
     *
     * @return the current domain.
     */
    DocumentModel getCurrentDomain();

    /**
     * Find the path to current domain.
     * <p>
     * This method tries hard to always returns an answer.
     * If no current domain has been selected, then it will choose one.
     * </p>
     *
     * @return the path
     */
    String getCurrentDomainPath() throws ClientException;

    void setCurrentDomain(DocumentModel currentDomain) throws ClientException;

    /**
     * Current Document other than Domain or Workspace.
     *
     * @return the current document.
     */
    DocumentModel getCurrentDocument();

    /**
     * Returns the currentSuperSpace (Section, Workspace...).
     *
     * Uses SuperSpace facet for that.
     *
     * @return
     */
    DocumentModel getCurrentSuperSpace() throws ClientException;

    void setCurrentDocument(DocumentModel documentModel) throws ClientException;

    /**
     * This is used for documents about to be created, which are not yet
     * persisted thus not all their fields might be valid (like its reference).
     *
     * @return
     */
    DocumentModel getChangeableDocument();

    void setChangeableDocument(DocumentModel changeableDocument);

    /**
     * Saves the current copy of the document to server.
     *
     * @throws ClientException
     */
    void saveCurrentDocument() throws ClientException;

    /**
     * Retrieves the documents contained in the current parent.
     */
    DocumentModelList getCurrentDocumentChildren() throws ClientException;

    /**
     * @return list of children for the current document composing the current
     *         page
     * @throws ClientException
     */
    DocumentModelList getCurrentDocumentChildrenPage() throws ClientException;

    /**
     * @throws ClientException
     *
     */
    List<PathElement> getCurrentPathList() throws ClientException;

    /**
     * Returns a list of the DocumentsModels (to build the BreadCrumb for
     * example).
     *
     * @return DocumentModelList
     */
    DocumentModelList getCurrentPath() throws ClientException;

    /**
     *
     * @return the URL that can be used to refer the current document from
     *         outside current context
     */
    String getCurrentDocumentUrl();

    /**
     *
     * @return the URL that can be used to refer the current document from
     *         outside current context
     */
    String getCurrentDocumentFullUrl();

    /**
     * Updates the current document in the (session) context and resets the
     * other common structures (breadcrumbs, etc).
     *
     * @param doc DocumentModel that will be set as current document
     * @throws ClientException
     */
    void updateDocumentContext(DocumentModel doc) throws ClientException;

    /**
     * Resets all context variables.
     *
     */
    void resetCurrentContext();

    /**
     * Performs context updates and returns the view associated with the
     * document and action passed.
     *
     * @param doc
     * @param action
     * @return document view associated with the result
     * @throws ClientException
     */
    String getActionResult(DocumentModel doc, UserAction action)
            throws ClientException;

    String goHome();

    String goBack() throws ClientException;

    /**
     * Updates the context and returns the view for the given document id.
     */
    String navigateToId(String documentId) throws ClientException;

    /**
     * Updates the context and returns the view for the given document ref.
     *
     * @param docRef
     * @return
     * @throws ClientException
     */
    String navigateToRef(DocumentRef docRef) throws ClientException;

    /**
     * Updates the context and returns the view for the given document.
     *
     * @param doc
     * @return
     * @throws ClientException
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
     *
     * @param serverLocation
     * @param docRef
     * @return
     * @throws ClientException
     */
    String navigateTo(RepositoryLocation serverLocation, DocumentRef docRef)
            throws ClientException;

    /**
     * Initializes the context for the given refs and returns the default view
     * for the doc.
     *
     * @param documentUrl
     * @return
     * @throws ClientException
     */
    String navigateToURL(String documentUrl) throws ClientException;

    /**
     * Initializes the context for the given refs and returns the default view
     * for the doc.
     * <p>
     * Takes parameter String docRef from the Request.
     *
     * @return
     * @throws ClientException
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
     * @throws ClientException
     */
    void setCurrentServerLocation(RepositoryLocation serverLocation)
            throws ClientException;

    /**
     * Returns the current documentManager if any or create a new session to the
     * currently selected repository location.
     *
     * @throws ClientException
     */
    CoreSession getOrCreateDocumentManager() throws ClientException;

    /**
     * @param docModel
     * @param versionModel
     * @return
     */
    String navigateToDocument(DocumentModel docModel, VersionModel versionModel)
            throws ClientException;

    /**
     * Gets the currentDocument and puts it in Page context.
     *
     * @return
     */
    DocumentModel factoryCurrentDocument();

    /**
     * Gets the currentDomain and puts it in Page context.
     *
     * @return
     */
    DocumentModel factoryCurrentDomain();

    /**
     * Gets the currentWorkspace and puts it in Page context.
     *
     * @return
     */
    DocumentModel factoryCurrentWorkspace();

    /**
     * Gets the currentSuperSpace and puts it in Page context.
     *
     * @return
     */
    DocumentModel factoryCurrentSuperSpace() throws ClientException;

    /**
     * Gets the currentContentRootWorkspace and puts it in Page context.
     *
     * @return
     */
    DocumentModel factoryCurrentContentRoot();

    /**
     * Gets the current ServerLocation and puts it in Page Context.
     *
     * @return
     */
    RepositoryLocation factoryCurrentServerLocation();

    /**
     * Gets children of currentDocuments and put it in Page context.
     *
     * @return
     * @throws ClientException
     */
    DocumentModelList factoryCurrentDocumentChildren() throws ClientException;

    /**
     * Gets current document for edition.
     *
     * @return
     * @throws ClientException
     */
    DocumentModel factoryChangeableDocument();

    /**
     * Gets the current Content Root (Workspaces, Sections, Templates...).
     *
     * @return
     */
    DocumentModel getCurrentContentRoot();

    /**
     * Gets the current Workspace.
     *
     * @return
     */
    DocumentModel getCurrentWorkspace();

    /**
     * Sets the current ContentRoot.
     *
     * @param currentContentRoot
     */
    void setCurrentContentRoot(DocumentModel currentContentRoot);

    /**
     * Listener for the events that must trigger the reset of the list of
     * current document children.
     *
     * @param targetDoc
     */
    void resetCurrentDocumentChildrenCache(DocumentModel targetDoc);

    /**
     * Sets the current PagedDocumentsProvider object that will be used for
     * displaying current result (documents list).
     *
     * @param resultsProvider
     */
    void setCurrentResultsProvider(PagedDocumentsProvider resultsProvider);

    /**
     * Invalidates children provider (temporarily).
     */
    void invalidateChildrenProvider();

    /**
     * Will trigger reloading of current document data from the server.
     *
     * @throws ClientException
     */
    void invalidateCurrentDocument() throws ClientException;

}
