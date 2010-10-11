package org.nuxeo.ecm.platform.signature.core.pki;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.math.BigInteger;
import java.security.InvalidKeyException;
import java.security.KeyPair;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.CertificateParsingException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.security.auth.x500.X500Principal;

import org.bouncycastle.asn1.ASN1Set;
import org.bouncycastle.asn1.DERObjectIdentifier;
import org.bouncycastle.asn1.pkcs.Attribute;
import org.bouncycastle.asn1.pkcs.PKCSObjectIdentifiers;
import org.bouncycastle.asn1.x509.BasicConstraints;
import org.bouncycastle.asn1.x509.ExtendedKeyUsage;
import org.bouncycastle.asn1.x509.KeyPurposeId;
import org.bouncycastle.asn1.x509.KeyUsage;
import org.bouncycastle.asn1.x509.X509Extension;
import org.bouncycastle.asn1.x509.X509Extensions;
import org.bouncycastle.jce.PKCS10CertificationRequest;
import org.bouncycastle.x509.X509V3CertificateGenerator;
import org.bouncycastle.x509.extension.AuthorityKeyIdentifierStructure;
import org.bouncycastle.x509.extension.SubjectKeyIdentifierStructure;
import org.nuxeo.ecm.platform.signature.api.exception.CertException;
import org.nuxeo.ecm.platform.signature.api.pki.CAService;
import org.nuxeo.ecm.platform.signature.api.pki.KeyService;
import org.nuxeo.ecm.platform.signature.api.user.CNField;
import org.nuxeo.ecm.platform.signature.api.user.UserInfo;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.model.ComponentInstance;
import org.nuxeo.runtime.model.DefaultComponent;

/**
 * @author <a href="mailto:ws@nuxeo.com">Wojciech Sulejman</a>
 *
 */
public class CAServiceImpl extends DefaultComponent implements CAService {

    private List<CADescriptor> config = new ArrayList<CADescriptor>();
    protected KeyService keyService;
    protected KeyPair rootKeys;
    protected X509Certificate rootCertificate;

    //TODO move to descriptor
    private static final String CERT_SIGNATURE_ALGORITHM= "SHA256WithRSAEncryption";
    private static final String ROOT_USER_DN="CN=PDFCA";

    public void setRootCertificate(X509Certificate rootCertificate) {
        this.rootCertificate = rootCertificate;
    }

    @Override
    public X509Certificate createCertificateFromCSR(
            PKCS10CertificationRequest csr, UserInfo userInfo)
            throws CertException {
        X509Certificate cert;
        try {
            X509V3CertificateGenerator certGen = new X509V3CertificateGenerator();
            certGen.setSerialNumber(BigInteger.valueOf(System.currentTimeMillis()));
            certGen.setIssuerDN(getRootCertificate().getIssuerX500Principal());
            certGen.setSubjectDN(csr.getCertificationRequestInfo().getSubject());
            certGen.setNotBefore(getCertStartDate());
            certGen.setNotAfter(getCertEndDate());
            certGen.setPublicKey(csr.getPublicKey("BC"));
            certGen.setSignatureAlgorithm(CERT_SIGNATURE_ALGORITHM);
            certGen.addExtension(X509Extensions.SubjectKeyIdentifier, false,
                    new SubjectKeyIdentifierStructure(csr.getPublicKey("BC")));
            certGen.addExtension(X509Extensions.AuthorityKeyIdentifier, false,
                    new AuthorityKeyIdentifierStructure(getRootCertificate()));
            certGen.addExtension(X509Extensions.BasicConstraints, true,
                    new BasicConstraints(false));
            certGen.addExtension(X509Extensions.KeyUsage, true, new KeyUsage(
                    KeyUsage.digitalSignature));
            certGen.addExtension(X509Extensions.ExtendedKeyUsage, true,
                    new ExtendedKeyUsage(KeyPurposeId.id_kp_serverAuth));

            ASN1Set attributes = csr.getCertificationRequestInfo().getAttributes();
            for (int i = 0; i != attributes.size(); i++) {
                Attribute attr = Attribute.getInstance(attributes.getObjectAt(i));
                if (attr.getAttrType().equals(
                        PKCSObjectIdentifiers.pkcs_9_at_extensionRequest)) {
                    X509Extensions extensions = X509Extensions.getInstance(attr.getAttrValues().getObjectAt(
                            0));
                    Enumeration e = extensions.oids();
                    while (e.hasMoreElements()) {
                        DERObjectIdentifier oid = (DERObjectIdentifier) e.nextElement();
                        X509Extension ext = extensions.getExtension(oid);
                        certGen.addExtension(oid, ext.isCritical(),
                                ext.getValue().getOctets());
                    }
                }
            }

            cert = certGen.generate(getRootKeys().getPrivate(), "BC");
        } catch (CertificateParsingException e) {
            throw new CertException(e);
        } catch (CertificateEncodingException e) {
            throw new CertException(e);
        } catch (InvalidKeyException e) {
            throw new CertException(e);
        } catch (IllegalStateException e) {
            throw new CertException(e);
        } catch (NoSuchProviderException e) {
            throw new CertException(e);
        } catch (NoSuchAlgorithmException e) {
            throw new CertException(e);
        } catch (java.security.SignatureException e) {
            throw new CertException(e);
        }
        return cert;
    }

    @Override
    public X509Certificate getRootCertificate() throws CertException {
        if (rootCertificate == null) {
            rootCertificate = getCertificate(getRootCertificateFile());
        }
        return rootCertificate;
    }

    private X509Certificate createRootCertificate() throws CertException {
        X509V3CertificateGenerator certGen = new X509V3CertificateGenerator();
        certGen.setSerialNumber(BigInteger.valueOf(System.currentTimeMillis()));
        certGen.setIssuerDN(new X500Principal(ROOT_USER_DN));
        certGen.setSubjectDN(new X500Principal(ROOT_USER_DN));
        certGen.setNotBefore(getCertStartDate());
        certGen.setNotAfter(getCertEndDate());
        certGen.setPublicKey(getRootKeys().getPublic());
        certGen.setSignatureAlgorithm(CERT_SIGNATURE_ALGORITHM);
        try {
            certGen.addExtension(
                    X509Extensions.SubjectKeyIdentifier,
                    false,
                    new SubjectKeyIdentifierStructure(getRootKeys().getPublic()));
            certGen.addExtension(X509Extensions.BasicConstraints, true,
                    new BasicConstraints(false));
            certGen.addExtension(X509Extensions.KeyUsage, true, new KeyUsage(
                    KeyUsage.digitalSignature | KeyUsage.keyEncipherment));
            certGen.addExtension(X509Extensions.ExtendedKeyUsage, true,
                    new ExtendedKeyUsage(KeyPurposeId.id_kp_codeSigning));
            rootCertificate = certGen.generate(getRootKeys().getPrivate(), "BC");
        } catch (CertificateEncodingException e) {
            throw new CertException(e);
        } catch (InvalidKeyException e) {
            throw new CertException(e);
        } catch (CertificateParsingException e) {
            throw new CertException(e);
        } catch (IllegalStateException e) {
            throw new CertException(e);

        } catch (NoSuchProviderException e) {
            throw new CertException(e);

        } catch (NoSuchAlgorithmException e) {
            throw new CertException(e);

        } catch (java.security.SignatureException e) {
            throw new CertException(e);
        }
        return rootCertificate;
    }

    private KeyPair getRootKeys() throws CertException {
        if (rootKeys == null) {
            rootKeys = getKeyService().getKeys(getRootUserInfo());
        }
        return rootKeys;
    }

    protected KeyService getKeyService() throws CertException {
        if (keyService == null) {
            try {
                keyService = Framework.getService(KeyService.class);
            } catch (Exception e) {
                throw new CertException(e);
            }
        }
        return keyService;
    }

    public X509Certificate getCertificate(File certFile) throws CertException {
        X509Certificate rootCert;
        try {
            InputStream in = new FileInputStream(certFile);
            CertificateFactory certificateFactory = CertificateFactory.getInstance(
                    "X.509", "BC");
            rootCert = (X509Certificate) certificateFactory.generateCertificate(in);
        } catch (FileNotFoundException e) {
            throw new CertException(e);
        } catch (CertificateException e) {
            throw new CertException(e);
        } catch (NoSuchProviderException e) {
            throw new CertException(e);
        }
        return rootCert;
    }

    File getRootCertificateFile() throws CertException {
        File rootCertificateFile = null;
        String rootCertificateFilePath = null;
        for (CADescriptor ca : config) {
            if (ca.getRootCertificateFilePath() != null) {
                rootCertificateFilePath = ca.getRootCertificateFilePath();
            }
        }
        if (rootCertificateFilePath == null) {
            throw new CertException(
                    "You have to provide path for the root certificate file");
        }
        rootCertificateFile = new File(rootCertificateFilePath);
        if (!rootCertificateFile.exists()) {
            throw new CertException("There is no root certificate file at:"
                    + rootCertificateFile.getAbsolutePath());
        }
        return rootCertificateFile;
    }

    @Override
    public void registerContribution(Object contribution,
            String extensionPoint, ComponentInstance contributor) {
        config.add((CADescriptor) contribution);
    }

    @Override
    public void unregisterContribution(Object contribution,
            String extensionPoint, ComponentInstance contributor) {
        config.remove(contribution);
    }

    //TODO move to descriptor
    protected UserInfo getRootUserInfo() throws CertException{
        Map<CNField,String> rootUserFields;
            rootUserFields = new HashMap<CNField,String>();
            rootUserFields.put(CNField.C, "US");
            rootUserFields.put(CNField.O, "Nuxeo");
            rootUserFields.put(CNField.OU, "CA");
            rootUserFields.put(CNField.CN, "PDF Certificate Authority");
            rootUserFields.put(CNField.Email, "ca@nuxeo.com");
            rootUserFields.put(CNField.UserID, "pdfca");
        UserInfo rootUserInfo= new UserInfo(rootUserFields);
        return rootUserInfo;
    }

    protected Date getCertStartDate(){
        Calendar cal = Calendar.getInstance();
        return cal.getTime();
    }

    protected Date getCertEndDate(){
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.MONTH, 12);
        return cal.getTime();
    }

}