/*
 * Copyright (c) 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     bstefanescu
 */
package org.nuxeo.ecm.automation.core.operations.blob;

import java.io.Serializable;
import java.util.HashMap;
import org.nuxeo.ecm.automation.core.Constants;
import org.nuxeo.ecm.automation.core.annotations.Context;
import org.nuxeo.ecm.automation.core.annotations.Operation;
import org.nuxeo.ecm.automation.core.annotations.OperationMethod;
import org.nuxeo.ecm.automation.core.util.BlobList;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.blobholder.BlobHolder;
import org.nuxeo.ecm.core.api.blobholder.SimpleBlobHolder;
import org.nuxeo.ecm.core.convert.api.ConversionService;

/**
 * Save the input document
 * 
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 * @author tiry
 */
@Operation(id = BlobToPDF.ID, category = Constants.CAT_CONVERSION, label = "Convert To PDF", description = "Convert the input file to a PDF and return the new file.")
public class BlobToPDF {

    public static final String ID = "Blob.ToPDF";

    @Context
    protected ConversionService service;

    @OperationMethod
    public Blob run(DocumentModel doc) throws Exception {
        BlobHolder bh = doc.getAdapter(BlobHolder.class);
        if (bh == null) {
            return null;
        }
        BlobHolder pdfBh = service.convertToMimeType("application/pdf", bh,
                new HashMap<String, Serializable>());
        Blob result = pdfBh.getBlob();

        String fname = result.getFilename();
        String filename = bh.getBlob().getFilename();
        if (filename != null && !filename.isEmpty()) {
            // add pdf extension
            int pos = filename.lastIndexOf('.');
            if (pos > 0) {
                filename = filename.substring(0, pos);
            }
            filename += ".pdf";
            result.setFilename(filename);
        } else if (fname != null && !fname.isEmpty()) {
            result.setFilename(fname);
        } else {
            result.setFilename("file");
        }

        result.setMimeType("application/pdf");
        return result;
    }

    @OperationMethod
    public Blob run(Blob blob) throws Exception {
        BlobHolder bh = new SimpleBlobHolder(blob);
        bh = service.convertToMimeType("application/pdf", bh,
                new HashMap<String, Serializable>());
        Blob result = bh.getBlob();
        adjustBlobName(blob, result);
        return result;
    }

    @OperationMethod
    public BlobList run(BlobList blobs) throws Exception {
        BlobList bl = new BlobList();
        for (Blob blob : blobs) {
            bl.add(this.run(blob));
        }
        return bl;
    }

    protected void adjustBlobName(Blob in, Blob out) {
        String fname = in.getFilename();
        if (fname == null) {
            fname = "Unknown_" + System.identityHashCode(in);
        }
        out.setFilename(fname + ".pdf");
        out.setMimeType("application/pdf");
    }

}
