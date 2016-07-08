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

import java.io.IOException;
import org.nuxeo.ecm.automation.core.Constants;
import org.nuxeo.ecm.automation.core.annotations.Context;
import org.nuxeo.ecm.automation.core.annotations.Operation;
import org.nuxeo.ecm.automation.core.annotations.OperationMethod;
import org.nuxeo.ecm.automation.core.annotations.Param;
import org.nuxeo.ecm.automation.core.collectors.DocumentModelCollector;
import org.nuxeo.ecm.automation.core.util.Properties;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.platform.pdf.PDFInfo;

/**
 * Extracts PDF info to specific fields.
 * <p>
 * If there is no blob or if the blob is not a PDF document, we empty the values.
 * <p>
 * <b>IMPORTANT</b> We don't check if the blob is a PDF or not. If it is not, this will likely lead to PDFBox errors.
 * <p>
 * For the values to use in the properties parameter, see {@link PDFInfo#toHashMap}.
 *
 * @since 8.4
 */
@Operation(id = PDFExtractInfoOperation.ID, category = Constants.CAT_DOCUMENT, label = "PDF: Extract Info",
    description = "Extract the info of the PDF stored in <code>xpath</code> and put it in the fields referenced by " +
        "<code>properties</code>. <code>properties</code> is a <code>key=value</code> list (one key-value pair/line, " +
        "where <code>key</code> is the xpath of the destination field and <code>value</code> is the exact label " +
        "(case sensitive) as returned by the PageExtractor (see this operation documentation). If there is no blob " +
        "or the blob is not a PDF, all the values referenced in <code>properties</code> are cleared (set to empty " +
        "string, 0, ...).")
public class PDFExtractInfoOperation {

    public static final String ID = "PDF.ExtractInfo";

    @Context
    protected CoreSession session;

    @Param(name = "xpath", required = false, values = { "file:content" })
    protected String xpath = "file:content";

    // The map has the xpath as key and the metadata property as value.
    // For example, say we have a custom pdfinfo schema:
    // pdfinfo:title=Title
    // pdfinfo:producer=PDF Producer
    // pdfinfo:mediabox_width=Media box width
    // ...
    @Param(name = "properties", required = false)
    protected Properties properties;

    @Param(name = "save", required = false, values = { "true" })
    protected boolean save = true;

    @OperationMethod(collector = DocumentModelCollector.class)
    public DocumentModel run(DocumentModel inDoc) throws IOException {
        // Get the blob
        // If there is no blob, we empty all the values
        Blob theBlob;
        theBlob = (Blob) inDoc.getPropertyValue(xpath);
        if (theBlob == null || !theBlob.getMimeType().equals("application/pdf")) {
            for (String inXPath : properties.keySet()) {
                inDoc.setPropertyValue(inXPath, "");
            }
            if (save) {
                session.saveDocument(inDoc);
            }
        } else {
            PDFInfo info = new PDFInfo(inDoc);
            inDoc = info.toFields(inDoc, properties, save, session);
        }
        return inDoc;
    }

}
