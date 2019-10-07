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
 *     Fred Vadon
 *     Miguel Nixo
 */
package org.nuxeo.ecm.platform.pdf.operations;

import java.io.IOException;

import org.apache.commons.lang3.StringUtils;
import org.nuxeo.ecm.automation.core.Constants;
import org.nuxeo.ecm.automation.core.annotations.Context;
import org.nuxeo.ecm.automation.core.annotations.Operation;
import org.nuxeo.ecm.automation.core.annotations.OperationMethod;
import org.nuxeo.ecm.automation.core.annotations.Param;
import org.nuxeo.ecm.automation.core.collectors.DocumentModelCollector;
import org.nuxeo.ecm.automation.core.util.DocumentHelper;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.platform.pdf.PDFTextExtractor;

/**
 * Extracts raw text from a PDF.
 * <p>
 * If the PDF is encrypted, a password is required.
 *
 * @since 8.10
 */
@Operation(id = PDFExtractTextOperation.ID, category = Constants.CAT_DOCUMENT, label = "PDF: Extract Text", description = "Extracts raw text from a PDF."
        + " If the PDF is encrypted, a password is required."
        + " pdfxpath is the xpath of the blob (default to file:content)."
        + " The extracted text is set in the targetxpath property of the input document, which is saved if save is true."
        + " If patterntofind is not provided, extracts all the text it can, else it extracts only the line where the pattern is found."
        + " If patterntofind is provided and removepatternfromresult is true, the line is returned without the pattern.")
public class PDFExtractTextOperation {

    public static final String ID = "PDF.ExtractText";

    @Context
    protected CoreSession session;

    @Param(name = "pdfxpath", required = false)
    protected String pdfxpath = "file:content";

    @Param(name = "save", required = false)
    protected boolean save = false;

    @Param(name = "targetxpath", required = false)
    protected String targetxpath;

    @Param(name = "patterntofind", required = false)
    protected String patterntofind;

    @Param(name = "removepatternfromresult", required = false)
    protected boolean removepatternfromresult = false;

    @Param(name = "password", required = false)
    protected String password = null;

    @OperationMethod(collector = DocumentModelCollector.class)
    public DocumentModel run(DocumentModel input) throws IOException {
        PDFTextExtractor textExtractor = new PDFTextExtractor(input, pdfxpath);
        textExtractor.setPassword(password);
        String extractedText;
        if (StringUtils.isBlank(patterntofind)) {
            extractedText = textExtractor.getAllExtractedLines();
        } else if (removepatternfromresult) {
            extractedText = textExtractor.extractLastPartOfLine(patterntofind);
        } else {
            extractedText = textExtractor.extractLineOf(patterntofind);
        }
        if (extractedText != null) {
            input.setPropertyValue(targetxpath, extractedText);
        } else {
            DocumentHelper.removeProperty(input, targetxpath);
        }
        if (save) {
            input = session.saveDocument(input);
        }
        return input;
    }

}
