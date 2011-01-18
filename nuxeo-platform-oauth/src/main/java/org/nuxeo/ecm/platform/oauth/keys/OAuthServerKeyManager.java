package org.nuxeo.ecm.platform.oauth.keys;

public interface OAuthServerKeyManager {

    public String getPublicKeyCertificate();

    public String getBarePublicCertificate();

    public String getPrivateKey();

    public String getBarePrivateKey();

    public String getKeyName();

}