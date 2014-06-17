package com.okta.saml;

import org.opensaml.common.xml.SAMLConstants;
import org.opensaml.saml2.metadata.EntityDescriptor;
import org.opensaml.saml2.metadata.IDPSSODescriptor;
import org.opensaml.saml2.metadata.KeyDescriptor;
import org.opensaml.saml2.metadata.SingleSignOnService;
import org.opensaml.ws.security.SecurityPolicyException;
import org.opensaml.xml.XMLObject;
import org.opensaml.xml.io.UnmarshallingException;
import org.opensaml.xml.security.credential.UsageType;
import org.opensaml.xml.security.keyinfo.KeyInfoHelper;
import org.opensaml.xml.signature.X509Certificate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Element;

import java.security.cert.CertificateException;

/**
 * This class is created from application element from the configuration,
 * and is used to access fields from its EntityDescriptor such as the certificate
 */
public class Application {

    private static final Logger logger = LoggerFactory.getLogger(Application.class);

    private EntityDescriptor descriptor;
    private SingleSignOnService ssoPost;
    private Certificate certificate;

    /**
     * Called by Configuration
     *
     * @param application an element node from parsing the configuration file
     * @throws SecurityPolicyException if there is a problem while parsing/creating an Application
     */
    public Application(Element application) throws SecurityPolicyException {
        try {
            Element entity = (Element) application.getElementsByTagName("md:EntityDescriptor").item(0);

            XMLObject root = org.opensaml.Configuration.getUnmarshallerFactory().getUnmarshaller(entity).unmarshall(entity);
            EntityDescriptor descriptor = (EntityDescriptor) root;
            this.descriptor = descriptor;

            IDPSSODescriptor idpSSO = descriptor.getIDPSSODescriptor(SAMLConstants.SAML20P_NS);
            for (SingleSignOnService sso : idpSSO.getSingleSignOnServices()) {
                if (sso.getBinding().equals(SAMLConstants.SAML2_POST_BINDING_URI)) {
                    ssoPost = sso;
                }
            }

            for (KeyDescriptor keyDescriptor : idpSSO.getKeyDescriptors()) {
                if (keyDescriptor.getUse().equals(UsageType.SIGNING)) {
                    try {
                        X509Certificate x509Cert = keyDescriptor.getKeyInfo().getX509Datas().get(0).getX509Certificates().get(0);
                        certificate = new Certificate(KeyInfoHelper.getCertificate(x509Cert));
                    } catch (NullPointerException e) {
                        throw new SecurityPolicyException("X509Certificate field is missing from the configuration file");
                    }
                    break;
                }
            }
        } catch (UnmarshallingException e) {
            logger.debug(e.getMessage());
            throw new SecurityPolicyException("There was a problem while parsing EntityDescriptor from the configuration file");
        } catch (CertificateException e) {
            logger.debug(e.getMessage());
            throw new SecurityPolicyException("There's a problem with the certificate");
        }
    }

    /**
     * @return application EntityDescriptor entityID
     */
    public String getIssuer() {
        return descriptor.getEntityID();
    }

    /**
     * @return the Location of SingleSignOnService whose Binding is HTTP-POST
     */
    public String getAuthenticationURL() {
        return ssoPost.getLocation();
    }

    /**
     * @return the Certificate created from EntityDescriptor X509Certificate
     */
    public Certificate getCertificate() {
        return certificate;
    }
}
