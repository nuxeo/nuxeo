/*
 * (C) Copyright 2015 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Mariana Cedica <mcedica@nuxeo.com>
 *     Antoine Taillefer <ataillefer@nuxeo.com>
 */
package org.nuxeo.drive.elasticsearch;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.search.SearchType;
import org.elasticsearch.client.Client;
import org.elasticsearch.index.query.AndFilterBuilder;
import org.elasticsearch.index.query.BoolFilterBuilder;
import org.elasticsearch.index.query.FilterBuilder;
import org.elasticsearch.index.query.FilterBuilders;
import org.elasticsearch.index.query.OrFilterBuilder;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.query.RangeFilterBuilder;
import org.elasticsearch.index.query.TermsFilterBuilder;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.sort.SortOrder;
import org.nuxeo.drive.service.SynchronizationRoots;
import org.nuxeo.drive.service.impl.AuditChangeFinder;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.platform.audit.api.ExtendedInfo;
import org.nuxeo.ecm.platform.audit.api.LogEntry;
import org.nuxeo.elasticsearch.api.ElasticSearchAdmin;
import org.nuxeo.elasticsearch.audit.ESAuditBackend;
import org.nuxeo.elasticsearch.audit.io.AuditEntryJSONReader;
import org.nuxeo.runtime.api.Framework;

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

    protected Client esClient = null;

    protected List<LogEntry> queryESAuditEntries(CoreSession session, SynchronizationRoots activeRoots,
            Set<String> collectionSyncRootMemberIds, long lowerBound, long upperBound, boolean integerBounds, int limit) {

        SearchRequestBuilder builder = getClient().prepareSearch(ESAuditBackend.IDX_NAME)
                                                  .setTypes(ESAuditBackend.IDX_TYPE)
                                                  .setSearchType(SearchType.DFS_QUERY_THEN_FETCH);

        QueryBuilder queryBuilder = QueryBuilders.matchAllQuery();
        FilterBuilder filterBuilder = buildFilterClauses(session, activeRoots, collectionSyncRootMemberIds, lowerBound,
                upperBound, integerBounds, limit);
        builder.setQuery(QueryBuilders.filteredQuery(queryBuilder, filterBuilder));

        builder.addSort("repositoryId", SortOrder.ASC);
        builder.addSort("eventDate", SortOrder.DESC);

        List<LogEntry> entries = new ArrayList<>();
        SearchResponse searchResponse = builder.setSize(limit).execute().actionGet();
        for (SearchHit hit : searchResponse.getHits()) {
            try {
                entries.add(AuditEntryJSONReader.read(hit.getSourceAsString()));
            } catch (Exception e) {
                log.error("Error while reading Audit Entry from ES", e);
            }
        }
        return entries;
    }

    protected FilterBuilder buildFilterClauses(CoreSession session, SynchronizationRoots activeRoots,
            Set<String> collectionSyncRootMemberIds, long lowerBound, long upperBound, boolean integerBounds, int limit) {
        AndFilterBuilder filterBuilder = FilterBuilders.andFilter();

        // from LogEntry log where log.repositoryId = :repositoryId
        FilterBuilder repositoryClauseFilter = FilterBuilders.termFilter("repositoryId", session.getRepositoryName());
        filterBuilder.add(repositoryClauseFilter);

        if (activeRoots.getPaths().isEmpty()) {
            // AND (log.category = 'NuxeoDrive' and log.eventId != 'rootUnregistered')
            filterBuilder.add(getDriveLogsQueryClause());
        } else {

            OrFilterBuilder orFilterBuilderIfActiveRoots = FilterBuilders.orFilter();

            // LIST_DOC_EVENTS_IDS_QUERY

            // (log.category = 'eventDocumentCategory' and (log.eventId =
            // 'documentCreated' or log.eventId = 'documentModified' or
            // log.eventId = 'documentMoved' or log.eventId =
            // 'documentCreatedByCopy' or log.eventId = 'documentRestored' or
            // log.eventId = 'addedToCollection’ or log.eventId =
            // 'documentLocked' or log.eventId = 'documentUnlocked') or log.category =
            // 'eventLifeCycleCategory' and log.eventId =
            // 'lifecycle_transition_event' and log.docLifeCycle != 'deleted' )
            String eventIds[] = { "documentCreated", "documentModified", "documentMoved", "documentCreatedByCopy",
                    "documentRestored", "addedToCollection", "documentLocked", "documentUnlocked" };
            OrFilterBuilder orEventsFilter = FilterBuilders.orFilter();
            orEventsFilter.add(getEventsClause("eventDocumentCategory", eventIds, true));
            orEventsFilter.add(getEventsClause("eventLifeCycleCategory", new String[] { "lifecycle_transition_event" },
                    true));
            orEventsFilter.add(getEventsClause("eventLifeCycleCategory", new String[] { "deleted" }, false));

            // ROOT_PATHS log.docPath like :rootPath1
            if (collectionSyncRootMemberIds != null && collectionSyncRootMemberIds.size() > 0) {
                OrFilterBuilder rootsOrCollectionsFilter = FilterBuilders.orFilter();
                rootsOrCollectionsFilter.add(getCurrentRootsClause(activeRoots.getPaths()));
                rootsOrCollectionsFilter.add(getCollectionSyncRootClause(collectionSyncRootMemberIds));

                // ( LIST_DOC_EVENTS_IDS_QUERY and ( ROOT_PATHS or
                // COLECTIONS_PATHS)
                // or (log.category = 'NuxeoDrive' and log.eventId !=
                // 'rootUnregistered') )
                orFilterBuilderIfActiveRoots.add(FilterBuilders.andFilter(orEventsFilter, rootsOrCollectionsFilter));
            } else {
                orFilterBuilderIfActiveRoots.add(FilterBuilders.andFilter(orEventsFilter,
                        getCurrentRootsClause(activeRoots.getPaths())));
            }

            orFilterBuilderIfActiveRoots.add(getDriveLogsQueryClause());

            filterBuilder.add(orFilterBuilderIfActiveRoots);
        }

        filterBuilder.add(getLogIdBoundsClause(lowerBound, upperBound));
        return filterBuilder;

    }

    protected RangeFilterBuilder getLogIdBoundsClause(long lowerBound, long upperBound) {
        RangeFilterBuilder rangeFilter = FilterBuilders.rangeFilter("id");
        rangeFilter.gt(lowerBound);
        rangeFilter.lte(upperBound);
        return rangeFilter;
    }

    protected TermsFilterBuilder getCollectionSyncRootClause(Set<String> collectionSyncRootMemberIds) {
        return FilterBuilders.termsFilter("docUUID", collectionSyncRootMemberIds);
    }

    protected OrFilterBuilder getCurrentRootsClause(Set<String> rootPaths) {
        OrFilterBuilder orFilterRoots = FilterBuilders.orFilter();
        for (String rootPath : rootPaths) {
            orFilterRoots.add(FilterBuilders.prefixFilter("docPath", rootPath));
        }
        return orFilterRoots;
    }

    protected BoolFilterBuilder getDriveLogsQueryClause() {
        BoolFilterBuilder filterBuilder = FilterBuilders.boolFilter();
        filterBuilder.must(FilterBuilders.termFilter("category", "NuxeoDrive"));
        filterBuilder.mustNot(FilterBuilders.termFilter("eventId", "rootUnregistered"));
        return filterBuilder;
    }

    protected BoolFilterBuilder getEventsClause(String category, String[] eventIds, boolean shouldMatch) {
        BoolFilterBuilder filterBuilder = FilterBuilders.boolFilter();
        filterBuilder.must(FilterBuilders.termFilter("category", category));
        if (eventIds != null && eventIds.length > 0) {
            if (eventIds.length == 1) {
                if (shouldMatch) {
                    filterBuilder.must(FilterBuilders.termFilter("eventId", eventIds[0]));
                } else {
                    filterBuilder.mustNot(FilterBuilders.termFilter("eventId", eventIds[0]));
                }
            } else {
                if (shouldMatch) {
                    filterBuilder.must(FilterBuilders.termsFilter("eventId", eventIds));
                } else {
                    filterBuilder.mustNot(FilterBuilders.termsFilter("eventId", eventIds));
                }
            }
        }
        return filterBuilder;
    }

    @Override
    public long getUpperBound() {
        SearchRequestBuilder builder = getClient().prepareSearch(ESAuditBackend.IDX_NAME)
                                                  .setTypes(ESAuditBackend.IDX_TYPE)
                                                  .setSearchType(SearchType.DFS_QUERY_THEN_FETCH);
        // TODO refactor this to use max clause
        builder.setQuery(QueryBuilders.matchAllQuery());
        builder.addSort("id", SortOrder.DESC);
        builder.setSize(1);
        SearchResponse searchResponse = builder.execute().actionGet();
        List<LogEntry> entries = new ArrayList<>();
        SearchHits hits = searchResponse.getHits();
        for (SearchHit hit : hits) {
            try {
                entries.add(AuditEntryJSONReader.read(hit.getSourceAsString()));
            } catch (Exception e) {
                log.error("Error while reading Audit Entry from ES", e);
            }
        }
        return entries.size() > 0 ? entries.get(0).getId() : -1;
    }

    /**
     * Returns the last available log id in the audit index considering events older than the last clustering
     * invalidation date if clustering is enabled for at least one of the given repositories. This is to make sure the
     * {@code DocumentModel} further fetched from the session using the audit entry doc id is fresh.
     */
    @Override
    public long getUpperBound(Set<String> repositoryNames) {
        SearchRequestBuilder builder = getClient().prepareSearch(ESAuditBackend.IDX_NAME)
                                                  .setTypes(ESAuditBackend.IDX_TYPE)
                                                  .setSearchType(SearchType.DFS_QUERY_THEN_FETCH);
        QueryBuilder queryBuilder = QueryBuilders.matchAllQuery();
        long clusteringDelay = getClusteringDelay(repositoryNames);
        if (clusteringDelay > -1) {
            long lastClusteringInvalidationDate = System.currentTimeMillis() - 2 * clusteringDelay;
            FilterBuilder filterBuilder = FilterBuilders.rangeFilter("logDate").lt(
                    new Date(lastClusteringInvalidationDate));
            builder.setQuery(QueryBuilders.filteredQuery(queryBuilder, filterBuilder));
        } else {
            builder.setQuery(queryBuilder);
        }
        builder.addSort("id", SortOrder.DESC);
        builder.setSize(1);
        SearchResponse searchResponse = builder.execute().actionGet();
        List<LogEntry> entries = new ArrayList<>();
        SearchHits hits = searchResponse.getHits();
        for (SearchHit hit : hits) {
            try {
                entries.add(AuditEntryJSONReader.read(hit.getSourceAsString()));
            } catch (Exception e) {
                log.error("Error while reading Audit Entry from ES", e);
            }
        }
        if (entries.isEmpty()) {
            if (clusteringDelay > -1) {
                // Check for existing entries without the clustering invalidation date filter to not return -1 in this
                // case and make sure the lower bound of the next call to NuxeoDriveManager#getChangeSummary will be >=
                // 0
                builder.setQuery(queryBuilder);
                searchResponse = builder.execute().actionGet();
                if (searchResponse.getHits().iterator().hasNext()) {
                    log.debug("Found no audit log entries matching the criterias but some exist, returning 0");
                    return 0;
                }
            }
            log.debug("Found no audit log entries, returning -1");
            return -1;
        }
        return entries.get(0).getId();
    }

    @Override
    protected List<LogEntry> queryAuditEntries(CoreSession session, SynchronizationRoots activeRoots,
            Set<String> collectionSyncRootMemberIds, long lowerBound, long upperBound, boolean integerBounds, int limit) {
        List<LogEntry> entries = queryESAuditEntries(session, activeRoots, collectionSyncRootMemberIds, lowerBound,
                upperBound, integerBounds, limit);
        // Post filter the output to remove (un)registration that are unrelated
        // to the current user.
        // TODO move this to the ES query
        List<LogEntry> postFilteredEntries = new ArrayList<LogEntry>();
        String principalName = session.getPrincipal().getName();
        for (LogEntry entry : entries) {
            ExtendedInfo impactedUserInfo = entry.getExtendedInfos().get("impactedUserName");
            if (impactedUserInfo != null && !principalName.equals(impactedUserInfo.getValue(String.class))) {
                // ignore event that only impact other users
                continue;
            }
            if (log.isDebugEnabled()) {
                if (log.isDebugEnabled()) {
                    log.debug(String.format("Change with eventId=%d detected at eventDate=%s, logDate=%s: %s on %s",
                            entry.getId(), entry.getEventDate(), entry.getLogDate(), entry.getEventId(),
                            entry.getDocPath()));
                }
            }
            postFilteredEntries.add(entry);
        }
        return postFilteredEntries;
    }

    protected Client getClient() {
        if (esClient == null) {
            log.info("Activate Elasticsearch backend for Audit");
            ElasticSearchAdmin esa = Framework.getService(ElasticSearchAdmin.class);
            esClient = esa.getClient();
        }
        return esClient;
    }
}