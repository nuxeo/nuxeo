/*
 * (C) Copyright 2014 Nuxeo SA (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl-2.1.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
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
 * @since 8.4
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
