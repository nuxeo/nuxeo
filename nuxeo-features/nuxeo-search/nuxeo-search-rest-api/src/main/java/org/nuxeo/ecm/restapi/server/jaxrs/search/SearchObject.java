/*
 * (C) Copyright 2016-2018 Nuxeo (http://nuxeo.com/) and others.
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

import static javax.servlet.http.HttpServletResponse.SC_BAD_REQUEST;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import org.apache.commons.lang3.EnumUtils;
import org.apache.commons.lang3.StringUtils;
import org.nuxeo.ecm.automation.core.util.DocumentHelper;
import org.nuxeo.ecm.automation.core.util.PageProviderHelper;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.core.api.SortInfo;
import org.nuxeo.ecm.core.api.model.Property;
import org.nuxeo.ecm.core.api.model.PropertyNotFoundException;
import org.nuxeo.ecm.platform.query.api.PageProvider;
import org.nuxeo.ecm.platform.query.api.PageProviderDefinition;
import org.nuxeo.ecm.platform.query.api.PageProviderService;
import org.nuxeo.ecm.platform.query.api.QuickFilter;
import org.nuxeo.ecm.platform.search.core.InvalidSearchParameterException;
import org.nuxeo.ecm.platform.search.core.SavedSearch;
import org.nuxeo.ecm.platform.search.core.SavedSearchConstants;
import org.nuxeo.ecm.platform.search.core.SavedSearchRequest;
import org.nuxeo.ecm.platform.search.core.SavedSearchService;
import org.nuxeo.ecm.webengine.model.WebObject;
import org.nuxeo.ecm.webengine.model.exceptions.IllegalParameterException;
import org.nuxeo.runtime.api.Framework;

/**
 * @since 8.3 Search endpoint to perform queries via rest api, with support to save and execute saved search queries.
 */
@WebObject(type = "search")
public class SearchObject extends QueryExecutor {

    public static final String SAVED_SEARCHES_PAGE_PROVIDER = "SAVED_SEARCHES_ALL";

    public static final String SAVED_SEARCHES_PAGE_PROVIDER_PARAMS = "SAVED_SEARCHES_ALL_PAGE_PROVIDER";

    public static final String PAGE_PROVIDER_NAME_PARAM = "pageProvider";

    protected SavedSearchService savedSearchService;

    @Override
    public void initialize(Object... args) {
        initExecutor();
        savedSearchService = Framework.getService(SavedSearchService.class);
    }

    /**
     * @deprecated since 10.3, use {@link #doQueryByLang(UriInfo)} instead.
     */
    @GET
    @Path("lang/{queryLanguage}/execute")
    @Deprecated
    public Object doQueryByLang(@Context UriInfo uriInfo, @PathParam("queryLanguage") String queryLanguage) {
        MultivaluedMap<String, String> queryParams = uriInfo.getQueryParameters();
        return queryByLang(queryLanguage, queryParams);
    }

    /**
     * @since 10.3
     */
    @GET
    @Path("execute")
    public Object doQueryByLang(@Context UriInfo uriInfo) {
        MultivaluedMap<String, String> queryParams = uriInfo.getQueryParameters();
        return queryByLang(queryParams);
    }

    /**
     * @deprecated since 10.3, use {@link #doBulkActionByLang(UriInfo)} instead.
     */
    @Path("lang/{queryLanguage}/bulk")
    @Deprecated
    public Object doBulkActionByLang(@Context UriInfo uriInfo, @PathParam("queryLanguage") String queryLanguage) {
        if (!EnumUtils.isValidEnum(LangParams.class, queryLanguage)) {
            throw new IllegalParameterException("invalid query language");
        }
        MultivaluedMap<String, String> queryParams = uriInfo.getQueryParameters();
        String query = getQueryString(null, queryParams);
        return newObject("bulkAction", query);
    }

    /**
     * @since 10.3
     */
    @Path("bulk")
    public Object doBulkActionByLang(@Context UriInfo uriInfo) {
        MultivaluedMap<String, String> queryParams = uriInfo.getQueryParameters();
        String query = getQueryString(null, queryParams);
        String scrollName = queryParams.getFirst(SCROLL_PARAM);
        String queryLimit = queryParams.getFirst(QUERY_LIMIT_PARAM);
        return newObject("bulkAction", query, scrollName, queryLimit);
    }

    @GET
    @Path("pp/{pageProviderName}/execute")
    public Object doQueryByPageProvider(@Context UriInfo uriInfo,
            @PathParam("pageProviderName") String pageProviderName) {
        MultivaluedMap<String, String> queryParams = uriInfo.getQueryParameters();
        return queryByPageProvider(pageProviderName, queryParams);
    }

    @GET
    @Path("pp/{pageProviderName}")
    public Object doGetPageProviderDefinition(@PathParam("pageProviderName") String pageProviderName)
            throws IOException {
        return buildResponse(Response.Status.OK, MediaType.APPLICATION_JSON,
                getPageProviderDefinition(pageProviderName));
    }

    @Path("pp/{pageProviderName}/bulk")
    public Object doBulkActionByPageProvider(@PathParam("pageProviderName") String pageProviderName,
            @Context UriInfo uriInfo) {
        MultivaluedMap<String, String> queryParams = uriInfo.getQueryParameters();
        PageProvider<?> pageProvider = getPageProvider(pageProviderName, queryParams);
        String query = getQueryString(pageProvider);
        String scrollName;
        String scrollParam = queryParams.getFirst(SCROLL_PARAM);
        if (StringUtils.isEmpty(scrollParam)) {
            // no scroll parameter, fall back on page provider type
            scrollName = Framework.getService(PageProviderService.class).getPageProviderType(pageProvider).toString();
        } else {
            scrollName = scrollParam;
        }
        String queryLimit = queryParams.getFirst(QUERY_LIMIT_PARAM);
        return newObject("bulkAction", query, scrollName, queryLimit);
    }

    @GET
    @Path("saved")
    public List<SavedSearch> doGetSavedSearches(@Context UriInfo uriInfo) {
        MultivaluedMap<String, String> queryParams = uriInfo.getQueryParameters();
        DocumentModelList results = queryParams.containsKey(PAGE_PROVIDER_NAME_PARAM)
                ? queryByPageProvider(SAVED_SEARCHES_PAGE_PROVIDER_PARAMS, queryParams)
                : queryByPageProvider(SAVED_SEARCHES_PAGE_PROVIDER, queryParams);
        List<SavedSearch> savedSearches = new ArrayList<>(results.size());
        for (DocumentModel doc : results) {
            savedSearches.add(doc.getAdapter(SavedSearch.class));
        }
        return savedSearches;
    }

    @POST
    @Path("saved")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response doSaveSearch(SavedSearchRequest request) {
        try {
            SavedSearch search = savedSearchService.createSavedSearch(ctx.getCoreSession(), request.getTitle(),
                    request.getQueryParams(), null, request.getQuery(), request.getQueryLanguage(),
                    request.getPageProviderName(), request.getPageSize(), request.getCurrentPageIndex(),
                    request.getMaxResults(), request.getSortBy(), request.getSortOrder(), request.getContentViewData());
            setSaveSearchParams(request.getNamedParams(), search);
            return Response.ok(savedSearchService.saveSavedSearch(ctx.getCoreSession(), search)).build();
        } catch (InvalidSearchParameterException | IOException e) {
            throw new NuxeoException(e.getMessage(), SC_BAD_REQUEST);
        }
    }

    @GET
    @Path("saved/{id}")
    public Response doGetSavedSearch(@PathParam("id") String id) {
        SavedSearch search = savedSearchService.getSavedSearch(ctx.getCoreSession(), id);
        if (search == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
        return Response.ok(search).build();
    }

    @Path("saved/{id}/bulk")
    public Object doBulkActionBySavedSearch(@PathParam("id") String id, @Context UriInfo uriInfo) {
        SavedSearch search = savedSearchService.getSavedSearch(ctx.getCoreSession(), id);
        if (search == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
        MultivaluedMap<String, String> queryParams = uriInfo.getQueryParameters();
        String query = getQueryString(search.getPageProviderName(), queryParams);
        return newObject("bulkAction", query);
    }

    @PUT
    @Path("saved/{id}")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response doUpdateSavedSearch(SavedSearchRequest request, @PathParam("id") String id) {
        SavedSearch search = savedSearchService.getSavedSearch(ctx.getCoreSession(), id);
        if (search == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }

        search.setTitle(request.getTitle());
        search.setQueryParams(request.getQueryParams());
        search.setQuery(request.getQuery());
        search.setQueryLanguage(request.getQueryLanguage());
        search.setPageProviderName(request.getPageProviderName());
        search.setPageSize(request.getPageSize());
        search.setCurrentPageIndex(request.getCurrentPageIndex());
        search.setMaxResults(request.getMaxResults());
        search.setSortBy(request.getSortBy());
        search.setSortOrder(request.getSortOrder());
        search.setContentViewData(request.getContentViewData());
        try {
            setSaveSearchParams(request.getNamedParams(), search);
            search = savedSearchService.saveSavedSearch(ctx.getCoreSession(), search);
        } catch (InvalidSearchParameterException | IOException e) {
            throw new NuxeoException(e.getMessage(), SC_BAD_REQUEST);
        }
        return Response.ok(search).build();
    }

    @DELETE
    @Path("saved/{id}")
    public Response doDeleteSavedSearch(@PathParam("id") String id) {
        SavedSearch search = savedSearchService.getSavedSearch(ctx.getCoreSession(), id);
        savedSearchService.deleteSavedSearch(ctx.getCoreSession(), search);
        return Response.status(Response.Status.NO_CONTENT).build();
    }

    @GET
    @Path("saved/{id}/execute")
    public Object doExecuteSavedSearch(@PathParam("id") String id, @Context UriInfo uriInfo) {
        MultivaluedMap<String, String> params = uriInfo.getQueryParameters();
        SavedSearch search = savedSearchService.getSavedSearch(ctx.getCoreSession(), id);
        if (search == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
        return executeSavedSearch(search, params);
    }

    protected void setSaveSearchParams(Map<String, String> params, SavedSearch search) throws IOException {
        Map<String, String> namedParams = new HashMap<>();
        if (params != null) {
            for (Map.Entry<String, String> entry : params.entrySet()) {
                String key = entry.getKey();
                String value = entry.getValue();
                try {
                    Property prop = search.getDocument().getProperty(key);
                    DocumentHelper.setProperty(search.getDocument().getCoreSession(), search.getDocument(), key, value,
                            true);
                } catch (PropertyNotFoundException e) {
                    namedParams.put(key, value);
                }
            }
        }
        search.setNamedParams(namedParams);
    }

    protected DocumentModelList executeSavedSearch(SavedSearch search, MultivaluedMap<String, String> params) {
        Long pageSize = getPageSize(params);
        Long currentPageIndex = getCurrentPageIndex(params);
        Long currentPageOffset = getCurrentPageOffset(params);
        Long maxResults = getMaxResults(params);
        List<SortInfo> sortInfo = getSortInfo(params);

        if (!StringUtils.isEmpty(search.getPageProviderName())) {
            List<QuickFilter> quickFilters = getQuickFilters(search.getPageProviderName(), params);
            return querySavedSearchByPageProvider(search.getPageProviderName(),
                    pageSize != null ? pageSize : search.getPageSize(),
                    currentPageIndex != null ? currentPageIndex : search.getCurrentPageIndex(),
                    currentPageOffset != null ? currentPageOffset : search.getCurrentPageOffset(),
                    search.getQueryParams(), search.getNamedParams(),
                    sortInfo != null ? sortInfo : getSortInfo(search.getSortBy(), search.getSortOrder()), quickFilters,
                    !search.getDocument().getType().equals(SavedSearchConstants.PARAMETERIZED_SAVED_SEARCH_TYPE_NAME)
                            ? search.getDocument()
                            : null);
        } else if (!StringUtils.isEmpty(search.getQuery()) && !StringUtils.isEmpty(search.getQueryLanguage())) {
            return querySavedSearchByLang(search.getQueryLanguage(), search.getQuery(),
                    pageSize != null ? pageSize : search.getPageSize(),
                    currentPageIndex != null ? currentPageIndex : search.getCurrentPageIndex(),
                    currentPageOffset != null ? currentPageOffset : search.getCurrentPageOffset(),
                    maxResults != null ? maxResults : search.getMaxResults(), search.getQueryParams(),
                    search.getNamedParams(),
                    sortInfo != null ? sortInfo : getSortInfo(search.getSortBy(), search.getSortOrder()));
        } else {
            return null;
        }
    }

    protected DocumentModelList querySavedSearchByLang(String queryLanguage, String query, Long pageSize,
            Long currentPageIndex, Long currentPageOffset, Long maxResults, String orderedParams,
            Map<String, String> namedParameters, List<SortInfo> sortInfo) {
        Map<String, String> namedParametersProps = getNamedParameters(namedParameters);
        Object[] parameters = replaceParameterPattern(new Object[] { orderedParams });
        Map<String, Serializable> props = getProperties();

        DocumentModel searchDocumentModel = PageProviderHelper.getSearchDocumentModel(ctx.getCoreSession(), null,
                namedParametersProps);

        return queryByLang(query, pageSize, currentPageIndex, currentPageOffset, maxResults, sortInfo, props,
                searchDocumentModel, parameters);
    }

    protected DocumentModelList querySavedSearchByPageProvider(String pageProviderName, Long pageSize,
            Long currentPageIndex, Long currentPageOffset, String orderedParams, Map<String, String> namedParameters,
            List<SortInfo> sortInfo, List<QuickFilter> quickFilters, DocumentModel searchDocumentModel) {
        Map<String, String> namedParametersProps = getNamedParameters(namedParameters);
        Object[] parameters = orderedParams != null ? replaceParameterPattern(new Object[] { orderedParams })
                : new Object[0];
        Map<String, Serializable> props = getProperties();

        DocumentModel documentModel;
        if (searchDocumentModel == null) {
            documentModel = PageProviderHelper.getSearchDocumentModel(ctx.getCoreSession(), pageProviderName,
                    namedParametersProps);
        } else {
            documentModel = searchDocumentModel;
            if (namedParametersProps.size() > 0) {
                documentModel.putContextData(PageProviderService.NAMED_PARAMETERS, (Serializable) namedParametersProps);
            }
        }

        return queryByPageProvider(pageProviderName, pageSize, currentPageIndex, currentPageOffset, sortInfo,
                quickFilters, parameters, props, documentModel);
    }

    /**
     * Retrieves the page provider from the given page provider name and/or parameters.
     *
     * @since 2021.8
     */
    protected PageProvider<?> getPageProvider(String providerName, MultivaluedMap<String, String> parameters) {
        Map<String, String> namedParameters = getNamedParameters(parameters);
        Object[] queryParameters = getParameters(parameters);
        List<String> quickfilters = asStringList(parameters.getFirst(QUICK_FILTERS));
        Long pageSize = getPageSize(parameters);
        Long currentPageIndex = getCurrentPageIndex(parameters);
        List<String> sortBy = asStringList(parameters.getFirst(SORT_BY));
        List<String> sortOrder = asStringList(parameters.getFirst(SORT_ORDER));

        String query = getQuery(parameters);

        PageProviderDefinition def = providerName == null ? PageProviderHelper.getQueryPageProviderDefinition(query)
                : PageProviderHelper.getPageProviderDefinition(providerName);

        return PageProviderHelper.getPageProvider(ctx.getCoreSession(), def, namedParameters, sortBy, sortOrder,
                pageSize, currentPageIndex, null, quickfilters, queryParameters);
    }

    /**
     * Retrieves the query string from the given page provider name and/or parameters.
     */
    protected String getQueryString(String providerName, MultivaluedMap<String, String> parameters) {
        PageProvider<?> pageProvider = getPageProvider(providerName, parameters);
        return PageProviderHelper.buildQueryString(pageProvider);
    }

    /**
     * Retrieves the query string from the given page provider.
     *
     * @since 2021.8
     */
    protected String getQueryString(PageProvider<?> pageProvider) {
        return PageProviderHelper.buildQueryString(pageProvider);
    }

}
