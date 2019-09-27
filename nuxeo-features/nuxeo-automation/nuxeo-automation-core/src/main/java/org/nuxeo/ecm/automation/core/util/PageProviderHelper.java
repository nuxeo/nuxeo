/*
 * (C) Copyright 2018-2019 Nuxeo (http://nuxeo.com/) and others.
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

import static org.nuxeo.common.utils.DateUtils.formatISODateTime;
import static org.nuxeo.common.utils.DateUtils.nowIfNull;
import static org.nuxeo.ecm.platform.query.api.PageProviderService.NAMED_PARAMETERS;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import javax.el.ELContext;
import javax.el.ValueExpression;
import javax.validation.constraints.NotNull;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.core.api.SortInfo;
import org.nuxeo.ecm.core.api.impl.SimpleDocumentModel;
import org.nuxeo.ecm.core.api.model.PropertyNotFoundException;
import org.nuxeo.ecm.core.schema.SchemaManager;
import org.nuxeo.ecm.core.schema.types.Type;
import org.nuxeo.ecm.core.schema.types.primitives.IntegerType;
import org.nuxeo.ecm.core.schema.types.primitives.LongType;
import org.nuxeo.ecm.platform.actions.ELActionContext;
import org.nuxeo.ecm.platform.el.ELService;
import org.nuxeo.ecm.platform.query.api.Aggregate;
import org.nuxeo.ecm.platform.query.api.Bucket;
import org.nuxeo.ecm.platform.query.api.PageProvider;
import org.nuxeo.ecm.platform.query.api.PageProviderDefinition;
import org.nuxeo.ecm.platform.query.api.PageProviderService;
import org.nuxeo.ecm.platform.query.api.QuickFilter;
import org.nuxeo.ecm.platform.query.api.WhereClauseDefinition;
import org.nuxeo.ecm.platform.query.core.BucketRange;
import org.nuxeo.ecm.platform.query.core.BucketRangeDate;
import org.nuxeo.ecm.platform.query.core.BucketTerm;
import org.nuxeo.ecm.platform.query.core.CoreQueryPageProviderDescriptor;
import org.nuxeo.ecm.platform.query.core.GenericPageProviderDescriptor;
import org.nuxeo.ecm.platform.query.nxql.CoreQueryAndFetchPageProvider;
import org.nuxeo.ecm.platform.query.nxql.CoreQueryDocumentPageProvider;
import org.nuxeo.ecm.platform.query.nxql.NXQLQueryBuilder;
import org.nuxeo.runtime.api.Framework;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * @since 10.3
 */
public class PageProviderHelper {

    private static final Logger log = LogManager.getLogger(PageProviderHelper.class);

    static final class QueryAndFetchProviderDescriptor extends GenericPageProviderDescriptor {
        private static final long serialVersionUID = 1L;

        @SuppressWarnings("unchecked")
        public QueryAndFetchProviderDescriptor() {
            super();
            try {
                klass = (Class<PageProvider<?>>) Class.forName(CoreQueryAndFetchPageProvider.class.getName());
            } catch (ClassNotFoundException e) {
                // ignore
            }
        }
    }

    public static final String ASC = "ASC";

    public static final String DESC = "DESC";

    public static final String CURRENT_USERID_PATTERN = "$currentUser";

    public static final String CURRENT_REPO_PATTERN = "$currentRepository";

    protected static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    public static PageProviderDefinition getQueryAndFetchProviderDefinition(String query) {
        return getQueryAndFetchProviderDefinition(query, null);
    }

    public static PageProviderDefinition getQueryAndFetchProviderDefinition(String query,
            Map<String, String> properties) {
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
            Map<String, String> namedParameters, List<String> sortBy, List<String> sortOrder, Long pageSize,
            Long currentPageIndex, Object... queryParams) {
        return getPageProvider(session, def, namedParameters, sortBy, sortOrder, pageSize, currentPageIndex, null, null,
                queryParams);
    }

    public static PageProvider<?> getPageProvider(CoreSession session, PageProviderDefinition def,
            Map<String, String> namedParameters, List<String> sortBy, List<String> sortOrder, Long pageSize,
            Long currentPageIndex, List<String> highlights, List<String> quickFilters, Object... parameters) {

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
                    boolean sortAscending = (sortOrder != null && !sortOrder.isEmpty()
                            && ASC.equalsIgnoreCase(sortOrder.get(i).toLowerCase()));
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

        return pageProviderService.getPageProvider(def.getName(), def, searchDocumentModel, sortInfos, pageSize,
                currentPageIndex, props, highlights, quickFilterList, parameters);
    }

    public static DocumentModel getSearchDocumentModel(CoreSession session, String providerName,
            Map<String, String> namedParameters) {
        PageProviderService pageProviderService = Framework.getService(PageProviderService.class);
        return getSearchDocumentModel(session, pageProviderService, providerName, namedParameters);
    }

    /**
     * Returns a {@link DocumentModel searchDocumentModel} if the given {@code providerName} is not null and has a valid
     * {@link PageProviderDefinition definition}, or if the given {@code namedParameters} is not empty.
     * <p/>
     * {@link PageProviderDefinition Definition} is valid if either it has a type or if it declares where clause.
     *
     * @since 11.1
     */
    public static DocumentModel getSearchDocumentModel(CoreSession session, PageProviderService pps,
            String providerName, Map<String, String> namedParameters) {
        // generate search document model if type specified on the definition
        DocumentModel searchDocumentModel = null;
        if (StringUtils.isNotBlank(providerName)) {
            PageProviderDefinition def = pps.getPageProviderDefinition(providerName);
            if (def != null) {
                String searchDocType = def.getSearchDocumentType();
                if (searchDocType != null) {
                    searchDocumentModel = session.createDocumentModel(searchDocType);
                } else if (def.getWhereClause() != null) {
                    // avoid later error on null search doc, in case where clause is only referring to named parameters
                    // (and no namedParameters are given)
                    searchDocumentModel = SimpleDocumentModel.empty();
                }
            } else {
                log.error("No page provider definition found for provider: {}", providerName);
            }
        }

        if (namedParameters != null && !namedParameters.isEmpty()) {
            // fall back on simple document if no type defined on page provider
            if (searchDocumentModel == null) {
                searchDocumentModel = SimpleDocumentModel.empty();
            }
            fillSearchDocument(session, searchDocumentModel, namedParameters);
        }
        return searchDocumentModel;
    }

    /**
     * @since 11.1
     */
    protected static void fillSearchDocument(CoreSession session, @NotNull DocumentModel searchDoc,
            @NotNull Map<String, String> namedParameters) {
        // we might search on secured properties
        Framework.doPrivileged(() -> {
            for (Map.Entry<String, String> entry : namedParameters.entrySet()) {
                String key = entry.getKey();
                String value = entry.getValue();
                try {
                    DocumentHelper.setProperty(session, searchDoc, key, value, true);
                } catch (PropertyNotFoundException | IOException e) {
                    // assume this is a "pure" named parameter, not part of the search doc schema
                }
            }
            searchDoc.putContextData(NAMED_PARAMETERS, (Serializable) namedParameters);
        });
    }

    public static String buildQueryString(PageProvider<?> provider) {
        return buildQueryStringWithPageProvider(provider, false);
    }

    public static String buildQueryStringWithAggregates(PageProvider<?> provider) {
        return buildQueryStringWithPageProvider(provider, provider.hasAggregateSupport());
    }

    protected static String buildQueryStringWithPageProvider(PageProvider<?> provider, boolean useAggregates) {
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

        String aggregatesClause = useAggregates ? buildAggregatesClause(provider) : null;

        PageProviderDefinition def = provider.getDefinition();
        WhereClauseDefinition whereClause = def.getWhereClause();
        DocumentModel searchDocumentModel = provider.getSearchDocumentModel();
        Object[] parameters = provider.getParameters();
        String query;
        if (whereClause == null) {
            String pattern = def.getPattern();
            if (!quickFiltersClause.isEmpty()) {
                pattern = appendToPattern(pattern, quickFiltersClause);
            }
            if (StringUtils.isNotEmpty(aggregatesClause)) {
                pattern = appendToPattern(pattern, aggregatesClause);
            }

            query = NXQLQueryBuilder.getQuery(pattern, parameters, def.getQuotePatternParameters(),
                    def.getEscapePatternParameters(), searchDocumentModel, null);
        } else {
            if (searchDocumentModel == null) {
                throw new NuxeoException(
                        String.format("Cannot build query of provider '%s': " + "no search document model is set",
                                provider.getName()));
            }
            String additionalClause = NXQLQueryBuilder.appendClause(aggregatesClause, quickFiltersClause);
            query = NXQLQueryBuilder.getQuery(searchDocumentModel, whereClause, additionalClause, parameters, null);
        }
        return query;
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    protected static String buildAggregatesClause(PageProvider provider) {
        try {
            // Aggregates that are being used as filters are stored in the namedParameters context data
            Properties namedParameters = (Properties) provider.getSearchDocumentModel()
                                                              .getContextData(NAMED_PARAMETERS);
            if (namedParameters == null) {
                return "";
            }
            Map<String, Aggregate<? extends Bucket>> aggregates = provider.getAggregates();
            String aggregatesClause = "";
            for (Aggregate<? extends Bucket> aggregate : aggregates.values()) {
                if (namedParameters.containsKey(aggregate.getId())) {
                    JsonNode node = OBJECT_MAPPER.readTree(namedParameters.get(aggregate.getId()));
                    // Remove leading trailing and trailing quotes caused by
                    // the JSON serialization of the named parameters
                    List<String> keys = StreamSupport.stream(node.spliterator(), false)
                                                     .map(value -> value.asText().replaceAll("^\"|\"$", ""))
                                                     .collect(Collectors.toList());
                    // Build aggregate clause from given keys in the named parameters
                    String aggClause = aggregate.getBuckets()
                                                .stream()
                                                .filter(bucket -> keys.contains(bucket.getKey()))
                                                .map(bucket -> getClauseFromBucket(bucket, aggregate.getXPathField()))
                                                .collect(Collectors.joining(" OR "));
                    if (StringUtils.isNotEmpty(aggClause)) {
                        aggClause = "(" + aggClause + ")";
                        aggregatesClause = StringUtils.isEmpty(aggregatesClause) ? aggClause
                                : NXQLQueryBuilder.appendClause(aggregatesClause, aggClause);
                    }
                }
            }
            return aggregatesClause;
        } catch (IOException e) {
            throw new NuxeoException(e);
        }
    }

    protected static String getClauseFromBucket(Bucket bucket, String field) {
        String clause;
        // Replace potential '.' path separator with '/' character
        field = field.replaceAll("\\.", "/");
        if (bucket instanceof BucketTerm) {
            clause = field + "='" + bucket.getKey() + "'";
        } else if (bucket instanceof BucketRange) {
            BucketRange bucketRange = (BucketRange) bucket;
            clause = getRangeClause(field, bucketRange);
        } else if (bucket instanceof BucketRangeDate) {
            BucketRangeDate bucketRangeDate = (BucketRangeDate) bucket;
            clause = getRangeDateClause(field, bucketRangeDate);
        } else {
            throw new NuxeoException("Unknown bucket instance for NXQL translation : " + bucket.getClass());
        }
        return clause;
    }

    protected static String getRangeClause(String field, BucketRange bucketRange) {
        Type type = Framework.getService(SchemaManager.class).getField(field).getType();
        Double from = bucketRange.getFrom() != null ? bucketRange.getFrom() : Double.NEGATIVE_INFINITY;
        Double to = bucketRange.getTo() != null ? bucketRange.getTo() : Double.POSITIVE_INFINITY;
        if (type instanceof IntegerType) {
            return field + " BETWEEN " + from.intValue() + " AND " + to.intValue();
        } else if (type instanceof LongType) {
            return field + " BETWEEN " + from.longValue() + " AND " + to.longValue();
        }
        return field + " BETWEEN " + from + " AND " + to;
    }

    protected static String getRangeDateClause(String field, BucketRangeDate bucketRangeDate) {
        Double from = bucketRangeDate.getFrom();
        Double to = bucketRangeDate.getTo();
        if (from == null && to != null) {
            return field + " < TIMESTAMP '" + formatISODateTime(nowIfNull(bucketRangeDate.getToAsDate())) + "'";
        } else if (from != null && to == null) {
            return field + " >= TIMESTAMP '" + formatISODateTime(nowIfNull(bucketRangeDate.getFromAsDate())) + "'";
        }
        return field + " BETWEEN TIMESTAMP '" + formatISODateTime(nowIfNull(bucketRangeDate.getFromAsDate()))
                + "' AND TIMESTAMP '" + formatISODateTime(nowIfNull(bucketRangeDate.getToAsDate())) + "'";
    }

    protected static String appendToPattern(String pattern, String clause) {
        return StringUtils.containsIgnoreCase(pattern, " WHERE ") ? NXQLQueryBuilder.appendClause(pattern, clause)
                : pattern + " WHERE " + clause;
    }

    /**
     * Resolves additional parameters that could have been defined in the contribution.
     *
     * @param parameters parameters from the operation
     */
    public static Object[] resolveELParameters(PageProviderDefinition def, Object... parameters) {
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

    private PageProviderHelper() {
        // utility class
    }
}
