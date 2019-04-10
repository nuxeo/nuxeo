/*
 * (C) Copyright 2016 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Thibaud Arguillere
 *     Miguel Nixo
 */
package org.nuxeo.ecm.platform.pdf.tests;

import java.io.File;
import java.util.List;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.common.utils.FileUtils;
import org.nuxeo.ecm.automation.AutomationService;
import org.nuxeo.ecm.automation.OperationChain;
import org.nuxeo.ecm.automation.OperationContext;
import org.nuxeo.ecm.automation.test.AutomationFeature;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.impl.blob.FileBlob;
import org.nuxeo.ecm.platform.pdf.LinkInfo;
import org.nuxeo.ecm.platform.pdf.PDFLinks;
import org.nuxeo.ecm.platform.pdf.operations.PDFExtractLinksOperation;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import javax.inject.Inject;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;

@RunWith(FeaturesRunner.class)
@Features({ AutomationFeature.class })
@Deploy("org.nuxeo.ecm.platform.pdf")
public class PDFLinksTest {

    private static final String PDF_LINKED_2_LOCAL_PATH = TestUtils.PDF_LINKED_2_PATH.replace("files/", "");

    private static final String PDF_LINKED_3_LOCAL_PATH = TestUtils.PDF_LINKED_3_PATH.replace("files/", "");
    
    @Inject
    CoreSession coreSession;

    @Inject
    AutomationService automationService;

    @Test
    public void testLaunchAndRemoteLinks() throws Exception {
        File f = FileUtils.getResourceFileFromContext(TestUtils.PDF_LINKED_1_PATH);
        FileBlob fb = new FileBlob(f);
        PDFLinks pdfl = new PDFLinks(fb);
        List<LinkInfo> launchLinks = pdfl.getLaunchLinks();
        assertEquals(2, launchLinks.size());
        assertEquals(PDF_LINKED_2_LOCAL_PATH, launchLinks.get(0).getLink());
        assertEquals(PDF_LINKED_3_LOCAL_PATH, launchLinks.get(1).getLink());
        List<LinkInfo> remoteLinks = pdfl.getRemoteGoToLinks();
        assertEquals(1, remoteLinks.size());
        assertEquals(PDF_LINKED_2_LOCAL_PATH, remoteLinks.get(0).getLink());
        pdfl.close();
    }

    @Test
    public void testGetLinksOperation() throws Exception {
        OperationChain chain = new OperationChain("testChain");
        OperationContext ctx = new OperationContext(coreSession);
        File f = FileUtils.getResourceFileFromContext(TestUtils.PDF_LINKED_1_PATH);
        FileBlob fb = new FileBlob(f);
        ctx.setInput(fb);
        chain.add(PDFExtractLinksOperation.ID).set("getAll", true);
        String result = (String) automationService.run(ctx, chain);
        assertNotNull(result);
        assertNotEquals("", result);
        JSONArray array = new JSONArray(result);
        assertEquals(3, array.length());
        assertEquals(PDF_LINKED_2_LOCAL_PATH, ((JSONObject) array.get(0)).getString("link"));
        assertEquals(PDF_LINKED_3_LOCAL_PATH, ((JSONObject) array.get(1)).getString("link"));
        assertEquals(PDF_LINKED_2_LOCAL_PATH, ((JSONObject) array.get(2)).getString("link"));
    }

}
