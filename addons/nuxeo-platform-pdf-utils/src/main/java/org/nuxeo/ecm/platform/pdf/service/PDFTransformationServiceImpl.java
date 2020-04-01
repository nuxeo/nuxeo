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
 *     Michael Vachette
 */
package org.nuxeo.ecm.platform.pdf.service;


import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.multipdf.Overlay;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.PDResources;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.apache.pdfbox.pdmodel.graphics.state.PDExtendedGraphicsState;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.Blobs;
import org.nuxeo.ecm.core.api.CloseableFile;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.platform.pdf.PDFUtils;
import org.nuxeo.ecm.platform.pdf.service.watermark.WatermarkProperties;
import org.nuxeo.runtime.model.DefaultComponent;

import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * @since 8.10
 */
public class PDFTransformationServiceImpl extends DefaultComponent
        implements PDFTransformationService {

    protected static final Log log = LogFactory.getLog(PDFTransformationServiceImpl.class);

    protected static final String MIME_TYPE = "application/pdf";

    @Override
    public WatermarkProperties getDefaultProperties() {
        return new WatermarkProperties();
    }

    @Override
    public Blob applyTextWatermark(Blob input, String text, WatermarkProperties properties) {

        // Set up the graphic state to handle transparency
        // Define a new extended graphic state
        PDExtendedGraphicsState extendedGraphicsState = new PDExtendedGraphicsState();
        // Set the transparency/opacity
        extendedGraphicsState.setNonStrokingAlphaConstant((float) properties.getAlphaColor());
        COSName transparentStateName = COSName.getPDFName("TransparentState");

        try (PDDocument pdfDoc = PDDocument.load(input.getStream())) {

            PDFont font = PDFUtils.getStandardType1Font(properties.getFontFamily());
            float watermarkWidth = (float) (font.getStringWidth(text) * properties.getFontSize()
                                / 1000f);
            int[] rgb = PDFUtils.hex255ToRGB(properties.getHex255Color());

            for (PDPage page : pdfDoc.getDocumentCatalog().getPages()) {
                PDRectangle pageSize = page.getMediaBox();
                PDResources resources = page.getResources();
                resources.put(transparentStateName, extendedGraphicsState);

                try (PDPageContentStream contentStream =
                             new PDPageContentStream(pdfDoc, page, true, true, true)) {
                    contentStream.beginText();
                    contentStream.setFont(font, (float) properties.getFontSize());
                    contentStream.appendRawCommands("/TransparentState gs\n");
                    contentStream.setNonStrokingColor(rgb[0], rgb[1], rgb[2]);
                    Point2D position = computeTranslationVector(
                            pageSize.getWidth(),watermarkWidth,
                            pageSize.getHeight(),properties.getFontSize(),properties);
                    contentStream.setTextRotation(
                            Math.toRadians(properties.getRotation()),
                            position.getX(),
                            position.getY());
                    contentStream.drawString(text);
                    contentStream.endText();
                }
            }
            return saveInTempFile(pdfDoc);

        } catch (IOException e) {
            throw new NuxeoException(e);
        }
    }

    @Override
    public Blob applyImageWatermark(Blob input, Blob watermark, WatermarkProperties properties) {

        // Set up the graphic state to handle transparency
        // Define a new extended graphic state
        PDExtendedGraphicsState extendedGraphicsState = new PDExtendedGraphicsState();
        // Set the transparency/opacity
        extendedGraphicsState.setNonStrokingAlphaConstant((float) properties.getAlphaColor());
        COSName transparentStateName = COSName.getPDFName("TransparentState");

        try (PDDocument pdfDoc = PDDocument.load(input.getStream()); //
                CloseableFile image = watermark.getCloseableFile()) {
            PDImageXObject ximage = PDImageXObject.createFromFileByContent(image.getFile(), pdfDoc);

            for (PDPage page : pdfDoc.getDocumentCatalog().getPages()) {
                PDRectangle pageSize = page.getMediaBox();
                PDResources resources = page.getResources();
                resources.put(transparentStateName, extendedGraphicsState);

                try (PDPageContentStream contentStream = new PDPageContentStream(pdfDoc, page, true, true)) {
                    contentStream.appendRawCommands("/TransparentState gs\n");
                    contentStream.endMarkedContentSequence();

                    double watermarkWidth = ximage.getWidth()*properties.getScale();
                    double watermarkHeight = ximage.getHeight()*properties.getScale();

                    Point2D position = computeTranslationVector(
                            pageSize.getWidth(),watermarkWidth,
                            pageSize.getHeight(),watermarkHeight,properties);

                    contentStream.drawImage(
                            ximage,
                            (float)position.getX(),
                            (float)position.getY(),
                            (float)watermarkWidth,
                            (float)watermarkHeight);
                }
            }
            return saveInTempFile(pdfDoc);
        } catch (IOException e) {
            throw new NuxeoException(e);
        }
    }

    @Override
    public Blob overlayPDF(Blob input, Blob overlayBlob) {
        try (PDDocument pdfDoc = PDDocument.load(input.getStream());
             PDDocument pdfOverlayDoc = PDDocument.load(overlayBlob.getStream())) {
            Overlay overlay = new Overlay();
            overlay.setInputPDF(pdfDoc);
            overlay.setAllPagesOverlayPDF(pdfOverlayDoc);
            overlay.overlay(new HashMap<Integer, String>());
            return saveInTempFile(pdfDoc);
        } catch (IOException e) {
            throw new NuxeoException(e);
        }
    }

    public  Point2D computeTranslationVector(double pageWidth, double watermarkWidth,
                                               double pageHeight, double watermarkHeight,
                                               WatermarkProperties properties) {
        double xRotationOffset = 0;
        double yRotationOffset = 0;

        if (properties.getRotation() != 0) {
            Rectangle2D rectangle2D =
                    new Rectangle2D.Double(
                            0, -watermarkHeight, watermarkWidth, watermarkHeight);
            AffineTransform at = AffineTransform.getRotateInstance(
                    -Math.toRadians(properties.getRotation()), 0, 0);
            Shape shape = at.createTransformedShape(rectangle2D);
            Rectangle2D rotated = shape.getBounds2D();

            watermarkWidth = rotated.getWidth();
            if (!properties.isInvertX() || properties.isRelativeCoordinates()) {
                xRotationOffset = -rotated.getX();
            } else {
                xRotationOffset = rotated.getX();
            }

            watermarkHeight = rotated.getHeight();
            if (!properties.isInvertY() || properties.isRelativeCoordinates()) {
                yRotationOffset = rotated.getY()+rotated.getHeight();
            } else {
                yRotationOffset = -(rotated.getY()+rotated.getHeight());
            }
        }

        double xTranslation;
        double yTranslation;

        if (properties.isRelativeCoordinates()) {
            xTranslation = (pageWidth - watermarkWidth ) * properties.getxPosition() + xRotationOffset;
            yTranslation = (pageHeight - watermarkHeight ) * properties.getyPosition() + yRotationOffset;
        } else {
            xTranslation = properties.getxPosition() + xRotationOffset;
            yTranslation = properties.getyPosition() + yRotationOffset;
            if (properties.isInvertX()) {
                xTranslation = pageWidth - watermarkWidth - xTranslation;
            }
            if (properties.isInvertY()) {
                yTranslation = pageHeight - watermarkHeight - yTranslation;
            }
        }
        return new Point2D.Double(xTranslation, yTranslation);
    }

    protected Blob saveInTempFile(PDDocument PdfDoc) throws IOException {
        Blob blob = Blobs.createBlobWithExtension(".pdf"); // creates a tracked temporary file
        blob.setMimeType(MIME_TYPE);
        PdfDoc.save(blob.getFile());
        return blob;
    }

}
