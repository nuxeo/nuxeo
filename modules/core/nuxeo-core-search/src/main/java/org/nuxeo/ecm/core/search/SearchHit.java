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

import java.io.Serializable;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import jakarta.annotation.Nullable;

import org.apache.commons.lang3.builder.ToStringBuilder;

/**
 * @since 2025.0
 */
public class SearchHit {

    protected final String index;

    protected final String id;

    @Nullable
    protected final String repository;

    @Nullable
    protected final String docId;

    protected final Map<String, Serializable> fields;

    protected final Map<String, List<String>> highlights;

    /**
     * The relevance score assigned by the search backend (e.g. BM25 for full-text, cosine similarity for vector
     * search). {@code null} when no score is available.
     *
     * @since 2025.22
     */
    @Nullable
    protected final Double score;

    protected SearchHit(Builder builder) {
        this.index = builder.index;
        this.id = builder.id;
        this.repository = builder.repository;
        this.docId = builder.docId;
        this.fields = builder.fields;
        this.highlights = builder.highlights;
        this.score = builder.score;
    }

    public String getIndex() {
        return index;
    }

    public String getId() {
        return id;
    }

    @Nullable
    public String getRepository() {
        return repository;
    }

    @Nullable
    public String getDocId() {
        return docId;
    }

    public Map<String, Serializable> getFields() {
        return fields;
    }

    public Map<String, List<String>> getHighlights() {
        return highlights;
    }

    /**
     * Returns the relevance score, or {@code null} if no score is available.
     *
     * @since 2025.22
     */
    @Nullable
    public Double getScore() {
        return score;
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this);
    }

    public static Builder builder(String index, String id) {
        return new Builder(index, id);
    }

    public static class Builder {

        protected final String index;

        protected final String id;

        @Nullable
        protected String repository;

        @Nullable
        protected String docId;

        protected Map<String, Serializable> fields = Map.of();

        protected Map<String, List<String>> highlights = Map.of();

        @Nullable
        protected Double score;

        protected Builder(String index, String id) {
            this.index = index;
            this.id = id;
        }

        public Builder repository(String repository) {
            this.repository = repository;
            return this;
        }

        public Builder docId(String docId) {
            this.docId = docId;
            return this;
        }

        public Builder fields(Map<String, Serializable> fields) {
            this.fields = Collections.unmodifiableMap(fields);
            return this;
        }

        public Builder highlights(Map<String, List<String>> highlights) {
            this.highlights = Collections.unmodifiableMap(highlights);
            return this;
        }

        /**
         * Sets the relevance score for this hit.
         *
         * @since 2025.22
         */
        public Builder score(Double score) {
            this.score = score;
            return this;
        }

        public SearchHit build() {
            return new SearchHit(this);
        }
    }
}
