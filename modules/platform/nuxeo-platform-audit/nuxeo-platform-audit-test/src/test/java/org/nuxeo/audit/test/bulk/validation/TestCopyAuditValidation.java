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
package org.nuxeo.audit.test.bulk.validation;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.nuxeo.audit.bulk.CopyAuditAction.ACTION_NAME;
import static org.nuxeo.audit.bulk.CopyAuditAction.PARAMETER_BACKEND_NAME;
import static org.nuxeo.audit.service.AuditComponent.DEFAULT_AUDIT_BACKEND;
import static org.nuxeo.audit.test.MultiAuditFeature.OTHER_AUDIT_BACKEND;
import static org.nuxeo.ecm.core.api.security.SecurityConstants.SYSTEM_USERNAME;

import jakarta.inject.Inject;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.audit.scroll.AuditScroll;
import org.nuxeo.audit.test.MultiAuditFeature;
import org.nuxeo.ecm.core.bulk.BulkService;
import org.nuxeo.ecm.core.bulk.message.BulkCommand;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;

/**
 * @since 2025.19
 */
@RunWith(FeaturesRunner.class)
@Features(MultiAuditFeature.class)
public class TestCopyAuditValidation {

    @Inject
    protected BulkService bulkService;

    @Test
    public void testBackendNameParameterRequired() {
        var e = assertThrows(IllegalArgumentException.class,
                () -> bulkService.submit(new BulkCommand.Builder(ACTION_NAME, "SELECT * FROM default",
                        SYSTEM_USERNAME).useQueryBuilderScroller().scroller(AuditScroll.SCROLL_NAME).build()));
        assertTrue("Exception message is: " + e.getMessage(),
                e.getMessage().startsWith("Invalid backendName in command:"));
    }

    @Test
    public void testBackendNameParameterExists() {
        var e = assertThrows(IllegalArgumentException.class,
                () -> bulkService.submit(new BulkCommand.Builder(ACTION_NAME, "SELECT * FROM default",
                        SYSTEM_USERNAME).useQueryBuilderScroller()
                                        .scroller(AuditScroll.SCROLL_NAME)
                                        .param(PARAMETER_BACKEND_NAME, "does_not_exist")
                                        .build()));
        assertEquals("Unsupported backend name: does_not_exist", e.getMessage());
    }

    @Test
    public void testSourceBackendExists() {
        var e = assertThrows(IllegalArgumentException.class,
                () -> bulkService.submit(new BulkCommand.Builder(ACTION_NAME, "SELECT * FROM does_not_exist",
                        SYSTEM_USERNAME).useQueryBuilderScroller()
                                        .scroller(AuditScroll.SCROLL_NAME)
                                        .param(PARAMETER_BACKEND_NAME, OTHER_AUDIT_BACKEND)
                                        .build()));
        assertEquals("Unsupported backend name: does_not_exist", e.getMessage());
    }

    @Test
    public void testMultipleSourceBackendIsForbidden() {
        var e = assertThrows(IllegalArgumentException.class,
                () -> bulkService.submit(new BulkCommand.Builder(ACTION_NAME, "SELECT * FROM default, does_not_exist",
                        SYSTEM_USERNAME).useQueryBuilderScroller()
                                        .scroller(AuditScroll.SCROLL_NAME)
                                        .param(PARAMETER_BACKEND_NAME, OTHER_AUDIT_BACKEND)
                                        .build()));
        assertEquals("Invalid query with multiple FROM: SELECT * FROM default, does_not_exist", e.getMessage());
    }

    @Test
    public void testBackendParametersAreNotTheSame() {
        var e = assertThrows(IllegalArgumentException.class,
                () -> bulkService.submit(new BulkCommand.Builder(ACTION_NAME, "SELECT * FROM default",
                        SYSTEM_USERNAME).useQueryBuilderScroller()
                                        .scroller(AuditScroll.SCROLL_NAME)
                                        .param(PARAMETER_BACKEND_NAME, DEFAULT_AUDIT_BACKEND)
                                        .build()));
        assertEquals("Source and target backends must be different, backend name: default", e.getMessage());
    }
}
