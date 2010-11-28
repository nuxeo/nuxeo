/*
 * (C) Copyright 2006-2007 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *
 * $Id: ZipReader.java 29029 2008-01-14 18:38:14Z ldoguin $
 */

package org.nuxeo.ecm.core.io.impl.plugins;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.nuxeo.common.utils.FileUtils;
import org.nuxeo.ecm.core.io.DocumentReader;
import org.nuxeo.ecm.core.io.ExportedDocument;
import org.nuxeo.ecm.core.io.impl.AbstractDocumentReader;

/**
 * A reader to read zip files. If the zip file is recognized as a nuxeo archive
 * then the {@link NuxeoArchiveReader} will be used to read the zip otherwise
 * the zip will be deflated to a temporary directory and then
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
                root = File.createTempFile("nuxeo-import-", ".unzip");
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

    private static void extract(ZipInputStream in, ZipEntry entry, File root)
            throws IOException {
        if (!entry.isDirectory()) { // create the directtory
            File file = new File(root, entry.getName());
            if (!file.getParentFile().mkdirs()) { // make sure all parent
                                                  // directory exists
                throw new IOException("Failed to create directory: "
                        + file.getParent());
            }
            // write the file content
            FileOutputStream out = new FileOutputStream(file);
            try {
                FileUtils.copy(in, out);
            } finally {
                out.close();
            }
        }
    }

}
