/*
 * (C) Copyright 2014 Nuxeo SA (http://nuxeo.com/) and others.
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
 *      Vladimir Pasquier <vpasquier@nuxeo.com>
 */
package org.nuxeo.binary.metadata.internals.operations;

import java.util.HashMap;
import java.util.Map;

import org.nuxeo.binary.metadata.api.BinaryMetadataException;
import org.nuxeo.binary.metadata.api.BinaryMetadataService;
import org.nuxeo.ecm.automation.core.Constants;
import org.nuxeo.ecm.automation.core.annotations.Context;
import org.nuxeo.ecm.automation.core.annotations.Operation;
import org.nuxeo.ecm.automation.core.annotations.OperationMethod;
import org.nuxeo.ecm.automation.core.annotations.Param;
import org.nuxeo.ecm.automation.core.util.Properties;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.blobholder.BlobHolder;
import org.nuxeo.ecm.core.api.blobholder.DocumentBlobHolder;

/**
 * @since 7.1
 */
@Operation(id = WriteMetadataToBinaryFromDocument.ID, category = Constants.CAT_BLOB, label = "Write Metadata To Binary From Document", description = "Write metadata to a Blob (xpath parameter, or BlobHolder if empty) from a document (input) given a custom metadata mapping defined in a Properties parameter, using a named processor (exifTool for instance).", since = "7.1", addToStudio = true, aliases = { "Binary.WriteMetadataFromDocument" })
public class WriteMetadataToBinaryFromDocument {

    public static final String ID = "Blob.SetMetadataFromDocument";

    @Context
    protected CoreSession session;

    @Context
    protected BinaryMetadataService binaryMetadataService;

    @Param(name = "processor", required = false, description = "The processor to execute for overriding the input document blob.")
    protected String processor = "exifTool";

    @Param(name = "metadata", required = true, description = "Metadata to write into the input document blob.")
    protected Properties metadata;

    @Param(name = "blobXPath", required = false, description = "The blob xpath on the document. Default blob property for empty parameter.")
    protected String blobXPath;

    @Param(name = "ignorePrefix", required = false, description = "Ignore metadata prefixes or not")
    boolean ignorePrefix = true;

    @Param(name = "save", required = false, values = "true")
    protected boolean save = true;

    @OperationMethod
    public DocumentModel run(DocumentModel doc) {
        Map<String, Object> metadataMap = new HashMap<>(metadata.size());
        for (Map.Entry<String, String> entry : metadata.entrySet()) {
            metadataMap.put(entry.getKey(), entry.getValue());
        }
        BlobHolder blobHolder;
        if (blobXPath != null) {
            blobHolder = new DocumentBlobHolder(doc, blobXPath);
        } else {
            blobHolder = doc.getAdapter(BlobHolder.class);
        }
        Blob blob = blobHolder.getBlob();
        if (blob == null) {
            String message;
            if (blobXPath == null) {
                message = "No blob attached for document '" + doc.getId() + "'. Please specify a blobXPath parameter.";
            } else {
                message = "No blob attached for document '" + doc.getId() + "' and blob xpath '" + blobXPath
                        + "'. Please specify another blobXPath parameter.";
            }
            throw new BinaryMetadataException(message);
        }
        Blob newBlob = binaryMetadataService.writeMetadata(processor, blob, metadataMap, ignorePrefix);
        blobHolder.setBlob(newBlob);
        if (save) {
            doc = session.saveDocument(doc);
        }
        return doc;
    }

}
