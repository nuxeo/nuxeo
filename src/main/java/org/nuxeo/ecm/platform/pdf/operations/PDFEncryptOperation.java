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

import java.util.Map.Entry;
import java.util.stream.Collectors;

import org.apache.commons.lang.StringUtils;
import org.apache.pdfbox.pdmodel.encryption.AccessPermission;
import org.nuxeo.ecm.automation.core.Constants;
import org.nuxeo.ecm.automation.core.annotations.Operation;
import org.nuxeo.ecm.automation.core.annotations.OperationMethod;
import org.nuxeo.ecm.automation.core.annotations.Param;
import org.nuxeo.ecm.automation.core.util.BlobList;
import org.nuxeo.ecm.automation.core.util.Properties;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.platform.pdf.PDFEncryption;

/**
 * Encrypts the PDF with the given permissions.
 *
 * @since 8.10
 */
@Operation(id = PDFEncryptOperation.ID, category = Constants.CAT_CONVERSION, label = "PDF: Encrypt", //
description = "Encrypts the PDF with the given permissions, returning a copy. Permissions are print, modify, "
        + "copy, modifyAnnot, fillForms, extractForAccessibility, assemble and printDegraded. Any missing permission "
        + "is set to false (values are true or false, assemble=true for example). originalOwnerPwd is used if the PDF "
        + "was originally encrypted. If no keyLength is provided, use 128. If the operation is ran on Document(s), "
        + "xpath lets you specificy where to get the blob from (default: file:content).")
public class PDFEncryptOperation {

    public static final String ID = "PDF.Encrypt";

    @Param(name = "originalOwnerPwd")
    private String originalOwnerPwd;

    @Param(name = "ownerPwd")
    private String ownerPwd;

    @Param(name = "userPwd")
    private String userPwd;

    @Param(name = "keyLength", required = false, widget = Constants.W_OPTION, values = { "40", "128" })
    private String keyLength = "128";

    @Param(name = "xpath", required = false, values = { "file:content" })
    protected String xpath = "file:content";

    @Param(name = "permissions", required = false)
    protected Properties permissions;

    private AccessPermission computeAccessPermission(Properties properties) {
        AccessPermission ap = new AccessPermission(0);
        if (properties == null) {
            return ap;
        }
        for (Entry<String, String> property : properties.entrySet()) {
            boolean value = Boolean.parseBoolean(property.getValue());
            switch (property.getKey().toLowerCase()) {
            case "print":
                ap.setCanPrint(value);
                break;
            case "modify":
                ap.setCanModify(value);
                break;
            case "copy":
                ap.setCanExtractContent(value);
                break;
            case "modifyannot":
                ap.setCanModifyAnnotations(value);
                break;
            case "fillforms":
                ap.setCanFillInForm(value);
                break;
            case "extractforaccessibility":
                ap.setCanExtractForAccessibility(value);
                break;
            case "assemble":
                ap.setCanAssembleDocument(value);
                break;
            case "printdegraded":
                ap.setCanPrintDegraded(value);
                break;
            }
        }
        return ap;
    }

    @OperationMethod
    public Blob run(Blob inBlob) {
        PDFEncryption pdfe = new PDFEncryption(inBlob);
        pdfe.setKeyLength(Integer.parseInt(keyLength));
        pdfe.setOriginalOwnerPwd(originalOwnerPwd);
        pdfe.setOwnerPwd(ownerPwd);
        pdfe.setUserPwd(userPwd);
        return pdfe.encrypt(computeAccessPermission(permissions));
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
        Blob content = (Blob) inDoc.getPropertyValue(xpath);
        return (content != null) ? this.run(content) : null;
    }

    @OperationMethod
    public BlobList run(DocumentModelList inDocs) {
        if (StringUtils.isBlank(xpath)) {
            xpath = "file:content";
        }
        return inDocs.stream().map(this::run).collect(Collectors.toCollection(BlobList::new));
    }

}
