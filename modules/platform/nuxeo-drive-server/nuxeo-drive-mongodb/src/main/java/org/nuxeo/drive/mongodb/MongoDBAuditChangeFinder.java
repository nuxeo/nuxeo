/*
 * (C) Copyright 2020 Nuxeo (http://nuxeo.com/) and others.
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
 */
package org.nuxeo.drive.mongodb;

import static com.mongodb.client.model.Filters.and;
import static com.mongodb.client.model.Filters.eq;
import static com.mongodb.client.model.Filters.exists;
import static com.mongodb.client.model.Filters.gt;
import static com.mongodb.client.model.Filters.in;
import static com.mongodb.client.model.Filters.lte;
import static com.mongodb.client.model.Filters.ne;
import static com.mongodb.client.model.Filters.or;
import static com.mongodb.client.model.Filters.regex;
import static com.mongodb.client.model.Sorts.ascending;
import static com.mongodb.client.model.Sorts.descending;
import static com.mongodb.client.model.Sorts.orderBy;
import static org.nuxeo.ecm.platform.audit.api.BuiltinLogEntryData.LOG_ID;
import static org.nuxeo.ecm.platform.audit.api.BuiltinLogEntryData.LOG_LOG_DATE;

import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.nuxeo.drive.service.SynchronizationRoots;
import org.nuxeo.drive.service.impl.AuditChangeFinder;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.repository.RepositoryManager;
import org.nuxeo.ecm.core.query.sql.model.OrderByExprs;
import org.nuxeo.ecm.core.query.sql.model.Predicates;
import org.nuxeo.ecm.core.query.sql.model.QueryBuilder;
import org.nuxeo.ecm.platform.audit.api.AuditQueryBuilder;
import org.nuxeo.ecm.platform.audit.api.AuditReader;
import org.nuxeo.ecm.platform.audit.api.LogEntry;
import org.nuxeo.mongodb.audit.MongoDBAuditBackend;
import org.nuxeo.mongodb.audit.MongoDBAuditEntryReader;
import org.nuxeo.runtime.api.Framework;

import com.mongodb.client.MongoCollection;

/** @since 11.3 */
public class MongoDBAuditChangeFinder extends AuditChangeFinder {

    private static final Logger log = LogManager.getLogger(MongoDBAuditChangeFinder.class);

    @Override
    public long getUpperBound() {
        RepositoryManager repositoryManager = Framework.getService(RepositoryManager.class);
        return getUpperBound(new HashSet<>(repositoryManager.getRepositoryNames()));
    }

    /**
     * Returns the last available log id in the audit collection considering events older than the last clustering
     * invalidation date if clustering is enabled for at least one of the given repositories. This is to make sure the
     * {@code DocumentModel} further fetched from the session using the audit entry doc id is fresh.
     */
    @Override
    public long getUpperBound(Set<String> repositoryNames) {
        long clusteringDelay = getClusteringDelay(repositoryNames);
        MongoDBAuditBackend auditService = (MongoDBAuditBackend) Framework.getService(AuditReader.class);
        // TODO remove this dummy predicate once we can query with no predicate at all
        QueryBuilder queryBuilder = new AuditQueryBuilder().predicate(Predicates.gt("eventDate", new Date(0)));
        if (clusteringDelay > -1) {
            // Double the delay in case of overlapping, see https://jira.nuxeo.com/browse/NXP-14826
            ZonedDateTime lastClusteringInvalidationDate = ZonedDateTime.now()
                                                                        .minus(2 * clusteringDelay, ChronoUnit.MILLIS);
            log.debug("Last clustering invalidation date in milliseconds for the upper bound check: {}",
                    lastClusteringInvalidationDate);
            queryBuilder.and(Predicates.lt(LOG_LOG_DATE, lastClusteringInvalidationDate));
        }
        queryBuilder.order(OrderByExprs.desc(LOG_ID));
        queryBuilder.limit(1);
        List<LogEntry> entries = auditService.queryLogs(queryBuilder);

        if (entries.isEmpty()) {
            if (clusteringDelay > -1 && auditService.getAuditCollection().estimatedDocumentCount() > 0) {
                // Check for existing entries without the clustering invalidation date filter to not return -1 in this
                // case and make sure the lower bound of the next call to NuxeoDriveManager#getChangeSummary will be >=
                // 0
                log.debug("Found no audit log entries matching the criterias but some exist, returning 0");
                return 0;
            }
            log.debug("Found no audit log entries, returning -1");
            return -1;
        }
        return entries.get(0).getId();
    }

    @Override
    protected List<LogEntry> queryAuditEntries(CoreSession session, SynchronizationRoots activeRoots,
            Set<String> collectionSyncRootMemberIds, long lowerBound, long upperBound, int limit) {
        MongoDBAuditBackend auditService = (MongoDBAuditBackend) Framework.getService(AuditReader.class);
        MongoCollection<Document> auditCollection = auditService.getAuditCollection();

        // Build intermediate filters
        Bson lifeCycleEvent = and(eq("category", "eventLifeCycleCategory"), eq("eventId", "lifecycle_transition_event"),
                ne("docLifeCycle", "deleted"));
        Bson documentEvent = and(eq("category", "eventDocumentCategory"),
                in("eventId", "documentCreated", "documentModified", "documentMoved", "documentCreatedByCopy",
                        "documentRestored", "addedToCollection", "documentProxyPublished", "documentLocked",
                        "documentUnlocked", "documentUntrashed"));
        Bson sessionRepository = eq("repositoryId", session.getRepositoryName());
        Bson driveCategory = eq("category", "NuxeoDrive");
        Bson notRootUnregistered = ne("eventId", "rootUnregistered");
        Bson inEvents = or(documentEvent, lifeCycleEvent);
        Bson isGeneral = addRoots(inEvents, activeRoots.getPaths(), collectionSyncRootMemberIds);
        Bson isDrive = and(driveCategory, notRootUnregistered);
        Bson idRange = and(gt("_id", lowerBound), lte("_id", upperBound));
        Bson isUser = or(exists("extended.impactedUserName", false),
                eq("extended.impactedUserName", session.getPrincipal().getName()));

        // Build final filter
        Bson filter = and(sessionRepository, or(isGeneral, isDrive), idRange, isUser);
        log.debug("Query on MongoDB-Audit: {}",
                () -> filter.toBsonDocument(Document.class, auditCollection.getCodecRegistry()).toJson());
        Bson order = orderBy(ascending("repositoryId"), descending("eventDate"));

        return auditCollection.find(filter).sort(order).map(MongoDBAuditEntryReader::read).into(new ArrayList<>());
    }

    protected Bson addRoots(Bson baseFilter, Set<String> rootPaths, Set<String> collectionSyncRootMemberIds) {
        List<Bson> rootFilters = new ArrayList<>();
        if (!rootPaths.isEmpty()) {
            rootFilters.add(regex("docPath", "^" + String.join("|^", rootPaths)));
        }
        if (!collectionSyncRootMemberIds.isEmpty()) {
            rootFilters.add(in("docUUID", collectionSyncRootMemberIds));
        }
        if (rootFilters.isEmpty()) {
            return baseFilter;
        } else {
            return and(baseFilter, or(rootFilters));
        }
    }

}
