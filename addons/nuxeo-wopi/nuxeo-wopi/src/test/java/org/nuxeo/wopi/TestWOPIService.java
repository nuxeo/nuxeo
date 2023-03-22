/*
 * (C) Copyright 2018 Nuxeo (http://nuxeo.com/) and others.
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
 *     Thomas Roger
 */

package org.nuxeo.wopi;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.nuxeo.wopi.Constants.ACTION_CONVERT;
import static org.nuxeo.wopi.Constants.ACTION_EDIT;
import static org.nuxeo.wopi.Constants.ACTION_VIEW;

import java.io.File;
import java.io.IOException;
import java.util.List;

import javax.inject.Inject;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.common.utils.FileUtils;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.Blobs;
import org.nuxeo.ecm.core.blob.BlobInfo;
import org.nuxeo.ecm.core.blob.BlobManager;
import org.nuxeo.ecm.core.blob.BlobProvider;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.LogCaptureFeature;
import org.nuxeo.runtime.test.runner.LogFeature;

/**
 * @since 10.3
 */
@RunWith(FeaturesRunner.class)
@Features({ WOPIFeature.class, WOPIDiscoveryFeature.class, LogFeature.class, LogCaptureFeature.class })
public class TestWOPIService {

    @Inject
    protected LogFeature logFeature;

    @Inject
    protected LogCaptureFeature.Result logCaptureResult;

    @Inject
    protected WOPIService wopiService;

    @Inject
    protected BlobManager blobManager;

    @Test
    @LogCaptureFeature.FilterOn(logLevel = "ERROR")
    public void testInvalidDiscovery() throws IOException {
        WOPIServiceImpl wopiServiceImpl = (WOPIServiceImpl) wopiService;

        // clear extension mappings loaded by the WOPIDiscoveryFeature
        wopiServiceImpl.extensionAppNames.clear();
        wopiServiceImpl.extensionActionURLs.clear();

        logFeature.hideErrorFromConsoleLog();
        try {
            // try to load some invalid XML bytes
            assertFalse(wopiServiceImpl.loadDiscovery("plain text".getBytes()));
            assertFalse(wopiService.isEnabled());

            // try to load an invalid WOPI discovery
            File invalidDiscoveryFile = FileUtils.getResourceFileFromContext("test-invalid-discovery.xml");
            assertFalse(wopiServiceImpl.loadDiscovery(
                    org.apache.commons.io.FileUtils.readFileToByteArray(invalidDiscoveryFile)));
            assertFalse(wopiService.isEnabled());
        } finally {
            logFeature.restoreConsoleLog();
        }

        List<String> caughtEvents = logCaptureResult.getCaughtEventMessages();
        assertEquals(2, caughtEvents.size());
        assertTrue(caughtEvents.get(0).startsWith("Error while reading WOPI discovery"));
        assertEquals("Invalid WOPI discovery, no net-zone element", caughtEvents.get(1));
    }

    @Test
    public void testDiscoveryLoaded() {
        assertTrue(wopiService.isEnabled());
        WOPIServiceImpl wopiServiceImpl = (WOPIServiceImpl) wopiService;
        assertEquals(4, wopiServiceImpl.extensionAppNames.size());
        assertEquals("Excel", wopiServiceImpl.extensionAppNames.get("xlsx"));
        assertEquals("Excel", wopiServiceImpl.extensionAppNames.get("xls"));
        assertEquals("Word", wopiServiceImpl.extensionAppNames.get("docx"));
        assertEquals("Word", wopiServiceImpl.extensionAppNames.get("rtf"));
        // extensions not supported by WOPI
        assertNull(wopiServiceImpl.extensionAppNames.get("png"));
        assertNull(wopiServiceImpl.extensionAppNames.get("bin"));
        // extension not supported by Nuxeo
        assertNull(wopiServiceImpl.extensionAppNames.get("pdf"));
        // proof keys
        assertNotNull(wopiServiceImpl.proofKey);
        assertNotNull(wopiServiceImpl.oldProofKey);
    }

    @Test
    public void testGetActionURL() {
        Blob blob = Blobs.createBlob("content");
        // no filename
        assertNull(wopiService.getActionURL(blob, ACTION_VIEW));
        assertNull(wopiService.getActionURL(blob, ACTION_EDIT));

        // extension not supported by WOPI
        blob.setFilename("file.txt");
        assertNull(wopiService.getActionURL(blob, ACTION_VIEW));
        assertNull(wopiService.getActionURL(blob, ACTION_EDIT));

        // extension not supported by Nuxeo
        blob.setFilename("file.pdf");
        assertNull(wopiService.getActionURL(blob, ACTION_VIEW));
        assertNull(wopiService.getActionURL(blob, ACTION_EDIT));

        // Excel
        blob.setFilename("file.xlsx");
        assertEquals("https://excel.officeapps-df.live.com/x/_layouts/xlviewerinternal.aspx?IsLicensedUser=1&",
                wopiService.getActionURL(blob, ACTION_VIEW));
        assertEquals("https://excel.officeapps-df.live.com/x/_layouts/xlviewerinternal.aspx?edit=1&IsLicensedUser=1&",
                wopiService.getActionURL(blob, ACTION_EDIT));
        blob.setFilename("file.xls");
        assertEquals("https://excel.officeapps-df.live.com/x/_layouts/ExcelConvertAndEdit.aspx?IsLicensedUser=1&",
                wopiService.getActionURL(blob, ACTION_CONVERT));

        // Word
        blob.setFilename("file.rtf");
        assertNull(wopiService.getActionURL(blob, ACTION_VIEW));
        assertEquals("https://word-edit.officeapps-df.live.com/we/wordeditorframe.aspx?IsLicensedUser=1&",
                wopiService.getActionURL(blob, ACTION_EDIT));
        blob.setFilename("file.docx");
        assertEquals("https://word-view.officeapps-df.live.com/wv/wordviewerframe.aspx?IsLicensedUser=1&",
                wopiService.getActionURL(blob, ACTION_VIEW));
        blob.setFilename("file.DOCX");
        assertEquals("https://word-view.officeapps-df.live.com/wv/wordviewerframe.aspx?IsLicensedUser=1&",
                wopiService.getActionURL(blob, ACTION_VIEW));
    }

    @Test
    public void testGetWOPIBlobInfo() {
        Blob blob = createBlob("dummy content", "content.xlsx");
        // extension not supported by WOPI
        Blob blobOne = createBlob("one", "one.bin");
        // extension not supported by Nuxeo
        Blob blobTwo = createBlob("two", "two.pdf");
        Blob blobThree = createBlob("three", "three.rtf");
        // uppercase extension
        Blob blobFour = createBlob("four", "four.DOCX");

        WOPIBlobInfo info = wopiService.getWOPIBlobInfo(blob);
        assertEquals("Excel", info.appName);
        assertEquals(2, info.actions.size());
        assertTrue(info.actions.contains(ACTION_VIEW));
        assertTrue(info.actions.contains(ACTION_EDIT));

        assertNull(wopiService.getWOPIBlobInfo(blobOne));
        assertNull(wopiService.getWOPIBlobInfo(blobTwo));

        info = wopiService.getWOPIBlobInfo(blobThree);
        assertEquals("Word", info.appName);
        assertEquals(1, info.actions.size());
        assertTrue(info.actions.contains(ACTION_EDIT));

        info = wopiService.getWOPIBlobInfo(blobFour);
        assertEquals("Word", info.appName);
        assertEquals(1, info.actions.size());
        assertTrue(info.actions.contains(ACTION_VIEW));
    }

    // creates a blob that's actually backed by a blob provider, as wopi service requires it
    protected Blob createBlob(String string, String filename) {
        BlobProvider blobProvider = blobManager.getBlobProvider("test");
        try {
            Blob blob = Blobs.createBlob(string, null, null, filename);
            String key = blobProvider.writeBlob(blob);
            // get a blob that comes from the blob provider
            BlobInfo blobInfo = new BlobInfo();
            blobInfo.key = key;
            blobInfo.filename = filename;
            return blobProvider.readBlob(blobInfo);
        } catch (IOException e) {
            throw new UnsupportedOperationException();
        }
    }

}
