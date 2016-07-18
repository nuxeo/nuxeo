/*
 * (C) Copyright 2014 Nuxeo SA (http://nuxeo.com/) and others.
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
 * Returns a <i>new</i> blob combining the input PDF and the image (stored in a blob) referenced either by
 * <code>imageContextVarName</code> (name of a Context variable holding the blob) or by <code>imageDocRef</code> (path
 * or ID of a document whose <code>file:content</code> is the image to use). If the value of <code>scale</code> is <= 0
 * it is set to 1.0.
 * <p>
 * If <code>imageDocRef</code> is used, an UnrestrictedSession fetches its blob, so the PDF can be watermarked even if
 * current user has not enough rights to read the watermark itself.
 *
 * @since 8.4
 */
@Operation(id = PDFWatermarkImageOperation.ID, category = Constants.CAT_CONVERSION, label = "PDF: Watermark with Image",
    description = "Returns a <i>new</i> blob combining the input PDF and the image (stored in a blob) referenced " +
        "either by <code>imageContextVarName</code> (name of a Context variable holding the blob) or by " +
        "<code>imageDoc</code> (path or ID of a document whose <code>file:content</code> is the image to use). If " +
        "<code>scale</code> is <= 0 it is set to 1.0. If <code>imageDocRef</code> is used, an UnrestrictedSession " +
        "fetches its blob, so the PDF can be watermarked even if current user has not enough rights to read the " +
        "watermark itself.")
public class PDFWatermarkImageOperation {

    public static final String ID = "PDF.WatermarkWithImage";

    @Context
    protected CoreSession session;

    @Context
    protected OperationContext context;

    @Param(name = "imageContextVarName", required = false)
    protected String imageContextVarName = "";

    @Param(name = "imageDocRef", required = false)
    protected String imageDocRef = "";

    @Param(name = "x", required = false, values = { "0" })
    protected long x = 0;

    @Param(name = "y", required = false, values = { "0" })
    protected long y = 0;

    @Param(name = "scale", required = false, values = { "1.0" })
    protected String scale = "1.0";

    @OperationMethod(collector = BlobCollector.class)
    public Blob run(Blob inBlob) throws NuxeoException {
        Blob blobImage = null;
        if (imageContextVarName != null && !imageContextVarName.isEmpty()) {
            blobImage = (Blob) context.get(imageContextVarName);
        } else if (imageDocRef != null && !imageDocRef.isEmpty()) {
            PDFUtils.UnrestrictedGetBlobForDocumentIdOrPath r = new PDFUtils.UnrestrictedGetBlobForDocumentIdOrPath(
                session, imageDocRef);
            r.runUnrestricted();
            blobImage = r.getBlob();
        }
        PDFWatermarking pdfw = new PDFWatermarking(inBlob);
        return pdfw.watermarkWithImage(blobImage, (int) x, (int) y, Float.parseFloat(scale));
    }

}
