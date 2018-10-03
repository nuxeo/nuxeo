/*
 * (C) Copyright 2014 Nuxeo SA (http://nuxeo.com/) and others.
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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.search.aggregations.Aggregation;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.core.query.QueryParseException;
import org.nuxeo.ecm.platform.query.api.Aggregate;
import org.nuxeo.ecm.platform.query.api.AggregateDefinition;
import org.nuxeo.ecm.platform.query.api.Bucket;
import org.nuxeo.ecm.platform.query.nxql.CoreQueryDocumentPageProvider;
import org.nuxeo.elasticsearch.aggregate.AggregateEsBase;
import org.nuxeo.elasticsearch.aggregate.AggregateFactory;
import org.nuxeo.elasticsearch.api.ElasticSearchService;
import org.nuxeo.elasticsearch.api.EsResult;
import org.nuxeo.elasticsearch.query.NxQueryBuilder;
import org.nuxeo.elasticsearch.query.NxqlQueryConverter;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.services.config.ConfigurationService;

/**
 * Elasticsearch Page provider that converts the NXQL query build by CoreQueryDocumentPageProvider.
 *
 * @since 5.9.3
 */
public class ElasticSearchNxqlPageProvider extends CoreQueryDocumentPageProvider {

    public static final String CORE_SESSION_PROPERTY = "coreSession";

    public static final String SEARCH_ON_ALL_REPOSITORIES_PROPERTY = "searchAllRepositories";

    // @since 9.2
    public static final String ES_MAX_RESULT_WINDOW_PROPERTY = "org.nuxeo.elasticsearch.provider.maxResultWindow";

    // This is the default ES index.max_result_window
    public static final String DEFAULT_ES_MAX_RESULT_WINDOW_VALUE = "10000";

    protected static final Log log = LogFactory.getLog(ElasticSearchNxqlPageProvider.class);

    private static final long serialVersionUID = 1L;

    protected HashMap<String, Aggregate<? extends Bucket>> currentAggregates;

    protected Long maxResultWindow;

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
        CoreSession coreSession = getCoreSession();
        if (query == null) {
            buildQuery(coreSession);
        }
        if (query == null) {
            throw new NuxeoException(String.format("Cannot perform null query: check provider '%s'", getName()));
        }
        // Build and execute the ES query
        ElasticSearchService ess = Framework.getService(ElasticSearchService.class);
        try {
            NxQueryBuilder nxQuery = new NxQueryBuilder(getCoreSession()).nxql(query)
                                                                         .offset((int) getCurrentPageOffset())
                                                                         .limit(getLimit())
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
        fireSearchEvent(getCoreSession().getPrincipal(), query, currentPageDocuments, System.currentTimeMillis() - t0);

        return currentPageDocuments;
    }

    protected int getLimit() {
        int ret = (int) getMinMaxPageSize();
        if (ret == 0) {
            ret = -1;
        }
        return ret;
    }

    public QueryBuilder getCurrentQueryAsEsBuilder() {
        String nxql = getCurrentQuery();
        return NxqlQueryConverter.toESQueryBuilder(nxql);
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

    @Override
    protected CoreSession getCoreSession() {
        Map<String, Serializable> props = getProperties();
        CoreSession coreSession = (CoreSession) props.get(CORE_SESSION_PROPERTY);
        if (coreSession == null) {
            throw new NuxeoException("cannot find core session");
        }
        return coreSession;
    }

    private List<AggregateEsBase<? extends Aggregation, ? extends Bucket>> buildAggregates() {
        ArrayList<AggregateEsBase<? extends Aggregation, ? extends Bucket>> ret = new ArrayList<>(getAggregateDefinitions().size());
        boolean skip = isSkipAggregates();
        for (AggregateDefinition def : getAggregateDefinitions()) {
            AggregateEsBase<? extends Aggregation, ? extends Bucket> agg = AggregateFactory.create(def, getSearchDocumentModel());
            if (!skip || !agg.getSelection().isEmpty()) {
                // if we want to skip aggregates but one is selected, it has to be computed to filter the result set
                ret.add(AggregateFactory.create(def, getSearchDocumentModel()));
            }
        }
        return ret;
    }

    protected boolean searchOnAllRepositories() {
        String value = (String) getProperties().get(SEARCH_ON_ALL_REPOSITORIES_PROPERTY);
        if (value == null) {
            return false;
        }
        return Boolean.parseBoolean(value);
    }

    @Override
    public boolean hasAggregateSupport() {
        return true;
    }

    @Override
    public Map<String, Aggregate<? extends Bucket>> getAggregates() {
        getCurrentPage();
        return currentAggregates;
    }

    /**
     * Extends the default implementation to add results of aggregates
     *
     * @since 7.4
     */
    @Override
    protected void incorporateAggregates(Map<String, Serializable> eventProps) {

        super.incorporateAggregates(eventProps);
        if (currentAggregates != null) {
            HashMap<String, Serializable> aggregateMatches = new HashMap<>();
            for (String key : currentAggregates.keySet()) {
                Aggregate<? extends Bucket> ag = currentAggregates.get(key);
                ArrayList<HashMap<String, Serializable>> buckets = new ArrayList<>();
                for (Bucket bucket : ag.getBuckets()) {
                    HashMap<String, Serializable> b = new HashMap<>();
                    b.put("key", bucket.getKey());
                    b.put("count", bucket.getDocCount());
                    buckets.add(b);
                }
                aggregateMatches.put(key, buckets);
            }
            eventProps.put("aggregatesMatches", aggregateMatches);
        }
    }

    @Override
    public boolean isLastPageAvailable() {
        if ((getResultsCount() + getPageSize()) <= getMaxResultWindow()) {
            return super.isNextPageAvailable();
        }
        return false;
    }

    @Override
    public boolean isNextPageAvailable() {
        if ((getCurrentPageOffset() + 2 * getPageSize()) <= getMaxResultWindow()) {
            return super.isNextPageAvailable();
        }
        return false;
    }

    @Override
    public long getPageLimit() {
        return getMaxResultWindow() / getPageSize();
    }

    /**
     * Returns the max result window where the PP can navigate without raising Elasticsearch
     * QueryPhaseExecutionException. {@code from + size} must be less than or equal to this value.
     *
     * @since 9.2
     */
    public long getMaxResultWindow() {
        if (maxResultWindow == null) {
            ConfigurationService cs = Framework.getService(ConfigurationService.class);
            String maxResultWindowStr = cs.getProperty(ES_MAX_RESULT_WINDOW_PROPERTY,
                    DEFAULT_ES_MAX_RESULT_WINDOW_VALUE);
            try {
                maxResultWindow = Long.valueOf(maxResultWindowStr);
            } catch (NumberFormatException e) {
                log.warn(String.format(
                        "Invalid maxResultWindow property value: %s for page provider: %s, fallback to default.",
                        maxResultWindowStr, getName()));
                maxResultWindow = Long.valueOf(DEFAULT_ES_MAX_RESULT_WINDOW_VALUE);
            }
        }
        return maxResultWindow;
    }

    @Override
    public long getResultsCountLimit() {
        return getMaxResultWindow();
    }

    /**
     * Set the max result window where the PP can navigate, for testing purpose.
     *
     * @since 9.2
     */
    public void setMaxResultWindow(long maxResultWindow) {
        this.maxResultWindow = maxResultWindow;
    }

}
