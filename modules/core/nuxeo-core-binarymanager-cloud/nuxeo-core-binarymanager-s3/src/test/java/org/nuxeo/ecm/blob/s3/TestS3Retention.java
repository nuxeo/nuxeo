/*
 * (C) Copyright 2024 Nuxeo (http://nuxeo.com/) and others.
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
package org.nuxeo.ecm.blob.s3;

import static com.amazonaws.services.s3.model.ObjectLockLegalHoldStatus.OFF;
import static com.amazonaws.services.s3.model.ObjectLockLegalHoldStatus.ON;
import static java.util.Calendar.MILLISECOND;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.awaitility.Awaitility.await;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assume.assumeTrue;
import static org.nuxeo.ecm.core.api.CoreSession.RETAIN_UNTIL_INDETERMINATE;
import static org.nuxeo.ecm.core.blob.KeyStrategy.VER_SEP;

import java.io.Serializable;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

import javax.inject.Inject;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.api.Blobs;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.blob.BlobManager;
import org.nuxeo.ecm.core.blob.ManagedBlob;
import org.nuxeo.ecm.core.blob.TransactionalBlobStore;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.test.runner.BlacklistComponent;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.LogCaptureFeature;
import org.nuxeo.runtime.test.runner.TransactionalFeature;

import com.amazonaws.SdkBaseException;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.GetObjectLegalHoldRequest;
import com.amazonaws.services.s3.model.GetObjectLegalHoldResult;
import com.amazonaws.services.s3.model.GetObjectMetadataRequest;
import com.amazonaws.services.s3.model.GetObjectRetentionRequest;
import com.amazonaws.services.s3.model.ObjectLockLegalHold;
import com.amazonaws.services.s3.model.ObjectLockLegalHoldStatus;
import com.amazonaws.services.s3.model.SetObjectLegalHoldRequest;

/**
 * Requires S3 Object Lock enabled.
 *
 * @since 2025.0
 */
@RunWith(FeaturesRunner.class)
@Features({ CoreFeature.class, LogCaptureFeature.class, S3BlobProviderFeature.class })
@Deploy("org.nuxeo.ecm.core.storage.binarymanager.s3.tests:OSGI-INF/test-blob-provider-s3-record.xml")
@BlacklistComponent("org.nuxeo.ecm.core.storage.cloud.requestcontroller.service.contrib")
public class TestS3Retention {

    @Inject
    protected CoreSession session;

    @Inject
    protected LogCaptureFeature.Result logCaptureResult;

    @Inject
    protected TransactionalFeature txFeature;

    protected DocumentModel doc;

    protected String bucketKey;

    protected String versionId;

    protected String bucketName;

    protected AmazonS3 amazonS3;

    protected void assertNoRetention() {
        var metadata = amazonS3.getObjectMetadata(new GetObjectMetadataRequest(bucketName, bucketKey, versionId));
        assertNull("Object has Object Lock mode set", metadata.getObjectLockLegalHoldStatus());
        assertNull("Object has Object Lock retain until date set", metadata.getObjectLockRetainUntilDate());
    }

    protected void assertObjectLegalHold(ObjectLockLegalHoldStatus expectedStatus) {
        GetObjectLegalHoldResult response = amazonS3.getObjectLegalHold(
                new GetObjectLegalHoldRequest().withBucketName(bucketName).withKey(bucketKey).withVersionId(versionId));
        assertEquals(expectedStatus.toString(), response.getLegalHold().getStatus());
    }

    protected void assertRetention(Date retainUntil) {
        var metadata = amazonS3.getObjectMetadata(new GetObjectMetadataRequest(bucketName, bucketKey, versionId));
        assertEquals(retainUntil, metadata.getObjectLockRetainUntilDate());
    }

    @Before
    public void setUp() {
        S3BlobProvider blobProvider = (S3BlobProvider) Framework.getService(BlobManager.class).getBlobProvider("test");
        assumeTrue("Cannot run test without s3 object lock enabled", blobProvider.config.s3RetentionEnabled);
        // Create a document with blob
        doc = session.createDocumentModel("/", "document", "File");
        doc.setPropertyValue("file:content", (Serializable) Blobs.createBlob("A retainable content"));
        doc = session.createDocument(doc);
        session.makeRecord(doc.getRef());
        txFeature.nextTransaction();

        doc = session.getDocument(doc.getRef());
        ManagedBlob blob = (ManagedBlob) doc.getPropertyValue("file:content");
        assertNotNull(blob);
        String key = blob.getKey().substring("test:".length());
        int seppos = key.indexOf(VER_SEP);
        String objectKey = key.substring(0, seppos);
        versionId = key.substring(seppos + 1);
        bucketKey = blobProvider.config.bucketKey(objectKey);
        amazonS3 = blobProvider.config.amazonS3;
        bucketName = blobProvider.config.bucketName;
    }

    @After
    public void tearDown() {
        if (amazonS3 != null) {
            try {
                // To clean the bucket, wait for retention expired
                await().atMost(2, SECONDS)
                       .pollInterval(200, MILLISECONDS)
                       .until(() -> new Date().after(
                               amazonS3.getObjectRetention(new GetObjectRetentionRequest().withBucketName(bucketName)
                                                                                          .withKey(bucketKey)
                                                                                          .withVersionId(versionId))
                                       .getRetention()
                                       .getRetainUntilDate()));
                // and remove hold
                amazonS3.setObjectLegalHold(
                        new SetObjectLegalHoldRequest().withBucketName(bucketName)
                                                       .withKey(bucketKey)
                                                       .withVersionId(versionId)
                                                       .withLegalHold(new ObjectLockLegalHold().withStatus(OFF)));
            } catch (SdkBaseException e) {
                // Never mind
            }
        }
    }

    @Test
    public void testRetainUntilShortWhileAndExtend() {
        Calendar retainShortWhile = Calendar.getInstance();
        retainShortWhile.add(MILLISECOND, 500);
        session.setRetainUntil(doc.getRef(), retainShortWhile, null);
        txFeature.nextTransaction();
        assertRetention(retainShortWhile.getTime());

        // Extend retention expiration date
        Calendar retainLongerWhile = Calendar.getInstance();
        retainLongerWhile.add(MILLISECOND, 500);
        session.setRetainUntil(doc.getRef(), retainLongerWhile, null);
        txFeature.nextTransaction();
        assertRetention(retainLongerWhile.getTime());
    }

    @Test
    public void testLegalHoldUnHold() {
        session.setLegalHold(doc.getRef(), true, null);
        txFeature.nextTransaction();
        assertObjectLegalHold(ON);

        session.setLegalHold(doc.getRef(), false, null);
        txFeature.nextTransaction();
        assertObjectLegalHold(OFF);
    }

    @Test
    public void testRetainUntilIndeterminate() {
        session.setRetainUntil(doc.getRef(), RETAIN_UNTIL_INDETERMINATE, null);
        txFeature.nextTransaction();
        assertObjectLegalHold(ON);

        Calendar retainShortWhile = Calendar.getInstance();
        retainShortWhile.add(MILLISECOND, 500);
        session.setRetainUntil(doc.getRef(), retainShortWhile, null);
        txFeature.nextTransaction();
        assertObjectLegalHold(OFF);
    }

    @Test
    public void testRetainUntilIndeterminateAndHold() {
        session.setRetainUntil(doc.getRef(), RETAIN_UNTIL_INDETERMINATE, null);
        txFeature.nextTransaction();
        assertObjectLegalHold(ON);

        session.setLegalHold(doc.getRef(), true, bucketKey);
        txFeature.nextTransaction();

        Calendar retainShortWhile = Calendar.getInstance();
        retainShortWhile.add(MILLISECOND, 500);
        session.setRetainUntil(doc.getRef(), retainShortWhile, null);
        txFeature.nextTransaction();
        assertObjectLegalHold(ON);
    }

    @Test
    public void testRetainUntilIndeterminateHoldAndUnhold() {
        session.setRetainUntil(doc.getRef(), RETAIN_UNTIL_INDETERMINATE, null);
        txFeature.nextTransaction();
        assertObjectLegalHold(ON);

        // explicitly set legal hold (including at repository level)
        session.setLegalHold(doc.getRef(), true, bucketKey);
        txFeature.nextTransaction();
        assertObjectLegalHold(ON);

        // explicitly unset legal hold (including at repository level)
        session.setLegalHold(doc.getRef(), false, bucketKey);
        txFeature.nextTransaction();
        assertObjectLegalHold(ON); // due to retention indeterminate

        Calendar retainShortWhile = Calendar.getInstance();
        retainShortWhile.add(MILLISECOND, 500);
        session.setRetainUntil(doc.getRef(), retainShortWhile, null);
        txFeature.nextTransaction();
        assertObjectLegalHold(OFF);
    }

    @Test
    @LogCaptureFeature.FilterOn(logLevel = "ERROR", loggerClass = TransactionalBlobStore.class)
    public void testRetainUntilPastDateSkipsCloudRetention() {
        Instant instant = Instant.now().minus(Duration.ofMillis(500));
        ZonedDateTime zonedDateTime = ZonedDateTime.ofInstant(instant, ZoneId.systemDefault());
        Calendar pastRetention = GregorianCalendar.from(zonedDateTime);
        session.setRetainUntil(doc.getRef(), pastRetention, null);
        txFeature.nextTransaction();

        // Record blob stores are transactional, check no error was logged while committing the transaction
        assertTrue(logCaptureResult.getCaughtEvents().isEmpty());

        // No retention should have been set at cloud level since the date is in the past
        assertNoRetention();
    }

}
