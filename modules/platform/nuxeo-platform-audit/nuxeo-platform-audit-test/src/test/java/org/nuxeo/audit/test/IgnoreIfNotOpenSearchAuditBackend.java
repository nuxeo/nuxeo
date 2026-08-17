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
 *     Benoit Delbosc
 */
package org.nuxeo.audit.test;

import static org.nuxeo.common.test.configuration.ThirdPartyUnderTest.AUDIT_SERVICE_VALUE;

import org.nuxeo.runtime.test.runner.ConditionalIgnore;

/**
 * Runs a test only on the OpenSearch-family audit backends (OpenSearch and Elasticsearch), which keep their entries in
 * an index surviving runtime restarts and re-prime the shared audit UID sequence from that index on start. The Mem,
 * MongoDB and SQL backends do not re-prime from a surviving store, so a test simulating a sequence reset while the
 * store survives is meaningful only here.
 * <p>
 * The check is value-based ({@code nuxeo.test.audit} starting with {@code opensearch} or {@code elasticsearch}) so it
 * also covers the OpenSearch2 and Elasticsearch9 addons, whose backend classes are not visible from this module.
 *
 * @since 2025.24
 */
public class IgnoreIfNotOpenSearchAuditBackend implements ConditionalIgnore.Condition {

    @Override
    public boolean shouldIgnore() {
        String audit = AUDIT_SERVICE_VALUE == null ? "" : AUDIT_SERVICE_VALUE.toLowerCase();
        return !audit.startsWith("opensearch") && !audit.startsWith("elasticsearch");
    }
}
