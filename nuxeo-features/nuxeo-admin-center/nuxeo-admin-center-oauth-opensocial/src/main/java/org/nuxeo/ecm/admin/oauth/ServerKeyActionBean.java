package org.nuxeo.ecm.admin.oauth;

import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.nuxeo.ecm.platform.oauth.keys.OAuthServerKeyManager;
import org.nuxeo.runtime.api.Framework;

@Name("oauthServerKeyActions")
@Scope(ScopeType.EVENT)
public class ServerKeyActionBean {

    public String getPublicCertificate() {
        OAuthServerKeyManager skm = Framework.getLocalService(OAuthServerKeyManager.class);
        return skm.getPublicKeyCertificate();
    }

    public String getKeyName() {
        OAuthServerKeyManager skm = Framework.getLocalService(OAuthServerKeyManager.class);
        return skm.getKeyName();
    }

}
