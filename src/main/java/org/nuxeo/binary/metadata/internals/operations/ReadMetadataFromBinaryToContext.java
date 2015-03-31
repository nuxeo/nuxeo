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

import java.util.ArrayList;

import org.nuxeo.binary.metadata.api.BinaryMetadataService;
import org.nuxeo.ecm.automation.OperationContext;
import org.nuxeo.ecm.automation.core.Constants;
import org.nuxeo.ecm.automation.core.annotations.Context;
import org.nuxeo.ecm.automation.core.annotations.Operation;
import org.nuxeo.ecm.automation.core.annotations.OperationMethod;
import org.nuxeo.ecm.automation.core.annotations.Param;
import org.nuxeo.ecm.automation.core.util.StringList;
import org.nuxeo.ecm.core.api.Blob;

/**
 * @since 7.1
 */
@Operation(id = ReadMetadataFromBinaryToContext.ID, category = Constants.CAT_EXECUTION, label = "Read Metadata From "
        + "Binary to Context", description = "Read Metadata From binary to Context "
        + "for a given input blob and given metadata to inject into the "
        + "Operation context (if not specified, all metadata will be injected) " + "", since = "7.1", addToStudio = true, aliases = { "Context.ReadMetadataFromBinary" })
public class ReadMetadataFromBinaryToContext {

    public static final String ID = "Context.SetMetadataFromBlob";

    public static final String CTX_BINARY_METADATA = "binaryMetadata";

    @Context
    protected BinaryMetadataService binaryMetadataService;

    @Context
    protected OperationContext operationContext;

    @Param(name = "ignorePrefix", required = false, description = "Ignore metadata prefixes or not")
    boolean ignorePrefix = true;

    @Param(name = "processor", required = false, description = "The processor to execute for overriding the input blob.")
    protected String processor = "exifTool";

    @Param(name = "metadata", required = false, description = "Metadata list to filter on the blob.")
    protected StringList metadata;

    @OperationMethod
    public void run(Blob blob) {
        if (metadata == null || metadata.isEmpty()) {
            operationContext.put(CTX_BINARY_METADATA, binaryMetadataService.readMetadata(blob, ignorePrefix));
        } else {
            ArrayList<String> metadataList = new ArrayList<>();
            for (String meta : metadata) {
                metadataList.add(meta);
            }
            operationContext.put(CTX_BINARY_METADATA,
                    binaryMetadataService.readMetadata(blob, metadataList,
                            ignorePrefix));
        }
    }
}
