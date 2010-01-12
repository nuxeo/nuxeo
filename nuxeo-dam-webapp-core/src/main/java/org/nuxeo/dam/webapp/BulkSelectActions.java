package org.nuxeo.dam.webapp;

import static org.jboss.seam.ScopeType.CONVERSATION;

import java.io.Serializable;
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
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.contexts.Context;
import org.jboss.seam.contexts.Contexts;
import org.jboss.seam.core.Events;
import org.jboss.seam.faces.FacesMessages;
import org.nuxeo.common.utils.Path;
import org.nuxeo.dam.webapp.contentbrowser.DocumentActions;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.PagedDocumentsProvider;
import org.nuxeo.ecm.core.api.security.SecurityConstants;
import org.nuxeo.ecm.platform.actions.Action;
import org.nuxeo.ecm.platform.ui.web.api.WebActions;
import org.nuxeo.ecm.platform.ui.web.model.SelectDataModel;
import org.nuxeo.ecm.platform.ui.web.model.SelectDataModelRow;
import org.nuxeo.ecm.platform.ui.web.util.DocumentsListsUtils;
import org.nuxeo.ecm.webapp.clipboard.ClipboardActions;
import org.nuxeo.ecm.webapp.documentsLists.DocumentsListsManager;
import org.nuxeo.ecm.webapp.helpers.EventNames;
import org.nuxeo.ecm.webapp.helpers.ResourcesAccessor;
import org.nuxeo.ecm.webapp.pagination.ResultsProvidersCache;

/**
 * @author <a href="mailto:pdilorenzo@nuxeo.com">Peter Di Lorenzo</a>
 * @author <a href="mailto:cbaican@nuxeo.com">Catalin Baican</a>
 */
@Scope(CONVERSATION)
@Name("bulkSelectActions")
public class BulkSelectActions implements Serializable {

    private static final long serialVersionUID = 1L;

    private static final Log log = LogFactory.getLog(BulkSelectActions.class);

    protected static final String CACHED_SELECTED_DOCUMENT_IDS = "cachedSelectedDocumentIds";

    @In(create = true, required = false)
    protected CoreSession documentManager;

    @In(create = true)
    protected DocumentsListsManager documentsListsManager;

    @In(create = true, required = false)
    protected FacesMessages facesMessages;

    @In(create = true)
    protected ResourcesAccessor resourcesAccessor;

    @In(create = true)
    protected transient WebActions webActions;

    @In(create = true)
    protected DocumentActions documentActions;

    @In(create = true)
    protected ClipboardActions clipboardActions;

    @In(create = true)
    protected transient ResultsProvidersCache resultsProvidersCache;

    public void deleteSelection() throws ClientException {
        if (!documentsListsManager.isWorkingListEmpty(DocumentsListsManager.CURRENT_DOCUMENT_SELECTION)) {
            deleteSelection(documentsListsManager.getWorkingList(DocumentsListsManager.CURRENT_DOCUMENT_SELECTION));
        }
    }

    public void deleteSelection(List<DocumentModel> docsToDelete)
            throws ClientException {
        if (null != docsToDelete) {

            if (docsToDelete.contains(documentActions.getCurrentSelection())) {
                documentActions.setCurrentSelection(null);
            }

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

            // remove from all lists
            documentsListsManager.removeFromAllLists(docsThatCanBeDeleted);

            // TODO: factorize trash management from Nuxeo DM
            documentManager.removeDocuments(references.toArray(new DocumentRef[references.size()]));
            documentManager.save();

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
        }
    }

    protected List<DocumentModel> filterDeleteListAccordingToPerms(
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

                // TODO: factorize locked document filtering from DAM
                docsThatCanBeDeleted.add(docToDelete);
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

    private static class PathComparator implements Comparator<DocumentModel>,
            Serializable {

        private static final long serialVersionUID = -6449747704324789701L;

        public int compare(DocumentModel o1, DocumentModel o2) {
            return o1.getPathAsString().compareTo(o2.getPathAsString());
        }

    }

    /**
     * Tests if a document is in the working list of selected documents.
     *
     * @param String docId The DocumentRef of the document
     * @param String listName The name of the working list of selected
     *            documents. If null, the default list will be used.
     * @return boolean true if the document is in the list, false if it isn't.
     */
    @SuppressWarnings("unchecked")
    public boolean getIsCurrentSelectionInWorkingList(String docId,
            String listName) {
        if (docId == null) {
            return false;
        }
        String lName = (listName == null) ? DocumentsListsManager.CURRENT_DOCUMENT_SELECTION
                : listName;

        // Caching the construction of the set of selected document ids so as
        // not to call the document list API 30 times per page rendering
        Context eventContext = Contexts.getEventContext();
        Set<String> selectedIds = (Set<String>) eventContext.get(CACHED_SELECTED_DOCUMENT_IDS);
        if (selectedIds == null) {
            selectedIds = new HashSet<String>();
            List<DocumentModel> selectedDocumentsList = documentsListsManager.getWorkingList(lName);
            if (selectedDocumentsList != null) {
                for (DocumentModel selectedDocumentModel : selectedDocumentsList) {
                    selectedIds.add(selectedDocumentModel.getId());
                }
            }
            eventContext.set(CACHED_SELECTED_DOCUMENT_IDS, selectedIds);
        }
        return selectedIds.contains(docId);
    }

    /**
     * Tests if all of the documents of the current page are in the working list
     * of selected documents.
     *
     * @param String providerName The provider name
     * @param String listName The name of the working list of selected
     *            documents. If null, the default list will be used.
     * @return boolean true if the document is in the list, false if it isn't.
     */
    public boolean getIsCurrentPageInWorkingList(String providerName,
            String listName) {
        listName = (listName == null) ? DocumentsListsManager.CURRENT_DOCUMENT_SELECTION
                : listName;

        List<DocumentModel> list = documentsListsManager.getWorkingList(listName);
        if (list == null) {
            log.error("no registered list with name " + listName);
            return false;
        }

        PagedDocumentsProvider provider;
        try {
            provider = resultsProvidersCache.get(providerName);
        } catch (ClientException e) {
            log.error("Failed to get provider " + providerName, e);
            return false;
        }
        DocumentModelList documents = provider.getCurrentPage();

        return list.containsAll(documents);
    }

    /**
     * Clears the working list of selected documents.
     *
     * @param String lName The name of the working list of selected documents.
     *            If null, the default list will be used.
     */
    public void clearWorkingList(String listName) {

        String lName = (listName == null) ? DocumentsListsManager.CURRENT_DOCUMENT_SELECTION
                : listName;

        List<DocumentModel> selectedDocumentsList = documentsListsManager.getWorkingList(lName);
        selectedDocumentsList.clear();
    }

    // TODO: move this API to documentsListsManager directly
    /**
     * Tests whether the current working list of selected documents is empty.
     *
     * @param String listName The name of the working list of selected
     *            documents. If null, the default list will be used.
     * @return boolean true if empty, false otherwise
     */
    public boolean getIsCurrentWorkingListEmpty(String listName) {

        String lName = (listName == null) ? DocumentsListsManager.CURRENT_DOCUMENT_SELECTION
                : listName;
        List<DocumentModel> selectedDocumentsList = documentsListsManager.getWorkingList(lName);
        if (selectedDocumentsList == null) {
            return true;
        }
        return selectedDocumentsList.isEmpty();
    }

    public void toggleDocumentSelection(DocumentModel doc, String listName) {
        listName = (listName == null) ? DocumentsListsManager.CURRENT_DOCUMENT_SELECTION
                : listName;
        List<DocumentModel> list = documentsListsManager.getWorkingList(listName);
        if (list == null) {
            log.error("no registered list with name " + listName);
            return;
        }
        if (list.contains(doc)) {
            documentsListsManager.removeFromWorkingList(listName, doc);
        } else {
            documentsListsManager.addToWorkingList(listName, doc);
        }
    }

    public void togglePageSelection(String providerName, String listName,
            SelectDataModel selectDataModel) {
        PagedDocumentsProvider provider;
        try {
            provider = resultsProvidersCache.get(providerName);
        } catch (ClientException e) {
            log.error("Failed to get provider " + providerName, e);
            return;
        }
        DocumentModelList documents = provider.getCurrentPage();
        listName = (listName == null) ? DocumentsListsManager.CURRENT_DOCUMENT_SELECTION
                : listName;
        List<DocumentModel> list = documentsListsManager.getWorkingList(listName);
        if (list == null) {
            log.error("no registered list with name " + listName);
            return;
        }
        if (list.containsAll(documents)) {
            documentsListsManager.removeFromWorkingList(listName, documents);
            for (SelectDataModelRow row : selectDataModel.getRows()) {
                row.setSelected(false);
            }
        } else {
            documentsListsManager.addToWorkingList(listName, documents);
            for (SelectDataModelRow row : selectDataModel.getRows()) {
                row.setSelected(true);
            }
        }
    }

    public boolean getCanDelete() {
        List<DocumentModel> docsToDelete = documentsListsManager.getWorkingList(DocumentsListsManager.CURRENT_DOCUMENT_SELECTION);

        if (docsToDelete == null || docsToDelete.isEmpty()) {
            return false;
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

    public boolean getCanExport() {
        List<DocumentModel> docsToExport = documentsListsManager.getWorkingList(DocumentsListsManager.CURRENT_DOCUMENT_SELECTION);

        if (docsToExport == null || docsToExport.isEmpty()) {
            return false;
        }

        return true;
    }

    public void exportSelection() throws ClientException {
        if (!documentsListsManager.isWorkingListEmpty(DocumentsListsManager.CURRENT_DOCUMENT_SELECTION)) {
            exportSelection(documentsListsManager.getWorkingList(DocumentsListsManager.CURRENT_DOCUMENT_SELECTION));
        }
    }

    public String exportSelection(List<DocumentModel> docsToExport)
            throws ClientException {
        if (docsToExport != null && !docsToExport.isEmpty()) {
            return clipboardActions.exportWorklistAsZip(docsToExport, false);
        }

        return null;
    }

    public List<Action> getActionsForSelectionNoAjax() {
        return webActions.getUnfiltredActionsList(DocumentsListsManager.CURRENT_DOCUMENT_SELECTION
                + "_NOA4J_LIST");
    }

}
