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

package org.nuxeo.ecm.platform.pdf.operations;

import org.nuxeo.ecm.automation.core.Constants;
import org.nuxeo.ecm.automation.core.annotations.Operation;
import org.nuxeo.ecm.automation.core.annotations.OperationMethod;
import org.nuxeo.ecm.automation.core.annotations.Param;
import org.nuxeo.ecm.automation.core.collectors.BlobCollector;
import org.nuxeo.ecm.automation.core.util.Properties;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.platform.pdf.PDFWatermarking;

/**
 * Returns a <i>new</i> blob combining the input PDF and the <code>watermark</code> text, using the different
 * <code>properties</code> (default values apply). Notice that <code>xPosition</code> and <code>yPosition</code> start
 * at the bottom-left corner. If <code>watermark</code> is empty, a simple copy of the input blob is returned.
 * <p>
 * Properties must be one or more of the following (in parenthesis is the default value if the property is not used):
 * <code>fontFamily</code> (Helvetica), <code>fontSize</code> (36), <code>textRotation</code> (0),
 * <code>hex255Color</code> (#000000), <code>alphaColor</code> (0.5), <code>xPosition</code> (0),
 * <code>yPosition</code> (0), <code>invertY</code> (false).
 *
 * @since 8.4
 */
@Operation(id = PDFWatermarkTextOperation.ID, category = Constants.CAT_CONVERSION, label = "PDF: Watermark with Text",
    description = "Returns a <i>new</i> blob combining the input PDF and the <code>watermark</code> text, using the " +
        "different properties. Properties must be one or more of the following (in parenthesis is the default value " +
        "if the property is not used): <code>fontFamily</code> (Helvetica), <code>fontSize</code> (36), " +
        "<code>textRotation</code> (0), <code>hex255Color</code> (#000000), <code>alphaColor</code> (0.5), " +
        "<code>xPosition</code> (0), <code>yPosition</code> (0), <code>invertY</code> (false). " +
        "<code>xPosition</code> and <code>yPosition</code> start at the <i>bottom-left</i> corner of the page. " +
        "If <code>watermark</code> is empty, a simple copy of the input blob is returned.")
public class PDFWatermarkTextOperation {

    public static final String ID = "PDF.WatermarkWithText";

    @Param(name = "watermark")
    protected String watermark = "";

    @Param(name = "properties", required = false)
    protected Properties properties;

    @OperationMethod(collector = BlobCollector.class)
    public Blob run(Blob inBlob) {
        PDFWatermarking pdfw = new PDFWatermarking(inBlob);
        pdfw.setText(watermark).setProperties(properties);
        return pdfw.watermark();
    }

}
