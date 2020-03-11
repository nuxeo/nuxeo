/*
 * (C) Copyright 2015 Nuxeo SA (http://nuxeo.com/) and others.
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
 * Nuxeo - initial API and implementation
 */

package org.nuxeo.ecm.platform.rendition.service.lazy;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.apache.commons.io.IOUtils;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.event.EventService;
import org.nuxeo.ecm.platform.rendition.Rendition;
import org.nuxeo.ecm.platform.rendition.extension.AutomationRenderer;
import org.nuxeo.ecm.platform.rendition.impl.LazyRendition;
import org.nuxeo.ecm.platform.rendition.service.RenditionFeature;
import org.nuxeo.ecm.platform.rendition.service.RenditionService;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.transaction.TransactionHelper;
import org.nuxeo.transientstore.test.TransientStoreFeature;

import com.google.inject.Inject;

@RunWith(FeaturesRunner.class)
@Features({ RenditionFeature.class, TransientStoreFeature.class })
@Deploy("org.nuxeo.ecm.platform.rendition.core:test-automation-contrib.xml")
@Deploy("org.nuxeo.ecm.platform.rendition.core:test-lazy-rendition-contrib.xml")
/**
 * Check that LazyRendition work via Nuxeo native API
 *
 * @author <a href="mailto:tdelprat@nuxeo.com">Tiry</a>
 */
public class TestLazyAutomationRenditions {

    @Inject
    RenditionService rs;

    @Inject
    EventService eventService;

    @Inject
    CoreSession session;

    @AfterClass
    public static void cleanup() throws Exception {
        //
    }

    @After
    public void tearDown() {
        waitForAsyncCompletion();
    }

    @Test
    public void testRenditions() throws Exception {
        doTestRenditions(false);
    }

    @Test
    public void testStoreRenditions() throws Exception {
        doTestRenditions(true);
    }

    protected void doTestRenditions(boolean store) throws Exception {
        DocumentModel folder = createFolder();
        assertNotNull(rs);
        Rendition rendition = rs.getRendition(folder, "lazyAutomation", store);
        assertNotNull(rendition);
        assertFalse(rendition.isStored());
        Blob blob = rendition.getBlob();
        assertEquals(0, blob.getLength());
        assertTrue(blob.getMimeType().contains("empty=true"));
        assertTrue(blob.getFilename().equals(LazyRendition.IN_PROGRESS_MARKER));
        Thread.sleep(1000);
        Framework.getService(EventService.class).waitForAsyncCompletion(5000);

        rendition = rs.getRendition(folder, "lazyAutomation", store);
        assertEquals(store, rendition.isStored());
        blob = rendition.getBlob();
        assertFalse(blob.getMimeType().contains("empty=true"));
        assertEquals("dummy.txt", blob.getFilename());
        String data = IOUtils.toString(blob.getStream(), UTF_8);
        assertEquals("dummy", data);
    }

    @Test
    public void testGetFilenameWithExtension() {
        assertEquals("a", AutomationRenderer.getFilenameWithExtension("a", null, null));
        assertEquals("a.x", AutomationRenderer.getFilenameWithExtension("a", null, "x"));
        assertEquals("a.txt", AutomationRenderer.getFilenameWithExtension("a", "text/plain", "x"));

        assertEquals("a.", AutomationRenderer.getFilenameWithExtension("a.", null, null));
        assertEquals("a.x", AutomationRenderer.getFilenameWithExtension("a.", null, "x"));
        assertEquals("a.txt", AutomationRenderer.getFilenameWithExtension("a.", "text/plain", "x"));

        assertEquals("a.c", AutomationRenderer.getFilenameWithExtension("a.c", null, null));
        assertEquals("a.x", AutomationRenderer.getFilenameWithExtension("a.c", null, "x"));
        assertEquals("a.txt", AutomationRenderer.getFilenameWithExtension("a.c", "text/plain", "x"));

        assertEquals("a.ar", AutomationRenderer.getFilenameWithExtension("a.ar", null, null));
        assertEquals("a.x", AutomationRenderer.getFilenameWithExtension("a.ar", null, "x"));
        assertEquals("a.txt", AutomationRenderer.getFilenameWithExtension("a.ar", "text/plain", "x"));

        assertEquals("a.doc", AutomationRenderer.getFilenameWithExtension("a.doc", null, null));
        assertEquals("a.x", AutomationRenderer.getFilenameWithExtension("a.doc", null, "x"));
        assertEquals("a.txt", AutomationRenderer.getFilenameWithExtension("a.doc", "text/plain", "x"));

        assertEquals("a.txt", AutomationRenderer.getFilenameWithExtension("a.jpeg", "text/plain", "x"));
        assertEquals("a.txt", AutomationRenderer.getFilenameWithExtension("a.smurf", "text/plain", "x"));
        assertEquals("a.txt", AutomationRenderer.getFilenameWithExtension("a.b c", "text/plain", "x"));
        assertEquals("file.txt", AutomationRenderer.getFilenameWithExtension("file", "text/plain", "x"));
    }

    protected DocumentModel createFolder() {
        DocumentModel folder = session.createDocumentModel("/", "dummy", "Folder");
        folder = session.createDocument(folder);
        TransactionHelper.commitOrRollbackTransaction();
        eventService.waitForAsyncCompletion();
        TransactionHelper.startTransaction();
        folder = session.getDocument(folder.getRef());
        return folder;
    }

    protected void waitForAsyncCompletion() {
        Framework.getService(EventService.class).waitForAsyncCompletion(5000);
    }

}
