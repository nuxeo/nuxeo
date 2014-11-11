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
 * $Id: NuxeoArchiveReader.java 29029 2008-01-14 18:38:14Z ldoguin $
 */

package org.nuxeo.ecm.core.io.impl.plugins;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.io.SAXReader;
import org.nuxeo.common.utils.FileUtils;
import org.nuxeo.common.utils.Path;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.impl.blob.StreamingBlob;
import org.nuxeo.ecm.core.io.ExportConstants;
import org.nuxeo.ecm.core.io.ExportedDocument;
import org.nuxeo.ecm.core.io.impl.AbstractDocumentReader;
import org.nuxeo.ecm.core.io.impl.DWord;
import org.nuxeo.ecm.core.io.impl.ExportedDocumentImpl;
import org.nuxeo.runtime.services.streaming.FileSource;
import org.nuxeo.runtime.services.streaming.ZipEntrySource;

/**
 * Reads nuxeo archives generated using {@link NuxeoArchiveWriter}.
 *
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
public class NuxeoArchiveReader extends AbstractDocumentReader {

    private ZipInputStream in;

    private String file;

    private final Collection<File> filesToDelete = new ArrayList<File>();

    public NuxeoArchiveReader(URL url) throws IOException {
        this(url.openStream());
        if (url.getProtocol().equals("file")) {
            file = FileUtils.getFileFromURL(url).getAbsolutePath();
        }
    }

    public NuxeoArchiveReader(File file) throws IOException {
        this(new FileInputStream(file));
        this.file = file.getAbsolutePath();
    }

    public NuxeoArchiveReader(InputStream in) throws IOException {
        this(new ZipInputStream(in));
    }

    public NuxeoArchiveReader(ZipInputStream in) throws IOException {
        this(in, true);
    }

    /**
     * Package-visible constructor used by {@link ZipReader}.
     *
     * @param in
     * @param checkMarker
     * @throws IOException
     */
    NuxeoArchiveReader(ZipInputStream in, boolean checkMarker)
            throws IOException {
        this.in = in;
        if (checkMarker) {
            checkMarker();
        }
    }

    @Override
    public ExportedDocument read() throws IOException {
        ZipEntry entry = in.getNextEntry();
        if (entry == null) {
            return null;
        }
        if (!entry.isDirectory()) {
            if (entry.getName().equals(ExportConstants.DOCUMENT_FILE)) {
                // the repository ROOT! TODO: how to handle root? it doesn't
                // have a dir ..
                ExportedDocument xdoc = new ExportedDocumentImpl();
                xdoc.setPath(new Path("/"));
                xdoc.setDocument(loadXML(entry));
                return xdoc;
            } else {
                throw new IOException("Invalid Nuxeo archive");
            }
        }
        int count = getFilesCount(entry);
        if (count == 0) {
            return read(); // empty dir -> try next directory
        }
        String name = entry.getName();
        ExportedDocument xdoc = new ExportedDocumentImpl();
        xdoc.setPath(new Path(name).removeTrailingSeparator());
        for (int i = 0; i < count; i++) {
            entry = in.getNextEntry();
            name = entry.getName();
            if (name.endsWith(ExportConstants.DOCUMENT_FILE)) {
                xdoc.setDocument(loadXML(entry));
            } else if (name.endsWith(".xml")) { // external doc file
                xdoc.putDocument(FileUtils.getFileNameNoExt(entry.getName()),
                        loadXML(entry));
            } else { // should be a blob
                xdoc.putBlob(FileUtils.getFileName(entry.getName()),
                        createBlob(entry));
            }
        }
        return xdoc;
    }

    @Override
    public void close() {
        if (in != null) {
            try {
                in.close();
            } catch (IOException e) {
                // do nothing
            } finally {
                in = null;
            }
        }
        for (File file : filesToDelete) {
            file.delete();
        }
    }

    private static int getFilesCount(ZipEntry entry) throws IOException {
        byte[] bytes = entry.getExtra();
        if (bytes == null) {
            return 0;
        } else if (bytes.length != 4) {
            throw new IOException("Invalid Nuxeo Archive");
        } else {
            return new DWord(bytes).getInt();
        }
    }

    private Document loadXML(ZipEntry entry) throws IOException {
        try {
            // the SAXReader is closing the stream so that we need to copy the
            // content somewhere
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            FileUtils.copy(in, baos);
            return new SAXReader().read(new ByteArrayInputStream(
                    baos.toByteArray()));
        } catch (DocumentException e) {
            IOException ioe = new IOException("Failed to read zip entry "
                    + entry.getName() + ": " + e.getMessage());
            ioe.setStackTrace(e.getStackTrace());
            throw ioe;
        }
    }

    private Blob createBlob(ZipEntry entry) throws IOException {
        if (file != null) { // the zip is a file : optimize blob loading -> do
                            // not decompress blobs
            ZipEntrySource src = new ZipEntrySource(file, entry.getName());
            return new StreamingBlob(src);
        } else { // should decompress since this is a generic stream
            File file = File.createTempFile("nuxeo-import", "blob");
            filesToDelete.add(file);
            OutputStream out = new FileOutputStream(file);
            try {
                FileUtils.copy(in, out);
            } finally {
                out.close();
            }
            FileSource src = new FileSource(file);
            return new StreamingBlob(src);
        }
    }

    private void checkMarker() throws IOException {
        ZipEntry entry = in.getNextEntry();
        if (entry == null) {
            throw new IOException(
                    "Not a valid Nuxeo Archive - no marker file found (unexpected end of zip)");
        }
        if (!isMarkerEntry(entry)) {
            throw new IOException(
                    "Not a valid Nuxeo Archive - no marker file found");
        }
    }

    public static boolean isMarkerEntry(ZipEntry entry) {
        return entry.getName().equals(ExportConstants.MARKER_FILE);
    }

}
