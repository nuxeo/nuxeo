package org.nuxeo.ecm.platform.signature.core.pki;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigInteger;
import java.security.InvalidKeyException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Security;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateParsingException;
import java.security.cert.X509Certificate;
import java.util.Calendar;
import java.util.Date;
import java.util.Enumeration;
import java.util.Vector;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.bouncycastle.asn1.ASN1InputStream;
import org.bouncycastle.asn1.ASN1Set;
import org.bouncycastle.asn1.DERObject;
import org.bouncycastle.asn1.DERObjectIdentifier;
import org.bouncycastle.asn1.DEROctetString;
import org.bouncycastle.asn1.DERSet;
import org.bouncycastle.asn1.DERUTF8String;
import org.bouncycastle.asn1.pkcs.Attribute;
import org.bouncycastle.asn1.pkcs.CertificationRequest;
import org.bouncycastle.asn1.pkcs.PKCSObjectIdentifiers;
import org.bouncycastle.asn1.x509.BasicConstraints;
import org.bouncycastle.asn1.x509.ExtendedKeyUsage;
import org.bouncycastle.asn1.x509.GeneralName;
import org.bouncycastle.asn1.x509.GeneralNames;
import org.bouncycastle.asn1.x509.KeyPurposeId;
import org.bouncycastle.asn1.x509.KeyUsage;
import org.bouncycastle.asn1.x509.X509Extension;
import org.bouncycastle.asn1.x509.X509Extensions;
import org.bouncycastle.jce.PKCS10CertificationRequest;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.x509.X509V3CertificateGenerator;
import org.bouncycastle.x509.extension.AuthorityKeyIdentifierStructure;
import org.bouncycastle.x509.extension.SubjectKeyIdentifierStructure;
import org.nuxeo.ecm.platform.signature.api.exception.CertException;
import org.nuxeo.ecm.platform.signature.api.pki.CertService;
import org.nuxeo.ecm.platform.signature.api.pki.RootService;
import org.nuxeo.ecm.platform.signature.api.user.AliasType;
import org.nuxeo.ecm.platform.signature.api.user.AliasWrapper;
import org.nuxeo.ecm.platform.signature.api.user.CNField;
import org.nuxeo.ecm.platform.signature.api.user.UserInfo;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.model.DefaultComponent;

/**
 * Base implementation of the certification service.
 * 
 * @author <a href="mailto:ws@nuxeo.com">Wojciech Sulejman</a>
 * 
 */
public class CertServiceImpl extends DefaultComponent implements CertService {

    protected RootService rootService;

    private static final Log LOG = LogFactory.getLog(CertServiceImpl.class);

    public void setRootService(RootService rootService) {
        this.rootService = rootService;
    }

    protected X509Certificate rootCertificate;

    private static final int CERTIFICATE_DURATION_IN_MONTHS = 12;

    private static final String CERT_SIGNATURE_ALGORITHM = "SHA256WithRSAEncryption";

    private static final String KEY_ALGORITHM = "RSA";

    private static final int KEY_SIZE = 1024;

    private static final String KEYSTORE_TYPE = "JKS";

    static {
        if (Security.getProvider("BC") == null) {
            Security.addProvider(new BouncyCastleProvider());
        }
    }

    private X509Certificate createCertificateFromCSR(
            PKCS10CertificationRequest csr) throws CertException {
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

            KeyPair rootKeyPair = getKeyPair(rootService.getRootKeyStore(),
                    rootService.getRootKeyAlias(),
                    rootService.getRootCertificateAlias(),
                    rootService.getRootKeyPassword());
            cert = certGen.generate(rootKeyPair.getPrivate(), "BC");
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
        LOG.debug("Certificate generated for subject: " + cert.getSubjectDN());
        return cert;
    }

    @Override
    public X509Certificate getRootCertificate() throws CertException {
        if (rootCertificate == null) {
            rootCertificate = getCertificate(
                    getRootService().getRootKeyStore(),
                    getRootService().getRootCertificateAlias());
        }
        return rootCertificate;
    }

    protected Date getCertStartDate() {
        Calendar cal = Calendar.getInstance();
        return cal.getTime();
    }

    protected Date getCertEndDate() {
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.MONTH, CERTIFICATE_DURATION_IN_MONTHS);
        return cal.getTime();
    }

    @Override
    public KeyStore initializeUser(UserInfo userInfo, String suppliedPassword)
            throws CertException {
        char[] password = suppliedPassword.toCharArray();
        KeyStore ks = null;
        String userName = userInfo.getUserFields().get(CNField.UserID);
        AliasWrapper keystoreAlias = new AliasWrapper(userName);
        try {
            ks = java.security.KeyStore.getInstance(KEYSTORE_TYPE);
            ks.load(null, password);
            KeyPairGenerator keyGen = KeyPairGenerator.getInstance(KEY_ALGORITHM);
            keyGen.initialize(KEY_SIZE);
            KeyPair keyPair = keyGen.genKeyPair();
            java.security.cert.Certificate[] chain = { getRootCertificate() };
            ks.setKeyEntry(keystoreAlias.getId(AliasType.KEY),
                    keyPair.getPrivate(), password, chain);
            X509Certificate cert = getCertificate(keyPair, userInfo);
            ks.setCertificateEntry(keystoreAlias.getId(AliasType.CERT), cert);
        } catch (CertificateException e) {
            throw new CertException(e);
        } catch (IOException e) {
            throw new CertException(e);
        } catch (KeyStoreException e) {
            throw new CertException(e);
        } catch (NoSuchAlgorithmException e) {
            throw new CertException(e);
        }
        return ks;
    }

    @Override
    public KeyPair getKeyPair(KeyStore ks, String keyAlias, String certAlias,
            String keyPassword) throws CertException {
        KeyPair keyPair = null;
        try {
            if (!ks.containsAlias(keyAlias)) {
                throw new CertException(
                        "Missing keystore key entry for key alias:" + keyAlias);
            }
            if (!ks.containsAlias(certAlias)) {
                throw new CertException(
                        "Missing keystore certificate entry for :" + certAlias);
            }
            PrivateKey privateKey = (PrivateKey) ks.getKey(keyAlias,
                    keyPassword.toCharArray());
            X509Certificate cert = (X509Certificate) ks.getCertificate(certAlias);
            PublicKey publicKey = cert.getPublicKey();
            keyPair = new KeyPair(publicKey, privateKey);
        } catch (UnrecoverableKeyException e) {
            throw new CertException(e);
        } catch (KeyStoreException e) {
            throw new CertException(e);
        } catch (NoSuchAlgorithmException e) {
            throw new CertException(e);
        }
        return keyPair;
    }

    @Override
    public X509Certificate getCertificate(KeyStore ks, String certificateAlias)
            throws CertException {
        X509Certificate certificate = null;
        try {

            if (ks == null) {
                throw new CertException("Keystore missing for "
                        + certificateAlias);
            }
            if (ks.containsAlias(certificateAlias)) {
                certificate = (X509Certificate) ks.getCertificate(certificateAlias);
            } else {
                throw new CertException("Certificate not found");
            }
        } catch (KeyStoreException e) {
            throw new CertException(e);
        }
        return certificate;
    }

    private X509Certificate getCertificate(KeyPair keyPair, UserInfo userInfo)
            throws CertException {
        PKCS10CertificationRequest csr = (PKCS10CertificationRequest) generateCSR(
                keyPair, userInfo);
        X509Certificate certificate = createCertificateFromCSR(csr);
        return certificate;
    }

    private CertificationRequest generateCSR(KeyPair keyPair, UserInfo userInfo)
            throws CertException {

        CertificationRequest csr;

        GeneralNames subjectAltName = new GeneralNames(new GeneralName(
                GeneralName.rfc822Name, userInfo.getUserFields().get(
                        CNField.Email)));

        Vector<DERObjectIdentifier> objectIdentifiers = new Vector<DERObjectIdentifier>();
        Vector<X509Extension> extensionValues = new Vector<X509Extension>();

        objectIdentifiers.add(X509Extensions.SubjectAlternativeName);
        extensionValues.add(new X509Extension(false, new DEROctetString(
                subjectAltName)));

        X509Extensions extensions = new X509Extensions(objectIdentifiers,
                extensionValues);

        Attribute attribute = new Attribute(
                PKCSObjectIdentifiers.pkcs_9_at_extensionRequest, new DERSet(
                        extensions));
        try {
            csr = new PKCS10CertificationRequest(CERT_SIGNATURE_ALGORITHM,
                    userInfo.getX500Principal(), keyPair.getPublic(),
                    new DERSet(attribute), keyPair.getPrivate());
        } catch (InvalidKeyException e) {
            throw new CertException(e);
        } catch (NoSuchAlgorithmException e) {
            throw new CertException(e);
        } catch (NoSuchProviderException e) {
            throw new CertException(e);
        } catch (java.security.SignatureException e) {
            throw new CertException(e);
        } catch (Exception e) {
            throw new CertException(e);
        }
        return csr;
    }

    @Override
    public KeyStore getKeyStore(InputStream keystoreIS, String password)
            throws CertException {
        KeyStore ks;
        try {
            ks = java.security.KeyStore.getInstance(KEYSTORE_TYPE);
            ks.load(keystoreIS, password.toCharArray());
        } catch (KeyStoreException e) {
            throw new CertException(e);
        } catch (NoSuchAlgorithmException e) {
            throw new CertException(e);
        } catch (CertificateException e) {
            throw new CertException(e);
        } catch (IOException e) {
            throw new CertException(e);
        }
        return ks;
    }


    @Override
    public String getCertificateEmail(X509Certificate certificate) throws CertException{
        String emailOID = "2.5.29.17";
        byte[] emailBytes = certificate.getExtensionValue(emailOID);
        String certificateEmail=null;
        try {
            byte[] octets=((DEROctetString)org.bouncycastle.asn1.ASN1Object.fromByteArray(emailBytes)).getOctets();
            GeneralNames generalNameCont=GeneralNames.getInstance(org.bouncycastle.asn1.ASN1Object.fromByteArray(octets));
            GeneralName[] generalNames=generalNameCont.getNames();
            if(generalNames.length>0){
                GeneralName generalName=generalNames[0];
                certificateEmail=generalName.getName().toString();
            }
        } catch (IOException e) {
             throw new CertException("Email could not be extracted from certificate",e);
        }
        return certificateEmail;
    }

    
  
    
    /**
     * {@inheritDoc}
     */
    @Override
    public void storeCertificate(KeyStore keystore, OutputStream os,
            String keystorePassword) throws CertException {
        try {
            keystore.store(os, keystorePassword.toCharArray());
        } catch (KeyStoreException e) {
            throw new CertException(e);
        } catch (NoSuchAlgorithmException e) {
            throw new CertException(e);
        } catch (CertificateException e) {
            throw new CertException(e);
        } catch (IOException e) {
            throw new CertException(e);
        }
    }

    protected RootService getRootService() throws CertException {
        if (rootService == null) {
            try {
                rootService = Framework.getService(RootService.class);
            } catch (Exception e) {
                String message = "RootService not found";
                LOG.error(message + " " + e);
                throw new CertException(message);
            }
        }
        return rootService;
    }
}