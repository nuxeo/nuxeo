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
import static org.nuxeo.ecm.webapp.helpers.EventNames.NAVIGATE_TO_DOCUMENT;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jboss.seam.Component;
import org.jboss.seam.annotations.Begin;
import org.jboss.seam.annotations.Create;
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
import org.nuxeo.ecm.core.api.VersionModel;
import org.nuxeo.ecm.core.api.impl.DocumentModelListImpl;
import org.nuxeo.ecm.core.api.security.SecurityConstants;
import org.nuxeo.ecm.core.schema.FacetNames;
import org.nuxeo.ecm.core.schema.SchemaManager;
import org.nuxeo.ecm.core.schema.types.Schema;
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
import org.nuxeo.ecm.webapp.delegate.DocumentManagerBusinessDelegate;
import org.nuxeo.ecm.webapp.helpers.ApplicationControllerHelper;
import org.nuxeo.ecm.webapp.helpers.EventManager;
import org.nuxeo.ecm.webapp.helpers.EventNames;
import org.nuxeo.runtime.api.Framework;

/**
 * Implementation for the navigationContext component available on the session.
 */
@Name("navigationContext")
@Scope(CONVERSATION)
@Install(precedence = FRAMEWORK)
public class NavigationContextBean implements NavigationContext, Serializable {

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

    // document model that is not persisted yet (used for creation)
    private DocumentModel changeableDocument;

    private List<PathElement> parents;

    private SchemaManager schemaManager;

    @In(create = true, required = false)
    protected transient CoreSession documentManager;

    @Create
    public void init() {
        parents = null;
    }

    @BypassInterceptors
    public DocumentModel getCurrentDocument() {
        return currentDocument;
    }

    public String getCurrentDomainPath() throws ClientException {
        if (currentDomain != null) {
            return currentDomain.getPathAsString();
        }
        Path path;
        if (currentDocument != null) {
            path = currentDocument.getPath();
        } else {
            // Find any document, and lookup its domain.
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
        if (log.isDebugEnabled()) {
            log.debug("Setting current document to " + documentModel);
        }

        if (!checkIfUpdateNeeded(currentDocument, documentModel)) {
            if (log.isDebugEnabled()) {
                log.debug(String.format(
                        "Current document already set to %s => give up updates",
                        documentModel));
            }
            return;
        }

        currentSuperSpace = null;
        currentDocument = documentModel;
        // update all depending variables
        updateContextVariables();
        resetCurrentPath();
        Contexts.getEventContext().remove("currentDocument");

        EventManager.raiseEventsOnDocumentSelected(currentDocument);

        if (log.isDebugEnabled()) {
            log.debug("Current document set to: " + changeableDocument);
        }
    }

    @BypassInterceptors
    public DocumentModel getChangeableDocument() {
        return changeableDocument;
    }

    public void setChangeableDocument(DocumentModel changeableDocument) {
        if (log.isDebugEnabled()) {
            log.debug("Setting changeable document to: " + changeableDocument);
        }
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
            if (currentDocument.hasFacet(FacetNames.SUPER_SPACE)) {
                currentSuperSpace = currentDocument;
            } else if (documentManager != null) {
                currentSuperSpace = documentManager.getSuperSpace(currentDocument);
            }
        }
        return currentSuperSpace;
    }

    public void invalidateCurrentDocument() throws ClientException {
        if (currentDocument != null) {
            currentDocument = documentManager.getDocument(currentDocument.getRef());
            updateContextVariables();
        }
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
            try {
                setCurrentDocument(null);
            } catch (ClientException e) {
                log.error(e, e);
            }
        }

        // if we switched branch then realign currentDocument
        if (currentDocumentParents != null
                && !DocumentsListsUtils.isDocumentInList(domainDocModel,
                        currentDocumentParents)) {
            try {
                setCurrentDocument(domainDocModel);
            } catch (ClientException e) {
                log.error(e, e);
            }
        }

        Events.instance().raiseEvent(EventNames.DOMAIN_SELECTION_CHANGED,
                currentDomain);
    }

    protected boolean checkIfUpdateNeeded(DocumentModel ctxDoc,
            DocumentModel newDoc) {
        if (log.isDebugEnabled()) {
            log.debug(String.format("Check if update needed: compare context "
                    + "doc '%s' to new doc '%s'", ctxDoc, newDoc));
        }
        if (ctxDoc == null && newDoc != null || ctxDoc != null
                && newDoc == null) {
            return true;
        }
        if (ctxDoc == null && newDoc == null) {
            return false;
        }
        try {
            if (log.isDebugEnabled()) {
                log.debug(String.format(
                        "Check if update needed: compare cache key on "
                                + "context doc '%s' with new doc '%s'",
                        ctxDoc.getCacheKey(), newDoc.getCacheKey()));
            }
            return !ctxDoc.getCacheKey().equals(newDoc.getCacheKey());
        } catch (ClientException e) {
            throw new ClientRuntimeException(e);
        }
    }

    public void saveCurrentDocument() throws ClientException {
        if (currentDocument == null) {
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

        // Don't flush changeable document with a null id (NXP-10732)
        if ((getChangeableDocument() != null)
                && (getChangeableDocument().getId() != null)) {
            setChangeableDocument(null);
        }

        if (currentDocument == null) {
            currentDocumentParents = null;
            return;
        }

        DocumentRef ref = currentDocument.getRef();
        if (ref == null) {
            throw new ClientException(
                    "DocumentRef is null for currentDocument: "
                            + currentDocument.getName());
        }
        // Recompute document parents
        currentDocumentParents = documentManager.getParentDocuments(ref);

        // iterate in reverse list order to go down the tree
        // set all navigation variables according to docType
        // => update to tree
        String docType;
        if (currentDocumentParents != null) {
            for (int i = currentDocumentParents.size() - 1; i >= 0; i--) {
                DocumentModel docModel = currentDocumentParents.get(i);
                docType = docModel.getType();

                if (docType != null && hasSuperType(docType, "Workspace")) {
                    setCurrentWorkspace(docModel);
                }

                if (docType == null || hasSuperType(docType, "WorkspaceRoot")
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

        // lazily recompute some fields
        currentSuperSpace = null;
        parents = null;
    }

    private boolean hasSuperType(String targetDocType, String superType)
            throws ClientException {
        SchemaManager schemaManager = Framework.getLocalService(SchemaManager.class);
        return schemaManager.hasSuperType(targetDocType, superType);
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

        if (doc == null) {
            return null;
        }
        if (UserAction.CREATE == action) {
            // the given document is a changeable document
            setChangeableDocument(doc);
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
            log.error(String.format("Unknown action '%s' for navigation on "
                    + "document '%s' with title '%s': ", action.name(),
                    doc.getId(), doc.getTitle()));
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

        Events.instance().raiseEvent(NAVIGATE_TO_DOCUMENT, currentDocument);

        return chosenView;
    }

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

    public String getCurrentDocumentUrl() {
        if (currentDocument == null) {
            log.error("current document is null");
            return null;
        }
        return DocumentLocator.getDocumentUrl(getCurrentServerLocation(),
                currentDocument.getRef());
    }

    public String getCurrentDocumentFullUrl() {
        if (currentDocument == null) {
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

    protected void resetCurrentPath() throws ClientException {
        final String logPrefix = "<resetCurrentPath> ";

        parents = new ArrayList<PathElement>();

        if (documentManager == null) {
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
                log.error(e, e);
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
                log.error(e, e);
            }
        }
    }

}
