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

import org.nuxeo.binary.metadata.api.BinaryMetadataService;
import org.nuxeo.ecm.automation.OperationContext;
import org.nuxeo.ecm.automation.core.Constants;
import org.nuxeo.ecm.automation.core.annotations.Context;
import org.nuxeo.ecm.automation.core.annotations.Operation;
import org.nuxeo.ecm.automation.core.annotations.OperationMethod;
import org.nuxeo.ecm.automation.core.annotations.Param;
import org.nuxeo.ecm.core.api.DocumentModel;

/**
 * @since 7.1
 */
@Operation(id = TriggerMetadataMappingOnDocument.ID, category = Constants.CAT_BLOB, label = "Trigger Metadata Mapping", description = "Write Metadata To Document From Binary according to metadata mapping.", since = "7.1", addToStudio = true, aliases = { "Document.TriggerMetadataMapping" })
public class TriggerMetadataMappingOnDocument {

    public static final String ID = "Document.SetMetadataFromBlob";

    @Context
    protected BinaryMetadataService binaryMetadataService;

    @Context
    protected OperationContext operationContext;

    @Param(name = "processor", required = false, description = "The processor to execute for reading blobs metadata.")
    protected String processor = "exifTool";

    @Param(name = "metadataMappingId", required = true, description = "The metadata mapping id to apply on the input document.")
    protected String metadataMappingId;

    @OperationMethod
    public void run(DocumentModel document) {
        binaryMetadataService.writeMetadata(document, metadataMappingId);
    }
}
