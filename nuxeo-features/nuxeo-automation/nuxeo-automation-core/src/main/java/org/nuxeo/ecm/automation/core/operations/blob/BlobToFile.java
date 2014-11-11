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

import java.io.File;
import java.io.IOException;

import org.nuxeo.ecm.automation.core.Constants;
import org.nuxeo.ecm.automation.core.annotations.Operation;
import org.nuxeo.ecm.automation.core.annotations.OperationMethod;
import org.nuxeo.ecm.automation.core.annotations.Param;
import org.nuxeo.ecm.automation.core.collectors.BlobCollector;
import org.nuxeo.ecm.core.api.Blob;

/**
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
        // get the output file
        File file = getFile(name);
        // use a .tmp extension while writing the blob and rename it when write
        // is done this is allowing external tools to track when the file becomes
        // available.
        File tmp = new File(file.getParentFile(), file.getName() + ".tmp");
        blob.transferTo(tmp);
        tmp.renameTo(file);
    }

    @OperationMethod(collector=BlobCollector.class)
    public Blob run(Blob blob) throws Exception {
        init();
        writeFile(blob);
        return blob;
    }

}
