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

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;

import javax.imageio.ImageIO;

import org.apache.commons.lang.StringUtils;
import org.apache.pdfbox.exceptions.COSVisitorException;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.util.ImageIOUtil;
import org.apache.pdfbox.util.PageExtractor;
import org.nuxeo.ecm.automation.core.util.BlobList;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.core.api.impl.blob.FileBlob;
import org.nuxeo.runtime.api.Framework;

/**
 * Extract pages from a PDF.
 *
 * @since 8.10
 */
public class PDFPageExtractor {

    private Blob pdfBlob;

    private String password;

    public PDFPageExtractor(Blob inBlob) {
        pdfBlob = inBlob;
    }

    /**
     * Constructor with a <code>DocumentModel</code>. Default value for <code>inXPath</code> (if passed
     * <code>null</code> or ""), is <code>file:content</code>.
     *
     * @param inDoc Input DocumentModel.
     * @param inXPath Input XPath.
     */
    public PDFPageExtractor(DocumentModel inDoc, String inXPath) {
        if (StringUtils.isBlank(inXPath)) {
            inXPath = "file:content";
        }
        pdfBlob = (Blob) inDoc.getPropertyValue(inXPath);
    }

    public Blob extract(int inStartPage, int inEndPage) {
        return extract(inStartPage, inEndPage, null, null, null, null);
    }

    private String getFileName(Blob blob) {
        String originalName = blob.getFilename();
        if (StringUtils.isBlank(originalName)) {
            return "extracted";
        } else {
            int pos = originalName.toLowerCase().lastIndexOf(".pdf");
            if (pos > 0) {
                originalName = originalName.substring(0, pos);
            }
            return originalName;
        }
    }

    /**
     * Return a Blob built from page <code>inStartPage</code> to <code>inEndPage</code> (inclusive).
     * <p>
     * If <code>inEndPage</code> is greater than the number of pages in the source document, it will go to the end of
     * the document. If <code>inStartPage</code> is less than 1, it'll start with page 1. If <code>inStartPage</code> is
     * greater than <code>inEndPage</code> or greater than the number of pages in the source document, a blank document
     * will be returned.
     * <p>
     * If fileName is null or "", if is set to the original name + the page range: mydoc.pdf and pages 10-75 +>
     * mydoc-10-75.pdf.
     * <p>
     * The mimetype is always set to "application/pdf".
     * <p>
     * Can set the title, subject and author of the resulting PDF. <b>Notice</b>: If the value is null or "", it is just
     * ignored.
     *
     * @param inStartPage Number of first page to be included.
     * @param inEndPage Number of the last page to be included.
     * @param inFileName Name of the resulting PDF.
     * @param inTitle Title of the resulting PDF.
     * @param inSubject Subject of the resulting PDF.
     * @param inAuthor Author of the resulting PDF.
     * @return FileBlob
     */
    public Blob extract(int inStartPage, int inEndPage, String inFileName, String inTitle, String inSubject,
                        String inAuthor) throws NuxeoException {
        Blob result;
        PDDocument extracted;
        try (PDDocument pdfDoc = PDFUtils.load(pdfBlob, password)) {
            PageExtractor pe = new PageExtractor(pdfDoc, inStartPage, inEndPage);
            extracted = pe.extract();
            PDFUtils.setInfos(extracted, inTitle, inSubject, inAuthor);
            result = PDFUtils.saveInTempFile(extracted);
            result.setMimeType("application/pdf");
            if (StringUtils.isBlank(inFileName)) {
                inFileName = getFileName(pdfBlob) + "-" + inStartPage + "-" + inEndPage + ".pdf";
            }
            result.setFilename(inFileName);
            extracted.close();
        } catch (IOException | COSVisitorException e) {
            throw new NuxeoException("Failed to extract the pages", e);
        }
        return result;
    }

    public BlobList getPagesAsImages(String inFileName) throws NuxeoException {
        ImageIO.scanForPlugins();
        BlobList results = new BlobList();
        String resultFileName;
        // Use file name parameter if passed, otherwise use original file name.
        if (StringUtils.isBlank(inFileName)) {
            inFileName = getFileName(pdfBlob) + ".pdf";
        }
        try (PDDocument pdfDoc = PDFUtils.load(pdfBlob, password)) {
            // Get all PDF pages.
            List pages = pdfDoc.getDocumentCatalog().getAllPages();
            // Convert each page to PNG.
            for (Object pageObject : pages) {
                PDPage page = (PDPage) pageObject;
                resultFileName = inFileName + "-" + (pages.indexOf(page) + 1);
                BufferedImage bim = page.convertToImage(BufferedImage.TYPE_INT_RGB, 300);
                File resultFile = Framework.createTempFile(resultFileName, ".png");
                FileOutputStream resultFileStream = new FileOutputStream(resultFile);
                ImageIOUtil.writeImage(bim, "png", resultFileStream, 300);
                // Convert each PNG to Nuxeo Blob.
                FileBlob result = new FileBlob(resultFile);
                result.setFilename(resultFileName + ".png");
                result.setMimeType("picture/png");
                // Add to BlobList.
                results.add(result);
                Framework.trackFile(resultFile, result);
            }
            pdfDoc.close();
        } catch (IOException e) {
            throw new NuxeoException("Failed to extract the pages", e);
        }
        return results;
    }

    public void setPassword(String password) {
        this.password = password;
    }

}
