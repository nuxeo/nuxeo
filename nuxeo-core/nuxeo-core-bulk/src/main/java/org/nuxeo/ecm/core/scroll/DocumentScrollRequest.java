/*
 * (C) Copyright 2019 Nuxeo (http://nuxeo.com/) and others.
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
package org.nuxeo.ecm.core.scroll;

import static org.apache.commons.lang3.StringUtils.isEmpty;

import java.time.Duration;
import java.util.Objects;

import org.nuxeo.ecm.core.api.repository.RepositoryManager;
import org.nuxeo.ecm.core.api.scroll.ScrollRequest;
import org.nuxeo.runtime.api.Framework;

/**
 * Request to scroll documents.
 *
 * @since 11.1
 */
public class DocumentScrollRequest implements ScrollRequest {

    protected static final String SCROLL_TYPE = "document";

    protected final String name;

    protected final String query;

    protected final String repository;

    protected final Duration timeout;

    protected final int size;

    protected final String username;


    protected DocumentScrollRequest(Builder builder) {
        this.name = builder.getName();
        this.query = builder.getQuery();
        this.timeout = builder.getTimeout();
        this.size = builder.getSize();
        this.username = builder.getUsername();
        this.repository = builder.getRepository();
    }

    @Override
    public String getType() {
        return SCROLL_TYPE;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public int getSize() {
        return size;
    }

    public String getQuery() {
        return query;
    }

    public Duration getTimeout() {
        return timeout;
    }

    public String getUsername() {
        return username;
    }

    public String getRepository() {
        return repository;
    }

    @Override
    public String toString() {
        return "DocumentScrollRequest{" + "name='" + name + '\'' + ", query='" + query + '\'' + ", repository='"
                + repository + '\'' + ", timeout=" + timeout + ", size=" + size + ", username='" + username + '\''
                + '}';
    }

    /**
     * Creates a builder using an NXQL query.
     */
    public static Builder builder(String nxqlQuery) {
        return new Builder(nxqlQuery);
    }

    public static class Builder {

        protected final String query;

        protected String name;

        protected String username;

        protected String repository;

        protected Duration timeout;

        protected int size;

        public static final String UNKNOWN = "unknown";

        public static final int DEFAULT_SCROLL_SIZE = 50;

        public static final Duration DEFAULT_TIMEOUT = Duration.ofSeconds(120);

        protected Builder(String nxqlQuery) {
            this.query = Objects.requireNonNull(nxqlQuery, "NXQL query cannot be null");
        }

        /**
         * Uses a registered scroll implementation, {@code null} for default implementation.
         */
        public Builder name(String name) {
            this.name = name;
            return this;
        }

        /**
         * Maximum duration between iteration on Scroll.
         */
        public Builder timeout(Duration timeout) {
            this.timeout = Objects.requireNonNull(timeout, "Timeout cannot be null");
            return this;
        }

        /**
         * The number of item to fetch.
         */
        public Builder size(int size) {
            if (size <= 0) {
                throw new IllegalArgumentException("size must be > 0");
            }
            this.size = size;
            return this;
        }

        /**
         * The repository to execute the NXQL request.
         */
        public Builder repository(String repository) {
            this.repository = repository;
            return this;
        }

        /**
         * The user executing the NXQL request.
         */
        public Builder username(String username) {
            this.username = username;
            return this;
        }

        public String getName() {
            return name;
        }

        public String getQuery() {
            return query;
        }

        public Duration getTimeout() {
            return timeout == null ? DEFAULT_TIMEOUT : timeout;
        }

        public int getSize() {
            return size == 0 ? DEFAULT_SCROLL_SIZE : size;
        }

        public String getUsername() {
            return username == null ? UNKNOWN : username;
        }

        public String getRepository() {
            if (isEmpty(repository)) {
                RepositoryManager repoManager = Framework.getService(RepositoryManager.class);
                if (repoManager == null) {
                    return UNKNOWN;
                }
                return repoManager.getDefaultRepositoryName();
            }
            return repository;
        }

        public DocumentScrollRequest build() {
            return new DocumentScrollRequest(this);
        }
    }

}
