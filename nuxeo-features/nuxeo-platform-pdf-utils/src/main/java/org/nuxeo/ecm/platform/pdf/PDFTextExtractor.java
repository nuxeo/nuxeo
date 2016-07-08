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
 *     Thiabud Arguillere
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

