/*
 * (C) Copyright 2011 Nuxeo SA (http://nuxeo.com/) and contributors.
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
import java.security.Principal;
import java.security.cert.X509Certificate;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.faces.context.FacesContext;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.faces.FacesMessages;
import org.jboss.seam.international.StatusMessage;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.ListDiff;
import org.nuxeo.ecm.core.api.blobholder.BlobHolder;
import org.nuxeo.ecm.core.api.event.DocumentEventCategories;
import org.nuxeo.ecm.core.api.impl.blob.FileBlob;
import org.nuxeo.ecm.core.event.EventProducer;
import org.nuxeo.ecm.core.event.impl.DocumentEventContext;
import org.nuxeo.ecm.platform.signature.api.exception.CertException;
import org.nuxeo.ecm.platform.signature.api.exception.SignException;
import org.nuxeo.ecm.platform.signature.api.pki.CertService;
import org.nuxeo.ecm.platform.signature.api.sign.SignatureService;
import org.nuxeo.ecm.platform.signature.api.user.CUserService;
import org.nuxeo.ecm.platform.ui.web.api.NavigationContext;
import org.nuxeo.ecm.platform.usermanager.UserManager;
import org.nuxeo.ecm.webapp.helpers.ResourcesAccessor;
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

    @In(create = true)
    protected ResourcesAccessor resourcesAccessor;

    @In(create = true, required = false)
    protected FacesMessages facesMessages;

    @In(create = true)
    protected transient UserManager userManager;

    public static final String DOCUMENT_SIGNED = "documentSigned";

    public static final String CATEGORY = "Document";

    public static final String DOCUMENT_SIGNED_COMMENT = "PDF signed";

    /**
     * Signs digitally a PDF blob contained in the current document, modifies
     * the document status and updates UI & auditing messages related to signing
     * 
     * @param signingReason
     * @param password
     * @throws SignException
     * @throws ClientException
     */
    public void signCurrentDoc(String signingReason, String password)
            throws SignException, ClientException {

        DocumentModel currentDoc = navigationContext.getCurrentDocument();
        if (currentDoc == null) {
            facesMessages.add(StatusMessage.Severity.ERROR,
                    resourcesAccessor.getMessages().get(
                            "label.sign.document.missing"));
        }

        BlobHolder blobHolder = (BlobHolder) currentDoc.getAdapter(BlobHolder.class);
        Blob originalBlob;
        try {
            originalBlob = blobHolder.getBlob();
            if (originalBlob == null) {
                facesMessages.add(StatusMessage.Severity.ERROR,
                        resourcesAccessor.getMessages().get(
                                "label.sign.attachments.missing"));
            } else {

                if (!originalBlob.getMimeType().equals("application/pdf")) {
                    facesMessages.add(StatusMessage.Severity.ERROR,
                            resourcesAccessor.getMessages().get(
                                    "label.sign.pdf.warning"));
                }

                File signedPdf = signatureService.signPDF(getCurrentUser(),
                        password, signingReason, originalBlob.getStream());
                FileBlob signedBlob = new FileBlob(signedPdf);

                archiveOriginal(currentDoc, originalBlob, signedBlob);

                navigationContext.saveCurrentDocument();

                // write to the audit log
                Map<String, Serializable> properties = new HashMap<String, Serializable>();
                String comment = DOCUMENT_SIGNED_COMMENT;
                notifyEvent(DOCUMENT_SIGNED, currentDoc, properties, comment);

                // display a signing message
                facesMessages.add(StatusMessage.Severity.INFO,
                        signedBlob.getFilename()
                                + " "
                                + resourcesAccessor.getMessages().get(
                                        "notification.sign.signed"));
            }
        } catch (CertException e) {
            LOG.info("PDF SIGNING PROBLEM. CERTIFICATE ACCESS PROBLEM" + e);
            facesMessages.add(StatusMessage.Severity.ERROR,
                    resourcesAccessor.getMessages().get(
                            "notification.sign.certificate.access.problem"));
        } catch (SignException e) {
            LOG.info("PDF signing problem:" + e);
            facesMessages.add(StatusMessage.Severity.ERROR,
                    resourcesAccessor.getMessages().get(
                            "notification.sign.problem"));
        } catch (IOException e) {
            LOG.info("PDF SIGNING PROBLEM:" + e);
            facesMessages.add(StatusMessage.Severity.ERROR,
                    resourcesAccessor.getMessages().get(
                            "notification.sign.problem"));
        } catch (ClientException ce) {
            LOG.info("PDF SIGNING PROBLEM:" + ce);
            facesMessages.add(StatusMessage.Severity.ERROR,
                    resourcesAccessor.getMessages().get(
                            "notification.sign.problem"));
        }
    }

    /**
     * NXP-6726 Moves the original blob to the "files" schema for the signedBlob
     * to become the main file of the document
     * 
     * @param documentToBeSigned
     * @param originalBlob
     * @param signedBlob
     * @return
     * @throws ClientException
     */
    protected DocumentModel archiveOriginal(DocumentModel signedDoc,
            Blob originalBlob, Blob signedBlob) throws ClientException {

        // update the signedBlob fields
        signedBlob.setFilename(originalBlob.getFilename());
        signedBlob.setEncoding(originalBlob.getEncoding());
        signedBlob.setMimeType(originalBlob.getMimeType());

        // add the original blob to the file list in the "files" schema
        // prefix the original file name with user name
        final Map<String, Object> filesEntry = new HashMap<String, Object>();
        String userNamePrefix = getCurrentUser().getProperty("user",
                "firstName")
                + "_" + getCurrentUser().getProperty("user", "lastName");

        DateFormat localeDateFormat = DateFormat.getDateInstance();
        String currentDatePrefix = localeDateFormat.format(new Date());

        filesEntry.put("filename", "Previous-name-before-" + userNamePrefix
                + "-signed-on-" + currentDatePrefix + "-"
                + originalBlob.getFilename());

        ListDiff outputFileList = new ListDiff();
        filesEntry.put("file", originalBlob);
        outputFileList.add(filesEntry);

        // set the grown list to become new "files" schema
        signedDoc.setProperty("files", "files", outputFileList);

        // set the main file to the newly signed blob
        signedDoc.getAdapter(BlobHolder.class).setBlob(signedBlob);
        return signedDoc;
    }

    /**
     * Checks if the current PDF was signed by the current user. Ignores
     * presence of other signatures.
     * 
     * @return
     * @throws SignException
     * @throws ClientException
     */
    public boolean isPDFSignedByCurrentUser() throws SignException,
            ClientException {
        return isPDFSigned(true);
    }

    /**
     * Checks if the current PDF was signed by anyone (if it contains any
     * signatures at all)
     * 
     * @return
     * @throws SignException
     * @throws ClientException
     */
    public boolean isPDFSigned() throws SignException, ClientException {
        return isPDFSigned(false);
    }

    /**
     * Checks whether a document was already signed with an X509 certificate.
     * 
     * @return
     * @throws SignException
     * @throws ClientException
     */
    protected boolean isPDFSigned(boolean checkCurrentUserOnly)
            throws SignException, ClientException {
        boolean isSigned = false;

        DocumentModel currentDoc = navigationContext.getCurrentDocument();
        if (currentDoc == null) {
            facesMessages.add(StatusMessage.Severity.ERROR,
                    resourcesAccessor.getMessages().get(
                            "label.sign.document.missing"));
        }

        BlobHolder blobHolder = (BlobHolder) currentDoc.getAdapter(BlobHolder.class);
        Blob blob = null;
        blob = blobHolder.getBlob();
        if (blob == null) {
            facesMessages.add(StatusMessage.Severity.WARN,
                    "Your document does not contain any attachments");
        } else {
            if (blob.getMimeType() == null) {
                facesMessages.add(StatusMessage.Severity.INFO,
                        resourcesAccessor.getMessages().get(
                                "label.sign.document.mime"));
            } else if (!blob.getMimeType().equals("application/pdf")) {
                facesMessages.add(StatusMessage.Severity.ERROR,
                        resourcesAccessor.getMessages().get(
                                "label.sign.pdf.warning"));
            } else if (blob.getLength() == 0) {
                facesMessages.add("The file is empty");
            } else {
                List<X509Certificate> pdfCertificates;
                try {
                    pdfCertificates = signatureService.getPDFCertificates(blob.getStream());
                } catch (IOException e) {
                    throw new SignException(e);
                }
                if (pdfCertificates.size() > 0) {
                    if (checkCurrentUserOnly) {
                        return containsCurrentPrincipal(pdfCertificates);
                    }
                    isSigned = true;
                }
            }
        }
        return isSigned;
    }

    /**
     * Checks if extracted certificates contain the current context principal
     * use the emailAddress as certificate identity field
     * 
     * @param certificates
     * @return
     */
    protected boolean containsCurrentPrincipal(
            List<X509Certificate> certificates) throws ClientException {
        String currentUserEmail = (String) getCurrentUser().getProperty("user",
                "email");
        if (currentUserEmail == null || currentUserEmail.length() == 0) {
            return false;
        }

        for (X509Certificate certificate : certificates) {
            String certificateEmail = certService.getCertificateEmail(certificate);
            if (currentUserEmail.equals(certificateEmail)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Returns a basic textual description of the certificate contained in the
     * current document. The information has the following format:"This document was certified by CN=aaa bbb,OU=IT,O=Nuxeo,C=US. The certificate was issued by E=pdfca@nuxeo.com,C=US,ST=MA,L=Burlington,O=Nuxeo,OU=CA,CN=PDFCA. The certificate is valid till Thu Jan 05 09:31:15 EST 2012"
     * 
     * @return
     * @throws SignException
     * @throws ClientException
     */
    public List<String> getPDFCertificateList() throws SignException, ClientException {
        List<String> pdfCertificateList = new ArrayList<String>();

        DocumentModel currentDoc = navigationContext.getCurrentDocument();
        if (currentDoc == null) {
            facesMessages.add(StatusMessage.Severity.ERROR,
                    resourcesAccessor.getMessages().get(
                            "label.sign.document.missing"));
        }

        BlobHolder blobHolder = (BlobHolder) currentDoc.getAdapter(BlobHolder.class);
        Blob blob = null;
        blob = blobHolder.getBlob();
        if (blob == null) {
            facesMessages.add(StatusMessage.Severity.ERROR,
                    resourcesAccessor.getMessages().get(
                            "label.sign.attachments.missing"));
        } else {
            if (!blob.getMimeType().equals("application/pdf")) {
                facesMessages.add(StatusMessage.Severity.ERROR,
                        resourcesAccessor.getMessages().get(
                                "label.sign.pdf.warning"));
            }
            List<X509Certificate> pdfCertificates;
            try {
                pdfCertificates = signatureService.getPDFCertificates(blob.getStream());
            } catch (IOException e) {
                throw new SignException(e);
            }
            for(X509Certificate certificate:pdfCertificates){
                pdfCertificateList.add(getCertificateInfo(certificate));
            }
        }
        return pdfCertificateList;
    }

    protected EventProducer getEventProducer() throws ClientException {
        try {
            return Framework.getService(EventProducer.class);
        } catch (Exception e) {
            throw new ClientException(e);
        }
    }

    protected void notifyEvent(String eventId, DocumentModel source,
            Map<String, Serializable> properties, String comment)
            throws ClientException {
        properties.put(DocumentEventContext.COMMENT_PROPERTY_KEY, comment);
        properties.put(DocumentEventContext.CATEGORY_PROPERTY_KEY,
                DocumentEventCategories.EVENT_DOCUMENT_CATEGORY);

        DocumentEventContext eventContext = new DocumentEventContext(
                source.getCoreSession(),
                source.getCoreSession().getPrincipal(), source);

        eventContext.setProperties(properties);

        try {
            getEventProducer().fireEvent(eventContext.newEvent(eventId));
        } catch (ClientException e) {
            LOG.error("Error firing an audit event", e);
        }
    }

    DocumentModel getCurrentUser() throws ClientException {
        FacesContext facesContext = FacesContext.getCurrentInstance();
        Principal currentPrincipal = facesContext.getExternalContext().getUserPrincipal();
        DocumentModel user = userManager.getUserModel(currentPrincipal.getName());
        return user;
    }
    
    String getCertificateInfo(X509Certificate certificate){
        String pdfCertificateInfo = "This certificate belongs to"
            + certificate.getSubjectDN()
            + ". It was issued by "
            + certificate.getIssuerDN()
            + ". It will expire on "
            + certificate.getNotAfter();
        return pdfCertificateInfo;
    }
    
}