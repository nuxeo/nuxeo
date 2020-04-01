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
package org.nuxeo.ecm.platform.pdf.operations;

import java.io.IOException;
import org.nuxeo.ecm.automation.core.Constants;
import org.nuxeo.ecm.automation.core.annotations.Operation;
import org.nuxeo.ecm.automation.core.annotations.OperationMethod;
import org.nuxeo.ecm.automation.core.annotations.Param;
import org.nuxeo.ecm.automation.core.collectors.BlobCollector;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.platform.pdf.PDFPageNumbering;
import org.nuxeo.ecm.platform.pdf.PDFPageNumbering.PAGE_NUMBER_POSITION;

/**
 * Add page numbers to the PDF.
 * <p>
 * If the PDF is encrypted, a password is required.
 *
 * @since 8.10
 */
@Operation(id = PDFAddPageNumbersOperation.ID, category = Constants.CAT_CONVERSION, label = "PDF: Add Page Numbers",
    description = "Add the page numbers to the PDF, using the misc parameters. If the PDF is encrypted, a password " +
        "is required.")
public class PDFAddPageNumbersOperation {

    public static final String ID = "PDF.AddPageNumbers";

    @Param(name = "startAtPage", required = false, values = { "1" })
    private long startAtPage = 1;

    @Param(name = "startAtNumber", required = false, values = { "1" })
    private long startAtNumber = 1;

    @Param(name = "position", required = false, widget = Constants.W_OPTION, values = {
        "Bottom right", "Bottom center", "Bottom left", "Top right", "Top center", "Top left" })
    private String position = "Bottom right";

    @Param(name = "fontName", required = false, values = { "Helvetica" })
    private String fontName = "Helvetica";

    @Param(name = "fontSize", required = false, values = { "16" })
    private long fontSize = 16;

    @Param(name = "hex255Color", required = false, values = { "0xffffff" })
    private String hex255Color = "0xffffff";

    @Param(name = "password", required = false)
    protected String password = null;

    @OperationMethod(collector = BlobCollector.class)
    public Blob run(Blob inBlob) throws IOException {
        PAGE_NUMBER_POSITION pos;
        switch (position.toLowerCase()) {
        case "bottom center":
            pos = PAGE_NUMBER_POSITION.BOTTOM_CENTER;
            break;
        case "bottom left":
            pos = PAGE_NUMBER_POSITION.BOTTOM_LEFT;
            break;
        case "top right":
            pos = PAGE_NUMBER_POSITION.TOP_RIGHT;
            break;
        case "top center":
            pos = PAGE_NUMBER_POSITION.TOP_CENTER;
            break;
        case "top left":
            pos = PAGE_NUMBER_POSITION.TOP_LEFT;
            break;
        default:
            pos = PAGE_NUMBER_POSITION.BOTTOM_RIGHT;
            break;
        }
        PDFPageNumbering pn = new PDFPageNumbering(inBlob);
        pn.setPassword(password);
        Blob result = pn.addPageNumbers((int) startAtPage, (int) startAtNumber, fontName, fontSize, hex255Color, pos);
        result.setFilename(inBlob.getFilename());
        return result;
    }

}
