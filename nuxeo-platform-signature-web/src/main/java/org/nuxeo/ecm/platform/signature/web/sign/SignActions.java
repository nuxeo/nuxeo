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
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.VersioningOption;
import org.nuxeo.ecm.core.api.blobholder.BlobHolder;
import org.nuxeo.ecm.core.api.event.DocumentEventCategories;
import org.nuxeo.ecm.core.api.impl.blob.FileBlob;
import org.nuxeo.ecm.core.event.Event;
import org.nuxeo.ecm.core.event.EventContext;
import org.nuxeo.ecm.core.event.EventProducer;
import org.nuxeo.ecm.core.event.EventService;
import org.nuxeo.ecm.core.event.impl.DocumentEventContext;
import org.nuxeo.ecm.core.versioning.VersioningService;
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
            facesMessages.add(StatusMessage.Severity.ERROR,
                    resourcesAccessor.getMessages().get(
                            "label.sign.document.missing"));
        }

        BlobHolder blobHolder = (BlobHolder) currentDoc.getAdapter(BlobHolder.class);
        Blob blob;
        try {
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
                    currentDoc.putContextData(
                            VersioningService.VERSIONING_OPTION,
                            VersioningOption.MINOR);
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
                // display a signing message
                facesMessages.add(StatusMessage.Severity.INFO,
                        outputBlob.getFilename()
                                + " "
                                + resourcesAccessor.getMessages().get(
                                        "notification.sign.signed"));

                // add an entry to the audit log
                Map<String, Serializable> properties = new HashMap<String, Serializable>();
                String comment = DOCUMENT_SIGNED_COMMENT;
                notifyEvent(DOCUMENT_SIGNED, currentDoc, properties, comment);
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
            } else if(blob.getLength()==0){
                facesMessages.add("The file is empty");
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
     * current document. The information has the following format:"This document was certified by CN=aaa bbb,OU=IT,O=Nuxeo,C=US. The certificate was issued by E=pdfca@nuxeo.com,C=US,ST=MA,L=Burlington,O=Nuxeo,OU=CA,CN=PDFCA. The certificate is valid till Thu Jan 05 09:31:15 EST 2012"
     * 
     * @return
     * @throws SignException
     * @throws ClientException
     */
    public String getPDFCertificateInfo() throws SignException, ClientException {
        String pdfCertificateInfo = "";

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
            if (pdfCertificates.size() > 0) {
                X509Certificate certificate = pdfCertificates.get(0);
                pdfCertificateInfo = "This document was certified by "
                        + certificate.getSubjectDN()
                        + ". The certificate was issued by "
                        + certificate.getIssuerDN()
                        + ". The certificate will expire on "
                        + certificate.getNotAfter();
            }
        }
        return pdfCertificateInfo;
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
}