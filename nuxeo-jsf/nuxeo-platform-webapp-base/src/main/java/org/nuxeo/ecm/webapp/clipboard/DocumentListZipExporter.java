/*
 * (C) Copyright 2011 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Laurent Doguin
 */
package org.nuxeo.ecm.webapp.clipboard;

import java.io.BufferedInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipOutputStream;

import org.nuxeo.common.utils.StringUtils;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.Blobs;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.LifeCycleConstants;
import org.nuxeo.ecm.core.api.blobholder.BlobHolder;
import org.nuxeo.ecm.core.api.impl.blob.FileBlob;
import org.nuxeo.runtime.api.Framework;

public class DocumentListZipExporter {

    public static final String ZIP_ENTRY_ENCODING_PROPERTY = "zip.entry.encoding";

    public static enum ZIP_ENTRY_ENCODING_OPTIONS {
        ascii
    }

    private static final int BUFFER = 2048;

    private static final String SUMMARY_FILENAME = "INDEX.txt";

    public Blob exportWorklistAsZip(List<DocumentModel> documents, CoreSession documentManager, boolean exportAllBlobs)
            throws IOException {
        StringBuilder blobList = new StringBuilder();

        FileBlob blob = new FileBlob("zip");

        try (ZipOutputStream out = new ZipOutputStream(new FileOutputStream(blob.getFile()))) {
            out.setMethod(ZipOutputStream.DEFLATED);
            out.setLevel(9);
            byte[] data = new byte[BUFFER];

            for (DocumentModel doc : documents) {

                // first check if DM is attached to the core
                if (doc.getSessionId() == null) {
                    // refetch the doc from the core
                    doc = documentManager.getDocument(doc.getRef());
                }

                // NXP-2334 : skip deleted docs
                if (LifeCycleConstants.DELETED_STATE.equals(doc.getCurrentLifeCycleState())) {
                    continue;
                }

                BlobHolder bh = doc.getAdapter(BlobHolder.class);
                if (doc.isFolder() && !isEmptyFolder(doc, documentManager)) {
                    addFolderToZip("", out, doc, data, documentManager, blobList, exportAllBlobs);
                } else if (bh != null) {
                    addBlobHolderToZip("", out, doc, data, blobList, bh, exportAllBlobs);
                }
            }
            if (blobList.length() > 1) {
                addSummaryToZip(out, data, blobList);
            }
        } catch (IOException cause) {
            blob.getFile().delete();
            throw cause;
        }

        return blob;
    }

    private void addFolderToZip(String path, ZipOutputStream out, DocumentModel doc, byte[] data,
            CoreSession documentManager, StringBuilder blobList, boolean exportAllBlobs) throws
            IOException {

        String title = doc.getTitle();
        List<DocumentModel> docList = documentManager.getChildren(doc.getRef());
        for (DocumentModel docChild : docList) {
            // NXP-2334 : skip deleted docs
            if (LifeCycleConstants.DELETED_STATE.equals(docChild.getCurrentLifeCycleState())) {
                continue;
            }
            BlobHolder bh = docChild.getAdapter(BlobHolder.class);
            String newPath = null;
            if (path.length() == 0) {
                newPath = title;
            } else {
                newPath = path + "/" + title;
            }
            if (docChild.isFolder() && !isEmptyFolder(docChild, documentManager)) {
                addFolderToZip(newPath, out, docChild, data, documentManager, blobList, exportAllBlobs);
            } else if (bh != null) {
                addBlobHolderToZip(newPath, out, docChild, data, blobList, bh, exportAllBlobs);
            }
        }
    }

    private boolean isEmptyFolder(DocumentModel doc, CoreSession documentManager) {

        List<DocumentModel> docList = documentManager.getChildren(doc.getRef());
        for (DocumentModel docChild : docList) {
            // If there is a blob or a folder, it is not empty.
            if (docChild.getAdapter(BlobHolder.class) != null || docChild.isFolder()) {
                return false;
            }
        }
        return true;
    }

    /**
     * Writes a summary file and puts it in the archive.
     */
    private void addSummaryToZip(ZipOutputStream out, byte[] data, StringBuilder sb) throws IOException {

        Blob content = Blobs.createBlob(sb.toString());

        BufferedInputStream buffi = new BufferedInputStream(content.getStream(), BUFFER);

        ZipEntry entry = new ZipEntry(SUMMARY_FILENAME);
        out.putNextEntry(entry);
        int count = buffi.read(data, 0, BUFFER);

        while (count != -1) {
            out.write(data, 0, count);
            count = buffi.read(data, 0, BUFFER);
        }
        out.closeEntry();
        buffi.close();
    }

    private void addBlobHolderToZip(String path, ZipOutputStream out, DocumentModel doc, byte[] data,
            StringBuilder blobList, BlobHolder bh, boolean exportAllBlobs) throws IOException {
        List<Blob> blobs = new ArrayList<Blob>();

        if (exportAllBlobs) {
            if (bh.getBlobs() != null) {
                blobs = bh.getBlobs();
            }
        } else {
            Blob mainBlob = bh.getBlob();
            if (mainBlob != null) {
                blobs.add(mainBlob);
            }
        }

        if (blobs.size() > 0) { // add document info
            SimpleDateFormat format = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss");
            if (path.length() > 0) {
                blobList.append(path).append('/');
            }
            blobList.append(doc.getTitle()).append(" ");
            blobList.append(doc.getType()).append(" ");

            Calendar c = (Calendar) doc.getPropertyValue("dc:modified");
            if (c != null) {
                blobList.append(format.format(c.getTime()));
            }
            blobList.append("\n");
        }

        for (Blob content : blobs) {
            String fileName = content.getFilename();
            if (fileName == null) {
                // use a default value
                fileName = "file.bin";
            }
            BufferedInputStream buffi = new BufferedInputStream(content.getStream(), BUFFER);

            // Workaround to deal with duplicate file names.
            int tryCount = 0;
            String entryPath = null;
            String entryName = null;
            while (true) {
                try {
                    ZipEntry entry = null;
                    if (tryCount == 0) {
                        entryName = fileName;
                    } else {
                        entryName = formatFileName(fileName, "(" + tryCount + ")");
                    }
                    if (path.length() == 0) {
                        entryPath = entryName;
                    } else {
                        entryPath = path + "/" + entryName;
                    }
                    entryPath = escapeEntryPath(entryPath);
                    entry = new ZipEntry(entryPath);
                    out.putNextEntry(entry);
                    break;
                } catch (ZipException e) {
                    tryCount++;
                }
            }
            blobList.append(" - ").append(entryName).append("\n");

            int count = buffi.read(data, 0, BUFFER);
            while (count != -1) {
                out.write(data, 0, count);
                count = buffi.read(data, 0, BUFFER);
            }
            out.closeEntry();
            buffi.close();
        }
    }

    private String formatFileName(String filename, String count) {
        StringBuilder sb = new StringBuilder();
        CharSequence name = filename.subSequence(0, filename.lastIndexOf("."));
        CharSequence extension = filename.subSequence(filename.lastIndexOf("."), filename.length());
        sb.append(name).append(count).append(extension);
        return sb.toString();
    }

    protected String escapeEntryPath(String path) {
        String zipEntryEncoding = Framework.getProperty(ZIP_ENTRY_ENCODING_PROPERTY);
        if (zipEntryEncoding != null && zipEntryEncoding.equals(ZIP_ENTRY_ENCODING_OPTIONS.ascii.toString())) {
            return StringUtils.toAscii(path, true);
        }
        return path;
    }

}
