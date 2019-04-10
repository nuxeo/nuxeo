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
 *     Michael Vachette
 */
package org.nuxeo.ecm.platform.pdf.service;

import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.platform.pdf.service.watermark.WatermarkProperties;

/**
 * @since 8.10
 */
public interface PDFTransformationService {

    /**
     *
     * @return the watermark default properties
     */
    WatermarkProperties getDefaultProperties();


    /**
     * Add a text watermark to the input PDF blob
     *
     * @param input A PDF blob
     * @param text The text to use for the watermark
     * @param properties the properties of the watermark
     * @return a new PDF file
     */
    Blob applyTextWatermark(Blob input, String text, WatermarkProperties properties);

    /**
     * Add an Image watermark to the input PDF blob
     *
     * @param input A PDF blob
     * @param watermark The image to use for the watermark
     * @param properties the properties of the watermark
     * @return a new PDF file
     */
    Blob applyImageWatermark(Blob input, Blob watermark, WatermarkProperties properties);


    /**
     * Overlay a PDF file on top of the input Blob
     *
     * @param input The original PDF file
     * @param overlayBlob The PDF file to overlay on top inBlob
     * @return a new PDF file
     */
    Blob overlayPDF(Blob input, Blob overlayBlob);

}
