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

import org.nuxeo.ecm.automation.core.Constants;
import org.nuxeo.ecm.automation.core.annotations.Operation;
import org.nuxeo.ecm.automation.core.annotations.OperationMethod;
import org.nuxeo.ecm.automation.core.annotations.Param;
import org.nuxeo.ecm.automation.core.util.BlobList;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.platform.pdf.PDFPageExtractor;

/**
 * Converts each page of a PDF into a picture.
 * <p>
 * Returns Blob list of pictures.
 *
 * @since 8.10
 */
@Operation(id = PDFConvertToPicturesOperation.ID, category = Constants.CAT_CONVERSION,
    label = "PDF: Convert to Pictures",
    description = "Convert each page of a PDF into a picture. Returns Blob list of pictures.")
public class PDFConvertToPicturesOperation {

    public static final String ID = "PDF.ConvertToPictures";

    @Param(name = "fileName", required = false)
    protected String fileName = "";

    @Param(name = "xpath", required = false, values = {"file:content"})
    protected String xpath = "";

    @Param(name = "password", required = false)
    protected String password = null;

    @OperationMethod
    public BlobList run(DocumentModel inDoc) {
        PDFPageExtractor pe = new PDFPageExtractor(inDoc, xpath);
        pe.setPassword(password);
        return pe.getPagesAsImages(fileName);
    }

}
