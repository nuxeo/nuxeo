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

import org.dom4j.Document;
import org.dom4j.io.OutputFormat;
import org.dom4j.io.XMLWriter;
import org.nuxeo.common.utils.FileUtils;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.DocumentLocation;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.io.DocumentTranslationMap;
import org.nuxeo.ecm.core.io.ExportConstants;
import org.nuxeo.ecm.core.io.ExportedDocument;
import org.nuxeo.ecm.core.io.impl.AbstractDocumentWriter;
import org.nuxeo.ecm.core.io.impl.DWord;
import org.nuxeo.ecm.core.io.impl.DocumentTranslationMapImpl;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class NuxeoArchiveWriter extends AbstractDocumentWriter {

    protected ZipOutputStream out;


    public NuxeoArchiveWriter(File destination) throws IOException {
        this(new BufferedOutputStream(new FileOutputStream(destination)),
                Deflater.DEFAULT_COMPRESSION);
    }

    public NuxeoArchiveWriter(File destination, int compressionLevel)
            throws IOException {
        this(new BufferedOutputStream(new FileOutputStream(destination)),
                compressionLevel);
    }

    public NuxeoArchiveWriter(OutputStream out) throws IOException {
        this(new ZipOutputStream(out), Deflater.DEFAULT_COMPRESSION);
    }

    public NuxeoArchiveWriter(OutputStream out, int compressionLevel)
            throws IOException {
        this(new ZipOutputStream(out), compressionLevel);
    }

    public NuxeoArchiveWriter(ZipOutputStream out) throws IOException {
        this(out, Deflater.DEFAULT_COMPRESSION);
    }

    public NuxeoArchiveWriter(ZipOutputStream out, int compressionLevel)
            throws IOException {
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
    public DocumentTranslationMap write(ExportedDocument doc)
            throws IOException {
        String path = doc.getPath().toString();
        writeDocument(path, doc);
        // keep location unchanged
        DocumentLocation oldLoc = doc.getSourceLocation();
        String oldServerName = oldLoc.getServerName();
        DocumentRef oldDocRef = oldLoc.getDocRef();
        DocumentTranslationMap map = new DocumentTranslationMapImpl(
                oldServerName, oldServerName);
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

    private void writeDocument(String path, ExportedDocument doc)
            throws IOException {

        if (path.equals("/") || path.length() == 0) {
            path = "";
        } else { // avoid adding a root entry
            path += '/';
            ZipEntry entry = new ZipEntry(path);
            // store the number of child as an extra info on the entry
            entry.setExtra(new DWord(doc.getFilesCount()).getBytes());
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
            InputStream in = null;
            try {
                in = blobEntry.getValue().getStream();
                FileUtils.copy(in, out);
            } finally {
                if (in != null) {
                    in.close();
                }
                out.closeEntry();
                // System.out.println(">> add entry: "+entry.getName());
            }
        }
    }

    protected static void writeXML(Document doc, OutputStream out) throws IOException {
        OutputFormat format = AbstractDocumentWriter.createPrettyPrint();
        XMLWriter writer = new XMLWriter(out, format);
        writer.write(doc);
    }

    private void writeMarker() throws IOException {
        ZipEntry entry = new ZipEntry(ExportConstants.MARKER_FILE);
        out.putNextEntry(entry);
        out.closeEntry();
    }
}
