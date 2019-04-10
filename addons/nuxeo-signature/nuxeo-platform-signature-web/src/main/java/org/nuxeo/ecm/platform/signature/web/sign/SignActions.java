/*
 * (C) Copyright 2011-2012 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Wojciech Sulejman
 *     Florent Guillaume
 */
package org.nuxeo.ecm.platform.signature.web.sign;

import static org.jboss.seam.international.StatusMessage.Severity.ERROR;
import static org.jboss.seam.international.StatusMessage.Severity.INFO;
import static org.jboss.seam.international.StatusMessage.Severity.WARN;

import java.io.Serializable;
import java.security.Principal;
import java.security.cert.X509Certificate;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.faces.FacesMessages;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.blobholder.BlobHolder;
import org.nuxeo.ecm.core.api.event.DocumentEventCategories;
import org.nuxeo.ecm.core.event.EventProducer;
import org.nuxeo.ecm.core.event.impl.DocumentEventContext;
import org.nuxeo.ecm.platform.signature.api.exception.CertException;
import org.nuxeo.ecm.platform.signature.api.exception.SignException;
import org.nuxeo.ecm.platform.signature.api.pki.CertService;
import org.nuxeo.ecm.platform.signature.api.sign.SignatureService;
import org.nuxeo.ecm.platform.signature.api.sign.SignatureService.SigningDisposition;
import org.nuxeo.ecm.platform.signature.api.sign.SignatureService.StatusWithBlob;
import org.nuxeo.ecm.platform.ui.web.api.NavigationContext;
import org.nuxeo.ecm.platform.usermanager.UserManager;
import org.nuxeo.ecm.webapp.helpers.ResourcesAccessor;
import org.nuxeo.runtime.api.Framework;

/**
 * Document signing actions
 */
@Name("signActions")
@Scope(ScopeType.CONVERSATION)
public class SignActions implements Serializable {

    private static final long serialVersionUID = 1L;

    private static final Log log = LogFactory.getLog(SignActions.class);

    /**
     * If this system property is set to "true", then signature will use PDF/A.
     */
    public static final String SIGNATURE_USE_PDFA_PROP = "org.nuxeo.ecm.signature.pdfa";

    /**
     * Signature disposition for PDF files. Can be "replace", "archive" or "attach".
     */
    public static final String SIGNATURE_DISPOSITION_PDF = "org.nuxeo.ecm.signature.disposition.pdf";

    /**
     * Signature disposition for non-PDF files. Can be "replace", "archive" or "attach".
     */
    public static final String SIGNATURE_DISPOSITION_NOTPDF = "org.nuxeo.ecm.signature.disposition.notpdf";

    public static final String SIGNATURE_ARCHIVE_FILENAME_FORMAT_PROP = "org.nuxeo.ecm.signature.archive.filename.format";

    /** Used with {@link SimpleDateFormat}. */
    public static final String DEFAULT_ARCHIVE_FORMAT = " ('archive' yyyy-MM-dd HH:mm:ss)";

    protected static final String LABEL_SIGN_DOCUMENT_MISSING = "label.sign.document.missing";

    protected static final String NOTIFICATION_SIGN_PROBLEM = "notification.sign.problem";

    protected static final String NOTIFICATION_SIGN_CERTIFICATE_ACCESS_PROBLEM = "notification.sign.certificate.access.problem";

    protected static final String NOTIFICATION_SIGN_SIGNED = "notification.sign.signed";

    public static final String MIME_TYPE_PDF = "application/pdf";

    public static final String DOCUMENT_SIGNED = "documentSigned";

    public static final String DOCUMENT_SIGNED_COMMENT = "PDF signed";

    @In(create = true)
    protected transient SignatureService signatureService;

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

    @In(create = true)
    protected Principal currentUser;

    protected void info(String msg) {
        facesMessages.add(INFO, getMessage(msg));
    }

    protected void warn(String msg) {
        facesMessages.add(WARN, getMessage(msg));
    }

    protected void error(String msg) {
        facesMessages.add(ERROR, getMessage(msg));
    }

    protected String getMessage(String msg) {
        return resourcesAccessor.getMessages().get(msg);
    }

    protected DocumentModel getCurrentUserModel() {
        return userManager.getUserModel(currentUser.getName());
    }

    /**
     * Signs digitally a PDF blob contained in the current document, modifies the document status and updates UI &
     * auditing messages related to signing
     *
     * @param signingReason
     * @param password
     * @throws SignException
     */
    public void signCurrentDoc(String signingReason, String password) {

        DocumentModel currentDoc = navigationContext.getCurrentDocument();
        DocumentModel currentUserModel = getCurrentUserModel();
        StatusWithBlob swb = signatureService.getSigningStatus(currentDoc, currentUserModel);
        if (swb.status == StatusWithBlob.UNSIGNABLE) {
            error(LABEL_SIGN_DOCUMENT_MISSING);
            return;
        }

        Blob originalBlob = currentDoc.getAdapter(BlobHolder.class).getBlob();
        boolean originalIsPdf = MIME_TYPE_PDF.equals(originalBlob.getMimeType());

        // decide if we want PDF/A
        boolean pdfa = getPDFA();

        // decide disposition
        SigningDisposition disposition = getDisposition(originalIsPdf);

        // decide archive filename
        String filename = originalBlob.getFilename();
        String archiveFilename = getArchiveFilename(filename);

        try {
            signatureService.signDocument(currentDoc, currentUserModel, password, signingReason, pdfa, disposition,
                    archiveFilename);
        } catch (CertException e) {
            log.debug("Signing problem: " + e.getMessage(), e);
            error(NOTIFICATION_SIGN_CERTIFICATE_ACCESS_PROBLEM);
            return;
        } catch (SignException e) {
            log.debug("Signing problem: " + e.getMessage(), e);
            error(NOTIFICATION_SIGN_PROBLEM);
            facesMessages.add(ERROR, e.getMessage());
            return;
        }

        // important to save doc now
        navigationContext.saveCurrentDocument();

        // write to the audit log
        Map<String, Serializable> properties = new HashMap<String, Serializable>();
        String comment = DOCUMENT_SIGNED_COMMENT;
        notifyEvent(DOCUMENT_SIGNED, currentDoc, properties, comment);

        // display a signing message
        facesMessages.add(INFO, filename + " " + getMessage(NOTIFICATION_SIGN_SIGNED));
    }

    protected boolean getPDFA() {
        return Framework.isBooleanPropertyTrue(SIGNATURE_USE_PDFA_PROP);
    }

    protected SigningDisposition getDisposition(boolean originalIsPdf) {
        String disp;
        if (originalIsPdf) {
            disp = Framework.getProperty(SIGNATURE_DISPOSITION_PDF, SigningDisposition.ARCHIVE.name());
        } else {
            disp = Framework.getProperty(SIGNATURE_DISPOSITION_NOTPDF, SigningDisposition.ATTACH.name());
        }
        try {
            return Enum.valueOf(SigningDisposition.class, disp.toUpperCase());
        } catch (RuntimeException e) {
            log.warn("Invalid signing disposition: " + disp);
            return SigningDisposition.ATTACH;
        }
    }

    protected String getArchiveFilename(String filename) {
        String format = Framework.getProperty(SIGNATURE_ARCHIVE_FILENAME_FORMAT_PROP, DEFAULT_ARCHIVE_FORMAT);
        return FilenameUtils.getBaseName(filename) + new SimpleDateFormat(format).format(new Date()) + "."
                + FilenameUtils.getExtension(filename);
    }

    /**
     * Gets the signing status for the current document.
     *
     * @return the signing status
     */
    public StatusWithBlob getSigningStatus() {
        DocumentModel currentDoc = navigationContext.getCurrentDocument();
        return signatureService.getSigningStatus(currentDoc, getCurrentUserModel());
    }

    /**
     * Returns info about the certificates contained in the current document.
     */
    public List<X509Certificate> getCertificateList() throws SignException {

        DocumentModel currentDoc = navigationContext.getCurrentDocument();
        if (currentDoc == null) {
            error(LABEL_SIGN_DOCUMENT_MISSING);
            return Collections.emptyList();
        }

        return signatureService.getCertificates(currentDoc);
        // certificate.getSubjectDN()
        // certificate.getIssuerDN()
        // certificate.getNotAfter()
    }

    protected void notifyEvent(String eventId, DocumentModel source, Map<String, Serializable> properties,
            String comment) {
        properties.put(DocumentEventContext.COMMENT_PROPERTY_KEY, comment);
        properties.put(DocumentEventContext.CATEGORY_PROPERTY_KEY, DocumentEventCategories.EVENT_DOCUMENT_CATEGORY);

        DocumentEventContext eventContext = new DocumentEventContext(source.getCoreSession(),
                source.getCoreSession().getPrincipal(), source);

        eventContext.setProperties(properties);

        Framework.getService(EventProducer.class).fireEvent(eventContext.newEvent(eventId));
    }

}
