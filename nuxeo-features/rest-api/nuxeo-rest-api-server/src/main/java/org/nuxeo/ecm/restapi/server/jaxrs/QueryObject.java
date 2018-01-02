/*
 * (C) Copyright 2014-2018 Nuxeo (http://nuxeo.com/) and others.
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
 *     Vladimir Pasquier <vpasquier@nuxeo.com>
 */
package org.nuxeo.ecm.restapi.server.jaxrs;

import static javax.servlet.http.HttpServletResponse.SC_BAD_REQUEST;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.UriInfo;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.automation.core.util.DocumentHelper;
import org.nuxeo.ecm.automation.core.util.Properties;
import org.nuxeo.ecm.automation.jaxrs.io.documents.PaginableDocumentModelListImpl;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.core.api.SortInfo;
import org.nuxeo.ecm.core.api.impl.SimpleDocumentModel;
import org.nuxeo.ecm.core.api.model.PropertyNotFoundException;
import org.nuxeo.ecm.platform.query.api.PageProvider;
import org.nuxeo.ecm.platform.query.api.PageProviderDefinition;
import org.nuxeo.ecm.platform.query.api.PageProviderService;
import org.nuxeo.ecm.platform.query.api.QuickFilter;
import org.nuxeo.ecm.platform.query.nxql.CoreQueryDocumentPageProvider;
import org.nuxeo.ecm.restapi.server.jaxrs.adapters.SearchAdapter;
import org.nuxeo.ecm.webengine.model.WebObject;
import org.nuxeo.ecm.webengine.model.impl.AbstractResource;
import org.nuxeo.ecm.webengine.model.impl.ResourceTypeImpl;
import org.nuxeo.runtime.api.Framework;

/**
 * @since 6.0 Search endpoint to perform queries on the repository through rest api.
 */
@WebObject(type = "query")
public class QueryObject extends AbstractResource<ResourceTypeImpl> {

    public static final String PATH = "query";

    public static final String NXQL = "NXQL";

    public static final String QUERY = "query";

    public static final String PAGE_SIZE = "pageSize";

    public static final String CURRENT_PAGE_INDEX = "currentPageIndex";

    public static final String MAX_RESULTS = "maxResults";

    public static final String SORT_BY = "sortBy";

    public static final String SORT_ORDER = "sortOrder";

    public static final String ORDERED_PARAMS = "queryParams";

    public static final String CURRENT_USERID_PATTERN = "$currentUser";

    public static final String CURRENT_REPO_PATTERN = "$currentRepository";

    /**
     * @since 8.4
     */
    public static final String QUICK_FILTERS = "quickFilters";

    private static final Log log = LogFactory.getLog(QueryObject.class);

    protected EnumMap<QueryParams, String> queryParametersMap;

    protected EnumMap<LangParams, String> langPathMap;

    protected PageProviderService pageProviderService;

    @Override
    public void initialize(Object... args) {
        pageProviderService = Framework.getService(PageProviderService.class);
        // Query Enum Parameters Map
        queryParametersMap = new EnumMap<>(QueryParams.class);
        queryParametersMap.put(QueryParams.PAGE_SIZE, PAGE_SIZE);
        queryParametersMap.put(QueryParams.CURRENT_PAGE_INDEX, CURRENT_PAGE_INDEX);
        queryParametersMap.put(QueryParams.MAX_RESULTS, MAX_RESULTS);
        queryParametersMap.put(QueryParams.SORT_BY, SORT_BY);
        queryParametersMap.put(QueryParams.SORT_ORDER, SORT_ORDER);
        queryParametersMap.put(QueryParams.QUERY, QUERY);
        queryParametersMap.put(QueryParams.ORDERED_PARAMS, ORDERED_PARAMS);
        queryParametersMap.put(QueryParams.QUICK_FILTERS, QUICK_FILTERS);
        // Lang Path Enum Map
        langPathMap = new EnumMap<>(LangParams.class);
        langPathMap.put(LangParams.NXQL, NXQL);
    }

    @SuppressWarnings("unchecked")
    protected DocumentModelList getQuery(UriInfo uriInfo, String langOrProviderName) {
        // Fetching all parameters
        MultivaluedMap<String, String> queryParams = uriInfo.getQueryParameters();
        // Look if provider name is given
        String providerName = null;
        if (!langPathMap.containsValue(langOrProviderName)) {
            providerName = langOrProviderName;
        }
        String query = queryParams.getFirst(QUERY);
        String pageSize = queryParams.getFirst(PAGE_SIZE);
        String currentPageIndex = queryParams.getFirst(CURRENT_PAGE_INDEX);
        String maxResults = queryParams.getFirst(MAX_RESULTS);
        String sortBy = queryParams.getFirst(SORT_BY);
        String sortOrder = queryParams.getFirst(SORT_ORDER);
        List<String> orderedParams = queryParams.get(ORDERED_PARAMS);
        String quickFilters = queryParams.getFirst(QUICK_FILTERS);

        // If no query or provider name has been found
        // Execute big select
        if (query == null && StringUtils.isBlank(providerName)) {
            // provide a defaut query
            query = "SELECT * from Document";
        }

        // Fetching named parameters (other custom query parameters in the
        // path)
        Properties namedParameters = new Properties();
        for (String namedParameterKey : queryParams.keySet()) {
            if (!queryParametersMap.containsValue(namedParameterKey)) {
                String value = queryParams.getFirst(namedParameterKey);
                if (value != null) {
                    if (value.equals(CURRENT_USERID_PATTERN)) {
                        value = ctx.getCoreSession().getPrincipal().getName();
                    } else if (value.equals(CURRENT_REPO_PATTERN)) {
                        value = ctx.getCoreSession().getRepositoryName();
                    }
                }
                namedParameters.put(namedParameterKey, value);
            }
        }

        // Target query page
        Long targetPage = null;
        if (currentPageIndex != null) {
            targetPage = Long.valueOf(currentPageIndex);
        }

        // Target page size
        Long targetPageSize = null;
        if (pageSize != null) {
            targetPageSize = Long.valueOf(pageSize);
        }

        // Ordered Parameters
        Object[] parameters = null;
        if (orderedParams != null && !orderedParams.isEmpty()) {
            parameters = orderedParams.toArray(new String[orderedParams.size()]);
            // expand specific parameters
            for (int idx = 0; idx < parameters.length; idx++) {
                String value = (String) parameters[idx];
                if (value.equals(CURRENT_USERID_PATTERN)) {
                    parameters[idx] = ctx.getCoreSession().getPrincipal().getName();
                } else if (value.equals(CURRENT_REPO_PATTERN)) {
                    parameters[idx] = ctx.getCoreSession().getRepositoryName();
                }
            }
        }

        Map<String, Serializable> props = new HashMap<>();
        props.put(CoreQueryDocumentPageProvider.CORE_SESSION_PROPERTY, (Serializable) ctx.getCoreSession());

        DocumentModel searchDocumentModel = getSearchDocumentModel(ctx.getCoreSession(), pageProviderService,
                providerName, namedParameters);

        // Sort Info Management
        List<SortInfo> sortInfoList = null;
        if (!StringUtils.isBlank(sortBy)) {
            sortInfoList = new ArrayList<>();
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

        PaginableDocumentModelListImpl res;
        if (query != null) {
            PageProviderDefinition ppdefinition = pageProviderService.getPageProviderDefinition(
                    SearchAdapter.pageProviderName);
            ppdefinition.setPattern(query);
            if (maxResults != null && !maxResults.isEmpty() && !maxResults.equals("-1")) {
                // set the maxResults to avoid slowing down queries
                ppdefinition.getProperties().put("maxResults", maxResults);
            }
            if (StringUtils.isBlank(providerName)) {
                providerName = SearchAdapter.pageProviderName;
            }

            res = new PaginableDocumentModelListImpl(
                    (PageProvider<DocumentModel>) pageProviderService.getPageProvider(providerName, ppdefinition,
                            searchDocumentModel, sortInfoList, targetPageSize, targetPage, props, parameters),
                    null);
        } else {
            PageProviderDefinition pageProviderDefinition = pageProviderService.getPageProviderDefinition(providerName);
            // Quick filters management
            List<QuickFilter> quickFilterList = new ArrayList<>();
            if (quickFilters != null && !quickFilters.isEmpty()) {
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
            res = new PaginableDocumentModelListImpl(
                    (PageProvider<DocumentModel>) pageProviderService.getPageProvider(providerName, searchDocumentModel,
                            sortInfoList, targetPageSize, targetPage, props, quickFilterList, parameters),
                    null);
        }
        if (res.hasError()) {
            throw new NuxeoException(res.getErrorMessage(), SC_BAD_REQUEST);
        }
        return res;
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

    /**
     * Perform query on the repository. By default in NXQL.
     *
     * @param uriInfo Query parameters
     * @return Document Listing
     */
    @GET
    public Object doQuery(@Context UriInfo uriInfo) {
        return getQuery(uriInfo, NXQL);
    }

    /**
     * Perform query on the repository in NXQL or specific pageprovider name
     *
     * @param uriInfo Query parameters
     * @param langOrProviderName NXQL or specific provider name
     * @return Document Listing
     */
    @GET
    @Path("{langOrProviderName}")
    public Object doSpecificQuery(@Context UriInfo uriInfo,
            @PathParam("langOrProviderName") String langOrProviderName) {
        return getQuery(uriInfo, langOrProviderName);
    }

    public enum QueryParams {
        PAGE_SIZE, CURRENT_PAGE_INDEX, MAX_RESULTS, SORT_BY, SORT_ORDER, ORDERED_PARAMS, QUERY, QUICK_FILTERS
    }

    public enum LangParams {
        NXQL
    }

}
