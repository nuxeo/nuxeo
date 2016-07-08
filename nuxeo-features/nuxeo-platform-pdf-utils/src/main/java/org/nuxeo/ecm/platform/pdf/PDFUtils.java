/*
 * (C) Copyright 2014 Nuxeo SA (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl-2.1.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     Thibaut Arguillere
 *     Miguel Nixo
 */
package org.nuxeo.ecm.platform.pdf;

import java.io.File;
import java.io.IOException;
import org.apache.commons.lang.StringUtils;
import org.apache.pdfbox.exceptions.COSVisitorException;
import org.apache.pdfbox.exceptions.CryptographyException;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDDocumentInformation;
import org.apache.pdfbox.pdmodel.encryption.BadSecurityHandlerException;
import org.apache.pdfbox.pdmodel.encryption.StandardDecryptionMaterial;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.Blobs;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.core.api.PathRef;
import org.nuxeo.ecm.core.api.UnrestrictedSessionRunner;
import org.nuxeo.ecm.core.api.impl.blob.FileBlob;

/**
 * Grouping miscellaneous utilities in this class.
 *
 * @since 8.4
 */
public class PDFUtils {

    public static final String DEFAULT_BLOB_XPATH = "file:content";

    public static int[] hex255ToRGB(String inHex) {
        int[] result = { 0, 0, 0 };
        if (inHex != null) {
            inHex = inHex.toLowerCase().replace("#", "").replace("0x", "");
            if (inHex.length() >= 6) {
                for (int i = 0; i < 3; i++) {
                    result[i] = Integer.parseInt(inHex.substring(i * 2, i * 2 + 2), 16);
                }
            }
        }
        return result;
    }

    /**
     * This is just a shortcut. We often load() and openProtection().
     *
     * @param inBlob Input Blob.
     * @param inPwd Input password.
     * @throws NuxeoException
     */
    public static PDDocument load(Blob inBlob, String inPwd) throws NuxeoException {
        PDDocument pdfDoc = null;
        try {
            pdfDoc = PDDocument.load(inBlob.getStream());
            if (pdfDoc.isEncrypted()) {
                pdfDoc.openProtection(new StandardDecryptionMaterial(inPwd));
            }
        } catch (IOException e) {
            throw new NuxeoException("Failed to load the PDF", e);
        } catch (BadSecurityHandlerException | CryptographyException e) {
            throw new NuxeoException("Failed to decrypt the PDF", e);
        }
        return pdfDoc;
    }

    /**
     * Create a temporary PDF file and return a FileBlob built from this file.
     * <p>
     * Mainly a utility used just by this plug-in actually.
     *
     * @param inPdfDoc Input PDF document.
     * @return FileBlob
     * @throws IOException
     * @throws COSVisitorException
     */
    public static FileBlob saveInTempFile(PDDocument inPdfDoc) throws IOException, COSVisitorException {
        // FileBlob result = null;
        // File tempFile = File.createTempFile("nuxeo-pdfutils-", ".pdf");
        // inPdfDoc.save(tempFile);
        // result = new FileBlob(tempFile);
        // result.setMimeType("application/pdf");
        // Framework.trackFile(tempFile, result);
        // return result;
        return saveInTempFile(inPdfDoc, null);
    }

    public static FileBlob saveInTempFile(PDDocument inPdfDoc, String inFileName) throws IOException,
        COSVisitorException {
        Blob result = Blobs.createBlobWithExtension(".pdf");
        File resultFile = result.getFile();
        inPdfDoc.save(result.getFile());
        result.setMimeType("application/pdf");
        if (StringUtils.isNotBlank(inFileName)) {
            result.setFilename(inFileName);
        }
        FileBlob fb = new FileBlob(resultFile);
        fb.setMimeType("application/pdf");
        return fb;
    }

    /**
     * Convenience method: If a parameter is null or "", it is not modified.
     *
     * @param inPdfDoc Input PDF document.
     * @param inTitle Title of the PDF document.
     * @param inSubject Subject of the PDF document.
     * @param inAuthor Author of the PDF document.
     */
    public static void setInfos(PDDocument inPdfDoc, String inTitle, String inSubject, String inAuthor) {
        if (inTitle != null && inTitle.isEmpty()) {
            inTitle = null;
        }
        if (inSubject != null && inSubject.isEmpty()) {
            inSubject = null;
        }
        if (inAuthor != null && inAuthor.isEmpty()) {
            inAuthor = null;
        }
        if (inTitle != null || inAuthor != null || inSubject != null) {
            PDDocumentInformation docInfo = inPdfDoc.getDocumentInformation();
            if (inTitle != null) {
                docInfo.setTitle(inTitle);
            }
            if (inSubject != null) {
                docInfo.setSubject(inSubject);
            }
            if (inAuthor != null) {
                docInfo.setAuthor(inAuthor);
            }
            inPdfDoc.setDocumentInformation(docInfo);
        }
    }

    public static String checkXPath(String inXPath) {
        if (inXPath == null || inXPath.isEmpty()) {
            inXPath = DEFAULT_BLOB_XPATH;
        }
        return inXPath;
    }

    public static void closeSilently(PDDocument... inPdfDocs) {
        for (PDDocument theDoc : inPdfDocs) {
            if (theDoc != null) {
                try {
                    theDoc.close();
                } catch (IOException e) {
                    // Ignore
                }
            }
        }
    }

    public static class UnrestrictedGetBlobForDocumentIdOrPath extends UnrestrictedSessionRunner {

        protected String idOrPath;

        protected DocumentModel doc;

        protected Blob blob;

        protected CoreSession session;

        public UnrestrictedGetBlobForDocumentIdOrPath(CoreSession inSession, String inIdOrPath) {
            super(inSession);
            session = inSession;
            idOrPath = inIdOrPath;
        }

        @Override
        public void run() throws NuxeoException {
            if (idOrPath.startsWith("/")) {
                doc = session.getDocument(new PathRef(idOrPath));
            } else {
                doc = session.getDocument(new IdRef(idOrPath));
            }
            blob = (Blob) doc.getPropertyValue("file:content");
        }

        public Blob getBlob() {
            return blob;
        }
    }

}