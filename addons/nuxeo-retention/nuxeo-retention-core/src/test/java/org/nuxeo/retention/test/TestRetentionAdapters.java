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

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.Calendar;

import javax.inject.Inject;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.ecm.core.test.DefaultRepositoryInit;
import org.nuxeo.ecm.core.test.annotations.Granularity;
import org.nuxeo.ecm.core.test.annotations.RepositoryConfig;
import org.nuxeo.retention.RetentionConstants;
import org.nuxeo.retention.adapters.Record;
import org.nuxeo.retention.adapters.RetentionRule;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;

/**
 * @since 11.1
 */
@RunWith(FeaturesRunner.class)
@Features({ CoreFeature.class })
@Deploy("org.nuxeo.retention.core:OSGI-INF/retention-core-types.xml")
@Deploy("org.nuxeo.retention.core:OSGI-INF/retention-adapters.xml")
@RepositoryConfig(init = DefaultRepositoryInit.class, cleanup = Granularity.METHOD)
public class TestRetentionAdapters {

    @Inject
    protected CoreSession session;

    @Test
    public void testRecordAdapterRetainDate() {
        DocumentModel file = session.createDocumentModel("/", "File", "File");
        file = session.createDocument(file);
        file.addFacet(RetentionConstants.RECORD_FACET);
        file = session.saveDocument(file);

        Record record = file.getAdapter(Record.class);
        assertFalse(record.isRetainUntilInderterminate());

        session.makeRecord(file.getRef());
        session.setRetainUntil(file.getRef(), CoreSession.RETAIN_UNTIL_INDETERMINATE, null);
        file = session.getDocument(file.getRef());

        record = file.getAdapter(Record.class);
        assertTrue(record.isRetainUntilInderterminate());
        assertFalse(record.isRetentionExpired());

        Calendar retainUntil = Calendar.getInstance();
        retainUntil.add(Calendar.HOUR, -1);
        session.setRetainUntil(file.getRef(), retainUntil, null);
        file = session.getDocument(file.getRef());
        record = file.getAdapter(Record.class);

        assertFalse(record.isRetainUntilInderterminate());
        assertTrue(record.isRetentionExpired());

        retainUntil = Calendar.getInstance();
        retainUntil.add(Calendar.HOUR, 2);
        session.setRetainUntil(file.getRef(), retainUntil, null);
        file = session.getDocument(file.getRef());
        record = file.getAdapter(Record.class);

        assertFalse(record.isRetainUntilInderterminate());
        assertFalse(record.isRetentionExpired());
    }

    @Test
    public void testRecordAdapterPolicy() {
        DocumentModel ruleDoc = session.createDocumentModel("/", "RetentionRule", "RetentionRule");
        RetentionRule rule = ruleDoc.getAdapter(RetentionRule.class);
        rule.setStartingPointPolicy(RetentionRule.StartingPointPolicy.IMMEDIATE);
        rule.setApplicationPolicy(RetentionRule.ApplicationPolicy.AUTO);
        ruleDoc = session.createDocument(ruleDoc);
        ruleDoc = session.saveDocument(ruleDoc);
        rule = ruleDoc.getAdapter(RetentionRule.class);

        assertTrue(rule.isAuto());
        assertFalse(rule.isManual());
        rule.setApplicationPolicy(RetentionRule.ApplicationPolicy.MANUAL);
        assertFalse(rule.isAuto());
        assertTrue(rule.isManual());

        assertTrue(rule.isImmediate());
        assertFalse(rule.isAfterDely());
        assertFalse(rule.isEventBased());
        assertFalse(rule.isMetadataBased());

        rule.setStartingPointPolicy(RetentionRule.StartingPointPolicy.AFTER_DELAY);
        assertFalse(rule.isImmediate());
        assertTrue(rule.isAfterDely());
        assertFalse(rule.isEventBased());
        assertFalse(rule.isMetadataBased());

        rule.setStartingPointPolicy(RetentionRule.StartingPointPolicy.EVENT_BASED);
        assertFalse(rule.isImmediate());
        assertFalse(rule.isAfterDely());
        assertTrue(rule.isEventBased());
        assertFalse(rule.isMetadataBased());

        rule.setStartingPointPolicy(RetentionRule.StartingPointPolicy.METADATA_BASED);
        assertFalse(rule.isImmediate());
        assertFalse(rule.isAfterDely());
        assertFalse(rule.isEventBased());
        assertTrue(rule.isMetadataBased());
    }

    @Test
    public void testMetadataXPathValidity() {
        DocumentModel ruleDoc = session.createDocumentModel("/", "RetentionRule", "RetentionRule");
        RetentionRule rule = ruleDoc.getAdapter(RetentionRule.class);
        rule.setStartingPointPolicy(RetentionRule.StartingPointPolicy.IMMEDIATE);
        rule.setApplicationPolicy(RetentionRule.ApplicationPolicy.MANUAL);
        ruleDoc = session.createDocument(ruleDoc);
        ruleDoc = session.saveDocument(ruleDoc);
        rule = ruleDoc.getAdapter(RetentionRule.class);

        try {
            rule.setMetadataXpath("dc:title");
            fail("Metatada xpath should be of type Date");
        } catch (IllegalArgumentException e) {
            // expected
        }
    }

}
