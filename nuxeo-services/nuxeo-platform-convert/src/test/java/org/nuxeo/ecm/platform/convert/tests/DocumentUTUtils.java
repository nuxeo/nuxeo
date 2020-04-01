//
// Copyright (C) 2004-2006 - Mirko Nasato <mirko@artofsolving.com>
//
// This library is free software; you can redistribute it and/or
// modify it under the terms of the GNU Lesser General Public
// License as published by the Free Software Foundation; either
// version 2.1 of the License, or (at your option) any later version.
//
// This library is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
// Lesser General Public License for more details.
// http://www.gnu.org/copyleft/lesser.html
//
// JOOConverter - The Open Source Java/OpenOffice Document Converter
//
package org.nuxeo.ecm.platform.convert.tests;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringWriter;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;

public final class DocumentUTUtils {

    private static final int BYTE_ORDER_MARK_CHAR = 0xFEFF;

    // This is an utility class, but making ctor private somehow confuses
    // Maven Surefire.

    private DocumentUTUtils() {
    }

    /**
     * Extracts the text from a PDF file.
     *
     * @return the document content as plain text
     */
    public static String readPdfText(File pdfFile) throws IOException {
        PDFTextStripper textStripper = new PDFTextStripper();
        PDDocument document = PDDocument.load(pdfFile);
        String text = textStripper.getText(document);
        document.close();
        return text.trim();
    }

    public static String readContent(File file) throws IOException {
        char[] buffer = new char[2048];
        InputStreamReader reader = new InputStreamReader(new FileInputStream(file), "UTF-8");
        StringWriter writer = new StringWriter();
        int length;
        while ((length = reader.read(buffer, 0, 2048)) != -1) {
            writer.write(buffer, 0, length);
        }
        String content = stripByteOrderMarkChar(writer.toString());
        return content.trim();
    }

    private static String stripByteOrderMarkChar(String content) {
        if (content.length() > 0 && content.charAt(0) == BYTE_ORDER_MARK_CHAR) {
            return content.substring(1).trim();
        }
        return content;
    }

}
