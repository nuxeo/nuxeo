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

import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.platform.audit.api.AuditReader;
import org.nuxeo.ecm.platform.audit.api.LogEntry;
import org.nuxeo.ecm.platform.query.api.PageProvider;
import org.nuxeo.ecm.platform.query.api.PageProviderService;
import org.nuxeo.ecm.platform.query.nxql.CoreQueryDocumentPageProvider;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.LocalDeploy;

@Deploy({ "org.nuxeo.ecm.platform.query.api", "org.nuxeo.admin.center" })
@RunWith(FeaturesRunner.class)
@Features(MongoDBAuditFeature.class)
@LocalDeploy({ "org.nuxeo.mongodb.audit.test:OSGI-INF/mongodb-audit-pageprovider-test-contrib.xml",
        "org.nuxeo.mongodb.audit.test:OSGI-INF/mongodb-pageprovider-track-test-contrib.xml" })
public class TestPageProviderTrackingWithMongoDB {

    @Inject
    protected CoreSession session;

    @Inject
    protected PageProviderService pps;

    @Test
    public void shouldLogPageProviderCallsInAudit() throws Exception {

        Map<String, Serializable> props = new HashMap<>();
        props.put(CoreQueryDocumentPageProvider.CORE_SESSION_PROPERTY, (Serializable) session);

        PageProvider<?> pp = pps.getPageProvider("CURRENT_DOCUMENT_CHILDREN_TRACK", null, 10L, 0L, props,
                session.getRootDocument().getId());
        assertNotNull(pp);

        AuditReader reader = Framework.getLocalService(AuditReader.class);
        List<LogEntry> trail = reader.queryLogs(new String[] { "search" }, null);

        assertEquals(0, trail.size());

        pp.getCurrentPage();

        LogEntryGen.flushAndSync();
        trail = reader.queryLogs(new String[] { "search" }, null);
        assertEquals(1, trail.size());

        LogEntry entry = trail.get(0);

        assertEquals(session.getPrincipal().getName(), entry.getPrincipalName());

        assertEquals("search", entry.getEventId());

        assertEquals("CURRENT_DOCUMENT_CHILDREN_TRACK",
                entry.getExtendedInfos().get("pageProviderName").getSerializableValue());

        assertEquals(0L, entry.getExtendedInfos().get("pageIndex").getSerializableValue());

        assertEquals(0L, entry.getExtendedInfos().get("resultsCountInPage").getSerializableValue());

        assertTrue(((String) entry.getExtendedInfos().get("params").getSerializableValue()).contains(
                session.getRootDocument().getId()));

        pp.getCurrentPage();

        LogEntryGen.flushAndSync();
        trail = reader.queryLogs(new String[] { "search" }, null);
        assertEquals(2, trail.size());

    }

}
