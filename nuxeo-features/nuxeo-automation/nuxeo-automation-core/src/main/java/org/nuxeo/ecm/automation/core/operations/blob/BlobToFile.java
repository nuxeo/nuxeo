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

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

import org.nuxeo.common.Environment;
import org.nuxeo.ecm.automation.OperationContext;
import org.nuxeo.ecm.automation.OperationException;
import org.nuxeo.ecm.automation.core.Constants;
import org.nuxeo.ecm.automation.core.annotations.Context;
import org.nuxeo.ecm.automation.core.annotations.Operation;
import org.nuxeo.ecm.automation.core.annotations.OperationMethod;
import org.nuxeo.ecm.automation.core.annotations.Param;
import org.nuxeo.ecm.automation.core.collectors.BlobCollector;
import org.nuxeo.ecm.core.api.Blob;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
@Operation(id = BlobToFile.ID, category = Constants.CAT_BLOB, label = "Export to File", description = "Save the input blob(s) as a file(s) into the given target directory. The blob(s) filename is used as the file name. You can specify an optional <b>prefix</b> string to prepend to the file name. Return back the blob(s).", aliases = {
        "Blob.ToFile" })
public class BlobToFile {

    public static final String ID = "Blob.ExportToFS";

    @Context
    protected OperationContext ctx;

    @Param(name = "directory", required = true)
    protected String directory;

    @Param(name = "prefix", required = false)
    protected String prefix;

    protected File root;

    protected void init() {
        root = new File(directory);
        root.mkdirs();
    }

    protected boolean isTargetDirectoryForbidden() {
        File nuxeoHome = Environment.getDefault().getServerHome().getAbsoluteFile();
        return Paths.get(directory)
                    .toAbsolutePath()
                    .normalize()
                    .startsWith(nuxeoHome.toPath().toAbsolutePath().normalize());
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
        Files.move(tmp.toPath(), file.toPath());
    }

    @OperationMethod(collector = BlobCollector.class)
    public Blob run(Blob blob) throws IOException, OperationException {
        if (!ctx.getPrincipal().isAdministrator()) {
            throw new OperationException("Not allowed. You must be administrator to use this operation");
        }
        if (isTargetDirectoryForbidden()) {
            throw new OperationException(
                    "Not allowed. The target directory is forbidden for this operation (" + directory + ").");
        }
        init();
        writeFile(blob);
        return blob;
    }

}
