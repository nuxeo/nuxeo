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

import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.annotations.remoting.WebRemote;
import org.jboss.seam.contexts.Context;
import org.jboss.seam.contexts.Contexts;
import org.jboss.seam.core.Events;
import org.jboss.seam.faces.FacesMessages;
import org.nuxeo.common.utils.Path;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.security.SecurityConstants;
import org.nuxeo.ecm.platform.ui.web.util.DocumentsListsUtils;
import org.nuxeo.ecm.webapp.documentsLists.DocumentsListsManager;
import org.nuxeo.ecm.webapp.helpers.EventNames;
import org.nuxeo.ecm.webapp.helpers.ResourcesAccessor;

/**
 * @author <a href="mailto:pdilorenzo@nuxeo.com">Peter Di Lorenzo</a>
 */
@Scope(CONVERSATION)
@Name("bulkSelectActions")
public class BulkSelectActions implements Serializable {

    private static final long serialVersionUID = 1L;

    protected static final String CACHED_SELECTED_DOCUMENT_IDS = "cachedSelectedDocumentIds";

    @In(create = true, required = false)
    protected CoreSession documentManager;

    @In(create = true)
    protected DocumentsListsManager documentsListsManager;

    @In(create = true, required = false)
    protected FacesMessages facesMessages;

    @In(create = true)
    protected ResourcesAccessor resourcesAccessor;

    public void deleteSelection() throws ClientException {
        if (!documentsListsManager.isWorkingListEmpty(DocumentsListsManager.CURRENT_DOCUMENT_SELECTION)) {
            deleteSelection(documentsListsManager.getWorkingList(DocumentsListsManager.CURRENT_DOCUMENT_SELECTION));
        }
    }

    public void deleteSelection(List<DocumentModel> docsToDelete)
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
     * Clears the working list of selected documents.
     * 
     * @param String lName The name of the working list of selected documents.
     *            If null, the default list will be used.
     */
    @WebRemote
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
     * @param String listName The name of the working list of selected documents.
     *            If null, the default list will be used.
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

}