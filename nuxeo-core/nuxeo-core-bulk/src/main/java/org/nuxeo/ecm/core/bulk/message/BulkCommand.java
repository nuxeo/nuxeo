/*
 * (C) Copyright 2018 Nuxeo (http://nuxeo.com/) and others.
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

import static org.apache.commons.lang3.StringUtils.isEmpty;

import java.io.Serializable;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.apache.avro.reflect.AvroEncode;
import org.apache.avro.reflect.Nullable;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;

/**
 * A message representing a bulk command
 *
 * @since 10.2
 */
public class BulkCommand implements Serializable {

    private static final long serialVersionUID = 20181021L;

    protected String id;

    protected String action;

    protected String query;

    @Nullable
    protected String username;

    @Nullable
    protected String repository;

    protected int bucketSize;

    protected int batchSize;

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
        this.action = builder.action;
        this.bucketSize = builder.bucketSize;
        this.batchSize = builder.batchSize;
        this.params = builder.params;
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

    public void setBatchSize(int batchSize) {
        this.batchSize = batchSize;
    }

    public void setBucketSize(int bucketSize) {
        this.bucketSize = bucketSize;
    }

    public void setRepository(String repository) {
        this.repository = repository;
    }

    public static class Builder {
        protected final String action;

        protected final String query;

        protected String repository;

        protected String username;

        protected int bucketSize;

        protected int batchSize;

        protected Map<String, Serializable> params = new HashMap<>();

        /**
         * BulkCommand builder
         *
         * @param action the registered bulk action name
         * @param nxqlQuery the query that represent the document set to apply the action
         */
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
         * User running the bulk action
         */
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

        public BulkCommand build() {
            return new BulkCommand(this);
        }

    }
}
