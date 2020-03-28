/*
 * (C) Copyright 2016 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Thibaud Arguillere
 *     Miguel Nixo
 */
package org.nuxeo.ecm.platform.pdf;

import java.io.File;
import java.io.IOException;

import org.apache.pdfbox.multipdf.PDFMergerUtility;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.nuxeo.ecm.automation.core.util.BlobList;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.core.api.impl.blob.FileBlob;
import org.nuxeo.runtime.api.Framework;

/**
 * Merge several PDFs in one.
 * <p>
 * Basically, the caller adds blobs and then <code>merge()</code>. The PDFs are merged in the order they were added.
 * <p>
 * The class accepts misc parameters: Single <code>Blob</code>, <code>BlobList</code>, single
 * <code>DocumentModel</code>, <code>DocumentModelList</code> or a list of IDs of <code>DocumentModel</code>
 * <p>
 * <i>Notice</i>: These are nuxeo <code>Blob</code>, <code>BlobList</code>, etc.
 * <p>
 * When a <code>DocumentModel</code> is used, the code may expect an xpath to extract the blob from the document. When
 * the xpath parameter is not used (<code>null</code> or ""), the default <code>file:content</code> xpath is used.
 * <p>
 * To let the caller be generic, it's ok to pass a null blob, so it is just ignored.
 *
 * @since 8.10
 */
public class PDFMerge {

    private BlobList blobs = new BlobList();

    public PDFMerge() {

    }

    public PDFMerge(Blob inBlob) {
        addBlob(inBlob);
    }

    public PDFMerge(BlobList inBlobs) {
        addBlobs(inBlobs);
    }

    public PDFMerge(DocumentModel inDoc, String inXPath) {
        addBlob(inDoc, inXPath);
    }

    public PDFMerge(DocumentModelList inDocs, String inXPath) {
        addBlobs(inDocs, inXPath);
    }

    // The original usecase actually :-)
    public PDFMerge(String[] inDocIDs, String inXPath, CoreSession inSession) {
        addBlobs(inDocIDs, inXPath, inSession);
    }

    public void addBlob(Blob inBlob) {
        if (inBlob != null) {
            blobs.add(inBlob);
        }
    }

    public void addBlobs(BlobList inBlobs) {
        inBlobs.forEach(this::addBlob);
    }

    public void addBlob(DocumentModel inDoc, String inXPath) {
        if (inXPath == null || inXPath.isEmpty()) {
            inXPath = "file:content";
        }
        addBlob((Blob) inDoc.getPropertyValue(inXPath));
    }

    public void addBlobs(DocumentModelList inDocs, String inXPath) {
        for (DocumentModel doc : inDocs) {
            addBlob(doc, inXPath);
        }
    }

    public void addBlobs(String[] inDocIDs, String inXPath, CoreSession inSession) {
        for (String id : inDocIDs) {
            DocumentModel doc = inSession.getDocument(new IdRef(id));
            addBlob(doc, inXPath);
        }
    }

    /**
     * Merge the PDFs.
     *
     * @param inFileName Name of the merged result.
     * @return The Blob embedding the PDF resulting form the merge.
     * @throws COSVisitorException
     * @throws IOException
     */
    public Blob merge(String inFileName) throws IOException {
        return merge(inFileName, null, null, null);
    }

    /**
     * Merge the PDFs. optionnaly, can set the title, subject and author of the resulting PDF.
     * <p>
     * <b>Notice</b> for title, author and subject: If the value is null or "", it is just ignored.
     *
     * @param inFileName Name of the merged result.
     * @param inTitle Title of the resulting PDF.
     * @param inSubject Subject of the resulting PDF.
     * @param inAuthor Author of the resulting PDF.
     * @return The Blob embedding the PDF resulting from the merge.
     * @throws IOException
     * @throws COSVisitorException
     */
    public Blob merge(String inFileName, String inTitle, String inSubject, String inAuthor) throws IOException {
        Blob finalBlob;
        switch (blobs.size()) {
        case 0:
            finalBlob = null;
            break;
        case 1:
            finalBlob = blobs.get(0);
            break;
        default:
            PDFMergerUtility ut = new PDFMergerUtility();
            for (Blob b : blobs) {
                ut.addSource(b.getStream());
            }
            File tempFile = File.createTempFile("mergepdf", ".pdf");
            ut.setDestinationFileName(tempFile.getAbsolutePath());
            ut.mergeDocuments();
            if (inTitle != null || inAuthor != null || inSubject != null) {
                PDDocument finalDoc = PDDocument.load(tempFile);
                PDFUtils.setInfos(finalDoc, inTitle, inSubject, inAuthor);
                finalDoc.save(tempFile);
                finalDoc.close();
            }
            finalBlob = new FileBlob(tempFile);
            Framework.trackFile(tempFile, finalBlob);
            if (inFileName != null && !inFileName.isEmpty()) {
                finalBlob.setFilename(inFileName);
            } else {
                finalBlob.setFilename(blobs.get(0).getFilename());
            }
            finalBlob.setMimeType("application/pdf");
            break;
        }
        return finalBlob;
    }

}
