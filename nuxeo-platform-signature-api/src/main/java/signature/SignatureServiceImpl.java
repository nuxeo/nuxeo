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
 *     ws@nuxeo.com
 */

package signature;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.security.KeyPair;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.util.ArrayList;
import java.util.List;

import key.CertInfo;
import key.CertService;
import key.CertServiceImpl;
import key.KeyService;
import key.KeyServiceImpl;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.lowagie.text.DocumentException;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.PdfReader;
import com.lowagie.text.pdf.PdfSignatureAppearance;
import com.lowagie.text.pdf.PdfStamper;

/**
 * @author <a href="mailto:ws@nuxeo.com">WS</a>
 *
 */
public class SignatureServiceImpl implements SignatureService {
    private static final Log log = LogFactory.getLog(SignatureServiceImpl.class);

    public Status signPDF(CertInfo certInfo, String origPdfPath,
            String outputPdfPath) {
        Status status = Status.OK;
        KeyService keyService = new KeyServiceImpl();
        CertService certService = new CertServiceImpl();
        try {
            PdfReader reader = new PdfReader(origPdfPath);
            FileOutputStream fout = new FileOutputStream(outputPdfPath);
            PdfStamper stp = PdfStamper.createSignature(reader, fout, '\0');
            PdfSignatureAppearance sap = stp.getSignatureAppearance();
            KeyPair keyPair = keyService.getKeys(certInfo);
            Certificate certificate = certService.createCertificate(keyPair,
                    certInfo);
            List<Certificate> certificates = new ArrayList<Certificate>();
            certificates.add(certificate);

            Certificate[] certChain = certificates.toArray(new Certificate[0]);
            sap.setCrypto(keyPair.getPrivate(), certChain, null,
                    PdfSignatureAppearance.SELF_SIGNED);

            // TODO replace
            sap.setReason("Nuxeo signed document");

            sap.setVisibleSignature(new Rectangle(400, 450, 200, 200), 1, null);
            stp.close();
        } catch (UnrecoverableKeyException e) {
            log.error(e);
            status = Status.UnrecoverableKeyException;
        } catch (KeyStoreException e) {
            log.error(e);
            status = Status.KeyStoreException;
        } catch (NoSuchAlgorithmException e) {
            log.error(e);
            status = Status.NoSuchAlgorithmException;
        } catch (CertificateException e) {
            log.error(e);
            status = Status.CertificateException;
        } catch (FileNotFoundException e) {
            log.error(e);
            status = Status.FileNotFoundException;
        } catch (IOException e) {
            log.error(e);
            if (e.getCause().getClass().equals(UnrecoverableKeyException.class)) {
                status = Status.UnrecoverableKeyException;
            } else {
                status = Status.IOException;
            }
        } catch (DocumentException e) {
            log.error(e);
            status = Status.DocumentException;
        } catch (Exception e) {
            log.error(e);
            status = Status.Exception;
        }
        return status;
    }
}