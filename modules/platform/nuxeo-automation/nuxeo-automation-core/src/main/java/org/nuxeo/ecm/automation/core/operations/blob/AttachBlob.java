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
import org.nuxeo.ecm.automation.core.annotations.Context;
import org.nuxeo.ecm.automation.core.annotations.Operation;
import org.nuxeo.ecm.automation.core.annotations.OperationMethod;
import org.nuxeo.ecm.automation.core.annotations.Param;
import org.nuxeo.ecm.automation.core.collectors.BlobCollector;
import org.nuxeo.ecm.automation.core.collectors.BlobListCollector;
import org.nuxeo.ecm.automation.core.util.BlobList;
import org.nuxeo.ecm.automation.core.util.DocumentHelper;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
// TODO accept xpath that points to a blob entry in a list and insert a blob
// before. see SetDocumentBlob too.
@Operation(id = AttachBlob.ID, category = Constants.CAT_BLOB, label = "Attach File", description = "Attach the input file to the document given as a parameter. If the xpath points to a blob list then the blob is appended to the list, otherwise the xpath should point to a blob property. If the save parameter is set the document modification will be automatically saved. Return the blob.", aliases = { "Blob.Attach" })
public class AttachBlob {

    public static final String ID = "Blob.AttachOnDocument";

    @Context
    protected CoreSession session;

    @Param(name = "xpath", required = false, values = "file:content")
    protected String xpath = "file:content";

    @Param(name = "document")
    protected DocumentModel doc;

    @Param(name = "save", required = false, values = "true")
    protected boolean save = true;

    @OperationMethod(collector = BlobCollector.class)
    public Blob run(Blob blob) {
        DocumentHelper.addBlob(doc.getProperty(xpath), blob);
        if (save) {
            doc = session.saveDocument(doc);
        }
        return blob;
    }

    @OperationMethod(collector = BlobListCollector.class)
    public BlobList run(BlobList blobs) {
        blobs.forEach(blob -> DocumentHelper.addBlob(doc.getProperty(xpath), blob));
        if (save) {
            doc = session.saveDocument(doc);
        }
        return blobs;
    }

}
