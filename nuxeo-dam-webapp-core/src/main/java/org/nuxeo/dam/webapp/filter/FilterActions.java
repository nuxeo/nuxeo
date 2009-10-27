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
import org.nuxeo.ecm.webapp.querymodel.QueryModelActions;
import org.nuxeo.runtime.api.Framework;

@Scope(CONVERSATION)
@Name("filterActions")
public class FilterActions implements Serializable, ResultsProviderFarm {

	private static final long serialVersionUID = 1L;

	@SuppressWarnings("unused")
	private static final Log log = LogFactory.getLog(FilterActions.class);

	public static final List<String> DAM_DOCUMENT_TYPES = Arrays.asList("File",
			"Picture", "Video", "Audio");

	protected static final String QUERY_MODEL_NAME = "FILTERED_DOCUMENTS";

	protected static final String DOCTYPE_FIELD_XPATH = "filter_query:ecm_primaryType";

	@In(create = true, required = false)
	protected transient CoreSession documentManager;

    @In(create = true)
    protected transient QueryModelActions queryModelActions;

	@In(create = true, required = false)
	transient ResultsProvidersCache resultsProvidersCache;

	@RequestParameter
	protected String docType;

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
		items.add(new SelectItem("All", "label.type.All", "", docTypeSelection
				.isEmpty()));
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
					// back to empty selection which means no document type filtering:
					previousSelection.clear();
				}
			}
		}
		filterDocument.setPropertyValue(DOCTYPE_FIELD_XPATH,
				(Serializable) previousSelection);
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
		return provider;
	}

	@Observer(EventNames.QUERY_MODEL_CHANGED)
	public void queryModelChanged(QueryModel qm) {
		resultsProvidersCache.invalidate(qm.getDescriptor().getName());
	}

	public void invalidateProvider() {
		resultsProvidersCache.invalidate(QUERY_MODEL_NAME);
	}

}
