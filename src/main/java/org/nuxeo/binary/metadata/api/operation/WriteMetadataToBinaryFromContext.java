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
package org.nuxeo.binary.metadata.api.operation;

import org.nuxeo.binary.metadata.api.service.BinaryMetadataService;
import org.nuxeo.ecm.automation.core.Constants;
import org.nuxeo.ecm.automation.core.annotations.Context;
import org.nuxeo.ecm.automation.core.annotations.Operation;
import org.nuxeo.ecm.automation.core.annotations.OperationMethod;
import org.nuxeo.ecm.automation.core.annotations.Param;
import org.nuxeo.ecm.core.api.DocumentModelList;

/**
 * @since 7.1
 */
@Operation(id = WriteMetadataToBinaryFromContext.ID, category = Constants.CAT_BLOB, label = "Write Metadata To Binary From Context", description = "", since = "7.1", addToStudio = true)
public class WriteMetadataToBinaryFromContext {

    public static final String ID = "Binary.WriteMetadataFromContext";

    @Context
    protected BinaryMetadataService binaryMetadataService;

    @Param(name = "processor", required = false, description = "The processor.")
    protected String processor = "exifTool";

    @OperationMethod
    public DocumentModelList run() {
        return null;
    }
}
