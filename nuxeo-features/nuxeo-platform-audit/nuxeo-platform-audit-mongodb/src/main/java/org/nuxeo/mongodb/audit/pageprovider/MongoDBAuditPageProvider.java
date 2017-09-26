/*
 * (C) Copyright 2017 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Kevin Leturc
 */
package org.nuxeo.mongodb.audit.pageprovider;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.bson.BsonDocument;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.core.api.SortInfo;
import org.nuxeo.ecm.platform.audit.api.LogEntry;
import org.nuxeo.ecm.platform.audit.api.comment.CommentProcessorHelper;
import org.nuxeo.ecm.platform.audit.service.AuditBackend;
import org.nuxeo.ecm.platform.audit.service.NXAuditEventsService;
import org.nuxeo.ecm.platform.query.api.AbstractPageProvider;
import org.nuxeo.ecm.platform.query.api.PageProvider;
import org.nuxeo.ecm.platform.query.api.PageProviderDefinition;
import org.nuxeo.ecm.platform.query.api.PredicateDefinition;
import org.nuxeo.ecm.platform.query.api.PredicateFieldDefinition;
import org.nuxeo.ecm.platform.query.api.QuickFilter;
import org.nuxeo.ecm.platform.query.api.WhereClauseDefinition;
import org.nuxeo.ecm.platform.query.nxql.NXQLQueryBuilder;
import org.nuxeo.mongodb.audit.MongoDBAuditBackend;
import org.nuxeo.mongodb.audit.MongoDBAuditEntryReader;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.mongodb.MongoDBSerializationHelper;

import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Sorts;

/**
 * @since 9.1
 */
public class MongoDBAuditPageProvider extends AbstractPageProvider<LogEntry> implements PageProvider<LogEntry> {

    private static final long serialVersionUID = 1L;

    private static final String EMPTY_QUERY = "{}";

    public static final String CORE_SESSION_PROPERTY = "coreSession";

    public static final String UICOMMENTS_PROPERTY = "generateUIComments";

    @Override
    public String toString() {
        return buildAuditFilter().toString();
    }

    protected CoreSession getCoreSession() {
        Object session = getProperties().get(CORE_SESSION_PROPERTY);
        if (session instanceof CoreSession) {
            return (CoreSession) session;
        }
        return null;
    }

    protected void preprocessCommentsIfNeeded(List<LogEntry> entries) {
        Serializable preprocess = getProperties().get(UICOMMENTS_PROPERTY);

        if (preprocess != null && "true".equalsIgnoreCase(preprocess.toString())) {
            @SuppressWarnings("resource")
            CoreSession session = getCoreSession();
            if (session != null) {
                CommentProcessorHelper cph = new CommentProcessorHelper(session);
                cph.processComments(entries);
            }
        }
    }

    @Override
    public List<LogEntry> getCurrentPage() {
        long t0 = System.currentTimeMillis();

        Bson filter = buildAuditFilter();

        List<SortInfo> sortInfos = getSortInfos();
        List<Bson> sorts = new ArrayList<>(sortInfos.size());
        for (SortInfo sortInfo : sortInfos) {
            String sortColumn = sortInfo.getSortColumn();
            if ("id".equals(sortColumn)) {
                sortColumn = MongoDBSerializationHelper.MONGODB_ID;
            }
            if (sortInfo.getSortAscending()) {
                sorts.add(Sorts.ascending(sortColumn));
            } else {
                sorts.add(Sorts.descending(sortColumn));
            }
        }
        MongoCollection<Document> auditCollection = getMongoDBBackend().getAuditCollection();
        FindIterable<Document> response = auditCollection.find(filter)
                                                         .sort(Sorts.orderBy(sorts))
                                                         .skip((int) (getCurrentPageIndex() * pageSize))
                                                         .limit((int) getMinMaxPageSize());
        List<LogEntry> entries = new ArrayList<>();

        // set total number of results
        setResultsCount(auditCollection.count(filter));

        for (Document document : response) {
            entries.add(MongoDBAuditEntryReader.read(document));
        }
        preprocessCommentsIfNeeded(entries);

        CoreSession session = getCoreSession();
        if (session != null) {
            // send event for statistics !
            fireSearchEvent(session.getPrincipal(), filter.toString(), entries, System.currentTimeMillis() - t0);
        }

        return entries;
    }

    protected String getFixedPart() {
        WhereClauseDefinition whereClause = getDefinition().getWhereClause();
        if (whereClause == null) {
            return null;
        } else {
            String fixedPart = whereClause.getFixedPart();
            if (fixedPart == null || fixedPart.isEmpty()) {
                fixedPart = EMPTY_QUERY;
            }
            return fixedPart;
        }
    }

    protected boolean allowSimplePattern() {
        return true;
    }

    protected MongoDBAuditBackend getMongoDBBackend() {
        NXAuditEventsService audit = (NXAuditEventsService) Framework.getRuntime()
                                                                     .getComponent(NXAuditEventsService.NAME);
        AuditBackend backend = audit.getBackend();
        if (backend instanceof MongoDBAuditBackend) {
            return (MongoDBAuditBackend) backend;
        }
        throw new NuxeoException(
                "Unable to use MongoDBAuditPageProvider if audit service is not configured to run with MongoDB");
    }

    protected Bson buildAuditFilter() {
        PageProviderDefinition def = getDefinition();
        Object[] params = getParameters();
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

        Bson filter;
        WhereClauseDefinition whereClause = def.getWhereClause();
        MongoDBAuditBackend mongoDBBackend = getMongoDBBackend();
        if (whereClause == null) {
            // Simple Pattern
            if (!allowSimplePattern()) {
                throw new UnsupportedOperationException("This page provider requires a explicit Where Clause");
            }
            String originalPattern = def.getPattern();
            String pattern = quickFiltersClause.isEmpty() ? originalPattern
                    : StringUtils.containsIgnoreCase(originalPattern, " WHERE ")
                            ? NXQLQueryBuilder.appendClause(originalPattern, quickFiltersClause)
                            : originalPattern + " WHERE " + quickFiltersClause;

            String baseQuery = mongoDBBackend.expandQueryVariables(pattern, params);
            filter = mongoDBBackend.buildFilter(baseQuery, null);
        } else {

            // Add the quick filters clauses to the fixed part
            String fixedPart = getFixedPart();
            if (StringUtils.isNotBlank(quickFiltersClause)) {
                fixedPart = StringUtils.isNotBlank(fixedPart)
                        ? NXQLQueryBuilder.appendClause(fixedPart, quickFiltersClause) : quickFiltersClause;
            }

            // Where clause based on DocumentModel
            String baseQuery = mongoDBBackend.expandQueryVariables(fixedPart, params);
            filter = buildSearchFilter(baseQuery, whereClause.getPredicates(), getSearchDocumentModel());
        }
        return filter;
    }

    @Override
    public void refresh() {
        setCurrentPageOffset(0);
        super.refresh();
    }

    @Override
    public long getResultsCount() {
        return resultsCount;
    }

    @Override
    public List<SortInfo> getSortInfos() {
        // because ContentView can reuse PageProvider without redefining columns
        // ensure compat for ContentView configured with JPA log.* sort syntax
        List<SortInfo> sortInfos = super.getSortInfos();
        for (SortInfo si : sortInfos) {
            if (si.getSortColumn().startsWith("log.")) {
                si.setSortColumn(si.getSortColumn().substring(4));
            }
        }
        return sortInfos;
    }

    private Bson buildFilter(PredicateDefinition[] predicates, DocumentModel searchDocumentModel) {
        if (searchDocumentModel == null) {
            return new Document();
        }

        List<Bson> filters = new ArrayList<>();

        for (PredicateDefinition predicate : predicates) {

            // extract data from DocumentModel
            PredicateFieldDefinition[] fieldDef = predicate.getValues();
            Object[] val = new Object[fieldDef.length];
            for (int fidx = 0; fidx < fieldDef.length; fidx++) {
                Object value;
                if (fieldDef[fidx].getXpath() != null) {
                    value = searchDocumentModel.getPropertyValue(fieldDef[fidx].getXpath());
                } else {
                    value = searchDocumentModel.getProperty(fieldDef[fidx].getSchema(), fieldDef[fidx].getName());
                }
                // Convert Calendar objects
                if (value instanceof Calendar) {
                    value = ((Calendar) value).getTime();
                }
                val[fidx] = value;
            }

            if (!isNonNullParam(val)) {
                // skip predicate where all values are null
                continue;
            }

            String op = predicate.getOperator();
            Object firstValue = val[0];
            String predicateParameter = predicate.getParameter();
            if (op.equalsIgnoreCase("IN")) {

                String[] values;
                if (firstValue instanceof Iterable<?>) {
                    List<String> l = new ArrayList<>();
                    Iterable<?> vals = (Iterable<?>) firstValue;

                    for (Object v : vals) {
                        if (v != null) {
                            l.add(v.toString());
                        }
                    }
                    values = l.toArray(new String[l.size()]);
                } else if (firstValue instanceof Object[]) {
                    values = (String[]) firstValue;
                } else {
                    throw new NuxeoException("IN operand required a list or an array as parameter");
                }
                filters.add(Filters.in(predicateParameter, values));
            } else if (op.equalsIgnoreCase("BETWEEN")) {
                filters.add(Filters.gt(predicateParameter, firstValue));
                if (val.length > 1) {
                    filters.add(Filters.lt(predicateParameter, val[1]));
                }
            } else if (">".equals(op)) {
                filters.add(Filters.gt(predicateParameter, firstValue));
            } else if (">=".equals(op)) {
                filters.add(Filters.gte(predicateParameter, firstValue));
            } else if ("<".equals(op)) {
                filters.add(Filters.lt(predicateParameter, firstValue));
            } else if ("<=".equals(op)) {
                filters.add(Filters.lte(predicateParameter, firstValue));
            } else {
                filters.add(Filters.eq(predicateParameter, firstValue));
            }
        }

        if (filters.isEmpty()) {
            return new Document();
        } else {
            return Filters.and(filters);
        }
    }

    private Bson buildSearchFilter(String fixedPart, PredicateDefinition[] predicates,
            DocumentModel searchDocumentModel) {
        Document fixedFilter = Document.parse(fixedPart);
        BsonDocument filter = buildFilter(predicates, searchDocumentModel).toBsonDocument(BsonDocument.class,
                getMongoDBBackend().getAuditCollection().getCodecRegistry());
        if (fixedFilter.isEmpty()) {
            return filter;
        } else if (filter.isEmpty()) {
            return fixedFilter;
        } else {
            return Filters.and(fixedFilter, filter);
        }
    }

    private boolean isNonNullParam(Object[] val) {
        if (val == null) {
            return false;
        }
        for (Object v : val) {
            if (v instanceof String) {
                if (!((String) v).isEmpty()) {
                    return true;
                }
            } else if (v instanceof String[]) {
                if (((String[]) v).length > 0) {
                    return true;
                }
            } else if (v != null) {
                return true;
            }
        }
        return false;
    }

}
