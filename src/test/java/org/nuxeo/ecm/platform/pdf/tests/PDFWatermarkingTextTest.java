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
import org.nuxeo.ecm.platform.pdf.operations.PDFWatermarkTextOperation;
import org.nuxeo.ecm.platform.pdf.service.PDFTransformationService;
import org.nuxeo.ecm.platform.pdf.service.watermark.WatermarkProperties;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;

import javax.inject.Inject;
import java.io.IOException;

@RunWith(FeaturesRunner.class)
@Features({ AutomationFeature.class })
@Deploy("org.nuxeo.ecm.platform.pdf")
public class PDFWatermarkingTextTest {

    private static final String TEXT_WATERMARK = "(c) Test Text Watermark";
    private static final String PDF_PATH = "/files/test-watermark.pdf";

    @Inject
    CoreSession coreSession;

    @Inject
    PDFTransformationService pdfTransformationService;

    @Inject
    AutomationService automationService;

    @Test
    public void testServiceDefault() throws IOException {
        Blob blob = new FileBlob(getClass().getResourceAsStream(PDF_PATH));
        WatermarkProperties properties = pdfTransformationService.getDefaultProperties();
        Blob result = pdfTransformationService.applyTextWatermark(
                blob,TEXT_WATERMARK,properties);
        TestUtils.hasTextOnAllPages(result,TEXT_WATERMARK);
    }

    @Test
    public void testWatermarkCenter() throws IOException {
        Blob blob = new FileBlob(getClass().getResourceAsStream(PDF_PATH));
        WatermarkProperties properties = pdfTransformationService.getDefaultProperties();
        properties.setRelativeCoordinates(true);
        properties.setxPosition(0.5f);
        properties.setyPosition(0.5f);
        Blob result = pdfTransformationService.applyTextWatermark(
                blob,TEXT_WATERMARK,properties);
        TestUtils.hasTextOnAllPages(result,TEXT_WATERMARK);
    }

    @Test
    public void testWatermarkRotateUp() throws IOException {
        Blob blob = new FileBlob(getClass().getResourceAsStream(PDF_PATH));
        WatermarkProperties properties = pdfTransformationService.getDefaultProperties();
        properties.setRotation(45);
        Blob result = pdfTransformationService.applyTextWatermark(
                blob,TEXT_WATERMARK,properties);
        TestUtils.hasTextOnAllPages(result,TEXT_WATERMARK);
    }

    @Test
    public void testOpWithDefault() throws IOException, OperationException {
        Blob input = new FileBlob(getClass().getResourceAsStream(PDF_PATH));
        OperationChain chain;
        OperationContext ctx = new OperationContext(coreSession);
        ctx.setInput(input);
        chain = new OperationChain("testWithDefault");
        chain.add(PDFWatermarkTextOperation.ID).set("text",TEXT_WATERMARK);
        Blob result = (Blob) automationService.run(ctx, chain);
        Assert.assertNotNull(result);
        TestUtils.hasTextOnAllPages(result,TEXT_WATERMARK);
    }

    @Test
    public void testOpWithProperties() throws IOException, OperationException {
        Blob input = new FileBlob(getClass().getResourceAsStream(PDF_PATH));
        OperationChain chain;
        OperationContext ctx = new OperationContext(coreSession);
        ctx.setInput(input);
        chain = new OperationChain("testWithDefault");
        chain.add(PDFWatermarkTextOperation.ID).
                set("text",TEXT_WATERMARK).
                set("properties","alphaColor=1.0");
        Blob result = (Blob) automationService.run(ctx, chain);
        Assert.assertNotNull(result);
        TestUtils.hasTextOnAllPages(result,TEXT_WATERMARK);
    }

}
