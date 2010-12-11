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
import java.io.IOException;
import java.io.Serializable;
import java.security.KeyStore;
import java.security.Principal;
import javax.faces.application.FacesMessage;
import javax.faces.context.FacesContext;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.faces.FacesMessages;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.blobholder.BlobHolder;
import org.nuxeo.ecm.core.api.facet.VersioningDocument;
import org.nuxeo.ecm.core.api.impl.blob.FileBlob;
import org.nuxeo.ecm.platform.signature.api.exception.CertException;
import org.nuxeo.ecm.platform.signature.api.exception.SignException;
import org.nuxeo.ecm.platform.signature.api.pki.CertService;
import org.nuxeo.ecm.platform.signature.api.sign.SignatureService;
import org.nuxeo.ecm.platform.signature.api.user.CertUserService;
import org.nuxeo.ecm.platform.signature.api.user.UserInfo;
import org.nuxeo.ecm.platform.ui.web.api.NavigationContext;
import org.nuxeo.ecm.platform.usermanager.UserManager;
import org.nuxeo.ecm.platform.versioning.api.VersioningManager;

/**
 * Document signing actions
 * @author <a href="mailto:ws@nuxeo.com">Wojciech Sulejman</a>
 */
@Name("signActions")
@Scope(ScopeType.CONVERSATION)
public class SignActions implements Serializable {

    private static final long serialVersionUID = 2L;

    private static final Log LOG = LogFactory.getLog(SignActions.class);

    @In(create = true)
    protected transient SignatureService signatureService;

    @In(create = true)
    protected transient CertUserService certUserService;

    @In(create = true)
    protected transient CertService certService;

    @In(create = true)
    protected transient NavigationContext navigationContext;

    @In(create = true, required = false)
    protected FacesMessages facesMessages;

    @In(create = true)
    private transient VersioningManager versioningManager;

    @In(create = true)
    protected transient UserManager userManager;

    /**
     * Signs digitally a PDF blob contained in the current document.
     * @param signingReason
     * @param password
     * @throws SignException
     * @throws ClientException
     */
    public void signCurrentDoc(String signingReason, String password)
            throws SignException, ClientException {

        FacesContext facesContext = FacesContext.getCurrentInstance();

        Principal currentUser = facesContext.getExternalContext().getUserPrincipal();

        DocumentModel user = userManager.getUserModel(currentUser.getName());

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
            } else {

                if (!blob.getMimeType().equals("application/pdf")) {
                    facesMessages.add(FacesMessage.SEVERITY_ERROR,
                            "The attachment must be a PDF", null);
                }

                String userID = (String) user.getPropertyValue("user:username");

                KeyStore keystore = certUserService.getUserKeystore(userID,
                        password);

                UserInfo userInfo = certUserService.getUserInfo(user);

                File signedPdf;
                signedPdf = signatureService.signPDF(user, password,
                        signingReason, blob.getStream());

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
            }
        } catch (CertException e) {
            LOG.info("PDF SIGNING PROBLEM. CERTIFICATE ACCESS PROBLEM" + e);
            facesMessages.add(FacesMessage.SEVERITY_ERROR,
                    "Problem accessing your certificate. Make sure your password is correct.", e);
        } catch (SignException e) {
            LOG.info("PDF SIGNING PROBLEM:" + e);
            facesMessages.add(FacesMessage.SEVERITY_ERROR,
                    "PDF signing problem ", e);
        } catch (IOException e) {
            LOG.info("PDF SIGNING PROBLEM:" + e);
            facesMessages.add(FacesMessage.SEVERITY_ERROR,
                    "PDF signing problem ", e);
        } catch (ClientException ce) {
            LOG.info("PDF SIGNING PROBLEM:" + ce);
            facesMessages.add(FacesMessage.SEVERITY_ERROR,
                    "PDF signing problem, see the logs ", ce);
        }
    }
}