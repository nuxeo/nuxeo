/*
 * (C) Copyright 2016-2018 Nuxeo (http://nuxeo.com/) and others.
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
import org.nuxeo.ecm.core.api.impl.blob.FileBlob;

/**
 * Grouping miscellaneous utilities in this class.
 *
 * @since 8.10
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
        PDDocument pdfDoc;
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
        if (StringUtils.isBlank(inXPath)) {
            inXPath = DEFAULT_BLOB_XPATH;
        }
        return inXPath;
    }

    public static void closeSilently(PDDocument... inPdfDocs) {
        for (PDDocument doc : inPdfDocs) {
            if (doc != null) {
                try {
                    doc.close();
                } catch (IOException e) {
                    // Ignore
                }
            }
        }
    }

}