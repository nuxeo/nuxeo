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
package org.nuxeo.audit.test.scroll;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.nuxeo.audit.service.AuditComponent.DEFAULT_AUDIT_BACKEND;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.stream.IntStream;

import jakarta.inject.Inject;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.audit.api.LogEntry;
import org.nuxeo.audit.test.AuditFeature;
import org.nuxeo.ecm.core.api.scroll.Scroll;
import org.nuxeo.ecm.core.api.scroll.ScrollService;
import org.nuxeo.ecm.core.query.scroll.QueryBuilderScrollRequest;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;

/**
 * @since 2025.18
 */
@RunWith(FeaturesRunner.class)
@Features(AuditFeature.class)
public class TestAuditScroll {

    @Inject
    protected AuditFeature auditFeature;

    @Inject
    protected ScrollService scrollService;

    @Test
    public void testScroll() {
        auditFeature.generateLogEntries(50, idx -> LogEntry.builder("test", new Date()).build());

        var request = QueryBuilderScrollRequest.builder("audit", "SELECT * FROM " + DEFAULT_AUDIT_BACKEND).build();
        List<String> expectedIds = IntStream.rangeClosed(1, 50).mapToObj(String::valueOf).toList();
        assertTrue(scrollService.exists(request));
        try (Scroll scroll = scrollService.scroll(request)) {
            List<String> actualIds = new ArrayList<>();
            assertNotNull(scroll);
            int i = 0;
            do {
                assertTrue(scroll.hasNext());
                List<String> next = scroll.next();
                assertTrue("Unexpected scrolled entries", i + next.size() <= expectedIds.size());
                actualIds.addAll(next);
                i += next.size();
            } while (i < expectedIds.size());
            assertEquals("Unexpected scrolled entries", expectedIds, actualIds);
            assertFalse(scroll.hasNext());
            assertThrows("Should not be able to scroll beyond limit.", NoSuchElementException.class, scroll::next);
        }
    }
}
