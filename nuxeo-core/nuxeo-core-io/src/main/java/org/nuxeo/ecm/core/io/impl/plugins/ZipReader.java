/*
 * (C) Copyright 2006-2014 Nuxeo SA (http://nuxeo.com/) and others.
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
 *
 */

package org.nuxeo.ecm.core.io.impl.plugins;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.apache.commons.io.IOUtils;
import org.nuxeo.ecm.core.io.DocumentReader;
import org.nuxeo.ecm.core.io.ExportedDocument;
import org.nuxeo.ecm.core.io.impl.AbstractDocumentReader;
import org.nuxeo.runtime.api.Framework;

/**
 * A reader to read zip files. If the zip file is recognized as a nuxeo archive then the {@link NuxeoArchiveReader} will
 * be used to read the zip otherwise the zip will be deflated to a temporary directory and then
 * {@link XMLDirectoryReader} will be used to read the zip.
 *
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
public class ZipReader extends AbstractDocumentReader {

    private final ZipInputStream in;

    private DocumentReader delegate;

    public ZipReader(File file) throws IOException {
        this(new FileInputStream(file));
    }

    public ZipReader(InputStream in) throws IOException {
        this.in = new ZipInputStream(in);
        initialize();
    }

    private void initialize() throws IOException {
        ZipEntry entry = in.getNextEntry();
        if (NuxeoArchiveReader.isMarkerEntry(entry)) {
            delegate = new NuxeoArchiveReader(in, false);
        } else { // not a nuxeo archive file
            File root = null;
            try {
                root = Framework.createTempFile("nuxeo-import-", ".unzip");
                Framework.trackFile(root, root);
                root.delete();
                root.mkdirs();
                extract(in, entry, root);
                while ((entry = in.getNextEntry()) != null) {
                    extract(in, entry, root);
                }
            } finally {
                in.close();
            }
            delegate = new XMLDirectoryReader(root);
        }
    }

    @Override
    public ExportedDocument read() throws IOException {
        return delegate.read();
    }

    @Override
    public void close() {
        delegate.close();
    }

    private static void extract(ZipInputStream in, ZipEntry entry, File root) throws IOException {
        if (entry.getName().contains("..")) {
            return;
        }

        if (!entry.isDirectory()) { // create the directtory
            File file = new File(root, entry.getName());
            if (!file.getParentFile().mkdirs()) { // make sure all parent
                                                  // directory exists
                throw new IOException("Failed to create directory: " + file.getParent());
            }
            // write the file content
            try (FileOutputStream out = new FileOutputStream(file)) {
                IOUtils.copy(in, out);
            }
        }
    }

}
