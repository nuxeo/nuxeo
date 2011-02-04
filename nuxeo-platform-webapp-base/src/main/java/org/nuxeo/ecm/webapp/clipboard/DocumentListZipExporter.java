package org.nuxeo.ecm.webapp.clipboard;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipOutputStream;

import org.nuxeo.common.utils.StringUtils;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.core.api.LifeCycleConstants;
import org.nuxeo.ecm.core.api.blobholder.BlobHolder;
import org.nuxeo.ecm.core.api.impl.blob.StringBlob;
import org.nuxeo.runtime.api.Framework;

public class DocumentListZipExporter {

    public static final String ZIP_ENTRY_ENCODING_PROPERTY = "zip.entry.encoding";

    public static enum  ZIP_ENTRY_ENCODING_OPTIONS {
        ascii
    }

    private static final int BUFFER = 2048;

    private static final String SUMMARY_FILENAME = "INDEX.txt";

    private static final String SUMMARY_HEADER = ".";

    public File exportWorklistAsZip(List<DocumentModel> documents,
            CoreSession documentManager, boolean exportAllBlobs)
            throws ClientException, IOException {
        SummaryImpl summary = new SummaryImpl();
        SummaryEntry summaryRoot = new SummaryEntry("", SUMMARY_HEADER,
                new Date(), "", "", null);
        summaryRoot.setDocumentRef(new IdRef("0"));
        summary.put(new IdRef("0").toString(), summaryRoot);

        File tmpFile = File.createTempFile("NX-BigZipFile-", ".zip");
        tmpFile.deleteOnExit();
        Framework.trackFile(tmpFile, this);
        FileOutputStream fout = new FileOutputStream(tmpFile);
        ZipOutputStream out = new ZipOutputStream(fout);
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

                SummaryEntry summaryLeaf = new SummaryEntry(doc);
                summaryLeaf.setParent(summaryRoot);
                // Quick Fix to avoid adding the logo in summary
                if (doc.getType().equals("Workspace")
                        || doc.getType().equals("WorkspaceRoot")) {
                    summaryLeaf.setFilename("");
                }
                summary.put(summaryLeaf.getPath(), summaryLeaf);

                addFolderToZip("", out, doc, data, documentManager,
                        summary.get(summaryLeaf.getPath()), summary,
                        exportAllBlobs);
            } else if (bh != null) {
                addBlobHolderToZip("", out, doc, data,
                        summary.getSummaryRoot(), summary, bh,
                        exportAllBlobs);
            }
        }
        if (summary.size() > 1) {
            addSummaryToZip(out, data, summary);
        }
        try {
            out.close();
            fout.close();
        } catch (ZipException e) {
            return null;
        }
        return tmpFile;
    }

    private void addFolderToZip(String path, ZipOutputStream out,
            DocumentModel doc, byte[] data, CoreSession documentManager,
            SummaryEntry parent, SummaryImpl summary, boolean exportAllBlobs)
            throws ClientException, IOException {

        String title = (String) doc.getProperty("dublincore", "title");
        List<DocumentModel> docList = documentManager.getChildren(doc.getRef());
        for (DocumentModel docChild : docList) {

            // NXP-2334 : skip deleted docs
            if (LifeCycleConstants.DELETED_STATE.equals(docChild.getCurrentLifeCycleState())) {
                continue;
            }

            BlobHolder bh = docChild.getAdapter(BlobHolder.class);
            if (docChild.isFolder()
                    && !isEmptyFolder(docChild, documentManager)) {

                SummaryEntry summaryLeaf = new SummaryEntry(docChild);
                if (doc.getType().equals("Workspace")
                        || doc.getType().equals("WorkspaceRoot")) {
                    summaryLeaf.setFilename("");
                }
                summaryLeaf.setParent(parent);
                summary.put(summaryLeaf.getPath(), summaryLeaf);

                addFolderToZip(path + title + "/", out, docChild, data,
                        documentManager, summary.get(summaryLeaf.getPath()),
                        summary, exportAllBlobs);
            } else if (bh != null) {
                addBlobHolderToZip(path + title + "/", out, docChild, data,
                        summary.get(parent.getPath()), summary, bh,
                        exportAllBlobs);
            }
        }
    }

    private boolean isEmptyFolder(DocumentModel doc, CoreSession documentManager)
            throws ClientException {

        List<DocumentModel> docList = documentManager.getChildren(doc.getRef());
        for (DocumentModel docChild : docList) {
            // If there is a blob or a folder, it is not empty.
            if (docChild.getAdapter(BlobHolder.class) != null
                    || docChild.isFolder()) {
                return false;
            }
        }
        return true;
    }

    /**
     * Writes a summary file and puts it in the archive.
     */
    private void addSummaryToZip(ZipOutputStream out, byte[] data,
            SummaryImpl summary) throws IOException {

        StringBuilder sb = new StringBuilder();
        sb.append(summary.toString());

        Blob content = new StringBlob(sb.toString());

        BufferedInputStream buffi = new BufferedInputStream(
                content.getStream(), BUFFER);

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

    private void addBlobHolderToZip(String path, ZipOutputStream out,
            DocumentModel doc, byte[] data, SummaryEntry parent,
            SummaryImpl summary, BlobHolder bh, boolean exportAllBlobs)
            throws IOException, ClientException {
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

        for (Blob content : blobs) {
            String fileName = content.getFilename();

            SummaryEntry summaryLeaf = new SummaryEntry(doc);
            summaryLeaf.setParent(parent);
            summary.put(summaryLeaf.getPath(), summaryLeaf);

            BufferedInputStream buffi = new BufferedInputStream(
                    content.getStream(), BUFFER);

            // Workaround to deal with duplicate file names.
            int tryCount = 0;
            while (true) {
                try {
                    ZipEntry entry;
                    if (tryCount == 0) {
                        String entryPath = path + fileName;
                        entryPath = escapeEntryPath(entryPath);
                        entry = new ZipEntry(entryPath);
                    } else {
                        String entryPath = path
                                + formatFileName(fileName, "(" + tryCount + ")");
                        entryPath = escapeEntryPath(entryPath);
                        entry = new ZipEntry(entryPath);
                    }
                    out.putNextEntry(entry);
                    break;
                } catch (ZipException e) {
                    tryCount++;
                }
            }

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
        CharSequence extension = filename.subSequence(
                filename.lastIndexOf("."), filename.length());
        sb.append(name).append(count).append(extension);
        return sb.toString();
    }

    protected String escapeEntryPath(String path) {
        String zipEntryEncoding = Framework.getProperty(ZIP_ENTRY_ENCODING_PROPERTY);
        if (zipEntryEncoding != null
                && zipEntryEncoding.equals(ZIP_ENTRY_ENCODING_OPTIONS.ascii.toString())) {
            return StringUtils.toAscii(path);
        }
        return path;
    }

}
