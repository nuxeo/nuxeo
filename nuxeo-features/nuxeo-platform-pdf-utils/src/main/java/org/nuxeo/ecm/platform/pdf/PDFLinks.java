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
 * @since 8.4
 */
public class PDFLinks {

    protected Blob pdfBlob;

    protected PDDocument pdfDoc;

    protected String password;

    protected ArrayList<LinkInfo> remoteGoToLinks;

    protected ArrayList<LinkInfo> launchLinks;

    protected ArrayList<LinkInfo> uriLinks;

    PDFTextStripperByArea stripper;

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
    protected void loadAndPreflightPdf() throws NuxeoException {
        if (pdfDoc == null) {
            pdfDoc = PDFUtils.load(pdfBlob, password);
            @SuppressWarnings("unchecked")
            List<PDPage> allPages = pdfDoc.getDocumentCatalog().getAllPages();
            try {
                stripper = new PDFTextStripperByArea();
                for (PDPage page : allPages) {
                    List<PDAnnotation> annotations = page.getAnnotations();
                    for (int j = 0; j < annotations.size(); j++) {
                        PDAnnotation annot = annotations.get(j);
                        if (annot instanceof PDAnnotationLink) {
                            PDAnnotationLink link = (PDAnnotationLink) annot;
                            PDRectangle rect = link.getRectangle();
                            // need to reposition link rectangle to match text space
                            float x = rect.getLowerLeftX();
                            float y = rect.getUpperRightY();
                            float width = rect.getWidth();
                            float height = rect.getHeight();
                            int rotation = page.findRotation();
                            if (rotation == 0) {
                                PDRectangle pageSize = page.findMediaBox();
                                y = pageSize.getHeight() - y;
                            } else if (rotation == 90) {
                                // do nothing
                            }
                            Rectangle2D.Float awtRect = new Rectangle2D.Float(x, y, width, height);
                            stripper.addRegion("" + j, awtRect);
                        }
                    }
                }
            } catch (IOException e) {
                throw new NuxeoException("Cannot prefilght and prepare regions", e);
            }
        }
    }

    /**
     * Return all links of type "GoToR" ({@link PDActionRemoteGoTo#SUB_TYPE}).
     *
     * @throws IOException
     */
    public ArrayList<LinkInfo> getRemoteGoToLinks() throws IOException {
        if (remoteGoToLinks == null) {
            loadAndPreflightPdf();
            remoteGoToLinks = parseForLinks(PDActionRemoteGoTo.SUB_TYPE);
        }
        return remoteGoToLinks;
    }

    /**
     * Return all links of type "Launch" ({@link PDActionLaunch#SUB_TYPE}).
     *
     * @throws IOException
     */
    public ArrayList<LinkInfo> getLaunchLinks() throws IOException {
        if (launchLinks == null) {
            loadAndPreflightPdf();
            launchLinks = parseForLinks(PDActionLaunch.SUB_TYPE);
        }
        return launchLinks;
    }

    /**
     * Return all links of type "URI" ({@link PDActionURI#SUB_TYPE}).
     *
     * @throws IOException
     */
    public ArrayList<LinkInfo> getURILinks() throws IOException {
        if (uriLinks == null) {
            loadAndPreflightPdf();
            uriLinks = parseForLinks(PDActionURI.SUB_TYPE);
        }
        return uriLinks;
    }

    @SuppressWarnings("unchecked")
    protected ArrayList<LinkInfo> parseForLinks(String inSubType) throws IOException {
        PDActionRemoteGoTo goTo;
        PDActionLaunch launch;
        PDActionURI uri;
        PDFileSpecification fspec;
        ArrayList<LinkInfo> li = new ArrayList<>();
        List<PDPage> allPages;
        allPages = pdfDoc.getDocumentCatalog().getAllPages();
        int pageNum = 0;
        for (PDPage page : allPages) {
            pageNum += 1;
            stripper.extractRegions(page);
            List<PDAnnotation> annotations = page.getAnnotations();
            for (int j = 0; j < annotations.size(); j++) {
                PDAnnotation annot = annotations.get(j);
                if (annot instanceof PDAnnotationLink) {
                    PDAnnotationLink link = (PDAnnotationLink) annot;
                    PDAction action = link.getAction();
                    if (action.getSubType().equals(inSubType)) {
                        String urlText = stripper.getTextForRegion("" + j);
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
                            li.add(new LinkInfo(pageNum, inSubType, urlText, urlValue));
                        }
                    }
                }
            }
        }
        return li;
    }

    public void setPassword(String password) {
        this.password = password;
    }

}
