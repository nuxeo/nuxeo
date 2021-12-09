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
 *
 * $Id: NuxeoArchiveWriter.java 29029 2008-01-14 18:38:14Z ldoguin $
 */

package org.nuxeo.ecm.core.io.impl.plugins;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Map;
import java.util.zip.Deflater;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.apache.commons.io.IOUtils;
import org.dom4j.Document;
import org.dom4j.io.OutputFormat;
import org.dom4j.io.XMLWriter;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.DocumentLocation;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.io.DocumentTranslationMap;
import org.nuxeo.ecm.core.io.ExportConstants;
import org.nuxeo.ecm.core.io.ExportedDocument;
import org.nuxeo.ecm.core.io.impl.AbstractDocumentWriter;
import org.nuxeo.ecm.core.io.impl.DWord;
import org.nuxeo.ecm.core.io.impl.DocumentTranslationMapImpl;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.services.config.ConfigurationService;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
public class NuxeoArchiveWriter extends AbstractDocumentWriter {

    /** @since 2021.13 */
    protected static final String ENABLE_EXTRA_FILES_COUNT_KEY = "nuxeo.core.io.archive.extra.files.count";

    protected ZipOutputStream out;

    public NuxeoArchiveWriter(File destination) throws IOException {
        this(new BufferedOutputStream(new FileOutputStream(destination)), Deflater.DEFAULT_COMPRESSION);
    }

    public NuxeoArchiveWriter(File destination, int compressionLevel) throws IOException {
        this(new BufferedOutputStream(new FileOutputStream(destination)), compressionLevel);
    }

    public NuxeoArchiveWriter(OutputStream out) throws IOException {
        this(new ZipOutputStream(out), Deflater.DEFAULT_COMPRESSION);
    }

    public NuxeoArchiveWriter(OutputStream out, int compressionLevel) throws IOException {
        this(new ZipOutputStream(out), compressionLevel);
    }

    public NuxeoArchiveWriter(ZipOutputStream out) throws IOException {
        this(out, Deflater.DEFAULT_COMPRESSION);
    }

    public NuxeoArchiveWriter(ZipOutputStream out, int compressionLevel) throws IOException {
        this.out = out;
        this.out.setLevel(compressionLevel);
        setComment("");
        // write the marker entry
        writeMarker();
    }

    public void setComment(String comment) {
        if (out != null) {
            out.setComment(ExportConstants.ZIP_HEADER + "\r\n" + comment);
        }
    }

    @Override
    public DocumentTranslationMap write(ExportedDocument doc) throws IOException {
        String path = doc.getPath().toString();
        writeDocument(path, doc);
        // keep location unchanged
        DocumentLocation oldLoc = doc.getSourceLocation();
        String oldServerName = oldLoc.getServerName();
        DocumentRef oldDocRef = oldLoc.getDocRef();
        DocumentTranslationMap map = new DocumentTranslationMapImpl(oldServerName, oldServerName);
        map.put(oldDocRef, oldDocRef);
        return map;
    }

    @Override
    public void close() {
        if (out != null) {
            try {
                out.close();
            } catch (IOException e) {
                // do nothing
            } finally {
                out = null;
            }
        }
    }

    protected void writeDocument(String path, ExportedDocument doc) throws IOException {

        if (path.equals("/") || path.length() == 0) {
            path = "";
        } else { // avoid adding a root entry
            path += '/';
            ZipEntry entry = new ZipEntry(path);
            // store the number of child as an extra info on the entry
            if (Framework.getService(ConfigurationService.class).isBooleanTrue(ENABLE_EXTRA_FILES_COUNT_KEY)) {
                entry.setExtra(new DWord(doc.getFilesCount()).getBytes());
            }
            out.putNextEntry(entry);
            out.closeEntry();
            // System.out.println(">> add entry: "+entry.getName());
        }

        // write metadata
        ZipEntry entry = new ZipEntry(path + ExportConstants.DOCUMENT_FILE);
        out.putNextEntry(entry);
        try {
            writeXML(doc.getDocument(), out);
        } finally {
            out.closeEntry();
            // System.out.println(">> add entry: "+entry.getName());
        }

        // write external documents
        for (Map.Entry<String, Document> ext : doc.getDocuments().entrySet()) {
            String fileName = ext.getKey() + ".xml";
            entry = new ZipEntry(path + fileName);
            out.putNextEntry(entry);
            try {
                writeXML(ext.getValue(), out);
            } finally {
                out.closeEntry();
            }
        }

        // write blobs
        Map<String, Blob> blobs = doc.getBlobs();
        for (Map.Entry<String, Blob> blobEntry : blobs.entrySet()) {
            String fileName = blobEntry.getKey();
            entry = new ZipEntry(path + fileName);
            out.putNextEntry(entry);
            try (InputStream in = blobEntry.getValue().getStream()) {
                IOUtils.copy(in, out);
            }
            // DO NOT CALL out.close(), we want to keep writing to it
            out.closeEntry();
        }
    }

    protected static void writeXML(Document doc, OutputStream out) throws IOException {
        OutputFormat format = AbstractDocumentWriter.createPrettyPrint();
        XMLWriter writer = new XMLWriter(out, format);
        writer.write(doc);
    }

    protected void writeMarker() throws IOException {
        ZipEntry entry = new ZipEntry(ExportConstants.MARKER_FILE);
        out.putNextEntry(entry);
        out.closeEntry();
    }
}
