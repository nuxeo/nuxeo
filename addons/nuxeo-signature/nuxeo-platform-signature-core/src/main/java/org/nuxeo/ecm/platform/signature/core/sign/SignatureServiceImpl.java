/*
 * (C) Copyright 2013-2018 Nuxeo (http://nuxeo.com/) and others.
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
 *     Vladimir Pasquier <vpasquier@nuxeo.com>
 */
package org.nuxeo.ecm.platform.signature.core.sign;

import static org.nuxeo.ecm.platform.signature.api.sign.SignatureService.StatusWithBlob.SIGNED_CURRENT;
import static org.nuxeo.ecm.platform.signature.api.sign.SignatureService.StatusWithBlob.SIGNED_OTHER;
import static org.nuxeo.ecm.platform.signature.api.sign.SignatureService.StatusWithBlob.UNSIGNABLE;
import static org.nuxeo.ecm.platform.signature.api.sign.SignatureService.StatusWithBlob.UNSIGNED;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.Serializable;
import java.security.KeyPair;
import java.security.KeyStore;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.Blobs;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.ListDiff;
import org.nuxeo.ecm.core.api.blobholder.BlobHolder;
import org.nuxeo.ecm.core.api.blobholder.DocumentBlobHolder;
import org.nuxeo.ecm.core.api.blobholder.SimpleBlobHolder;
import org.nuxeo.ecm.core.convert.api.ConversionException;
import org.nuxeo.ecm.core.convert.api.ConversionService;
import org.nuxeo.ecm.platform.signature.api.exception.AlreadySignedException;
import org.nuxeo.ecm.platform.signature.api.exception.CertException;
import org.nuxeo.ecm.platform.signature.api.exception.SignException;
import org.nuxeo.ecm.platform.signature.api.pki.CertService;
import org.nuxeo.ecm.platform.signature.api.sign.SignatureAppearanceFactory;
import org.nuxeo.ecm.platform.signature.api.sign.SignatureLayout;
import org.nuxeo.ecm.platform.signature.api.sign.SignatureService;
import org.nuxeo.ecm.platform.signature.api.user.AliasType;
import org.nuxeo.ecm.platform.signature.api.user.AliasWrapper;
import org.nuxeo.ecm.platform.signature.api.user.CUserService;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.model.ComponentInstance;
import org.nuxeo.runtime.model.DefaultComponent;

import com.lowagie.text.DocumentException;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.AcroFields;
import com.lowagie.text.pdf.PdfPKCS7;
import com.lowagie.text.pdf.PdfReader;
import com.lowagie.text.pdf.PdfSignatureAppearance;
import com.lowagie.text.pdf.PdfStamper;

/**
 * Base implementation for the signature service (also a Nuxeo component).
 * <p>
 * The main document is signed. If it's not already a PDF, then a PDF conversion is done.
 * <p>
 * Once signed, it can replace the main document or be stored as the first attachment. If replacing the main document,
 * an archive of the original can be kept.
 * <p>
 * <ul>
 * <li>
 */
public class SignatureServiceImpl extends DefaultComponent implements SignatureService {

    private static final Log log = LogFactory.getLog(SignatureServiceImpl.class);

    protected static final int SIGNATURE_FIELD_HEIGHT = 50;

    protected static final int SIGNATURE_FIELD_WIDTH = 150;

    protected static final int SIGNATURE_MARGIN = 10;

    protected static final int PAGE_TO_SIGN = 1;

    protected static final String XP_SIGNATURE = "signature";

    protected static final String ALREADY_SIGNED_BY = "This document has already been signed by ";

    protected static final String MIME_TYPE_PDF = "application/pdf";

    /** From JODBasedConverter */
    protected static final String PDFA1_PARAM = "PDF/A-1";

    protected static final String FILE_CONTENT = "file:content";

    protected static final String FILES_FILES = "files:files";

    protected static final String FILES_FILE = "file";

    protected static final String USER_EMAIL = "user:email";

    protected final Map<String, SignatureDescriptor> signatureRegistryMap;

    public SignatureServiceImpl() {
        signatureRegistryMap = new HashMap<>();
    }

    @Override
    public void registerContribution(Object contribution, String extensionPoint, ComponentInstance contributor) {
        if (XP_SIGNATURE.equals(extensionPoint)) {
            SignatureDescriptor signatureDescriptor = (SignatureDescriptor) contribution;
            if (!signatureDescriptor.getRemoveExtension()) {
                signatureRegistryMap.put(signatureDescriptor.getId(), signatureDescriptor);
            } else {
                signatureRegistryMap.remove(signatureDescriptor.getId());
            }
        }
    }

    @Override
    public void unregisterContribution(Object contribution, String extensionPoint, ComponentInstance contributor) {
        if (XP_SIGNATURE.equals(extensionPoint)) {
            SignatureDescriptor signatureDescriptor = (SignatureDescriptor) contribution;
            if (!signatureDescriptor.getRemoveExtension()) {
                signatureRegistryMap.remove(signatureDescriptor.getId());
            }
        }
    }

    //
    // ----- SignatureService -----
    //

    @Override
    public StatusWithBlob getSigningStatus(DocumentModel doc, DocumentModel user) {
        if (doc == null) {
            return new StatusWithBlob(UNSIGNABLE, null, null, null);
        }
        StatusWithBlob blobAndStatus = getSignedPdfBlobAndStatus(doc, user);
        if (blobAndStatus != null) {
            return blobAndStatus;
        }
        BlobHolder mbh = doc.getAdapter(BlobHolder.class);
        Blob blob;
        if (mbh == null || (blob = mbh.getBlob()) == null) {
            return new StatusWithBlob(UNSIGNABLE, null, null, null);
        }
        return new StatusWithBlob(UNSIGNED, blob, mbh, FILE_CONTENT);
    }

    protected int getSigningStatus(Blob pdfBlob, DocumentModel user) {
        if (pdfBlob == null) {
            return UNSIGNED;
        }
        List<X509Certificate> certificates = getCertificates(pdfBlob);
        if (certificates.isEmpty()) {
            return UNSIGNED;
        }
        if (user == null) {
            return SIGNED_OTHER;
        }
        String email = (String) user.getPropertyValue(USER_EMAIL);
        if (StringUtils.isEmpty(email)) {
            return SIGNED_OTHER;
        }
        CertService certService = Framework.getService(CertService.class);
        for (X509Certificate certificate : certificates) {
            String certEmail;
            try {
                certEmail = certService.getCertificateEmail(certificate);
            } catch (CertException e) {
                continue;
            }
            if (email.equals(certEmail)) {
                return SIGNED_CURRENT;
            }
        }
        return SIGNED_OTHER;
    }

    /**
     * Finds the first signed PDF blob.
     */
    protected StatusWithBlob getSignedPdfBlobAndStatus(DocumentModel doc, DocumentModel user) {
        BlobHolder mbh = doc.getAdapter(BlobHolder.class);
        if (mbh != null) {
            Blob blob = mbh.getBlob();
            if (blob != null && MIME_TYPE_PDF.equals(blob.getMimeType())) {
                int status = getSigningStatus(blob, user);
                if (status != UNSIGNED) {
                    // TODO for File document it works, but for general
                    // blob holders the path may be incorrect
                    return new StatusWithBlob(status, blob, mbh, FILE_CONTENT);
                }
            }
        }
        @SuppressWarnings("unchecked")
        List<Map<String, Serializable>> files = (List<Map<String, Serializable>>) doc.getPropertyValue(FILES_FILES);
        int i = -1;
        for (Map<String, Serializable> map : files) {
            i++;
            Blob blob = (Blob) map.get(FILES_FILE);
            if (blob != null && MIME_TYPE_PDF.equals(blob.getMimeType())) {
                int status = getSigningStatus(blob, user);
                if (status != UNSIGNED) {
                    String pathbase = FILES_FILES + "/" + i + "/";
                    String path = pathbase + FILES_FILE;
                    BlobHolder bh = new DocumentBlobHolder(doc, path);
                    return new StatusWithBlob(status, blob, bh, path);
                }
            }
        }
        return null;
    }

    @Override
    public Blob signDocument(DocumentModel doc, DocumentModel user, String keyPassword, String reason, boolean pdfa,
            SigningDisposition disposition, String archiveFilename) {

        StatusWithBlob blobAndStatus = getSignedPdfBlobAndStatus(doc, user);
        if (blobAndStatus != null) {
            // re-sign it
            Blob signedBlob = signPDF(blobAndStatus.blob, doc, user, keyPassword, reason);
            signedBlob.setFilename(blobAndStatus.blob.getFilename());
            // replace the previous blob with a new one
            blobAndStatus.blobHolder.setBlob(signedBlob);
            return signedBlob;
        }

        Blob originalBlob;
        BlobHolder mbh = doc.getAdapter(BlobHolder.class);
        if (mbh == null || (originalBlob = mbh.getBlob()) == null) {
            return null;
        }

        Blob pdfBlob;
        if (MIME_TYPE_PDF.equals(originalBlob.getMimeType())) {
            pdfBlob = originalBlob;
        } else {
            // convert to PDF or PDF/A first
            ConversionService conversionService = Framework.getService(ConversionService.class);
            Map<String, Serializable> parameters = new HashMap<>();
            if (pdfa) {
                parameters.put(PDFA1_PARAM, Boolean.TRUE);
            }
            try {
                BlobHolder holder = conversionService.convert("any2pdf", new SimpleBlobHolder(originalBlob),
                        parameters);
                pdfBlob = holder.getBlob();
            } catch (ConversionException conversionException) {
                throw new SignException(conversionException);
            }
        }

        Blob signedBlob = signPDF(pdfBlob, doc, user, keyPassword, reason);
        signedBlob.setFilename(FilenameUtils.getBaseName(originalBlob.getFilename()) + ".pdf");

        Map<String, Serializable> map;
        ListDiff listDiff;
        switch (disposition) {
        case REPLACE:
            // replace main blob
            mbh.setBlob(signedBlob);
            break;
        case ARCHIVE:
            // archive as attachment
            originalBlob.setFilename(archiveFilename);
            map = new HashMap<>();
            map.put(FILES_FILE, (Serializable) originalBlob);
            listDiff = new ListDiff();
            listDiff.add(map);
            doc.setPropertyValue(FILES_FILES, listDiff);
            // and replace main blob
            mbh.setBlob(signedBlob);
            break;
        case ATTACH:
            // set as first attachment
            map = new HashMap<>();
            map.put(FILES_FILE, (Serializable) signedBlob);
            listDiff = new ListDiff();
            listDiff.insert(0, map);
            doc.setPropertyValue(FILES_FILES, listDiff);
            break;
        }

        return signedBlob;
    }

    @Override
    public Blob signPDF(Blob pdfBlob, DocumentModel doc, DocumentModel user, String keyPassword, String reason) {
        CertService certService = Framework.getService(CertService.class);
        CUserService cUserService = Framework.getService(CUserService.class);
        try {
            File outputFile = Framework.createTempFile("signed-", ".pdf");
            Blob blob = Blobs.createBlob(outputFile, MIME_TYPE_PDF);
            Framework.trackFile(outputFile, blob);

            PdfReader pdfReader = new PdfReader(pdfBlob.getStream());
            List<X509Certificate> pdfCertificates = getCertificates(pdfReader);

            // allows for multiple signatures
            PdfStamper pdfStamper = PdfStamper.createSignature(pdfReader, new FileOutputStream(outputFile), '\0', null,
                    true);

            String userID = (String) user.getPropertyValue("user:username");
            AliasWrapper alias = new AliasWrapper(userID);
            KeyStore keystore = cUserService.getUserKeystore(userID, keyPassword);
            Certificate certificate = certService.getCertificate(keystore, alias.getId(AliasType.CERT));
            KeyPair keyPair = certService.getKeyPair(keystore, alias.getId(AliasType.KEY), alias.getId(AliasType.CERT),
                    keyPassword);

            if (certificatePresentInPDF(certificate, pdfCertificates)) {
                X509Certificate userX509Certificate = (X509Certificate) certificate;
                String message = ALREADY_SIGNED_BY + userX509Certificate.getSubjectDN();
                log.debug(message);
                throw new AlreadySignedException(message);
            }

            PdfSignatureAppearance pdfSignatureAppearance = pdfStamper.getSignatureAppearance();
            pdfSignatureAppearance.setCrypto(keyPair.getPrivate(), (X509Certificate) certificate, null, PdfSignatureAppearance.SELF_SIGNED);
            if (StringUtils.isBlank(reason)) {
                reason = getSigningReason();
            }
            pdfSignatureAppearance.setVisibleSignature(getNextCertificatePosition(pdfReader, pdfCertificates), 1, null);
            getSignatureAppearanceFactory().format(pdfSignatureAppearance, doc, userID, reason);

            pdfStamper.close(); // closes the file

            log.debug("File " + outputFile.getAbsolutePath() + " created and signed with " + reason);

            return blob;
        } catch (IOException | DocumentException | InstantiationException | IllegalAccessException e) {
            throw new SignException(e);
        } catch (IllegalArgumentException e) {
            if (String.valueOf(e.getMessage()).contains("PdfReader not opened with owner password")) {
                // iText PDF reading
                throw new SignException("PDF is password-protected");
            }
            throw new SignException(e);
        }
    }

    /**
     * @since 5.8
     * @return the signature layout. Default one if no contribution.
     */
    @Override
    public SignatureLayout getSignatureLayout() {
        for (SignatureDescriptor signatureDescriptor : signatureRegistryMap.values()) {
            SignatureLayout signatureLayout = signatureDescriptor.getSignatureLayout();
            if (signatureLayout != null) {
                return signatureLayout;
            }
        }
        return new SignatureDescriptor.SignatureLayout();
    }

    protected SignatureAppearanceFactory getSignatureAppearanceFactory()
            throws InstantiationException, IllegalAccessException {
        if (!signatureRegistryMap.isEmpty()) {
            SignatureDescriptor signatureDescriptor = signatureRegistryMap.values().iterator().next();
            return signatureDescriptor.getAppearanceFatory();
        }
        return new DefaultSignatureAppearanceFactory();
    }

    protected String getSigningReason() throws SignException {
        for (SignatureDescriptor sd : signatureRegistryMap.values()) {
            String reason = sd.getReason();
            if (!StringUtils.isBlank(reason)) {
                return reason;
            }
        }
        throw new SignException("No default signing reason provided in configuration");
    }

    protected boolean certificatePresentInPDF(Certificate userCert, List<X509Certificate> pdfCertificates)
            throws SignException {
        X509Certificate xUserCert = (X509Certificate) userCert;
        for (X509Certificate xcert : pdfCertificates) {
            // matching certificate found
            if (xcert.getSubjectX500Principal().equals(xUserCert.getSubjectX500Principal())) {
                return true;
            }
        }
        return false;
    }

    /**
     * @since 5.8 Provides the position rectangle for the next certificate. An assumption is made that all previous
     *        certificates in a given PDF were placed using the same technique and settings. New certificates are added
     *        depending of signature layout contributed.
     */
    protected Rectangle getNextCertificatePosition(PdfReader pdfReader, List<X509Certificate> pdfCertificates)
            throws SignException {
        int numberOfSignatures = pdfCertificates.size();

        Rectangle pageSize = pdfReader.getPageSize(PAGE_TO_SIGN);

        // PDF size
        float width = pageSize.getWidth();
        float height = pageSize.getHeight();

        // Signature size
        float rectangleWidth = width / getSignatureLayout().getColumns();
        float rectangeHeight = height / getSignatureLayout().getLines();

        // Signature location
        int column = numberOfSignatures % getSignatureLayout().getColumns() + getSignatureLayout().getStartColumn();
        int line = numberOfSignatures / getSignatureLayout().getColumns() + getSignatureLayout().getStartLine();
        if (column > getSignatureLayout().getColumns()) {
            column = column % getSignatureLayout().getColumns();
            line++;
        }

        // Skip rectangle display If number of signatures exceed free locations
        // on pdf layout
        if (line > getSignatureLayout().getLines()) {
            return new Rectangle(0, 0, 0, 0);
        }

        // make smaller by page margin
        float topRightX = rectangleWidth * column;
        float bottomLeftY = height - rectangeHeight * line;
        float bottomLeftX = topRightX - SIGNATURE_FIELD_WIDTH;
        float topRightY = bottomLeftY + SIGNATURE_FIELD_HEIGHT;

        // verify current position coordinates in case they were
        // misconfigured
        validatePageBounds(pdfReader, 1, bottomLeftX, true);
        validatePageBounds(pdfReader, 1, bottomLeftY, false);
        validatePageBounds(pdfReader, 1, topRightX, true);
        validatePageBounds(pdfReader, 1, topRightY, false);

        Rectangle positionRectangle = new Rectangle(bottomLeftX, bottomLeftY, topRightX, topRightY);

        return positionRectangle;
    }

    /**
     * Verifies that a provided value fits within the page bounds. If it does not, a sign exception is thrown. This is
     * to verify externally configurable signature positioning.
     *
     * @param isHorizontal - if false, the current value is checked agains the vertical page dimension
     */
    protected void validatePageBounds(PdfReader pdfReader, int pageNo, float valueToCheck, boolean isHorizontal)
            throws SignException {
        if (valueToCheck < 0) {
            String message = "The new signature position " + valueToCheck
                    + " exceeds the page bounds. The position must be a positive number.";
            log.debug(message);
            throw new SignException(message);
        }

        Rectangle pageRectangle = pdfReader.getPageSize(pageNo);
        if (isHorizontal && valueToCheck > pageRectangle.getRight()) {
            String message = "The new signature position " + valueToCheck
                    + " exceeds the horizontal page bounds. The page dimensions are: (" + pageRectangle + ").";
            log.debug(message);
            throw new SignException(message);
        }
        if (!isHorizontal && valueToCheck > pageRectangle.getTop()) {
            String message = "The new signature position " + valueToCheck
                    + " exceeds the vertical page bounds. The page dimensions are: (" + pageRectangle + ").";
            log.debug(message);
            throw new SignException(message);
        }
    }

    @Override
    public List<X509Certificate> getCertificates(DocumentModel doc) {
        StatusWithBlob signedBlob = getSignedPdfBlobAndStatus(doc, null);
        if (signedBlob == null) {
            return Collections.emptyList();
        }
        return getCertificates(signedBlob.blob);
    }

    protected List<X509Certificate> getCertificates(Blob pdfBlob) throws SignException {
        try {
            PdfReader pdfReader = new PdfReader(pdfBlob.getStream());
            return getCertificates(pdfReader);
        } catch (IOException e) {
            String message = "";
            if (e.getMessage().equals("PDF header signature not found.")) {
                message = "PDF seems to be corrupted";
            }
            throw new SignException(message, e);
        }
    }

    protected List<X509Certificate> getCertificates(PdfReader pdfReader) throws SignException {
        List<X509Certificate> pdfCertificates = new ArrayList<>();
        AcroFields acroFields = pdfReader.getAcroFields();
        @SuppressWarnings("unchecked")
        List<String> signatureNames = acroFields.getSignatureNames();
        for (String signatureName : signatureNames) {
            PdfPKCS7 pdfPKCS7 = acroFields.verifySignature(signatureName);
            X509Certificate signingCertificate = pdfPKCS7.getSigningCertificate();
            pdfCertificates.add(signingCertificate);
        }
        return pdfCertificates;
    }

}
