/*
 * (C) Copyright 2016-2018 Nuxeo (http://nuxeo.com/) and others.
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

import java.util.stream.Collectors;

import org.apache.commons.lang.StringUtils;
import org.nuxeo.ecm.automation.core.Constants;
import org.nuxeo.ecm.automation.core.annotations.Operation;
import org.nuxeo.ecm.automation.core.annotations.OperationMethod;
import org.nuxeo.ecm.automation.core.annotations.Param;
import org.nuxeo.ecm.automation.core.util.BlobList;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.platform.pdf.PDFEncryption;

/**
 * Encrypts the PDF to "readonly" for users. Read only means they can read, copy, and print. They can't assemble,
 * modify, modify annotations, ...
 *
 * @since 8.10
 */
@Operation(id = PDFEncryptReadOnlyOperation.ID, category = Constants.CAT_CONVERSION, label = "PDF: Encrypt Read Only",
    description = "Encrypts the PDF, returning a copy. User can read, print and copy but cannot modify. " +
        "originalOwnerPwd is used if the PDF was originally encrypted. If ownerPwd is empty, use originalOwnerPwd to " +
        "encrypt. If no keyLength is provided, use 128. If the operation is ran on Document(s), xpath lets you " +
        "specificy where to get the blob from (default: file:content).")
public class PDFEncryptReadOnlyOperation {

    public static final String ID = "PDF.EncryptReadOnly";

    @Param(name = "originalOwnerPwd", required = false)
    protected String originalOwnerPwd;

    @Param(name = "ownerPwd", required = false)
    protected String ownerPwd;

    @Param(name = "userPwd", required = false)
    protected String userPwd;

    @Param(name = "keyLength", required = false, widget = Constants.W_OPTION, values = { "40", "128" })
    protected String keyLength = "128";

    @Param(name = "xpath", required = false, values = { "file:content" })
    protected String xpath = "file:content";

    @OperationMethod
    public Blob run(Blob inBlob) {
        PDFEncryption pdfe = new PDFEncryption(inBlob);
        pdfe.setKeyLength(Integer.parseInt(keyLength));
        pdfe.setOriginalOwnerPwd(originalOwnerPwd);
        pdfe.setOwnerPwd(ownerPwd);
        pdfe.setUserPwd(userPwd);
        Blob result = pdfe.encryptReadOnly();
        return result;
    }

    @OperationMethod
    public BlobList run(BlobList inBlobs) {
        return inBlobs.stream().map(this::run).collect(Collectors.toCollection(BlobList::new));
    }

    @OperationMethod
    public Blob run(DocumentModel inDoc) {
        if (StringUtils.isBlank(xpath)) {
            xpath = "file:content";
        }
        Blob result = null;
        Blob content;
        content = (Blob) inDoc.getPropertyValue(xpath);
        if (content != null) {
            result = this.run(content);
        }
        return result;
    }

    @OperationMethod
    public BlobList run(DocumentModelList inDocs) {
        if (StringUtils.isBlank(xpath)) {
            xpath = "file:content";
        }
        return inDocs.stream().map(this::run).collect(Collectors.toCollection(BlobList::new));
    }

}
