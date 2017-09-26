/*
 * (C) Copyright 2017 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Kevin Leturc
 */
package org.nuxeo.mongodb.audit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.HashMap;
import java.util.List;

import javax.inject.Inject;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.platform.audit.api.LogEntry;
import org.nuxeo.ecm.platform.query.api.PageProvider;
import org.nuxeo.ecm.platform.query.api.PageProviderService;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.LocalDeploy;

@Deploy({ "org.nuxeo.ecm.platform.query.api", "org.nuxeo.admin.center" })
@RunWith(FeaturesRunner.class)
@Features(MongoDBAuditFeature.class)
@LocalDeploy({ "org.nuxeo.mongodb.audit.test:OSGI-INF/mongodb-audit-pageprovider-test-contrib.xml" })
@SuppressWarnings("unchecked")
public class TestAuditPageProviderWithMongoDB {

    @Inject
    protected CoreSession session;

    @Inject
    protected PageProviderService pps;

    @Test
    public void testSimplePageProvider() throws Exception {

        LogEntryGen.generate("dummy", "entry", "category", 15);
        PageProvider<?> pp = pps.getPageProvider("SimpleMongoDBAuditPP", null, Long.valueOf(5), Long.valueOf(0),
                new HashMap<>());
        assertNotNull(pp);

        List<LogEntry> entries = (List<LogEntry>) pp.getCurrentPage();

        assertEquals(5, entries.size());
        assertEquals(5, pp.getCurrentPageSize());
        assertEquals(7, pp.getResultsCount());

        // check that sort does work
        assertTrue(entries.get(0).getId() < entries.get(1).getId());
        assertTrue(entries.get(3).getId() < entries.get(4).getId());
    }

    @Test
    public void testSimplePageProviderWithParams() throws Exception {

        LogEntryGen.generate("withParams", "entry", "category", 15);
        PageProvider<?> pp = pps.getPageProvider("SimpleMongoDBAuditPPWithParams", null, Long.valueOf(5),
                Long.valueOf(0), new HashMap<>(), "category1");
        assertNotNull(pp);

        List<LogEntry> entries = (List<LogEntry>) pp.getCurrentPage();
        assertEquals(2, entries.size());

        // check that sort does work
        assertTrue(entries.get(0).getId() > entries.get(1).getId());

        pp = pps.getPageProvider("SimpleMongoDBAuditPPWithParams", null, Long.valueOf(5), Long.valueOf(0),
                new HashMap<>(), "category0");
        entries = (List<LogEntry>) pp.getCurrentPage();
        assertEquals(1, entries.size());

    }

    @Test
    public void testSimplePageProviderWithUUID() throws Exception {

        LogEntryGen.generate("uuid1", "uentry", "ucategory", 10);
        PageProvider<?> pp = pps.getPageProvider("SearchById", null, Long.valueOf(5), Long.valueOf(0), new HashMap<>(),
                "uuid1");
        assertNotNull(pp);

        List<LogEntry> entries = (List<LogEntry>) pp.getCurrentPage();
        assertEquals(5, entries.size());
    }

    @Test
    public void testAdminPageProvider() throws Exception {

        LogEntryGen.generate("uuid2", "aentry", "acategory", 10);

        PageProvider<?> pp = pps.getPageProvider("ADMIN_HISTORY", null, Long.valueOf(5), Long.valueOf(0),
                new HashMap<>());
        assertNotNull(pp);

        List<LogEntry> entries = (List<LogEntry>) pp.getCurrentPage();
        assertEquals(5, entries.size());
    }

}
