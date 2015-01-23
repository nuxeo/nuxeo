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
@Operation(id = WriteMetadataToBinaryFromContext.ID, category = Constants.CAT_BLOB, label = "Write Metadata To Binary From"
        + " Context", description = "Write Metadata To Binary From Context "
        + "given a processor name (or the default Nuxeo one) and given metadata" + ".", since = "7.1", addToStudio = true, aliases = { "Binary.WriteMetadataFromContext" })
public class WriteMetadataToBinaryFromContext {

    public static final String ID = "Blob.SetMetadataFromContext";

    @Context
    protected BinaryMetadataService binaryMetadataService;

    @Param(name = "processor", required = false, description = "The processor to execute for overriding the input blob.")
    protected String processor = "exifTool";

    @Param(name = "metadata", required = true, description = "Metadata to write into the input blob.")
    protected Properties metadata;

    @OperationMethod
    public void run(Blob blob) {
        Map<String, Object> metadataMap = new HashMap<>(metadata.size());
        for (Map.Entry<String, String> entry : metadata.entrySet()) {
            metadataMap.put(entry.getKey(), entry.getValue());
        }
        binaryMetadataService.writeMetadata(processor, blob, metadataMap);
    }
}
