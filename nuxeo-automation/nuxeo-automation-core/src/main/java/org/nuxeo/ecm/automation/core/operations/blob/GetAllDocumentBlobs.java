/*
 * Copyright (c) 2006-2012 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * <a href=mailto:vpasquier@nuxeo.com>Vladimir Pasquier</a>
 */
package org.nuxeo.ecm.automation.core.operations.blob;

import java.util.List;

import org.nuxeo.ecm.automation.core.Constants;
import org.nuxeo.ecm.automation.core.annotations.Operation;
import org.nuxeo.ecm.automation.core.annotations.OperationMethod;
import org.nuxeo.ecm.automation.core.collectors.BlobListCollector;
import org.nuxeo.ecm.automation.core.util.BlobList;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.blobholder.BlobHolder;

/**
 * Get all document blobs
 */
@Operation(id = GetAllDocumentBlobs.ID, category = Constants.CAT_BLOB, label = "Get All Document Files", description = "Gets a list of all blobs that are attached on the input document. Returns a list of files.")
public class GetAllDocumentBlobs {

    public static final String ID = "Blob.GetAll";

    @OperationMethod(collector = BlobListCollector.class)
    public BlobList run(DocumentModel doc) throws Exception {
        BlobList blobs = new BlobList();
        BlobHolder bh = doc.getAdapter(BlobHolder.class);
        if (bh != null) {
            List<Blob> docBlobs = bh.getBlobs();
            if (docBlobs != null && !docBlobs.isEmpty()) {
                for (Blob blob : docBlobs) {
                    blobs.add(blob);
                }
                return blobs;
            } else {
                return null;
            }
        } else {
            return null;
        }
    }
}
