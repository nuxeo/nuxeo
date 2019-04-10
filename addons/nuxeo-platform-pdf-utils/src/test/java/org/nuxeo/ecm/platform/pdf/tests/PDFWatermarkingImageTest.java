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
 *     Michael Vachette
 */
package org.nuxeo.ecm.platform.pdf.tests;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.automation.AutomationService;
import org.nuxeo.ecm.automation.OperationChain;
import org.nuxeo.ecm.automation.OperationContext;
import org.nuxeo.ecm.automation.OperationException;
import org.nuxeo.ecm.automation.test.AutomationFeature;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.impl.blob.FileBlob;
import org.nuxeo.ecm.platform.pdf.operations.PDFWatermarkImageOperation;
import org.nuxeo.ecm.platform.pdf.service.PDFTransformationService;
import org.nuxeo.ecm.platform.pdf.service.watermark.WatermarkProperties;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;

import javax.imageio.ImageIO;
import javax.inject.Inject;
import java.io.IOException;

import static org.junit.Assert.assertTrue;

@RunWith(FeaturesRunner.class)
@Features({ AutomationFeature.class })
@Deploy("org.nuxeo.ecm.platform.pdf")
public class PDFWatermarkingImageTest {

    private static final String PDF_PATH = "/files/test-watermark.pdf";
    private static final String JPEG_IMAGE_PATH = "/files/nuxeo-logo-gray.jpg";
    private static final String PNG_IMAGE_PATH = "/files/logo.png";

    @Inject
    CoreSession coreSession;

    @Inject
    PDFTransformationService pdfTransformationService;

    @Inject
    AutomationService automationService;

    @BeforeClass
    public static void onceExecutedBeforeAll() {
        // javax.imageio.IIOException: Can't create cache file!
        ImageIO.setUseCache(false);
    }

    @Test
    public void testServiceDefaultWithJPEG() throws IOException {
        Blob blob = new FileBlob(getClass().getResourceAsStream(PDF_PATH));
        Blob image = new FileBlob(getClass().getResourceAsStream(JPEG_IMAGE_PATH));
        WatermarkProperties properties = pdfTransformationService.getDefaultProperties();
        Blob result = pdfTransformationService.applyImageWatermark(blob,image,properties);
        assertTrue(TestUtils.hasImageOnAllPages(result));
    }

    @Test
    public void testServiceDefaultWithPNG() throws IOException {
        Blob blob = new FileBlob(getClass().getResourceAsStream(PDF_PATH));
        Blob image = new FileBlob(getClass().getResourceAsStream(PNG_IMAGE_PATH));
        WatermarkProperties properties = pdfTransformationService.getDefaultProperties();
        Blob result = pdfTransformationService.applyImageWatermark(blob,image,properties);
        assertTrue(TestUtils.hasImageOnAllPages(result));
    }

    @Test
    public void testServiceCenter() throws IOException {
        Blob blob = new FileBlob(getClass().getResourceAsStream(PDF_PATH));
        Blob image = new FileBlob(getClass().getResourceAsStream(JPEG_IMAGE_PATH));
        WatermarkProperties properties = pdfTransformationService.getDefaultProperties();
        properties.setRelativeCoordinates(true);
        properties.setxPosition(0.5);
        properties.setyPosition(0.5);
        properties.setScale(2.0);
        Blob result = pdfTransformationService.applyImageWatermark(blob,image,properties);
        assertTrue(TestUtils.hasImageOnAllPages(result));
    }

    @Test
    public void testWithDefault() throws IOException, OperationException {
        Blob blob = new FileBlob(getClass().getResourceAsStream(PDF_PATH));
        Blob image = new FileBlob(getClass().getResourceAsStream(JPEG_IMAGE_PATH));
        OperationChain chain;
        OperationContext ctx = new OperationContext(coreSession);
        ctx.setInput(blob);
        chain = new OperationChain("testWithDefault");
        chain.add(PDFWatermarkImageOperation.ID).set("image",image);
        Blob result = (Blob) automationService.run(ctx, chain);
        Assert.assertNotNull(result);
        assertTrue(TestUtils.hasImageOnAllPages(result));
    }

    @Test
    public void testWithProperties() throws IOException, OperationException {
        Blob blob = new FileBlob(getClass().getResourceAsStream(PDF_PATH));
        Blob image = new FileBlob(getClass().getResourceAsStream(JPEG_IMAGE_PATH));
        OperationChain chain;
        OperationContext ctx = new OperationContext(coreSession);
        ctx.setInput(blob);
        chain = new OperationChain("testWithDefault");
        chain.add(PDFWatermarkImageOperation.ID).
                set("image",image).
                set("properties","scale=2.0");
        Blob result = (Blob) automationService.run(ctx, chain);
        Assert.assertNotNull(result);
        assertTrue(TestUtils.hasImageOnAllPages(result));
    }
}
