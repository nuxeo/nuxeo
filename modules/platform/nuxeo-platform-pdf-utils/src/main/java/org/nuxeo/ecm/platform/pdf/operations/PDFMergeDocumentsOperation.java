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
import org.nuxeo.ecm.automation.OperationContext;
import org.nuxeo.ecm.automation.core.Constants;
import org.nuxeo.ecm.automation.core.annotations.Context;
import org.nuxeo.ecm.automation.core.annotations.Operation;
import org.nuxeo.ecm.automation.core.annotations.OperationMethod;
import org.nuxeo.ecm.automation.core.annotations.Param;
import org.nuxeo.ecm.automation.core.util.BlobList;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.platform.pdf.PDFMerge;

/**
 * The input document(s) always is(are) the first PDF(s), and each pdf is read in the <code>xpath</code> field. If
 * <code>xpath</xpath> is not set, it is set to the default value <code>file:content</code>.
 * <p>
 * The operation appends:
 * <ul>
 * <li>First, The blob referenced in the <code>toAppendVarName</code> Context variable.</li>
 * <li>Then, it appends all the blobs stored in the <code>toAppendListVarName</code> Context variable.</li>
 * <li>And last, it appends the blobs stored in the docs whose IDs are passed in <code>toAppendDocIDsVarName</code>,
 * using the <code>xpath</code> parameter.</li>
 * </ul>
 * <p>
 * All variable names are optional: You can pass only <code>toAppendVarName</code>, or <code>toAppendVarName</code> and
 * <code>toAppendDocIDsVarName</code>, or ...
 * <p>
 * Returns the final PDF.
 *
 * @since 8.10
 */
@Operation(id = PDFMergeDocumentsOperation.ID, category = Constants.CAT_CONVERSION,
    label = "PDF: Merge with Document(s)", description = "The input document(s) always is(are) the first PDFs, and " +
    "their PDF is read in the <code>xpath</code> field (but it is ok for the input doc to have no blob). The " +
    "operation appends the blob referenced in the <code>toAppendVarName</code> Context variable. It then appends all " +
    "the blobs stored in the <code>toAppendListVarName</code> Context variable. It then append the blobs stored in " +
    "the docs whose IDs are passed in <code>toAppendDocIDsVarName</code> (the same <code>xpath</code> is used). " +
    "Returns the final PDF.")
public class PDFMergeDocumentsOperation {

    public static final String ID = "PDF.MergeWithDocs";

    @Context
    protected CoreSession session;

    @Context
    protected OperationContext ctx;

    @Param(name = "xpath", required = false, values = { "file:content" })
    protected String xpath = "file:content";

    @Param(name = "toAppendVarName", required = false)
    protected String toAppendVarName = "";

    @Param(name = "toAppendListVarName", required = false)
    protected String toAppendListVarName = "";

    @Param(name = "toAppendDocIDsVarName", required = false)
    protected String toAppendDocIDsVarName = "";

    @Param(name = "fileName", required = false)
    protected String fileName = "";

    @Param(name = "pdfTitle", required = false)
    protected String pdfTitle = "";

    @Param(name = "pdfSubject", required = false)
    protected String pdfSubject = "";

    @Param(name = "pdfAuthor", required = false)
    protected String pdfAuthor = "";

    @OperationMethod
    public Blob run(DocumentModel inDoc) throws NuxeoException {
        PDFMerge pdfm = new PDFMerge(inDoc, xpath);
        return doMerge(pdfm);
    }

    @OperationMethod
    public Blob run(DocumentModelList inDocs) throws NuxeoException {
        PDFMerge pdfm = new PDFMerge(inDocs, xpath);
        return doMerge(pdfm);
    }

    protected Blob doMerge(PDFMerge inMergeTool) throws NuxeoException {
        // Append the single blob
        if (toAppendVarName != null && !toAppendVarName.isEmpty()) {
            inMergeTool.addBlob((Blob) ctx.get(toAppendVarName));
        }
        // Append the blob list
        if (toAppendListVarName != null && !toAppendListVarName.isEmpty()) {
            if (ctx.get(toAppendListVarName) instanceof BlobList) {
                inMergeTool.addBlobs((BlobList) ctx.get(toAppendListVarName));
            } else {
                throw new NuxeoException(ctx.get(toAppendListVarName).getClass() + " is not a Collection");
            }
        }
        // Append a list of Documents via their IDs
        if (toAppendDocIDsVarName != null && !toAppendDocIDsVarName.isEmpty()) {
            if (ctx.get(toAppendDocIDsVarName) instanceof String[]) {
                inMergeTool.addBlobs((String[]) ctx.get(toAppendDocIDsVarName), xpath, session);
            } else {
                throw new NuxeoException(ctx.get(toAppendDocIDsVarName).getClass() + " is not a String[]");
            }
        }
        // Merge
        try {
            return inMergeTool.merge(fileName, pdfTitle, pdfSubject, pdfAuthor);
        } catch (IOException e) {
            throw new NuxeoException(e);
        }
    }

}
