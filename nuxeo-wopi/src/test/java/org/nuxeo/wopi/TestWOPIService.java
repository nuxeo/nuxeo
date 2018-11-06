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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.nuxeo.ecm.core.api.Blobs.createBlob;

import javax.inject.Inject;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.Blobs;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;

/**
 * @since 10.3
 */
@RunWith(FeaturesRunner.class)
@Features(WOPIFeature.class)
public class TestWOPIService {

    @Inject
    protected CoreSession session;

    @Inject
    protected WOPIService wopiService;

    @Test
    public void testLoadDiscovery() {
        assertTrue(wopiService.isEnabled());
        WOPIServiceImpl wopiServiceImpl = (WOPIServiceImpl) wopiService;
        assertEquals(3, wopiServiceImpl.extensionAppNames.size());
        assertEquals("Excel", wopiServiceImpl.extensionAppNames.get("xlsx"));
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
        assertNull(wopiService.getActionURL(blob, "view"));
        assertNull(wopiService.getActionURL(blob, "edit"));

        // extension not supported by WOPI
        blob.setFilename("file.txt");
        assertNull(wopiService.getActionURL(blob, "view"));
        assertNull(wopiService.getActionURL(blob, "edit"));

        // extension not supported by Nuxeo
        blob.setFilename("file.pdf");
        assertNull(wopiService.getActionURL(blob, "view"));
        assertNull(wopiService.getActionURL(blob, "edit"));

        // Excel
        blob.setFilename("file.xlsx");
        assertEquals("https://excel.officeapps-df.live.com/x/_layouts/xlviewerinternal.aspx?",
                wopiService.getActionURL(blob, "view"));
        assertEquals("https://excel.officeapps-df.live.com/x/_layouts/xlviewerinternal.aspx?edit=1&",
                wopiService.getActionURL(blob, "edit"));

        // Word
        blob.setFilename("file.rtf");
        assertNull(wopiService.getActionURL(blob, "view"));
        assertEquals("https://word-edit.officeapps-df.live.com/we/wordeditorframe.aspx?",
                wopiService.getActionURL(blob, "edit"));
        blob.setFilename("file.docx");
        assertEquals("https://word-view.officeapps-df.live.com/wv/wordviewerframe.aspx?",
                wopiService.getActionURL(blob, "view"));
    }

    @Test
    public void testGetWOPIBlobInfo() {
        Blob blob = createBlob("dummy content", null, null, "content.xlsx");
        // extension not supported by WOPI
        Blob blobOne = createBlob("one", null, null, "one.bin");
        // extension not supported by Nuxeo
        Blob blobTwo = createBlob("two", null, null, "two.pdf");
        Blob blobThree = createBlob("three", null, null, "three.rtf");

        WOPIBlobInfo info = wopiService.getWOPIBlobInfo(blob);
        assertEquals("Excel", info.appName);
        assertEquals(2, info.actions.size());
        assertTrue(info.actions.contains("view"));
        assertTrue(info.actions.contains("edit"));

        assertNull(wopiService.getWOPIBlobInfo(blobOne));
        assertNull(wopiService.getWOPIBlobInfo(blobTwo));

        info = wopiService.getWOPIBlobInfo(blobThree);
        assertEquals("Word", info.appName);
        assertEquals(1, info.actions.size());
        assertTrue(info.actions.contains("edit"));
    }

}
