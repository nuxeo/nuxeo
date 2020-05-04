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

import java.io.Serializable;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.nuxeo.ecm.core.api.scroll.ScrollRequest;

/**
 * Generic Scroll Request.
 *
 * @since 11.1
 */
public class GenericScrollRequest implements ScrollRequest {

    public static final String SCROLL_TYPE = "generic";

    protected final int size;

    protected final String query;

    protected final String scrollName;

    protected final Map<String, Serializable> options;

    protected GenericScrollRequest(Builder builder) {
        this.query = builder.query;
        this.scrollName = builder.scrollerName;
        this.size = builder.getSize();
        this.options = builder.getOptions();
    }

    @Override
    public String getType() {
        return SCROLL_TYPE;
    }

    @Override
    public String getName() {
        return scrollName;
    }

    @Override
    public int getSize() {
        return size;
    }

    public String getQuery() {
        return query;
    }

    public Map<String, Serializable> getOptions() {
        return options;
    }

    public static Builder builder(String scrollName, String query) {
        return new Builder(scrollName, query);
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this);
    }

    public static class Builder {

        public static final int DEFAULT_SCROLL_SIZE = 10;

        protected final String query;

        protected final String scrollerName;

        protected int size;

        protected Map<String, Serializable> options;

        public Builder(String scrollerName, String query) {
            Objects.requireNonNull(scrollerName, "scrollerName cannot be null");
            Objects.requireNonNull(query, "query cannot be null");
            this.scrollerName = scrollerName;
            this.query = query;
        }

        public Builder size(int size) {
            if (size <= 0) {
                throw new IllegalArgumentException("size must be > 0");
            }
            this.size = size;
            return this;
        }

        public int getSize() {
            return size == 0 ? DEFAULT_SCROLL_SIZE : size;
        }

        public Builder options(Map<String, Serializable> options) {
            if (options != null && !options.isEmpty()) {
                if (options.containsKey(null)) {
                    throw new IllegalArgumentException("option key cannot be null");
                }
                this.options = options;
            }
            return this;
        }

        public Map<String, Serializable> getOptions() {
            return options == null ? Collections.emptyMap() : options;
        }

        public GenericScrollRequest build() {
            return new GenericScrollRequest(this);
        }
    }

}
