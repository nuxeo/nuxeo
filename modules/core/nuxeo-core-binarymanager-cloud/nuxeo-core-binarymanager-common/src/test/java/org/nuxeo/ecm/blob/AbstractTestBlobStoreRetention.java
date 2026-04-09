/*
 * (C) Copyright 2025 Nuxeo (http://nuxeo.com/) and others.
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
 *     Guillaume Renard
 */
package org.nuxeo.ecm.blob;

import static org.awaitility.Awaitility.await;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.nuxeo.ecm.core.api.CoreSession.RETAIN_UNTIL_INDETERMINATE;

import java.io.IOException;
import java.io.Serializable;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Calendar;
import java.util.GregorianCalendar;

import jakarta.inject.Inject;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.api.Blobs;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.blob.ManagedBlob;
import org.nuxeo.ecm.core.blob.TransactionalBlobStore;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.runtime.test.runner.ConditionalIgnore;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.LogCaptureFeature;
import org.nuxeo.runtime.test.runner.TransactionalFeature;

/**
 * @since 2025.8
 */
@RunWith(FeaturesRunner.class)
@Features({ CoreFeature.class, LogCaptureFeature.class })
@ConditionalIgnore(condition = IgnoreIfStorageRetentionDisabled.class)
public abstract class AbstractTestBlobStoreRetention<T extends CloudBlobStoreConfiguration, S extends CloudBlobKey<T>> {

    @Inject
    protected CoreSession session;

    @Inject
    protected LogCaptureFeature.Result logCaptureResult;

    @Inject
    protected TransactionalFeature txFeature;

    protected DocumentModel doc;

    protected abstract void assertNoRetention();

    protected abstract void assertObjectHasLegalHold();

    protected abstract void assertObjectHasNotLegalHold();

    protected abstract void assertRetention(Instant retainUntil);

    protected abstract T getConfig();

    protected abstract S getCloudKey();

    protected abstract boolean isRetentionExpired() throws IOException;

    protected abstract void removeLegalHold() throws IOException;

    protected Duration getRetentionDelay() {
        return Duration.ofMillis(500);
    }

    protected Calendar getNextRetentionDelay() {
        Instant instant = Instant.now().plus(getRetentionDelay());
        ZonedDateTime zonedDateTime = ZonedDateTime.ofInstant(instant, ZoneId.systemDefault());
        return GregorianCalendar.from(zonedDateTime);
    }

    protected Calendar getPastRetention() {
        Instant instant = Instant.now().minus(getRetentionDelay());
        ZonedDateTime zonedDateTime = ZonedDateTime.ofInstant(instant, ZoneId.systemDefault());
        return GregorianCalendar.from(zonedDateTime);
    }

    @Before
    public void setUp() {
        // Create a document with blob
        doc = session.createDocumentModel("/", "document", "File");
        doc.setPropertyValue("file:content", (Serializable) Blobs.createBlob("A retainable content"));
        doc = session.createDocument(doc);
        session.makeRecord(doc.getRef());
        txFeature.nextTransaction();

        doc = session.getDocument(doc.getRef());
        ManagedBlob blob = (ManagedBlob) doc.getPropertyValue("file:content");
        assertNotNull(blob);
    }

    @After
    public void tearDown() {
        try {
            // remove hold
            removeLegalHold();
            // to clean the bucket, wait for retention expired
            await().atMost(getRetentionDelay().multipliedBy(2).plus(Duration.ofSeconds(1)))
                   .pollInterval(Duration.ofMillis(200))
                   .until(this::isRetentionExpired);

        } catch (IOException e) {
            // Never mind
        }
    }

    @Test
    public void testRetainUntilShortWhileAndExtend() {
        Calendar retainShortWhile = getNextRetentionDelay();
        session.setRetainUntil(doc.getRef(), retainShortWhile, null);
        txFeature.nextTransaction();
        assertRetention(retainShortWhile.toInstant());

        // Extend retention expiration date
        Calendar retainLongerWhile = getNextRetentionDelay();
        session.setRetainUntil(doc.getRef(), retainLongerWhile, null);
        txFeature.nextTransaction();
        assertRetention(retainLongerWhile.toInstant());
    }

    @Test
    public void testLegalHoldUnHold() {
        session.setLegalHold(doc.getRef(), true, null);
        txFeature.nextTransaction();
        assertObjectHasLegalHold();

        session.setLegalHold(doc.getRef(), false, null);
        txFeature.nextTransaction();
        assertObjectHasNotLegalHold();
    }

    @Test
    public void testRetainUntilIndeterminate() {
        session.setRetainUntil(doc.getRef(), RETAIN_UNTIL_INDETERMINATE, null);
        txFeature.nextTransaction();
        assertObjectHasLegalHold();

        Calendar retainShortWhile = getNextRetentionDelay();
        session.setRetainUntil(doc.getRef(), retainShortWhile, null);
        txFeature.nextTransaction();
        assertObjectHasNotLegalHold();
    }

    @Test
    public void testRetainUntilIndeterminateAndHold() {
        session.setRetainUntil(doc.getRef(), RETAIN_UNTIL_INDETERMINATE, null);
        txFeature.nextTransaction();
        assertObjectHasLegalHold();

        session.setLegalHold(doc.getRef(), true, null);
        txFeature.nextTransaction();

        Calendar retainShortWhile = getNextRetentionDelay();
        session.setRetainUntil(doc.getRef(), retainShortWhile, null);
        txFeature.nextTransaction();
        assertObjectHasLegalHold();
    }

    @Test
    public void testRetainUntilIndeterminateHoldAndUnhold() {
        session.setRetainUntil(doc.getRef(), RETAIN_UNTIL_INDETERMINATE, null);
        txFeature.nextTransaction();
        assertObjectHasLegalHold();

        // explicitly set legal hold (including at repository level)
        session.setLegalHold(doc.getRef(), true, null);
        txFeature.nextTransaction();
        assertObjectHasLegalHold();

        // explicitly unset legal hold (including at repository level)
        session.setLegalHold(doc.getRef(), false, null);
        txFeature.nextTransaction();
        assertObjectHasLegalHold(); // due to retention indeterminate

        Calendar retainShortWhile = getNextRetentionDelay();
        session.setRetainUntil(doc.getRef(), retainShortWhile, null);
        txFeature.nextTransaction();
        assertObjectHasNotLegalHold();
    }

    @Test
    @LogCaptureFeature.FilterOn(logLevel = "ERROR", loggerClass = TransactionalBlobStore.class)
    public void testRetainUntilPastDateSkipsCloudRetention() {
        Calendar pastRetention = getPastRetention();
        session.setRetainUntil(doc.getRef(), pastRetention, null);
        txFeature.nextTransaction();

        // Record blob stores are transactional, check no error was logged while committing the transaction
        assertTrue(logCaptureResult.getCaughtEvents().isEmpty());

        // No retention should have been set at cloud level since the date is in the past
        assertNoRetention();
    }

}
