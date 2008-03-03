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

package org.nuxeo.ecm.core.search.api.client.querymodel;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.DataModel;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.ecm.core.api.PagedDocumentsProvider;
import org.nuxeo.ecm.core.api.SortInfo;
import org.nuxeo.ecm.core.query.QueryParseException;
import org.nuxeo.ecm.core.query.sql.SQLQueryParser;
import org.nuxeo.ecm.core.search.api.client.SearchException;
import org.nuxeo.ecm.core.search.api.client.SearchService;
import org.nuxeo.ecm.core.search.api.client.common.SearchServiceDelegate;
import org.nuxeo.ecm.core.search.api.client.query.QueryException;
import org.nuxeo.ecm.core.search.api.client.query.SearchPrincipal;
import org.nuxeo.ecm.core.search.api.client.query.impl.ComposedNXQueryImpl;
import org.nuxeo.ecm.core.search.api.client.querymodel.descriptor.FieldDescriptor;
import org.nuxeo.ecm.core.search.api.client.querymodel.descriptor.QueryModelDescriptor;
import org.nuxeo.ecm.core.search.api.client.search.results.ResultSet;
import org.nuxeo.ecm.core.search.api.client.search.results.document.SearchPageProvider;
import org.nuxeo.runtime.api.Framework;

public class QueryModel implements Serializable {

    private static final long serialVersionUID = 762348097532723566L;

    private static final Log log = LogFactory.getLog(QueryModel.class);

    protected transient QueryModelDescriptor descriptor;

    private String descriptorName;

    protected final DocumentModel documentModel;

    protected final Map<String, Map<String, Object>> defaultValues;

    protected final SearchPrincipal principal;

    protected transient QueryModelService queryModelService;

    protected transient SearchService searchService;

    public QueryModel(QueryModelDescriptor descriptor,
            DocumentModel documentModel, NuxeoPrincipal principal) {
        this.descriptor = descriptor;
        if (descriptor != null) {
            descriptorName = descriptor.getName();
        }

        this.documentModel = documentModel;

        // store default values to be able to reset the DocumentModel offline
        defaultValues = new HashMap<String, Map<String, Object>>();

        if (documentModel != null) {
            for (DataModel dm : documentModel.getDataModels().values()) {
                defaultValues.put(dm.getSchema(), new HashMap<String, Object>(
                        dm.getMap()));
            }
        }

        lookupSearchService();
        if (principal == null) {
            this.principal = null;
        } else {
            this.principal = searchService.getSearchPrincipal(principal);
        }
    }

    public QueryModel(QueryModelDescriptor descriptor, NuxeoPrincipal principal) {
        this(descriptor, null, principal);
    }

    public DocumentModel getDocumentModel() {
        return documentModel;
    }

    public DocumentModelList getDocuments() throws ClientException,
            QueryException {
        return getDocuments(null);
    }

    public DocumentModelList getDocuments(Object[] params)
            throws ClientException, QueryException {
        return getResultsProvider(params).getCurrentPage();
    }

    private SearchPrincipal getSearchPrincipal() {
        return principal;
    }

    private void lookupSearchService() {
        if (searchService == null) {
            searchService = SearchServiceDelegate.getRemoteSearchService();
        }
    }

    /**
     * Used to reconstruct the descriptor after a ser/de-serialization cycle
     */
    private void checkDescriptor() {
        if (descriptor == null) {
            if (queryModelService == null) {
                queryModelService = (QueryModelService) Framework.getRuntime().getComponent(
                        QueryModelService.NAME);
            }
            descriptor = queryModelService.getQueryModelDescriptor(descriptorName);
        }
    }

    public PagedDocumentsProvider getResultsProvider(Object[] params)
            throws ClientException, QueryException {
        return getResultsProvider(params, null);
    }

    public PagedDocumentsProvider getResultsProvider(Object[] params,
            SortInfo sortInfo) throws ClientException, QueryException {
        lookupSearchService();
        checkDescriptor();
        if (searchService == null) {
            log.warn("Cannot find Search Service");
            return null;
        }
        Integer max = descriptor.getMax();

        if (sortInfo == null) {
            sortInfo = descriptor.getDefaultSortInfo(documentModel);
        }

        String query;
        if (descriptor.isStateful()) {
            query = descriptor.getQuery(documentModel, sortInfo);
        } else {
            query = descriptor.getQuery(params, sortInfo);
        }

        if (max == null) {
            max = Integer.MAX_VALUE;
        }
        try {
            if (log.isDebugEnabled()) {
                log.debug("execute query: " + query.replace('\n', ' '));
            }
            ResultSet resultSet = searchService.searchQuery(
                    new ComposedNXQueryImpl(SQLQueryParser.parse(query),
                            principal), 0, max);
            return new SearchPageProvider(resultSet, isSortable(), sortInfo,
                    query);
        } catch (Exception e) {
            // re-run the query without sort parameters
            try {
                log.error("sorted query failed: " + query);
                log.debug("sorted query failed", e);
                if (descriptor.isStateful()) {
                    query = descriptor.getQuery(documentModel, null);
                } else {
                    query = descriptor.getQuery(params, null);
                }
                log.debug("re-execute query without sort: " + query);
                ResultSet resultSet = searchService.searchQuery(
                        new ComposedNXQueryImpl(SQLQueryParser.parse(query),
                                principal), 0, max);
                return new SearchPageProvider(resultSet, isSortable(), null,
                        query);
            } catch (SearchException e2) {
                throw new ClientException(String.format(
                        "%s in search by QueryModel %s", e2.getMessage(),
                        descriptor.getName()), e2);
            } catch (QueryParseException e3) {
                throw new QueryException("Invalid query syntax", e3);
            }
        }
    }

    public QueryModelDescriptor getDescriptor() {
        checkDescriptor();
        return descriptor;
    }

    /*
     * Convenience API
     */

    public Object getProperty(String schemaName, String name) {
        return documentModel.getProperty(schemaName, name);
    }

    public void setProperty(String schemaName, String name, Object value) {
        documentModel.setProperty(schemaName, name, value);
    }

    public void setSortColumn(String value) {
        FieldDescriptor fd = getDescriptor().getSortColumnField();
        setProperty(fd.getSchema(), fd.getName(), value);
    }

    public String getSortColumn() {
        FieldDescriptor fd = getDescriptor().getSortColumnField();
        return (String) getProperty(fd.getSchema(), fd.getName());
    }

    public boolean getSortAscending() {
        FieldDescriptor fd = getDescriptor().getSortAscendingField();
        Boolean result = (Boolean) getProperty(fd.getSchema(), fd.getName());
        return Boolean.TRUE.equals(result);
    }

    public void setSortAscending(boolean sortAscending) {
        FieldDescriptor fd = getDescriptor().getSortAscendingField();
        setProperty(fd.getSchema(), fd.getName(), sortAscending);
    }

    public boolean isSortable() {
        return getDescriptor().isSortable();
    }

    public void reset() throws ClientException {
        for (String schemaName : defaultValues.keySet()) {
            Map<String, Object> defaultData = new HashMap<String, Object>(
                    defaultValues.get(schemaName));
            documentModel.setProperties(schemaName, defaultData);
        }
    }

}
