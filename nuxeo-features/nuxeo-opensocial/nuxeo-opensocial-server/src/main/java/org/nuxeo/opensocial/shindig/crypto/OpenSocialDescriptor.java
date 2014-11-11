package org.nuxeo.opensocial.shindig.crypto;

import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XNodeList;
import org.nuxeo.common.xmap.annotation.XObject;

/**
 * This class is the java-level description of the parameters that are
 * configured into the opensocial implementation. This contribution is usually
 * packaged as default-opensocial-config.xml and resides in the config
 * directory.
 * 
 * The implementation of opensocial is based on shindig 1.1 and is integrated
 * inside nuxeo itself (there is no extra "shindig process").
 * 
 * @author iansmith
 * 
 */
@XObject("opensocial")
public class OpenSocialDescriptor {

    /**
     * This field is the key that is used by shindig to communicate with itself.
     * For example, sometimes the interpretation of a gadget results in a call
     * to the "make request" servlet for access to external resources. This
     * symmetric key is used to sign the message going from shindig to shindig
     * to verify that the message receivied by the make request servlet is not
     * "forged".
     * 
     * This value can and, in most cases should, be left empty. When it is left
     * empty, the system will use a random set of bytes for this key.
     */
    @XNode("signingKey")
    protected String signingKey;

    /**
     * This is the URL that shindig should tell other servers to use to call us
     * back on. If you have changed where nuxeo is mounted (not in /nuxeo) you
     * may need to set this to have your prefix.
     * 
     * If you are running nuxeo in the default configuration, you should not
     * need to configure this.
     */
    @XNode("oauthCallbackUrl")
    protected String callbackUrl;

    /**
     * You can have any number of portal configurations, but most people should
     * simply ignore this.
     */
    @XNodeList(value = "portals/portalConfig", type = PortalConfig[].class, componentType = PortalConfig.class)
    protected PortalConfig[] portal;

    /**
     * This is the key that is used by a particular instance of nuxeo (running
     * shindig) for signing requests to external OAuth providers. This key is
     * the "consumer private key" in the language of OAuth. It must be of type
     * RSA because we currently do not have support for the HMAC style keys. The
     * generation of this private key can be done like this on unix:
     * 
     * <PRE>
     * openssl req -newkey rsa:1024 -days 365 -nodes -x509 -keyout testkey.pem -out testkey.pem -subj '/CN=mytestkey'
     * openssl pkcs8 -in testkey.pem -out oauthkey.pem -topk8 -nocrypt -outform PEM
     * </PRE>
     * 
     * The result is in oauth and should be pasted into the configuration file
     * as the externalPrivateKey value.
     * 
     */
    @XNode("externalPrivateKey")
    protected String externalPrivateKey;

    /**
     * Most folks should not need to change this from the default value of
     * nuxeo. It's unclear that there are many service providers that actually
     * use this value.
     */
    @XNode("externalPrivateKeyName")
    protected String externalPrivateKeyName;

    /**
     * This is here just for convenience of keeping everything together. This is
     * the public key (really a certificate) that you need to give to external
     * oauth provider to indicate that you have the private key above. Assuming
     * you generated the key with the lines above, you should be able to simply
     * paste the "certificate" portion of the testkey.pem file into this field.
     * It is not used by nuxeo in any way, but <b>will</b> be needed when you
     * configure an external provider.
     * 
     * Note that many providers accept the certificate then run a computation to
     * extract the public key from it. This means that the value displayed when
     * you look at the provider configuration may be different than the one you
     * provided.
     */
    @XNode("externalPublicCertificate")
    protected String externalPublicCertificate;

    /**
     * This a list of nuxeo trusted hosts. Such a host will be passed the
     * browsers jsession id to avoid the need to constantly re-authenticate to
     * retrieve nuxeo data when the user is already logged into a nuxeo server
     * to access the dashboard. Will be colon separated.
     */
    @XNode("trustedHosts")
    protected String trustedHosts;

    public String getSigningKey() {
        return signingKey;
    }

    public String getExternalPrivateKeyName() {
        return externalPrivateKeyName;
    }

    /**
     * For now, this is always null because it isn't used.
     * 
     * @return
     */
    public PortalConfig[] getPortalConfig() {
        return portal;
    }

    public String getCallbackUrl() {
        return callbackUrl;
    }

    public void setSigningKey(String keyAsBase64) {
        signingKey = keyAsBase64;
    }

    public String getExternalPrivateKey() {
        return externalPrivateKey;

    }

    public String[] getTrustedHosts() {
        return trustedHosts.split(":");
    }

    /**
     * You can have any number of oauthservice configurations, but most people
     * should simply ignore this.
     */
    @XNodeList(value = "oauthservices/oauthservice", type = OAuthServiceDescriptor[].class, componentType = OAuthServiceDescriptor.class)
    protected OAuthServiceDescriptor[] services;

    public OAuthServiceDescriptor[] getOAuthServices() {
        return services;

    }
}