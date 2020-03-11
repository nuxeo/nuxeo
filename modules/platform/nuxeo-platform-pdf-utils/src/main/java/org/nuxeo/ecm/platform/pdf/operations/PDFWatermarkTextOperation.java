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
 *     Michael Vachette
 */
package org.nuxeo.ecm.platform.pdf.operations;

import org.nuxeo.ecm.automation.core.Constants;
import org.nuxeo.ecm.automation.core.annotations.Context;
import org.nuxeo.ecm.automation.core.annotations.Operation;
import org.nuxeo.ecm.automation.core.annotations.OperationMethod;
import org.nuxeo.ecm.automation.core.annotations.Param;
import org.nuxeo.ecm.automation.core.collectors.BlobCollector;
import org.nuxeo.ecm.automation.core.util.Properties;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.platform.pdf.service.PDFTransformationService;
import org.nuxeo.ecm.platform.pdf.service.watermark.WatermarkProperties;

/**
 * Return a new blob combining the input PDF and the watermark text given as a parameter.
 * @since 8.10
 */
@Operation(
        id = PDFWatermarkTextOperation.ID,
        category = Constants.CAT_CONVERSION,
        label = "PDF: Watermark with Text",
        description = PDFWatermarkTextOperation.DESCRIPTION)
public class PDFWatermarkTextOperation {

    public static final String ID = "PDF.WatermarkWithText";

    public static final String DESCRIPTION =
        "<p>Return a <em>new</em> blob combining the input PDF and the <code>text</code> text.</p>" +
        "<p>Properties must be one or more of the following (the default if the property is not set):</p>" +
        "<ul>" +
        "<li><code>fontFamily</code> (Helvetica) </li>" +
        "<li><code>fontSize</code> (72)</li>" +
        "<li><code>rotation</code> (0): in&nbsp;counterclockwise degrees</li>" +
        "<li><code>hex255Color</code> (#000000)</li>" +
        "<li><code>alphaColor</code> (0.5) : 0 is full transparency, 1 is solid</li>" +
        "<li><code>xPosition</code> (0) : in pixels from left or between 0 (left) and 1 (right) if relativeCoordinates is set to true</li>" +
        "<li><code>yPosition</code> (0) : in pixels from bottom or between 0 (bottom) and 1 (top) if relativeCoordinates is set to true</li>" +
        "<li><code>invertX</code> (false) : xPosition starts from the right going left</li>" +
        "<li><code>invertY</code> (false) : yPosition starts from the top going down</li>" +
        "<li><code>relativeCoordinates</code> (false)</li>" +
        "</ul>";

    @Param(name = "text", description="The text to use for the watermark")
    protected String text;

    @Param(name = "properties", description="The watermark properties", required = false)
    protected Properties properties = new Properties();

    @Context
    protected PDFTransformationService pdfTransformationService;

    @OperationMethod(collector = BlobCollector.class)
    public Blob run(Blob inBlob) {
        return pdfTransformationService.
                applyTextWatermark(inBlob, text, convertProperties());
    }

    private WatermarkProperties convertProperties() {
        WatermarkProperties watermarkProperties = pdfTransformationService.getDefaultProperties();
        watermarkProperties.updateFromMap(properties);
        return watermarkProperties;
    }

}
