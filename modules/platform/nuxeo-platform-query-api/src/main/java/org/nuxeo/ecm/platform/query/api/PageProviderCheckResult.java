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

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.nuxeo.ecm.core.api.SortInfo;

/**
 * @since 2025.19
 */
public record PageProviderCheckResult(String pageProvider, List<SortInfo> orders, long pageSize,
        Map<String, Execution> executions) {

    public PageProviderCheckResult {
        Objects.requireNonNull(pageProvider);
        orders = List.copyOf(orders);
        executions = new LinkedHashMap<>(executions);
    }

    public record Execution(Duration duration, long resultsCount, long resultsCountLimit, List<?> results) {

        public Execution {
            Objects.requireNonNull(duration);
            results = List.copyOf(results);
        }
    }
}
