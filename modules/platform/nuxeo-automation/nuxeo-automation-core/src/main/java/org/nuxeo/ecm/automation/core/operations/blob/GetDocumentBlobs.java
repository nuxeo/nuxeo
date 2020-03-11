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
import org.nuxeo.ecm.core.api.model.Property;
import org.nuxeo.ecm.core.api.model.impl.ListProperty;

/**
 * Get document blobs inside the files:files property
 *
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 * @author tiry
 */
@Operation(id = GetDocumentBlobs.ID, category = Constants.CAT_BLOB, label = "Get Document Files", description = "Gets a list of files that are attached on the input document. The files location should be specified using the blob list property xpath. Returns a list of files.", aliases = { "Blob.GetList" })
public class GetDocumentBlobs {

    public static final String ID = "Document.GetBlobsByProperty";

    @Param(name = "xpath", required = false, values = "files:files")
    protected String xpath = "files:files";

    @OperationMethod(collector = BlobListCollector.class)
    public BlobList run(DocumentModel doc) {
        BlobList blobs = new BlobList();
        ListProperty list = (ListProperty) doc.getProperty(xpath);
        if (list == null) {
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
        for (Property p : list) {
            blobs.add((Blob) p.getValue("file"));
        }
        return blobs;
    }

}
