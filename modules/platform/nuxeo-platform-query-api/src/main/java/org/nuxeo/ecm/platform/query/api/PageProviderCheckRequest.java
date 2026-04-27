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
package org.nuxeo.ecm.platform.query.api;

import static org.apache.commons.lang3.ObjectUtils.getIfNull;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;

/**
 * Request object holding information to run a {@link PageProvider page provider} check.
 * <p>
 * The page provider will be run with the given {@link Execution executions} (generally different properties or
 * parameters).
 * 
 * @since 2025.19
 */
public class PageProviderCheckRequest {

    protected static final int DEFAULT_CHECK_PAGE_SIZE = 10;

    protected final String name;

    protected final long pageSize;

    protected final Function<Object, Object> resultMapper;

    protected final Map<String, Execution> executions;

    protected PageProviderCheckRequest(Builder builder) {
        this.name = builder.name;
        this.pageSize = builder.pageSize == null || builder.pageSize < 1 ? DEFAULT_CHECK_PAGE_SIZE : builder.pageSize;
        this.resultMapper = getIfNull(builder.resultMapper, Function::identity);
        this.executions = new LinkedHashMap<>(builder.executions);
    }

    public String name() {
        return name;
    }

    public long pageSize() {
        return pageSize;
    }

    public Function<Object, Object> resultMapper() {
        return resultMapper;
    }

    public Map<String, Execution> executions() {
        return executions;
    }

    public static Builder builder(String name) {
        return new Builder(name);
    }

    public static class Builder {

        protected final String name;

        protected final Map<String, Execution> executions = new LinkedHashMap<>();

        protected Long pageSize;

        protected Function<Object, Object> resultMapper;

        public Builder(String name) {
            this.name = Objects.requireNonNull(name, "A PageProvider name should be provided");
        }

        public Builder pageSize(Long pageSize) {
            this.pageSize = pageSize;
            return this;
        }

        @SuppressWarnings("unchecked")
        public <T> Builder resultMapper(Function<T, Object> resultMapper) {
            this.resultMapper = (Function<Object, Object>) resultMapper;
            return this;
        }

        public Builder execution(String executionName, Execution execution) {
            executions.put(executionName, execution);
            return this;
        }

        public Builder executions(Map<String, Execution> executions) {
            this.executions.putAll(executions);
            return this;
        }

        public PageProviderCheckRequest build() {
            if (executions.isEmpty()) {
                throw new IllegalArgumentException("No execution to run");
            }
            return new PageProviderCheckRequest(this);
        }
    }

    public static class Execution {

        protected final Map<String, Serializable> properties;

        protected final Object[] parameters;

        protected Execution(Builder builder) {
            this.properties = Map.copyOf(builder.properties);
            this.parameters = builder.parameters.toArray();
        }

        public static Builder builder() {
            return new Builder();
        }

        public Map<String, Serializable> properties() {
            return properties;
        }

        public Object[] parameters() {
            return parameters;
        }

        public static class Builder {

            protected final Map<String, Serializable> properties = new LinkedHashMap<>();

            protected final List<Object> parameters = new ArrayList<>();

            public Builder property(String name, Serializable value) {
                properties.put(name, value);
                return this;
            }

            public Builder properties(Map<String, Serializable> properties) {
                this.properties.putAll(properties);
                return this;
            }

            public Builder parameter(Object parameter) {
                parameters.add(parameter);
                return this;
            }

            public Builder parameters(List<Object> parameters) {
                this.parameters.addAll(parameters);
                return this;
            }

            public Execution build() {
                return new Execution(this);
            }
        }
    }
}
