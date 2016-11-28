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

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.event.EventService;
import org.nuxeo.ecm.core.work.api.WorkManager;
import org.nuxeo.ecm.platform.audit.api.AuditLogger;
import org.nuxeo.ecm.platform.audit.api.LogEntry;
import org.nuxeo.ecm.platform.audit.service.AuditBackend;
import org.nuxeo.ecm.platform.audit.service.DefaultAuditBackend;
import org.nuxeo.ecm.platform.audit.service.NXAuditEventsService;
import org.nuxeo.elasticsearch.api.ElasticSearchAdmin;
import org.nuxeo.elasticsearch.audit.ESAuditBackend;
import org.nuxeo.elasticsearch.test.RepositoryElasticSearchFeature;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.LocalDeploy;
import org.nuxeo.runtime.test.runner.RuntimeHarness;
import org.nuxeo.runtime.transaction.TransactionHelper;

@Deploy({ "org.nuxeo.ecm.platform.audit.api", "org.nuxeo.runtime.datasource", "org.nuxeo.ecm.core.persistence",
        "org.nuxeo.ecm.platform.audit", "org.nuxeo.ecm.platform.uidgen.core", "org.nuxeo.elasticsearch.seqgen",
        "org.nuxeo.elasticsearch.seqgen.test:elasticsearch-seqgen-index-test-contrib.xml" })
@RunWith(FeaturesRunner.class)
@Features({ RepositoryElasticSearchFeature.class })
@LocalDeploy({ "org.nuxeo.elasticsearch.audit:nxaudit-ds.xml", "org.nuxeo.elasticsearch.audit:nxuidsequencer-ds.xml",
        "org.nuxeo.elasticsearch.audit:elasticsearch-test-contrib.xml",
        "org.nuxeo.elasticsearch.audit:elasticsearch-audit-index-test-contrib.xml",
        "org.nuxeo.elasticsearch.audit:audit-test-contrib.xml" })
@SuppressWarnings("unchecked")
public class TestAuditMigration {

    protected @Inject CoreSession session;

    @Inject
    protected ElasticSearchAdmin esa;

    @Inject
    protected RuntimeHarness harness;

    @Before
    public void setupIndex() throws Exception {
        // make sure that the audit bulker don't drain pending log entries while we reset the index
        LogEntryGen.flushAndSync();
        esa.initIndexes(true);
    }

    @Test
    public void shouldMigrate() throws Exception {

        // start with JPA based Audit
        NXAuditEventsService audit = (NXAuditEventsService) Framework.getRuntime().getComponent(
                NXAuditEventsService.NAME);
        Assert.assertNotNull(audit);
        AuditBackend backend = audit.getBackend();
        Assert.assertNotNull(backend);
        Assert.assertTrue(backend instanceof DefaultAuditBackend);

        // generate some entries
        List<LogEntry> entries = new ArrayList<>();
        AuditLogger logger = Framework.getLocalService(AuditLogger.class);
        Assert.assertNotNull(logger);

        for (int i = 0; i < 1000; i++) {
            entries.add(LogEntryGen.doCreateEntry("mydoc", "evt" + i, "cat" + i % 2));
        }
        logger.addLogEntries(entries);

        TransactionHelper.commitOrRollbackTransaction();
        Framework.getLocalService(EventService.class).waitForAsyncCompletion();
        TransactionHelper.startTransaction();

        List<Long> res = (List<Long>) backend.nativeQuery("select count(*) from LogEntry", 1, 20);
        final long nbEntriesToMigrate = res.get(0).longValue();
        Assert.assertEquals(1000, nbEntriesToMigrate);

        harness.deployBundle("org.nuxeo.elasticsearch.audit");
        backend = audit.getBackend();
        Assert.assertNotNull(backend);
        Assert.assertTrue(backend instanceof ESAuditBackend);

        ESAuditBackend esBackend = (ESAuditBackend) backend;

        esBackend.migrate(100);

        Framework.getLocalService(WorkManager.class).awaitCompletion(1, TimeUnit.MINUTES);

        LogEntryGen.flushAndSync();

        String singleQuery = "            {\n" + "                \"bool\" : {\n" + "                  \"must\" : {\n"
                + "                    \"match\" : {\n" + "                      \"docUUID\" : {\n"
                + "                        \"query\" : \"mydoc\",\n"
                + "                        \"type\" : \"boolean\"\n" + "                      }\n"
                + "                    }\n" + "                  }\n" + "                }\n"
                + "              }          \n" + "";
        List<LogEntry> migratedEntries = (List<LogEntry>) backend.nativeQuery(singleQuery, 0, 1001);
        Assert.assertEquals(1000, migratedEntries.size());
    }

}
