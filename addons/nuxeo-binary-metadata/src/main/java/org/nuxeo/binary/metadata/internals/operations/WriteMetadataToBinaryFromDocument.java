/*
 * (C) Copyright 2014 Nuxeo SA (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl-2.1.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
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
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.blobholder.BlobHolder;

/**
 * @since 7.1
 */
@Operation(id = WriteMetadataToBinaryFromDocument.ID, category = Constants.CAT_BLOB, label = "Write Metadata To Binary From Document", description = "Write metadata to a Blob (xpath parameter, or BlobHolder if empty) from a document (input) given a custom metadata mapping defined in a Properties parameter, using a named processor (exifTool for instance).", since = "7.1", addToStudio = true, aliases = { "Binary.WriteMetadataFromDocument" })
public class WriteMetadataToBinaryFromDocument {

    public static final String ID = "Blob.SetMetadataFromDocument";

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

    @OperationMethod
    public void run(DocumentModel doc) {
        Map<String, Object> metadataMap = new HashMap<>(metadata.size());
        for (Map.Entry<String, String> entry : metadata.entrySet()) {
            metadataMap.put(entry.getKey(), entry.getValue());
        }
        Blob blob;
        if (blobXPath != null) {
            blob = doc.getProperty(blobXPath).getValue(Blob.class);
        } else {
            BlobHolder blobHolder = doc.getAdapter(BlobHolder.class);
            blob = blobHolder.getBlob();
        }
        if (blob != null) {
            binaryMetadataService.writeMetadata(processor, blob, metadataMap, ignorePrefix);
        } else {
            String message;
            if (blobXPath != null) {
                message = "No blob attached for document '" + doc.getId() + "'. Please specify a blobXPath parameter.";
            } else {
                message = "No blob attached for document '" + doc.getId() + "' and blob xpath '" + blobXPath
                        + "'. Please specify another blobXPath parameter.";
            }
            throw new BinaryMetadataException(message);
        }
    }
}
