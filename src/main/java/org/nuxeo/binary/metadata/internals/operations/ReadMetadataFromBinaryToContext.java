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
