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
