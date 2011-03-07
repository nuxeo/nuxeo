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
import java.security.cert.X509Certificate;
import java.util.List;

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
import org.nuxeo.ecm.core.event.Event;
import org.nuxeo.ecm.core.event.EventContext;
import org.nuxeo.ecm.core.event.EventService;
import org.nuxeo.ecm.core.event.impl.DocumentEventContext;
import org.nuxeo.ecm.platform.signature.api.exception.CertException;
import org.nuxeo.ecm.platform.signature.api.exception.SignException;
import org.nuxeo.ecm.platform.signature.api.pki.CertService;
import org.nuxeo.ecm.platform.signature.api.sign.SignatureService;
import org.nuxeo.ecm.platform.signature.api.user.CUserService;
import org.nuxeo.ecm.platform.signature.api.user.UserInfo;
import org.nuxeo.ecm.platform.ui.web.api.NavigationContext;
import org.nuxeo.ecm.platform.usermanager.UserManager;
import org.nuxeo.ecm.platform.versioning.api.VersioningManager;
import org.nuxeo.runtime.api.Framework;

/**
 * Document signing actions
 *
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
    protected transient CUserService cUserService;

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
     *
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

                KeyStore keystore = cUserService.getUserKeystore(userID,
                        password);

                UserInfo userInfo = cUserService.getUserInfo(user);

                File signedPdf;
                signedPdf = signatureService.signPDF(user, password,
                        signingReason, blob.getStream());

                FileBlob outputBlob = new FileBlob(signedPdf);
                outputBlob.setFilename(blob.getFilename());
                outputBlob.setEncoding(blob.getEncoding());
                outputBlob.setMimeType(blob.getMimeType());
                currentDoc.getAdapter(BlobHolder.class).setBlob(outputBlob);
                CoreSession session = currentDoc.getCoreSession();
                if (currentDoc.isVersionable()) {
                    currentDoc = versioningManager.incrementMinor(currentDoc);
                    currentDoc.putContextData(
                            org.nuxeo.common.collections.ScopeType.REQUEST,
                            VersioningDocument.CREATE_SNAPSHOT_ON_SAVE_KEY,
                            true);
                    currentDoc = session.saveDocument(currentDoc);
                }
                navigationContext.saveCurrentDocument();

                // save the digital signing event to the audit log
                EventContext ctx = new DocumentEventContext(session,
                        session.getPrincipal(), currentDoc);
                Event event = ctx.newEvent("documentSigned"); // auditable
                event.setInline(false);
                event.setImmediate(true);
                Framework.getLocalService(EventService.class).fireEvent(event);

                // and display a signing message
                facesMessages.add(FacesMessage.SEVERITY_INFO,
                        outputBlob.getFilename()
                                + " has been signed with the following reason "
                                + signingReason, null);
            }
        } catch (CertException e) {
            LOG.info("PDF SIGNING PROBLEM. CERTIFICATE ACCESS PROBLEM" + e);
            facesMessages.add(
                    FacesMessage.SEVERITY_ERROR,
                    "Problem accessing your certificate. Make sure your password is correct.",
                    e);
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

    /**
     * Checks whether a document was already signed with an X509 certificate.
     *
     * @return
     * @throws SignException
     * @throws ClientException
     */
    public boolean isPDFSigned() throws SignException, ClientException {
        boolean isSigned = false;

        DocumentModel currentDoc = navigationContext.getCurrentDocument();
        if (currentDoc == null) {
            facesMessages.add(FacesMessage.SEVERITY_ERROR,
                    "Current document missing");
        }

        BlobHolder blobHolder = (BlobHolder) currentDoc.getAdapter(BlobHolder.class);
        Blob blob = null;
        blob = blobHolder.getBlob();
        if (blob == null) {
            facesMessages.add(FacesMessage.SEVERITY_ERROR,
                    "Your document does not contain any attachments", null);
        } else {
            if (blob.getMimeType() == null) {
                facesMessages.add(FacesMessage.SEVERITY_INFO,
                        "Your attachment might not be a PDF", null);
            } else if (!blob.getMimeType().equals("application/pdf")) {
                facesMessages.add(FacesMessage.SEVERITY_ERROR,
                        "The attachment must be a PDF", null);
            } else {
                List<X509Certificate> pdfCertificates;
                try {
                    pdfCertificates = signatureService.getPDFCertificates(blob.getStream());
                } catch (IOException e) {
                    throw new SignException(e);
                }
                if (pdfCertificates.size() > 0) {
                    isSigned = true;
                }
            }
        }
        return isSigned;
    }

    /**
     * Returns a basic textual description of the certificate contained in the
     * current document. The information has the following format:
     * "This document was certified by CN=aaa bbb,OU=IT,O=Nuxeo,C=US. The certificate was issued by E=pdfca@nuxeo.com,C=US,ST=MA,L=Burlington,O=Nuxeo,OU=CA,CN=PDFCA. The certificate is valid till Thu Jan 05 09:31:15 EST 2012"
     *
     * @return
     * @throws SignException
     * @throws ClientException
     */
    public String getPDFCertificateInfo() throws SignException, ClientException {
        boolean isSigned = false;
        String pdfCertificateInfo = "";

        DocumentModel currentDoc = navigationContext.getCurrentDocument();
        if (currentDoc == null) {
            facesMessages.add(FacesMessage.SEVERITY_ERROR,
                    "Current document missing");
        }

        BlobHolder blobHolder = (BlobHolder) currentDoc.getAdapter(BlobHolder.class);
        Blob blob = null;
        blob = blobHolder.getBlob();
        if (blob == null) {
            facesMessages.add(FacesMessage.SEVERITY_ERROR,
                    "Your document does not contain any attachments", null);
        } else {
            if (!blob.getMimeType().equals("application/pdf")) {
                facesMessages.add(FacesMessage.SEVERITY_ERROR,
                        "The attachment must be a PDF", null);
            }
            List<X509Certificate> pdfCertificates;
            try {
                pdfCertificates = signatureService.getPDFCertificates(blob.getStream());
            } catch (IOException e) {
                throw new SignException(e);
            }
            if (pdfCertificates.size() > 0) {
                X509Certificate certificate = pdfCertificates.get(0);
                pdfCertificateInfo = "This document was certified by "
                        + certificate.getSubjectDN()
                        + ". The certificate was issued by "
                        + certificate.getIssuerDN()
                        + ". The certificate is valid till "
                        + certificate.getNotAfter();
            }
        }
        return pdfCertificateInfo;
    }
}