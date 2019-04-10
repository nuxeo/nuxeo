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

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.apache.pdfbox.exceptions.CryptographyException;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDDocumentCatalog;
import org.apache.pdfbox.pdmodel.PDDocumentInformation;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.common.PDMetadata;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.encryption.AccessPermission;
import org.apache.pdfbox.pdmodel.encryption.BadSecurityHandlerException;
import org.apache.pdfbox.pdmodel.encryption.StandardDecryptionMaterial;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;

/**
 * The class will parse the info embedded in a PDF, and return them either globally (<code>toHashMap()</code> or
 * <code>toString()</code>) or via individual getters.
 * <p>
 * The PDF is parsed only at first call to <code>run()</code>. Values are cached during first call.
 * <p>
 * About page sizes, see <a href="http://www.prepressure.com/pdf/basics/page-boxes">PDF page boxes</a> for details.
 * Here, we get the info from the first page only. The dimensions are in points. Divide by 72 to get it in inches.
 *
 * @since 8.10
 */
public class PDFInfo {

    private Blob pdfBlob;

    private int numberOfPages = -1;

    private float mediaBoxWidthInPoints = 0.0f;

    private float mediaBoxHeightInPoints = 0.0f;

    private float cropBoxWidthInPoints = 0.0f;

    private float cropBoxHeightInPoints = 0.0f;

    private long fileSize = -1;

    private boolean isEncrypted;

    private boolean doXMP = false;

    private boolean alreadyParsed = false;

    private String password;

    private String author = "";

    private String contentCreator = "";

    private String fileName = "";

    private String keywords = "";

    private String pageLayout = "";

    private String pdfVersion = "";

    private String producer = "";

    private String subject = "";

    private String title;

    private String xmp;

    private Calendar creationDate;

    private Calendar modificationDate;

    private AccessPermission permissions;

    private LinkedHashMap<String, String> cachedMap;

    /**
     * Constructor with a Blob.
     *
     * @param inBlob Input blob.
     */
    public PDFInfo(Blob inBlob) {
        this(inBlob, null);
    }

    /**
     * Constructor for Blob + encrypted PDF.
     *
     * @param inBlob Input blob.
     * @param inPassword If the PDF is encrypted.
     */
    public PDFInfo(Blob inBlob, String inPassword) {
        pdfBlob = inBlob;
        password = inPassword;
        title = "";
    }

    /**
     * Constructor with a DocumentModel. Uses the default <code>file:content</code> xpath to get the blob from the
     * document.
     *
     * @param inDoc Input DocumentModel.
     */
    public PDFInfo(DocumentModel inDoc) {
        this(inDoc, null, null);
    }

    /**
     * Constructor for DocumentModel + encrypted PDF
     * <p>
     * If <inXPath</code> is <code>null</code> or "", it is set to the default
     * <code>file:content</code> value.
     *
     * @param inDoc Input DocumentModel.
     * @param inXPath Input XPath.
     * @param inPassword If the PDF is encrypted.
     */
    public PDFInfo(DocumentModel inDoc, String inXPath, String inPassword) {
        if (inXPath == null || inXPath.isEmpty()) {
            inXPath = "file:content";
        }
        pdfBlob = (Blob) inDoc.getPropertyValue(inXPath);
        password = inPassword;
        title = "";
    }

    /**
     * If set to true, parsing will extract PDF.
     * <p>
     * The value cannot be modified if <code>run()</code> already has been called.
     *
     * @param inValue true to extract XMP.
     */
    public void setParseWithXMP(boolean inValue) {
        if (alreadyParsed && doXMP != inValue) {
            throw new NuxeoException("Value of 'doXML' cannot be modified after the blob has been already parsed.");
        }
        doXMP = inValue;
    }

    private String checkNotNull(String inValue) {
        return inValue == null ? "" : inValue;
    }

    /**
     * After building the object with the correct constructor, and after possibly having set some parsing property
     * (<code>setParseWithXMP()</code>, for example), this method will extract the information from the PDF.
     * <p>
     * After extraction, the info is available through getters: Either all of them (<code>toHashMap()</code> or
     * <code>toString()</code>) or individual info (see all getters).
     *
     * @throws NuxeoException
     */
    public void run() throws NuxeoException {
        // In case the caller calls several time the run() method
        if (alreadyParsed) {
            return;
        }
        fileName = pdfBlob.getFilename();
        File pdfFile = pdfBlob.getFile();
        fileSize = (pdfFile == null) ? -1 : pdfFile.length();
        try (PDDocument pdfDoc = PDDocument.load(pdfBlob.getStream())) {
            isEncrypted = pdfDoc.isEncrypted();
            if (isEncrypted) {
                pdfDoc.openProtection(new StandardDecryptionMaterial(password));
            }
            numberOfPages = pdfDoc.getNumberOfPages();
            PDDocumentCatalog docCatalog = pdfDoc.getDocumentCatalog();
            pageLayout = checkNotNull(docCatalog.getPageLayout());
            pdfVersion = String.valueOf(pdfDoc.getDocument().getVersion());
            PDDocumentInformation docInfo = pdfDoc.getDocumentInformation();
            author = checkNotNull(docInfo.getAuthor());
            contentCreator = checkNotNull(docInfo.getCreator());
            keywords = checkNotNull(docInfo.getKeywords());
            try {
                creationDate = docInfo.getCreationDate();
            } catch (IOException e) {
                creationDate = null;
            }
            try {
                modificationDate = docInfo.getModificationDate();
            } catch (IOException e) {
                modificationDate = null;
            }
            producer = checkNotNull(docInfo.getProducer());
            subject = checkNotNull(docInfo.getSubject());
            title = checkNotNull(docInfo.getTitle());
            permissions = pdfDoc.getCurrentAccessPermission();
            // Getting dimension is a bit tricky
            mediaBoxWidthInPoints = mediaBoxHeightInPoints = cropBoxWidthInPoints = cropBoxHeightInPoints = -1;
            List allPages = docCatalog.getAllPages();
            boolean gotMediaBox = false, gotCropBox = false;
            for (Object pageObject : allPages) {
                PDPage page = (PDPage) pageObject;
                if (page != null) {
                    PDRectangle r = page.findMediaBox();
                    if (r != null) {
                        mediaBoxWidthInPoints = r.getWidth();
                        mediaBoxHeightInPoints = r.getHeight();
                        gotMediaBox = true;
                    }
                    r = page.findCropBox();
                    if (r != null) {
                        cropBoxWidthInPoints = r.getWidth();
                        cropBoxHeightInPoints = r.getHeight();
                        gotCropBox = true;
                    }
                }
                if (gotMediaBox && gotCropBox) {
                    break;
                }
            }
            if (doXMP) {
                xmp = null;
                PDMetadata metadata = docCatalog.getMetadata();
                if (metadata != null) {
                    xmp = "";
                    try (InputStream xmlInputStream = metadata.createInputStream(); //
                            BufferedReader reader = new BufferedReader(new InputStreamReader(xmlInputStream))) {
                        String line;
                        do {
                            line = reader.readLine();
                            if (line != null) {
                                xmp += line + "\n";
                            }
                        } while (line != null);
                    }
                }
            }
            alreadyParsed = true;
        } catch (IOException | BadSecurityHandlerException | CryptographyException e) {
            throw new NuxeoException(e);
        }
    }

    /**
     * Return all and every parsed info in a String <code>HashMap</code>.
     * <p>
     * Possible values are:
     * <ul>
     * <li>File name</li>
     * <li>File size</li>
     * <li>PDF version</li>
     * <li>Page count</li>
     * <li>Page size</li>
     * <li>Page width</li>
     * <li>Page height</li>
     * <li>Page layout</li>
     * <li>Title</li>
     * <li>Author</li>
     * <li>Subject</li>
     * <li>PDF producer</li>
     * <li>Content creator</li>
     * <li>Creation date</li>
     */
    public HashMap<String, String> toHashMap() {
        // Parse if needed
        run();
        if (cachedMap == null) {
            cachedMap = new LinkedHashMap<>();
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            cachedMap.put("File name", fileName);
            cachedMap.put("File size", String.valueOf(fileSize));
            cachedMap.put("PDF version", pdfVersion);
            cachedMap.put("Page count", String.valueOf(numberOfPages));
            cachedMap.put("Page size",
                    String.format(Locale.ENGLISH, "%.1f x %.1f points", mediaBoxWidthInPoints, mediaBoxHeightInPoints));
            cachedMap.put("Page width", String.valueOf(mediaBoxWidthInPoints));
            cachedMap.put("Page height", String.valueOf(mediaBoxHeightInPoints));
            cachedMap.put("Page layout", pageLayout);
            cachedMap.put("Title", title);
            cachedMap.put("Author", author);
            cachedMap.put("Subject", subject);
            cachedMap.put("PDF producer", producer);
            cachedMap.put("Content creator", contentCreator);
            if (creationDate != null) {
                cachedMap.put("Creation date", dateFormat.format(creationDate.getTime()));
            } else {
                cachedMap.put("Creation date", "");
            }
            if (modificationDate != null) {
                cachedMap.put("Modification date", dateFormat.format(modificationDate.getTime()));
            } else {
                cachedMap.put("Modification date", "");
            }
            // "Others"
            cachedMap.put("Encrypted", String.valueOf(isEncrypted));
            cachedMap.put("Keywords", keywords);
            cachedMap.put("Media box width", String.valueOf(mediaBoxWidthInPoints));
            cachedMap.put("Media box height", String.valueOf(mediaBoxHeightInPoints));
            cachedMap.put("Crop box width", String.valueOf(cropBoxWidthInPoints));
            cachedMap.put("Crop box height", String.valueOf(cropBoxHeightInPoints));
            if(permissions != null) {
                cachedMap.put("Can Print", String.valueOf(permissions.canPrint()));
                cachedMap.put("Can Modify", String.valueOf(permissions.canModify()));
                cachedMap.put("Can Extract", String.valueOf(permissions.canExtractContent()));
                cachedMap.put("Can Modify Annotations", String.valueOf(permissions.canModifyAnnotations()));
                cachedMap.put("Can Fill Forms", String.valueOf(permissions.canFillInForm()));
                cachedMap.put("Can Extract for Accessibility", String.valueOf(
                    permissions.canExtractForAccessibility()));
                cachedMap.put("Can Assemble", String.valueOf(permissions.canAssembleDocument()));
                cachedMap.put("Can Print Degraded", String.valueOf(permissions.canPrintDegraded()));
            }
        }
        return cachedMap;
    }

    /**
     * The <code>inMapping</code> map is an HashMap where the key is the xpath of the destination field, and the value
     * is the exact label of a PDF info as returned by <code>toHashMap()</code>. For example:
     * <p>
     * <code><pre>
     * pdfinfo:title=Title
     * pdfinfo:producer=PDF Producer
     * pdfinfo:mediabox_width=Media box width
     * ...
     * </pre></code>
     * <p>
     * If <code>inSave</code> is false, inSession can be null.
     *
     * @param inDoc Input DocumentModel.
     * @param inMapping Input Mapping.
     * @param inSave Whether should save.
     * @param inSession If is saving, should do it in this particular session.
     */
    public DocumentModel toFields(DocumentModel inDoc, HashMap<String, String> inMapping, boolean inSave,
                                  CoreSession inSession) {
        // Parse if needed
        run();
        Map<String, String> values = toHashMap();
        for (String inXPath : inMapping.keySet()) {
            String value = values.get(inMapping.get(inXPath));
            inDoc.setPropertyValue(inXPath, value);
        }
        if (inSave) {
            inDoc = inSession.saveDocument(inDoc);
        }
        return inDoc;
    }

    /**
     * Wrapper for <code>toHashMap().toString()</code>
     */
    @Override
    public String toString() {
        return toHashMap().toString();
    }

    public int getNumberOfPages() {
        return numberOfPages;
    }

    public float getMediaBoxWidthInPoints() {
        return mediaBoxWidthInPoints;
    }

    public float getMediaBoxHeightInPoints() {
        return mediaBoxHeightInPoints;
    }

    public float getCropBoxWidthInPoints() {
        return cropBoxWidthInPoints;
    }

    public float getCropBoxHeightInPoints() {
        return cropBoxHeightInPoints;
    }

    public long getFileSize() {
        return fileSize;
    }

    public boolean isEncrypted() {
        return isEncrypted;
    }

    public String getAuthor() {
        return author;
    }

    public String getContentCreator() {
        return contentCreator;
    }

    public String getFileName() {
        return fileName;
    }

    public String getKeywords() {
        return keywords;
    }

    public String getPageLayout() {
        return pageLayout;
    }

    public String getPdfVersion() {
        return pdfVersion;
    }

    public String getProducer() {
        return producer;
    }

    public String getSubject() {
        return subject;
    }

    public String getTitle() {
        return title;
    }

    public String getXmp() {
        return xmp;
    }

    public Calendar getCreationDate() {
        return creationDate;
    }

    public Calendar getModificationDate() {
        return modificationDate;
    }

    public AccessPermission getPermissions() {
        return permissions;
    }

}
