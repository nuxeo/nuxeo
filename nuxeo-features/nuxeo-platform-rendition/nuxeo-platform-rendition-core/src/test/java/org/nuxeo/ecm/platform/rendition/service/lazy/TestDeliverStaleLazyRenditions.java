/*
 * (C) Copyright 2017 Nuxeo (http://nuxeo.com/) and others.
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
 *     Antoine Taillefer <ataillefer@nuxeo.com>
 */
package org.nuxeo.ecm.platform.rendition.service.lazy;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.GregorianCalendar;
import java.util.stream.IntStream;

import javax.inject.Inject;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.core.event.EventService;
import org.nuxeo.ecm.platform.rendition.Rendition;
import org.nuxeo.ecm.platform.rendition.impl.LazyRendition;
import org.nuxeo.ecm.platform.rendition.service.DummyDocToTxt;
import org.nuxeo.ecm.platform.rendition.service.RenditionFeature;
import org.nuxeo.ecm.platform.rendition.service.RenditionService;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.TransactionalFeature;

/**
 * Check that intermediate stale lazy renditions are delivered.
 *
 * @since 9.3
 */
@RunWith(FeaturesRunner.class)
@Features(RenditionFeature.class)
@Deploy("org.nuxeo.ecm.platform.rendition.core:test-lazy-rendition-contrib.xml")
public class TestDeliverStaleLazyRenditions {

    private static Log log = LogFactory.getLog(TestDeliverStaleLazyRenditions.class);

    protected static int STALE_RENDITION_COUNT = 5;

    protected static int STORED_STALE_RENDITION_COUNT = 2;

    @Inject
    protected CoreSession session;

    @Inject
    protected TransactionalFeature txFeature;

    @Inject
    protected RenditionService rs;

    protected DocumentModel doc;

    @After
    public void tearDown() {
        log.debug(
                "Wait for async completion at teardown to ensure the rendition works are completed before the transient store teardown");
        waitForAsyncCompletion();
    }

    @Test
    public void testLazyRenditions() throws Exception {
        checkInitialRendition(false);
        checkStaleRenditions();
        checkUpToDateRendition(false);
    }

    @Test
    public void testStoredLazyRenditions() throws Exception {
        checkInitialRendition(true);
        checkStoredStaleRenditions();
        // If a stored stale rendition exists, asking for a stored rendition will never return an up-to-date rendition
        // as shown by the previous call, so not calling checkUpToDateRendition
    }

    protected void checkInitialRendition(boolean store) throws IOException {
        log.debug("Create test document");
        doc = session.createDocumentModel("/", "testDoc", "File");
        doc.setPropertyValue("dc:issued", new GregorianCalendar());
        doc = session.createDocument(doc);
        txFeature.nextTransaction();

        log.debug("Ask immediately for a lazy rendition, expecting an empty rendition");
        checkEmptyRendition(doc, "lazyAutomation", store);

        log.debug("Wait for async completion");
        waitForAsyncCompletion();

        log.debug("Ask again for a lazy rendition, should be rendered, expecting an up-to-date rendition");
        checkUpToDateRendition(doc, "lazyAutomation", store, "testDoc.txt", DummyDocToTxt.getDigest(doc));
    }

    protected void checkStaleRenditions() {
        IntStream.rangeClosed(1, STALE_RENDITION_COUNT).forEach(this::checkStaleRendition);
    }

    protected void checkStoredStaleRenditions() {
        String latestRenditionDigest = DummyDocToTxt.getDigest(doc);
        log.debug(String.format("Computed latest rendition digest: %s", latestRenditionDigest));
        IntStream.rangeClosed(1, STORED_STALE_RENDITION_COUNT)
                 .forEach((index) -> checkStoredStaleRendition(index, latestRenditionDigest));
    }

    protected void checkStaleRendition(int count) {
        try {
            log.debug(String.format("Check stale rendition #%d", count));

            String latestRenditionDigest = DummyDocToTxt.getDigest(doc);
            log.debug(String.format("Saved latest rendition digest: %s", latestRenditionDigest));

            log.debug("Update dc:issued on test document");
            Thread.sleep(1000);
            doc.setPropertyValue("dc:issued", new GregorianCalendar());
            session.saveDocument(doc);
            txFeature.nextTransaction();

            log.debug("Ask immediately for a lazy rendition, expecting a stale rendition");
            checkStaleRendition(doc, "lazyAutomation", false, "testDoc.txt", latestRenditionDigest);

            log.debug("Wait for async completion");
            waitForAsyncCompletion();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new NuxeoException(e);
        }
    }

    protected void checkStoredStaleRendition(int count, String latestRenditionDigest) {
        try {
            log.debug(String.format("Check stale rendition #%d", count));

            log.debug("Update dc:issued on test document");
            Thread.sleep(1000);
            doc.setPropertyValue("dc:issued", new GregorianCalendar());
            session.saveDocument(doc);
            txFeature.nextTransaction();

            log.debug("Ask immediately for a lazy rendition, expecting always the same stored stale rendition");
            checkStaleRendition(doc, "lazyAutomation", true, "testDoc.txt", latestRenditionDigest);

            log.debug("Wait for async completion");
            waitForAsyncCompletion();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new NuxeoException(e);
        }
    }

    protected void checkUpToDateRendition(boolean store) {
        log.debug("Ask for a lazy rendition, should be rendered, expecting an up-to-date rendition");
        checkUpToDateRendition(doc, "lazyAutomation", store, "testDoc.txt", DummyDocToTxt.getDigest(doc));
    }

    protected void checkEmptyRendition(DocumentModel doc, String renditionName, boolean store) {
        Rendition rendition = rs.getRendition(doc, renditionName, store);
        assertNotNull(rendition);
        assertFalse(rendition.isStored());
        Blob renditionBlob = rendition.getBlob();
        assertTrue(renditionBlob.getMimeType().contains(LazyRendition.EMPTY_MARKER));
        assertTrue(renditionBlob.getFilename().equals(LazyRendition.IN_PROGRESS_MARKER));
        assertEquals(0, renditionBlob.getLength());
    }

    protected void checkStaleRendition(DocumentModel doc, String renditionName, boolean store, String expectedFilename,
            String expectedDigest) {
        checkRenderedRendition(doc, renditionName, store, true, expectedFilename, expectedDigest);
    }

    protected void checkUpToDateRendition(DocumentModel doc, String renditionName, boolean store,
            String expectedFilename, String expectedDigest) {
        checkRenderedRendition(doc, renditionName, store, false, expectedFilename, expectedDigest);
    }

    protected void checkRenderedRendition(DocumentModel doc, String renditionName, boolean store, boolean stale,
            String expectedFilename, String expectedDigest) {
        Rendition rendition = rs.getRendition(doc, renditionName, store);
        assertNotNull(rendition);
        assertTrue(rendition.isStored() == store);
        Blob blob = rendition.getBlob();
        String mimeType = blob.getMimeType();
        assertFalse(blob.getMimeType().contains(LazyRendition.EMPTY_MARKER));
        assertTrue(stale == mimeType.contains(LazyRendition.STALE_MARKER));
        assertEquals(expectedFilename, blob.getFilename());
        assertTrue(blob.getLength() > 0);
        String digest = blob.getDigest();
        log.debug(String.format("Comparing expected digest %s and %s rendition blob digest %s", expectedDigest,
                stale ? "stale" : "up-to-date", digest));
        assertEquals(expectedDigest, digest);
    }

    protected void waitForAsyncCompletion() {
        Framework.getService(EventService.class).waitForAsyncCompletion(5000);
    }

}
