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
import java.util.List;

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
        if (bh==null) {
            return null;
        }
        BlobHolder pdfBh = service.convertToMimeType("application/pdf", bh,
                new HashMap<String, Serializable>());
        Blob result = pdfBh.getBlob();

        String fname = result.getFilename();
        if(fname==null || fname.isEmpty()) {
            fname = bh.getBlob().getFilename();
            fname = fname + ".pdf";
            result.setFilename(fname);
        }
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
        BlobHolder bh = new SimpleBlobHolder(blobs);
        bh = service.convertToMimeType("application/pdf", bh,
                new HashMap<String, Serializable>());
        // TODO optimize this
        List<Blob> result = bh.getBlobs();
        for (int i = 0, size = result.size(); i < size; i++) {
            adjustBlobName(blobs.get(i), result.get(i));
        }
        return new BlobList(result);
    }

    protected void adjustBlobName(Blob in, Blob out) {
        String fname = in.getFilename();
        if (fname == null) {
            fname = "Unknown_" + System.identityHashCode(in);
        }
        out.setFilename(fname + ".pdf");
    }

}
