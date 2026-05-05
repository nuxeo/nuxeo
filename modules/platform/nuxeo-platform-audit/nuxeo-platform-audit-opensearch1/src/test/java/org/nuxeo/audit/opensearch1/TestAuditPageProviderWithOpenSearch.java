/*
 * (C) Copyright 2014-2026 Nuxeo (http://nuxeo.com/) and others.
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
 *     Tiry
 */
package org.nuxeo.audit.opensearch1;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.List;
import java.util.Map;

import jakarta.inject.Inject;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.audit.AuditCoreFeature;
import org.nuxeo.audit.api.LogEntry;
import org.nuxeo.audit.opensearch1.pageprovider.OpenSearchAuditPageProvider;
import org.nuxeo.ecm.platform.query.api.PageProvider;
import org.nuxeo.ecm.platform.query.api.PageProviderService;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;

@RunWith(FeaturesRunner.class)
@Features(OpenSearchAuditFeature.class)
@Deploy("org.nuxeo.ecm.platform.audit.tests:test-audit-contrib.xml")
@Deploy("org.nuxeo.audit.opensearch1.test:OSGI-INF/opensearch-audit-pageprovider-test-contrib.xml")
@SuppressWarnings("unchecked")
public class TestAuditPageProviderWithOpenSearch {

    @Inject
    protected PageProviderService pps;

    @Inject
    protected AuditCoreFeature auditCoreFeature;

    @Test
    public void testSimplePageProvider() {
        auditCoreFeature.generateLogEntries("dummy", "entry", "category", 15);
        PageProvider<?> pp = pps.getPageProvider("SimpleESAuditPP", null, 5L, 0L, Map.of());
        assertNotNull(pp);

        var entries = (List<LogEntry>) pp.getCurrentPage();

        assertEquals(5, entries.size());
        assertEquals(5, pp.getCurrentPageSize());
        assertEquals(7, pp.getResultsCount());

        // check that sort does work
        assertTrue(entries.get(0).getId() < entries.get(1).getId());
        assertTrue(entries.get(3).getId() < entries.get(4).getId());
    }

    @Test
    public void testSimplePageProviderWithParams() {
        auditCoreFeature.generateLogEntries("withParams", "entry", "category", 15);
        PageProvider<?> pp = pps.getPageProvider("SimpleESAuditPPWithParams", null, 5L, 0L, Map.of(), "category1");
        assertNotNull(pp);

        var entries = (List<LogEntry>) pp.getCurrentPage();
        assertEquals(2, entries.size());

        // check that sort does work
        assertTrue(entries.get(0).getId() > entries.get(1).getId());

        pp = pps.getPageProvider("SimpleESAuditPPWithParams", null, Long.valueOf(5), Long.valueOf(0), Map.of(),
                "category0");
        entries = (List<LogEntry>) pp.getCurrentPage();
        assertEquals(1, entries.size());

    }

    @Test
    public void testSimplePageProviderWithUUID() {
        auditCoreFeature.generateLogEntries("uuid1", "uentry", "ucategory", 10);
        PageProvider<?> pp = pps.getPageProvider("SearchById", null, 5L, 0L, Map.of(), "uuid1");
        assertNotNull(pp);

        var entries = (List<LogEntry>) pp.getCurrentPage();
        assertEquals(5, entries.size());
    }

    @Test
    public void testAdminPageProvider() {
        auditCoreFeature.generateLogEntries("uuid2", "aentry", "acategory", 10);
        PageProvider<?> pp = pps.getPageProvider("ADMIN_HISTORY", null, 5L, 0L, Map.of());
        assertNotNull(pp);
        var entries = (List<LogEntry>) pp.getCurrentPage();
        assertTrue(pp.isNextPageAvailable());
        assertTrue(pp.isLastPageAvailable());
        assertEquals(5, entries.size());
    }

    @Test
    public void testMaxResultWindow() {
        auditCoreFeature.generateLogEntries("uuid2", "aentry", "acategory", 10);

        PageProvider<?> pp = pps.getPageProvider("ADMIN_HISTORY", null, 2L, 0L, Map.of());
        // get current page
        pp.getCurrentPage();
        // limit the result window to the 6 first results
        ((OpenSearchAuditPageProvider) pp).setMaxResultWindow(6);

        assertEquals(10, pp.getResultsCount());
        assertEquals(5, pp.getNumberOfPages());
        assertTrue(pp.isNextPageAvailable());
        // last page is not accessible
        assertFalse(pp.isLastPageAvailable());
        // only 3 pages are navigable
        assertEquals(3, pp.getPageLimit());
        assertTrue(pp.isNextPageAvailable());
        // page 2
        pp.nextPage();
        assertTrue(pp.isNextPageAvailable());
        // page 3 reach the max result window of 6 docs
        pp.nextPage();
        assertFalse(pp.isNextPageAvailable());
        assertFalse(pp.isLastPageAvailable());
    }

}
