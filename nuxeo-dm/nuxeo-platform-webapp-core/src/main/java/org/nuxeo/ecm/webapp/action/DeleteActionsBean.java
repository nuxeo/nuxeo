/*
 * (C) Copyright 2006-2010 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Thierry Delprat
 *     Florent Guillaume
 */

package org.nuxeo.ecm.webapp.action;

import static org.nuxeo.ecm.webapp.contentbrowser.DocumentActions.CHILDREN_DOCUMENT_LIST;
import static org.nuxeo.ecm.webapp.documentsLists.DocumentsListsManager.CURRENT_DOCUMENT_SECTION_SELECTION;
import static org.nuxeo.ecm.webapp.documentsLists.DocumentsListsManager.CURRENT_DOCUMENT_SELECTION;
import static org.nuxeo.ecm.webapp.documentsLists.DocumentsListsManager.CURRENT_DOCUMENT_TRASH_SELECTION;
import static org.nuxeo.ecm.webapp.helpers.EventNames.DOCUMENT_CHILDREN_CHANGED;
import static org.nuxeo.ecm.webapp.helpers.EventNames.FOLDERISHDOCUMENT_SELECTION_CHANGED;

import java.io.Serializable;
import java.security.Principal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jboss.seam.Component;
import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Install;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Observer;
import org.jboss.seam.annotations.Out;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.annotations.intercept.BypassInterceptors;
import org.jboss.seam.core.Events;
import org.jboss.seam.international.StatusMessage;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.LifeCycleConstants;
import org.nuxeo.ecm.core.api.PagedDocumentsProvider;
import org.nuxeo.ecm.core.api.SortInfo;
import org.nuxeo.ecm.core.api.impl.DocumentModelListImpl;
import org.nuxeo.ecm.core.search.api.client.querymodel.QueryModel;
import org.nuxeo.ecm.core.api.security.SecurityConstants;
import org.nuxeo.ecm.core.trash.TrashInfo;
import org.nuxeo.ecm.core.trash.TrashService;
import org.nuxeo.ecm.platform.actions.Action;
import org.nuxeo.ecm.platform.ui.web.api.ResultsProviderFarm;
import org.nuxeo.ecm.platform.ui.web.api.WebActions;
import org.nuxeo.ecm.platform.ui.web.model.SelectDataModel;
import org.nuxeo.ecm.platform.ui.web.model.SelectDataModelListener;
import org.nuxeo.ecm.platform.ui.web.model.impl.SelectDataModelImpl;
import org.nuxeo.ecm.platform.ui.web.model.impl.SelectDataModelRowEvent;
import org.nuxeo.ecm.platform.ui.web.pagination.ResultsProviderFarmUserException;
import org.nuxeo.ecm.platform.util.RepositoryLocation;
import org.nuxeo.ecm.webapp.base.InputController;
import org.nuxeo.ecm.webapp.documentsLists.DocumentsListsManager;
import org.nuxeo.ecm.webapp.edit.lock.LockActions;
import org.nuxeo.ecm.webapp.pagination.ResultsProvidersCache;
import org.nuxeo.ecm.webapp.querymodel.QueryModelActions;
import org.nuxeo.ecm.webapp.search.SearchActions;
import org.nuxeo.ecm.webapp.trashManagement.TrashManager;
import org.nuxeo.runtime.api.Framework;

@Name("deleteActions")
@Scope(ScopeType.EVENT)
@Install(precedence = Install.FRAMEWORK)
public class DeleteActionsBean extends InputController implements
        DeleteActions, Serializable, SelectDataModelListener,
        ResultsProviderFarm {

    private static final long serialVersionUID = 1L;

    private static final Log log = LogFactory.getLog(DeleteActionsBean.class);

    public static final String DELETED_CHILDREN_BY_COREAPI = "CURRENT_DOC_DELETED_CHILDREN";

    public static final String BOARD_USER_DELETED = "USER_DELETED_DOCUMENTS";

    public static final String DELETE_OUTCOME = "after_delete";

    @In(create = true, required = false)
    protected transient CoreSession documentManager;

    @In(create = true, required = false)
    protected RepositoryLocation currentServerLocation;

    @In(create = true)
    protected transient DocumentsListsManager documentsListsManager;

    @In(create = true)
    protected transient TrashManager trashManager;

    @In(create = true)
    protected transient LockActions lockActions;

    @In(create = true)
    protected transient WebActions webActions;

    @In(create = true)
    protected transient QueryModelActions queryModelActions;

    @In(create = true)
    protected transient SearchActions searchActions;

    @Out(required = false)
    @Deprecated
    protected PagedDocumentsProvider resultsProvider;

    protected DocumentModelList currentDocumentChildren;

    @In
    protected transient Principal currentUser;

    protected Boolean searchDeletedDocuments;

    protected transient TrashService trashService;

    protected TrashService getTrashService() {
        if (trashService == null) {
            try {
                trashService = Framework.getService(TrashService.class);
            } catch (Exception e) {
                throw new RuntimeException("TrashService not available", e);
            }
        }
        return trashService;
    }

    public boolean getCanDeleteItem(DocumentModel container)
            throws ClientException {
        if (container == null) {
            return false;
        }
        return getTrashService().folderAllowsDelete(container);
    }

    public boolean getCanDelete() {
        List<DocumentModel> docs = documentsListsManager.getWorkingList(CURRENT_DOCUMENT_SELECTION);
        try {
            return getTrashService().canDelete(docs, currentUser, false);
        } catch (ClientException e) {
            log.error("Cannot check delete permission", e);
            return false;
        }
    }

    public boolean getCanDeleteSections() {
        List<DocumentModel> docs = documentsListsManager.getWorkingList(CURRENT_DOCUMENT_SECTION_SELECTION);
        try {
            return getTrashService().canDelete(docs, currentUser, true);
        } catch (ClientException e) {
            log.error("Cannot check delete permission", e);
            return false;
        }
    }

    public boolean getCanPurge() throws ClientException {
        List<DocumentModel> docs = documentsListsManager.getWorkingList(CURRENT_DOCUMENT_TRASH_SELECTION);
        try {
            return getTrashService().canPurgeOrUndelete(docs, currentUser);
        } catch (ClientException e) {
            log.error("Cannot check delete permission", e);
            return false;
        }
    }

    public boolean checkDeletePermOnParents(List<DocumentModel> docs) {
        try {
            return getTrashService().checkDeletePermOnParents(docs);
        } catch (ClientException e) {
            log.error("Cannot check delete permission", e);
            return false;
        }
    }

    public String deleteSelection() throws ClientException {
        if (!documentsListsManager.isWorkingListEmpty(CURRENT_DOCUMENT_SELECTION)) {
            return deleteSelection(documentsListsManager.getWorkingList(CURRENT_DOCUMENT_SELECTION));
        } else {
            log.debug("No documents selection in context to process delete on...");
            return null;
        }
    }

    public String deleteSelectionSections() throws ClientException {
        if (!documentsListsManager.isWorkingListEmpty(CURRENT_DOCUMENT_SECTION_SELECTION)) {
            return deleteSelection(documentsListsManager.getWorkingList(CURRENT_DOCUMENT_SECTION_SELECTION));
        } else {
            log.debug("No documents selection in context to process delete on...");
            return null;
        }
    }

    protected static final int OP_DELETE = 1, OP_PURGE = 2, OP_UNDELETE = 3;

    public String deleteSelection(List<DocumentModel> docs)
            throws ClientException {
        int op = isTrashManagementEnabled() ? OP_DELETE : OP_PURGE;
        return actOnSelection(op, docs);
    }

    public String purgeSelection() throws ClientException {
        if (!documentsListsManager.isWorkingListEmpty(CURRENT_DOCUMENT_TRASH_SELECTION)) {
            return purgeSelection(documentsListsManager.getWorkingList(CURRENT_DOCUMENT_TRASH_SELECTION));
        } else {
            log.debug("No documents selection in context to process delete on...");
            return null;
        }
    }

    public String purgeSelection(List<DocumentModel> docs)
            throws ClientException {
        return actOnSelection(OP_PURGE, docs);
    }

    public String undeleteSelection() throws ClientException {
        if (!documentsListsManager.isWorkingListEmpty(CURRENT_DOCUMENT_TRASH_SELECTION)) {
            return undeleteSelection(documentsListsManager.getWorkingList(CURRENT_DOCUMENT_TRASH_SELECTION));
        } else {
            log.debug("No documents selection in context to process delete on...");
            return null;
        }
    }

    public String undeleteSelection(List<DocumentModel> docs)
            throws ClientException {
        return actOnSelection(OP_UNDELETE, docs);

    }

    protected String actOnSelection(int op, List<DocumentModel> docs)
            throws ClientException {
        if (docs == null) {
            return null;
        }
        TrashInfo info;
        try {
            info = getTrashService().getTrashInfo(docs, currentUser, false,
                    false);
        } catch (ClientException e) {
            log.error("Cannot check delete permission", e);
            return null;
        }

        DocumentModel targetContext = getTrashService().getAboveDocument(
                navigationContext.getCurrentDocument(), info.rootPaths);

        // remove from all lists
        documentsListsManager.removeFromAllLists(info.docs);

        Set<DocumentRef> parentRefs;
        String msgid;
        // operation to do
        switch (op) {
        case OP_PURGE:
            getTrashService().purgeDocuments(documentManager, info.rootRefs);
            parentRefs = info.rootParentRefs;
            msgid = "n_deleted_docs";
            break;
        case OP_DELETE:
            getTrashService().trashDocuments(info.docs);
            parentRefs = info.rootParentRefs;
            msgid = "n_deleted_docs";
            break;
        case OP_UNDELETE:
            parentRefs = getTrashService().undeleteDocuments(info.docs);
            msgid = "n_undeleted_docs";
            break;
        default:
            throw new AssertionError(op);
        }

        // Update context if needed
        navigationContext.setCurrentDocument(targetContext);

        // Notify parents
        if (parentRefs.isEmpty()) {
            // Globally refresh content views
            Events.instance().raiseEvent(DOCUMENT_CHILDREN_CHANGED);
        } else {
            for (DocumentRef parentRef : parentRefs) {
                if (documentManager.hasPermission(parentRef,
                        SecurityConstants.READ)) {
                    DocumentModel parent = documentManager.getDocument(parentRef);
                    if (parent != null) {
                        Events.instance().raiseEvent(DOCUMENT_CHILDREN_CHANGED,
                                parent);
                    }
                }
            }
        }

        // User feedback
        if (info.proxies > 0) {
            facesMessages.add(StatusMessage.Severity.WARN,
                    "can_not_delete_proxies");
        }
        Object[] params = { Integer.valueOf(info.docs.size()) };
        facesMessages.add(StatusMessage.Severity.INFO, "#0 "
                + resourcesAccessor.getMessages().get(msgid), params);

        return computeOutcome(DELETE_OUTCOME);
    }

    public boolean isTrashManagementEnabled() {
        return trashManager.isTrashManagementEnabled();
    }

    public SelectDataModel getDeletedChildrenSelectModel()
            throws ClientException {
        DocumentModelList documents = getCurrentDocumentDeletedChildrenPage();
        List<DocumentModel> selectedDocuments = documentsListsManager.getWorkingList(CURRENT_DOCUMENT_TRASH_SELECTION);
        SelectDataModel model = new SelectDataModelImpl(CHILDREN_DOCUMENT_LIST,
                documents, selectedDocuments);
        model.addSelectModelListener(this);
        // XXX AT: see if cache is useful
        // cacheUpdateNotifier.addCacheListener(model);
        return model;
    }

    public DocumentModelList getCurrentDocumentDeletedChildrenPage()
            throws ClientException {

        if (documentManager == null) {
            log.error("documentManager not initialized");
            return new DocumentModelListImpl();
        }

        try {
            ResultsProvidersCache resultsProvidersCache = (ResultsProvidersCache) Component.getInstance("resultsProvidersCache");
            resultsProvider = resultsProvidersCache.get(DELETED_CHILDREN_BY_COREAPI);
            currentDocumentChildren = resultsProvider.getCurrentPage();
        } catch (Throwable t) {
            throw ClientException.wrap(t);
        }
        return currentDocumentChildren;
    }

    /**
     * Listener method - not used for now because the binding is not used but
     * might be used after the refactoring.
     */
    public void processSelectRowEvent(SelectDataModelRowEvent event) {
        Boolean selection = event.getSelected();
        DocumentModel data = (DocumentModel) event.getRowData();
        if (selection) {
            documentsListsManager.addToWorkingList(
                    CURRENT_DOCUMENT_TRASH_SELECTION, data);
        } else {
            documentsListsManager.removeFromWorkingList(
                    CURRENT_DOCUMENT_TRASH_SELECTION, data);
        }
    }

    public List<Action> getActionsForTrashSelection() {
        return webActions.getUnfiltredActionsList(CURRENT_DOCUMENT_TRASH_SELECTION
                + "_LIST");
    }

    // @Create
    public void create() {
    }

    // @Destroy
    // @Remove
    public void destroy() {
    }

    public PagedDocumentsProvider getResultsProvider(String name)
            throws ClientException, ResultsProviderFarmUserException {
        return getResultsProvider(name, null);
    }

    public PagedDocumentsProvider getResultsProvider(String name,
            SortInfo sortInfo) throws ClientException,
            ResultsProviderFarmUserException {
        PagedDocumentsProvider provider = null;

        if (BOARD_USER_DELETED.equals(name)) {
            Object[] params = { currentUser.getName() };
            try {
                provider = getQmDocuments(name, params, sortInfo);
            } catch (Exception e) {
                log.error("sorted query failed");
                log.debug(e);
                log.error("retrying without sort parameters");
                provider = getQmDocuments(name, params, null);
            }
        } else if (DELETED_CHILDREN_BY_COREAPI.equals(name)) {
            provider = getResultsProviderForDeletedDocs(name, sortInfo);
        }
        provider.setName(name);
        return provider;
    }

    protected PagedDocumentsProvider getResultsProviderForDeletedDocs(
            String name, SortInfo sortInfo) throws ClientException {
        final DocumentModel currentDoc = navigationContext.getCurrentDocument();

        if (DELETED_CHILDREN_BY_COREAPI.equals(name)) {
            PagedDocumentsProvider provider = getChildrenResultsProviderQMPattern(
                    name, currentDoc, sortInfo);
            provider.setName(name);
            return provider;
        } else {
            throw new ClientException("Unknown provider: " + name);
        }
    }

    protected PagedDocumentsProvider getQmDocuments(String qmName,
            Object[] params, SortInfo sortInfo) throws ClientException {
        return queryModelActions.get(qmName).getResultsProvider(
                documentManager, params, sortInfo);
    }

    /**
     * Usable with a queryModel that defines a pattern NXQL.
     */
    protected PagedDocumentsProvider getChildrenResultsProviderQMPattern(
            String queryModelName, DocumentModel parent, SortInfo sortInfo)
            throws ClientException {

        final String parentId = parent.getId();
        Object[] params = { parentId };
        return getResultsProvider(queryModelName, params, sortInfo);
    }

    protected PagedDocumentsProvider getResultsProvider(String qmName,
            Object[] params, SortInfo sortInfo) throws ClientException {
        QueryModel qm = queryModelActions.get(qmName);
        return qm.getResultsProvider(documentManager, params, sortInfo);
    }

    /**
     * @return the searchDeletedDocuments.
     */
    public Boolean getSearchDeletedDocuments() {
        return searchDeletedDocuments;
    }

    /**
     * @param searchDeletedDocuments the searchDeletedDocuments to set.
     * @throws ClientException
     */
    public void setSearchDeletedDocuments(Boolean searchDeletedDocuments)
            throws ClientException {
        this.searchDeletedDocuments = searchDeletedDocuments;

        // check if it should search for deleted documents
        String[] states = null;
        if (searchDeletedDocuments) {
            states = new String[] { "project", "approved", "obsolete",
                    LifeCycleConstants.DELETED_STATE };
        } else {
            states = new String[] { "project", "approved", "obsolete" };
        }
        searchActions.getDocumentModel().setProperty("advanced_search",
                "currentLifeCycleStates", states);
    }

    public void restoreCurrentDocument() throws ClientException {
        List<DocumentModel> doc = new ArrayList<DocumentModel>();
        doc.add(navigationContext.getCurrentDocument());
        undeleteSelection(doc);
    }

    public boolean getCanRestoreCurrentDoc() throws ClientException {
        DocumentModel doc = navigationContext.getCurrentDocument();
        if (doc == null) {
            // this shouldn't happen, if it happens probably there is a
            // customization bug, we guard this though
            log.warn("Null currentDocument in navigationContext");
            return false;
        }
        try {
            return getTrashService().canPurgeOrUndelete(
                    Collections.singletonList(doc), currentUser);
        } catch (ClientException e) {
            log.error("Cannot check delete permission", e);
            return false;
        }

    }

    @Observer(value = { FOLDERISHDOCUMENT_SELECTION_CHANGED })
    @BypassInterceptors
    public void resetProviderCache() {
        ResultsProvidersCache resultsProvidersCache = (ResultsProvidersCache) Component.getInstance("resultsProvidersCache");
        resultsProvidersCache.invalidate(DELETED_CHILDREN_BY_COREAPI);
    }

    public boolean restoreActionDisplay() throws ClientException {
        return getCanRestoreCurrentDoc() && isTrashManagementEnabled();
    }
}
