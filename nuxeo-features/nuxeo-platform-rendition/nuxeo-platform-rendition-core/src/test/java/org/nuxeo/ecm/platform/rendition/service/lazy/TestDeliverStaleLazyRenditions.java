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
import org.nuxeo.ecm.core.test.TransactionalFeature;
import org.nuxeo.ecm.platform.rendition.Rendition;
import org.nuxeo.ecm.platform.rendition.impl.LazyRendition;
import org.nuxeo.ecm.platform.rendition.service.DummyDocToTxt;
import org.nuxeo.ecm.platform.rendition.service.RenditionFeature;
import org.nuxeo.ecm.platform.rendition.service.RenditionService;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.LocalDeploy;

/**
 * Check that intermediate stale lazy renditions are delivered.
 *
 * @since 9.3
 */
@RunWith(FeaturesRunner.class)
@Features(RenditionFeature.class)
@LocalDeploy("org.nuxeo.ecm.platform.rendition.core:test-lazy-rendition-contrib.xml")
public class TestDeliverStaleLazyRenditions {

    private static Log log = LogFactory.getLog(TestDeliverStaleLazyRenditions.class);

    protected static int STALE_RENDITION_COUNT = 5;

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
        // never ask to store a rendition
        checkInitialRendition(false);
        checkStaleRenditions(false);
        checkUpToDateRendition(false);

        // ask to store the final up-to-date rendition
        checkInitialRendition(false);
        checkStaleRenditions(false);
        checkUpToDateRendition(true);

        // ask to store the intermediate stale renditions, for test purpose as they will actually never get stored
        checkInitialRendition(false);
        checkStaleRenditions(true);
        checkUpToDateRendition(false);

        // ask to store the intermediate stale renditions, for test purpose as they will actually never get stored
        // ask to store the final up-to-date rendition
        checkInitialRendition(false);
        checkStaleRenditions(true);
        checkUpToDateRendition(true);

        // ask to store the initial up-to-date rendition
        checkInitialRendition(true);
        checkStaleRenditions(false);
        checkUpToDateRendition(false);

        // ask to store the initial up-to-date rendition
        // ask to store the final up-to-date rendition
        checkInitialRendition(true);
        checkStaleRenditions(false);
        checkUpToDateRendition(true);

        // ask to store the initial up-to-date rendition
        // ask to store the intermediate stale renditions, for test purpose as they will actually never get stored
        checkInitialRendition(true);
        checkStaleRenditions(true);
        checkUpToDateRendition(false);

        // ask to store the initial up-to-date rendition
        // ask to store the intermediate stale renditions, for test purpose as they will actually never get stored
        // ask to store the final up-to-date rendition
        checkInitialRendition(true);
        checkStaleRenditions(true);
        checkUpToDateRendition(true);
    }

    /**
     * Creates a test document and asks for a "lazyAutomation" rendition, expecting it to be empty.
     * <p>
     * Waits for asynchronous completion then asks again for the same rendition, expecting it to be up-to-date.
     *
     * @param store whether to ask to store the up-to-date rendition
     */
    protected void checkInitialRendition(boolean store) throws IOException {
        log.debug("Create test document");
        doc = session.createDocumentModel("/", "testDoc", "File");
        doc.setPropertyValue("dc:issued", new GregorianCalendar());
        doc = session.createDocument(doc);
        txFeature.nextTransaction();

        log.debug("Ask immediately for a lazy rendition, expecting an empty rendition");
        checkEmptyRendition(doc, "lazyAutomation");

        log.debug("Wait for async completion");
        waitForAsyncCompletion();

        log.debug("Ask again for a lazy rendition, should be rendered, expecting an up-to-date rendition");
        checkUpToDateRendition(store);
    }

    /**
     * Iterates {@link #STALE_RENDITION_COUNT} times over {@link #checkStaleRendition(boolean)}.
     *
     * @param store whether to ask to store the stale rendition, for test purpose as it will actually never get stored
     */
    protected void checkStaleRenditions(boolean store) {
        IntStream.rangeClosed(1, STALE_RENDITION_COUNT).forEach((index) -> {
            log.debug(String.format("Check stale rendition #%d", index));
            checkStaleRendition(store);
        });
    }

    /**
     * Updates the test document and asks for a "lazyAutomation" rendition, expecting it to be stale.
     * <p>
     * Waits for asynchronous completion.
     *
     * @param store whether to ask to store the stale rendition, for test purpose as it will actually never get stored
     */
    protected void checkStaleRendition(boolean store) {
        try {
            String latestRenditionDigest = DummyDocToTxt.getDigest(doc);
            log.debug(String.format("Saved latest rendition digest: %s", latestRenditionDigest));

            log.debug("Update dc:issued on test document");
            Thread.sleep(1000);
            doc.setPropertyValue("dc:issued", new GregorianCalendar());
            doc = session.saveDocument(doc);
            txFeature.nextTransaction();

            log.debug("Ask immediately for a lazy rendition, expecting a stale rendition");
            checkStaleRendition(doc, "lazyAutomation", store, "testDoc.txt", latestRenditionDigest);

            log.debug("Wait for async completion");
            waitForAsyncCompletion();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new NuxeoException(e);
        }
    }

    /**
     * Asks for a "lazyAutomation" rendition, expecting it to be up-to-date.
     *
     * @param store whether to ask to store the up-to-date rendition
     */
    protected void checkUpToDateRendition(boolean store) {
        log.debug("Ask for a lazy rendition, should be rendered, expecting an up-to-date rendition");
        // An up-to-date stored rendition, ie. not stale, is necessarily stored if asked to be processed and stored
        checkRenderedRendition(doc, "lazyAutomation", store, store, false, "testDoc.txt", DummyDocToTxt.getDigest(doc));
    }

    protected void checkEmptyRendition(DocumentModel doc, String renditionName) {
        Rendition rendition = rs.getRendition(doc, renditionName);
        assertNotNull(rendition);
        assertFalse(rendition.isStored());
        Blob renditionBlob = rendition.getBlob();
        assertTrue(renditionBlob.getMimeType().contains(LazyRendition.EMPTY_MARKER));
        assertTrue(renditionBlob.getFilename().equals(LazyRendition.IN_PROGRESS_MARKER));
        assertEquals(0, renditionBlob.getLength());
    }

    protected void checkStaleRendition(DocumentModel doc, String renditionName, boolean store, String expectedFilename,
            String expectedDigest) {
        // A stale rendition is never stored even if asked to be processed and stored
        checkRenderedRendition(doc, renditionName, store, false, true, expectedFilename, expectedDigest);
    }

    protected void checkRenderedRendition(DocumentModel doc, String renditionName, boolean store, boolean isStored,
            boolean isStale, String expectedFilename, String expectedDigest) {
        Rendition rendition = rs.getRendition(doc, renditionName, store);
        assertNotNull(rendition);
        assertTrue(rendition.isStored() == isStored);
        Blob blob = rendition.getBlob();
        String mimeType = blob.getMimeType();
        assertFalse(blob.getMimeType().contains(LazyRendition.EMPTY_MARKER));
        assertTrue(isStale == mimeType.contains(LazyRendition.STALE_MARKER));
        assertEquals(expectedFilename, blob.getFilename());
        assertTrue(blob.getLength() > 0);
        String digest = blob.getDigest();
        log.debug(String.format("Comparing expected digest %s and %s rendition blob digest %s", expectedDigest,
                isStale ? "stale" : "up-to-date", digest));
        assertEquals(expectedDigest, digest);
    }

    protected void waitForAsyncCompletion() {
        Framework.getService(EventService.class).waitForAsyncCompletion(5000);
    }

}
