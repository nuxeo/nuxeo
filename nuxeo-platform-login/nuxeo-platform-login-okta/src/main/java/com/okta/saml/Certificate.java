package com.okta.saml;

import org.opensaml.xml.security.credential.Credential;
import org.opensaml.xml.security.x509.BasicX509Credential;

import java.security.cert.X509CRL;
import java.security.cert.X509Certificate;
import java.util.ArrayList;

/**
 * Wrapper for an X509Certificate
 */
public class Certificate {

    private final X509Certificate certificate;

    public Certificate(X509Certificate x509Cert) {
        certificate = x509Cert;
    }

    public X509Certificate getX509Cert() {
        return certificate;
    }

    /**
     * Used to validate signature in SAMLResponse.validateSignature()
     * @return Credential created from its X509Certificate
     */
    public Credential getCredential() {
        BasicX509Credential credential = new BasicX509Credential();
        credential.setEntityCertificate(certificate);
        credential.setPublicKey(certificate.getPublicKey());
        credential.setCRLs(new ArrayList<X509CRL>());
        return credential;
    }
}
