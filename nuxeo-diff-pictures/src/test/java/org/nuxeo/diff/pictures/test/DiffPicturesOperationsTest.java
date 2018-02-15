/*
 * (C) Copyright 2015-2016 Nuxeo SA (http://nuxeo.com/) and others.
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
 */

package org.nuxeo.diff.pictures.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.nuxeo.common.Environment;
import org.nuxeo.common.utils.FileUtils;
import org.nuxeo.diff.pictures.DiffPicturesWithBlobsOp;
import org.nuxeo.diff.pictures.DiffPicturesWithDocsOp;
import org.nuxeo.ecm.automation.AutomationService;
import org.nuxeo.ecm.automation.OperationChain;
import org.nuxeo.ecm.automation.OperationContext;
import org.nuxeo.ecm.automation.test.AutomationFeature;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.impl.blob.FileBlob;
import org.nuxeo.ecm.platform.test.PlatformFeature;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;

import com.google.inject.Inject;

/**
 * @since 7.4
 */

@RunWith(FeaturesRunner.class)
@Features({ PlatformFeature.class, AutomationFeature.class })
@Deploy({ "org.nuxeo.ecm.platform.rendition.core", "org.nuxeo.ecm.platform.picture.api",
        "org.nuxeo.ecm.platform.picture.core", "org.nuxeo.ecm.platform.picture.convert",
        "org.nuxeo.ecm.platform.commandline.executor", "org.nuxeo.diff.pictures", "org.nuxeo.ecm.platform.tag" })
public class DiffPicturesOperationsTest {

    protected static final Log log = LogFactory.getLog(DiffPicturesOperationsTest.class);

    protected static final String ISLAND_PNG = "island.png";

    protected static final int ISLAND_W = 400;

    protected static final int ISLAND_H = 282;

    protected static final String ISLAND_MODIF_PNG = "island-modif.png";

    protected static final String PNG_MIME_TYPE = "image/png";

    protected File fileImage;

    protected File fileImageModif;

    protected DocumentModel parentOfTestDocs;

    protected DocumentModel docImage;

    protected DocumentModel docImageModif;

    @Inject
    CoreSession coreSession;

    @Inject
    AutomationService automationService;

    @Before
    public void setUp() {
        fileImage = FileUtils.getResourceFileFromContext(ISLAND_PNG);
        fileImageModif = FileUtils.getResourceFileFromContext(ISLAND_MODIF_PNG);

        parentOfTestDocs = coreSession.createDocumentModel("/", "test-pictures", "Folder");
        parentOfTestDocs.setPropertyValue("dc:title", "test-pictures");
        parentOfTestDocs = coreSession.createDocument(parentOfTestDocs);

        docImage = createPictureDocument(fileImage);
        docImageModif = createPictureDocument(fileImageModif);

        coreSession.save();
    }

    protected DocumentModel createPictureDocument(File inFile) {
        DocumentModel pictDoc = coreSession.createDocumentModel(parentOfTestDocs.getPathAsString(), inFile.getName(),
                "Picture");
        pictDoc.setPropertyValue("dc:title", inFile.getName());
        pictDoc.setPropertyValue("file:content", new FileBlob(inFile, PNG_MIME_TYPE));
        return coreSession.createDocument(pictDoc);
    }

    protected BufferedImage checkIsImage(Blob inBlob) throws Exception {
        assertTrue(inBlob instanceof FileBlob);
        return checkIsImage((FileBlob) inBlob);
    }

    protected BufferedImage checkIsImage(FileBlob inBlob) throws Exception {
        assertNotNull(inBlob);

        File f = inBlob.getFile();
        assertNotNull(f);
        assertTrue(f.length() > 0);

        BufferedImage bi;
        try {
            ImageIO.setCacheDirectory(Environment.getDefault().getTemp());
            bi = ImageIO.read(f);
            assertNotNull(bi);

        } catch (IOException e) {
            throw new Exception("Error reading the file", e);
        }

        return bi;
    }

    protected void deleteFile(Blob inBlob) {
        if (inBlob instanceof FileBlob) {
            File f = inBlob.getFile();
            if (f != null) {
                f.delete();
            }
        }
    }

    @Test
    public void testOperationWithBlobs_defaultParameters() throws Exception {
        FileBlob fb1 = new FileBlob(fileImage, PNG_MIME_TYPE);
        FileBlob fb2 = new FileBlob(fileImageModif, PNG_MIME_TYPE);

        OperationContext ctx = new OperationContext(coreSession);
        assertNotNull(ctx);

        ctx.setInput(fb1);
        ctx.put("varBlob", fb2);

        OperationChain chain = new OperationChain("testChain");
        chain.add(DiffPicturesWithBlobsOp.ID).set("blob2VarName", "varBlob");

        Blob result = (Blob) automationService.run(ctx, chain);
        assertNotNull(result);

        BufferedImage bi = checkIsImage(result);
        assertEquals(bi.getWidth(), ISLAND_W);
        assertEquals(bi.getHeight(), ISLAND_H);
    }

    @Test
    public void testOperationWithDocss_defaultParameters() throws Exception {
        OperationContext ctx = new OperationContext(coreSession);
        assertNotNull(ctx);

        ctx.setInput(null);

        OperationChain chain = new OperationChain("testChain");
        chain.add(DiffPicturesWithDocsOp.ID)
             .set("blob2VarName", "varBlob")
             .set("leftDoc", docImage.getId())
             .set("rightDoc", docImageModif.getId());

        Blob result = (Blob) automationService.run(ctx, chain);
        assertNotNull(result);

        BufferedImage bi = checkIsImage(result);
        assertEquals(bi.getWidth(), ISLAND_W);
        assertEquals(bi.getHeight(), ISLAND_H);
    }

}
