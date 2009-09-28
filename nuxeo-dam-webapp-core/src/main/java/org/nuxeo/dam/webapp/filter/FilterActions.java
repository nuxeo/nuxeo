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
 *     Quentin Lamerand
 *
 * $Id$
 */

package org.nuxeo.dam.webapp.filter;

import static org.jboss.seam.ScopeType.CONVERSATION;

import java.io.Serializable;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Observer;
import org.jboss.seam.annotations.Scope;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.PagedDocumentsProvider;
import org.nuxeo.ecm.core.api.SortInfo;
import org.nuxeo.ecm.core.search.api.client.querymodel.QueryModel;
import org.nuxeo.ecm.core.search.api.client.querymodel.QueryModelService;
import org.nuxeo.ecm.core.search.api.client.querymodel.descriptor.QueryModelDescriptor;
import org.nuxeo.ecm.platform.ui.web.api.ResultsProviderFarm;
import org.nuxeo.ecm.platform.ui.web.api.SortNotSupportedException;
import org.nuxeo.ecm.platform.ui.web.pagination.ResultsProviderFarmUserException;
import org.nuxeo.ecm.webapp.helpers.EventNames;
import org.nuxeo.ecm.webapp.pagination.ResultsProvidersCache;
import org.nuxeo.runtime.api.Framework;

@Scope(CONVERSATION)
@Name("filterActions")
public class FilterActions implements Serializable, ResultsProviderFarm {

    private static final long serialVersionUID = 8713355502550622010L;

    @SuppressWarnings("unused")
    private static final Log log = LogFactory.getLog(FilterActions.class);

    protected static final String QUERY_MODEL_NAME = "FILTERED_DOCUMENTS";

    @In(create = true, required = false)
    transient CoreSession documentManager;

    @In(create = true, required = false)
    transient ResultsProvidersCache resultsProvidersCache;

    protected transient QueryModelService queryModelService;

    protected DocumentModel filterDocument;

    public DocumentModel getFilterDocument() throws ClientException {
        if (filterDocument == null) {
            QueryModelDescriptor descriptor;
            try {
                descriptor = getQueryModelDescriptor(QUERY_MODEL_NAME);
            } catch (Exception e) {
                throw new ClientException("Failed to get query model: " + QUERY_MODEL_NAME, e);
            }
            filterDocument = documentManager.createDocumentModel(descriptor.getDocType());
        }
        return filterDocument;
    }

    public void setFilterDocument(DocumentModel filterDocument) {
        this.filterDocument = filterDocument;
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

        QueryModelDescriptor descriptor;
        try {
            descriptor = getQueryModelDescriptor(queryModelName);
        } catch (Exception e) {
            throw new ClientException("Failed to get query model: " + queryModelName, e);
        }

        QueryModel model = new QueryModel(descriptor, getFilterDocument());

        if (!descriptor.isSortable() && sortInfo != null) {
            throw new SortNotSupportedException();
        }

        PagedDocumentsProvider provider = model.getResultsProvider(
                documentManager, null, sortInfo);
        provider.setName(queryModelName);
        return provider;
    }

    @Observer(EventNames.QUERY_MODEL_CHANGED)
    public void queryModelChanged(QueryModel qm) {
        resultsProvidersCache.invalidate(qm.getDescriptor().getName());
    }
    
    public void invalidateProvider() {
        resultsProvidersCache.invalidate(QUERY_MODEL_NAME);
    }

    protected QueryModelDescriptor getQueryModelDescriptor(String descriptorName)
            throws Exception {
        if (queryModelService == null) {
            queryModelService = (QueryModelService) Framework.getService(QueryModelService.class);
        }
        return queryModelService.getQueryModelDescriptor(descriptorName);
    }

}
