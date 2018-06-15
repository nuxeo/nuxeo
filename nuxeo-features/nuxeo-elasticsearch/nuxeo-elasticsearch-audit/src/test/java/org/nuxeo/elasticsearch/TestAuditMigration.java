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

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.automation.AutomationService;
import org.nuxeo.ecm.automation.OperationContext;
import org.nuxeo.ecm.automation.core.operations.services.AuditRestore;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.test.TransactionalFeature;
import org.nuxeo.ecm.core.work.api.WorkManager;
import org.nuxeo.ecm.platform.audit.api.AuditQueryBuilder;
import org.nuxeo.ecm.platform.audit.api.ExtendedInfo;
import org.nuxeo.ecm.platform.audit.api.LogEntry;
import org.nuxeo.ecm.platform.audit.impl.ExtendedInfoImpl;
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

@RunWith(FeaturesRunner.class)
@Features(RepositoryElasticSearchFeature.class)
@Deploy("org.nuxeo.ecm.automation.core")
@Deploy("org.nuxeo.ecm.automation.features")
@Deploy("org.nuxeo.runtime.metrics")
@Deploy("org.nuxeo.ecm.platform.audit.api")
@Deploy("org.nuxeo.runtime.datasource")
@Deploy("org.nuxeo.ecm.core.persistence")
@Deploy("org.nuxeo.ecm.platform.audit")
@Deploy("org.nuxeo.ecm.platform.uidgen.core")
@Deploy("org.nuxeo.elasticsearch.core.test:elasticsearch-test-contrib.xml")
@Deploy("org.nuxeo.elasticsearch.seqgen")
@Deploy("org.nuxeo.elasticsearch.seqgen.test:elasticsearch-seqgen-index-test-contrib.xml")
@Deploy("org.nuxeo.elasticsearch.audit")
@Deploy("org.nuxeo.elasticsearch.audit:audit-jpa-storage-test-contrib.xml")
@Deploy("org.nuxeo.elasticsearch.audit:nxaudit-ds.xml")
@Deploy("org.nuxeo.elasticsearch.audit:nxuidsequencer-ds.xml")
@Deploy("org.nuxeo.elasticsearch.audit:elasticsearch-audit-index-test-contrib.xml")
@Deploy("org.nuxeo.elasticsearch.audit:audit-test-contrib.xml")
public class TestAuditMigration {

    public static final String DEFAULT_AUDIT_STORAGE = "defaultAuditStorage";

    @Inject
    protected CoreSession session;

    @Inject
    protected ElasticSearchAdmin esa;

    @Inject
    protected TransactionalFeature txFeature;

    @Inject
    protected AutomationService automationService;

    protected DefaultAuditBackend jpaBackend;

    @Before
    public void setupIndex() throws Exception {
        // make sure that the audit bulker don't drain pending log entries while we reset the index
        LogEntryGen.flushAndSync();
        esa.initIndexes(true);

        NXAuditEventsService audit = (NXAuditEventsService) Framework.getRuntime()
                                                                     .getComponent(NXAuditEventsService.NAME);
        Assert.assertNotNull(audit);

        // start with JPA based Audit
        jpaBackend = (DefaultAuditBackend) audit.getAuditStorage(DEFAULT_AUDIT_STORAGE);

    }

    @After
    public void tearDown() {
        jpaBackend.getOrCreatePersistenceProvider().run(true, em -> {
            em.createNativeQuery("delete from nxp_logs_mapextinfos").executeUpdate();
            em.createNativeQuery("delete from nxp_logs_extinfo").executeUpdate();
            em.createNativeQuery("delete from nxp_logs").executeUpdate();
        });
    }

    @Test
    public void shouldMigrate() throws Exception {

        // generate some entries
        List<LogEntry> entries = new ArrayList<>();

        for (int i = 0; i < 1000; i++) {
            LogEntry entry = LogEntryGen.doCreateEntry("mydoc", "evt" + i, "cat" + i % 2);
            Map<String, ExtendedInfo> extendedInfos = new HashMap<>();
            extendedInfos.put("json", new ExtendedInfoImpl.StringInfo(
                    "{\"k1\":\"test\", \"k2\":\"test\", \"k3\":{\"k4\":\"test\", \"k5\":\"test\"}}"));
            extendedInfos.put("json2",
                    new ExtendedInfoImpl.StringInfo("[{t1=test1:toto, t2=test1},{t1=test2, t2=test2}]"));
            entry.setExtendedInfos(extendedInfos);
            entries.add(entry);
        }
        jpaBackend.addLogEntries(entries);

        txFeature.nextTransaction();

        List<Long> res = (List<Long>) jpaBackend.nativeQuery("select count(*) from LogEntry", 1, 20);
        final long nbEntriesToMigrate = res.get(0).longValue();
        Assert.assertEquals(1000, nbEntriesToMigrate);

        AuditBackend backend = Framework.getService(AuditBackend.class);
        Assert.assertNotNull(backend);
        Assert.assertTrue(backend instanceof ESAuditBackend);

        ESAuditBackend esBackend = (ESAuditBackend) backend;

        esBackend.migrate(100);

        Framework.getService(WorkManager.class).awaitCompletion(1, TimeUnit.MINUTES);

        LogEntryGen.flushAndSync();

        String singleQuery = "            {\"query\": {\n" + "                \"bool\" : {\n"
                + "                  \"must\" : {\n" + "                    \"match\" : {\n"
                + "                      \"docUUID\" : {\n" + "                        \"query\" : \"mydoc\"\n"
                + "                      }\n"
                + "                    }\n" + "                  }\n" + "                }\n"
                + "              }}          \n" + "";
        List<LogEntry> migratedEntries = (List<LogEntry>) backend.nativeQuery(singleQuery, 0, 1001);
        Assert.assertEquals(1000, migratedEntries.size());
    }

    @Test
    @Deploy("org.nuxeo.elasticsearch.audit:elasticsearch-audit-index-test-override-contrib.xml")
    public void shouldMigrateWithPreviousMapping() throws Exception {
        shouldMigrate();
    }

    @Test
    public void testRestorationFromAuditStorage() throws Exception {

        List<LogEntry> entries = new ArrayList<>();
        for (int i = 0; i < 1000; i++) {
            entries.add(LogEntryGen.doCreateEntry("mydoc", "evt", "cat"));
        }
        jpaBackend.addLogEntries(entries);

        List<LogEntry> originalEntries = jpaBackend.queryLogs(new AuditQueryBuilder());
        Assert.assertEquals(1000, originalEntries.size());

        txFeature.nextTransaction();

        ESAuditBackend esBackend = (ESAuditBackend) Framework.getService(AuditBackend.class);

        esBackend.restore(jpaBackend, 100, 10);
        LogEntryGen.flushAndSync();

        List<LogEntry> migratedEntries = esBackend.queryLogs(new AuditQueryBuilder());
        Assert.assertEquals(1000, migratedEntries.size());
    }

    @Test
    public void testRestorationFromAutomationOperation() throws Exception {

        List<LogEntry> entries = new ArrayList<>();
        for (int i = 0; i < 1000; i++) {
            entries.add(LogEntryGen.doCreateEntry("mydoc", "evt", "cat"));
        }
        jpaBackend.addLogEntries(entries);
        txFeature.nextTransaction();

        OperationContext ctx = new OperationContext(session);
        Map<String, Serializable> params = new HashMap<>();

        params.put("auditStorage", DEFAULT_AUDIT_STORAGE);

        automationService.run(ctx, AuditRestore.ID, params);

        LogEntryGen.flushAndSync();
        ESAuditBackend esBackend = (ESAuditBackend) Framework.getService(AuditBackend.class);
        List<LogEntry> migratedEntries = esBackend.queryLogs(new AuditQueryBuilder());
        Assert.assertEquals(1000, migratedEntries.size());
    }

}
