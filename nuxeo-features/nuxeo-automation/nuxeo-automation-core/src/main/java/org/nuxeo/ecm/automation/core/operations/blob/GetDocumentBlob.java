/*
 * (C) Copyright 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     bstefanescu
 */
package org.nuxeo.ecm.automation.core.operations.blob;

import org.nuxeo.ecm.automation.core.Constants;
import org.nuxeo.ecm.automation.core.annotations.Operation;
import org.nuxeo.ecm.automation.core.annotations.OperationMethod;
import org.nuxeo.ecm.automation.core.annotations.Param;
import org.nuxeo.ecm.automation.core.collectors.BlobCollector;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.Blobs;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.blobholder.BlobHolder;

/**
 * Get document blob inside the file:content property
 *
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 * @author tiry
 */
@Operation(id = GetDocumentBlob.ID, category = Constants.CAT_BLOB, label = "Get Document File", description = "Gets a file attached to the input document. The file location is specified using an xpath to the blob property of the document. Returns the file.", aliases = { "Blob.Get" })
public class GetDocumentBlob {

    public static final String ID = "Document.GetBlob";

    @Param(name = "xpath", required = false, values = "file:content")
    protected String xpath = "file:content";

    @OperationMethod(collector = BlobCollector.class)
    public Blob run(DocumentModel doc) {
        Blob blob = (Blob) doc.getPropertyValue(xpath);
        if (blob == null) {
            BlobHolder bh = doc.getAdapter(BlobHolder.class);
            if (bh != null) {
                blob = bh.getBlob();
            }
        }
        // cannot return null since it may break the next operation
        if (blob == null) { // create an empty blob
            blob = Blobs.createBlob("");
            blob.setFilename(doc.getName() + ".null");
        }
        return blob;
    }

}
