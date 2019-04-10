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
import org.nuxeo.ecm.platform.pdf.operations.PDFWatermarkPDFOperation;
import org.nuxeo.ecm.platform.pdf.service.PDFTransformationService;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;

import javax.inject.Inject;
import java.io.IOException;

@RunWith(FeaturesRunner.class)
@Features({ AutomationFeature.class })
@Deploy("org.nuxeo.ecm.platform.pdf")
public class PDFWatermarkingPDFTest {

    private static final String PDF_PATH = "/files/test-watermark.pdf";
    static final String PDF_WATERMARK_PATH = "/files/logo.pdf";

    @Inject
    CoreSession coreSession;

    @Inject
    PDFTransformationService pdfTransformationService;

    @Inject
    AutomationService automationService;

    @Test
    public void testServiceDefault() throws IOException {
        Blob input = new FileBlob(getClass().getResourceAsStream(PDF_PATH));
        Blob overlayBlob = new FileBlob(getClass().getResourceAsStream(PDF_WATERMARK_PATH));
        Blob result = pdfTransformationService.overlayPDF(input,overlayBlob);
        Assert.assertNotNull(result);
        Assert.assertNotEquals(
                TestUtils.calculateMd5(input.getFile()),
                TestUtils.calculateMd5(result.getFile()));
    }

    @Test
    public void testOp() throws IOException, OperationException {
        Blob input = new FileBlob(getClass().getResourceAsStream(PDF_PATH));
        Blob overlayBlob = new FileBlob(getClass().getResourceAsStream(PDF_WATERMARK_PATH));
        OperationChain chain;
        OperationContext ctx = new OperationContext(coreSession);
        ctx.setInput(input);
        chain = new OperationChain("testWithDefault");
        chain.add(PDFWatermarkPDFOperation.ID).set("overlayPdf",overlayBlob);
        Blob result = (Blob) automationService.run(ctx, chain);
        Assert.assertNotNull(result);
    }

}
