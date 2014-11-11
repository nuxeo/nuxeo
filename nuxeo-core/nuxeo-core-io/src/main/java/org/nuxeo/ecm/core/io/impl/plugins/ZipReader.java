/*
 * Copyright (c) 2006-2014 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
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

import org.nuxeo.common.utils.FileUtils;
import org.nuxeo.ecm.core.io.DocumentReader;
import org.nuxeo.ecm.core.io.ExportedDocument;
import org.nuxeo.ecm.core.io.impl.AbstractDocumentReader;
import org.nuxeo.runtime.api.Framework;

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
