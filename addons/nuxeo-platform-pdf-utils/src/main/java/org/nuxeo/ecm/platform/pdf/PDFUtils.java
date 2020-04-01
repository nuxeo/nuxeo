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

import static org.apache.pdfbox.pdmodel.font.PDType1Font.COURIER;
import static org.apache.pdfbox.pdmodel.font.PDType1Font.COURIER_BOLD;
import static org.apache.pdfbox.pdmodel.font.PDType1Font.COURIER_BOLD_OBLIQUE;
import static org.apache.pdfbox.pdmodel.font.PDType1Font.COURIER_OBLIQUE;
import static org.apache.pdfbox.pdmodel.font.PDType1Font.HELVETICA;
import static org.apache.pdfbox.pdmodel.font.PDType1Font.HELVETICA_BOLD;
import static org.apache.pdfbox.pdmodel.font.PDType1Font.HELVETICA_BOLD_OBLIQUE;
import static org.apache.pdfbox.pdmodel.font.PDType1Font.HELVETICA_OBLIQUE;
import static org.apache.pdfbox.pdmodel.font.PDType1Font.SYMBOL;
import static org.apache.pdfbox.pdmodel.font.PDType1Font.TIMES_BOLD;
import static org.apache.pdfbox.pdmodel.font.PDType1Font.TIMES_BOLD_ITALIC;
import static org.apache.pdfbox.pdmodel.font.PDType1Font.TIMES_ITALIC;
import static org.apache.pdfbox.pdmodel.font.PDType1Font.TIMES_ROMAN;
import static org.apache.pdfbox.pdmodel.font.PDType1Font.ZAPF_DINGBATS;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDDocumentInformation;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
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

    protected static final Map<String, PDType1Font> STANDARD_14;
    static {
        Map<String, PDType1Font> map = new HashMap<>();
        for (PDType1Font font : Arrays.asList( //
                TIMES_ROMAN, TIMES_BOLD, TIMES_ITALIC, TIMES_BOLD_ITALIC, //
                HELVETICA, HELVETICA_BOLD, HELVETICA_OBLIQUE, HELVETICA_BOLD_OBLIQUE, //
                COURIER, COURIER_BOLD, COURIER_OBLIQUE, COURIER_BOLD_OBLIQUE, //
                SYMBOL, ZAPF_DINGBATS)) {
            map.put(font.getBaseFont(), font);
        }
        STANDARD_14 = Collections.unmodifiableMap(map);
    }

    /**
     * Gets one of the Standard 14 Type 1 Fonts.
     *
     * @since 11.1
     */
    public static PDType1Font getStandardType1Font(String name) {
        return STANDARD_14.get(name);
    }

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
            pdfDoc = PDDocument.load(inBlob.getStream(), inPwd);
        } catch (IOException e) {
            throw new NuxeoException("Failed to load the PDF", e);
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
     */
    public static FileBlob saveInTempFile(PDDocument inPdfDoc) throws IOException {
        return saveInTempFile(inPdfDoc, null);
    }

    public static FileBlob saveInTempFile(PDDocument inPdfDoc, String inFileName) throws IOException {
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