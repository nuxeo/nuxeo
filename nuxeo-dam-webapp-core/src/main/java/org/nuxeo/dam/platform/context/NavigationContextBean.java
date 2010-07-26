/*
 * (C) Copyright 2006-2009 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     Nuxeo
 */

package org.nuxeo.dam.platform.context;

import java.io.Serializable;
import java.util.List;

import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.Factory;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Install;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.contexts.Context;
import org.jboss.seam.contexts.Contexts;
import org.nuxeo.dam.webapp.contentbrowser.DocumentActions;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.PagedDocumentsProvider;
import org.nuxeo.ecm.core.api.SortInfo;
import org.nuxeo.ecm.core.api.VersionModel;
import org.nuxeo.ecm.core.api.repository.RepositoryManager;
import org.nuxeo.ecm.platform.ui.web.api.NavigationContext;
import org.nuxeo.ecm.platform.ui.web.api.UserAction;
import org.nuxeo.ecm.platform.ui.web.pagination.ResultsProviderFarmUserException;
import org.nuxeo.ecm.platform.ui.web.pathelements.PathElement;
import org.nuxeo.ecm.platform.util.RepositoryLocation;
import org.nuxeo.ecm.webapp.delegate.DocumentManagerBusinessDelegate;
import org.nuxeo.ecm.webapp.helpers.EventManager;

import static org.jboss.seam.annotations.Install.FRAMEWORK;

@Name("navigationContext")
@Scope(ScopeType.CONVERSATION)
@Install(precedence = FRAMEWORK)
public class NavigationContextBean implements NavigationContext, Serializable {

    private static final long serialVersionUID = 1L;

    @In(create = true)
    protected transient RepositoryManager repositoryManager;

    @In(create = true, required = false)
    protected transient CoreSession documentManager;

    @In
    protected transient Context conversationContext;

    @In(create = true)
    protected transient DocumentActions documentActions;

    public DocumentModel factoryChangeableDocument() {
        // TODO Auto-generated method stub
        return null;
    }

    public DocumentModel factoryCurrentContentRoot() {
        // TODO Auto-generated method stub
        return null;
    }

    @Factory(value = "currentDocument", scope = ScopeType.EVENT)
    public DocumentModel factoryCurrentDocument() {
        return getCurrentDocument();
    }

    public DocumentModelList factoryCurrentDocumentChildren()
            throws ClientException {
        // TODO Auto-generated method stub
        return null;
    }

    public DocumentModel factoryCurrentDomain() {
        // TODO Auto-generated method stub
        return null;
    }

    public RepositoryLocation factoryCurrentServerLocation() {
        // TODO Auto-generated method stub
        return null;
    }

    public DocumentModel factoryCurrentSuperSpace() throws ClientException {
        // TODO Auto-generated method stub
        return null;
    }

    public DocumentModel factoryCurrentWorkspace() {
        // TODO Auto-generated method stub
        return null;
    }

    public String getActionResult(DocumentModel doc, UserAction action)
            throws ClientException {
        // TODO Auto-generated method stub
        return null;
    }

    public DocumentModel getChangeableDocument() {
        // TODO Auto-generated method stub
        return null;
    }

    public DocumentModel getCurrentContentRoot() {
        // TODO Auto-generated method stub
        return null;
    }

    public DocumentModel getCurrentDocument() {
        return documentActions.getCurrentSelection();
    }

    public DocumentModelList getCurrentDocumentChildren()
            throws ClientException {
        // TODO Auto-generated method stub
        return null;
    }

    public DocumentModelList getCurrentDocumentChildrenPage()
            throws ClientException {
        // TODO Auto-generated method stub
        return null;
    }

    public String getCurrentDocumentFullUrl() {
        // TODO Auto-generated method stub
        return null;
    }

    public String getCurrentDocumentUrl() {
        // TODO Auto-generated method stub
        return null;
    }

    public DocumentModel getCurrentDomain() {
        // TODO Auto-generated method stub
        return null;
    }

    public String getCurrentDomainPath() throws ClientException {
        // TODO Auto-generated method stub
        return null;
    }

    public DocumentModelList getCurrentPath() throws ClientException {
        // TODO Auto-generated method stub
        return null;
    }

    public List<PathElement> getCurrentPathList() throws ClientException {
        // TODO Auto-generated method stub
        return null;
    }

    public RepositoryLocation getCurrentServerLocation() {
        // TODO Auto-generated method stub
        return null;
    }

    public DocumentModel getCurrentSuperSpace() throws ClientException {
        // TODO Auto-generated method stub
        return null;
    }

    public DocumentModel getCurrentWorkspace() {
        // TODO Auto-generated method stub
        return null;
    }

    public CoreSession getOrCreateDocumentManager() throws ClientException {
        if (documentManager != null) {
            return documentManager;
        }

        DocumentManagerBusinessDelegate documentManagerBD = (DocumentManagerBusinessDelegate) Contexts.lookupInStatefulContexts("documentManager");

        if (documentManagerBD == null) {
            // this is the first time we select the location, create a
            // DocumentManagerBusinessDelegate instance
            documentManagerBD = new DocumentManagerBusinessDelegate();
            conversationContext.set("documentManager", documentManagerBD);
        }
        RepositoryLocation repLoc = new RepositoryLocation(
                repositoryManager.getRepositories().iterator().next().getName());

        documentManager = documentManagerBD.getDocumentManager(repLoc);
        return documentManager;
    }

    public RepositoryLocation getSelectedServerLocation() {
        // TODO Auto-generated method stub
        return null;
    }

    public String goBack() throws ClientException {
        // TODO Auto-generated method stub
        return null;
    }

    public String goHome() {
        EventManager.raiseEventsOnGoingHome();
        return "home";
    }

    public void init() {
        // TODO Auto-generated method stub
    }

    public void invalidateChildrenProvider() {
        // TODO Auto-generated method stub
    }

    public void invalidateCurrentDocument() throws ClientException {
        // TODO Auto-generated method stub
    }

    public String navigateTo(RepositoryLocation serverLocation,
            DocumentRef docRef) throws ClientException {
        // TODO Auto-generated method stub
        return null;
    }

    public String navigateToDocument(DocumentModel doc) throws ClientException {
        // TODO Auto-generated method stub
        return null;
    }

    public String navigateToDocument(DocumentModel doc, String viewId)
            throws ClientException {
        // TODO Auto-generated method stub
        return null;
    }

    public String navigateToDocument(DocumentModel docModel,
            VersionModel versionModel) throws ClientException {
        // TODO Auto-generated method stub
        return null;
    }

    public String navigateToDocumentWithView(DocumentModel doc, String viewId)
            throws ClientException {
        // TODO Auto-generated method stub
        return null;
    }

    public String navigateToId(String documentId) throws ClientException {
        // TODO Auto-generated method stub
        return null;
    }

    public String navigateToRef(DocumentRef docRef) throws ClientException {
        // TODO Auto-generated method stub
        return null;
    }

    public String navigateToURL() throws ClientException {
        // TODO Auto-generated method stub
        return null;
    }

    public String navigateToURL(String documentUrl) throws ClientException {
        // TODO Auto-generated method stub
        return null;
    }

    public void resetCurrentContext() {
        // TODO Auto-generated method stub
    }

    public void resetCurrentDocumentChildrenCache(DocumentModel targetDoc) {
        // TODO Auto-generated method stub
    }

    public void saveCurrentDocument() throws ClientException {
        // TODO Auto-generated method stub
    }

    public void selectionChanged() {
        // TODO Auto-generated method stub
    }

    public void setChangeableDocument(DocumentModel changeableDocument) {
        // TODO Auto-generated method stub
    }

    public void setCurrentContentRoot(DocumentModel currentContentRoot) {
        // TODO Auto-generated method stub
    }

    public void setCurrentDocument(DocumentModel documentModel)
            throws ClientException {
        // TODO Auto-generated method stub
    }

    public void setCurrentDomain(DocumentModel currentDomain)
            throws ClientException {
        // TODO Auto-generated method stub
    }

    public void setCurrentResultsProvider(PagedDocumentsProvider resultsProvider) {
        // TODO Auto-generated method stub
    }

    public void setCurrentServerLocation(RepositoryLocation serverLocation)
            throws ClientException {
        // TODO Auto-generated method stub
    }

    public void updateDocumentContext(DocumentModel doc) throws ClientException {
        // TODO Auto-generated method stub
    }

    public PagedDocumentsProvider getResultsProvider(String name)
            throws ClientException, ResultsProviderFarmUserException {
        // TODO Auto-generated method stub
        return null;
    }

    public PagedDocumentsProvider getResultsProvider(String name,
            SortInfo sortInfo) throws ClientException,
            ResultsProviderFarmUserException {
        // TODO Auto-generated method stub
        return null;
    }

}
