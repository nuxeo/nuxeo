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
 *       Kevin Leturc <kleturc@nuxeo.com>
 */
package org.nuxeo.ecm.core.bulk.message;

import static java.util.Objects.requireNonNullElse;
import static org.apache.commons.lang3.StringUtils.isEmpty;

import java.io.Serializable;
import java.time.Duration;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.apache.avro.reflect.AvroDefault;
import org.apache.avro.reflect.AvroEncode;
import org.apache.avro.reflect.Nullable;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;

/**
 * A message representing a bulk command
 *
 * @since 10.2
 */
public class BulkCommand implements Serializable {

    private static final long serialVersionUID = 20200904L;

    protected String id;

    protected String action;

    protected String query;

    // @scince 11.4
    @Nullable
    protected Long queryLimit;

    protected String username;

    @Nullable
    protected String repository;

    protected int bucketSize;

    protected int batchSize;

    // @since 11.5
    @Nullable
    protected Long batchTransactionTimeout;

    // @since 11.1
    @Nullable
    protected String scroller;

    // @since 11.1
    @AvroDefault("false")
    protected boolean genericScroller;

    // @since 11.3
    @AvroDefault("false")
    protected boolean externalScroller;

    @AvroEncode(using = MapAsJsonAsStringEncoding.class)
    protected Map<String, Serializable> params;

    protected BulkCommand() {
        // Empty constructor for Avro decoder
    }

    public BulkCommand(Builder builder) {
        this.id = UUID.randomUUID().toString();
        this.username = builder.username;
        this.repository = builder.repository;
        this.query = builder.query;
        this.queryLimit = builder.queryLimit;
        this.action = builder.action;
        this.bucketSize = builder.bucketSize;
        this.batchSize = builder.batchSize;
        this.batchTransactionTimeout = builder.batchTransactionTimeout;
        this.params = builder.params;
        this.scroller = builder.scroller;
        this.genericScroller = BooleanUtils.toBoolean(builder.genericScroller);
        this.externalScroller = BooleanUtils.toBoolean(builder.externalScroller);
    }

    public String getUsername() {
        return username;
    }

    public String getRepository() {
        return repository;
    }

    public String getQuery() {
        return query;
    }

    public String getAction() {
        return action;
    }

    public String getScroller() {
        return scroller;
    }

    /**
     * True if the command uses a generic scroller.
     *
     * @since 11.1
     */
    public boolean useGenericScroller() {
        return genericScroller;
    }

    /**
     * True if the command uses an external scroller.
     *
     * @since 11.3
     */
    public boolean useExternalScroller() {
        return externalScroller;
    }

    public Map<String, Serializable> getParams() {
        return Collections.unmodifiableMap(params);
    }

    @SuppressWarnings("unchecked")
    public <T> T getParam(String key) {
        return (T) params.get(key);
    }

    public String getId() {
        return id;
    }

    public int getBucketSize() {
        return bucketSize;
    }

    public int getBatchSize() {
        return batchSize;
    }

    /**
     * Precision is second.
     *
     * @since 11.5
     */
    public Duration getBatchTransactionTimeout() {
        return Duration.ofSeconds(requireNonNullElse(batchTransactionTimeout, 0L));
    }

    /**
     * When greater than 0, the limit applied to the query results
     *
     * @since 11.4
     */
    public Long getQueryLimit() {
        return queryLimit;
    }

    @Override
    public int hashCode() {
        return HashCodeBuilder.reflectionHashCode(this);
    }

    @Override
    public boolean equals(Object o) {
        return EqualsBuilder.reflectionEquals(this, o);
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this);
    }

    public void setQueryLimit(Long limit) {
        this.queryLimit = limit;
    }

    public void setBatchSize(int batchSize) {
        this.batchSize = batchSize;
    }

    public void setBucketSize(int bucketSize) {
        this.bucketSize = bucketSize;
    }

    /**
     * Precision is second.
     *
     * @since 11.5
     */
    public void setBatchTransactionTimeout(Duration timeout) {
        this.batchTransactionTimeout = timeout.toSeconds();
    }

    public void setRepository(String repository) {
        this.repository = repository;
    }

    public void setScroller(String scrollerName) {
        this.scroller = scrollerName;
    }

    public static class Builder {
        protected final String action;

        protected final String query;

        protected Long queryLimit;

        protected String repository;

        protected String username;

        protected int bucketSize;

        protected int batchSize;

        protected Long batchTransactionTimeout;

        protected String scroller;

        protected Boolean genericScroller;

        protected Boolean externalScroller;

        protected Map<String, Serializable> params = new HashMap<>();

        /**
         * BulkCommand builder
         *
         * @param action the registered bulk action name
         * @param query by default an NXQL query that represents the document set to apply the action. When using a
         *            generic scroller the query syntax is a convention with the scroller implementation. When using an
         *            external scroller the field is null.
         * @param username the user with whose rights the computation will be executed
         * @since 11.1
         */
        public Builder(String action, String query, String username) {
            if (isEmpty(action)) {
                throw new IllegalArgumentException("Action cannot be empty");
            }
            this.action = action;
            if (isEmpty(query)) {
                throw new IllegalArgumentException("Query cannot be empty");
            }
            this.query = query;
            if (isEmpty(username)) {
                throw new IllegalArgumentException("Username cannot be empty");
            }
            this.username = username;
        }

        /**
         * BulkCommand builder
         *
         * @param action the registered bulk action name
         * @param nxqlQuery the query that represent the document set to apply the action
         * @deprecated since 11.1, use {@link #Builder(String, String, String)} constructor with username instead
         */
        @Deprecated
        public Builder(String action, String nxqlQuery) {
            if (isEmpty(action)) {
                throw new IllegalArgumentException("Action cannot be empty");
            }
            this.action = action;
            if (isEmpty(nxqlQuery)) {
                throw new IllegalArgumentException("Query cannot be empty");
            }
            this.query = nxqlQuery;
        }

        /**
         * Use a non default document repository
         */
        public Builder repository(String name) {
            this.repository = name;
            return this;
        }

        /**
         * Limits the query result.
         *
         * @since 11.4
         */
        public Builder queryLimit(long limit) {
            if (limit <= 0) {
                throw new IllegalArgumentException(String.format("Invalid limit: %d, must be > 0", limit));
            }
            this.queryLimit = limit;
            return this;
        }

        /**
         * Unlimited query results, this will override the action defaultQueryLimit.
         *
         * @since 11.4
         */
        public Builder queryUnlimited() {
            this.queryLimit = 0L;
            return this;
        }

        /**
         * User running the bulk action
         *
         * @deprecated since 11.1, use {@link #Builder(String, String, String)} constructor with username instead
         */
        @Deprecated
        public Builder user(String name) {
            this.username = name;
            return this;
        }

        /**
         * The size of a bucket of documents id that fits into a record
         */
        public Builder bucket(int size) {
            if (size <= 0) {
                throw new IllegalArgumentException("Invalid bucket size must > 0");
            }
            if (batchSize > size) {
                throw new IllegalArgumentException(
                        String.format("Bucket size: %d must be greater or equals to batch size: %d", size, batchSize));
            }
            this.bucketSize = size;
            return this;
        }

        /**
         * The number of documents processed by action within a transaction
         */
        public Builder batch(int size) {
            if (size <= 0) {
                throw new IllegalArgumentException("Invalid batch size must > 0");
            }
            if (bucketSize > 0 && size > bucketSize) {
                throw new IllegalArgumentException(
                        String.format("Bucket size: %d must be greater or equals to batch size: %d", size, batchSize));
            }
            this.batchSize = size;
            return this;
        }

        /**
         * Precision is second.
         *
         * @since 11.5
         */
        public Builder batchTransactionTimeout(Duration timeout) {
            this.batchTransactionTimeout = timeout.toSeconds();
            return this;
        }

        /**
         * Add an action parameter
         */
        public Builder param(String key, Serializable value) {
            if (isEmpty(key)) {
                throw new IllegalArgumentException("Param key cannot be null");
            }
            params.put(key, value);
            return this;
        }

        /**
         * Set all action parameters
         */
        public Builder params(Map<String, Serializable> params) {
            if (params != null && !params.isEmpty()) {
                if (params.containsKey(null)) {
                    throw new IllegalArgumentException("Param key cannot be null");
                }
                this.params = params;
            }
            return this;
        }

        /**
         * Sets scroller name used to materialized the document set
         */
        public Builder scroller(String scrollerName) {
            this.scroller = scrollerName;
            return this;
        }

        /**
         * Uses a generic scroller, the query syntax depends on scroller implementation.
         *
         * @since 11.1
         */
        public Builder useGenericScroller() {
            checkScrollerType();
            this.genericScroller = true;
            return this;
        }

        /**
         * Uses a document scroller, the query must be a valid NXQL query. This is the default.
         *
         * @since 11.1
         */
        public Builder useDocumentScroller() {
            checkScrollerType();
            this.genericScroller = false;
            return this;
        }

        /**
         * Uses an external scroller.
         *
         * @since 11.3
         */
        public Builder useExternalScroller() {
            checkScrollerType();
            this.externalScroller = true;
            return this;
        }

        protected void checkScrollerType() {
            if (this.genericScroller != null || this.externalScroller != null) {
                throw new IllegalArgumentException("Only one useScroller method should be called");
            }
        }

        public BulkCommand build() {
            return new BulkCommand(this);
        }

    }
}
