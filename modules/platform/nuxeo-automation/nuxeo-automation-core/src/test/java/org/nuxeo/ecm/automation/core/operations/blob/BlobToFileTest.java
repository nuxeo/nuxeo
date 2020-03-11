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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.common.Environment;
import org.nuxeo.ecm.automation.AutomationService;
import org.nuxeo.ecm.automation.OperationContext;
import org.nuxeo.ecm.automation.OperationException;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.Blobs;
import org.nuxeo.ecm.core.api.CloseableCoreSession;
import org.nuxeo.ecm.core.api.CoreInstance;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;

@RunWith(FeaturesRunner.class)
@Features(CoreFeature.class)
@Deploy("org.nuxeo.ecm.automation.core")
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
        tempDirectory = Framework.createTempDirectory("tmp").toFile();
        Environment.getDefault().setServerHome(Environment.getDefault().getTemp());
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
        try (OperationContext ctx = buildCtx(session)) {
            targetDirectory = createAllowedTargetDirectory();
            Map<String, Object> params = buildParams(targetDirectory);
            Blob outputBlob = (Blob) automationService.run(ctx, BlobToFile.ID, params);
            assertNotNull(outputBlob);
            assertEquals(pdfFile, outputBlob.getFile());
            File[] directoryContent = FileUtils
                    .convertFileCollectionToFileArray(FileUtils.listFiles(targetDirectory, null, false));
            assertEquals(1, directoryContent.length);
            assertEquals(new File(targetDirectory.getAbsolutePath(), PDF_NAME), directoryContent[0]);
        }
    }

    @Test
    public void testNotAllowedWhenNotAdmin() throws Exception {
        try (CloseableCoreSession notAdminSession = CoreInstance.openCoreSession(session.getRepositoryName(), DEFAULT_USER_ID)) {
            try (OperationContext ctx = buildCtx(notAdminSession)) {
                targetDirectory = createAllowedTargetDirectory();
                Map<String, Object> params = buildParams(targetDirectory);
                String errorMessage = "Not allowed. You must be administrator";
                testNotAllowed(ctx, BlobToFile.ID, params, errorMessage);
                for (String alias : automationService.getOperation(BlobToFile.ID).getAliases()) {
                    testNotAllowed(ctx, alias, params, errorMessage);
                }
            }
        }
    }

    @Test
    public void testNotAllowedWhenForbiddenTargetDirectory() throws Exception {
        try (OperationContext ctx = buildCtx(session)) {
            targetDirectory = createForbiddenTargetDirectory();
            Map<String, Object> params = buildParams(targetDirectory);
            String errorMessage = "Not allowed. The target directory is forbidden";
            testNotAllowed(ctx, BlobToFile.ID, params, errorMessage);
            for (String alias : automationService.getOperation(BlobToFile.ID).getAliases()) {
                testNotAllowed(ctx, alias, params, errorMessage);
            }
        }
    }

    protected void testNotAllowed(OperationContext ctx, String id, Map<String, Object> params, String message)
            throws IOException {
        try {
            automationService.run(ctx, id, params);
            fail("Expected OperationException");
        } catch (OperationException e) {
            assertNotNull(e.getMessage());
            assertTrue(e.getMessage().contains(message));
        }
    }

    protected OperationContext buildCtx(CoreSession coreSession) throws IOException {
        OperationContext ctx = new OperationContext(coreSession);
        ctx.setInput(blob);
        return ctx;
    }

    protected Map<String, Object> buildParams(File directory) {
        Map<String, Object> params = new HashMap<>();
        params.put("directory", directory.getAbsolutePath());
        params.put("prefix", "");
        return params;
    }

    protected File createAllowedTargetDirectory() throws IOException {
        File directory = File.createTempFile("target", "",
                FileUtils.getTempDirectory().getParentFile().getParentFile());
        directory.delete();
        directory.mkdirs();
        return directory;
    }

    protected File createForbiddenTargetDirectory() throws IOException {
        return Framework.createTempDirectory("target").toFile();
    }
}
