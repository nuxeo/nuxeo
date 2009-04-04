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

package org.nuxeo.ecm.webapp.action;

import static org.jboss.seam.ScopeType.EVENT;
import static org.jboss.seam.annotations.Install.FRAMEWORK;

import java.io.Serializable;
import java.security.Principal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import javax.faces.application.FacesMessage;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Install;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Out;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.core.Events;
import org.jboss.seam.faces.FacesMessages;
import org.nuxeo.common.utils.Path;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.PagedDocumentsProvider;
import org.nuxeo.ecm.core.api.SortInfo;
import org.nuxeo.ecm.core.api.impl.DocumentModelListImpl;
import org.nuxeo.ecm.core.api.security.SecurityConstants;
import org.nuxeo.ecm.core.search.api.client.query.QueryException;
import org.nuxeo.ecm.core.search.api.client.querymodel.QueryModel;
import org.nuxeo.ecm.platform.actions.Action;
import org.nuxeo.ecm.platform.ui.web.api.ResultsProviderFarm;
import org.nuxeo.ecm.platform.ui.web.api.WebActions;
import org.nuxeo.ecm.platform.ui.web.model.SelectDataModel;
import org.nuxeo.ecm.platform.ui.web.model.SelectDataModelListener;
import org.nuxeo.ecm.platform.ui.web.model.impl.SelectDataModelImpl;
import org.nuxeo.ecm.platform.ui.web.model.impl.SelectDataModelRowEvent;
import org.nuxeo.ecm.platform.ui.web.pagination.ResultsProviderFarmUserException;
import org.nuxeo.ecm.platform.ui.web.util.DocumentsListsUtils;
import org.nuxeo.ecm.platform.util.RepositoryLocation;
import org.nuxeo.ecm.webapp.base.InputController;
import org.nuxeo.ecm.webapp.contentbrowser.DocumentActions;
import org.nuxeo.ecm.webapp.documentsLists.DocumentsListsManager;
import org.nuxeo.ecm.webapp.edit.lock.LockActions;
import org.nuxeo.ecm.webapp.helpers.EventNames;
import org.nuxeo.ecm.webapp.pagination.ResultsProvidersCache;
import org.nuxeo.ecm.webapp.querymodel.QueryModelActions;
import org.nuxeo.ecm.webapp.search.SearchActions;
import org.nuxeo.ecm.webapp.trashManagement.TrashManager;

@Name("deleteActions")
@Scope(EVENT)
@Install(precedence = FRAMEWORK)
public class DeleteActionsBean extends InputController implements
        DeleteActions, Serializable, SelectDataModelListener,
        ResultsProviderFarm {

    public static final String DELETED_CHILDREN_BY_COREAPI = "CURRENT_DOC_DELETED_CHILDREN";

    protected static final String BOARD_USER_DELETED = "USER_DELETED_DOCUMENTS";

    private static final long serialVersionUID = 9860854328986L;

    private static final Log log = LogFactory.getLog(DeleteActionsBean.class);

    private static final String DOC_REF = "ref";

    private static final String WANTED_TRANSITION = "transition";

    private static final String DELETE_OUTCOME = "after_delete";

    private static final String DELETE_TRANSITION = "delete";

    private static final String UNDELETE_TRANSITION = "undelete";

    @In(create = true, required = false)
    private transient CoreSession documentManager;

    @In(create = true, required = false)
    protected RepositoryLocation currentServerLocation;

    @In(create = true)
    private transient DocumentsListsManager documentsListsManager;

    @In(create = true)
    private transient TrashManager trashManager;

    @In(create = true)
    private transient LockActions lockActions;

    @In(create = true)
    protected transient WebActions webActions;

    @In(create = true)
    protected transient QueryModelActions queryModelActions;

    @In(create = true)
    private transient SearchActions searchActions;

    // Imported from Navigation context - used to get the deleted sub-documents
    @In(create = true)
    private transient ResultsProvidersCache resultsProvidersCache;

    @Out(required = false)
    @Deprecated
    private PagedDocumentsProvider resultsProvider;

    private DocumentModelList currentDocumentChildren;

    // end import

    @In
    private transient Principal currentUser;

    private Boolean searchDeletedDocuments;


    private static class PathComparator implements Comparator<DocumentModel>, Serializable {

        private static final long serialVersionUID = -6449747704324789701L;

        public int compare(DocumentModel o1, DocumentModel o2) {
            return o1.getPathAsString().compareTo(o2.getPathAsString());
        }

    }

    public String purgeSelection() throws ClientException {
        if (!documentsListsManager.isWorkingListEmpty(DocumentsListsManager.CURRENT_DOCUMENT_TRASH_SELECTION)) {
            return purgeSelection(documentsListsManager.getWorkingList(DocumentsListsManager.CURRENT_DOCUMENT_TRASH_SELECTION));
        } else {
            log.debug("No documents selection in context to process delete on...");
            return null;
        }
    }

    public String purgeSelection(List<DocumentModel> docsToPurge)
            throws ClientException {
        if (null != docsToPurge) {
            List<DocumentModel> docsThatCanBeDeleted = filterDeleteListAccordingToPerms(docsToPurge);

            // Keep only topmost documents (see NXP-1411)
            // This is not strictly necessary with Nuxeo Core >= 1.3.2
            Collections.sort(docsThatCanBeDeleted, new PathComparator());
            List<DocumentModel> rootDocsToDelete = new LinkedList<DocumentModel>();
            Path previousPath = null;
            for (DocumentModel doc : docsThatCanBeDeleted) {
                if (previousPath == null
                        || !previousPath.isPrefixOf(doc.getPath())) {
                    rootDocsToDelete.add(doc);
                    previousPath = doc.getPath();
                }
            }

            // Three auxiliary collections deduced from rootDocsToDelete:
            // references, paths, and references of parents.
            // Computed before actual removal for robustness
            List<DocumentRef> references = DocumentsListsUtils.getDocRefs(rootDocsToDelete);
            Set<Path> paths = new HashSet<Path>();
            for (DocumentModel doc : rootDocsToDelete) {
                paths.add(doc.getPath());
            }
            Set<DocumentRef> parentsRefs = new HashSet<DocumentRef>();
            parentsRefs.addAll(DocumentsListsUtils.getParentRefFromDocumentList(rootDocsToDelete));

            // Treat the cases where the navigation context is under one of the
            // deleted documents by climbing up.
            // Target document computation is before removal for robustness.
            DocumentModel targetContext = navigationContext.getCurrentDocument();
            while (underOneOf(targetContext.getPath(), paths)) {
                targetContext = documentManager.getParentDocument(targetContext.getRef());
            }

            // remove from all lists
            documentsListsManager.removeFromAllLists(docsThatCanBeDeleted);

            // ACTUAL REMOVAL
            documentManager.removeDocuments(references.toArray(new DocumentRef[references.size()]));
            documentManager.save();

            // Update context if needed
            navigationContext.setCurrentDocument(targetContext);

            // Notify parents
            for (DocumentRef parentRef : parentsRefs) {
                DocumentModel parent = documentManager.getDocument(parentRef);
                if (parent != null) {
                    Events.instance().raiseEvent(
                            EventNames.DOCUMENT_CHILDREN_CHANGED, parent);
                }
            }

            // User feedback
            Object[] params = { references.size() };
            FacesMessage message = FacesMessages.createFacesMessage(
                    FacesMessage.SEVERITY_INFO, "#0 "
                            + resourcesAccessor.getMessages().get(
                                    "n_deleted_docs"), params);
            facesMessages.add(message);
            log.debug("documents deleted...");
        } else {
            log.debug("nothing to delete...");
        }

        return computeOutcome(DELETE_OUTCOME);
    }

    private static boolean underOneOf(Path testedPath, Set<Path> paths) {
        for (Path path : paths) {
            if (path.isPrefixOf(testedPath)) {
                return true;
            }
        }
        return false;
    }

    public boolean getCanDeleteItem(DocumentModel container)
            throws ClientException {
        if (documentManager != null && container != null) {
            if (documentManager.hasPermission(container.getRef(),
                    SecurityConstants.REMOVE_CHILDREN)) {
                return true;
            }
        }
        return false;
    }

    public boolean getCanDelete() throws ClientException {
        List<DocumentModel> docsToDelete = documentsListsManager.getWorkingList(DocumentsListsManager.CURRENT_DOCUMENT_SELECTION);

        if (docsToDelete == null || docsToDelete.isEmpty()) {
            return false;
        }

        // do simple filtering
        return checkDeletePermOnParents(docsToDelete);
    }

    public boolean getCanDeleteSections() throws ClientException {
        List<DocumentModel> docsToDelete = documentsListsManager.getWorkingList(
                DocumentsListsManager.CURRENT_DOCUMENT_SECTION_SELECTION);

        if (docsToDelete == null || docsToDelete.isEmpty()) {
            return false;
        }

        List<DocumentModel> realDocsToDelete = new ArrayList<DocumentModel>();
        for (DocumentModel doc : docsToDelete) {
            if (!doc.isProxy()) {
                realDocsToDelete.add(doc);
            }
        }

        if (realDocsToDelete.isEmpty()) {
            return false;
        }

        // do simple filtering
        return checkDeletePermOnParents(realDocsToDelete);
    }

    public boolean getCanPurge() throws ClientException {
        List<DocumentModel> docsToDelete = documentsListsManager.getWorkingList(
                DocumentsListsManager.CURRENT_DOCUMENT_TRASH_SELECTION);

        if (docsToDelete == null || docsToDelete.isEmpty()) {
            return false;
        }

        for (DocumentModel doc : docsToDelete) {
            if (!"deleted".equals(doc.getCurrentLifeCycleState())) {
                return false;
            }
        }

        // do simple filtering
        return checkDeletePermOnParents(docsToDelete);
    }

    public boolean checkDeletePermOnParents(List<DocumentModel> docsToDelete) {

        List<DocumentRef> parentRefs = DocumentsListsUtils.getParentRefFromDocumentList(docsToDelete);

        for (DocumentRef parentRef : parentRefs) {
            try {
                if (documentManager.hasPermission(parentRef,
                        SecurityConstants.REMOVE_CHILDREN)) {
                    return true;
                }
            } catch (ClientException e) {
                log.error(e);
            }
        }
        return false;
    }

    private List<DocumentModel> filterDeleteListAccordingToPermsOnParents(
            List<DocumentModel> docsToDelete) throws ClientException {

        List<DocumentModel> docsThatCanBeDeleted = new ArrayList<DocumentModel>();
        List<DocumentRef> parentRefs = DocumentsListsUtils.getParentRefFromDocumentList(docsToDelete);

        for (DocumentRef parentRef : parentRefs) {
            if (documentManager.hasPermission(parentRef,
                    SecurityConstants.REMOVE_CHILDREN)) {
                for (DocumentModel doc : docsToDelete) {
                    if (doc.getParentRef().equals(parentRef)) {
                        docsThatCanBeDeleted.add(doc);
                    }
                }
            }
        }
        return docsThatCanBeDeleted;
    }

    private List<DocumentModel> filterDeleteListAccordingToPerms(
            List<DocumentModel> docsToDelete) throws ClientException {
        // first filter on parents
        List<DocumentModel> docsThatCanBeDeletedOnParent = filterDeleteListAccordingToPermsOnParents(docsToDelete);
        List<DocumentModel> docsThatCanBeDeleted = new ArrayList<DocumentModel>();

        int forbiddenDocs = docsToDelete.size()
                - docsThatCanBeDeletedOnParent.size();

        int lockedDocs = 0;
        for (DocumentModel docToDelete : docsThatCanBeDeletedOnParent) {
            if (documentManager.hasPermission(docToDelete.getRef(),
                    SecurityConstants.REMOVE)) {

                // Check if document is locker
                if (docToDelete.isLocked()) {
                    String locker = lockActions.getLockDetails(docToDelete).get(
                            LockActions.LOCKER);
                    if (currentUser.getName().equals(locker)) {
                        docsThatCanBeDeleted.add(docToDelete);
                    } else {
                        lockedDocs += 1;
                    }
                } else {
                    docsThatCanBeDeleted.add(docToDelete);
                }
            } else {
                forbiddenDocs += 1;
            }
        }

        if (lockedDocs > 0) {
            Object[] params = { lockedDocs };
            facesMessages.add(FacesMessage.SEVERITY_WARN, "#0 "
                    + resourcesAccessor.getMessages().get(
                            "n_locked_docs_can_not_delete"), params);
        }

        if (forbiddenDocs > 0) {
            Object[] params2 = { forbiddenDocs };
            facesMessages.add(FacesMessage.SEVERITY_WARN, "#0 "
                    + resourcesAccessor.getMessages().get(
                            "n_forbidden_docs_can_not_delete"), params2);
        }

        return docsThatCanBeDeleted;
    }

    public String deleteSelection() throws ClientException {
        if (!documentsListsManager.isWorkingListEmpty(DocumentsListsManager.CURRENT_DOCUMENT_SELECTION)) {
            return deleteSelection(documentsListsManager.getWorkingList(DocumentsListsManager.CURRENT_DOCUMENT_SELECTION));
        } else {
            log.debug("No documents selection in context to process delete on...");
            return null;
        }
    }

    public String deleteSelectionSections() throws ClientException {
        List<DocumentModel> docsToDelete = documentsListsManager.getWorkingList(
                DocumentsListsManager.CURRENT_DOCUMENT_SECTION_SELECTION);

        if (docsToDelete == null || docsToDelete.isEmpty()) {
            return null;
        }
        boolean selectionContainsProxy = false;
        List<DocumentModel> nonProxyDocsToDelete = new ArrayList<DocumentModel>();
        for (DocumentModel doc : docsToDelete) {
            if (doc.isProxy()) {
                selectionContainsProxy = true;
            } else {
                nonProxyDocsToDelete.add(doc);
            }
        }

        if (selectionContainsProxy) {
            FacesMessage message = FacesMessages.createFacesMessage(
                    FacesMessage.SEVERITY_WARN, "can_not_delete_proxies", null);
            facesMessages.add(message);

        }
        return deleteSelection(nonProxyDocsToDelete);
    }

    public String deleteSelection(List<DocumentModel> docsToDelete)
            throws ClientException {
        if (null != docsToDelete) {

            List<DocumentModel> docsThatCanBeDeleted = filterDeleteListAccordingToPerms(docsToDelete);

            // Keep only topmost documents (see NXP-1411)
            // This is not strictly necessary with Nuxeo Core >= 1.3.2
            Collections.sort(docsThatCanBeDeleted, new PathComparator());
            List<DocumentModel> rootDocsToDelete = new LinkedList<DocumentModel>();
            Path previousPath = null;
            for (DocumentModel doc : docsThatCanBeDeleted) {
                if (previousPath == null
                        || !previousPath.isPrefixOf(doc.getPath())) {
                    rootDocsToDelete.add(doc);
                    previousPath = doc.getPath();
                }
            }

            // Three auxiliary collections deduced from rootDocsToDelete:
            // references, paths, and references of parents.
            // Computed before actual removal for robustness
            List<DocumentRef> references = DocumentsListsUtils.getDocRefs(rootDocsToDelete);
            Set<Path> paths = new HashSet<Path>();
            for (DocumentModel doc : rootDocsToDelete) {
                paths.add(doc.getPath());
            }
            Set<DocumentRef> parentsRefs = new HashSet<DocumentRef>();
            parentsRefs.addAll(DocumentsListsUtils.getParentRefFromDocumentList(rootDocsToDelete));

            // Treat the cases where the navigation context is under one of the
            // deleted documents by climbing up.
            // Target document computation is before removal for robustness.
            DocumentModel targetContext = navigationContext.getCurrentDocument();
            while (underOneOf(targetContext.getPath(), paths)) {
                targetContext = documentManager.getParentDocument(targetContext.getRef());
            }

            // remove from all lists
            documentsListsManager.removeFromAllLists(docsThatCanBeDeleted);

            if (trashManager.isTrashManagementEnabled()) {
                // Change state
                moveDocumentsToTrash(docsThatCanBeDeleted);
            } else {
                // Trash bin is not enabled
                documentManager.removeDocuments(references.toArray(new DocumentRef[references.size()]));
            }

            documentManager.save();

            // Update context if needed
            navigationContext.setCurrentDocument(targetContext);

            // Notify parents
            for (DocumentRef parentRef : parentsRefs) {
                DocumentModel parent = documentManager.getDocument(parentRef);
                if (parent != null) {
                    Events.instance().raiseEvent(
                            EventNames.DOCUMENT_CHILDREN_CHANGED, parent);
                }
            }

            // User feedback
            Object[] params = { references.size() };
            FacesMessage message = FacesMessages.createFacesMessage(
                    FacesMessage.SEVERITY_INFO, "#0 "
                            + resourcesAccessor.getMessages().get(
                                    "n_deleted_docs"), params);
            facesMessages.add(message);
            log.debug("documents deleted...");
        } else {
            log.debug("nothing to delete...");
        }

        return computeOutcome(DELETE_OUTCOME);
    }

    /**
     * Starts a trasition to delete state for the documents in the list
     *
     * @param docsThatCanBeDeleted
     * @throws ClientException
     */
    private void moveDocumentsToTrash(List<DocumentModel> docsThatCanBeDeleted)
            throws ClientException {
        for (DocumentModel docModel : docsThatCanBeDeleted) {
            // check if transition is available
            DocumentModel document = documentManager.getDocument(docModel.getRef());

            // delete children
            if (document.getAllowedStateTransitions().contains(
                    DELETE_TRANSITION)) {
                document.followTransition(DELETE_TRANSITION);
            } else {
                log.warn("Document " + document.getId() + " of type "
                        + document.getType() + " in state "
                        + document.getCurrentLifeCycleState()
                        + " does not support transition " + DELETE_TRANSITION
                        + ", it will be deleted immediately");
                documentManager.removeDocument(document.getRef());
            }
        }
    }


    public String undeleteSelection() throws ClientException {
        if (!documentsListsManager.isWorkingListEmpty(DocumentsListsManager.CURRENT_DOCUMENT_TRASH_SELECTION)) {
            return undeleteSelection(documentsListsManager.getWorkingList(DocumentsListsManager.CURRENT_DOCUMENT_TRASH_SELECTION));
        } else {
            log.debug("No documents selection in context to process delete on...");
            return null;
        }
    }

    public String undeleteSelection(List<DocumentModel> docsToUndelete)
            throws ClientException {
        if (null != docsToUndelete) {

            List<DocumentModel> docsThatCanBeUndeleted = filterDeleteListAccordingToPerms(docsToUndelete);

            // Keep only topmost documents (see NXP-1411)
            // This is not strictly necessary with Nuxeo Core >= 1.3.2
            Collections.sort(docsThatCanBeUndeleted, new PathComparator());
            List<DocumentModel> rootDocsToDelete = new LinkedList<DocumentModel>();
            Path previousPath = null;
            for (DocumentModel doc : docsToUndelete) {
                if (previousPath == null
                        || !previousPath.isPrefixOf(doc.getPath())) {
                    rootDocsToDelete.add(doc);
                    previousPath = doc.getPath();
                }
            }

            // Three auxiliary collections deduced from rootDocsToDelete:
            // references, paths, and references of parents.
            // Computed before actual removal for robustness
            List<DocumentRef> references = DocumentsListsUtils.getDocRefs(rootDocsToDelete);
            Set<Path> paths = new HashSet<Path>();
            for (DocumentModel doc : rootDocsToDelete) {
                paths.add(doc.getPath());
            }
            Set<DocumentRef> parentsRefs = new HashSet<DocumentRef>();
            parentsRefs.addAll(DocumentsListsUtils.getParentRefFromDocumentList(rootDocsToDelete));

            // Treat the cases where the navigation context is under one of the
            // deleted documents by climbing up.
            // Target document computation is before removal for robustness.
            DocumentModel targetContext = navigationContext.getCurrentDocument();
            while (underOneOf(targetContext.getPath(), paths)) {
                targetContext = documentManager.getParentDocument(targetContext.getRef());
            }

            // remove from all lists
            documentsListsManager.removeFromAllLists(docsThatCanBeUndeleted);

            // Change state
            undeleteDocumentsFromTrash(docsThatCanBeUndeleted);
            for (DocumentModel document : docsThatCanBeUndeleted) {
                undeleteDocument(documentManager.getParentDocuments(document.getRef()));
            }
            documentManager.save();

            // Update context if needed
            navigationContext.setCurrentDocument(targetContext);

            // Notify parents
            for (DocumentRef parentRef : parentsRefs) {
                List<DocumentModel> parents = documentManager.getParentDocuments(parentRef);
                for (DocumentModel parent : parents) {
                    if (parent != null) {
                        Events.instance().raiseEvent(
                                EventNames.DOCUMENT_CHILDREN_CHANGED, parent);
                    }
                }
            }

            // User feedback
            Object[] params = { references.size() };
            FacesMessage message = FacesMessages.createFacesMessage(
                    FacesMessage.SEVERITY_INFO, "#0 "
                            + resourcesAccessor.getMessages().get(
                                    "n_undeleted_docs"), params);
            facesMessages.add(message);
            log.debug("documents undeleted...");
        } else {
            log.debug("nothing to undelete...");
        }

        return computeOutcome(DELETE_OUTCOME);
    }

    public boolean isTrashManagementEnabled() throws ClientException {
        return trashManager.isTrashManagementEnabled();
    }

    private void undeleteDocumentsFromTrash(
            List<DocumentModel> docsToBeUndeleted) throws ClientException {
        // TODO Notify all concerned documents...
        for (DocumentModel docModel : docsToBeUndeleted) {
            // check if transition is available
            DocumentModel document = documentManager.getDocument(docModel.getRef());
            if (document.getAllowedStateTransitions().contains(
                    UNDELETE_TRANSITION)) {
                document.followTransition(UNDELETE_TRANSITION);
            } else {
                throw new ClientException("Impossible to move document="
                        + document.getPathAsString()
                        + " Life Cycle is not available 1");
            }

            // restore children
            /*
            if (document.isFolder()) {

                DocumentRef parentRef = document.getRef();
                String transition = UNDELETE_TRANSITION;
                String aUser = currentUser.toString();
                String repository = currentServerLocation.getName();

                MassLifeCycleTransitionMessage msg = new MassLifeCycleTransitionMessage(
                        aUser, transition, repository, parentRef);

                try {
                    getDocumentMessageProducer().produce(msg);

                } catch (Exception e) {
                    throw new ClientException(e);
                }
            }*/
        }
    }

    private static void undeleteDocument(List<DocumentModel> docsToBeUndeleted)
            throws ClientException {
        // TODO Notify all concerned documents...
        for (DocumentModel document : docsToBeUndeleted) {
            // check if transition is available
            if (document.getAllowedStateTransitions().contains(
                    UNDELETE_TRANSITION)) {
                document.followTransition(UNDELETE_TRANSITION);
            } else {
                log.debug("No undelete transition defined....");
            }
        }
    }

    public SelectDataModel getDeletedChildrenSelectModel()
            throws ClientException {

        DocumentModelList documents = getCurrentDocumentDeletedChildrenPage();
        List<DocumentModel> selectedDocuments = documentsListsManager.getWorkingList(
                DocumentsListsManager.CURRENT_DOCUMENT_TRASH_SELECTION);
        SelectDataModel model = new SelectDataModelImpl(
                DocumentActions.CHILDREN_DOCUMENT_LIST, documents,
                selectedDocuments);
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

            resultsProvidersCache.invalidate(DELETED_CHILDREN_BY_COREAPI);

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
    public void processSelectRowEvent(SelectDataModelRowEvent event)
            throws ClientException {
        Boolean selection = event.getSelected();
        DocumentModel data = (DocumentModel) event.getRowData();
        if (selection) {
            documentsListsManager.addToWorkingList(
                    DocumentsListsManager.CURRENT_DOCUMENT_TRASH_SELECTION,
                    data);
        } else {
            documentsListsManager.removeFromWorkingList(
                    DocumentsListsManager.CURRENT_DOCUMENT_TRASH_SELECTION,
                    data);
        }
    }

    public List<Action> getActionsForTrashSelection() {
        return webActions.getUnfiltredActionsList(DocumentsListsManager.CURRENT_DOCUMENT_TRASH_SELECTION
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

    private PagedDocumentsProvider getResultsProviderForDeletedDocs(
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
    private PagedDocumentsProvider getChildrenResultsProviderQMPattern(
            String queryModelName, DocumentModel parent, SortInfo sortInfo)
            throws ClientException {

        final String parentId = parent.getId();
        Object[] params = { parentId };
        return getResultsProvider(queryModelName, params, sortInfo);
    }

    private PagedDocumentsProvider getResultsProvider(String qmName,
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
                    "deleted" };
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

    // FIXME: should check permissions
    public boolean getCanRestoreCurrentDoc() throws ClientException {
        DocumentModel currentDoc = navigationContext.getCurrentDocument();
        if (currentDoc != null) {
            return "deleted".equals(currentDoc.getCurrentLifeCycleState());
        } else {
            // this shouldn't happen, if it happens probably there is a
            // customization bug, we guard this though
            log.warn("Null currentDocument in navigationContext");
            return false;
        }
    }

}
