/*
 * (C) Copyright 2015-2018 Nuxeo (http://nuxeo.com/) and others.
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
 *     Mariana Cedica <mcedica@nuxeo.com>
 *     Antoine Taillefer <ataillefer@nuxeo.com>
 *     Kevin Leturc <kleturc@nuxeo.com>
 */
package org.nuxeo.drive.elasticsearch;

import java.io.IOException;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.search.SearchType;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.query.RangeQueryBuilder;
import org.elasticsearch.index.query.TermsQueryBuilder;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.sort.SortOrder;
import org.nuxeo.drive.service.SynchronizationRoots;
import org.nuxeo.drive.service.impl.AuditChangeFinder;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.repository.RepositoryManager;
import org.nuxeo.ecm.platform.audit.api.ExtendedInfo;
import org.nuxeo.ecm.platform.audit.api.LogEntry;
import org.nuxeo.ecm.platform.audit.impl.LogEntryImpl;
import org.nuxeo.elasticsearch.ElasticSearchConstants;
import org.nuxeo.elasticsearch.api.ESClient;
import org.nuxeo.elasticsearch.api.ElasticSearchAdmin;
import org.nuxeo.runtime.api.Framework;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Override the JPA audit based change finder to execute query in ES.
 * <p>
 * The structure of the query executed by the {@link AuditChangeFinder} is:
 *
 * <pre>
 * from LogEntry log where log.repositoryId = :repositoryId
 *
 * + AND if ActiveRoots (activeRoots) NOT empty
 *
 * from LogEntry log where log.repositoryId = :repositoryId and (
 * LIST_DOC_EVENTS_IDS_QUERY and ( ROOT_PATHS or COLECTIONS_PATHS) or
 * (log.category = 'NuxeoDrive' and log.eventId != 'rootUnregistered') )
 *
 *
 * if ActiveRoots EMPTY:
 *
 * from LogEntry log where log.repositoryId = :repositoryId and ((log.category =
 * 'NuxeoDrive' and log.eventId != 'rootUnregistered'))
 *
 * + AND (log.id > :lowerBound and log.id <= :upperBound) + order by
 * log.repositoryId asc, log.eventDate desc
 * </pre>
 *
 * @since 7.3
 */
public class ESAuditChangeFinder extends AuditChangeFinder {

    private static final long serialVersionUID = 1L;

    public static final Log log = LogFactory.getLog(ESAuditChangeFinder.class);

    protected ESClient esClient = null;

    protected List<LogEntry> queryESAuditEntries(CoreSession session, SynchronizationRoots activeRoots,
            Set<String> collectionSyncRootMemberIds, long lowerBound, long upperBound, boolean integerBounds,
            int limit) {

        SearchRequest request = new SearchRequest(getESIndexName()).types(ElasticSearchConstants.ENTRY_TYPE)
                                                                   .searchType(SearchType.DFS_QUERY_THEN_FETCH);

        QueryBuilder queryBuilder = QueryBuilders.matchAllQuery();
        QueryBuilder filterBuilder = buildFilterClauses(session, activeRoots, collectionSyncRootMemberIds, lowerBound,
                upperBound, integerBounds, limit);
        SearchSourceBuilder source = new SearchSourceBuilder().query(
                QueryBuilders.boolQuery().must(queryBuilder).filter(filterBuilder));
        source.sort("repositoryId", SortOrder.ASC).sort("eventDate", SortOrder.DESC);
        source.size(limit);
        request.source(source);
        List<LogEntry> entries = new ArrayList<>();
        logSearchRequest(request);
        SearchResponse searchResponse = getClient().search(request);
        logSearchResponse(searchResponse);
        ObjectMapper mapper = new ObjectMapper();
        for (SearchHit hit : searchResponse.getHits()) {
            try {
                entries.add(mapper.readValue(hit.getSourceAsString(), LogEntryImpl.class));
            } catch (IOException e) {
                log.error("Error while reading Audit Entry from ES", e);
            }
        }
        return entries;
    }

    protected QueryBuilder buildFilterClauses(CoreSession session, SynchronizationRoots activeRoots,
            Set<String> collectionSyncRootMemberIds, long lowerBound, long upperBound, boolean integerBounds,
            int limit) {
        BoolQueryBuilder filterBuilder = QueryBuilders.boolQuery();

        // from LogEntry log where log.repositoryId = :repositoryId
        QueryBuilder repositoryClauseFilter = QueryBuilders.termQuery("repositoryId", session.getRepositoryName());
        filterBuilder.must(repositoryClauseFilter);

        if (activeRoots.getPaths().isEmpty()) {
            // AND (log.category = 'NuxeoDrive' and log.eventId != 'rootUnregistered')
            filterBuilder.must(getDriveLogsQueryClause());
        } else {

            BoolQueryBuilder orFilterBuilderIfActiveRoots = QueryBuilders.boolQuery();

            // LIST_DOC_EVENTS_IDS_QUERY

            // (log.category = 'eventDocumentCategory' and (log.eventId =
            // 'documentCreated' or log.eventId = 'documentModified' or
            // log.eventId = 'documentMoved' or log.eventId =
            // 'documentCreatedByCopy' or log.eventId = 'documentRestored' or
            // log.eventId = 'addedToCollection’ or log.eventId = 'documentProxyPublished’ or log.eventId =
            // 'documentLocked' or log.eventId = 'documentUnlocked') or log.category =
            // 'eventLifeCycleCategory' and log.eventId =
            // 'lifecycle_transition_event' and log.docLifeCycle != 'deleted' )
            String eventIds[] = { "documentCreated", "documentModified", "documentMoved", "documentCreatedByCopy",
                    "documentRestored", "addedToCollection", "documentProxyPublished", "documentLocked",
                    "documentUnlocked" };
            BoolQueryBuilder orEventsFilter = QueryBuilders.boolQuery();
            orEventsFilter.should(getEventsClause("eventDocumentCategory", eventIds, true));
            orEventsFilter.should(
                    getEventsClause("eventLifeCycleCategory", new String[] { "lifecycle_transition_event" }, true));
            orEventsFilter.should(getEventsClause("eventLifeCycleCategory", new String[] { "deleted" }, false));

            // ROOT_PATHS log.docPath like :rootPath1
            if (collectionSyncRootMemberIds != null && collectionSyncRootMemberIds.size() > 0) {
                BoolQueryBuilder rootsOrCollectionsFilter = QueryBuilders.boolQuery();
                rootsOrCollectionsFilter.should(getCurrentRootsClause(activeRoots.getPaths()));
                rootsOrCollectionsFilter.should(getCollectionSyncRootClause(collectionSyncRootMemberIds));

                // ( LIST_DOC_EVENTS_IDS_QUERY and ( ROOT_PATHS or
                // COLECTIONS_PATHS)
                // or (log.category = 'NuxeoDrive' and log.eventId !=
                // 'rootUnregistered') )
                orFilterBuilderIfActiveRoots.should(
                        QueryBuilders.boolQuery().must(orEventsFilter).must(rootsOrCollectionsFilter));
            } else {
                orFilterBuilderIfActiveRoots.should(QueryBuilders.boolQuery().must(orEventsFilter).must(
                        getCurrentRootsClause(activeRoots.getPaths())));
            }

            orFilterBuilderIfActiveRoots.should(getDriveLogsQueryClause());

            filterBuilder.must(orFilterBuilderIfActiveRoots);
        }

        filterBuilder.must(getLogIdBoundsClause(lowerBound, upperBound));
        return filterBuilder;

    }

    protected RangeQueryBuilder getLogIdBoundsClause(long lowerBound, long upperBound) {
        RangeQueryBuilder rangeFilter = QueryBuilders.rangeQuery("id");
        rangeFilter.gt(lowerBound);
        rangeFilter.lte(upperBound);
        return rangeFilter;
    }

    protected TermsQueryBuilder getCollectionSyncRootClause(Set<String> collectionSyncRootMemberIds) {
        return QueryBuilders.termsQuery("docUUID", collectionSyncRootMemberIds);
    }

    protected BoolQueryBuilder getCurrentRootsClause(Set<String> rootPaths) {
        BoolQueryBuilder orFilterRoots = QueryBuilders.boolQuery();
        for (String rootPath : rootPaths) {
            orFilterRoots.should(QueryBuilders.prefixQuery("docPath", rootPath));
        }
        return orFilterRoots;
    }

    protected BoolQueryBuilder getDriveLogsQueryClause() {
        BoolQueryBuilder filterBuilder = QueryBuilders.boolQuery();
        filterBuilder.must(QueryBuilders.termQuery("category", "NuxeoDrive"));
        filterBuilder.mustNot(QueryBuilders.termQuery("eventId", "rootUnregistered"));
        return filterBuilder;
    }

    protected BoolQueryBuilder getEventsClause(String category, String[] eventIds, boolean shouldMatch) {
        BoolQueryBuilder filterBuilder = QueryBuilders.boolQuery();
        filterBuilder.must(QueryBuilders.termQuery("category", category));
        if (eventIds != null && eventIds.length > 0) {
            if (eventIds.length == 1) {
                if (shouldMatch) {
                    filterBuilder.must(QueryBuilders.termQuery("eventId", eventIds[0]));
                } else {
                    filterBuilder.mustNot(QueryBuilders.termQuery("eventId", eventIds[0]));
                }
            } else {
                if (shouldMatch) {
                    filterBuilder.must(QueryBuilders.termsQuery("eventId", eventIds));
                } else {
                    filterBuilder.mustNot(QueryBuilders.termsQuery("eventId", eventIds));
                }
            }
        }
        return filterBuilder;
    }

    @Override
    public long getUpperBound() {
        RepositoryManager repositoryManager = Framework.getService(RepositoryManager.class);
        return getUpperBound(new HashSet<>(repositoryManager.getRepositoryNames()));
    }

    /**
     * Returns the last available log id in the audit index considering events older than the last clustering
     * invalidation date if clustering is enabled for at least one of the given repositories. This is to make sure the
     * {@code DocumentModel} further fetched from the session using the audit entry doc id is fresh.
     */
    @Override
    public long getUpperBound(Set<String> repositoryNames) {
        SearchRequest request = new SearchRequest(getESIndexName()).types(ElasticSearchConstants.ENTRY_TYPE)
                                                                   .searchType(SearchType.DFS_QUERY_THEN_FETCH);
        RangeQueryBuilder filterBuilder = QueryBuilders.rangeQuery("logDate");
        long clusteringDelay = getClusteringDelay(repositoryNames);
        if (clusteringDelay > -1) {
            long lastClusteringInvalidationDate = System.currentTimeMillis() - 2 * clusteringDelay;
            filterBuilder = filterBuilder.lt(lastClusteringInvalidationDate);
        }
        SearchSourceBuilder source = new SearchSourceBuilder();
        source.sort("id", SortOrder.DESC).size(1);
        // scroll on previous days with a times 2 step up to 32
        for (int i = 1; i <= 32; i = i * 2) {
            ZonedDateTime lowerLogDateTime = ZonedDateTime.now().truncatedTo(ChronoUnit.DAYS).minusDays(i);
            // set lower bound in query
            filterBuilder = filterBuilder.gt(lowerLogDateTime.toInstant().toEpochMilli());
            source.query(QueryBuilders.boolQuery().filter(filterBuilder));
            request.source(source);
            // run request
            logSearchRequest(request);
            SearchResponse searchResponse = getClient().search(request);
            logSearchResponse(searchResponse);

            // if results return the first hit id
            ObjectMapper mapper = new ObjectMapper();
            SearchHits hits = searchResponse.getHits();
            for (SearchHit hit : hits) {
                try {
                    return mapper.readValue(hit.getSourceAsString(), LogEntryImpl.class).getId();
                } catch (IOException e) {
                    log.error("Error while reading Audit Entry from ES", e);
                }
            }
        }
        if (clusteringDelay > -1) {
            // Check for existing entries without the clustering invalidation date filter to not return -1 in this
            // case and make sure the lower bound of the next call to NuxeoDriveManager#getChangeSummary will be >= 0
            source.query(QueryBuilders.matchAllQuery()).size(0);
            request.source(source);
            logSearchRequest(request);
            SearchResponse searchResponse = getClient().search(request);
            logSearchResponse(searchResponse);
            if (searchResponse.getHits().getTotalHits() > 0) {
                log.debug("Found no audit log entries matching the criterias but some exist, returning 0");
                return 0;
            }
        }
        log.debug("Found no audit log entries, returning -1");
        return -1;
    }

    @Override
    protected List<LogEntry> queryAuditEntries(CoreSession session, SynchronizationRoots activeRoots,
            Set<String> collectionSyncRootMemberIds, long lowerBound, long upperBound, boolean integerBounds,
            int limit) {
        List<LogEntry> entries = queryESAuditEntries(session, activeRoots, collectionSyncRootMemberIds, lowerBound,
                upperBound, integerBounds, limit);
        // Post filter the output to remove (un)registration that are unrelated
        // to the current user.
        // TODO move this to the ES query
        List<LogEntry> postFilteredEntries = new ArrayList<>();
        String principalName = session.getPrincipal().getName();
        for (LogEntry entry : entries) {
            ExtendedInfo impactedUserInfo = entry.getExtendedInfos().get("impactedUserName");
            if (impactedUserInfo != null && !principalName.equals(impactedUserInfo.getValue(String.class))) {
                // ignore event that only impact other users
                continue;
            }
            if (log.isDebugEnabled()) {
                if (log.isDebugEnabled()) {
                    log.debug(String.format("Change detected: %s", entry));
                }
            }
            postFilteredEntries.add(entry);
        }
        return postFilteredEntries;
    }

    protected ESClient getClient() {
        if (esClient == null) {
            ElasticSearchAdmin esa = Framework.getService(ElasticSearchAdmin.class);
            esClient = esa.getClient();
        }
        return esClient;
    }

    protected String getESIndexName() {
        ElasticSearchAdmin esa = Framework.getService(ElasticSearchAdmin.class);
        return esa.getIndexNameForType(ElasticSearchConstants.ENTRY_TYPE);
    }

    protected void logSearchRequest(SearchRequest request) {
        if (log.isDebugEnabled()) {
            log.debug(String.format(
                    "Elasticsearch search request: curl -XGET 'http://localhost:9200/%s/%s/_search?pretty' -d '%s'",
                    getESIndexName(), ElasticSearchConstants.ENTRY_TYPE, request.toString()));
        }
    }

    protected void logSearchResponse(SearchResponse response) {
        if (log.isDebugEnabled()) {
            log.debug("Elasticsearch search response: " + response.toString());
        }
    }
}
