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

package org.nuxeo.ecm.webapp.context;

import static org.jboss.seam.ScopeType.CONVERSATION;
import static org.jboss.seam.ScopeType.EVENT;
import static org.jboss.seam.annotations.Install.FRAMEWORK;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import javax.annotation.security.PermitAll;
import javax.ejb.PostActivate;
import javax.ejb.Remove;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jboss.seam.Component;
import org.jboss.seam.annotations.Begin;
import org.jboss.seam.annotations.Create;
import org.jboss.seam.annotations.Destroy;
import org.jboss.seam.annotations.Factory;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Install;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.annotations.intercept.BypassInterceptors;
import org.jboss.seam.annotations.web.RequestParameter;
import org.jboss.seam.contexts.Context;
import org.jboss.seam.contexts.Contexts;
import org.jboss.seam.core.Events;
import org.nuxeo.common.utils.Path;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.ClientRuntimeException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentLocation;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.core.api.PagedDocumentsProvider;
import org.nuxeo.ecm.core.api.VersionModel;
import org.nuxeo.ecm.core.api.impl.DocumentModelListImpl;
import org.nuxeo.ecm.core.api.security.SecurityConstants;
import org.nuxeo.ecm.core.schema.FacetNames;
import org.nuxeo.ecm.core.schema.SchemaManager;
import org.nuxeo.ecm.core.search.api.client.querymodel.descriptor.QueryModelDescriptor;
import org.nuxeo.ecm.platform.types.Type;
import org.nuxeo.ecm.platform.types.adapter.TypeInfo;
import org.nuxeo.ecm.platform.ui.web.api.NavigationContext;
import org.nuxeo.ecm.platform.ui.web.api.UserAction;
import org.nuxeo.ecm.platform.ui.web.pathelements.ArchivedVersionsPathElement;
import org.nuxeo.ecm.platform.ui.web.pathelements.DocumentPathElement;
import org.nuxeo.ecm.platform.ui.web.pathelements.HiddenDocumentPathElement;
import org.nuxeo.ecm.platform.ui.web.pathelements.PathElement;
import org.nuxeo.ecm.platform.ui.web.pathelements.VersionDocumentPathElement;
import org.nuxeo.ecm.platform.ui.web.util.BadDocumentUriException;
import org.nuxeo.ecm.platform.ui.web.util.DocumentLocator;
import org.nuxeo.ecm.platform.ui.web.util.DocumentsListsUtils;
import org.nuxeo.ecm.platform.util.RepositoryLocation;
import org.nuxeo.ecm.webapp.action.TypesTool;
import org.nuxeo.ecm.webapp.contentbrowser.DocumentChildrenStdFarm;
import org.nuxeo.ecm.webapp.delegate.DocumentManagerBusinessDelegate;
import org.nuxeo.ecm.webapp.helpers.ApplicationControllerHelper;
import org.nuxeo.ecm.webapp.helpers.EventManager;
import org.nuxeo.ecm.webapp.helpers.EventNames;
import org.nuxeo.ecm.webapp.pagination.ResultsProvidersCache;
import org.nuxeo.ecm.webapp.querymodel.QueryModelActions;
import org.nuxeo.runtime.api.Framework;

/**
 * Implementation for the navigationContext component available on the session.
 */
@Name("navigationContext")
@Scope(CONVERSATION)
@Install(precedence = FRAMEWORK)
public class NavigationContextBean implements NavigationContextLocal,
        Serializable {

    private static final long serialVersionUID = -3708768859028774906L;

    private static final Log log = LogFactory.getLog(NavigationContextBean.class);

    // --------------------------------------------
    // fields managed by this class
    // These fields can be accessed by 2 ways
    // - simple getters
    // - via the context thanks to @Factory

    private DocumentModel currentDomain;

    private DocumentModel currentContentRoot;

    private DocumentModel currentWorkspace;

    protected DocumentModel currentDocument;

    protected DocumentModel currentSuperSpace;

    protected DocumentModelList currentDocumentChildren;

    protected List<DocumentModel> currentDocumentParents;

    // document model that is not persisted yet (useful for creation)
    private DocumentModel changeableDocument;

    private List<PathElement> parents;

    private SchemaManager schemaManager;

    @In(create = true, required = false)
    protected transient CoreSession documentManager;

    @Create
    @PostActivate
    public void init() {
        log.debug("<init> ");
        parents = null;
    }

    @BypassInterceptors
    public DocumentModel getCurrentDocument() {
        return currentDocument;
    }

    /**
     * Implementation details: the path to current domain is deduced from the
     * path of current document (hardcoded rule that it'd be the first
     * element).
     * <p>
     * If current document is null, then the first document found is used
     * instead.
     */
    public String getCurrentDomainPath() throws ClientException {
        if (currentDomain != null) {
            return currentDomain.getPathAsString();
        }
        Path path;
        if (currentDocument != null) {
            path = currentDocument.getPath();
        } else {
            // Find any document, and use its domain.
            DocumentModelList docs = documentManager.query(
                    "SELECT * FROM Document", 1);
            if (docs.size() < 1) {
                log.debug("Could not find a single document readable by current user.");
                return null;
            }
            path = docs.get(0).getPath();
        }
        if (path.segmentCount() > 0) {
            String[] segs = { path.segment(0) };
            return Path.createFromSegments(segs).toString();
        } else {
            return null;
        }
    }




    public void setCurrentDocument(DocumentModel documentModel)
            throws ClientException {

        if (!checkIfUpdateNeeded(currentDocument, documentModel)) {
            if (checkIfChangeableResetNeeded()) {
                setChangeableDocument(null);
            }
            return;
        }

        invalidateChildrenProvider();

        currentSuperSpace = null;
        currentDocument = documentModel;
        if (currentDocument == null) {
            currentDocumentParents = null;
        } else {
            DocumentRef ref = currentDocument.getRef();
            if (ref == null) {
                throw new ClientException(
                        "DocumentRef is null for currentDocument: "
                                + currentDocument.getName());
            }
            currentDocumentParents = documentManager.getParentDocuments(ref);
        }
        // update all depending variables
        updateContextVariables();
        resetCurrentPath();
        Contexts.getEventContext().remove("currentDocument");

        EventManager.raiseEventsOnDocumentSelected(currentDocument);
    }

    protected boolean isCreationEntered = false;

    /**
     * Changeable doc reset is needed when we're leaving from the document creation form
     * for navigating the parent document itself.
     * RestHelper is navigating the document when the user ask for creating a document.
     * The field isCreationEntered is used for tracking that event.
     *
     * @return
     * @throws ClientException
     */
    protected boolean checkIfChangeableResetNeeded() throws ClientException {
        if (changeableDocument == null) {
            return isCreationEntered = false;
        }

        if (changeableDocument.getRef() == null) {
            isCreationEntered = !isCreationEntered;
            return !isCreationEntered;
        }
        return isCreationEntered = false;
    }

    @BypassInterceptors
    public DocumentModel getChangeableDocument() {
        return changeableDocument;
    }

    public void setChangeableDocument(DocumentModel changeableDocument) {
        this.changeableDocument = changeableDocument;
        Contexts.getEventContext().set("changeableDocument", changeableDocument);
    }

    public DocumentModelList getCurrentPath() throws ClientException {
        DocumentModelList parentDocsList = new DocumentModelListImpl();

        List<DocumentModel> fromRoot = documentManager.getParentDocuments(currentDocument.getRef());
        // add in reverse order
        parentDocsList.addAll(fromRoot);
        Collections.reverse(parentDocsList);

        return parentDocsList;
    }

    public DocumentModel getCurrentSuperSpace() throws ClientException {
        if (currentSuperSpace == null && currentDocument != null) {
            if (currentDocument.hasFacet("SuperSpace")) {
                currentSuperSpace = currentDocument;
            } else if (documentManager != null) {
                currentSuperSpace = documentManager.getSuperSpace(currentDocument);
            }
        }
        return currentSuperSpace;
    }

    @Deprecated
    public DocumentModelList getCurrentDocumentChildren()
            throws ClientException {
        final String logPrefix = "<getCurrentDocumentChildren> ";

        if (documentManager == null) {
            log.error(logPrefix + "documentManager not initialized");
            return new DocumentModelListImpl();
        }

        QueryModelActions queryModelActions = (QueryModelActions) Component.getInstance("queryModelActions");
        QueryModelDescriptor queryDescriptor = queryModelActions.get(
                DocumentChildrenStdFarm.CHILDREN_BY_COREAPI).getDescriptor();
        String query = queryDescriptor.getQuery(new Object[] { currentDocument.getId() });

        currentDocumentChildren = documentManager.query(query);
        return currentDocumentChildren;
    }

    public void invalidateChildrenProvider() {
        ResultsProvidersCache resultsProvidersCache = (ResultsProvidersCache) Component.getInstance("resultsProvidersCache");
        resultsProvidersCache.invalidate(DocumentChildrenStdFarm.CHILDREN_BY_COREAPI);
    }

    public void invalidateCurrentDocument() throws ClientException {
        currentDocument = documentManager.getDocument(currentDocument.getRef());
        updateContextVariables();
    }

    @Deprecated
    public DocumentModelList getCurrentDocumentChildrenPage()
            throws ClientException {
        final String logPrefix = "<getCurrentDocumentChildrenPage> ";

        if (documentManager == null) {
            log.error(logPrefix + "documentManager not initialized");
            return new DocumentModelListImpl();
        }

        try {
            ResultsProvidersCache resultsProvidersCache = (ResultsProvidersCache) Component.getInstance("resultsProvidersCache");
            PagedDocumentsProvider resultsProvider = resultsProvidersCache.get(DocumentChildrenStdFarm.CHILDREN_BY_COREAPI);

            currentDocumentChildren = resultsProvider.getCurrentPage();
        } catch (Throwable t) {
            throw ClientException.wrap(t);
        }
        return currentDocumentChildren;
    }

    @BypassInterceptors
    public DocumentModel getCurrentDomain() {
        return currentDomain;
    }

    public void setCurrentDomain(DocumentModel domainDocModel)
            throws ClientException {
        if (!checkIfUpdateNeeded(currentDomain, domainDocModel)) {
            return;
        }

        currentDomain = domainDocModel;
        Contexts.getEventContext().remove("currentDomain");

        if (domainDocModel == null) {
            Events.instance().raiseEvent(EventNames.DOMAIN_SELECTION_CHANGED,
                    currentDomain);
            return;
        }

        if (currentDocument == null) {
            // setCurrentWorkspace(null);
            try {
                setCurrentDocument(null);
            } catch (ClientException e) {
                // TODO: more robust exception handling?
                log.error(e);
            }
        }

        // if we switched branch then realign currentDocument
        if (currentDocumentParents != null
                && !DocumentsListsUtils.isDocumentInList(domainDocModel,
                        currentDocumentParents)) {
            try {
                setCurrentDocument(domainDocModel);
            } catch (ClientException e) {
                // TODO: more robust exception handling?
                log.error(e);
            }
        }

        Events.instance().raiseEvent(EventNames.DOMAIN_SELECTION_CHANGED,
                currentDomain);
    }

    protected boolean checkIfUpdateNeeded(DocumentModel ctxDoc,
            DocumentModel newDoc) {
        if (ctxDoc == null && newDoc != null || ctxDoc != null
                && newDoc == null) {
            return true;
        }
        if (ctxDoc == null && newDoc == null) {
            return false;
        }
        try {
            return !ctxDoc.getCacheKey().equals(newDoc.getCacheKey());
        } catch (ClientException e) {
            throw new ClientRuntimeException(e);
        }
    }

    public void saveCurrentDocument() throws ClientException {
        if (null == currentDocument) {
            // cannot call saveDocument with null arg => nasty stateful bean
            // de-serialization error
            throw new IllegalStateException("null currentDocument");
        }
        currentDocument = documentManager.saveDocument(currentDocument);
        documentManager.save();
    }

    public List<PathElement> getCurrentPathList() throws ClientException {
        if (parents == null) {
            resetCurrentPath();
        }
        return parents;
    }

    protected ServerContextBean getServerLocator() {
        return (ServerContextBean) Component.getInstance("serverLocator");
    }

    public RepositoryLocation getCurrentServerLocation() {
        return getServerLocator().getCurrentServerLocation();
    }

    /**
     * @deprecated use getCurrentServerLocation() instead
     */
    @Deprecated
    public RepositoryLocation getSelectedServerLocation() {
        return getServerLocator().getCurrentServerLocation();
    }

    /**
     * Switches to a new server location by updating the context and updating
     * to the CoreSession (DocumentManager).
     */
    public void setCurrentServerLocation(RepositoryLocation serverLocation)
            throws ClientException {
        if (serverLocation == null) {
            log.warn("Setting ServerLocation to null, is this normal ?");
        }

        RepositoryLocation currentServerLocation = serverLocation;
        getServerLocator().setRepositoryLocation(serverLocation);
        resetCurrentContext();
        Contexts.getEventContext().set("currentServerLocation",
                currentServerLocation);

        // update the documentManager
        documentManager = null;
        documentManager = getOrCreateDocumentManager();
        Events.instance().raiseEvent(EventNames.LOCATION_SELECTION_CHANGED);

        DocumentModel rootDocument = documentManager.getRootDocument();
        if (documentManager.hasPermission(rootDocument.getRef(),
                SecurityConstants.READ)) {
            currentDocument = rootDocument;
            updateContextVariables();
        }
    }

    /**
     * Returns the current documentManager if any or create a new session to
     * the current location.
     */
    public CoreSession getOrCreateDocumentManager() throws ClientException {
        if (documentManager != null) {
            return documentManager;
        }
        // protect for unexpected wrong cast
        Object supposedDocumentManager = Contexts.lookupInStatefulContexts("documentManager");
        DocumentManagerBusinessDelegate documentManagerBD = null;
        if (supposedDocumentManager != null) {
            if (supposedDocumentManager instanceof DocumentManagerBusinessDelegate) {
                documentManagerBD = (DocumentManagerBusinessDelegate) supposedDocumentManager;
            } else {
                log.error("Found the documentManager being "
                        + supposedDocumentManager.getClass()
                        + " instead of DocumentManagerBusinessDelegate. This is wrong.");
            }
        }
        if (documentManagerBD == null) {
            // this is the first time we select the location, create a
            // DocumentManagerBusinessDelegate instance
            documentManagerBD = new DocumentManagerBusinessDelegate();
            Contexts.getConversationContext().set("documentManager",
                    documentManagerBD);
        }
        documentManager = documentManagerBD.getDocumentManager(getCurrentServerLocation());
        return documentManager;
    }

    @BypassInterceptors
    public DocumentModel getCurrentWorkspace() {
        return currentWorkspace;
    }

    // Factories to make navigation related data
    // available in the context

    @Factory(value = "currentDocument", scope = EVENT)
    public DocumentModel factoryCurrentDocument() {
        return currentDocument;
    }

    @Factory(value = "changeableDocument", scope = EVENT)
    public DocumentModel factoryChangeableDocument() {
        return changeableDocument;
    }

    @Factory(value = "currentDomain", scope = EVENT)
    public DocumentModel factoryCurrentDomain() {
        return currentDomain;
    }

    @Factory(value = "currentWorkspace", scope = EVENT)
    public DocumentModel factoryCurrentWorkspace() {
        return currentWorkspace;
    }

    @Factory(value = "currentContentRoot", scope = EVENT)
    public DocumentModel factoryCurrentContentRoot() {
        return currentContentRoot;
    }

    // @Factory(value = "currentServerLocation", scope = EVENT)
    public RepositoryLocation factoryCurrentServerLocation() {
        return getCurrentServerLocation();
    }

    @Factory(value = "currentDocumentChildren", scope = EVENT)
    public DocumentModelList factoryCurrentDocumentChildren()
            throws ClientException {
        return getCurrentDocumentChildren();
    }

    @Factory(value = "currentSuperSpace", scope = EVENT)
    public DocumentModel factoryCurrentSuperSpace() throws ClientException {
        return getCurrentSuperSpace();
    }

    public void setCurrentWorkspace(DocumentModel workspaceDocModel)
            throws ClientException {

        if (!checkIfUpdateNeeded(currentWorkspace, workspaceDocModel)) {
            return;
        }
        currentWorkspace = workspaceDocModel;

        if (workspaceDocModel == null) {
            return;
        }

        if (currentDocument == null) {
            setCurrentDocument(workspaceDocModel);
            return;
        }

        // if we switched branch then realign currentDocument
        if (currentDocumentParents != null
                && !DocumentsListsUtils.isDocumentInList(workspaceDocModel,
                        currentDocumentParents)) {
            setCurrentDocument(workspaceDocModel);
            return;
        }
    }

    public void updateDocumentContext(DocumentModel doc) throws ClientException {
        setCurrentDocument(doc);
    }

    /**
     * Updates variables according to hierarchy rules and to the new
     * currentDocument.
     */
    protected void updateContextVariables() throws ClientException {

        // XXX flush method is not implemented for Event context :)
        Contexts.getEventContext().set("currentDocument", currentDocument);

        if (currentDocument != null) {
            changeableDocument = currentDocument;
            Contexts.getEventContext().set("changeableDocument",
                    changeableDocument);
        } else {
            changeableDocument = currentDocument;
            Contexts.getEventContext().remove("changeableDocument");
        }

        if (currentDocument == null) {
            return;
        }

        // iterate in reverse list order to go down the tree
        // set all navigation variables according to docType
        // => update to tree
        if (currentDocumentParents == null) {
            currentDocumentParents = documentManager.getParentDocuments(currentDocument.getRef());
        }
        String docType;
        if (currentDocumentParents != null) {
            for (int i = currentDocumentParents.size() - 1; i >= 0; i--) {
                DocumentModel docModel = currentDocumentParents.get(i);
                docType = docModel.getType();

                if (docType != null && hasSuperType(docType, "Workspace")) {
                    setCurrentWorkspace(docModel);
                }

                if (null == docType || hasSuperType(docType, "WorkspaceRoot")
                        || hasSuperType(docType, "SectionRoot")) {
                    setCurrentContentRoot(docModel);
                }

                if (docType != null && hasSuperType(docType, "Domain")) {
                    setCurrentDomain(docModel);
                }
            }
        }
        // reinit lower tree
        docType = currentDocument.getType();
        if (docType.equals("Root")) {
            setCurrentDomain(null);
            setCurrentContentRoot(null);
            setCurrentWorkspace(null);
        } else if (hasSuperType(docType, "Domain")) {
            setCurrentDomain(currentDocument);
            setCurrentContentRoot(null);
            setCurrentWorkspace(null);
        } else if (hasSuperType(docType, "WorkspaceRoot")
                || hasSuperType(docType, "SectionRoot")) {
            setCurrentContentRoot(currentDocument);
            setCurrentWorkspace(null);
        } else if (hasSuperType(docType, "Workspace")) {
            setCurrentWorkspace(currentDocument);
        }
    }

    private SchemaManager getSchemaManager() throws Exception {
        if (schemaManager == null) {
            schemaManager = Framework.getService(SchemaManager.class);
            if (schemaManager == null) {
                throw new ClientException(
                        "Could not find SchemaManager service");
            }
        }
        return schemaManager;
    }

    private boolean hasSuperType(String targetDocType, String superType)
            throws ClientException {
        if (targetDocType == null) {
            return false;
        }
        try {
            Set<String> typeNames = getSchemaManager().getDocumentTypeNamesExtending(
                    superType);
            for (String type : typeNames) {
                if (type.equals(targetDocType)) {
                    return true;
                }
            }
            return false;
        } catch (Exception e) {
            throw new ClientException("Could not extending types", e);
        }
    }

    public void resetCurrentContext() {
        // flush event context
        Context eventContext = Contexts.getEventContext();
        eventContext.remove("currentDocument");
        eventContext.remove("changeableDocument");
        eventContext.remove("currentDocumentChildren");
        eventContext.remove("currentDomain");
        eventContext.remove("currentServerLocation");
        eventContext.remove("currentWorkspace");
        eventContext.remove("currentContentRoot");
        eventContext.remove("currentSuperSpace");
    }

    // XXX AT: we should let each action listener raise specific events
    // (edition) and decide what's the next view, let's just handle context
    // setting and redirection + this should be callable from templates i.e use
    // view as a string.
    public String getActionResult(DocumentModel doc, UserAction action)
            throws ClientException {

        TypesTool typesTool = (TypesTool) Component.getInstance("typesTool");

        // return displayDocument(doc, action.toString());
        final String logPrefix = "<getActionResult> ";

        if (doc == null) {
            return null;
        }
        if (UserAction.CREATE == action) {
            // the given document is a changeable document
            changeableDocument = doc;
            Contexts.getEventContext().set("changeableDocument", doc);
        } else {
            updateDocumentContext(doc);
        }

        final Type type = typesTool.getType(doc.getType());

        final String result;
        if (UserAction.VIEW == action) {
            assert currentDocument != null;
            EventManager.raiseEventsOnDocumentSelected(currentDocument);
            result = ApplicationControllerHelper.getPageOnSelectedDocumentType(type);
        } else if (UserAction.EDIT == action) {
            throw new UnsupportedOperationException("for action " + action);
        } else if (UserAction.AFTER_EDIT == action) {
            assert currentDocument != null;
            EventManager.raiseEventsOnDocumentChange(currentDocument);
            result = ApplicationControllerHelper.getPageOnEditedDocumentType(type);
        } else if (UserAction.CREATE == action) {
            EventManager.raiseEventsOnDocumentCreate(changeableDocument);
            result = ApplicationControllerHelper.getPageOnCreateDocumentType(type);
        } else if (UserAction.AFTER_CREATE == action) {
            assert currentDocument != null;
            EventManager.raiseEventsOnDocumentSelected(currentDocument);
            result = ApplicationControllerHelper.getPageOnCreatedDocumentType(type);
        } else if (UserAction.GO_HOME == action) {
            EventManager.raiseEventsOnGoingHome();
            result = "home";
        } else {
            log.error(logPrefix + "Bad action: " + action);
            result = null;
        }
        return result;
    }

    public String goHome() {
        resetCurrentContext();
        EventManager.raiseEventsOnGoingHome();
        return "home";
    }

    public String goBack() throws ClientException {
        if (currentDocument != null) {
            setChangeableDocument(null);
            return navigateToDocument(currentDocument);
        } else {
            // XXX AT: should return to currentServer page if set
            return goHome();
        }
    }

    public String navigateToId(String documentId) throws ClientException {
        if (documentManager == null) {
            throw new IllegalStateException("documentManager not initialized");
        }
        DocumentRef docRef = new IdRef(documentId);
        final DocumentModel doc = documentManager.getDocument(docRef);
        return navigateToDocument(doc, "view");
    }

    public String navigateToRef(DocumentRef docRef) throws ClientException {
        if (documentManager == null) {
            throw new IllegalStateException("documentManager not initialized");
        }

        final DocumentModel doc = documentManager.getDocument(docRef);

        return navigateToDocument(doc, "view");
    }

    public String navigateToDocument(DocumentModel doc) throws ClientException {
        return navigateToDocument(doc, "view");
    }

    /**
     * Updates context with given document and returns given view.
     * <p>
     * The view is supposed to be set on the document type information. If such
     * a view id is not available for the type, use its default vieW.
     */
    public String navigateToDocument(DocumentModel doc, String viewId)
            throws ClientException {
        if (doc != null) {
            updateDocumentContext(doc);
        }
        assert currentDocument != null;
        TypeInfo typeInfo = currentDocument.getAdapter(TypeInfo.class);
        String chosenView = null;
        if (typeInfo != null) {
            String defaultView = typeInfo.getDefaultView();
            // hardcoded default views
            if ("view".equals(viewId)) {
                chosenView = defaultView;
            } else if ("create".equals(viewId)) {
                chosenView = typeInfo.getCreateView();
            } else if ("edit".equals(viewId)) {
                chosenView = typeInfo.getEditView();
            } else {
                chosenView = typeInfo.getView(viewId);
            }
            if (chosenView == null) {
                chosenView = defaultView;
            }
        }
        return chosenView;
    }

    /**
     * Alias to
     * <code>navigateToDocument(DocumentModel doc, String viewId)</code> so
     * that JSF EL sees no ambiguity)
     * <p>
     * The view is supposed to be set on the document type information. If such
     * a view id is not available for the type, use its default vieW.
     */
    public String navigateToDocumentWithView(DocumentModel doc, String viewId)
            throws ClientException {
        return navigateToDocument(doc, viewId);
    }

    public String navigateToDocument(DocumentModel docModel,
            VersionModel versionModel) throws ClientException {
        DocumentModel docVersion = documentManager.getDocumentWithVersion(
                docModel.getRef(), versionModel);
        return navigateToDocument(docVersion);
    }

    public void selectionChanged() {
        final String logPrefix = "<selectionChanged> ";
        try {
            resetCurrentPath();
        } catch (ClientException e) {
            log.error(logPrefix + "error reseting current path", e);
        }
    }

    /**
     * @see NavigationContext#getCurrentDocumentUrl()
     */
    public String getCurrentDocumentUrl() {
        if (null == currentDocument) {
            log.error("current document is null");
            return null;
        }
        return DocumentLocator.getDocumentUrl(getCurrentServerLocation(),
                currentDocument.getRef());
    }

    /**
     * @see NavigationContext#getCurrentDocumentFullUrl()
     */
    public String getCurrentDocumentFullUrl() {
        if (null == currentDocument) {
            log.error("current document is null");
            return null;
        }
        return DocumentLocator.getFullDocumentUrl(getCurrentServerLocation(),
                currentDocument.getRef());
    }

    // start a new conversation if needed, join main if possible
    @Begin(id = "#{conversationIdGenerator.currentOrNewMainConversationId}", join = true)
    public String navigateTo(RepositoryLocation serverLocation,
            DocumentRef docRef) throws ClientException {
        // re-connect only if there is another repository specified
        if (!serverLocation.equals(getCurrentServerLocation())) {
            setCurrentServerLocation(serverLocation);
        }
        return navigateToRef(docRef);
    }

    // start a new conversation if needed, join main if possible
    @Begin(id = "#{conversationIdGenerator.currentOrNewMainConversationId}", join = true)
    public String navigateToURL(String documentUrl) throws ClientException {
        final DocumentLocation docLoc;
        try {
            docLoc = DocumentLocator.parseDocRef(documentUrl);
        } catch (BadDocumentUriException e) {
            log.error("Cannot get document ref from uri " + documentUrl + ". "
                    + e.getMessage(), e);
            return null;
        }
        final DocumentRef docRef = docLoc.getDocRef();
        RepositoryLocation repLoc = new RepositoryLocation(
                docLoc.getServerName());
        return navigateTo(repLoc, docRef);
    }

    @RequestParameter
    String docRef;

    /**
     * @see NavigationContext#navigateToURL()
     */
    @Begin(id = "#{conversationIdGenerator.currentOrNewMainConversationId}", join = true)
    public String navigateToURL() throws ClientException {
        if (docRef == null) {
            return null;
        }
        return navigateToURL(docRef);
    }

    @Destroy
    @Remove
    @PermitAll
    public void destroy() {
        log.debug("<destroy> ");
    }

    protected void resetCurrentPath() throws ClientException {
        final String logPrefix = "<resetCurrentPath> ";

        parents = new ArrayList<PathElement>();

        if (null == documentManager) {
            log.error(logPrefix + "documentManager not initialized");
            return;
        }

        if (currentDocument != null) {
            if (currentDocument.isVersion()) {
                DocumentModel sourceDocument = documentManager.getSourceDocument(currentDocument.getRef());

                List<DocumentModel> parentList = documentManager.getParentDocuments(sourceDocument.getRef());
                for (DocumentModel docModel : parentList) {
                    parents.add(getDocumentPathElement(docModel));
                }

                parents.add(new ArchivedVersionsPathElement(sourceDocument));
                parents.add(new VersionDocumentPathElement(currentDocument));
            } else {
                if (currentDocumentParents != null) {
                    for (DocumentModel docModel : currentDocumentParents) {
                        parents.add(getDocumentPathElement(docModel));
                    }
                }
            }
        }
    }

    protected PathElement getDocumentPathElement(DocumentModel doc) {
        if (doc != null && doc.hasFacet(FacetNames.HIDDEN_IN_NAVIGATION)) {
            return new HiddenDocumentPathElement(doc);
        }
        return new DocumentPathElement(doc);
    }

    @SuppressWarnings("unused")
    private void logDocWithTitle(String msg, DocumentModel doc) {
        if (null != doc) {
            try {
                log.debug(msg + " " + doc.getProperty("dublincore", "title"));
            } catch (ClientException e) {
                log.debug(msg + ", ERROR: " + e);
            }
        } else {
            log.debug(msg + " NULL DOC");
        }
    }

    public DocumentModel getCurrentContentRoot() {
        return currentContentRoot;
    }

    public void setCurrentContentRoot(DocumentModel crDocumentModel) {

        if (!checkIfUpdateNeeded(currentContentRoot, crDocumentModel)) {
            return;
        }
        currentContentRoot = crDocumentModel;
        Contexts.getEventContext().remove("currentContentRoot");

        if (crDocumentModel == null) {
            return;
        }

        if (currentDocument == null) {
            try {
                setCurrentDocument(null);
            } catch (ClientException e) {
                // TODO: more robust exception handling?
                log.error(e);
            }
            return;
        }

        // if we switched branch then realign currentDocument
        if (currentDocumentParents != null
                && !DocumentsListsUtils.isDocumentInList(crDocumentModel,
                        currentDocumentParents)) {
            try {
                setCurrentDocument(crDocumentModel);
            } catch (ClientException e) {
                // TODO: more robust exception handling?
                log.error(e);
            }
        }
    }

}
