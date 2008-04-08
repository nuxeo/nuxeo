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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.io.Serializable;

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
import org.jboss.seam.annotations.Observer;
import org.jboss.seam.annotations.Out;
import org.jboss.seam.annotations.RequestParameter;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.contexts.Context;
import org.jboss.seam.contexts.Contexts;
import org.jboss.seam.core.Events;
import org.nuxeo.common.utils.Path;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.ecm.core.api.PagedDocumentsProvider;
import org.nuxeo.ecm.core.api.SortInfo;
import org.nuxeo.ecm.core.api.VersionModel;
import org.nuxeo.ecm.core.api.impl.DocumentModelListImpl;
import org.nuxeo.ecm.core.api.impl.FacetFilter;
import org.nuxeo.ecm.core.api.security.SecurityConstants;
import org.nuxeo.ecm.core.query.sql.SQLQueryParser;
import org.nuxeo.ecm.core.search.api.backend.indexing.resources.factory.BuiltinDocumentFields;
import org.nuxeo.ecm.core.search.api.client.SearchService;
import org.nuxeo.ecm.core.search.api.client.common.SearchServiceDelegate;
import org.nuxeo.ecm.core.search.api.client.query.impl.ComposedNXQueryImpl;
import org.nuxeo.ecm.core.search.api.client.search.results.ResultSet;
import org.nuxeo.ecm.platform.ejb.EJBExceptionHandler;
import org.nuxeo.ecm.platform.types.Type;
import org.nuxeo.ecm.platform.types.adapter.TypeInfo;
import org.nuxeo.ecm.platform.ui.web.api.NavigationContext;
import org.nuxeo.ecm.platform.ui.web.api.UserAction;
import org.nuxeo.ecm.platform.ui.web.pagination.ResultsProviderFarmUserException;
import org.nuxeo.ecm.platform.ui.web.pathelements.ArchivedVersionsPathElement;
import org.nuxeo.ecm.platform.ui.web.pathelements.DocumentPathElement;
import org.nuxeo.ecm.platform.ui.web.pathelements.PathElement;
import org.nuxeo.ecm.platform.ui.web.pathelements.VersionDocumentPathElement;
import org.nuxeo.ecm.platform.ui.web.util.BadDocumentUriException;
import org.nuxeo.ecm.platform.ui.web.util.DocumentLocator;
import org.nuxeo.ecm.platform.ui.web.util.DocumentsListsUtils;
import org.nuxeo.ecm.platform.url.api.DocumentLocation;
import org.nuxeo.ecm.platform.util.RepositoryLocation;
import org.nuxeo.ecm.webapp.action.TypesTool;
import org.nuxeo.ecm.webapp.contentbrowser.DocumentChildrenStdFarm;
import org.nuxeo.ecm.webapp.delegate.DocumentManagerBusinessDelegate;
import org.nuxeo.ecm.webapp.helpers.ApplicationControllerHelper;
import org.nuxeo.ecm.webapp.helpers.EventManager;
import org.nuxeo.ecm.webapp.helpers.EventNames;
import org.nuxeo.ecm.webapp.pagination.ResultsProvidersCache;

/**
 * Implementation for the navigationContext component available on the session.
 *
 */
@Name("navigationContext")
@Scope(CONVERSATION)
@Install(precedence=FRAMEWORK)
public class NavigationContextBean implements NavigationContextLocal, Serializable {

    private static final long serialVersionUID = -3708768859028774906L;

    private static final Log log = LogFactory.getLog(NavigationContextBean.class);

    @In
    private transient Context conversationContext;

    @In
    protected transient Context eventContext;


    // --------------------------------------------
    // fields managed by this class
    // These fields can be accessed by 2 ways
    // - simple getters
    // - via the context thanks to @Factory

    @In(create=true, required = false)
    private ServerContextBean serverLocator;

    private DocumentModel currentDomain;

    private DocumentModel currentContentRoot;

    private DocumentModel currentWorkspace;

    private DocumentModel currentDocument;

    private DocumentModel currentSuperSpace;

    private DocumentModelList currentDocumentChildren;

    private List<DocumentModel> currentDocumentParents;

    // private String baseURL="";

    // this is used for documents about to create
    // which are not yet persisted thus not all their fields are
    // valid: like Ref
    // NXGED-1021: we need to re-inject this from the context with the values that might
    // be set from a create page. If we don't do this the document filled in with form
    // data will be lost in the context replaced later with an empty one that is de-seriliazed
    // with this bean.
    // XXX : injection removed !
    // @In(required = alse)
    // @Out(required = false)
    private DocumentModel changeableDocument;

    private List<PathElement> parents;


    @In(create = true, required = false)
    private transient CoreSession documentManager;

    @Out(required = false)
    @Deprecated
    private PagedDocumentsProvider resultsProvider;

    @Create
    @PostActivate
    public void init() {
        log.debug("<init> ");
        parents = null;
    }

    public DocumentModel getCurrentDocument() {
        return currentDocument;
    }

    /**
     * Implementation details: the path to current domain
     * is deduced from the path of current document
     * (hardcoded rule that it'd be the first element).
     *
     * If current document is null, then the first document found is
     * used instead.
     */
    public String getCurrentDomainPath() throws ClientException {
        if (currentDomain != null) {
            return currentDomain.getPathAsString();
        }
        Path path;
        if (currentDocument != null) {
            path = currentDocument.getPath();
        } else {
            // Find a document. Maybe using the core could be robuster
            // but not sure query support will exist for long in core
            SearchService ss = SearchServiceDelegate.getRemoteSearchService();
            if (ss == null) {
                throw new ClientException("Cannot find the Search Service");
            }

            ComposedNXQueryImpl q = new ComposedNXQueryImpl(
                    SQLQueryParser.parse("SELECT * FROM Document"),
                    ss.getSearchPrincipal((NuxeoPrincipal) documentManager.getPrincipal()));
            ResultSet results;
            try {
                results = ss.searchQuery(q, 0, 1);
            } catch (Exception e) {
                throw new ClientException(e);
            }
            if (results.getPageHits() != 1) {
                log.error("Could not find a single document readable by " +
                          "current user. Are the indexes empty?");
                return null;
            } else {
                path = new Path((String) results.get(0).get(BuiltinDocumentFields.FIELD_DOC_PATH));
            }
        }
        String[] segs = {path.segment(0)};
        return Path.createFromSegments(segs).toString();
    }

    public void setCurrentDocument(DocumentModel documentModel)
            throws ClientException {

        if (!checkIfUpdateNeeded(currentDocument, documentModel)) {
            return;
        }

        invalidateChildrenProvider();

        currentSuperSpace = null;
        currentDocument = documentModel;
        if (currentDocument == null) {
            currentDocumentParents = null;
        } else {
            // failsafe: if the ref is null (because of bugs in other
            // components)
            // we might end with the destruction of this bean.
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
        eventContext.remove("currentDocument");

        // raise events
        EventManager.raiseEventsOnDocumentSelected(currentDocument);
    }

    public DocumentModel getChangeableDocument() {
        return changeableDocument;
    }

    public void setChangeableDocument(DocumentModel changeableDocument) {
        this.changeableDocument = changeableDocument;
        eventContext.set("changeableDocument", changeableDocument);
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

    @Observer( value={ EventNames.DOCUMENT_CHILDREN_CHANGED }, create=false, inject=false)
    public void resetCurrentDocumentChildrenCache(DocumentModel targetDoc) {
        if (targetDoc != null && currentDocument != null
                && !currentDocument.getRef().equals(targetDoc.getRef())) {
            return;
        }
        resultsProvider = null;
    }

    // @Observer({ EventNames.CONTENT_ROOT_SELECTION_CHANGED,
    // EventNames.DOCUMENT_SELECTION_CHANGED })
    public DocumentModelList getCurrentDocumentChildren()
            throws ClientException {
        final String logPrefix = "<getCurrentDocumentChildren> ";

        if (documentManager == null) {
            log.error(logPrefix + "documentManager not initialized");
            return new DocumentModelListImpl();
        }


        FacetFilter facetFilter = new FacetFilter("HiddenInNavigation", false);
        try {
            currentDocumentChildren = documentManager.getChildren(
                    currentDocument.getRef(), null, SecurityConstants.READ,
                    facetFilter, null);
            // logDocWithTitle(logPrefix + "Retrieved children for: ",
            // currentDocument);
        } catch (Throwable t) {
            throw EJBExceptionHandler.wrapException(t);
        }
        return currentDocumentChildren;
    }

    public PagedDocumentsProvider getCurrentResultsProvider() {
        return resultsProvider;
    }

    /**
     * @see NavigationContext#setCurrentResultsProvider(PagedDocumentsProvider)
     */
    @Deprecated
    public void setCurrentResultsProvider(PagedDocumentsProvider resultsProvider) {
        this.resultsProvider = resultsProvider;
    }

    //@Observer( { EventNames.DOCUMENT_CHILDREN_CHANGED })
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

        // if (resultsProvider != null) {
        // return resultsProvider.getCurrentPage();
        // }

        if (documentManager == null) {
            log.error(logPrefix + "documentManager not initialized");
            return new DocumentModelListImpl();
        }

        // FacetFilter facetFilter = new FacetFilter("HiddenInNavigation",
        // false);
        try {
            // DocumentModelIterator resultDocsIt =
            // documentManager.getChildrenIterator(
            // currentDocument.getRef(), null, SecurityConstants.READ,
            // facetFilter);

            //
            // resultsProvider = new DocumentsPageProvider(resultDocsIt, 10);
            // resultsProvider =
            // documentChildrenFarm.getResultsProvider(DocumentChildrenStdFarm.CHILDREN_BY_COREAPI);

            ResultsProvidersCache resultsProvidersCache = (ResultsProvidersCache) Component.getInstance("resultsProvidersCache");
            resultsProvider = resultsProvidersCache.get(DocumentChildrenStdFarm.CHILDREN_BY_COREAPI);

            currentDocumentChildren = resultsProvider.getCurrentPage();

            // logDocWithTitle(logPrefix + "Retrieved children for: ",
            // currentDocument);
        } catch (Throwable t) {
            throw EJBExceptionHandler.wrapException(t);
        }
        return currentDocumentChildren;
    }

    public DocumentModel getCurrentDomain() {
        return currentDomain;
    }

    public void setCurrentDomain(DocumentModel domainDocModel)
            throws ClientException {
        /*
         * if (domainDocModel != currentDocument) { this.currentDomain =
         * domainDocModel; // XXX AT: domain is a document, having it as
         * currentDocument is // usefull this.currentDocument = domainDocModel; //
         * reset everything else this.currentWorkspace = null;
         * this.currentDocumentChildren = getCurrentDocumentChildren(); }
         */
        if (!checkIfUpdateNeeded(currentDomain, domainDocModel)) {
            return;
        }

        currentDomain = domainDocModel;
        eventContext.remove("currentDomain");

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
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }

        // if we switched branch then realign currentDocument
        if (currentDocumentParents != null
                && !DocumentsListsUtils.isDocumentInList(domainDocModel,
                        currentDocumentParents)) {
            try {
                setCurrentDocument(domainDocModel);
            } catch (ClientException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }

        Events.instance().raiseEvent(EventNames.DOMAIN_SELECTION_CHANGED,
                currentDomain);
    }

    protected boolean checkIfUpdateNeeded(DocumentModel ctxDoc,
            DocumentModel newDoc) {
        if (ctxDoc == null && newDoc != null
                || ctxDoc != null && newDoc == null) {
            return true;
        }
        if (ctxDoc == null && newDoc == null) {
            return false;
        }
        return !ctxDoc.getCacheKey().equals(newDoc.getCacheKey());
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

    public RepositoryLocation getCurrentServerLocation() {

        RepositoryLocation currentRepository = serverLocator.getCurrentServerLocation();

        /*if (currentRepository==null)
        {
            log.warn("currentServerLocation is null : is this normal");
            if (currentDocument!=null)
            {
                String repoName = currentDocument.getRepositoryName();
                log.warn("trying to automatically set currentServerLocation is null");
                try {
                    setCurrentServerLocation(new RepositoryLocation(repoName));
                } catch (ClientException e) {
                    log.warn("unable to find currentServerLocation");
                }
            }
        }*/
        return currentRepository;
    }

    // XXX: use getCurrentServerLocation() instead
    @Deprecated
    public RepositoryLocation getSelectedServerLocation() {
        return serverLocator.getCurrentServerLocation();
    }

    /**
     * Switches to a new server location by updating the context and updating to
     * the CoreSession (DocumentManager).
     *
     * @throws ClientException
     */
    public void setCurrentServerLocation(RepositoryLocation serverLocation)
            throws ClientException {
        if (serverLocation==null)
        {
            log.warn("Setting ServerLocation to null, is this normal ?");
        }

        RepositoryLocation currentServerLocation = serverLocation;
        serverLocator.setRepositoryLocation(serverLocation);
        resetCurrentContext();
        eventContext.set("currentServerLocation", currentServerLocation);

        // update the documentManager
        documentManager = null;
        documentManager = getOrCreateDocumentManager();
        Events.instance().raiseEvent(EventNames.LOCATION_SELECTION_CHANGED);

        DocumentModel rootDocument = documentManager.getRootDocument();
        if (documentManager.hasPermission(rootDocument.getRef(), SecurityConstants.READ)) {
            currentDocument = rootDocument;
            updateContextVariables();
        }
    }

    /**
     * Returns the current documentManager if any or create a new session to the
     * current location.
     *
     * @throws ClientException
     */
    public CoreSession getOrCreateDocumentManager() throws ClientException {
        if (documentManager != null) {
            return documentManager;
        }
        DocumentManagerBusinessDelegate documentManagerBD = (DocumentManagerBusinessDelegate) Contexts.lookupInStatefulContexts("documentManager");
        // DocumentManagerBusinessDelegate documentManagerBD =
        // (DocumentManagerBusinessDelegate) sessionContext
        // .get("documentManager");
        if (documentManagerBD == null) {
            // this is the first time we select the location, create a
            // DocumentManagerBusinessDelegate instance
            documentManagerBD = new DocumentManagerBusinessDelegate();
            conversationContext.set("documentManager", documentManagerBD);
        }
        documentManager = documentManagerBD.getDocumentManager(getCurrentServerLocation());
        return documentManager;
    }

    public DocumentModel getCurrentWorkspace() {
        return currentWorkspace;
    }

    // Factories to make navigation related data
    // available in the context

    @Factory(value = "currentDocument", scope = EVENT)
    public DocumentModel factoryCurrentDocument() {
        return getCurrentDocument();
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

    //@Factory(value = "currentServerLocation", scope = EVENT)
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

    /**
     * @param workspaceDocModel
     * @throws ClientException
     */
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

        // updateDocumentContext(workspaceDocModel);
    }

    public void updateDocumentContext(DocumentModel doc) throws ClientException {
        setCurrentDocument(doc);
    }

    /**
     * Updates variables according to hierachy rules and to the new
     * currentDocument.
     *
     * @throws ClientException
     *
     */
    protected void updateContextVariables() throws ClientException {

        // if (null == currentDocument) {
        // throw new IllegalArgumentException("null currentDocument");
        // }

        // XXX flush method is not implemented for Event context :)
        eventContext.set("currentDocument", currentDocument);

        // XXX: backward compatibility : to be removed
        //sessionContext.set("currentItem", currentDocument);
        //currentItem = currentDocument;

        //sessionContext.set("changeableDocument", currentDocument);
        if (currentDocument!=null)
        {
            changeableDocument = currentDocument;
            eventContext.set("changeableDocument", changeableDocument);
        }
        else
        {
            changeableDocument = currentDocument;
            eventContext.remove("changeableDocument");
        }

        // TODO: OG: what is the use of that variable?
        //sessionContext.set("selectedDocument", currentDocument);

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

                if (docType != null && docType.equals("Workspace")) {
                    //sessionContext.set("currentWorkspace", docModel);
                    setCurrentWorkspace(docModel);
                }

                if (null == docType || docType.equals("WorkspaceRoot")
                        || docType.equals("SectionRoot")) {
                    //sessionContext.set("contentRootDocument", docModel);
                    setCurrentContentRoot(docModel);
                }

                if (docType != null && docType.equals("Domain")) {

                    if (!docModel.equals(currentDomain)) {
                        // update the currentDomain
                        currentDomain = docModel;
                        //sessionContext.set("currentDomain", currentDomain);
                        setCurrentDomain(docModel);
                    }
                }
            }
        }
        // reinit lower tree
        docType = currentDocument.getType();
        if (docType.equals("Domain")) {
            setCurrentDomain(currentDocument);
            setCurrentContentRoot(null);
            setCurrentWorkspace(null);
        } else if (docType.equals("WorkspaceRoot")
                || docType.equals("SectionRoot")) {
            setCurrentContentRoot(currentDocument);
            setCurrentWorkspace(null);
        } else if (docType.equals("Workspace")) {
            setCurrentWorkspace(currentDocument);
        }
    }

    public void resetCurrentContext() {
        //sessionContext.set("currentItem", null);
        //sessionContext.set("currentDocument", null);
        //sessionContext.set("changeableDocument", null);
        //sessionContext.set("selectedDocument", null);
        //sessionContext.set("currentDomain", null);
        //sessionContext.set("contentRootDocument", null);

        // flush event context
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
            eventContext.set("changeableDocument", doc);
            //sessionContext.set("changeableDocument", doc);
        } else {
            updateDocumentContext(doc);
        }

        final Type type = typesTool.getType(doc.getType());

        // log.debug(logPrefix + "doc: " +
        // DocumentModelUtils.getInfo(currentDocument));

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
            // throw new UnsupportedOperationException("for action " + action);
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

    /*
     * (non-Javadoc)
     *
     * @see org.nuxeo.ecm.webapp.context.NavigationContext#navigateToDocument(org.nuxeo.ecm.core.api.DocumentModel,
     *      org.nuxeo.ecm.core.api.VersionModel)
     */
    public String navigateToDocument(DocumentModel docModel,
            VersionModel versionModel) throws ClientException {
        DocumentModel docVersion = documentManager.getDocumentWithVersion(
                docModel.getRef(), versionModel);
        return navigateToDocument(docVersion);
    }

    // ---------------------------------------------
    // listeners for seam events
    // @Observer( { EventNames.LOCATION_SELECTION_CHANGED,
    // EventNames.USER_ALL_DOCUMENT_TYPES_SELECTION_CHANGED,
    // EventNames.GO_HOME })
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
        if (null == getCurrentDocument()) {
            log.error("current document is null");
            return null;
        }
        return DocumentLocator.getFullDocumentUrl(getCurrentServerLocation(),
                getCurrentDocument().getRef());
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
        RepositoryLocation repLoc = new RepositoryLocation(docLoc.getServerLocationName());
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

    private void resetCurrentPath() throws ClientException {
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
                    parents.add(new DocumentPathElement(docModel));
                }

                parents.add(new ArchivedVersionsPathElement(sourceDocument));
                parents.add(new VersionDocumentPathElement(currentDocument));
            } else {
                if (currentDocumentParents != null) {
                    for (DocumentModel docModel : currentDocumentParents) {
                        parents.add(new DocumentPathElement(docModel));
                    }
                }
            }
        }

        resultsProvider = null;
    }

    @SuppressWarnings("unused")
    private void logDocWithTitle(String msg, DocumentModel doc) {
        if (null != doc) {
            log.debug(msg + " " + doc.getProperty("dublincore", "title"));
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
        eventContext.remove("currentContentRoot");

        if (crDocumentModel == null) {
            return;
        }

        if (currentDocument == null) {
            // setCurrentWorkspace(null);
            try {
                setCurrentDocument(null);
            } catch (ClientException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            return;
        }

        // if we switched branch then realign currentDocument
        if (currentDocumentParents != null
                && !DocumentsListsUtils.isDocumentInList(
                        crDocumentModel, currentDocumentParents)) {
            try {
                setCurrentDocument(crDocumentModel);
            } catch (ClientException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }

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
