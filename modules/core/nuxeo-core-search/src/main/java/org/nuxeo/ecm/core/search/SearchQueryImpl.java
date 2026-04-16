/*
 * (C) Copyright 2024 Nuxeo (http://nuxeo.com/) and others.
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
package org.nuxeo.ecm.core.search;

import static org.apache.commons.collections4.CollectionUtils.isEmpty;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.nuxeo.ecm.core.query.sql.NXQL.ECM_UUID;
import static org.nuxeo.ecm.core.search.SearchServiceImpl.getFromClause;
import static org.nuxeo.ecm.core.search.index.DefaultIndexingJsonWriter.REPOSITORY_PROP;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.ecm.core.api.SortInfo;
import org.nuxeo.ecm.core.query.QueryParseException;
import org.nuxeo.ecm.core.query.sql.SQLQueryParser;
import org.nuxeo.ecm.core.query.sql.model.DefaultQueryVisitor;
import org.nuxeo.ecm.core.query.sql.model.Operand;
import org.nuxeo.ecm.core.query.sql.model.Reference;
import org.nuxeo.ecm.core.query.sql.model.SQLQuery;
import org.nuxeo.ecm.core.query.sql.model.SelectClause;
import org.nuxeo.ecm.core.schema.SchemaManager;
import org.nuxeo.ecm.core.schema.types.Field;
import org.nuxeo.ecm.core.schema.types.Type;
import org.nuxeo.ecm.core.schema.types.primitives.StringType;
import org.nuxeo.ecm.core.security.SecurityService;
import org.nuxeo.ecm.platform.query.api.Aggregate;
import org.nuxeo.ecm.platform.query.api.Bucket;
import org.nuxeo.runtime.api.Framework;

/**
 * The main SearchQuery implementation to perform NXQL search.
 */
public class SearchQueryImpl implements SearchQuery {

    protected final List<SearchIndex> searchIndexes;

    protected final SQLQuery query;

    protected final NuxeoPrincipal principal;

    protected final int offset;

    protected final int limit;

    protected final boolean scrollSearch;

    protected final Duration scrollKeepAlive;

    protected final int scrollSize;

    protected final List<String> highlights;

    protected final List<Aggregate<? extends Bucket>> aggregates;

    protected final Map<String, Type> selectFields;

    protected SearchQueryImpl(Builder builder) {
        this.searchIndexes = Collections.unmodifiableList(builder.searchIndexes);
        this.query = buildSQLQuery(builder);
        this.principal = builder.principal;
        this.offset = builder.offset;
        this.limit = builder.limit;
        this.scrollSearch = builder.scrollSearch;
        this.scrollKeepAlive = builder.scrollKeepAlive;
        this.scrollSize = builder.scrollSize;
        this.highlights = Collections.unmodifiableList(builder.highlights);
        this.aggregates = Collections.unmodifiableList(builder.aggregates);
        this.selectFields = Collections.unmodifiableMap(buildSelectFields(this.query));
    }

    protected static SQLQuery buildSQLQuery(Builder builder) {
        String nxql = builder.nxql;
        try {
            // 1. add sort infos if any
            if (!builder.sortInfos.isEmpty()) {
                if (nxql.contains("ORDER BY")) {
                    throw new QueryParseException("Mixing NXQL ORDER BY and sortInfo is not supported");
                }
                nxql += " ORDER BY " + builder.sortInfos.stream()
                                                        .map(sort -> sort.getSortColumn() + " "
                                                                + (sort.getSortAscending() ? "ASC" : "DESC"))
                                                        .collect(Collectors.joining(", "));
            }
            // 2. parse the sql query
            SQLQuery sqlQuery = SQLQueryParser.parse(nxql);
            // 3. add security policy transformers of the first repository, multi repository search with different
            // policies cannot be handled
            var transformers = Framework.getService(SecurityService.class)
                                        .getPoliciesQueryTransformers(builder.searchIndexes.getFirst().repository());
            for (SQLQuery.Transformer trans : transformers) {
                sqlQuery = trans.transform(builder.principal, sqlQuery);
            }
            return sqlQuery;
        } catch (QueryParseException e) {
            e.addInfo("Failed to execute query: " + nxql);
            throw e;
        }
    }

    protected static Map<String, Type> buildSelectFields(SQLQuery query) {
        SchemaManager schemaManager = Framework.getService(SchemaManager.class);
        Map<String, Type> fieldsAndTypes = new LinkedHashMap<>();
        query.accept(new DefaultQueryVisitor() {

            @Override
            public void visitSelectClause(SelectClause selectClause) {
                for (int i = 0; i < selectClause.getSelectList().size(); i++) {
                    Operand op = selectClause.get(i);
                    if (!(op instanceof Reference reference)) {
                        // ignore it
                        continue;
                    }
                    String name = reference.name;
                    Field field = schemaManager.getField(name);
                    fieldsAndTypes.put(name, field == null ? null : field.getType());
                }
            }
        });
        // default fields
        fieldsAndTypes.put(ECM_UUID, StringType.INSTANCE);
        fieldsAndTypes.put(REPOSITORY_PROP, StringType.INSTANCE);
        return fieldsAndTypes;
    }

    @Override
    public List<SearchIndex> getSearchIndexes() {
        return searchIndexes;
    }

    @Override
    public SQLQuery getQuery() {
        return query;
    }

    @Override
    public NuxeoPrincipal getPrincipal() {
        return principal;
    }

    @Override
    public int getOffset() {
        return offset;
    }

    @Override
    public int getLimit() {
        return limit;
    }

    @Override
    public boolean isScrollSearch() {
        return scrollSearch;
    }

    @Override
    public Duration getScrollKeepAlive() {
        return scrollKeepAlive;
    }

    @Override
    public int getScrollSize() {
        return scrollSize;
    }

    @Override
    public List<String> getHighlights() {
        return highlights;
    }

    @Override
    public List<Aggregate<? extends Bucket>> getAggregates() {
        return aggregates;
    }

    @Override
    public Map<String, Type> getSelectFields() {
        return selectFields;
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this);
    }

    public static class Builder {

        protected static final String SELECT_ALL = "SELECT * FROM %s";

        protected static final String SELECT_ALL_WHERE = "SELECT * FROM %s WHERE ";

        protected static final Duration DEFAULT_SCROLL_TIMEOUT_DURATION = Duration.ofMinutes(5);

        protected static final int DEFAULT_SCROLL_SIZE = 250;

        protected static final int DEFAULT_LIMIT = 10;

        // collection fields

        protected final List<SortInfo> sortInfos = new ArrayList<>();

        protected final List<String> highlights = new ArrayList<>();

        protected final List<Aggregate<? extends Bucket>> aggregates = new ArrayList<>();

        protected final String nxql;

        protected final NuxeoPrincipal principal;

        protected List<SearchIndex> searchIndexes;

        protected int offset = 0;

        protected int limit = DEFAULT_LIMIT;

        protected boolean scrollSearch;

        protected Duration scrollKeepAlive = DEFAULT_SCROLL_TIMEOUT_DURATION;

        protected int scrollSize = DEFAULT_SCROLL_SIZE;

        /**
         * Creates a SearchQuery builder.
         *
         * @since 2025.1
         */
        protected Builder(String nxql, NuxeoPrincipal principal) {
            this.nxql = completeQueryWithSelect(nxql);
            this.principal = principal;
        }

        /**
         * @deprecated since 2025.1, use {@link #Builder(String, NuxeoPrincipal)} instead.
         */
        @Deprecated(since = "2025.1", forRemoval = true)
        protected Builder(List<SearchIndex> searchIndexes, String nxql, NuxeoPrincipal principal) {
            if (isEmpty(searchIndexes)) {
                throw new NullPointerException("searchIndexes cannot be null or empty");
            }
            index(searchIndexes.stream().map(SearchIndex::index).toList());
            this.nxql = completeQueryWithSelect(nxql);
            this.principal = principal;
        }

        /**
         * Search will be performed on this search index. Use the {@link SearchService} to find available index names
         * for a repository.
         *
         * @since 2025.1
         */
        public Builder index(String indexName) {
            if (isBlank(indexName)) {
                searchIndexes = List.of();
                return this;
            }
            return index(List.of(indexName));
        }

        /**
         * Search on multiple indexes to achieve multi repositories search. All indexes should belong to the same search
         * client.
         *
         * @since 2025.1
         * @throws IllegalArgumentException If indexes belong to different search clients
         */
        public Builder index(List<String> indexNames) {
            if (indexNames == null || indexNames.isEmpty()) {
                searchIndexes = List.of();
                return this;
            }
            var searchService = Framework.getService(SearchService.class);
            if (searchService == null) {
                throw new IllegalStateException("No SearchService available");
            }
            return searchIndex(indexNames.stream().map(searchService::getSearchIndex).toList());
        }

        /**
         * Search will be performed on this search index.
         *
         * @since 2025.1
         */
        public Builder searchIndex(SearchIndex index) {
            if (index == null) {
                searchIndexes = List.of();
                return this;
            }
            return searchIndex(List.of(index));
        }

        /**
         * Search on one or multiple SearchIndexes.
         *
         * @since 2025.1
         * @throws IllegalArgumentException If indexes belong to different search clients
         */
        public Builder searchIndex(List<SearchIndex> indexes) {
            if (indexes == null || indexes.isEmpty()) {
                searchIndexes = List.of();
                return this;
            }
            if (indexes.stream().map(SearchIndex::client).distinct().count() != 1) {
                throw new IllegalArgumentException("All searchIndexes must share the same SearchClient: " + indexes);
            }
            searchIndexes = indexes;
            return this;
        }

        protected static String completeQueryWithSelect(String nxql) {
            String query = (nxql == null) ? "" : nxql.trim();
            var fromClause = getFromClause();
            if (query.isEmpty()) {
                query = SELECT_ALL.formatted(fromClause);
            } else if (!query.toLowerCase().startsWith("select ")) {
                query = SELECT_ALL_WHERE.formatted(fromClause) + nxql;
            }
            return query;
        }

        /**
         * Says to skip that many documents before beginning to return documents. If both offset and limit appear, then
         * offset documents are skipped before starting to count the limit documents that are returned. This has no
         * effect on scroll search.
         */
        public Builder offset(int offset) {
            this.offset = offset;
            return this;
        }

        /**
         * No more than that many documents will be returned. This has no effect on scroll search.
         */
        public Builder limit(int limit) {
            this.limit = limit;
            return this;
        }

        /**
         * A scroll search is for deep pagination, returning large amount of results, use
         * {@link SearchService#searchScroll(SearchScrollContext)} to iterate.
         */
        public Builder scrollSearch(boolean value) {
            this.scrollSearch = value;
            return this;
        }

        /**
         * Sets the scroll context keep alive duration, this is the maximum duration between calls of searchScroll API,
         * which is the time to process a SearchResponse.
         */
        public Builder scrollKeepAlive(Duration value) {
            this.scrollKeepAlive = value;
            return scrollSearch(true);
        }

        /**
         * Sets the maximum number of hits to return when using the scroll API.
         */
        public Builder scrollSize(int scrollSize) {
            this.scrollSize = scrollSize;
            return scrollSearch(true);
        }

        /**
         * Adds an ordering clause.
         */
        public Builder addSort(SortInfo sortInfo) {
            sortInfos.add(sortInfo);
            return this;
        }

        /**
         * Adds ordering clauses.
         */
        public Builder addSorts(List<SortInfo> sortInfos) {
            this.sortInfos.addAll(sortInfos);
            return this;
        }

        /**
         * Asks to extract highlights for the following fields if the client supports highlight.
         */
        public Builder addHighlight(String highlight) {
            this.highlights.add(highlight);
            return this;
        }

        /**
         * Asks to extract highlights for the following fields if the client supports highlight.
         */
        public Builder addHighlights(List<String> highlights) {
            this.highlights.addAll(highlights);
            return this;
        }

        /**
         * Asks for an aggregate.
         */
        public Builder addAggregate(Aggregate<? extends Bucket> aggregate) {
            aggregates.add(aggregate);
            return this;
        }

        /**
         * Asks for a list of aggregates.
         */
        public Builder addAggregates(List<Aggregate<? extends Bucket>> aggregates) {
            this.aggregates.addAll(aggregates);
            return this;
        }

        /**
         * Creates the SearchQuery.
         */
        public SearchQuery build() {
            return new SearchQueryImpl(this);
        }
    }
}
