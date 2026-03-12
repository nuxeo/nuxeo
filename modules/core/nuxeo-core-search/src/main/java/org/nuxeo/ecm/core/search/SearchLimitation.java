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
 *     bdelbosc
 */
package org.nuxeo.ecm.core.search;

/**
 * Represents a limitation encountered while executing a search query. Enables consumers to distinguish root causes
 * (e.g. client unsupported vs. index mapping vs. operator not supported) and take appropriate action.
 *
 * @since 2025.17
 */
public interface SearchLimitation {

    /**
     * Category of limitation.
     */
    LimitationKind getKind();

    /**
     * High-level capability affected (for backward compatibility with {@link SearchResponse#getMissingCapabilities()}).
     */
    SearchClient.Capability getAffectedCapability();

    /**
     * Human-readable message for logging or UI.
     */
    String getMessage();

    /**
     * Creates a limitation with the given parameters.
     */
    static SearchLimitation of(LimitationKind kind, SearchClient.Capability affectedCapability, String message) {
        return new SearchLimitationImpl(kind, affectedCapability, message);
    }
}
