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
import org.jboss.seam.core.Events;
import org.jboss.seam.faces.FacesMessages;
import org.nuxeo.common.utils.Path;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.security.SecurityConstants;
import org.nuxeo.ecm.platform.ui.web.api.NavigationContext;
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
    private static final Log log = LogFactory.getLog(BulkSelectActions.class);
    
    @In(create = true, required = false)
    private transient CoreSession documentManager;
    
    @In(create = true)
    private transient DocumentsListsManager documentsListsManager;
    
    @In(create = true, required = false)
    private transient NavigationContext navigationContext;
    
    @In(create = true, required = false)
    protected FacesMessages facesMessages;

    @In(create = true)
    // won't inject this because of seam problem after activation
    // ::protected Map<String, String> messages;
    protected ResourcesAccessor resourcesAccessor;
 
    public String deleteSelection() throws ClientException {
        if (!documentsListsManager.isWorkingListEmpty(DocumentsListsManager.CURRENT_DOCUMENT_SELECTION)) {
            return deleteSelection(documentsListsManager.getWorkingList(DocumentsListsManager.CURRENT_DOCUMENT_SELECTION));
        } else {
            log.debug("No documents selection in context to process delete on...");
            return null;
        }
    }
    
    public String deleteSelection(List<DocumentModel> docsToDelete) throws ClientException {
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
        
            // Don't implement trash management in DAM for now
            /*
            if (trashManager.isTrashManagementEnabled()) {
                // Change state
                moveDocumentsToTrash(docsThatCanBeDeleted);
            } else {
                // Trash bin is not enabled
                documentManager.removeDocuments(references.toArray(new DocumentRef[references.size()]));
            }
            */
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
        
        // return computeOutcome(DELETE_OUTCOME);
        return null;
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

                // Dont implement the document locking stuff in DAM yet
                /*
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
                */
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
    
    private static boolean underOneOf(Path testedPath, Set<Path> paths) {
        for (Path path : paths) {
            if (path.isPrefixOf(testedPath)) {
                return true;
            }
        }
        return false;
    }
    
    
    
    private static class PathComparator implements Comparator<DocumentModel>,
    Serializable {

        private static final long serialVersionUID = -6449747704324789701L;
        
        public int compare(DocumentModel o1, DocumentModel o2) {
            return o1.getPathAsString().compareTo(o2.getPathAsString());
        }
        
        }

}