/*
 * (C) Copyright 2014 Nuxeo SA (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     Tiry
 *
 */
package org.nuxeo.elasticsearch;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.platform.audit.api.AuditReader;
import org.nuxeo.ecm.platform.audit.api.LogEntry;
import org.nuxeo.ecm.platform.query.api.PageProvider;
import org.nuxeo.ecm.platform.query.api.PageProviderService;
import org.nuxeo.ecm.platform.query.nxql.CoreQueryDocumentPageProvider;
import org.nuxeo.elasticsearch.api.ElasticSearchAdmin;
import org.nuxeo.elasticsearch.test.RepositoryElasticSearchFeature;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.LocalDeploy;

@Deploy({ "org.nuxeo.ecm.platform.audit.api", "org.nuxeo.ecm.platform.audit", "org.nuxeo.ecm.platform.uidgen.core",
        "org.nuxeo.elasticsearch.seqgen",
        "org.nuxeo.elasticsearch.seqgen.test:elasticsearch-seqgen-index-test-contrib.xml",
        "org.nuxeo.elasticsearch.audit" })
@RunWith(FeaturesRunner.class)
@Features({ RepositoryElasticSearchFeature.class })
@LocalDeploy({ "org.nuxeo.elasticsearch.audit:elasticsearch-test-contrib.xml",
        "org.nuxeo.elasticsearch.audit:elasticsearch-audit-index-test-contrib.xml",
        "org.nuxeo.elasticsearch.audit:audit-test-contrib.xml",
        "org.nuxeo.elasticsearch.audit:test-pageprovider-track-contrib.xml" })
public class TestPageProviderTracking {

    protected @Inject CoreSession session;

    @Inject
    protected ElasticSearchAdmin esa;

    @Inject
    protected PageProviderService pps;

    @Before
    public void setupIndex() throws Exception {
        // make sure that the audit bulker don't drain pending log entries while we reset the index
        LogEntryGen.flushAndSync();
        esa.initIndexes(true);
    }

    @Test
    public void shouldLogPageProviderCallsInAudit() throws Exception {

        Map<String, Serializable> props = new HashMap<String, Serializable>();
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

        assertTrue(((String) entry.getExtendedInfos().get("params").getSerializableValue()).contains(session.getRootDocument().getId()));

        pp.getCurrentPage();

        LogEntryGen.flushAndSync();
        trail = reader.queryLogs(new String[] { "search" }, null);
        assertEquals(2, trail.size());

    }

}
