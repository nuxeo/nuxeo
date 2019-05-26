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
 * $Id: JOOoConvertPluginImpl.java 18651 2007-05-13 20:28:53Z sfermigier $
 */

package org.nuxeo.ecm.platform.ui.web.api;

import java.util.List;

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
 * <li>manages the navigation context variables
 * <li>outjects them for compatibility
 * <li>provides getters and setters for navigation context variables (ie : hide what Seam scope is used for that)
 * <li>provides basic navigation features by leveraging Core API + Distributed cache + internal cache
 * </ul>
 * This Seam component should ideally serve only DocumentModel, lists of DMs and Trees of DMs: no UI related structure.
 */
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
     * @link{getCurrentDomainPath if you are in a situation where it is not guaranteed that the user has read permission
     *                            on the domain.
     * @return the current domain.
     */
    DocumentModel getCurrentDomain();

    /**
     * Finds the path to current domain.
     * <p>
     * This method tries hard to always returns an answer: the path to current domain is deduced from the path of
     * current document (assuming that it would be the first element of this path).
     * <p>
     * If current document is null, the same logic is applied to the first document found amongst all documents user has
     * access to.
     */
    String getCurrentDomainPath();

    void setCurrentDomain(DocumentModel currentDomain);

    /**
     * Current Document other than Domain or Workspace.
     */
    DocumentModel getCurrentDocument();

    /**
     * Returns the currentSuperSpace (Section, Workspace...). Uses SuperSpace facet for that.
     */
    DocumentModel getCurrentSuperSpace();

    void setCurrentDocument(DocumentModel documentModel);

    /**
     * This is used for documents about to be created, which are not yet persisted thus not all their fields might be
     * valid (like its reference).
     */
    DocumentModel getChangeableDocument();

    void setChangeableDocument(DocumentModel changeableDocument);

    /**
     * Saves the current copy of the document to server.
     */
    void saveCurrentDocument();

    List<PathElement> getCurrentPathList();

    /**
     * Returns a list of the DocumentsModels (to build the BreadCrumb for example).
     */
    DocumentModelList getCurrentPath();

    /**
     * @return the URL that can be used to refer the current document from outside current context
     */
    String getCurrentDocumentUrl();

    /**
     * @return the URL that can be used to refer the current document from outside current context
     */
    String getCurrentDocumentFullUrl();

    /**
     * Updates the current document in the (session) context and resets the other common structures (breadcrumbs, etc).
     *
     * @param doc DocumentModel that will be set as current document
     */
    void updateDocumentContext(DocumentModel doc);

    /**
     * Resets all context variables.
     */
    void resetCurrentContext();

    /**
     * Performs context updates and returns the view associated with the document and action passed.
     *
     * @return document view associated with the result
     */
    String getActionResult(DocumentModel doc, UserAction action);

    /**
     * go to the root server, the root document or to the dashboard if the latest document are not accessible.
     */
    String goHome();

    String goBack();

    /**
     * Updates the context and returns the view for the given document id.
     */
    String navigateToId(String documentId);

    /**
     * Updates the context and returns the view for the given document ref.
     */
    String navigateToRef(DocumentRef docRef);

    /**
     * Updates the context with given document and returns its default view.
     * <p>
     * The default view is configured on the UI document type definition.
     */
    String navigateToDocument(DocumentModel doc);

    /**
     * Initializes the context for the given doc and returns its given view.
     * <p>
     * If view is not found, use default view.
     *
     * @param doc
     * @param viewId for the document as defined in its type
     * @return navigation view for the document
     */
    String navigateToDocument(DocumentModel doc, String viewId);

    /**
     * Initializes the context for the given doc and returns its given view.
     * <p>
     * If view is not found, use default view set on the UI document type. This is an alias to
     * <code>navigateToDocument(DocumentModel doc, String viewId)</code>, to resolve ambiguity when method is invoked by
     * an EL expression.
     *
     * @param doc
     * @param viewId for the document as defined in its type
     * @return navigation view for the document
     */
    String navigateToDocumentWithView(DocumentModel doc, String viewId);

    /**
     * Initializes the context for the given refs and returns the default view for the doc.
     */
    String navigateTo(RepositoryLocation serverLocation, DocumentRef docRef);

    /**
     * Initializes the context for the given refs and returns the default view for the doc.
     */
    String navigateToURL(String documentUrl);

    /**
     * Initializes the context for the given refs and returns the default view for the doc.
     * <p>
     * Takes parameter String docRef from the Request.
     */
    String navigateToURL();

    // --- do not call these methods directly
    // --- they are listeners - called by Seam
    void selectionChanged();

    /**
     * Switch to a new server location by updating the context and updating to the CoreSession (aka the
     * 'documentManager' Seam component).
     *
     * @param serverLocation new server location to connect to
     */
    void setCurrentServerLocation(RepositoryLocation serverLocation);

    /**
     * Returns the current documentManager if any or create a new session to the currently selected repository location.
     */
    CoreSession getOrCreateDocumentManager();

    String navigateToDocument(DocumentModel docModel, VersionModel versionModel);

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
    DocumentModel factoryCurrentSuperSpace();

    /**
     * Gets the currentContentRootWorkspace and puts it in Page context.
     */
    DocumentModel factoryCurrentContentRoot();

    /**
     * Gets the current ServerLocation and puts it in Page Context.
     */
    RepositoryLocation factoryCurrentServerLocation();

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
     * Will trigger reloading of current document data from the server.
     */
    void invalidateCurrentDocument();

}
