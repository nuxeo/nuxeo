/*
 * (C) Copyright 2019 Nuxeo (http://nuxeo.com/) and others.
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
 *     Guillaume RENARD
 */
package org.nuxeo.retention.test;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.time.Duration;
import java.util.Arrays;
import java.util.List;

import javax.inject.Inject;

import org.junit.Before;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.automation.test.AutomationFeature;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.bulk.BulkService;
import org.nuxeo.ecm.core.security.RetentionExpiredFinderListener;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.ecm.core.test.DefaultRepositoryInit;
import org.nuxeo.ecm.core.test.annotations.Granularity;
import org.nuxeo.ecm.core.test.annotations.RepositoryConfig;
import org.nuxeo.ecm.platform.test.PlatformFeature;
import org.nuxeo.retention.adapters.RetentionRule;
import org.nuxeo.retention.adapters.RetentionRule.StartingPointPolicy;
import org.nuxeo.retention.service.RetentionManager;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.TransactionalFeature;

/**
 * @since 11.1
 */
@RunWith(FeaturesRunner.class)
@Features({ PlatformFeature.class, TransactionalFeature.class, AutomationFeature.class })
@RepositoryConfig(init = DefaultRepositoryInit.class, cleanup = Granularity.METHOD)
@Deploy("org.nuxeo.retention.core:OSGI-INF/retention-core-types.xml")
@Deploy("org.nuxeo.retention.core:OSGI-INF/retention-adapters.xml")
@Deploy("org.nuxeo.retention.core:OSGI-INF/retention-vocabularies.xml")
@Deploy("org.nuxeo.retention.core:OSGI-INF/retention-content-template.xml")
@Deploy("org.nuxeo.retention.core:OSGI-INF/retention-service-framework.xml")
@Deploy("org.nuxeo.retention.core:OSGI-INF/retention-listeners.xml")
@Deploy("org.nuxeo.retention.core:OSGI-INF/retention-operations.xml")
public class RetentionTestCase {

    @Inject
    protected RetentionManager service;

    @Inject
    protected CoreFeature coreFeature;

    @Inject
    protected CoreSession session;

    @Inject
    protected BulkService bulkService;

    protected DocumentModel file;

    @Before
    public void setup() {
        file = session.createDocumentModel("/", "File", "File");
        file = session.createDocument(file);
        file = session.saveDocument(file);
    }

    protected void assertStillUnderRetentionAfter(DocumentModel doc, RetentionRule rule, int timeoutMillis)
            throws InterruptedException {
        doc = service.attachRule(doc, rule, session);
        assertTrue(doc.isRecord());

        awaitRetentionExpiration(1_000);

        doc = session.getDocument(doc.getRef());

        // it is still under retention and has a retention date
        assertTrue(session.isUnderRetentionOrLegalHold(doc.getRef()));
        assertNotNull(session.getRetainUntil(doc.getRef()));
    }

    protected void awaitRetentionExpiration(long millis) throws InterruptedException {
        // wait a bit more than retention period to pass retention expiration date
        coreFeature.waitForAsyncCompletion();
        Thread.sleep(millis);
        // trigger manually instead of waiting for scheduler
        new RetentionExpiredFinderListener().handleEvent(null);
        assertTrue("Bulk action didn't finish", bulkService.await(Duration.ofSeconds(60)));
        coreFeature.waitForAsyncCompletion();
    }

    protected RetentionRule createRuleWithActions(RetentionRule.ApplicationPolicy policy,
            StartingPointPolicy startingPointPolicy, List<String> docTypes, String startingPointEventId,
            String startingPointExpression, String matadataXPath, long years, long months, long days,
            long durationMillis, List<String> beginActions, List<String> endActions) {
        DocumentModel doc = session.createDocumentModel("/RetentionRules", "testRule", "RetentionRule");
        RetentionRule rule = doc.getAdapter(RetentionRule.class);
        rule.setDurationYears(years);
        rule.setDurationMonths(months);
        rule.setDurationDays(days);
        rule.setApplicationPolicy(policy);
        rule.setStartingPointPolicy(startingPointPolicy);
        rule.setDocTypes(docTypes);
        rule.setStartingPointEvent(startingPointEventId);
        rule.setStartingPointExpression(startingPointExpression);
        rule.setMetadataXpath(matadataXPath);
        rule.setDurationMillis(durationMillis);
        rule.setBeginActions(beginActions);
        rule.setEndActions(endActions);
        doc = session.createDocument(doc);
        return session.saveDocument(rule.getDocument()).getAdapter(RetentionRule.class);
    }

    protected RetentionRule createImmediateRuleMillis(RetentionRule.ApplicationPolicy policy, long durationMillis,
            List<String> beginActions, List<String> endActions) {
        return createRuleWithActions(policy, RetentionRule.StartingPointPolicy.IMMEDIATE, Arrays.asList("File"), null,
                null, null, 0L, 0L, 0L, durationMillis, beginActions, endActions);
    }

    protected RetentionRule createManualImmediateRuleMillis(long durationMillis) {
        return createImmediateRuleMillis(RetentionRule.ApplicationPolicy.MANUAL, durationMillis, null, null);
    }

    protected RetentionRule createManualEventBasedRuleMillis(String eventId, String startingPointExpression,
            long durationMillis) {
        return createRuleWithActions(RetentionRule.ApplicationPolicy.MANUAL,
                RetentionRule.StartingPointPolicy.EVENT_BASED, null, eventId, startingPointExpression, null, 0L, 0L, 0L,
                durationMillis, null, null);
    }

    protected RetentionRule createManualMetadataBasedRuleMillis(String metadataXPath, long durationMillis) {
        return createRuleWithActions(RetentionRule.ApplicationPolicy.MANUAL,
                RetentionRule.StartingPointPolicy.METADATA_BASED, null, null, null, metadataXPath, 0L, 0L, 0L,
                durationMillis, null, null);
    }

}
