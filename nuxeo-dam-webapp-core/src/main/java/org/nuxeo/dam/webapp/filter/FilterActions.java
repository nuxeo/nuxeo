/*
 * (C) Copyright 2006-2009 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     Nuxeo
 */

package org.nuxeo.dam.webapp.filter;

import static org.jboss.seam.ScopeType.CONVERSATION;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.faces.model.SelectItem;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.Factory;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Observer;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.annotations.web.RequestParameter;
import org.nuxeo.dam.platform.context.ImportActionsBean;
import org.nuxeo.dam.webapp.contentbrowser.DocumentActions;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.PagedDocumentsProvider;
import org.nuxeo.ecm.core.api.SortInfo;
import org.nuxeo.ecm.core.search.api.client.querymodel.QueryModel;
import org.nuxeo.ecm.platform.ui.web.api.ResultsProviderFarm;
import org.nuxeo.ecm.platform.ui.web.api.SortNotSupportedException;
import org.nuxeo.ecm.platform.ui.web.pagination.ResultsProviderFarmUserException;
import org.nuxeo.ecm.webapp.directory.DirectoryTreeManager;
import org.nuxeo.ecm.webapp.directory.DirectoryTreeNode;
import org.nuxeo.ecm.webapp.helpers.EventNames;
import org.nuxeo.ecm.webapp.helpers.ResourcesAccessor;
import org.nuxeo.ecm.webapp.pagination.ResultsProvidersCache;
import org.nuxeo.ecm.webapp.querymodel.QueryModelActions;

@Scope(CONVERSATION)
@Name("filterActions")
public class FilterActions implements Serializable, ResultsProviderFarm {

    public static final String DC_COVERAGE_DIRECTORY_TREE = "dcCoverageDirectoryTree";

    public static final String TOPIC_DIRECTORY_TREE = "topicDirectoryTree";

    private static final long serialVersionUID = 1L;

    @SuppressWarnings("unused")
    private static final Log log = LogFactory.getLog(FilterActions.class);

    public static final List<String> DAM_DOCUMENT_TYPES = Arrays.asList("File",
            "Picture", "Video", "Audio");

    protected static final String QUERY_MODEL_NAME = "FILTERED_DOCUMENTS";

    protected static final String DOCTYPE_FIELD_XPATH = "filter_query:ecm_primaryType";

    protected static final String PATH_FIELD_XPATH = "filter_query:ecm_path";

    @In(create = true, required = false)
    protected transient CoreSession documentManager;

    @In(create = true)
    protected transient QueryModelActions queryModelActions;

    @In(create = true, required = false)
    transient ResultsProvidersCache resultsProvidersCache;

    @In(create = true)
    protected DirectoryTreeManager directoryTreeManager;

    @In(create = true)
    protected DocumentActions documentActions;

    @In(create = true)
    protected ResourcesAccessor resourcesAccessor;

    @RequestParameter
    protected String docType;

    @RequestParameter
    protected String folderPath;

    protected DocumentModel filterDocument;

    public DocumentModel getFilterDocument() throws ClientException {
        if (filterDocument == null) {
            filterDocument = queryModelActions.get(QUERY_MODEL_NAME).getDocumentModel();
        }
        return filterDocument;
    }

    @Factory(value = "docTypeSelectItems", scope = ScopeType.EVENT)
    public List<SelectItem> getDocTypeSelectItems() throws ClientException {
        DocumentModel filterDocument = getFilterDocument();
        List<String> docTypeSelection = filterDocument.getProperty(
                DOCTYPE_FIELD_XPATH).getValue(List.class);
        List<SelectItem> items = new ArrayList<SelectItem>();
        items.add(new SelectItem("All", "label.type.All", "",
                docTypeSelection.isEmpty()));
        for (String type : DAM_DOCUMENT_TYPES) {
            items.add(new SelectItem(type, "label.type." + type, "",
                    docTypeSelection.contains(type)));
        }
        return items;
    }

    @SuppressWarnings("unchecked")
    public void toggleSelectDocType() throws ClientException {
        DocumentModel filterDocument = getFilterDocument();
        List<String> previousSelection = filterDocument.getProperty(
                DOCTYPE_FIELD_XPATH).getValue(List.class);

        if ("All".equalsIgnoreCase(docType)) {
            previousSelection.clear();
        } else {
            if (previousSelection.contains(docType)) {
                previousSelection.remove(docType);
            } else {
                previousSelection.add(docType);

                if (previousSelection.size() == DAM_DOCUMENT_TYPES.size()) {
                    // back to empty selection which means no document type
                    // filtering:
                    previousSelection.clear();
                }
            }
        }
        filterDocument.setPropertyValue(DOCTYPE_FIELD_XPATH,
                (Serializable) previousSelection);
        invalidateProvider();
    }

    public List<DirectoryTreeNode> getCoverageTreeRoots() {
        return directoryTreeManager.get(DC_COVERAGE_DIRECTORY_TREE).getChildren();
    }

    public List<DirectoryTreeNode> getTopicTreeRoots() {
        return directoryTreeManager.get(TOPIC_DIRECTORY_TREE).getChildren();
    }

    @Factory(value = "folderSelectItems", scope = ScopeType.EVENT)
    public List<SelectItem> getFolderSelectItems() throws ClientException {
        DocumentModel filterDocument = getFilterDocument();
        String folderSelection = (String) filterDocument.getPropertyValue(PATH_FIELD_XPATH);
        List<SelectItem> items = new ArrayList<SelectItem>();
        items.add(new SelectItem("All", resourcesAccessor.getMessages().get(
                "label.type.All"), "", folderSelection == null));
        DocumentModelList docs = queryModelActions.get("IMPORT_FOLDERS").getDocuments(
                documentManager);
        for (DocumentModel doc : docs) {
            String docPath = doc.getPathAsString();
            items.add(new SelectItem(docPath, doc.getTitle(), "", docPath.equals(folderSelection)));
        }
        return items;
    }

    public void toggleSelectFolder() throws ClientException {
        DocumentModel filterDocument = getFilterDocument();
        if ("All".equals(folderPath)) {
            filterDocument.setPropertyValue(PATH_FIELD_XPATH, null);
        } else {
            filterDocument.setPropertyValue(PATH_FIELD_XPATH, folderPath);
        }
        invalidateProvider();
    }

    public PagedDocumentsProvider getResultsProvider(String queryModelName)
            throws ClientException, ResultsProviderFarmUserException {
        try {
            return getResultsProvider(queryModelName, null);
        } catch (SortNotSupportedException e) {
            throw new ClientException("unexpected exception", e);
        }
    }

    public PagedDocumentsProvider getResultsProvider(String queryModelName,
            SortInfo sortInfo) throws ClientException,
            ResultsProviderFarmUserException {
        if (!QUERY_MODEL_NAME.equals(queryModelName)) {
            return null;
        }

        QueryModel model = queryModelActions.get(queryModelName);

        if (!model.isSortable() && sortInfo != null) {
            throw new SortNotSupportedException();
        }

        PagedDocumentsProvider provider = model.getResultsProvider(
                documentManager, null, sortInfo);
        provider.setName(queryModelName);

        // CB: DAM-235 - On a page, first asset must be always selected
        DocumentModelList currentPage = provider.getCurrentPage();
        if (currentPage != null && !currentPage.isEmpty()) {
            documentActions.setCurrentSelection(currentPage.get(0));
        } else {
            // Nothing selected
            documentActions.setCurrentSelection(null);
        }

        return provider;
    }

    @Observer(EventNames.QUERY_MODEL_CHANGED)
    public void queryModelChanged(QueryModel qm) {
        resultsProvidersCache.invalidate(qm.getDescriptor().getName());
    }

    @Observer(EventNames.DOCUMENT_CHILDREN_CHANGED)
    public void invalidateProvider() {
        resultsProvidersCache.invalidate(QUERY_MODEL_NAME);
    }

    public void clearFilters() throws ClientException {
        //CB: DAM-281 - Clear filters
        filterDocument = null;
        queryModelActions.get(QUERY_MODEL_NAME).reset();
        invalidateProvider();
    }

}
