/* 
 * Copyright (c) 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
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
@Operation(id = CreateBlob.ID, category = Constants.CAT_FETCH, label = "File From URL", description = "Creates a file from a given URL. The file parameter specifies how to retrieve the file content. It should be an URL to the file you want to use as the source. You can also use an expression to get an URL from the context. Returns the created file.")
public class CreateBlob {

    public static final String ID = "Blob.Create";

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
        if (fileName == null) {
            fileName = file.getPath();
            int i = fileName.lastIndexOf('/');
            if (i > -1) {
                fileName = fileName.substring(i + 1);
            }
        }
        if (mimeType == null) { // TODO detect mime type

        }
        return new URLBlob(file, mimeType, encoding, fileName, null);
    }

}
