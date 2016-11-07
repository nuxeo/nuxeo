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
package org.nuxeo.ecm.platform.pdf.operations;

import org.nuxeo.ecm.automation.OperationContext;
import org.nuxeo.ecm.automation.core.Constants;
import org.nuxeo.ecm.automation.core.annotations.Context;
import org.nuxeo.ecm.automation.core.annotations.Operation;
import org.nuxeo.ecm.automation.core.annotations.OperationMethod;
import org.nuxeo.ecm.automation.core.annotations.Param;
import org.nuxeo.ecm.automation.core.collectors.BlobCollector;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.platform.pdf.PDFUtils;
import org.nuxeo.ecm.platform.pdf.PDFWatermarking;

/**
 * Returns a <i>new</i> blob combining the input PDF and an overlayed PDF on every page. The PDF to use for the
 * watermark can be either the <code>pdfContextVarName</code> Context variable name holding a blob of the PDF, of it can
 * be either the path or the ID of a document whose <code>file:content</code> field holds the PDF to use as overlay.
 * <p>
 * If <code>pdfDocRef</code> is used, an UnrestrictedSession fetches its blob, so the PDF can be watermarked even if
 * current user has not enough rights to read the watermark itself.
 *
 * @since 8.10
 */
@Operation(id = PDFWatermarkPDFOperation.ID, category = Constants.CAT_CONVERSION, label = "PDF: Watermark with PDF",
    description = "Returns a <i>new</i> blob combining the input PDF and an overlayed PDF on every page. The PDF to " +
        "use for the watermark can be either the <code>pdfContextVarName</code> Context variable name holding a blob " +
        "of the PDF, of it can be either the path or the ID of a document whose <code>file:content</code> field " +
        "holds the PDF to use as overlay. If <code>pdfDocRef</code> is used, an UnrestrictedSession fetches its " +
        "blob, so the PDF can be watermarked even if current user has not enough rights to read the watermark itself.")
public class PDFWatermarkPDFOperation {

    public static final String ID = "PDF.WatermarkWithPDF";

    @Context
    protected CoreSession session;

    @Context
    protected OperationContext context;

    @Param(name = "pdfContextVarName")
    protected String pdfContextVarName = "";

    @Param(name = "pdfDocRef", required = false)
    protected String pdfDocRef = "";

    @OperationMethod(collector = BlobCollector.class)
    public Blob run(Blob inBlob) throws NuxeoException {
        Blob blobPdf = null;
        if (pdfContextVarName != null && !pdfContextVarName.isEmpty()) {
            blobPdf = (Blob) context.get(pdfContextVarName);
        } else if (pdfDocRef != null && !pdfDocRef.isEmpty()) {
            PDFUtils.UnrestrictedGetBlobForDocumentIdOrPath r = new PDFUtils.UnrestrictedGetBlobForDocumentIdOrPath(
                session, pdfDocRef);
            r.runUnrestricted();
            blobPdf = r.getBlob();
        }
        PDFWatermarking pdfw = new PDFWatermarking(inBlob);
        return pdfw.watermarkWithPdf(blobPdf);
    }

}
