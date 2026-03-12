/*
 * (C) Copyright 2025-2026 Nuxeo (http://nuxeo.com/) and others.
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
package org.nuxeo.ecm.core.search;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * @param <R> the client search response type
 * @since 2025.0
 */
public abstract class AbstractSearchResponseTransformer<R> implements SearchResponseTransformer<R> {
    protected final Set<SearchClient.Capability> supportedCapabilities;

    protected AbstractSearchResponseTransformer(Set<SearchClient.Capability> supportedCapabilities) {
        this.supportedCapabilities = supportedCapabilities;
    }

    /**
     * Returns limitations encountered for the query. Default implementation derives from
     * {@link #getMissingCapabilities(SearchQuery)} for backward compatibility. Override to provide structured
     * limitations.
     *
     * @since 2025.17
     */
    protected List<SearchLimitation> getLimitations(SearchQuery query) {
        var missing = getMissingCapabilities(query);
        if (missing.isEmpty()) {
            return List.of();
        }
        return missing.stream()
                      .map(cap -> SearchLimitation.of(LimitationKind.UNSUPPORTED, cap,
                              "Client does not support " + cap))
                      .toList();
    }

    protected List<SearchClient.Capability> getMissingCapabilities(SearchQuery query) {
        List<SearchClient.Capability> ret = new ArrayList<>();
        if (query.isMultiRepositories()) {
            addIfNotSupported(ret, SearchClient.Capability.MULTI_REPOSITORIES);
        }
        if (!query.getAggregates().isEmpty()) {
            addIfNotSupported(ret, SearchClient.Capability.AGGREGATE);
        }
        if (!query.getHighlights().isEmpty()) {
            addIfNotSupported(ret, SearchClient.Capability.HIGHLIGHT);
        }
        return ret;
    }

    protected void addIfNotSupported(List<SearchClient.Capability> missingCapabilities,
            SearchClient.Capability capability) {
        if (!supportedCapabilities.contains(capability)) {
            missingCapabilities.add(capability);
        }
    }
}
