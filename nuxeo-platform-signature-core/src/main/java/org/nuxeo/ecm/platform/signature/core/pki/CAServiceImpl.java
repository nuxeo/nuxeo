package org.nuxeo.ecm.platform.signature.core.pki;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
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
import java.security.cert.Certificate;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.CertificateParsingException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Enumeration;
import java.util.List;
import java.util.Vector;

import org.bouncycastle.asn1.ASN1Set;
import org.bouncycastle.asn1.DERObjectIdentifier;
import org.bouncycastle.asn1.DEROctetString;
import org.bouncycastle.asn1.DERSet;
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
import org.nuxeo.ecm.platform.signature.api.pki.Alias;
import org.nuxeo.ecm.platform.signature.api.pki.AliasType;
import org.nuxeo.ecm.platform.signature.api.pki.CAService;
import org.nuxeo.ecm.platform.signature.api.user.CNField;
import org.nuxeo.ecm.platform.signature.api.user.UserInfo;
import org.nuxeo.runtime.model.ComponentInstance;
import org.nuxeo.runtime.model.DefaultComponent;

/**
 * @author <a href="mailto:ws@nuxeo.com">Wojciech Sulejman</a>
 *
 */

public class CAServiceImpl extends DefaultComponent implements CAService {

    private List<CADescriptor> config = new ArrayList<CADescriptor>();

    protected String rootKeystoreFilePath = null;

    protected KeyStore rootKeyStore;

    protected UserInfo rootUserInfo;

    private String rootPassword;

    protected X509Certificate rootCertificate;

    protected KeyPair rootKeys;

    private static final String CERT_SIGNATURE_ALGORITHM = "SHA256WithRSAEncryption";

    private static final String KEY_ALGORITHM = "RSA";

    private static final int KEY_SIZE = 1024;

    private static final String KEYSTORE_TYPE = "JKS";

    private String getRootPassword() {
        return rootPassword;
    }

    public KeyStore getRootKeyStore() {
        return rootKeyStore;
    }

    public void setRootKeyStore(KeyStore rootKeyStore) {
        this.rootKeyStore = rootKeyStore;
    }

    public UserInfo getRootUserInfo() {
        return rootUserInfo;
    }

    public void setRootUserInfo(UserInfo rootUserInfo) {
        this.rootUserInfo = rootUserInfo;
    }

    public void setRootPassword(String rootPassword) {
        this.rootPassword = rootPassword;
    }

    @Override
    public void setRootCertificate(X509Certificate rootCertificate) {
        this.rootCertificate = rootCertificate;
    }

    static {
        if (Security.getProvider("BC") == null) {
            Security.addProvider(new BouncyCastleProvider());
        }
    }

    /**
     * {@inheritDoc}
     */
    public X509Certificate createCertificateFromCSR(
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

            if (getRootKeyStore() == null || getRootUserInfo() == null
                    || getRootPassword() == null) {
                throw new CertException("Root not initialized");
            }
            KeyPair rootKeyPair = getKeyPair(getRootKeyStore(),
                    getRootUserInfo(), getRootPassword());
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
        return cert;
    }

    @Override
    public X509Certificate getRootCertificate() throws CertException {
        if (rootCertificate == null) {
            rootCertificate = getCertificate(getRootCertificateFile());
        }
        return rootCertificate;
    }

    @Override
    public X509Certificate getCertificate(File certFile) throws CertException {
        X509Certificate cert;
        try {
            InputStream in = new FileInputStream(certFile);
            CertificateFactory certificateFactory = CertificateFactory.getInstance(
                    "X.509", "BC");
            cert = (X509Certificate) certificateFactory.generateCertificate(in);
        } catch (FileNotFoundException e) {
            throw new CertException(e);
        } catch (CertificateException e) {
            throw new CertException(e);
        } catch (NoSuchProviderException e) {
            throw new CertException(e);
        }
        return cert;
    }

    private File getRootCertificateFile() throws CertException {
        File rootCertificateFile = null;
        for (CADescriptor ca : config) {
            if (ca.getRootKeystoreFilePath() != null) {
                rootKeystoreFilePath = ca.getRootKeystoreFilePath();
            }
        }
        if (rootKeystoreFilePath == null) {
            throw new CertException(
                    "You have to provide path for the root certificate file");
        }
        rootCertificateFile = new File(rootKeystoreFilePath);
        if (!rootCertificateFile.exists()) {
            throw new CertException("There is no root certificate file at:"
                    + rootCertificateFile.getAbsolutePath());
        }
        return rootCertificateFile;
    }

    protected Date getCertStartDate() {
        Calendar cal = Calendar.getInstance();
        return cal.getTime();
    }

    protected Date getCertEndDate() {
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.MONTH, 12);
        return cal.getTime();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public KeyStore initializeUser(UserInfo userInfo, String suppliedPassword)
            throws CertException {
        char[] password = suppliedPassword.toCharArray();
        KeyStore ks = null;
        String userName = userInfo.getUserFields().get(CNField.UserID);
        Alias keystoreAlias = new Alias(userName);
        try {
            ks = java.security.KeyStore.getInstance(KEYSTORE_TYPE);
            ks.load(null, password);
            KeyPairGenerator keyGen = KeyPairGenerator.getInstance(KEY_ALGORITHM);
            keyGen.initialize(KEY_SIZE);
            KeyPair keyPair = keyGen.genKeyPair();
            if (rootCertificate == null) {
                rootCertificate = getCertificate(getRootCertificateFile());
            }
            java.security.cert.Certificate[] chain = { rootCertificate };
            ks.setKeyEntry(keystoreAlias.getId(AliasType.KEY),
                    keyPair.getPrivate(), password, chain);
            Certificate cert = getCertificate(keyPair, userInfo);
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
    public KeyPair getKeyPair(KeyStore ks, UserInfo userInfo,
            String suppliedPassword) throws CertException {
        char[] password = suppliedPassword.toCharArray();
        KeyPair keyPair = null;
        String userName = userInfo.getUserFields().get(CNField.UserID);
        Alias ksAlias = new Alias(userName);
        try {
            if (!ks.containsAlias(ksAlias.getId(AliasType.KEY))) {
                throw new CertException("Missing keystore key entry for :"
                        + ksAlias.getUserName());
            }
            if (!ks.containsAlias(ksAlias.getId(AliasType.CERT))) {
                throw new CertException(
                        "Missing keystore certificate entry for :"
                                + ksAlias.getUserName());
            }
            PrivateKey privateKey = (PrivateKey) ks.getKey(
                    ksAlias.getId(AliasType.KEY), password);
            X509Certificate cert = (X509Certificate) ks.getCertificate(ksAlias.getId((AliasType.CERT)));
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
    public X509Certificate getCertificate(KeyStore ks, UserInfo userInfo)
            throws CertException {
        X509Certificate certificate = null;
        String userName = userInfo.getUserFields().get(CNField.UserID);
        Alias ksAlias = new Alias(userName);
        try {
            if (ks.containsAlias(ksAlias.getId(AliasType.CERT))) {
                certificate = (X509Certificate) ks.getCertificate(ksAlias.getId(AliasType.CERT));
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
    public KeyStore getKeyStore(InputStream keystoreIS, UserInfo userInfo,
            String password) throws CertException {
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
    public void registerContribution(Object contribution,
            String extensionPoint, ComponentInstance contributor) {
        config.add((CADescriptor) contribution);
    }

    @Override
    public void unregisterContribution(Object contribution,
            String extensionPoint, ComponentInstance contributor) {
        config.remove(contribution);
    }

}