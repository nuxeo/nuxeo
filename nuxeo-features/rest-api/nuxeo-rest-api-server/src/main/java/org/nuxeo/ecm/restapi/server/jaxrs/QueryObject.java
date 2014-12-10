/*
 * (C) Copyright 2014 Nuxeo SA (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl-2.1.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     Vladimir Pasquier <vpasquier@nuxeo.com>
 */
package org.nuxeo.ecm.restapi.server.jaxrs;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.UriInfo;

import org.apache.commons.lang.StringUtils;
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

    protected EnumMap<QueryParams, String> queryParametersMap;

    protected EnumMap<LangParams, String> langPathMap;

    protected PageProviderService pageProviderService;

    @Override
    public void initialize(Object... args) {
        pageProviderService = Framework.getLocalService(PageProviderService.class);
        // Query Enum Parameters Map
        queryParametersMap = new EnumMap<>(QueryParams.class);
        queryParametersMap.put(QueryParams.PAGE_SIZE, PAGE_SIZE);
        queryParametersMap.put(QueryParams.CURRENT_PAGE_INDEX, CURRENT_PAGE_INDEX);
        queryParametersMap.put(QueryParams.MAX_RESULTS, MAX_RESULTS);
        queryParametersMap.put(QueryParams.SORT_BY, SORT_BY);
        queryParametersMap.put(QueryParams.SORT_ORDER, SORT_ORDER);
        queryParametersMap.put(QueryParams.QUERY, QUERY);
        queryParametersMap.put(QueryParams.ORDERED_PARAMS, ORDERED_PARAMS);
        // Lang Path Enum Map
        langPathMap = new EnumMap<>(LangParams.class);
        langPathMap.put(LangParams.NXQL, NXQL);
    }

    @SuppressWarnings("unchecked")
    protected DocumentModelList getQuery(UriInfo uriInfo, String langOrProviderName) throws RestOperationException {
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
                namedParameters.put(namedParameterKey, queryParams.getFirst(namedParameterKey));
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

        Map<String, Serializable> props = new HashMap<String, Serializable>();
        props.put(CoreQueryDocumentPageProvider.CORE_SESSION_PROPERTY, (Serializable) ctx.getCoreSession());

        DocumentModel searchDocumentModel = getSearchDocumentModel(ctx.getCoreSession(), pageProviderService,
                providerName, namedParameters);

        // Sort Info Management
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

        PaginableDocumentModelListImpl res;
        if (query != null) {
            PageProviderDefinition ppdefinition = pageProviderService.getPageProviderDefinition(SearchAdapter.pageProviderName);
            ppdefinition.setPattern(query);
            if (maxResults != null && !maxResults.isEmpty() && !maxResults.equals("-1")) {
                // set the maxResults to avoid slowing down queries
                ppdefinition.getProperties().put("maxResults", maxResults);
            }
            res = new PaginableDocumentModelListImpl((PageProvider<DocumentModel>) pageProviderService.getPageProvider(
                    StringUtils.EMPTY, ppdefinition, searchDocumentModel, sortInfoList, targetPageSize, targetPage,
                    props, parameters), null);
        } else {
            res = new PaginableDocumentModelListImpl((PageProvider<DocumentModel>) pageProviderService.getPageProvider(
                    providerName, searchDocumentModel, sortInfoList, targetPageSize, targetPage, props, parameters),
                    null);
        }
        if (res.hasError()) {
            RestOperationException err = new RestOperationException(res.getErrorMessage());
            err.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            throw err;
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
    public Object doQuery(@Context UriInfo uriInfo) throws RestOperationException {
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
    public Object doSpecificQuery(@Context UriInfo uriInfo, @PathParam("langOrProviderName") String langOrProviderName)
            throws RestOperationException {
        return getQuery(uriInfo, langOrProviderName);
    }

    public enum QueryParams {
        PAGE_SIZE, CURRENT_PAGE_INDEX, MAX_RESULTS, SORT_BY, SORT_ORDER, ORDERED_PARAMS, QUERY
    }

    public enum LangParams {
        NXQL
    }

}
