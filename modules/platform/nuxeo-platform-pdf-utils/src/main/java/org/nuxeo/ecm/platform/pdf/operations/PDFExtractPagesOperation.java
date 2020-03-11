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
import org.nuxeo.ecm.automation.core.annotations.Context;
import org.nuxeo.ecm.automation.core.annotations.Operation;
import org.nuxeo.ecm.automation.core.annotations.OperationMethod;
import org.nuxeo.ecm.automation.core.annotations.Param;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.platform.pdf.PDFPageExtractor;

/**
 * Extract pages from <code>startPage</code> to <code>endPage</code> (inclusive) from the input object. If a Blob is
 * used as input, the <code>xpath</xpath> parameter is not used. <code>title</code>, <code>subject</code> and
 * <code>author</code> are optional.
 * <p>
 * If the PDF is encrypted, a password is required.
 *
 * @since 8.10
 */
@Operation(id = PDFExtractPagesOperation.ID, category = Constants.CAT_CONVERSION, label = "PDF: Extract Pages",
    description = "Extract pages from <code>startPage</code> to <code>endPage</code> (inclusive) from the input " +
        "object. If a Blob is used as input, the <code>xpath</xpath> parameter is not used. <code>title</code>, " +
        "<code>subject</code> and <code>author</code> are optional. If the PDF is encrypted, a password is required.")
public class PDFExtractPagesOperation {

    public static final String ID = "PDF.ExtractPages";

    @Context
    protected CoreSession session;

    @Param(name = "startPage")
    protected long startPage;

    @Param(name = "endPage")
    protected long endPage;

    @Param(name = "fileName", required = false)
    protected String fileName = "";

    @Param(name = "pdfTitle", required = false)
    protected String pdfTitle = "";

    @Param(name = "pdfSubject", required = false)
    protected String pdfSubject;

    @Param(name = "pdfAuthor", required = false)
    protected String pdfAuthor = "";

    @Param(name = "xpath", required = false, values = { "file:content" })
    protected String xpath = "";

    @Param(name = "password", required = false)
    protected String password = null;

    @OperationMethod
    public Blob run(Blob inBlob) {
        PDFPageExtractor pe = new PDFPageExtractor(inBlob);
        pe.setPassword(password);
        return pe.extract((int) startPage, (int) endPage, fileName, pdfTitle, pdfSubject, pdfAuthor);
    }

    @OperationMethod
    public Blob run(DocumentModel inDoc) {
        PDFPageExtractor pe = new PDFPageExtractor(inDoc, xpath);
        pe.setPassword(password);
        return pe.extract((int) startPage, (int) endPage, fileName, pdfTitle, pdfSubject, pdfAuthor);
    }

}
