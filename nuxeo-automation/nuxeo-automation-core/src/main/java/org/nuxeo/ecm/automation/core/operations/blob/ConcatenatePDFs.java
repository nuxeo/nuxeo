/*
 * (C) Copyright 2013 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Vladimir Pasquier <vpasquier@nuxeo.com>
 */
package org.nuxeo.ecm.automation.core.operations.blob;

import java.io.File;
import java.io.IOException;

import org.apache.pdfbox.exceptions.COSVisitorException;
import org.apache.pdfbox.util.PDFMergerUtility;
import org.nuxeo.ecm.automation.OperationContext;
import org.nuxeo.ecm.automation.OperationException;
import org.nuxeo.ecm.automation.core.Constants;
import org.nuxeo.ecm.automation.core.annotations.Context;
import org.nuxeo.ecm.automation.core.annotations.Operation;
import org.nuxeo.ecm.automation.core.annotations.OperationMethod;
import org.nuxeo.ecm.automation.core.annotations.Param;
import org.nuxeo.ecm.automation.core.util.BlobList;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.impl.blob.FileBlob;

/**
 * Given a File document holding a pdf on the file:content property and 2 pdfs
 * on the files:files property, the following chain will make the user download
 * a pdf that is the result of the merge of all the pdfs, with the content of
 * the one in file:content property first.
 * 
 * @since 5.8
 */
@Operation(id = ConcatenatePDFs.ID, category = Constants.CAT_CONVERSION, label = "Concatenate PDFs", description = "Given a File document holding a pdf on the file:content property and 2 pdfs on the files:files property, the following chain will make the user download a pdf that is the result of the merge of all the pdfs, with the content of the one in file:content property first.")
public class ConcatenatePDFs {

    public static final String ID = "Blob.ConcatenatePDFs";

    @Context
    protected OperationContext ctx;

    @Param(name = "blob_to_append", required = false, description = "Optional blob reference in context to append in first place.")
    protected String xpathBlobToAppend = "";

    @Param(name = "filename", required = true, description = "The merge pdf result filename.")
    protected String filename;

    @OperationMethod
    public Blob run(Blob blob) throws OperationException, IOException,
            COSVisitorException {
        PDFMergerUtility ut = new PDFMergerUtility();
        checkPdf(blob);
        ut.addSource(blob.getStream());
        if (xpathBlobToAppend.isEmpty()) {
            return blob;
        }
        handleBlobToAppend(ut);
        FileBlob fb = appendPDFs(ut);
        return fb;
    }

    protected FileBlob appendPDFs(PDFMergerUtility ut) throws IOException,
            COSVisitorException {
        File tempFile = File.createTempFile(filename, ".pdf");
        ut.setDestinationFileName(tempFile.getAbsolutePath());
        ut.mergeDocuments();
        FileBlob fb = new FileBlob(tempFile);
        fb.setFilename(filename);
        return fb;
    }

    protected void handleBlobToAppend(PDFMergerUtility ut) throws IOException,
            OperationException {
        try {
            Blob blobToAppend = (Blob) ctx.get(xpathBlobToAppend);
            ut.addSource(blobToAppend.getStream());
        } catch (ClassCastException e) {
            throw new OperationException(
                    "The blob to append from variable context: '"
                            + xpathBlobToAppend + "' is not a blob.", e);
        } catch (NullPointerException e) {
            throw new OperationException(
                    "The blob to append from variable context: '"
                            + xpathBlobToAppend + "' is null.", e);
        }
    }

    @OperationMethod
    public Blob run(BlobList blobs) throws IOException, OperationException,
            COSVisitorException {
        PDFMergerUtility ut = new PDFMergerUtility();
        for (Blob blob : blobs) {
            checkPdf(blob);
            ut.addSource(blob.getStream());
        }
        if (!xpathBlobToAppend.isEmpty()) {
            handleBlobToAppend(ut);
        }
        FileBlob fb = appendPDFs(ut);
        return fb;
    }

    protected void checkPdf(Blob blob) throws OperationException {
        if (!"application/pdf".equals(blob.getMimeType())) {
            throw new OperationException("Blob " + blob.getFilename()
                    + " is not a PDF.");
        }
    }
}
