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
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.apache.pdfbox.exceptions.COSVisitorException;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.edit.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.core.api.impl.blob.FileBlob;
import org.nuxeo.runtime.api.Framework;

/**
 * Add page numbers to a PDF, with misc parameters (font, size, color, position).
 *
 * @since 8.10
 */
public class PDFPageNumbering {

    public static float DEFAULT_FONT_SIZE = 16.0f;

    private Blob blob;

    private String password;

    public enum PAGE_NUMBER_POSITION {
        BOTTOM_LEFT, BOTTOM_CENTER, BOTTOM_RIGHT, TOP_LEFT, TOP_CENTER, TOP_RIGHT
    }

    public PDFPageNumbering(Blob inBlob) {
        blob = inBlob;
    }

    public PDFPageNumbering(DocumentModel inDoc, String inXPath) {
        if (StringUtils.isBlank(inXPath)) {
            inXPath = "file:content";
        }
        blob = (Blob) inDoc.getPropertyValue(inXPath);
    }

    /**
     * Adds page numbers and returns a <i>new</i> Blob. Original blob is not modified. This code assumes:
     * <ul>
     * <li>There is no page numbers already (it will always draw the numbers).</li>
     * <li>The PDF is not rotated.</li>
     * <li>Default values apply:
     * <ul>
     * <li><code>inStartAtPage</code> and <code>inStartAtNumber</code> are set to 1 if they are passed as < 1.</li>
     * <li><code>inStartAtPage</code> is set to 1 if it is > number of pages.</li>
     * <li><code>inFontName</code> is set to "Helvetica" if "" or null.</li>
     * <li><code>inFontSize</code> is set to 16 if it is <= 0.</li>
     * <li><code>inHex255Color</code> is set to black if "", null or if its length is < 6. Expected format is
     * <code>0xrrggbb</code>, <code>#rrggbb</code> or just <code>rrggbb</code>.</li>
     * <li><code>inPosition</code> is set to <code>BOTTOM_RIGHT</code> if null.</li>
     * </ul>
     * </li>
     * </ul>
     *
     * @param inStartAtPage Number of the first page to be numbered.
     * @param inStartAtNumber Starting number for the page numbering.
     * @param inFontName Name of the font to be used in the numbering.
     * @param inFontSize Size of the font to be used in the numbering.
     * @param inHex255Color Color of the font to be used in the numbering.
     * @param inPosition Page positioning of the numbering.
     * @return Blob
     */
    public Blob addPageNumbers(int inStartAtPage, int inStartAtNumber, String inFontName, float inFontSize,
            String inHex255Color, PAGE_NUMBER_POSITION inPosition) throws NuxeoException {
        Blob result;
        inStartAtPage = inStartAtPage < 1 ? 1 : inStartAtPage;
        int pageNumber = inStartAtNumber < 1 ? 1 : inStartAtNumber;
        inFontSize = inFontSize <= 0 ? DEFAULT_FONT_SIZE : inFontSize;
        int[] rgb = PDFUtils.hex255ToRGB(inHex255Color);
        try (PDDocument doc = PDFUtils.load(blob, password)) {
            List allPages;
            PDFont font;
            int max;
            if (StringUtils.isBlank(inFontName)) {
                font = PDType1Font.HELVETICA;
            } else {
                font = PDType1Font.getStandardFont(inFontName);
                if (font == null) {
                    font = new PDType1Font(inFontName);
                }
            }
            allPages = doc.getDocumentCatalog().getAllPages();
            max = allPages.size();
            inStartAtPage = inStartAtPage > max ? 1 : inStartAtPage;
            for (int i = inStartAtPage; i <= max; i++) {
                String pageNumAsStr = Integer.toString(pageNumber);
                pageNumber += 1;
                PDPage page = (PDPage) allPages.get(i - 1);
                PDPageContentStream footercontentStream = new PDPageContentStream(doc, page, true, true);
                float stringWidth = font.getStringWidth(pageNumAsStr) * inFontSize / 1000f;
                float stringHeight = font.getFontDescriptor().getFontBoundingBox().getHeight() * inFontSize / 1000;
                PDRectangle pageRect = page.findMediaBox();
                float xMoveAmount, yMoveAmount;
                if (inPosition == null) {
                    inPosition = PAGE_NUMBER_POSITION.BOTTOM_RIGHT;
                }
                switch (inPosition) {
                case BOTTOM_LEFT:
                    xMoveAmount = 10;
                    yMoveAmount = pageRect.getLowerLeftY() + 10;
                    break;
                case BOTTOM_CENTER:
                    xMoveAmount = (pageRect.getUpperRightX() / 2) - (stringWidth / 2);
                    yMoveAmount = pageRect.getLowerLeftY() + 10;
                    break;
                case TOP_LEFT:
                    xMoveAmount = 10;
                    yMoveAmount = pageRect.getHeight() - stringHeight - 10;
                    break;
                case TOP_CENTER:
                    xMoveAmount = (pageRect.getUpperRightX() / 2) - (stringWidth / 2);
                    yMoveAmount = pageRect.getHeight() - stringHeight - 10;
                    break;
                case TOP_RIGHT:
                    xMoveAmount = pageRect.getUpperRightX() - 10 - stringWidth;
                    yMoveAmount = pageRect.getHeight() - stringHeight - 10;
                    break;
                // Bottom-right is the default
                default:
                    xMoveAmount = pageRect.getUpperRightX() - 10 - stringWidth;
                    yMoveAmount = pageRect.getLowerLeftY() + 10;
                    break;
                }
                footercontentStream.beginText();
                footercontentStream.setFont(font, inFontSize);
                footercontentStream.moveTextPositionByAmount(xMoveAmount, yMoveAmount);
                footercontentStream.setNonStrokingColor(rgb[0], rgb[1], rgb[2]);
                footercontentStream.drawString(pageNumAsStr);
                footercontentStream.endText();
                footercontentStream.close();
            }
            File tempFile = File.createTempFile("pdfutils-", ".pdf");
            doc.save(tempFile);
            result = new FileBlob(tempFile);
            Framework.trackFile(tempFile, result);
        } catch (IOException | COSVisitorException e) {
            throw new NuxeoException("Failed to handle the pdf", e);
        }
        return result;
    }

    public void setPassword(String password) {
        this.password = password;
    }

}
