/*
 * (C) Copyright 2026-2026 Nuxeo (http://nuxeo.com/) and others.
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
package org.nuxeo.audit.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.nuxeo.audit.provider.AuditPageProvider.BACKEND_NAME_PROPERTY;
import static org.nuxeo.audit.service.AuditComponent.DEFAULT_AUDIT_BACKEND;
import static org.nuxeo.audit.test.MultiAuditFeature.OTHER_AUDIT_BACKEND;

import jakarta.inject.Inject;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.audit.AuditCoreFeature;
import org.nuxeo.audit.api.LogEntry;
import org.nuxeo.ecm.platform.query.api.PageProvider;
import org.nuxeo.ecm.platform.query.api.PageProviderService;
import org.nuxeo.ecm.platform.query.api.PageProviderSpec;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;

/**
 * Tests default deployment option when a multi audit backend is configured.
 * 
 * @since 2025.16
 */
@RunWith(FeaturesRunner.class)
@Features(MultiAuditFeature.class)
public class TestMultiAuditPageProvider {

    @Inject
    protected PageProviderService pps;

    @Inject
    protected AuditCoreFeature auditCoreFeature;

    @Test
    public void testPageProviderOnOtherBackend() {
        auditCoreFeature.generateLogEntries(DEFAULT_AUDIT_BACKEND, "doc001", "event", "defaultCategory", 5);
        auditCoreFeature.generateLogEntries(OTHER_AUDIT_BACKEND, "doc002", "event", "otherCategory", 5);

        @SuppressWarnings("unchecked")
        var defaultProvider = (PageProvider<LogEntry>) pps.getPageProvider(
                PageProviderSpec.builder("ADMIN_HISTORY").pageSize(100L).currentPage(0L).build());
        var defaultPage = defaultProvider.getCurrentPage();
        assertEquals(5, defaultPage.size());
        defaultPage.forEach(
                logEntry -> assertTrue("LogEntry: %s category doesn't start with default".formatted(logEntry),
                        logEntry.getCategory().startsWith("defaultCategory")));

        @SuppressWarnings("unchecked")
        var otherProvider = (PageProvider<LogEntry>) pps.getPageProvider(
                PageProviderSpec.builder("ADMIN_HISTORY")
                                .pageSize(100L)
                                .currentPage(0L)
                                .property(BACKEND_NAME_PROPERTY, OTHER_AUDIT_BACKEND)
                                .build());
        var otherPage = otherProvider.getCurrentPage();
        assertEquals(5, otherPage.size());
        otherPage.forEach(logEntry -> assertTrue("LogEntry: %s category doesn't start with other".formatted(logEntry),
                logEntry.getCategory().startsWith("otherCategory")));
    }
}
