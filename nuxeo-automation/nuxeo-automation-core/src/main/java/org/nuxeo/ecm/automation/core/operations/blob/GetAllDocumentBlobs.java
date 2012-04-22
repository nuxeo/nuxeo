/*
 * Copyright (c) 2006-2012 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 */
package org.nuxeo.ecm.automation.core.operations.blob;

import java.util.List;

import org.nuxeo.ecm.automation.core.Constants;
import org.nuxeo.ecm.automation.core.annotations.Operation;
import org.nuxeo.ecm.automation.core.annotations.OperationMethod;
import org.nuxeo.ecm.automation.core.annotations.Param;
import org.nuxeo.ecm.automation.core.collectors.BlobListCollector;
import org.nuxeo.ecm.automation.core.util.BlobList;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.blobholder.BlobHolder;
import org.nuxeo.ecm.core.api.impl.blob.StringBlob;
import org.nuxeo.ecm.core.api.model.Property;
import org.nuxeo.ecm.core.api.model.impl.ListProperty;

/**
 * Get document blobs inside the file:content and files:files properties
 */
@Operation(id = GetAllDocumentBlobs.ID, category = Constants.CAT_BLOB, label = "Get All Document Files", description = "Gets a list of main file and files that are attached on the input document. The files location should be specified using the blob list properties xpath. Returns a list of files.")
public class GetAllDocumentBlobs {

    public static final String ID = "Blob.GetAll";

    @Param(name = "xpathMainFile", required = false, values = "file:content")
    protected String xpathMainFile = "file:content";

    @Param(name = "xpathFiles", required = false, values = "files:files")
    protected String xpathFiles = "files:files";

    @OperationMethod(collector = BlobListCollector.class)
    public BlobList run(DocumentModel doc) throws Exception {
        BlobList blobs = new BlobList();
        Blob mainFile = null;
        if (doc.hasSchema("file")) {
            mainFile = (Blob) doc.getPropertyValue(xpathMainFile);
        }
        ListProperty listFiles = null;
        if (doc.hasSchema("files")) {
            listFiles = (ListProperty) doc.getProperty(xpathFiles);
        }
        if (mainFile == null) {
            BlobHolder bh = doc.getAdapter(BlobHolder.class);
            if (bh != null) {
                mainFile = bh.getBlob();
            }
            if (mainFile == null) {
                mainFile = new StringBlob("");
                mainFile.setMimeType("text/plain");
                mainFile.setFilename(doc.getName() + ".null");
            }
        }
        // Add main file to the blob list
        blobs.add(mainFile);
        if (listFiles == null) {
            BlobHolder bh = doc.getAdapter(BlobHolder.class);
            if (bh != null) {
                List<Blob> docBlobs = bh.getBlobs();
                if (docBlobs != null) {
                    for (Blob blob : docBlobs) {
                        blobs.add(blob);
                    }
                }
            }
            return blobs;
        }
        // Add attached files to the blob list
        for (Property p : listFiles) {
            blobs.add((Blob) p.getValue("file"));
        }
        return blobs;
    }
}
