/*
 * (C) Copyright 2006-2012 Nuxeo SA (http://nuxeo.com/) and others.
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
@Operation(id = GetAllDocumentBlobs.ID, category = Constants.CAT_BLOB, label = "Get All Document Files", description = "Gets a list of all blobs that are attached on the input document. Returns a list of files.", aliases = { "Blob.GetAll" })
public class GetAllDocumentBlobs {

    public static final String ID = "Document.GetBlobs";

    @OperationMethod(collector = BlobListCollector.class)
    public BlobList run(DocumentModel doc) {
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
