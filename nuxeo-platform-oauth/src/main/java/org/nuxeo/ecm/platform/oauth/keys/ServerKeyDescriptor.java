package org.nuxeo.ecm.platform.oauth.keys;

import java.io.Serializable;

import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XObject;

@XObject("serverKeyPair")
public class ServerKeyDescriptor implements Serializable {

    private static final long serialVersionUID = 1L;

    @XNode("privateKey")
    protected String externalPrivateKey;

    /**
     * Most folks should not need to change this from the default value of
     * nuxeo. It's unclear that there are many service providers that actually
     * use this value.
     */
    @XNode("privateKeyName")
    protected String externalPrivateKeyName;

    /**
     * This is here just for convenience of keeping everything together. This is
     * the public key (really a certificate) that you need to give to external
     * oauth provider to indicate that you have the private key above. Assuming
     * you generated the key with the lines above, you should be able to simply
     * paste the "certificate" portion of the testkey.pem file into this field.
     * It is not used by nuxeo in any way, but <b>will</b> be needed when you
     * configure an external provider.
     * <p>
     * Note that many providers accept the certificate then run a computation to
     * extract the public key from it. This means that the value displayed when
     * you look at the provider configuration may be different than the one you
     * provided.
     */
    @XNode("publicCertificate")
    protected String externalPublicCertificate;

}
