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

package org.nuxeo.ecm.platform.signature.core.sign;

import java.awt.Color;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.KeyPair;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.SignatureException;
import java.security.UnrecoverableKeyException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.IOUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.platform.signature.api.exception.AlreadySignedException;
import org.nuxeo.ecm.platform.signature.api.exception.CertException;
import org.nuxeo.ecm.platform.signature.api.exception.SignException;
import org.nuxeo.ecm.platform.signature.api.pki.CertService;
import org.nuxeo.ecm.platform.signature.api.sign.SignatureService;
import org.nuxeo.ecm.platform.signature.api.user.AliasType;
import org.nuxeo.ecm.platform.signature.api.user.AliasWrapper;
import org.nuxeo.ecm.platform.signature.api.user.CUserService;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.model.ComponentInstance;
import org.nuxeo.runtime.model.DefaultComponent;

import com.lowagie.text.DocumentException;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.AcroFields;
import com.lowagie.text.pdf.PdfPKCS7;
import com.lowagie.text.pdf.PdfReader;
import com.lowagie.text.pdf.PdfSignatureAppearance;
import com.lowagie.text.pdf.PdfStamper;

/**
 * 
 * Base implementation for the signature service.
 * 
 * @author <a href="mailto:ws@nuxeo.com">Wojciech Sulejman</a>
 */
public class SignatureServiceImpl extends DefaultComponent implements
        SignatureService {

    private static final int SIGNATURE_FIELD_HEIGHT = 50;
    private static final int SIGNATURE_FIELD_WIDTH = 150;
    private static final int SIGNATURE_MARGIN= 10;
    
    
    private static final int PAGE_TO_SIGN = 1;

    private static final Log log = LogFactory.getLog(SignatureServiceImpl.class);

    private List<SignatureDescriptor> config = new ArrayList<SignatureDescriptor>();

    protected CertService certService;

    protected CUserService cUserService;

    public File signPDF(DocumentModel user, String keyPassword, String reason,
            InputStream origPdfStream) throws SignException {
        File outputFile = null;
        try {
            byte[] origPDFBytes = IOUtils.toByteArray(origPdfStream);
            String userID = (String) user.getPropertyValue("user:username");

            outputFile = File.createTempFile("signed-", ".pdf");
            PdfReader reader = new PdfReader(origPDFBytes);

            // allows for multiple signatures
            PdfStamper stp = PdfStamper.createSignature(reader,
                    new FileOutputStream(outputFile), '\0', null, true);

            PdfSignatureAppearance pdfSignatureAppearance = stp.getSignatureAppearance();
            AliasWrapper alias = new AliasWrapper(userID);
            KeyStore keystore = getCUserService().getUserKeystore(userID,
                    keyPassword);
            Certificate certificate = getCertService().getCertificate(keystore,
                    alias.getId(AliasType.CERT));
            KeyPair keyPair = getCertService().getKeyPair(keystore,
                    alias.getId(AliasType.KEY), alias.getId(AliasType.CERT),
                    keyPassword);

            if (certificatePresentInPDF(origPDFBytes, certificate)) {
                X509Certificate userX509Certificate = (X509Certificate) certificate;
                String message = "This document has already been signed by "
                        + userX509Certificate.getSubjectX500Principal().getName();
                log.info(message);
                throw new AlreadySignedException(message);
            }

            List<Certificate> certificates = new ArrayList<Certificate>();
            certificates.add(certificate);

            Certificate[] certChain = certificates.toArray(new Certificate[0]);
            pdfSignatureAppearance.setCrypto(keyPair.getPrivate(), certChain,
                    null, PdfSignatureAppearance.SELF_SIGNED);
            if (null == reason || reason == "") {
                reason = getSigningReason();
            }
            pdfSignatureAppearance.setReason(reason);
            pdfSignatureAppearance.setAcro6Layers(true);
            Font layer2Font = FontFactory.getFont(FontFactory.TIMES, 10,
                    Font.NORMAL, new Color(0x00, 0x00, 0x00));
            pdfSignatureAppearance.setLayer2Font(layer2Font);
            pdfSignatureAppearance.setRender(PdfSignatureAppearance.SignatureRenderDescription);

            pdfSignatureAppearance.setVisibleSignature(
                    getNextCertificatePosition(reader, origPDFBytes), 1, null);

            stp.close();
            log.debug("File " + outputFile.getAbsolutePath()
                    + " created and signed with " + reason);

        } catch (UnrecoverableKeyException e) {
            throw new CertException(e);
        } catch (KeyStoreException e) {
            throw new CertException(e);
        } catch (NoSuchAlgorithmException e) {
            throw new SignException(e);
        } catch (CertificateException e) {
            throw new SignException(e);
        } catch (FileNotFoundException e) {
            throw new SignException(e);
        } catch (IOException e) {
            throw new SignException(e);
        } catch (SignatureException e) {
            throw new SignException(e);
        } catch (DocumentException e) {
            throw new SignException(e);
        } catch (AlreadySignedException e) {
            throw new SignException(e);
        } catch (Exception e) {
            throw new SignException(e);
        }
        return outputFile;
    }

    private boolean certificatePresentInPDF(byte[] origPDFBytes,
            Certificate userCert) throws SignException {
        boolean certificatePresent = false;
        X509Certificate xUserCert = (X509Certificate) userCert;
        List<X509Certificate> existingCertificates = getPDFCertificates(new ByteArrayInputStream(
                origPDFBytes));
        for (X509Certificate xcert : existingCertificates) {
            // matching certificate found
            if (xcert.getSubjectX500Principal().equals(
                    xUserCert.getSubjectX500Principal())) {
                return true;
            }
        }
        return certificatePresent;
    }

    /**
     * Provides the position rectangle for the next certificate.
     * 
     * An assumption is made that all previous certificates in a given PDF
     * were placed using the same technique and settings.
     * 
     * New certificates are added on a vertical plane going downwards.
     * 
     * @param reader
     * @param pdfBytes
     * @return
     * @throws SignException
     */
    private Rectangle getNextCertificatePosition(PdfReader reader,
            byte[] pdfBytes) throws SignException {
        int numberOfSignatures = getPDFCertificates(
                new ByteArrayInputStream(pdfBytes)).size();
        Rectangle pageSize = reader.getPageSize(PAGE_TO_SIGN);
        // make smaller by page margin
        float topRightX = pageSize.getRight() - SIGNATURE_MARGIN;
        float topRightY = pageSize.getHeight() - SIGNATURE_MARGIN- numberOfSignatures * SIGNATURE_FIELD_HEIGHT;
        float bottomLeftX = topRightX - SIGNATURE_FIELD_WIDTH;
        float bottomLeftY = topRightY - SIGNATURE_FIELD_HEIGHT;

        log.debug("The new signature position is: "+bottomLeftX+" "+bottomLeftY+" "+topRightX+" "+topRightY);

        // verify current position coordinates in case they were
        // misconfigured
        validatePageBounds(reader, 1, bottomLeftX, true);
        validatePageBounds(reader, 1, bottomLeftY, false);
        validatePageBounds(reader, 1, topRightX, true);
        validatePageBounds(reader, 1, topRightY, false);

        
        Rectangle positionRectangle = new Rectangle(bottomLeftX, bottomLeftY,
                topRightX, topRightY);

        return positionRectangle;
    }

    @Override
    public List<X509Certificate> getPDFCertificates(InputStream pdfStream)
            throws SignException {
        List<X509Certificate> pdfCertificates = new ArrayList<X509Certificate>();
        try {
            PdfReader pdfReader = new PdfReader(pdfStream);
            AcroFields acroFields = pdfReader.getAcroFields();

            List signatureNames = acroFields.getSignatureNames();
            for (int k = 0; k < signatureNames.size(); ++k) {
                String signatureName = (String) signatureNames.get(k);
                PdfPKCS7 pdfPKCS7 = acroFields.verifySignature(signatureName);
                X509Certificate signingCertificate = pdfPKCS7.getSigningCertificate();
                pdfCertificates.add(signingCertificate);
            }
        } catch (IOException e) {
            String message = "";
            if (e.getMessage().equals("PDF header signature not found.")) {
                message = "PDF seems to be corrupted";
            }
            throw new SignException(message, e);
        }
        return pdfCertificates;
    }

    /**
     * Verifies that a provided value fits within the page bounds. If it does
     * not, a sign exception is thrown. This is to verify externally
     * configurable signature positioning.
     * 
     * @param reader
     * @param pageNo
     * @param valueToCheck
     * @Param isHorizontal - if false, the current value is checked agains the
     *        vertical page dimension
     */
    protected void validatePageBounds(PdfReader reader, int pageNo,
            float valueToCheck, boolean isHorizontal) throws SignException {
        if (valueToCheck <= 0) {
            String message = "The new signature position "
                    + valueToCheck
                    + " exceeds the page bounds. The position must be a positive number.";
            log.warn(message);
            throw new SignException(message);
        }
        
        Rectangle pageRectangle = reader.getPageSize(pageNo);
        if (isHorizontal && valueToCheck > pageRectangle.getRight()) {
            String message = "The new signature position "
                    + valueToCheck
                    + " exceeds the horizontal page bounds. The page dimensions are: ("+pageRectangle+").";
            log.warn(message);
            throw new SignException(message);
        }
        if (!isHorizontal && valueToCheck > pageRectangle.getTop()) {
            String message = "The new signature position "
                    + valueToCheck
                    + " exceeds the vertical page bounds. The page dimensions are: ("+pageRectangle+").";
            log.warn(message);
            throw new SignException(message);
        }
    }

    protected CertService getCertService() throws Exception {
        if (certService == null) {
            certService = Framework.getService(CertService.class);
        }
        return certService;
    }

    protected CUserService getCUserService() throws Exception {
        if (cUserService == null) {
            cUserService = Framework.getService(CUserService.class);
        }
        return cUserService;
    }

    private String getSigningReason() throws SignatureException {
        String reason = null;
        for (SignatureDescriptor sd : config) {
            if (sd.getReason() != null) {
                reason = sd.getReason();
            }
        }
        if (reason == null) {
            throw new SignatureException(
                    "You have to provide a default reason in the extension point");
        }
        return reason;
    }

    
    
    
    @Override
    public void registerContribution(Object contribution,
            String extensionPoint, ComponentInstance contributor) {
        config.add((SignatureDescriptor) contribution);
    }

    @Override
    public void unregisterContribution(Object contribution,
            String extensionPoint, ComponentInstance contributor) {
        config.remove(contribution);
    }

}