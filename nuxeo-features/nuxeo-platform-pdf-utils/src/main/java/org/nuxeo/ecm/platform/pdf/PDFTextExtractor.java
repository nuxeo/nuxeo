/*
 * (C) Copyright 2014 Nuxeo SA (http://nuxeo.com/) and others.
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

import java.io.IOException;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.util.PDFTextStripper;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.core.api.DocumentModel;

/**
 * Extracts raw text from a PDF.
 *
 * @since 8.4
 */
public class PDFTextExtractor {

    protected Blob pdfBlob;

    protected String password;

    protected String extractedAllAsString = null;

    private static final String END_OF_LINE = "\n";

    public PDFTextExtractor(Blob inBlob) {
        pdfBlob = inBlob;
    }

    /**
     * Constructor with a <code>DocumentModel</code>. The default value for <code>inXPath</code> (if passed
     * <code>null</code> or "") is <code>file:content</code>.
     *
     * @param inDoc Input DocumentModel.
     * @param inXPath Input XPath.
     */
    public PDFTextExtractor(DocumentModel inDoc, String inXPath) {
        if (inXPath == null || inXPath.isEmpty()) {
            inXPath = "file:content";
        }
        pdfBlob = (Blob) inDoc.getPropertyValue(inXPath);
    }

    public String getAllExtractedLines() throws NuxeoException {
        if (extractedAllAsString == null) {
            PDDocument pdfDoc = null;
            try {
                pdfDoc = PDFUtils.load(pdfBlob, password);
                PDFTextStripper stripper = new PDFTextStripper();
                extractedAllAsString = stripper.getText(pdfDoc);
            } catch (IOException e) {
                throw new NuxeoException("Failed to handle the pdf", e);
            } finally {
                PDFUtils.closeSilently(pdfDoc);
            }
        }
        return extractedAllAsString;
    }

    public String extractLineOf(String inString) throws IOException {
        String extractedLine = null;
        int lineBegining = getAllExtractedLines().indexOf(inString);
        int lineEnd;
        if (lineBegining != -1) {
            lineEnd = getAllExtractedLines().indexOf(END_OF_LINE, lineBegining);
            extractedLine = getAllExtractedLines().substring(lineBegining, lineEnd).trim();
        }
        return extractedLine;
    }

    public String extractLastPartOfLine(String string) throws IOException {
        String extractedLine = null;
        extractedLine = extractLineOf(string);
        if (extractedLine != null) {
            extractedLine = extractedLine.substring(string.length(), extractedLine.length());
        }
        return extractedLine;
    }

    public void setPassword(String password) {
        this.password = password;
    }

}

