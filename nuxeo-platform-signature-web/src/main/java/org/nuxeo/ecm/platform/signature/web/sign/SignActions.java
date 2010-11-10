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
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.security.KeyStore;
import java.security.Principal;
import java.util.HashMap;
import java.util.Map;

import javax.faces.application.FacesMessage;
import javax.faces.context.FacesContext;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.faces.FacesMessages;
import org.nuxeo.common.utils.FileUtils;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.blobholder.BlobHolder;
import org.nuxeo.ecm.core.api.facet.VersioningDocument;
import org.nuxeo.ecm.core.api.impl.blob.FileBlob;
import org.nuxeo.ecm.platform.signature.api.exception.CertException;
import org.nuxeo.ecm.platform.signature.api.pki.CAService;
import org.nuxeo.ecm.platform.signature.api.sign.SignatureService;
import org.nuxeo.ecm.platform.signature.api.user.CNField;
import org.nuxeo.ecm.platform.signature.api.user.UserInfo;
import org.nuxeo.ecm.platform.ui.web.api.NavigationContext;
import org.nuxeo.ecm.platform.usermanager.UserManager;
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
    protected transient CAService caService;

    @In(create = true)
    protected transient NavigationContext navigationContext;

    @In(create = true, required = false)
    protected FacesMessages facesMessages;

    @In(create = true)
    private transient VersioningManager versioningManager;

    @In(create = true)
    protected transient UserManager userManager;

    private final String PASSWORD = "abc";

    String keystorePath = "test-files/keystore.ks";

    File keystoreFile = FileUtils.getResourceFileFromContext(keystorePath);

    InputStream getKeystoreIS() throws Exception {
        return new FileInputStream(keystoreFile);
    }

    KeyStore getKeyStore() throws CertException{
        KeyStore keystore;
        try {
            keystore = caService.getKeyStore(getKeystoreIS(), getUserInfo(), PASSWORD);
        } catch (Exception e) {
             throw new CertException("Keystore retrieval problem: "+e);
        }
        return keystore;
    }

    //TODO fix exception type
    public void signCurrentDoc(String signingReason) throws Exception {
        DocumentModel currentDoc = navigationContext.getCurrentDocument();
        if (currentDoc == null) {
            facesMessages.add(FacesMessage.SEVERITY_ERROR,
                    "Document could not be signed. Current document missing");
        }
        BlobHolder blobHolder = (BlobHolder) currentDoc.getAdapter(BlobHolder.class);
        Blob blob;
        try {
            blob = blobHolder.getBlob();
            if (blob == null) {
                facesMessages.add(FacesMessage.SEVERITY_ERROR,
                        "Your document does not contain any attachments", null);
                log.error("Attachments missing for:" + currentDoc.getName());
            } else {

                if (!blob.getMimeType().equals("application/pdf")) {
                    facesMessages.add(FacesMessage.SEVERITY_ERROR,
                            "The attachment must be a PDF", null);
                    log.error("The attachment is not a pdf for "
                            + currentDoc.getName());
                }
                File signedPdf = signatureService.signPDF(getKeyStore(),
                        getUserInfo(), PASSWORD, signingReason,
                        blob.getStream());
                FileBlob outputBlob = new FileBlob(signedPdf);
                outputBlob.setFilename(blob.getFilename());
                outputBlob.setEncoding(blob.getEncoding());
                currentDoc.getAdapter(BlobHolder.class).setBlob(outputBlob);
                if (currentDoc.isVersionable()) {
                    currentDoc = versioningManager.incrementMinor(currentDoc);
                    currentDoc.putContextData(
                            org.nuxeo.common.collections.ScopeType.REQUEST,
                            VersioningDocument.CREATE_SNAPSHOT_ON_SAVE_KEY,
                            true);
                    CoreSession session = currentDoc.getCoreSession();
                    currentDoc = session.saveDocument(currentDoc);
                }
                navigationContext.saveCurrentDocument();
                facesMessages.add(FacesMessage.SEVERITY_INFO,
                        outputBlob.getFilename()
                                + " has been signed with the following reason "
                                + signingReason, null);
                log.info("Document has been signed: "
                        + outputBlob.getFilename());
            }
        } catch (IOException e) {
            facesMessages.add(FacesMessage.SEVERITY_ERROR,
                    "PDF signing problem ", e);
            log.error("PDF signing problem: ", e);
        } catch (ClientException ce) {
            facesMessages.add(FacesMessage.SEVERITY_ERROR,
                    "PDF signing problem, see the logs ", ce);
            log.error("PDF signing problem: ", ce);
        }
    }

    UserInfo getUserInfo() throws CertException {
        Principal currentUser = FacesContext.getCurrentInstance().getExternalContext().getUserPrincipal();

        Map<CNField, String> userFields;
        userFields = new HashMap<CNField, String>();
        userFields.put(CNField.C, "US");
        userFields.put(CNField.O, "Nuxeo");
        userFields.put(CNField.OU, "IT");
        userFields.put(CNField.CN, "Wojciech Sulejman");
        userFields.put(CNField.Email, "wsulejman@nuxeo.com");
        userFields.put(CNField.UserID, currentUser.getName());
        UserInfo userInfo = new UserInfo(userFields);
        return userInfo;
    }

}