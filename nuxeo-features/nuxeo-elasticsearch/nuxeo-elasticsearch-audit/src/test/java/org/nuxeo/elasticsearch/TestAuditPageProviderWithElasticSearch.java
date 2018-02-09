/*
 * (C) Copyright 2014 Nuxeo SA (http://nuxeo.com/) and others.
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
 *
 */
package org.nuxeo.elasticsearch;

import static org.junit.Assert.assertNotNull;

import java.io.Serializable;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.platform.audit.api.ExtendedInfo;
import org.nuxeo.ecm.platform.audit.api.LogEntry;
import org.nuxeo.ecm.platform.audit.impl.ExtendedInfoImpl;
import org.nuxeo.ecm.platform.audit.impl.LogEntryImpl;
import org.nuxeo.ecm.platform.query.api.PageProvider;
import org.nuxeo.ecm.platform.query.api.PageProviderService;
import org.nuxeo.elasticsearch.api.ElasticSearchAdmin;
import org.nuxeo.elasticsearch.audit.pageprovider.ESAuditPageProvider;
import org.nuxeo.elasticsearch.test.RepositoryElasticSearchFeature;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;

@Deploy({ "org.nuxeo.runtime.metrics", "org.nuxeo.ecm.platform.audit.api", "org.nuxeo.ecm.platform.audit",
        "org.nuxeo.ecm.platform.uidgen.core", "org.nuxeo.elasticsearch.core", "org.nuxeo.elasticsearch.seqgen",
        "org.nuxeo.elasticsearch.core.test:elasticsearch-test-contrib.xml",
        "org.nuxeo.elasticsearch.seqgen.test:elasticsearch-seqgen-index-test-contrib.xml",
        "org.nuxeo.elasticsearch.audit", "org.nuxeo.admin.center" })
@RunWith(FeaturesRunner.class)
@Features({ RepositoryElasticSearchFeature.class })
@Deploy({ "org.nuxeo.elasticsearch.audit:elasticsearch-audit-index-test-contrib.xml",
        "org.nuxeo.elasticsearch.audit:audit-test-contrib.xml",
        "org.nuxeo.elasticsearch.audit:es-audit-pageprovider-test-contrib.xml" })
@SuppressWarnings("unchecked")
public class TestAuditPageProviderWithElasticSearch {

    protected @Inject CoreSession session;

    @Inject
    protected PageProviderService pps;

    @Inject
    protected ElasticSearchAdmin esa;

    @Before
    public void setupIndex() throws Exception {
        // make sure that the audit bulker don't drain pending log entries while we reset the index
        LogEntryGen.flushAndSync();
        esa.initIndexes(true);
    }

    protected Map<String, ExtendedInfo> createExtendedInfos() {
        Map<String, ExtendedInfo> infos = new HashMap<String, ExtendedInfo>();
        ExtendedInfo info = ExtendedInfoImpl.createExtendedInfo(new Long(1));
        infos.put("id", info);
        return infos;
    }

    protected LogEntry doCreateEntry(String docId, String eventId, String category) {
        LogEntry createdEntry = new LogEntryImpl();
        createdEntry.setEventId(eventId);
        createdEntry.setCategory(category);
        createdEntry.setDocUUID(docId);
        createdEntry.setEventDate(new Date());
        createdEntry.setDocPath("/" + docId);
        createdEntry.setRepositoryId("test");
        createdEntry.setExtendedInfos(createExtendedInfos());

        return createdEntry;
    }

    @Test
    public void testSimplePageProvider() throws Exception {

        LogEntryGen.generate("dummy", "entry", "category", 15);
        PageProvider<?> pp = pps.getPageProvider("SimpleESAuditPP", null, Long.valueOf(5), Long.valueOf(0),
                new HashMap<>());
        assertNotNull(pp);

        List<LogEntry> entries = (List<LogEntry>) pp.getCurrentPage();

        Assert.assertEquals(5, entries.size());
        Assert.assertEquals(5, pp.getCurrentPageSize());
        Assert.assertEquals(7, pp.getResultsCount());

        // check that sort does work
        Assert.assertTrue(entries.get(0).getId() < entries.get(1).getId());
        Assert.assertTrue(entries.get(3).getId() < entries.get(4).getId());
    }

    @Test
    public void testSimplePageProviderWithParams() throws Exception {

        LogEntryGen.generate("withParams", "entry", "category", 15);
        PageProvider<?> pp = pps.getPageProvider("SimpleESAuditPPWithParams", null, Long.valueOf(5), Long.valueOf(0),
                new HashMap<String, Serializable>(), "category1");
        assertNotNull(pp);

        List<LogEntry> entries = (List<LogEntry>) pp.getCurrentPage();
        Assert.assertEquals(2, entries.size());

        // check that sort does work
        Assert.assertTrue(entries.get(0).getId() > entries.get(1).getId());

        pp = pps.getPageProvider("SimpleESAuditPPWithParams", null, Long.valueOf(5), Long.valueOf(0),
                new HashMap<String, Serializable>(), "category0");
        entries = (List<LogEntry>) pp.getCurrentPage();
        Assert.assertEquals(1, entries.size());

    }

    @Test
    public void testSimplePageProviderWithUUID() throws Exception {

        LogEntryGen.generate("uuid1", "uentry", "ucategory", 10);
        PageProvider<?> pp = pps.getPageProvider("SearchById", null, Long.valueOf(5), Long.valueOf(0),
                new HashMap<String, Serializable>(), "uuid1");
        assertNotNull(pp);

        List<LogEntry> entries = (List<LogEntry>) pp.getCurrentPage();
        Assert.assertEquals(5, entries.size());
    }

    @Test
    public void testAdminPageProvider() throws Exception {

        LogEntryGen.generate("uuid2", "aentry", "acategory", 10);

        PageProvider<?> pp = pps.getPageProvider("ADMIN_HISTORY", null, Long.valueOf(5), Long.valueOf(0),
                new HashMap<String, Serializable>());
        assertNotNull(pp);
        List<LogEntry> entries = (List<LogEntry>) pp.getCurrentPage();
        Assert.assertTrue(pp.isNextPageAvailable());
        Assert.assertTrue(pp.isLastPageAvailable());
        Assert.assertEquals(5, entries.size());
    }

    @Test
    public void testMaxResultWindow() throws Exception {
        LogEntryGen.generate("uuid2", "aentry", "acategory", 10);

        PageProvider<?> pp = pps.getPageProvider("ADMIN_HISTORY", null, 2L, 0L,
                new HashMap<String, Serializable>());
        // get current page
        List<?> p = pp.getCurrentPage();
        // limit the result window to the 6 first results
        ((ESAuditPageProvider) pp).setMaxResultWindow(6);

        Assert.assertEquals(10, pp.getResultsCount());
        Assert.assertEquals(5, pp.getNumberOfPages());
        Assert.assertTrue(pp.isNextPageAvailable());
        // last page is not accessible
        Assert.assertFalse(pp.isLastPageAvailable());
        // only 3 pages are navigable
        Assert.assertEquals(3, pp.getPageLimit());
        Assert.assertTrue(pp.isNextPageAvailable());
        // page 2
        pp.nextPage();
        Assert.assertTrue(pp.isNextPageAvailable());
        // page 3 reach the max result window of 6 docs
        pp.nextPage();
        Assert.assertFalse(pp.isNextPageAvailable());
        Assert.assertFalse(pp.isLastPageAvailable());
    }

}
