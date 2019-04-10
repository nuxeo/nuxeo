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

import java.io.Serializable;
import java.util.List;

import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.Factory;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Observer;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.contexts.Contexts;
import org.nuxeo.dam.webapp.helper.DamEventNames;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.PagedDocumentsProvider;
import org.nuxeo.ecm.platform.ui.web.model.SelectDataModel;
import org.nuxeo.ecm.platform.ui.web.model.SelectDataModelListener;
import org.nuxeo.ecm.platform.ui.web.model.impl.SelectDataModelImpl;
import org.nuxeo.ecm.platform.ui.web.model.impl.SelectDataModelRowEvent;
import org.nuxeo.ecm.webapp.documentsLists.DocumentsListsManager;
import org.nuxeo.ecm.webapp.helpers.EventNames;
import org.nuxeo.ecm.webapp.pagination.ResultsProvidersCache;

@Name("filterResults")
@Scope(ScopeType.CONVERSATION)
public class FilterResults implements SelectDataModelListener, Serializable {

    private static final long serialVersionUID = 1L;

    public static final String SEARCH_DOCUMENT_LIST = "SEARCH_DOCUMENT_LIST";

    public static final String FILTERED_DOCUMENT_PROVIDER_NAME = "FILTERED_DOCUMENTS";

    public static final String FILTER_SELECT_MODEL_NAME = "filterSelectModel";

    @In(required = false, create = true)
    protected transient DocumentsListsManager documentsListsManager;

    @In(required = false, create = true)
    protected transient ResultsProvidersCache resultsProvidersCache;

    protected transient PagedDocumentsProvider provider;

    @Factory(value = FILTER_SELECT_MODEL_NAME, scope = ScopeType.EVENT)
    public SelectDataModel getResultsSelectModelFiltered()
            throws ClientException {
        return getResultsSelectModel(FILTERED_DOCUMENT_PROVIDER_NAME);
    }

    public SelectDataModel getResultsSelectModel(String providerName)
            throws ClientException {
        if (providerName == null) {
            throw new ClientException("providerName has not been set yet");
        }
        List<DocumentModel> selectedDocuments = documentsListsManager.getWorkingList(DocumentsListsManager.CURRENT_DOCUMENT_SELECTION);
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

    @Observer(DamEventNames.IMPORTSET_CREATED)
    public void invalidateFilterResult() {
        documentsListsManager.resetWorkingList(DocumentsListsManager.CURRENT_DOCUMENT_SELECTION);
        resultsProvidersCache.invalidate(FILTERED_DOCUMENT_PROVIDER_NAME);
        provider = null;
    }

    @Observer(EventNames.DOCUMENT_CHILDREN_CHANGED)
    public void invalidateSelectDataModel() {
        Contexts.getEventContext().remove(FILTER_SELECT_MODEL_NAME);
    }

}
