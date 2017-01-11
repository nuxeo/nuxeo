/*
 * (C) Copyright 2016 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Contributors:
 *     Gabriel Barata <gbarata@nuxeo.com>
 */
package org.nuxeo.ecm.restapi.server.jaxrs.search;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang3.EnumUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.jackson.map.ObjectMapper;
import org.nuxeo.ecm.automation.core.util.DocumentHelper;
import org.nuxeo.ecm.automation.core.util.Properties;
import org.nuxeo.ecm.automation.jaxrs.io.documents.PaginableDocumentModelListImpl;
import org.nuxeo.ecm.automation.server.jaxrs.RestOperationException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.SortInfo;
import org.nuxeo.ecm.core.api.impl.SimpleDocumentModel;
import org.nuxeo.ecm.core.api.model.PropertyNotFoundException;
import org.nuxeo.ecm.platform.query.api.PageProvider;
import org.nuxeo.ecm.platform.query.api.PageProviderDefinition;
import org.nuxeo.ecm.platform.query.api.PageProviderService;
import org.nuxeo.ecm.platform.query.api.QuickFilter;
import org.nuxeo.ecm.platform.query.nxql.CoreQueryDocumentPageProvider;
import org.nuxeo.ecm.restapi.server.jaxrs.adapters.SearchAdapter;
import org.nuxeo.ecm.webengine.model.impl.AbstractResource;
import org.nuxeo.ecm.webengine.model.impl.ResourceTypeImpl;
import org.nuxeo.runtime.api.Framework;

/**
 * @since 8.3
 */
public abstract class QueryExecutor extends AbstractResource<ResourceTypeImpl> {

    public static final String NXQL = "NXQL";

    public static final String QUERY = "query";

    public static final String PAGE_SIZE = "pageSize";

    public static final String CURRENT_PAGE_INDEX = "currentPageIndex";

    public static final String MAX_RESULTS = "maxResults";

    public static final String SORT_BY = "sortBy";

    public static final String SORT_ORDER = "sortOrder";

    public static final String ORDERED_PARAMS = "queryParams";

    /**
     * @since 8.4
     */
    public static final String QUICK_FILTERS = "quickFilters";

    /**
     * @since 9.1
     */
    public static final String HIGHLIGHT = "highlight";

    public static final String CURRENT_USERID_PATTERN = "$currentUser";

    public static final String CURRENT_REPO_PATTERN = "$currentRepository";

    public enum QueryParams {
        PAGE_SIZE, CURRENT_PAGE_INDEX, MAX_RESULTS, SORT_BY, SORT_ORDER, ORDERED_PARAMS, QUERY
    }

    public enum LangParams {
        NXQL
    }

    protected PageProviderService pageProviderService;

    private static final Log log = LogFactory.getLog(SearchObject.class);

    public void initExecutor() {
        pageProviderService = Framework.getService(PageProviderService.class);
    }

    protected String getQuery(MultivaluedMap<String, String> queryParams) {
        String query = queryParams.getFirst(QUERY);
        if (query == null) {
            query = "SELECT * FROM Document";
        }
        return query;
    }

    protected Long getCurrentPageIndex(MultivaluedMap<String, String> queryParams) {
        String currentPageIndex = queryParams.getFirst(CURRENT_PAGE_INDEX);
        if (currentPageIndex != null && !currentPageIndex.isEmpty()) {
            return Long.valueOf(currentPageIndex);
        }
        return null;
    }

    protected Long getPageSize(MultivaluedMap<String, String> queryParams) {
        String pageSize = queryParams.getFirst(PAGE_SIZE);
        if (pageSize != null && !pageSize.isEmpty()) {
            return Long.valueOf(pageSize);
        }
        return null;
    }

    protected Long getMaxResults(MultivaluedMap<String, String> queryParams) {
        String maxResults = queryParams.getFirst(MAX_RESULTS);
        if (maxResults != null && !maxResults.isEmpty()) {
            return Long.valueOf(maxResults);
        }
        return null;
    }

    protected List<SortInfo> getSortInfo(MultivaluedMap<String, String> queryParams) {
        String sortBy = queryParams.getFirst(SORT_BY);
        String sortOrder = queryParams.getFirst(SORT_ORDER);
        List<SortInfo> sortInfoList = new ArrayList<>();
        if (!StringUtils.isBlank(sortBy)) {
            String[] sorts = sortBy.split(",");
            String[] orders = null;
            if (!StringUtils.isBlank(sortOrder)) {
                orders = sortOrder.split(",");
            }
            for (int i = 0; i < sorts.length; i++) {
                String sort = sorts[i];
                boolean sortAscending = (orders != null && orders.length > i && "asc".equals(orders[i].toLowerCase()));
                sortInfoList.add(new SortInfo(sort, sortAscending));
            }
        }
        return sortInfoList;
    }

    protected List<SortInfo> getSortInfo(String sortBy, String sortOrder) {
        List<SortInfo> sortInfoList = new ArrayList<>();
        if (!StringUtils.isBlank(sortBy)) {
            String[] sorts = sortBy.split(",");
            String[] orders = null;
            if (!StringUtils.isBlank(sortOrder)) {
                orders = sortOrder.split(",");
            }
            for (int i = 0; i < sorts.length; i++) {
                String sort = sorts[i];
                boolean sortAscending = (orders != null && orders.length > i && "asc".equals(orders[i].toLowerCase()));
                sortInfoList.add(new SortInfo(sort, sortAscending));
            }
        }
        return sortInfoList;
    }

    /**
     * @since 8.4
     */
    protected List<QuickFilter> getQuickFilters(String providerName, MultivaluedMap<String, String> queryParams) {
        PageProviderDefinition pageProviderDefinition = pageProviderService.getPageProviderDefinition(providerName);
        String quickFilters = queryParams.getFirst(QUICK_FILTERS);
        List<QuickFilter> quickFilterList = new ArrayList<>();
        if (!StringUtils.isBlank(quickFilters)) {
            String[] filters = quickFilters.split(",");
            List<QuickFilter> ppQuickFilters = pageProviderDefinition.getQuickFilters();
            for (String filter : filters) {
                for (QuickFilter quickFilter : ppQuickFilters) {
                    if (quickFilter.getName().equals(filter)) {
                        quickFilterList.add(quickFilter);
                        break;
                    }
                }
            }
        }
        return quickFilterList;
    }

    protected List<String> getHighlights(MultivaluedMap<String, String> queryParams) {
        String highlight = queryParams.getFirst(HIGHLIGHT);
        List<String> highlightFields = new ArrayList<>();
        if (!StringUtils.isBlank(highlight)) {
            String[] fields = highlight.split(",");
            highlightFields = Arrays.asList(fields);
        }
        return highlightFields;
    }

    protected Properties getNamedParameters(MultivaluedMap<String, String> queryParams) {
        Properties namedParameters = new Properties();
        for (String namedParameterKey : queryParams.keySet()) {
            if (!EnumUtils.isValidEnum(QueryParams.class, namedParameterKey)) {
                String value = queryParams.getFirst(namedParameterKey);
                namedParameters.put(namedParameterKey, handleNamedParamVars(value));
            }
        }
        return namedParameters;
    }

    protected Properties getNamedParameters(Map<String, String> queryParams) {
        Properties namedParameters = new Properties();
        for (String namedParameterKey : queryParams.keySet()) {
            if (!EnumUtils.isValidEnum(QueryParams.class, namedParameterKey)) {
                String value = queryParams.get(namedParameterKey);
                namedParameters.put(namedParameterKey, handleNamedParamVars(value));
            }
        }
        return namedParameters;
    }

    protected String handleNamedParamVars(String value) {
        if (value != null) {
            if (value.equals(CURRENT_USERID_PATTERN)) {
                return ctx.getCoreSession().getPrincipal().getName();
            } else if (value.equals(CURRENT_REPO_PATTERN)) {
                return ctx.getCoreSession().getRepositoryName();
            }
        }
        return value;
    }

    protected Object[] getParameters(MultivaluedMap<String, String> queryParams) {
        List<String> orderedParams = queryParams.get(ORDERED_PARAMS);
        if (orderedParams != null && !orderedParams.isEmpty()) {
            Object[] parameters = orderedParams.toArray(new String[orderedParams.size()]);
            // expand specific parameters
            replaceParameterPattern(parameters);
            return parameters;
        }
        return null;
    }

    protected Object[] replaceParameterPattern(Object[] parameters) {
        for (int idx = 0; idx < parameters.length; idx++) {
            String value = (String) parameters[idx];
            if (value.equals(CURRENT_USERID_PATTERN)) {
                parameters[idx] = ctx.getCoreSession().getPrincipal().getName();
            } else if (value.equals(CURRENT_REPO_PATTERN)) {
                parameters[idx] = ctx.getCoreSession().getRepositoryName();
            }
        }
        return parameters;
    }

    protected Map<String, Serializable> getProperties() {
        Map<String, Serializable> props = new HashMap<String, Serializable>();
        props.put(CoreQueryDocumentPageProvider.CORE_SESSION_PROPERTY, (Serializable) ctx.getCoreSession());
        return props;
    }

    protected DocumentModelList queryByLang(String queryLanguage, MultivaluedMap<String, String> queryParams)
            throws RestOperationException {
        if (queryLanguage == null || !EnumUtils.isValidEnum(LangParams.class, queryLanguage)) {
            throw new RestOperationException("invalid query language", HttpServletResponse.SC_BAD_REQUEST);
        }

        String query = getQuery(queryParams);
        Long pageSize = getPageSize(queryParams);
        Long currentPageIndex = getCurrentPageIndex(queryParams);
        Long maxResults = getMaxResults(queryParams);
        Properties namedParameters = getNamedParameters(queryParams);
        Object[] parameters = getParameters(queryParams);
        List<SortInfo> sortInfo = getSortInfo(queryParams);
        Map<String, Serializable> props = getProperties();

        DocumentModel searchDocumentModel = getSearchDocumentModel(ctx.getCoreSession(), pageProviderService, null,
                namedParameters);

        return queryByLang(query, pageSize, currentPageIndex, maxResults, sortInfo, parameters, props,
                searchDocumentModel);
    }

    protected DocumentModelList queryByPageProvider(String pageProviderName, MultivaluedMap<String, String> queryParams)
            throws RestOperationException {
        if (pageProviderName == null) {
            throw new RestOperationException("invalid page provider name", HttpServletResponse.SC_BAD_REQUEST);
        }

        Long pageSize = getPageSize(queryParams);
        Long currentPageIndex = getCurrentPageIndex(queryParams);
        Properties namedParameters = getNamedParameters(queryParams);
        Object[] parameters = getParameters(queryParams);
        List<SortInfo> sortInfo = getSortInfo(queryParams);
        List<QuickFilter> quickFilters = getQuickFilters(pageProviderName, queryParams);
        List<String> highlights = getHighlights(queryParams);
        Map<String, Serializable> props = getProperties();

        DocumentModel searchDocumentModel = getSearchDocumentModel(ctx.getCoreSession(), pageProviderService,
                pageProviderName, namedParameters);

        return queryByPageProvider(pageProviderName, pageSize, currentPageIndex, sortInfo, highlights, quickFilters,
                parameters, props, searchDocumentModel);
    }

    protected DocumentModelList queryByLang(String query, Long pageSize, Long currentPageIndex, Long maxResults,
            List<SortInfo> sortInfo, Object[] parameters, Map<String, Serializable> props,
            DocumentModel searchDocumentModel) throws RestOperationException {
        PageProviderDefinition ppdefinition = pageProviderService.getPageProviderDefinition(
                SearchAdapter.pageProviderName);
        ppdefinition.setPattern(query);
        if (maxResults != null && maxResults != -1) {
            // set the maxResults to avoid slowing down queries
            ppdefinition.getProperties().put("maxResults", maxResults.toString());
        }
        PaginableDocumentModelListImpl res = new PaginableDocumentModelListImpl(
                (PageProvider<DocumentModel>) pageProviderService.getPageProvider(SearchAdapter.pageProviderName,
                        ppdefinition, searchDocumentModel, sortInfo, pageSize, currentPageIndex, props, parameters),
                null);

        if (res.hasError()) {
            RestOperationException err = new RestOperationException(res.getErrorMessage());
            err.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            throw err;
        }
        return res;
    }

    /**
     * @since 8.4
     */
    protected DocumentModelList queryByPageProvider(String pageProviderName, Long pageSize, Long currentPageIndex,
            List<SortInfo> sortInfo, List<QuickFilter> quickFilters, Object[] parameters,
            Map<String, Serializable> props, DocumentModel searchDocumentModel) throws RestOperationException {
        return queryByPageProvider(pageProviderName, pageSize, currentPageIndex, sortInfo, null, quickFilters,
                parameters, props, searchDocumentModel);
    }

    protected DocumentModelList queryByPageProvider(String pageProviderName, Long pageSize, Long currentPageIndex,
            List<SortInfo> sortInfo, List<String> highlights, List<QuickFilter> quickFilters, Object[] parameters,
            Map<String, Serializable> props, DocumentModel searchDocumentModel) throws RestOperationException {
        PaginableDocumentModelListImpl res = new PaginableDocumentModelListImpl(
                (PageProvider<DocumentModel>) pageProviderService.getPageProvider(pageProviderName, searchDocumentModel,
                        sortInfo, pageSize, currentPageIndex, props, quickFilters, highlights, parameters),
                null);
        if (res.hasError()) {
            RestOperationException err = new RestOperationException(res.getErrorMessage());
            err.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            throw err;
        }
        return res;
    }

    protected PageProviderDefinition getPageProviderDefinition(String pageProviderName) throws IOException {
        return pageProviderService.getPageProviderDefinition(pageProviderName);
    }

    protected DocumentModel getSearchDocumentModel(CoreSession session, PageProviderService pps, String providerName,
            Properties namedParameters) {
        // generate search document model if type specified on the definition
        DocumentModel searchDocumentModel = null;
        if (!StringUtils.isBlank(providerName)) {
            PageProviderDefinition pageProviderDefinition = pps.getPageProviderDefinition(providerName);
            if (pageProviderDefinition != null) {
                String searchDocType = pageProviderDefinition.getSearchDocumentType();
                if (searchDocType != null) {
                    searchDocumentModel = session.createDocumentModel(searchDocType);
                } else if (pageProviderDefinition.getWhereClause() != null) {
                    // avoid later error on null search doc, in case where clause is only referring to named parameters
                    // (and no namedParameters are given)
                    searchDocumentModel = new SimpleDocumentModel();
                }
            } else {
                log.error("No page provider definition found for " + providerName);
            }
        }

        if (namedParameters != null && !namedParameters.isEmpty()) {
            // fall back on simple document if no type defined on page provider
            if (searchDocumentModel == null) {
                searchDocumentModel = new SimpleDocumentModel();
            }
            for (Map.Entry<String, String> entry : namedParameters.entrySet()) {
                String key = entry.getKey();
                String value = entry.getValue();
                try {
                    DocumentHelper.setProperty(session, searchDocumentModel, key, value, true);
                } catch (PropertyNotFoundException | IOException e) {
                    // assume this is a "pure" named parameter, not part of the search doc schema
                    continue;
                }
            }
            searchDocumentModel.putContextData(PageProviderService.NAMED_PARAMETERS, namedParameters);
        }
        return searchDocumentModel;
    }

    protected Response buildResponse(Response.StatusType status, String type, Object object) throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        String message = mapper.writeValueAsString(object);
        return Response.status(status)
                       .header("Content-Length", message.getBytes("UTF-8").length)
                       .type(type + "; charset=UTF-8")
                       .entity(message)
                       .build();
    }

}
