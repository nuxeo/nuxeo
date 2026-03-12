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

import static org.nuxeo.ecm.core.search.SearchClient.Capability.INDEXING;
import static org.nuxeo.ecm.core.search.SearchClient.Capability.INIT_INDEX;

import jakarta.inject.Inject;

import org.nuxeo.runtime.test.runner.ConditionalIgnore;

/**
 * @since 2025.0
 */
public class IgnoreIfSearchClientDoesNotHaveInitIndexCapability implements ConditionalIgnore.Condition {

    @Inject
    protected SearchService searchService;

    @Inject
    protected SearchIndexingService searchIndexingService;

    @Override
    public boolean shouldIgnore() {
        var searchIndex = searchService.getSearchIndex(searchService.getDefaultIndexName());
        return !searchIndexingService.getClient(searchIndex.client()).hasCapability(INIT_INDEX);
    }
}
