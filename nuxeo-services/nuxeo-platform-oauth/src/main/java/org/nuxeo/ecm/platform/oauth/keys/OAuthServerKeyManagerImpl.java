package org.nuxeo.ecm.platform.oauth.keys;

import org.nuxeo.runtime.model.ComponentInstance;
import org.nuxeo.runtime.model.DefaultComponent;

public class OAuthServerKeyManagerImpl extends DefaultComponent implements OAuthServerKeyManager {

    protected ServerKeyDescriptor serverKeyDescriptor=null;

    public static final String XP_SERVER_KEY ="serverKeyPair";

    public void registerContribution(Object contribution, String extensionPoint,
            ComponentInstance contributor) throws Exception {

        if (XP_SERVER_KEY.equals(extensionPoint)) {
            serverKeyDescriptor = (ServerKeyDescriptor) contribution;
        }
    }

    public void unregisterContribution(Object contribution, String extensionPoint,
            ComponentInstance contributor) throws Exception {

        if (XP_SERVER_KEY.equals(extensionPoint)) {
            serverKeyDescriptor = null;
        }
    }

    public String getPublicKeyCertificate() {
        if (serverKeyDescriptor!=null) {
            return serverKeyDescriptor.externalPublicCertificate;
        }
        return null;
    }

    public String getBarePublicCertificate() {
        return stripOpenSSL(getPublicKeyCertificate());
    }

    public String getPrivateKey() {
        if (serverKeyDescriptor!=null) {
            return serverKeyDescriptor.externalPrivateKey;
        }
        return null;
    }

    public String getBarePrivateKey() {
        return stripOpenSSL(getPrivateKey());
    }

    public String getKeyName() {
        if (serverKeyDescriptor!=null) {
            return serverKeyDescriptor.externalPrivateKeyName;
        }
        return null;
    }

    protected String stripOpenSSL(String key) {
        if (key==null) {
            return null;
        }
        return key.replaceAll("-----[A-Z ]*-----", "").replace("\n", "");
    }

}

