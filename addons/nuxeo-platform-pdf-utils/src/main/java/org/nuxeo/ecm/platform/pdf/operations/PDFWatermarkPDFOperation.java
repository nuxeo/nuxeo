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
package org.nuxeo.ecm.platform.pdf.operations;

import org.nuxeo.ecm.automation.core.Constants;
import org.nuxeo.ecm.automation.core.annotations.Context;
import org.nuxeo.ecm.automation.core.annotations.Operation;
import org.nuxeo.ecm.automation.core.annotations.OperationMethod;
import org.nuxeo.ecm.automation.core.annotations.Param;
import org.nuxeo.ecm.automation.core.collectors.BlobCollector;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.platform.pdf.service.PDFTransformationService;

/**
 * Returns a new blob combining the input PDF and an overlaid PDF on every page.
 * @since 8.10
 */
@Operation(
        id = PDFWatermarkPDFOperation.ID,
        category = Constants.CAT_CONVERSION,
        label = "PDF: Watermark with PDF",
        description = PDFWatermarkPDFOperation.DESCRIPTION)
public class PDFWatermarkPDFOperation {

    public static final String ID = "PDF.WatermarkWithPDF";

    public static final String DESCRIPTION =
            "Returns a new blob combining the input PDF and an overlaid PDF on every page.";

    @Context
    protected PDFTransformationService pdfTransformationService;

    @Param(name = "overlayPdf", description="The PDF Blob to overlay on top of the input")
    protected Blob overlayPdf;

    @OperationMethod(collector = BlobCollector.class)
    public Blob run(Blob inBlob) throws NuxeoException {
        return pdfTransformationService.overlayPDF(inBlob,overlayPdf);
    }

}
