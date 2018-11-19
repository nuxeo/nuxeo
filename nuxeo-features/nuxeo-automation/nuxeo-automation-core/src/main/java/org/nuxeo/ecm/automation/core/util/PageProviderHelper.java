/*
 * (C) Copyright 2018 Nuxeo (http://nuxeo.com/) and others.
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
 *     Nelson Silva <nsilva@nuxeo.com>
 */
package org.nuxeo.ecm.automation.core.util;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;

import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.core.api.SortInfo;
import org.nuxeo.ecm.core.api.impl.SimpleDocumentModel;
import org.nuxeo.ecm.core.api.model.PropertyNotFoundException;
import org.nuxeo.ecm.platform.actions.ELActionContext;
import org.nuxeo.ecm.platform.el.ELService;
import org.nuxeo.ecm.platform.query.api.PageProvider;
import org.nuxeo.ecm.platform.query.api.PageProviderDefinition;
import org.nuxeo.ecm.platform.query.api.PageProviderService;
import org.nuxeo.ecm.platform.query.api.QuickFilter;
import org.nuxeo.ecm.platform.query.api.WhereClauseDefinition;
import org.nuxeo.ecm.platform.query.core.CoreQueryPageProviderDescriptor;
import org.nuxeo.ecm.platform.query.core.GenericPageProviderDescriptor;
import org.nuxeo.ecm.platform.query.nxql.CoreQueryAndFetchPageProvider;
import org.nuxeo.ecm.platform.query.nxql.CoreQueryDocumentPageProvider;
import org.nuxeo.ecm.platform.query.nxql.NXQLQueryBuilder;
import org.nuxeo.runtime.api.Framework;

import javax.el.ELContext;
import javax.el.ValueExpression;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @since 10.3
 */
public class PageProviderHelper {

    final static class QueryAndFetchProviderDescriptor extends GenericPageProviderDescriptor {
        private static final long serialVersionUID = 1L;

        public QueryAndFetchProviderDescriptor() {
            super();
            try {
                klass = (Class<PageProvider<?>>) Class.forName(CoreQueryAndFetchPageProvider.class.getName());
            } catch (ClassNotFoundException e) {
                // log.error(e, e);
            }
        }
    }

    public static final String ASC = "ASC";

    public static final String DESC = "DESC";

    public static final String CURRENT_USERID_PATTERN = "$currentUser";

    public static final String CURRENT_REPO_PATTERN = "$currentRepository";

    public static PageProviderDefinition getQueryAndFetchProviderDefinition(String query) {
        return getQueryAndFetchProviderDefinition(query, null);
    }

    public static PageProviderDefinition getQueryAndFetchProviderDefinition(String query, Map<String, String> properties) {
        QueryAndFetchProviderDescriptor desc = new QueryAndFetchProviderDescriptor();
        desc.setName(StringUtils.EMPTY);
        desc.setPattern(query);
        if (properties != null) {
            // set the maxResults to avoid slowing down queries
            desc.getProperties().putAll(properties);
        }
        return desc;
    }

    public static PageProviderDefinition getQueryPageProviderDefinition(String query) {
        return getQueryPageProviderDefinition(query, null);
    }

    public static PageProviderDefinition getQueryPageProviderDefinition(String query, Map<String, String> properties) {
        CoreQueryPageProviderDescriptor desc = new CoreQueryPageProviderDescriptor();
        desc.setName(StringUtils.EMPTY);
        desc.setPattern(query);
        if (properties != null) {
            // set the maxResults to avoid slowing down queries
            desc.getProperties().putAll(properties);
        }
        return desc;
    }

    public static PageProviderDefinition getPageProviderDefinition(String providerName) {
        PageProviderService pageProviderService = Framework.getService(PageProviderService.class);
        return pageProviderService.getPageProviderDefinition(providerName);
    }

    public static PageProvider<?> getPageProvider(CoreSession session, PageProviderDefinition def,
            Map<String, String> namedParameters, Object... queryParams) {
        return getPageProvider(session, def, namedParameters, null, null, null, null, queryParams);
    }

    public static PageProvider<?> getPageProvider(CoreSession session, PageProviderDefinition def,
            Map<String, String> namedParameters, List<String> sortBy, List<String> sortOrder,
            Long pageSize, Long currentPageIndex, Object... queryParams) {
        return getPageProvider(session, def, namedParameters, sortBy, sortOrder, pageSize, currentPageIndex,
                null, null, queryParams);
    }

    public static PageProvider<?> getPageProvider(CoreSession session, PageProviderDefinition def,
            Map<String, String> namedParameters, List<String> sortBy, List<String> sortOrder,
            Long pageSize, Long currentPageIndex, List<String> highlights, List<String> quickFilters,
            Object... parameters) {

        // Ordered parameters
        if (ArrayUtils.isNotEmpty(parameters)) {
            // expand specific parameters
            for (int idx = 0; idx < parameters.length; idx++) {
                String value = (String) parameters[idx];
                if (value.equals(CURRENT_USERID_PATTERN)) {
                    parameters[idx] = session.getPrincipal().getName();
                } else if (value.equals(CURRENT_REPO_PATTERN)) {
                    parameters[idx] = session.getRepositoryName();
                }
            }
        }

        // Sort Info Management
        List<SortInfo> sortInfos = null;
        if (sortBy != null) {
            sortInfos = new ArrayList<>();
            for (int i = 0; i < sortBy.size(); i++) {
                String sort = sortBy.get(i);
                if (StringUtils.isNotBlank(sort)) {
                    boolean sortAscending = (sortOrder != null && !sortOrder.isEmpty() && ASC.equalsIgnoreCase(
                            sortOrder.get(i).toLowerCase()));
                    sortInfos.add(new SortInfo(sort, sortAscending));
                }
            }
        }

        // Quick filters management
        List<QuickFilter> quickFilterList = null;
        if (quickFilters != null) {
            quickFilterList = new ArrayList<>();
            for (String filter : quickFilters) {
                for (QuickFilter quickFilter : def.getQuickFilters()) {
                    if (quickFilter.getName().equals(filter)) {
                        quickFilterList.add(quickFilter);
                        break;
                    }
                }
            }
        }

        Map<String, Serializable> props = new HashMap<>();
        props.put(CoreQueryDocumentPageProvider.CORE_SESSION_PROPERTY, (Serializable) session);
        DocumentModel searchDocumentModel = getSearchDocumentModel(session, def.getName(), namedParameters);

        PageProviderService pageProviderService = Framework.getService(PageProviderService.class);

        return pageProviderService.getPageProvider(def.getName(), def,
                searchDocumentModel, sortInfos, pageSize, currentPageIndex, props, highlights, quickFilterList, parameters);
    }

    public static DocumentModel getSearchDocumentModel(CoreSession session, String providerName,
            Map<String, String> namedParameters) {
        PageProviderService pageProviderService = Framework.getService(PageProviderService.class);
        PageProviderDefinition def = pageProviderService.getPageProviderDefinition(providerName);

        // generate search document model if type specified on the definition
        DocumentModel searchDocumentModel = null;

        if (def != null) {
            String searchDocType = def.getSearchDocumentType();
            if (searchDocType != null) {
                searchDocumentModel = session.createDocumentModel(searchDocType);
            } else if (def.getWhereClause() != null) {
                // avoid later error on null search doc, in case where clause is only referring to named parameters
                // (and no namedParameters are given)
                searchDocumentModel = new SimpleDocumentModel();
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
            searchDocumentModel.putContextData(PageProviderService.NAMED_PARAMETERS, (Serializable) namedParameters);
        }
        return searchDocumentModel;
    }

    public static String buildQueryString(PageProvider provider) {
        String quickFiltersClause = "";
        List<QuickFilter> quickFilters = provider.getQuickFilters();
        if (quickFilters != null) {
            for (QuickFilter quickFilter : quickFilters) {
                String clause = quickFilter.getClause();
                if (!quickFiltersClause.isEmpty() && clause != null) {
                    quickFiltersClause = NXQLQueryBuilder.appendClause(quickFiltersClause, clause);
                } else {
                    quickFiltersClause = StringUtils.defaultString(clause);
                }
            }
        }

        PageProviderDefinition def = provider.getDefinition();
        WhereClauseDefinition whereClause = def.getWhereClause();
        DocumentModel searchDocumentModel = provider.getSearchDocumentModel();
        Object[] parameters = provider.getParameters();
        String query;
        if (whereClause == null) {
            String pattern = def.getPattern();
            if (!quickFiltersClause.isEmpty()) {
                pattern = StringUtils.containsIgnoreCase(pattern, " WHERE ") ?
                        NXQLQueryBuilder.appendClause(pattern, quickFiltersClause)
                        : pattern + " WHERE " + quickFiltersClause;
            }

            query = NXQLQueryBuilder.getQuery(pattern, parameters, def.getQuotePatternParameters(),
                    def.getEscapePatternParameters(), searchDocumentModel, null);
        } else {
            if (searchDocumentModel == null) {
                throw new NuxeoException(String.format(
                        "Cannot build query of provider '%s': " + "no search document model is set", provider.getName()));
            }
            query = NXQLQueryBuilder.getQuery(searchDocumentModel, whereClause, quickFiltersClause, parameters, null);
        }
        return query;
    }

    /**
     * Resolves additional parameters that could have been defined in the contribution.
     *
     * @param parameters parameters from the operation
     */
    public static Object[] resolveELParameters(PageProviderDefinition def, Object ...parameters) {
        ELService elService = Framework.getService(ELService.class);
        if (elService == null) {
            return parameters;
        }

        // resolve additional parameters
        String[] params = def.getQueryParameters();
        if (params == null) {
            params = new String[0];
        }

        Object[] resolvedParams = new Object[params.length + (parameters != null ? parameters.length : 0)];

        ELContext elContext = elService.createELContext();

        int i = 0;
        if (parameters != null) {
            i = parameters.length;
            System.arraycopy(parameters, 0, resolvedParams, 0, i);
        }
        for (int j = 0; j < params.length; j++) {
            ValueExpression ve = ELActionContext.EXPRESSION_FACTORY.createValueExpression(elContext, params[j],
                    Object.class);
            resolvedParams[i + j] = ve.getValue(elContext);
        }
        return resolvedParams;
    }
}
