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

import org.nuxeo.binary.metadata.api.BinaryMetadataService;
import org.nuxeo.ecm.automation.core.Constants;
import org.nuxeo.ecm.automation.core.annotations.Context;
import org.nuxeo.ecm.automation.core.annotations.Operation;
import org.nuxeo.ecm.automation.core.annotations.OperationMethod;
import org.nuxeo.ecm.automation.core.annotations.Param;
import org.nuxeo.ecm.automation.core.util.Properties;
import org.nuxeo.ecm.core.api.Blob;

/**
 * @since 7.1
 */
@Operation(id = WriteMetadataToBinaryFromContext.ID, category = Constants.CAT_BLOB, label = "Write Metadata To Blob From"
        + " Context", description = "Write Metadata To Blob From Context "
                + "given a processor name (or the default Nuxeo one) and given metadata, and return the updated Blob"
                + ".", since = "7.1", addToStudio = true, aliases = { "Binary.WriteMetadataFromContext" })
public class WriteMetadataToBinaryFromContext {

    public static final String ID = "Blob.SetMetadataFromContext";

    @Context
    protected BinaryMetadataService binaryMetadataService;

    @Param(name = "ignorePrefix", required = false, description = "Ignore metadata prefixes or not")
    boolean ignorePrefix = true;

    @Param(name = "processor", required = false, description = "The processor to execute for overriding the input blob.")
    protected String processor = "exifTool";

    @Param(name = "metadata", required = true, description = "Metadata to write into the input blob.")
    protected Properties metadata;

    @OperationMethod
    public Blob run(Blob blob) {
        Map<String, Object> metadataMap = new HashMap<>(metadata.size());
        for (Map.Entry<String, String> entry : metadata.entrySet()) {
            metadataMap.put(entry.getKey(), entry.getValue());
        }
        return binaryMetadataService.writeMetadata(processor, blob, metadataMap, ignorePrefix);
    }
}
