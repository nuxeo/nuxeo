/*
 * (C) Copyright 2010 Nuxeo SA (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     Wojciech Sulejman
 */
package org.nuxeo.ecm.platform.signature.web.sign;

import java.io.File;
import java.io.Serializable;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.blobholder.BlobHolder;
import org.nuxeo.ecm.core.api.impl.blob.FileBlob;
import org.nuxeo.ecm.platform.signature.api.pki.CertInfo;
import org.nuxeo.ecm.platform.signature.api.sign.SignatureService;
import org.nuxeo.ecm.platform.ui.web.api.NavigationContext;
import org.nuxeo.ecm.platform.versioning.api.VersioningManager;

/**
 * Document signing actions
 *
 * @author <a href="mailto:ws@nuxeo.com">Wojciech Sulejman</a>
 *
 */
@Name("signActions")
@Scope(ScopeType.CONVERSATION)
public class SignActions implements Serializable {

    private static final long serialVersionUID = 2L;

    private static final Log log = LogFactory.getLog(SignActions.class);

    @In(create = true)
    protected transient SignatureService signatureService;

    @In(create = true)
    protected transient NavigationContext navigationContext;

    @In(create = true)
    private transient VersioningManager versioningManager;

    public void signCurrentDoc() throws Exception {
        DocumentModel currentDoc = navigationContext.getCurrentDocument();
        if (currentDoc == null) {
            throw new ClientException(
                    "Document could not be signed. Current document missing");
        }
        BlobHolder blobHolder = (BlobHolder) currentDoc.getAdapter(BlobHolder.class);
        Blob blob = blobHolder.getBlob();
        if (blob == null) {
            throw new ClientException(
                    "The current document does not contain an attachment: "
                            + currentDoc.getName());
        }
        // sign only if it is a PDF
        if (!blob.getMimeType().equals("application/pdf")) {
            throw new ClientException("The attachment must be a PDF");
        }

        File signedPdf = signatureService.signPDF(getCertInfo(),
                blob.getStream());
        FileBlob outputBlob = new FileBlob(signedPdf);
        outputBlob.setFilename(blob.getFilename());
        outputBlob.setEncoding(blob.getEncoding());
        currentDoc.getAdapter(BlobHolder.class).setBlob(outputBlob);
        if (currentDoc.isVersionable()) {
            currentDoc = versioningManager.incrementMajor(currentDoc);
            // doc.putContextData(ScopeType.REQUEST,
            // VersioningDocument.CREATE_SNAPSHOT_ON_SAVE_KEY, true);
        }
        navigationContext.saveCurrentDocument();
        log.info("Document has been signed: " + outputBlob.getFilename());
    }

    protected CertInfo getCertInfo() {
        // TODO Hardcoded for NXP-5532
        CertInfo certInfo = new CertInfo();
        certInfo.setSecurityProviderName("BC");// BouncyCastle
        certInfo.setUserID("100");
        certInfo.setUserName("Wojciech Sulejman");
        certInfo.setUserDN("User DN");
        certInfo.setKeyAlgorithm("RSA");
        certInfo.setNumBits(1024);
        certInfo.setSigningReason("Test from SignActions");
        certInfo.setCertSignatureAlgorithm("SHA256WithRSAEncryption");
        certInfo.setValidMillisBefore(0);
        certInfo.setValidMillisAfter(1000000);
        return certInfo;
    }

}