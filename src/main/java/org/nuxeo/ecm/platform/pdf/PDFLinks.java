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

import java.awt.geom.Rectangle2D;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.common.filespecification.PDFileSpecification;
import org.apache.pdfbox.pdmodel.interactive.action.type.PDAction;
import org.apache.pdfbox.pdmodel.interactive.action.type.PDActionLaunch;
import org.apache.pdfbox.pdmodel.interactive.action.type.PDActionRemoteGoTo;
import org.apache.pdfbox.pdmodel.interactive.action.type.PDActionURI;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAnnotation;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAnnotationLink;
import org.apache.pdfbox.util.PDFTextStripperByArea;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.NuxeoException;

/**
 * Extract links as list of {@link LinkInfo} from a PDF.
 * <p>
 * In this first version, extracts only the links of type PDActionRemoteGoTo and PDActionLaunch (typically, when a PDF
 * has a <i>relative</i> link to an external PDF).
 * <p>
 * If the PDF is encrypted, a call to <code>setPassword</code> must be done before any attempt to get the links.
 * <p>
 * <b>IMPORTANT</b>
 * <p>
 * Because we can parse the documents several times to get different links, we don't close it after every call
 * (optimization), it is the caller responsibility to explicitly close it to avoid leaks.
 *
 * @since 8.10
 */
public class PDFLinks {

    private Blob pdfBlob;

    private PDDocument pdfDoc;

    private String password;

    private List<LinkInfo> remoteGoToLinks;

    private List<LinkInfo> launchLinks;

    private List<LinkInfo> uriLinks;

    private PDFTextStripperByArea stripper;

    public PDFLinks(Blob inBlob) {
        pdfBlob = inBlob;
    }

    /**
     * To avoid opening/parsing several times the same document, we don't close it after a get...Link() call. It is
     * important that the caller explcitly closes it.
     */
    public void close() {
        PDFUtils.closeSilently(pdfDoc);
        pdfDoc = null;
        pdfBlob = null;
        password = null;
        remoteGoToLinks = null;
        launchLinks = null;
        stripper = null;
    }

    /**
     * Here, we not only open and load the PDF, we also prepare regions to get the text behind the annotation
     * rectangles.
     */
    private void loadAndPreflightPdf() throws NuxeoException {
        if (pdfDoc != null) {
            return;
        }
        pdfDoc = PDFUtils.load(pdfBlob, password);
        try {
            stripper = new PDFTextStripperByArea();
            for (Object pageObject : pdfDoc.getDocumentCatalog().getAllPages()) {
                PDPage page = (PDPage) pageObject;
                List pageAnnotations = page.getAnnotations();
                for (Object annotationObject : pageAnnotations) {
                    PDAnnotation annot = (PDAnnotation) annotationObject;
                    if (!(annot instanceof PDAnnotationLink)) {
                        continue;
                    }
                    PDAnnotationLink link = (PDAnnotationLink) annot;
                    PDRectangle rect = link.getRectangle();
                    // need to reposition link rectangle to match text space
                    float x = rect.getLowerLeftX(), y = rect.getUpperRightY();
                    float width = rect.getWidth(), height = rect.getHeight();
                    int rotation = page.findRotation();
                    if (rotation == 0) {
                        PDRectangle pageSize = page.findMediaBox();
                        y = pageSize.getHeight() - y;
                    }
                    Rectangle2D.Float awtRect = new Rectangle2D.Float(x, y, width, height);
                    stripper.addRegion(String.valueOf(pageAnnotations.indexOf(annot)), awtRect);
                }
            }
        } catch (IOException e) {
            throw new NuxeoException("Cannot preflight and prepare regions", e);
        }
    }

    /**
     * Return all links of type "GoToR" ({@link PDActionRemoteGoTo#SUB_TYPE}).
     */
    public List<LinkInfo> getRemoteGoToLinks() throws IOException {
        if (remoteGoToLinks == null) {
            loadAndPreflightPdf();
            remoteGoToLinks = parseForLinks(PDActionRemoteGoTo.SUB_TYPE);
        }
        return remoteGoToLinks;
    }

    /**
     * Return all links of type "Launch" ({@link PDActionLaunch#SUB_TYPE}).
     */
    public List<LinkInfo> getLaunchLinks() throws IOException {
        if (launchLinks == null) {
            loadAndPreflightPdf();
            launchLinks = parseForLinks(PDActionLaunch.SUB_TYPE);
        }
        return launchLinks;
    }

    /**
     * Return all links of type "URI" ({@link PDActionURI#SUB_TYPE}).
     */
    public List<LinkInfo> getURILinks() throws IOException {
        if (uriLinks == null) {
            loadAndPreflightPdf();
            uriLinks = parseForLinks(PDActionURI.SUB_TYPE);
        }
        return uriLinks;
    }

    private List<LinkInfo> parseForLinks(String inSubType) throws IOException {
        PDActionRemoteGoTo goTo;
        PDActionLaunch launch;
        PDActionURI uri;
        PDFileSpecification fspec;
        List<LinkInfo> li = new ArrayList<>();
        List allPages = pdfDoc.getDocumentCatalog().getAllPages();
        for (Object pageObject : allPages) {
            PDPage page = (PDPage) pageObject;
            stripper.extractRegions(page);
            List<PDAnnotation> annotations = page.getAnnotations();
            for (PDAnnotation annot : annotations) {
                if (!(annot instanceof PDAnnotationLink)) {
                    continue;
                }
                PDAnnotationLink link = (PDAnnotationLink) annot;
                PDAction action = link.getAction();
                if (!action.getSubType().equals(inSubType)) {
                    continue;
                }
                String urlText = stripper.getTextForRegion(String.valueOf(annotations.indexOf(annot)));
                String urlValue = null;
                switch (inSubType) {
                case PDActionRemoteGoTo.SUB_TYPE:
                    goTo = (PDActionRemoteGoTo) action;
                    fspec = goTo.getFile();
                    urlValue = fspec.getFile();
                    break;
                case PDActionLaunch.SUB_TYPE:
                    launch = (PDActionLaunch) action;
                    fspec = launch.getFile();
                    urlValue = fspec.getFile();
                    break;
                case PDActionURI.SUB_TYPE:
                    uri = (PDActionURI) action;
                    urlValue = uri.getURI();
                    break;
                // others...
                }
                if (StringUtils.isNotBlank(urlValue)) {
                    li.add(new LinkInfo(allPages.indexOf(page) + 1, inSubType, urlText, urlValue));
                }
            }
        }
        return li;
    }

    public void setPassword(String password) {
        this.password = password;
    }

}
