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
import java.security.UnrecoverableKeyException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.platform.signature.api.pki.CertInfo;
import org.nuxeo.ecm.platform.signature.api.pki.CertService;
import org.nuxeo.ecm.platform.signature.api.pki.KeyService;
import org.nuxeo.ecm.platform.signature.api.sign.SignatureService;
import org.nuxeo.ecm.platform.signature.core.pki.CertServiceImpl;
import org.nuxeo.ecm.platform.signature.core.pki.KeyServiceImpl;
import org.nuxeo.runtime.model.ComponentInstance;
import org.nuxeo.runtime.model.DefaultComponent;

import com.lowagie.text.DocumentException;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.PdfReader;
import com.lowagie.text.pdf.PdfSignatureAppearance;
import com.lowagie.text.pdf.PdfStamper;

/**
 *  @author <a href="mailto:ws@nuxeo.com">Wojciech Sulejman</a>
 *
 */
public class SignatureServiceImpl extends DefaultComponent implements
        SignatureService {
    private static final Log log = LogFactory.getLog(SignatureServiceImpl.class);

    private List<SignatureDescriptor> config = new ArrayList<SignatureDescriptor>();

    public File signPDF(CertInfo certInfo, InputStream origPdfStream)
            throws Exception {
        KeyService keyService = new KeyServiceImpl();
        CertService certService = new CertServiceImpl();
        File outputFile = null;
        try {
            outputFile = File.createTempFile("signed-", ".pdf");
            PdfReader reader = new PdfReader(origPdfStream);

            PdfStamper stp = PdfStamper.createSignature(reader,
                    new FileOutputStream(outputFile), '\0');
            PdfSignatureAppearance sap = stp.getSignatureAppearance();
            KeyPair keyPair = keyService.getKeys(certInfo);
            Certificate certificate = certService.createCertificate(keyPair,
                    certInfo);
            List<Certificate> certificates = new ArrayList<Certificate>();
            certificates.add(certificate);

            Certificate[] certChain = certificates.toArray(new Certificate[0]);
            sap.setCrypto(keyPair.getPrivate(), certChain, null,
                    PdfSignatureAppearance.SELF_SIGNED);

            sap.setReason(certInfo.getSigningReason());

            sap.setVisibleSignature(new Rectangle(400, 450, 200, 200), 1, null);

            stp.close();
        } catch (UnrecoverableKeyException e) {
            // TODO handling
            log.error(e);
        } catch (KeyStoreException e) {
            // TODO handling
            log.error(e);
        } catch (NoSuchAlgorithmException e) {
            // TODO handling
            log.error(e);
        } catch (CertificateException e) {
            // TODO handling
            log.error(e);
        } catch (FileNotFoundException e) {
            // TODO handling
            log.error(e);
        } catch (IOException e) {
            // TODO handling
            log.error(e);
        } catch (DocumentException e) {
            // TODO handling
            log.error(e);
        }
        return outputFile;
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