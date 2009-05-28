package org.nuxeo.dam.webapp.filter;

import java.util.List;

import org.nuxeo.ecm.platform.ui.web.model.SelectDataModel;
import org.nuxeo.ecm.platform.ui.web.model.SelectDataModelListener;
import org.nuxeo.ecm.platform.ui.web.model.impl.SelectDataModelImpl;
import org.nuxeo.ecm.platform.ui.web.model.impl.SelectDataModelRowEvent;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.PagedDocumentsProvider;
import org.nuxeo.ecm.webapp.documentsLists.DocumentsListsManager;
import org.nuxeo.ecm.webapp.pagination.ResultsProvidersCache;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.annotations.Factory;
import org.jboss.seam.ScopeType;

@Name("filterResults")
@Scope(ScopeType.CONVERSATION)
public class FilterResults implements SelectDataModelListener {

    public static final String SEARCH_DOCUMENT_LIST = "SEARCH_DOCUMENT_LIST";

    public static final String FILTERED_DOCUMENT_PROVIDER_NAME = "FILTERED_DOCUMENTS";

    @In(required = false, create = true)
    protected transient DocumentsListsManager documentsListsManager;

    @In(required = false, create = true)
    protected transient ResultsProvidersCache resultsProvidersCache;

    protected transient PagedDocumentsProvider provider;

    @Factory(value = "filterSelectModel", scope = ScopeType.EVENT)
    public SelectDataModel getResultsSelectModelFiltered() throws ClientException {
        return getResultsSelectModel(FILTERED_DOCUMENT_PROVIDER_NAME);
    }

    public SelectDataModel getResultsSelectModel(String providerName)
            throws ClientException {
        if (providerName == null) {
            throw new ClientException("providerName has not been set yet");
        }
        List<DocumentModel> selectedDocuments = documentsListsManager.getWorkingList(
                DocumentsListsManager.CURRENT_DOCUMENT_SELECTION);
        SelectDataModel model = new SelectDataModelImpl(SEARCH_DOCUMENT_LIST,
                getResultDocuments(providerName), selectedDocuments);
        model.addSelectModelListener(this);
        return model;
    }

    public List<DocumentModel> getResultDocuments(String providerName)
            throws ClientException {
        return getProvider(providerName).getCurrentPage();
    }

    public PagedDocumentsProvider getProvider(String providerName)
            throws ClientException {
        provider = resultsProvidersCache.get(providerName);
        if (provider == null) {
            throw new ClientException(
                    "Unknown or unbuildable results provider: " + providerName);
        }
        return provider;
    }

    public void processSelectRowEvent(SelectDataModelRowEvent event) {
        Boolean selection = event.getSelected();
        DocumentModel data = (DocumentModel) event.getRowData();
        if (selection) {
            documentsListsManager.addToWorkingList(
                    DocumentsListsManager.CURRENT_DOCUMENT_SELECTION, data);
        } else {
            documentsListsManager.removeFromWorkingList(
                    DocumentsListsManager.CURRENT_DOCUMENT_SELECTION, data);
        }
    }

}
