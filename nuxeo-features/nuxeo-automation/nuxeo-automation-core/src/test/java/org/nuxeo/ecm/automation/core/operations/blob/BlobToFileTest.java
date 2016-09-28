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
 *      Estelle Giuly <egiuly@nuxeo.com>
 */
package org.nuxeo.ecm.automation.core.operations.blob;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.automation.AutomationService;
import org.nuxeo.ecm.automation.OperationContext;
import org.nuxeo.ecm.automation.OperationException;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.Blobs;
import org.nuxeo.ecm.core.api.CoreInstance;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

@RunWith(FeaturesRunner.class)
@Features(CoreFeature.class)
@Deploy({ "org.nuxeo.ecm.automation.core" })
public class BlobToFileTest {

    protected static final String DEFAULT_USER_ID = "hsimpsons";

    protected static final String PDF_NAME = "pdfMerge1.pdf";

    @Inject
    AutomationService automationService;

    @Inject
    CoreSession session;

    protected Blob blob;

    protected File pdfFile;

    protected File targetDirectory;

    protected File tempDirectory;

    @Before
    public void setUp() throws Exception {
        targetDirectory = Framework.createTempDirectory("target").toFile();
        tempDirectory = Framework.createTempDirectory("tmp").toFile();
        pdfFile = new File(tempDirectory.getAbsolutePath(), PDF_NAME);
        blob = Blobs.createBlob(pdfFile);
        try (InputStream in = getClass().getClassLoader().getResourceAsStream(PDF_NAME);
                OutputStream out = new FileOutputStream(pdfFile)) {
            IOUtils.copy(in, out);
        }
    }

    @After
    public void tearDown() throws Exception {
        FileUtils.deleteQuietly(targetDirectory);
        FileUtils.deleteQuietly(tempDirectory);
    }

    @Test
    public void testExportBlobToFS() throws Exception {
        OperationContext ctx = buildCtx(session);
        Map<String, Object> params = buildParams();
        Blob outputBlob = (Blob) automationService.run(ctx, BlobToFile.ID, params);
        assertNotNull(outputBlob);
        assertEquals(pdfFile, outputBlob.getFile());
        File[] directoryContent = FileUtils.convertFileCollectionToFileArray(
                FileUtils.listFiles(targetDirectory, null, false));
        assertEquals(1, directoryContent.length);
        assertEquals(new File(targetDirectory.getAbsolutePath(), PDF_NAME), directoryContent[0]);
    }

    @Test
    public void testNotAllowedToExportBlobToFS() throws Exception {
        CoreSession notAdminSession = CoreInstance.openCoreSession(session.getRepositoryName(), DEFAULT_USER_ID);
        OperationContext ctx = buildCtx(notAdminSession);
        Map<String, Object> params = buildParams();
        testNotAllowed(ctx, BlobToFile.ID, params);
        for (String alias : automationService.getOperation(BlobToFile.ID).getAliases()) {
            testNotAllowed(ctx, alias, params);
        }
        notAdminSession.close();
    }

    protected void testNotAllowed(OperationContext ctx, String id, Map<String, Object> params) throws IOException {
        try {
            automationService.run(ctx, id, params);
        } catch (OperationException e) {
            assertNotNull(e.getMessage());
            assertTrue(e.getMessage().contains("Not allowed"));
        }
    }

    protected OperationContext buildCtx(CoreSession coreSession) throws IOException {
        OperationContext ctx = new OperationContext(coreSession);
        ctx.setInput(blob);
        return ctx;
    }

    protected Map<String, Object> buildParams() {
        Map<String, Object> params = new HashMap<>();
        params.put("directory", targetDirectory.getAbsolutePath());
        params.put("prefix", "");
        return params;
    }
}
