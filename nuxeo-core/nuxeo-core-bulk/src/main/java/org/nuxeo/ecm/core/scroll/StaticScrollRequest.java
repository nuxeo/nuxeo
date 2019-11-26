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

import java.time.Duration;
import java.util.Objects;

import org.nuxeo.ecm.core.api.scroll.ScrollRequest;

/**
 * @since 11.1
 */
public class StaticScrollRequest implements ScrollRequest {

    public static final String STATIC_TYPE = "static";

    public static final Duration STATIC_DURATION = Duration.ofDays(1);

    protected final String query;

    protected final int size;

    protected StaticScrollRequest(Builder builder) {
        this.query = builder.getQuery();
        this.size = builder.getSize();
    }

    @Override
    public String getType() {
        return STATIC_TYPE;
    }

    @Override
    public String getQuery() {
        return query;
    }

    @Override
    public Duration getTimeout() {
        return STATIC_DURATION;
    }

    @Override
    public int getSize() {
        return size;
    }

    @Override
    public String toString() {
        return "StaticScrollRequest{" + "query='" + query + '\'' + ", size=" + size + '}';
    }

    /**
     * Creates a builder using a comma separated list of identifier.
     */
    public static Builder builder(String identifiers) {
        return new Builder(identifiers);
    }

    public static class Builder {

        public static final int DEFAULT_SCROLL_SIZE = 10;

        protected final String query;

        protected int size;

        public Builder(String identifiers) {
            Objects.requireNonNull(identifiers, "identifiers cannot be null");
            this.query = identifiers;
        }

        public Builder scrollSize(int size) {
            if (size <= 0) {
                throw new IllegalArgumentException("size must be > 0");
            }
            this.size = size;
            return this;
        }

        public String getQuery() {
            return query;
        }

        public int getSize() {
            return size == 0 ? DEFAULT_SCROLL_SIZE : size;
        }

        public StaticScrollRequest build() {
            return new StaticScrollRequest(this);
        }
    }

}
