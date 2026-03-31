/*
 * (C) Copyright 2026 Nuxeo (http://nuxeo.com/) and others.
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
 *     Kevin Leturc <kevin.leturc@hyland.com>
 */
package org.nuxeo.ecm.core.query.scroll;

import java.time.Duration;
import java.util.List;
import java.util.Objects;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import org.nuxeo.ecm.core.api.scroll.ScrollRequest;
import org.nuxeo.ecm.core.query.sql.SQLQueryParser;
import org.nuxeo.ecm.core.query.sql.model.QueryBuilder;

/**
 * @since 2025.18
 */
public class QueryBuilderScrollRequest implements ScrollRequest {

    protected static final String SCROLL_TYPE = "queryBuilder";

    protected final String name;

    protected final QueryBuilder queryBuilder;

    protected final List<String> froms;

    protected final int size;

    protected final Duration timeout;

    protected final String reference;

    protected QueryBuilderScrollRequest(Builder builder) {
        this.name = builder.name;
        this.queryBuilder = builder.queryBuilder;
        this.froms = builder.froms;
        this.size = builder.size;
        this.timeout = builder.timeout;
        this.reference = builder.reference;
    }

    @Override
    public String getType() {
        return SCROLL_TYPE;
    }

    @Override
    public String getName() {
        return name;
    }

    public QueryBuilder getQueryBuilder() {
        return queryBuilder;
    }

    public List<String> getFroms() {
        return froms;
    }

    @Override
    public int getSize() {
        return size;
    }

    public Duration getTimeout() {
        return timeout;
    }

    @Override
    public String getReference() {
        return reference;
    }

    public static Builder builder(String name, String query) {
        return new Builder(name, query);
    }

    public static class Builder {

        protected final String name;

        protected final QueryBuilder queryBuilder;

        protected final List<String> froms;

        protected int size = 10;

        protected Duration timeout = Duration.ofMinutes(2);

        protected String reference;

        public Builder(@Nonnull String name, @Nonnull String queryBuilder) {
            this.name = Objects.requireNonNull(name, "name cannot be null");
            var sqlQuery = SQLQueryParser.parse(Objects.requireNonNull(queryBuilder, "query cannot be null"));
            this.queryBuilder = new QueryBuilder(sqlQuery);
            this.froms = List.copyOf(sqlQuery.getFromClause().elements.values());
        }

        public Builder size(int size) {
            if (size <= 0) {
                throw new IllegalArgumentException("size must be > 0");
            }
            this.size = size;
            return this;
        }

        public Builder timeout(@Nonnull Duration timeout) {
            this.timeout = Objects.requireNonNull(timeout, "Timeout cannot be null");
            return this;
        }

        public Builder reference(@Nullable String reference) {
            this.reference = reference;
            return this;
        }

        public QueryBuilderScrollRequest build() {
            return new QueryBuilderScrollRequest(this);
        }
    }
}
