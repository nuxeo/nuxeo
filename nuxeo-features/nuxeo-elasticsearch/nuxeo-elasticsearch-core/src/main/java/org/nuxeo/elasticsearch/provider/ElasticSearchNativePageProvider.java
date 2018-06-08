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
 *     bdelbosc
 */

package org.nuxeo.elasticsearch.provider;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.elasticsearch.index.query.QueryBuilder;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.core.api.SortInfo;
import org.nuxeo.ecm.core.query.QueryParseException;
import org.nuxeo.ecm.platform.query.api.AbstractPageProvider;
import org.nuxeo.ecm.platform.query.api.Aggregate;
import org.nuxeo.ecm.platform.query.api.AggregateDefinition;
import org.nuxeo.ecm.platform.query.api.Bucket;
import org.nuxeo.ecm.platform.query.api.PageProviderDefinition;
import org.nuxeo.ecm.platform.query.api.QuickFilter;
import org.nuxeo.ecm.platform.query.api.WhereClauseDefinition;
import org.nuxeo.ecm.platform.query.nxql.NXQLQueryBuilder;
import org.nuxeo.elasticsearch.aggregate.AggregateEsBase;
import org.nuxeo.elasticsearch.aggregate.AggregateFactory;
import org.nuxeo.elasticsearch.api.ElasticSearchService;
import org.nuxeo.elasticsearch.api.EsResult;
import org.nuxeo.elasticsearch.query.NxQueryBuilder;
import org.nuxeo.elasticsearch.query.PageProviderQueryBuilder;
import org.nuxeo.runtime.api.Framework;

public class ElasticSearchNativePageProvider extends AbstractPageProvider<DocumentModel> {

    public static final String CORE_SESSION_PROPERTY = "coreSession";

    public static final String SEARCH_ON_ALL_REPOSITORIES_PROPERTY = "searchAllRepositories";

    protected static final Log log = LogFactory.getLog(ElasticSearchNativePageProvider.class);

    private static final long serialVersionUID = 1L;

    protected List<DocumentModel> currentPageDocuments;

    protected Map<String, Aggregate<? extends Bucket>> currentAggregates;

    @Override
    public Map<String, Aggregate<? extends Bucket>> getAggregates() {
        getCurrentPage();
        return currentAggregates;
    }

    @Override
    public List<DocumentModel> getCurrentPage() {
        long t0 = System.currentTimeMillis();

        // use a cache
        if (currentPageDocuments != null) {
            return currentPageDocuments;
        }
        error = null;
        errorMessage = null;
        if (log.isDebugEnabled()) {
            log.debug(String.format("Perform query for provider '%s': with pageSize=%d, offset=%d", getName(),
                    getMinMaxPageSize(), getCurrentPageOffset()));
        }
        currentPageDocuments = new ArrayList<>();
        // Build the ES query
        QueryBuilder query = makeQueryBuilder();
        SortInfo[] sortArray = null;
        if (sortInfos != null) {
            sortArray = sortInfos.toArray(new SortInfo[sortInfos.size()]);
        }
        // Execute the ES query
        ElasticSearchService ess = Framework.getService(ElasticSearchService.class);
        try {
            NxQueryBuilder nxQuery = new NxQueryBuilder(getCoreSession()).esQuery(query)
                                                                         .offset((int) getCurrentPageOffset())
                                                                         .limit((int) getMinMaxPageSize())
                                                                         .addSort(sortArray)
                                                                         .addAggregates(buildAggregates());
            if (searchOnAllRepositories()) {
                nxQuery.searchOnAllRepositories();
            }

            List<String> highlightFields = getHighlights();
            if (highlightFields != null && !highlightFields.isEmpty()) {
                nxQuery.highlight(highlightFields);
            }

            EsResult ret = ess.queryAndAggregate(nxQuery);
            DocumentModelList dmList = ret.getDocuments();
            currentAggregates = new HashMap<>(ret.getAggregates().size());
            for (Aggregate<Bucket> agg : ret.getAggregates()) {
                currentAggregates.put(agg.getId(), agg);
            }
            setResultsCount(dmList.totalSize());
            currentPageDocuments = dmList;
        } catch (QueryParseException e) {
            error = e;
            errorMessage = e.getMessage();
            log.warn(e.getMessage(), e);
        }

        // send event for statistics !
        fireSearchEvent(getCoreSession().getPrincipal(), query.toString(), currentPageDocuments,
                System.currentTimeMillis() - t0);

        return currentPageDocuments;
    }

    private List<AggregateEsBase<? extends Bucket>> buildAggregates() {
        ArrayList<AggregateEsBase<? extends Bucket>> ret = new ArrayList<>(getAggregateDefinitions().size());
        boolean skip = isSkipAggregates();
        for (AggregateDefinition def : getAggregateDefinitions()) {
            AggregateEsBase<? extends Bucket> agg = AggregateFactory.create(def, getSearchDocumentModel());
            if (!skip || !agg.getSelection().isEmpty()) {
                // if we want to skip aggregates but one is selected, it has to be computed to filter the result set
                ret.add(AggregateFactory.create(def, getSearchDocumentModel()));
            }
        }
        return ret;
    }

    @Override
    public boolean hasAggregateSupport() {
        return true;
    }

    protected QueryBuilder makeQueryBuilder() {
        QueryBuilder ret;
        PageProviderDefinition def = getDefinition();
        List<QuickFilter> quickFilters = getQuickFilters();
        String quickFiltersClause = "";

        if (quickFilters != null && !quickFilters.isEmpty()) {
            for (QuickFilter quickFilter : quickFilters) {
                String clause = quickFilter.getClause();
                if (!quickFiltersClause.isEmpty() && clause != null) {
                    quickFiltersClause = NXQLQueryBuilder.appendClause(quickFiltersClause, clause);
                } else {
                    quickFiltersClause = clause != null ? clause : "";
                }
            }
        }

        WhereClauseDefinition whereClause = def.getWhereClause();
        if (whereClause == null) {

            String originalPattern = def.getPattern();
            String pattern = quickFiltersClause.isEmpty() ? originalPattern
                    : StringUtils.containsIgnoreCase(originalPattern, " WHERE ")
                            ? NXQLQueryBuilder.appendClause(originalPattern, quickFiltersClause)
                            : originalPattern + " WHERE " + quickFiltersClause;

            ret = PageProviderQueryBuilder.makeQuery(pattern, getParameters(), def.getQuotePatternParameters(),
                    def.getEscapePatternParameters(), isNativeQuery());
        } else {

            DocumentModel searchDocumentModel = getSearchDocumentModel();
            if (searchDocumentModel == null) {
                throw new NuxeoException(String.format(
                        "Cannot build query of provider '%s': " + "no search document model is set", getName()));
            }
            ret = PageProviderQueryBuilder.makeQuery(searchDocumentModel, whereClause, quickFiltersClause,
                    getParameters(), isNativeQuery());
        }
        return ret;
    }

    @Override
    protected void pageChanged() {
        currentPageDocuments = null;
        currentAggregates = null;
        super.pageChanged();
    }

    @Override
    public void refresh() {
        currentPageDocuments = null;
        currentAggregates = null;
        super.refresh();
    }

    protected CoreSession getCoreSession() {
        Map<String, Serializable> props = getProperties();
        CoreSession coreSession = (CoreSession) props.get(CORE_SESSION_PROPERTY);
        if (coreSession == null) {
            throw new NuxeoException("cannot find core session");
        }
        return coreSession;
    }

    protected boolean searchOnAllRepositories() {
        String value = (String) getProperties().get(SEARCH_ON_ALL_REPOSITORIES_PROPERTY);
        if (value == null) {
            return false;
        }
        return Boolean.parseBoolean(value);
    }

    public boolean isNativeQuery() {
        return true;
    }
}
