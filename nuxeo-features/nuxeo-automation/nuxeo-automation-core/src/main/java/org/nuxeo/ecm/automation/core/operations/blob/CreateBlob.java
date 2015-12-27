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

import java.net.URL;

import org.nuxeo.ecm.automation.core.Constants;
import org.nuxeo.ecm.automation.core.annotations.Operation;
import org.nuxeo.ecm.automation.core.annotations.OperationMethod;
import org.nuxeo.ecm.automation.core.annotations.Param;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.impl.blob.URLBlob;

/**
 * TODO: detect mine?
 *
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
@Operation(id = CreateBlob.ID, category = Constants.CAT_FETCH, label = "File From URL", description = "Creates a file from a given URL. The file parameter specifies how to retrieve the file content. It should be an URL to the file you want to use as the source. You can also use an expression to get an URL from the context. Returns the created file.", aliases = { "Blob.Create" })
public class CreateBlob {

    public static final String ID = "Blob.CreateFromURL";

    /** For tests. */
    public static boolean skipProtocolCheck;

    @Param(name = "file")
    protected URL file;

    @Param(name = "mime-type", required = false)
    protected String mimeType;

    @Param(name = "filename", required = false)
    protected String fileName;

    @Param(name = "encoding", required = false)
    protected String encoding;

    @OperationMethod
    public Blob run() {
        String protocol = file.getProtocol();
        if (!"http".equals(protocol) && !"https".equals(protocol) && !"ftp".equals(protocol)) {
            // don't let file: through
            if (!skipProtocolCheck) {
                return null;
            }
        }
        if (fileName == null) {
            fileName = file.getPath();
            int i = fileName.lastIndexOf('/');
            if (i > -1) {
                fileName = fileName.substring(i + 1);
            }
        }
        if (mimeType == null) { // TODO detect mime type

        }
        URLBlob blob = new URLBlob(file, mimeType, encoding);
        blob.setFilename(fileName);
        return blob;
    }

}
