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
package org.nuxeo.ecm.core.search.client.opensearch1;

import static org.nuxeo.common.test.configuration.ThirdPartyUnderTest.SEARCH_SERVICE_VALUE;

import org.nuxeo.runtime.test.runner.ConditionalIgnore;

/**
 * Ignore if not a search client based on OpenSearch/Elasticsearch.
 *
 * @since 2025.16
 */
public class IgnoreIfNotOpenSearchBasedSearchClient implements ConditionalIgnore.Condition {

    @Override
    public boolean shouldIgnore() {
        var name = SEARCH_SERVICE_VALUE == null ? "" : SEARCH_SERVICE_VALUE.toLowerCase();
        return !name.startsWith("opensearch") && !name.startsWith("elasticsearch");
    }

    @Override
    public boolean needsRuntime() {
        return false;
    }
}
