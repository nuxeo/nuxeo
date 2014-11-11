/*
 * (C) Copyright 2006-2008 Nuxeo SAS (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     bstefanescu
 */
package org.nuxeo.ecm.automation.core.operations.blob;

import java.io.File;
import java.io.IOException;

import org.nuxeo.ecm.automation.core.Constants;
import org.nuxeo.ecm.automation.core.annotations.Operation;
import org.nuxeo.ecm.automation.core.annotations.OperationMethod;
import org.nuxeo.ecm.automation.core.annotations.Param;
import org.nuxeo.ecm.automation.core.util.BlobList;
import org.nuxeo.ecm.core.api.Blob;

/**
 * Save the input document
 *
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
@Operation(id = BlobToFile.ID, category = Constants.CAT_BLOB, label = "Export to File", description = "Save the input blob(s) as a file(s) into the given target directory. The blob(s) filename is used as the file name. You can specify an optional <b>prefix</b> string to prepend to the file name. Return back the blob(s).")
public class BlobToFile {

    public static final String ID = "Blob.ToFile";

    @Param(name = "directory", required = true)
    protected String directory;

    @Param(name = "prefix", required = false)
    protected String prefix;

    protected File root;

    protected void init() {
        root = new File(directory);
        root.mkdirs();
    }

    protected File getFile(String name) {
        return new File(root, prefix != null ? prefix + name : name);
    }

    protected void writeFile(Blob blob) throws IOException {
        String name = blob.getFilename();
        if (name.length() == 0) {
            name = "blob#" + Integer.toHexString(System.identityHashCode(blob));
        }
        blob.transferTo(getFile(name));
    }

    @OperationMethod
    public Blob run(Blob blob) throws Exception {
        init();
        writeFile(blob);
        return blob;
    }

    @OperationMethod
    public BlobList run(BlobList blobs) throws Exception {
        init();
        for (Blob blob : blobs) {
            writeFile(blob);
        }
        return blobs;
    }

}
