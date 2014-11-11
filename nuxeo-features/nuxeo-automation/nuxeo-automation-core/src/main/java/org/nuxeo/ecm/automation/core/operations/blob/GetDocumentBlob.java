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

import org.nuxeo.ecm.automation.core.Constants;
import org.nuxeo.ecm.automation.core.annotations.Operation;
import org.nuxeo.ecm.automation.core.annotations.OperationMethod;
import org.nuxeo.ecm.automation.core.annotations.Param;
import org.nuxeo.ecm.automation.core.collectors.BlobCollector;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.blobholder.BlobHolder;
import org.nuxeo.ecm.core.api.impl.blob.StringBlob;

/**
 * Get document blob inside the file:content property
 *
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 * @author tiry
 */
@Operation(id = GetDocumentBlob.ID, category = Constants.CAT_BLOB, label = "Get Document File", description = "Gets a file attached to the input document. The file location is specified using an xpath to the blob property of the document. Returns the file.")
public class GetDocumentBlob {

    public static final String ID = "Blob.Get";

    @Param(name = "xpath", required = false, values = "file:content")
    protected String xpath = "file:content";

    @OperationMethod(collector=BlobCollector.class)
    public Blob run(DocumentModel doc) throws Exception {
        Blob blob = (Blob) doc.getPropertyValue(xpath);
        if (blob==null) {
            BlobHolder bh = doc.getAdapter(BlobHolder.class);
            if (bh!=null) {
                blob = bh.getBlob();
            }
        }
        // cannot return null since it may break the next operation
        if (blob == null) { // create an empty blob
            blob = new StringBlob("");
            blob.setMimeType("text/plain");
            blob.setFilename(doc.getName() + ".null");
        }
        return blob;
    }


}
