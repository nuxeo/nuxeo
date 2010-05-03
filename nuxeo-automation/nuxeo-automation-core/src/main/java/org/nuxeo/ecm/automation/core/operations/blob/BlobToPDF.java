/*
 * (C) Copyright 2006-2008 Nuxeo SAS (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
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
import org.nuxeo.ecm.core.api.blobholder.BlobHolder;
import org.nuxeo.ecm.core.api.blobholder.SimpleBlobHolder;
import org.nuxeo.ecm.core.convert.api.ConversionService;

/**
 * Save the input document
 *
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
@Operation(id=BlobToPDF.ID, category=Constants.CAT_BLOB, label="Convert To PDF",
        description="Convert the input file to a PDF and return the new file.")
public class BlobToPDF {

    public final static String ID = "Blob.ToPDF";

    @Context protected ConversionService service;

    @OperationMethod
    public Blob run(Blob blob) throws Exception {
        BlobHolder bh = new SimpleBlobHolder(blob);
        bh = service.convertToMimeType("application/pdf", bh, new HashMap<String,Serializable>());
        Blob result = bh.getBlob();
        adjustBlobName(blob, result);
        return result;
    }

    @OperationMethod
    public BlobList run(BlobList blobs) throws Exception {
        BlobHolder bh = new SimpleBlobHolder(blobs);
        bh = service.convertToMimeType("application/pdf", bh, new HashMap<String,Serializable>());
        //TODO optimize this
        List<Blob> result = bh.getBlobs();
        for (int i=0, size=result.size(); i<size; i++) {
           adjustBlobName(blobs.get(i), result.get(i));
        }
        return new BlobList(result);
    }


    protected void adjustBlobName(Blob in, Blob out) {
        String fname = in.getFilename();
        if (fname == null) {
            fname = "Unknown_"+System.identityHashCode(in);
        }
        out.setFilename(fname+".pdf");
    }

}
