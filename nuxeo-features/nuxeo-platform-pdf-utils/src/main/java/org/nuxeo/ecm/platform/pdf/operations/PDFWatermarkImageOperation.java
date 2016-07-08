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
