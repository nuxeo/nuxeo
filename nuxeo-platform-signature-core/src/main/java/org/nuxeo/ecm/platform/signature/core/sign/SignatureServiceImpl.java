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

package org.nuxeo.ecm.platform.signature.core.sign;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.KeyPair;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.SignatureException;
import java.security.UnrecoverableKeyException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.platform.signature.api.exception.CertException;
import org.nuxeo.ecm.platform.signature.api.exception.SignException;
import org.nuxeo.ecm.platform.signature.api.pki.CertInfo;
import org.nuxeo.ecm.platform.signature.api.pki.CertService;
import org.nuxeo.ecm.platform.signature.api.pki.KeyService;
import org.nuxeo.ecm.platform.signature.api.sign.SignatureService;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.model.ComponentInstance;
import org.nuxeo.runtime.model.DefaultComponent;

import com.lowagie.text.DocumentException;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.PdfReader;
import com.lowagie.text.pdf.PdfSignatureAppearance;
import com.lowagie.text.pdf.PdfStamper;

/**
 * @author <a href="mailto:ws@nuxeo.com">Wojciech Sulejman</a>
 *
 */
public class SignatureServiceImpl extends DefaultComponent implements
        SignatureService {
    private static final Log log = LogFactory.getLog(SignatureServiceImpl.class);

    private List<SignatureDescriptor> config = new ArrayList<SignatureDescriptor>();

    protected KeyService keyService;

    protected CertService certService;

    public File signPDF(CertInfo certInfo, InputStream origPdfStream)
            throws SignException {
        File outputFile = null;
        try {
            outputFile = File.createTempFile("signed-", ".pdf");
            PdfReader reader = new PdfReader(origPdfStream);

            PdfStamper stp = PdfStamper.createSignature(reader,
                    new FileOutputStream(outputFile), '\0');
            PdfSignatureAppearance sap = stp.getSignatureAppearance();
            KeyPair keyPair = getKeyService().getKeys(certInfo);
            Certificate certificate = getCertService().getCertificate(
                    certInfo);
            List<Certificate> certificates = new ArrayList<Certificate>();
            certificates.add(certificate);

            Certificate[] certChain = certificates.toArray(new Certificate[0]);
            sap.setCrypto(keyPair.getPrivate(), certChain, null,
                    PdfSignatureAppearance.SELF_SIGNED);
            if (certInfo.getSigningReason() == null) {
                sap.setReason(certInfo.getSigningReason());
            } else {
                sap.setReason(getReason());
            }
            sap.setVisibleSignature(new Rectangle(400, 450, 200, 200), 1, null);
            stp.close();
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
        } catch (Exception e){
            throw new SignException(e);
        }
        return outputFile;
    }

    protected KeyService getKeyService() throws Exception {
        if (keyService == null) {
            keyService = Framework.getService(KeyService.class);
        }
        return keyService;
    }

    protected CertService getCertService() throws Exception {
        if (certService == null) {
            certService = Framework.getService(CertService.class);
        }
        return certService;
    }

    private DateFormat getFormatter() {
        DateFormat formatter = new SimpleDateFormat("MM/dd/yy");
        return formatter;
    }

    String getReason() throws SignatureException{
        String reason=null;
        for (SignatureDescriptor sd : config) {
            if (sd.getReason() != null){
                reason=sd.getReason();
            }
        }
        if(reason==null){
            throw new SignatureException("You have to provide a default reason in the extension point");
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