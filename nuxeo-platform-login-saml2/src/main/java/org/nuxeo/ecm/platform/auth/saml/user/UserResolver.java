package org.nuxeo.ecm.platform.auth.saml.user;

import java.util.Map;

import org.nuxeo.ecm.platform.auth.saml.SAMLCredential;

public interface UserResolver {

    void init(Map<String, String> parameters);

    String findOrCreateNuxeoUser(SAMLCredential userInfo);
}
