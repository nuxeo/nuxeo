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

import java.io.IOException;

import org.nuxeo.ecm.automation.core.Constants;
import org.nuxeo.ecm.automation.core.annotations.Operation;
import org.nuxeo.ecm.automation.core.annotations.OperationMethod;
import org.nuxeo.ecm.automation.core.annotations.Param;
import org.nuxeo.ecm.automation.core.util.BlobList;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.utils.BlobUtils;

/**
 * TODO: detect mine?
 *
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
@Operation(id = CreateZip.ID, category = Constants.CAT_BLOB, label = "Zip", description = "Creates a zip file from the input file(s). If no file name is given, the first file name in the input will be used. Returns the zip file.")
public class CreateZip {

    public static final String ID = "Blob.CreateZip";

    @Param(name = "filename", required = false)
    protected String fileName;

    @OperationMethod
    public Blob run(Blob blob) throws IOException {
        return BlobUtils.zip(blob, fileName);
    }

    @OperationMethod
    public Blob run(BlobList blobs) throws IOException {
        return BlobUtils.zip(blobs, fileName);
    }

}
