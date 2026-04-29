/*
 * (C) Copyright 2026 Nuxeo (http://nuxeo.com/) and others.
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
 *     bdelbosc
 */
package org.nuxeo.ecm.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.nuxeo.ecm.core.api.security.SecurityConstants.SYSTEM_USERNAME;

import java.io.IOException;
import java.time.Duration;

import jakarta.inject.Inject;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.api.AbstractSession;
import org.nuxeo.ecm.core.api.Blobs;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.core.api.blobholder.BlobHolder;
import org.nuxeo.ecm.core.api.impl.blob.StringBlob;
import org.nuxeo.ecm.core.blob.DocumentBlobManager;
import org.nuxeo.ecm.core.bulk.BulkService;
import org.nuxeo.ecm.core.bulk.message.BulkCommand;
import org.nuxeo.ecm.core.bulk.message.BulkStatus;
import org.nuxeo.ecm.core.model.Session;
import org.nuxeo.ecm.core.storage.action.FixBinaryFulltextStorageAction;
import org.nuxeo.ecm.core.storage.dbs.IgnoreIfNotDBSRepository;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.ecm.core.test.FulltextStoredInBlobFeature;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.test.runner.ConditionalIgnoreRule;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.TransactionalFeature;
import org.nuxeo.runtime.test.runner.WithFrameworkProperty;

/**
 * Tests for fulltext storedInBlob with a size threshold.
 * <p>
 * With storedInBlob=true and a threshold, small fulltext is stored inline in the repository, while large fulltext is
 * externalized to a blob provider.
 *
 * @since 2025.19
 */
@RunWith(FeaturesRunner.class)
@Features({ CoreFeature.class, FulltextStoredInBlobFeature.class })
@ConditionalIgnoreRule.Ignore(condition = IgnoreIfNotDBSRepository.class, cause = "Fulltext storedInBlobThreshold is DBS only")
@WithFrameworkProperty(name = "nuxeo.test.fulltext.storedInBlobThreshold", value = "100b")
// Enable the bulk action for reverse migration test
@WithFrameworkProperty(name = FulltextStoredInBlobFeature.MIGRATION_KEY, value = "true")
public class TestFulltextStoredInBlobThreshold {

    @Inject
    protected CoreSession coreSession;

    @Inject
    protected TransactionalFeature txFeature;

    @Inject
    protected BulkService bulkService;

    @Test
    public void testSmallFulltextStoredInline() {
        // Create a document with small fulltext (< 100 bytes)
        String smallText = "small text for search";
        var doc = createDocWithBlob("smallDoc", smallText);
        txFeature.nextTransaction();

        // The field value should be inline text (not a blob key)
        String fieldValue = getBinaryFulltextFieldValue(doc);
        assertNotNull("fulltext field should not be null", fieldValue);
        assertFalse("small fulltext should be stored inline, not as blob key",
                AbstractSession.isFulltextValueABlobKey(fieldValue));

        // The fulltext content should be retrievable
        String fulltext = getBinaryFulltextValue(doc);
        assertNotNull(fulltext);
        assertTrue("fulltext should contain the original text", fulltext.trim().contains(smallText));
    }

    @Test
    public void testLargeFulltextStoredAsBlob() {
        // Create a document with large fulltext (>= 100 bytes)
        String largeText = "a]".repeat(60); // 120 bytes > 100 byte threshold
        var doc = createDocWithBlob("largeDoc", largeText);
        txFeature.nextTransaction();

        // The field value should be a blob key
        String fieldValue = getBinaryFulltextFieldValue(doc);
        assertNotNull("fulltext field should not be null", fieldValue);
        assertTrue("large fulltext should be stored as blob key", AbstractSession.isFulltextValueABlobKey(fieldValue));

        // The fulltext content should still be retrievable via the blob
        String fulltext = getBinaryFulltextValue(doc);
        assertNotNull(fulltext);
        assertTrue("fulltext should contain the original text", fulltext.trim().contains(largeText));
    }

    @Test
    public void testMigrationWithThreshold() throws InterruptedException {
        // Create a doc with large fulltext → stored as blob key
        String largeText = "b ".repeat(60); // 120 bytes > threshold
        var largeDoc = createDocWithBlob("largeDoc", largeText);
        // Create a doc with small fulltext → stored inline
        String smallText = "small content";
        var smallDoc = createDocWithBlob("smallDoc", smallText);
        txFeature.nextTransaction();

        // Verify initial state
        assertTrue("large doc should have blob key",
                AbstractSession.isFulltextValueABlobKey(getBinaryFulltextFieldValue(largeDoc)));
        assertFalse("small doc should be inline",
                AbstractSession.isFulltextValueABlobKey(getBinaryFulltextFieldValue(smallDoc)));

        // Force large doc's fulltext to be inline (simulate pre-threshold state)
        String largeFulltext = getBinaryFulltextValue(largeDoc);
        setBinaryFulltextFieldValue(largeDoc, largeFulltext);
        txFeature.nextTransaction();

        // Verify: large doc is now inline (simulated legacy)
        assertFalse("large doc should now be inline (simulated)",
                AbstractSession.isFulltextValueABlobKey(getBinaryFulltextFieldValue(largeDoc)));

        // Run migration
        var commandId = bulkService.submit(new BulkCommand.Builder(FixBinaryFulltextStorageAction.ACTION_NAME,
                "SELECT * FROM Document", SYSTEM_USERNAME).build());
        assertTrue("command timeout", bulkService.await(commandId, Duration.ofSeconds(60)));
        BulkStatus status = bulkService.getStatus(commandId);
        assertEquals(2, status.getTotal());

        txFeature.nextTransaction();

        // After migration: large doc should be back as blob key, small doc stays inline
        assertTrue("large doc should be migrated to blob key",
                AbstractSession.isFulltextValueABlobKey(getBinaryFulltextFieldValue(largeDoc)));
        assertFalse("small doc should remain inline",
                AbstractSession.isFulltextValueABlobKey(getBinaryFulltextFieldValue(smallDoc)));

        // Both should still have correct fulltext
        assertEquals(largeText.trim(), getBinaryFulltextValue(largeDoc).trim());
        assertEquals(smallText, getBinaryFulltextValue(smallDoc).trim());
    }

    @Test
    public void testReverseMigrationBlobToInline() throws InterruptedException {
        // Create a doc with small fulltext → stored inline due to threshold
        String smallText = "tiny text";
        var smallDoc = createDocWithBlob("smallDoc", smallText);
        txFeature.nextTransaction();

        // Verify initial state: small text is inline
        assertFalse("small doc should be inline",
                AbstractSession.isFulltextValueABlobKey(getBinaryFulltextFieldValue(smallDoc)));

        // Force small doc's fulltext to be stored as blob (simulate pre-threshold state)
        forceBinaryFulltextAsBlob(smallDoc, smallText);
        txFeature.nextTransaction();

        // Verify: small doc is now a blob key (simulated legacy without threshold)
        assertTrue("small doc should now be a blob key (simulated)",
                AbstractSession.isFulltextValueABlobKey(getBinaryFulltextFieldValue(smallDoc)));

        // Run migration — should move small text back to inline
        var commandId = bulkService.submit(new BulkCommand.Builder(FixBinaryFulltextStorageAction.ACTION_NAME,
                "SELECT * FROM Document", SYSTEM_USERNAME).build());
        assertTrue("command timeout", bulkService.await(commandId, Duration.ofSeconds(60)));

        txFeature.nextTransaction();

        // After migration: small doc should be back inline
        assertFalse("small doc should be migrated back to inline",
                AbstractSession.isFulltextValueABlobKey(getBinaryFulltextFieldValue(smallDoc)));
        assertEquals(smallText, getBinaryFulltextValue(smallDoc).trim());
    }

    protected String createDocWithBlob(String name, String blobContent) {
        var doc = coreSession.createDocumentModel("/", name, "File");
        BlobHolder holder = doc.getAdapter(BlobHolder.class);
        holder.setBlob(new StringBlob(blobContent));
        doc = coreSession.createDocument(doc);
        return doc.getId();
    }

    @SuppressWarnings("rawtypes")
    protected Session getSession() {
        return ((AbstractSession) coreSession).getSession();
    }

    protected String getBinaryFulltextFieldValue(String docId) {
        return (String) getSession().getDocumentByUUID(docId).getPropertyValue("ecm:fulltextBinary");
    }

    protected String getBinaryFulltextValue(String docId) {
        return coreSession.getDocument(new IdRef(docId)).getBinaryFulltext().get("binarytext");
    }

    protected void setBinaryFulltextFieldValue(String docId, String value) {
        getSession().getDocumentByUUID(docId).setPropertyValue("ecm:fulltextBinary", value);
        getSession().save();
    }

    protected void forceBinaryFulltextAsBlob(String docId, String text) {
        var blobManager = Framework.getService(DocumentBlobManager.class);
        var doc = getSession().getDocumentByUUID(docId);
        try {
            String key = blobManager.writeBlob(Blobs.createBlob(text), doc, "ecm:fulltextBinary");
            doc.setPropertyValue("ecm:fulltextBinary", key);
        } catch (IOException e) {
            throw new NuxeoException(e);
        }
        getSession().save();
    }
}
