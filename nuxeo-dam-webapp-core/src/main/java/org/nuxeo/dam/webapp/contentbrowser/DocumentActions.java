package org.nuxeo.dam.webapp.contentbrowser;

import java.io.Serializable;
import java.util.List;
import java.util.ArrayList;

import org.nuxeo.ecm.core.api.PagedDocumentsProvider;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.webapp.documentsLists.DocumentsListsManager;
import org.nuxeo.ecm.webapp.pagination.ResultsProvidersCache;
import org.nuxeo.ecm.platform.actions.Action;
import org.nuxeo.ecm.platform.ui.web.api.WebActions;
import org.nuxeo.common.utils.StringUtils;
import org.jboss.seam.annotations.remoting.WebRemote;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.ScopeType;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.logging.Log;

@Name("documentActions")
@Scope(ScopeType.CONVERSATION)
// TODO: All the commented coded from below will be moved to a new WebActionBean
// SEAM bean which will be defined as specified in
// http://jira.nuxeo.org/browse/DAM-167
public class DocumentActions implements Serializable {

    private static final long serialVersionUID = 1L;

    private static final Log log = LogFactory.getLog(DocumentActions.class);

    @In(create = true)
    protected transient ResultsProvidersCache resultsProvidersCache;

    @In(required = false, create = true)
    protected transient DocumentsListsManager documentsListsManager;

//    @In(create = true)
//    protected transient WebActions webActions;

    /**
     * Current selected asset
     */
    protected DocumentModel currentSelection;

//    @WebRemote
//    public String processSelectRow(String docRef, String providerName,
//            String listName, Boolean selection) {
//        PagedDocumentsProvider provider;
//        try {
//            provider = resultsProvidersCache.get(providerName);
//        } catch (ClientException e) {
//            return handleError(e.getMessage());
//        }
//        DocumentModel doc = null;
//        for (DocumentModel pagedDoc : provider.getCurrentPage()) {
//            if (pagedDoc.getRef().toString().equals(docRef)) {
//                doc = pagedDoc;
//                break;
//            }
//        }
//        if (doc == null) {
//            return handleError(String.format(
//                    "could not find doc '%s' in the current page of provider '%s'",
//                    docRef, providerName));
//        }
//        String lName = (listName == null) ? DocumentsListsManager.CURRENT_DOCUMENT_SELECTION
//                : listName;
//        if (selection) {
//            documentsListsManager.addToWorkingList(lName, doc);
//        } else {
//            documentsListsManager.removeFromWorkingList(lName, doc);
//        }
//        return computeSelectionActions(lName);
//    }
//
//    @WebRemote
//    public String processSelectPage(String providerName, String listName,
//            Boolean selection) {
//        PagedDocumentsProvider provider;
//        try {
//            provider = resultsProvidersCache.get(providerName);
//        } catch (ClientException e) {
//            return handleError(e.getMessage());
//        }
//        DocumentModelList documents = provider.getCurrentPage();
//        String lName = (listName == null) ? DocumentsListsManager.CURRENT_DOCUMENT_SELECTION
//                : listName;
//        if (selection) {
//            documentsListsManager.addToWorkingList(lName, documents);
//        } else {
//            documentsListsManager.removeFromWorkingList(lName, documents);
//        }
//        return computeSelectionActions(lName);
//    }
//
//    private String handleError(String errorMessage) {
//        log.error(errorMessage);
//        return "ERROR: " + errorMessage;
//    }

//    private String computeSelectionActions(String listName) {
//        List<Action> availableActions = webActions.getUnfiltredActionsList(listName
//                + "_LIST");
//        List<String> availableActionIds = new ArrayList<String>();
//        for (Action a : availableActions) {
//            if (a.getAvailable()) {
//                availableActionIds.add(a.getId());
//            }
//        }
//        String res = "";
//        if (!availableActionIds.isEmpty()) {
//            res = StringUtils.join(availableActionIds.toArray(), "|");
//        }
//        return res;
//    }

    public DocumentModel getCurrentSelection() {
        return currentSelection;
    }

    public void setCurrentSelection(DocumentModel selection) {
        currentSelection = selection;
    }
}
